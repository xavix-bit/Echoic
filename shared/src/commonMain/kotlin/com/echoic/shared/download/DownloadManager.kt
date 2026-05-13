package com.echoic.shared.download

import com.echoic.shared.model.LocalTTSProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.platformFileOutputStream
import com.echoic.shared.platform.PlatformOutputStream
import com.echoic.shared.platform.PlatformRandomAccessFile
import com.echoic.shared.platform.platformCurrentTimeMillis

sealed class DownloadState {
    data object Idle : DownloadState()

    data class Downloading(
        val progress: Float,
        val speed: Long,
        val sourceName: String,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val currentFile: Int = 0,
        val totalFiles: Int = 0,
    ) : DownloadState()

    data class Completed(val path: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data object Cancelled : DownloadState()
}

data class DownloadSource(
    val name: String,
    val url: String,
    val priority: Int,
    val isAvailable: Boolean = true,
    val description: String = "",
)

class DownloadManager {
    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60 * 60 * 1000 // 1 hour for the entire request
            connectTimeoutMillis = 30_000 // 30 seconds
            socketTimeoutMillis = 30_000 // 30 seconds of inactivity to trigger retry
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var isCancelled = false

    fun getDefaultSources(providerName: String): List<DownloadSource> {
        val modelPath = providerName.lowercase().replace(" ", "-")
        return listOf(
            DownloadSource(
                name = "hf-mirror.com",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/$modelPath",
                priority = 1,
                description = "国内镜像",
            ),
            DownloadSource(
                name = "HuggingFace",
                url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/$modelPath",
                priority = 2,
                description = "官方源",
            ),
        )
    }

    suspend fun discoverRepositoryFiles(
        provider: LocalTTSProvider,
        repositoryUrl: String,
    ): List<DownloadFile> {
        val repository = DownloadConfig.parseHuggingFaceRepository(repositoryUrl)
            ?: return DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl)

        return try {
            val response = httpClient.get(DownloadConfig.buildApiUrl(repository))
            if (!response.status.isSuccess()) {
                return DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl)
            }

            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val siblings = root["siblings"]?.jsonArray.orEmpty()
            val files = siblings.mapNotNull { element ->
                val obj = element.jsonObject
                val relativePath = obj["rfilename"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (!isModelAsset(relativePath)) return@mapNotNull null

                DownloadFile(
                    relativePath = relativePath,
                    url = DownloadConfig.buildResolveUrl(repository, relativePath),
                    sizeBytes = obj.longOrNull("size"),
                )
            }

            files.ifEmpty { DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl) }
        } catch (_: Exception) {
            DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl)
        }
    }

    /**
     * 下载多个文件，提供基于实际字节数的准确进度反馈。
     *
     * 当文件总大小未知时（如回退文件列表无 sizeBytes），会动态估算总量：
     * - 初始时根据已知文件大小估算总大小
     * - 每下载完一个文件，用实际大小更新估算
     * - 进度 = 已完成字节 / 动态估算总字节
     */
    suspend fun downloadFiles(
        files: List<DownloadFile>,
        targetDir: String,
        sourceName: String,
        expectedTotalBytes: Long = 0L,
        onProgress: (DownloadState) -> Unit,
    ) {
        if (files.isEmpty()) {
            fail("没有可下载的文件", onProgress)
            return
        }

        isCancelled = false

        val targetDirectory = PlatformFile(targetDir)
        targetDirectory.mkdirs()

        // 动态追踪每个文件的实际大小：null 表示尚不知道
        val fileSizes = files.map { it.sizeBytes }.toMutableList()
        val totalFiles = files.size
        var completedBytes = 0L

        fun computeDynamicTotal(): Long {
            val knownSizes = fileSizes.filterNotNull()
            if (knownSizes.size == totalFiles) {
                // 所有文件大小已知
                return knownSizes.sum()
            }
            if (knownSizes.isEmpty()) {
                // 完全不知道大小，返回 0（外部会用文件数回退）
                return 0L
            }
            // 基于已知文件的平均大小估算未知文件
            val knownSum = knownSizes.sum()
            val avgSize = knownSum / knownSizes.size
            val unknownCount = totalFiles - knownSizes.size
            return knownSum + (avgSize * unknownCount)
        }

        fun emitAggregate(
            fileIndex: Int,
            currentFileBytes: Long,
            currentFileTotal: Long,
            speed: Long,
            currentFileName: String,
        ) {
            val dynamicTotal = computeDynamicTotal()
            val aggregateDownloaded = completedBytes + currentFileBytes

            val aggregateProgress: Float
            val aggregateTotal: Long

            if (expectedTotalBytes > 0) {
                aggregateTotal = expectedTotalBytes
                aggregateProgress = (aggregateDownloaded.toFloat() / aggregateTotal).coerceIn(0f, 1f)
            } else if (dynamicTotal > 0) {
                aggregateTotal = dynamicTotal
                aggregateProgress = (aggregateDownloaded.toFloat() / dynamicTotal).coerceIn(0f, 1f)
            } else if (currentFileTotal > 0) {
                // 用当前文件大小粗略估算（假设所有文件差不多大）
                aggregateTotal = currentFileTotal * totalFiles
                aggregateProgress = (aggregateDownloaded.toFloat() / aggregateTotal).coerceIn(0f, 1f)
            } else {
                // 完全未知：用文件计数回退
                aggregateTotal = 0L
                aggregateProgress = ((fileIndex + (if (currentFileTotal > 0) currentFileBytes.toFloat() / currentFileTotal else 0f)) / totalFiles).coerceIn(0f, 1f)
            }

            val state = DownloadState.Downloading(
                progress = aggregateProgress,
                speed = speed,
                sourceName = "$sourceName: $currentFileName",
                downloadedBytes = aggregateDownloaded,
                totalBytes = aggregateTotal,
                currentFile = fileIndex + 1,
                totalFiles = totalFiles,
            )
            _downloadState.value = state
            onProgress(state)
        }

        try {
            files.forEachIndexed { index, file ->
                if (isCancelled) throw CancellationException("下载已取消")

                val destination = safeDestination(targetDirectory, file.relativePath)
                val fileSize = file.sizeBytes

                // 跳过已完整下载的文件
                if (fileSize != null && destination.exists() && destination.length() == fileSize) {
                    completedBytes += fileSize
                    emitAggregate(
                        fileIndex = index,
                        currentFileBytes = fileSize,
                        currentFileTotal = fileSize,
                        speed = 0L,
                        currentFileName = file.relativePath,
                    )
                    return@forEachIndexed
                }

                var retryCount = 0
                val maxRetries = 3
                var downloaded = 0L
                var success = false
                while (retryCount < maxRetries && !success) {
                    try {
                        downloaded = downloadSingleFile(
                            file = file,
                            destination = destination,
                            sourceName = sourceName,
                        ) { state ->
                            if (state is DownloadState.Downloading) {
                                // 动态更新文件大小估算
                                if (fileSizes[index] == null && state.totalBytes > 0) {
                                    fileSizes[index] = state.totalBytes
                                }
                                emitAggregate(
                                    fileIndex = index,
                                    currentFileBytes = state.downloadedBytes,
                                    currentFileTotal = state.totalBytes,
                                    speed = state.speed,
                                    currentFileName = file.relativePath,
                                )
                            }
                        }
                        success = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount >= maxRetries) {
                            throw e
                        }
                        kotlinx.coroutines.delay(1000L * retryCount) // exponential backoff
                    }
                }

                // 下载完成，记录实际大小
                if (fileSizes[index] == null) {
                    fileSizes[index] = downloaded
                }
                completedBytes += downloaded
            }

            val completedState = DownloadState.Completed(targetDirectory.absolutePath)
            _downloadState.value = completedState
            onProgress(completedState)
        } catch (e: CancellationException) {
            _downloadState.value = DownloadState.Cancelled
            onProgress(DownloadState.Cancelled)
            throw e
        } catch (e: Exception) {
            fail(e.message ?: "下载失败", onProgress)
            throw e
        }
    }

    suspend fun downloadFile(
        url: String,
        destination: PlatformFile,
        sourceName: String = "Direct",
        onProgress: (DownloadState) -> Unit = {},
    ) {
        _downloadState.value = DownloadState.Idle
        isCancelled = false
        try {
            downloadSingleFile(
                file = DownloadFile(destination.name, url),
                destination = destination,
                sourceName = sourceName,
                onProgress = { state ->
                    _downloadState.value = state
                    onProgress(state)
                },
            )
            val completedState = DownloadState.Completed(destination.absolutePath)
            _downloadState.value = completedState
            onProgress(completedState)
        } catch (e: CancellationException) {
            _downloadState.value = DownloadState.Cancelled
            onProgress(DownloadState.Cancelled)
            throw e
        } catch (e: Exception) {
            val failedState = DownloadState.Failed(e.message ?: "下载失败")
            _downloadState.value = failedState
            onProgress(failedState)
            throw e
        }
    }

    suspend fun testSourceSpeed(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val testUrl = DownloadConfig.parseHuggingFaceRepository(url)
                ?.let { DownloadConfig.buildApiUrl(it) }
                ?: url
            val startTime = platformCurrentTimeMillis()
            val response = httpClient.get(testUrl) {
                header(HttpHeaders.Range, "bytes=0-8191")
            }
            val elapsedMs = (platformCurrentTimeMillis() - startTime).coerceAtLeast(1)
            if (response.status.isSuccess() || response.status == HttpStatusCode.PartialContent) {
                response.bodyAsText().encodeToByteArray().size * 1000L / elapsedMs
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }

    fun cancelDownload() {
        isCancelled = true
        _downloadState.value = DownloadState.Cancelled
    }

    fun resetState() {
        isCancelled = false
        _downloadState.value = DownloadState.Idle
    }

    fun close() {
        httpClient.close()
    }

    private suspend fun downloadSingleFile(
        file: DownloadFile,
        destination: PlatformFile,
        sourceName: String,
        onProgress: (DownloadState) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()

        var existingBytes = if (destination.exists()) destination.length() else 0L
        if (file.sizeBytes != null && existingBytes > file.sizeBytes) {
            destination.delete()
            existingBytes = 0L
        }

        return@withContext httpClient.prepareGet(file.url) {
            header(HttpHeaders.Accept, "application/octet-stream")
            if (existingBytes > 0) {
                header(HttpHeaders.Range, "bytes=$existingBytes-")
            }
        }.execute { response ->
            if (response.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                val contentRange = response.headers[HttpHeaders.ContentRange]
                val actualSize = contentRange?.substringAfterLast("/")?.toLongOrNull()
                if (actualSize != null && existingBytes >= actualSize) {
                    // File is already fully downloaded
                    return@execute actualSize
                } else {
                    // Mismatched or invalid range, delete local file to retry from scratch
                    destination.delete()
                    throw Exception("${file.relativePath}: HTTP 416 Range Not Satisfiable (Cleaned partial file to retry)")
                }
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("${file.relativePath}: HTTP ${response.status.value} ${response.status.description}")
            }

            val contentType = response.headers[HttpHeaders.ContentType]?.lowercase().orEmpty()
            if (contentType.contains("text/html")) {
                throw Exception("${file.relativePath}: 下载链接返回 HTML 页面，不是模型文件（可能需要更换下载源）")
            }

            val resumeSupported = response.status == HttpStatusCode.PartialContent
            if (!resumeSupported && existingBytes > 0) {
                destination.delete()
                existingBytes = 0L
            }

            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
            val totalBytes = file.sizeBytes ?: if (resumeSupported && existingBytes > 0) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = existingBytes
            var lastReportTime = platformCurrentTimeMillis()
            var lastSpeedCalcTime = platformCurrentTimeMillis()
            var lastSpeedCalcBytes = existingBytes
            var smoothedSpeed = 0L

            fun emitProgress(force: Boolean = false) {
                val now = platformCurrentTimeMillis()
                
                // 限制 UI 刷新频率（每 200ms 最多刷新一次）
                if (!force && (now - lastReportTime) < 200) return

                // 限制速度计算频率（每 500ms 计算一次瞬时速度，并进行平滑处理）
                val speedElapsed = (now - lastSpeedCalcTime).coerceAtLeast(1)
                if (speedElapsed >= 500) {
                    val instantSpeed = (downloadedBytes - lastSpeedCalcBytes).coerceAtLeast(0L) * 1000L / speedElapsed
                    // 使用指数移动平均 (EMA) 使速度变化更平滑，避免剧烈跳变
                    smoothedSpeed = if (smoothedSpeed == 0L) {
                        instantSpeed
                    } else {
                        (smoothedSpeed * 0.7 + instantSpeed * 0.3).toLong()
                    }
                    lastSpeedCalcTime = now
                    lastSpeedCalcBytes = downloadedBytes
                }

                val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                onProgress(
                    DownloadState.Downloading(
                        progress = progress.coerceIn(0f, 1f),
                        speed = smoothedSpeed,
                        sourceName = "$sourceName: ${file.relativePath}",
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                    )
                )
                lastReportTime = now
            }

            if (resumeSupported && existingBytes > 0) {
                PlatformRandomAccessFile(destination, "rw").let { raf ->
                    try {
                        raf.seek(existingBytes)
                        while (!channel.isClosedForRead && !isCancelled) {
                            val read = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                                channel.readAvailable(buffer)
                            } ?: throw Exception("读取超时")
                            if (read > 0) {
                                raf.write(buffer, 0, read)
                                downloadedBytes += read
                                emitProgress()
                            }
                        }
                    } finally {
                        raf.close()
                    }
                }
            } else {
                val fos = platformFileOutputStream(destination, false)
                try {
                    while (!channel.isClosedForRead && !isCancelled) {
                        val read = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                            channel.readAvailable(buffer)
                        } ?: throw Exception("读取超时")
                        if (read > 0) {
                            fos.write(buffer, 0, read)
                            downloadedBytes += read
                            emitProgress()
                        }
                    }
                    fos.flush()
                } finally {
                    fos.close()
                }
            }

            if (isCancelled) throw CancellationException("下载已取消")
            emitProgress(force = true)

            if (file.sizeBytes != null && downloadedBytes != file.sizeBytes) {
                throw Exception("${file.relativePath}: 文件大小不匹配，期望 ${file.sizeBytes} bytes，实际 $downloadedBytes bytes")
            }

            downloadedBytes
        }
    }

    private fun fail(message: String, onProgress: (DownloadState) -> Unit) {
        val state = DownloadState.Failed(message)
        _downloadState.value = state
        onProgress(state)
    }

    private fun safeDestination(root: PlatformFile, relativePath: String): PlatformFile {
        val destination = PlatformFile(root, relativePath)
        val rootPath = root.canonicalPath
        val destinationPath = destination.canonicalPath
        if (!destinationPath.startsWith(rootPath)) {
            throw IllegalArgumentException("非法文件路径: $relativePath")
        }
        return destination
    }

    private fun isModelAsset(relativePath: String): Boolean {
        val name = relativePath.substringAfterLast("/")
        if (name == ".gitattributes" || name == ".gitignore" || name == "README.md" || name == "LICENSE") return false
        val lower = relativePath.lowercase()
        return listOf(
            ".bin",
            ".json",
            ".model",
            ".onnx",
            ".pth",
            ".pt",
            ".safetensors",
            ".spm",
            ".py",
            ".txt",
            ".yaml",
            ".yml",
            ".wav",
            ".mp3",
        ).any { lower.endsWith(it) } || !relativePath.contains(".")
    }

    private fun JsonObject.longOrNull(key: String): Long? {
        return this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
