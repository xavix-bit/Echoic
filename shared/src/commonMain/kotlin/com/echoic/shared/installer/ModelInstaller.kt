package com.echoic.shared.installer

import com.echoic.shared.download.DownloadConfig
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.download.DownloadSource
import com.echoic.shared.download.DownloadState
import com.echoic.shared.model.LocalModelInstallRepository
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.PlatformInputStream
import com.echoic.shared.platform.PlatformZipInputStream
import com.echoic.shared.platform.platformFileInputStream
import com.echoic.shared.platform.platformFileOutputStream
import com.echoic.shared.platform.platformGzipInputStream
import com.echoic.shared.platform.platformZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 模型安装状态
 */
sealed class InstallState {
    /** 空闲状态 */
    data object Idle : InstallState()

    /** 下载中 */
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speed: Long = 0L,
        val currentFile: Int = 0,
        val totalFiles: Int = 0,
    ) : InstallState()

    /** 解压中 */
    data class Extracting(val progress: Float) : InstallState()

    /** 验证中 */
    data object Verifying : InstallState()

    /** 安装完成 */
    data class Completed(val path: String) : InstallState()

    /** 安装失败 */
    data class Failed(val error: String, val canRetry: Boolean = true) : InstallState()

    /** 已取消 */
    data object Cancelled : InstallState()
}

/**
 * 模型安装管理器
 * 提供一键安装、重新安装、选择下载源安装等功能
 */
class ModelInstaller(
    private val downloadManager: DownloadManager,
    private val localModelManager: LocalModelManager,
    private val repository: LocalModelInstallRepository = LocalModelInstallRepository(),
) {
    suspend fun installModel(
        provider: LocalTTSProvider,
        selectedSource: DownloadSource? = null,
        onStateChange: (InstallState) -> Unit,
    ) {
        if (isInstalled(provider)) {
            onStateChange(InstallState.Completed(getInstallPath(provider)))
            return
        }

        val sources = if (selectedSource != null) {
            listOf(selectedSource)
        } else {
            getDownloadSources(provider)
        }

        if (sources.isEmpty()) {
            onStateChange(InstallState.Failed("没有可用的下载源", canRetry = false))
            return
        }

        val installDir = getInstallPath(provider)
        val downloadDir = getDownloadTempPath(provider)
        val errors = mutableListOf<String>()
        var downloadSucceeded = false

        for ((sourceIndex, source) in sources.withIndex()) {
            if (downloadSucceeded) break

            val label = if (sources.size > 1)
                "${source.name} (${sourceIndex + 1}/${sources.size})"
            else
                source.name

            // 发送初始状态，避免网络请求导致界面无响应
            onStateChange(InstallState.Downloading(progress = 0f))

            val downloadFiles = try {
                downloadManager.discoverRepositoryFiles(provider, source.url)
            } catch (_: Exception) {
                DownloadConfig.getFallbackDownloadFiles(provider, source.url)
            }

            if (downloadFiles.isEmpty()) {
                errors.add("${source.name}: 没有可下载的文件")
                continue
            }

            try {
                downloadManager.downloadFiles(
                    files = downloadFiles,
                    targetDir = downloadDir,
                    sourceName = label,
                    expectedTotalBytes = ((provider.modelSizeMB ?: 0) * 1024L * 1024L),
                    onProgress = { state ->
                        when (state) {
                            is DownloadState.Downloading -> {
                                onStateChange(
                                    InstallState.Downloading(
                                        progress = state.progress,
                                        downloadedBytes = state.downloadedBytes,
                                        totalBytes = state.totalBytes,
                                        speed = state.speed,
                                        currentFile = state.currentFile,
                                        totalFiles = state.totalFiles,
                                    )
                                )
                            }
                            is DownloadState.Completed -> {
                                downloadSucceeded = true
                            }
                            is DownloadState.Cancelled -> {
                                onStateChange(InstallState.Cancelled)
                            }
                            is DownloadState.Failed -> {
                                errors.add("${source.name}: ${state.error}")
                            }
                            else -> {}
                        }
                    },
                )
                if (downloadSucceeded) break
            } catch (e: CancellationException) {
                if (!downloadSucceeded) onStateChange(InstallState.Cancelled)
                return
            } catch (e: Exception) {
                errors.add("${source.name}: ${e.message ?: "下载失败"}")
            }
        }

        if (!downloadSucceeded) {
            val msg = if (errors.isEmpty()) "下载失败：未知错误" else errors.joinToString("\n")
            onStateChange(InstallState.Failed(msg, canRetry = true))
            return
        }

        val downloadDirFile = PlatformFile(downloadDir)
        val downloadedFiles = downloadDirFile.listFiles() ?: emptyList()
        val installDirFile = PlatformFile(installDir)

        val archiveFile = downloadedFiles.find { file ->
            file.name.endsWith(".tar.gz") ||
                file.name.endsWith(".zip") ||
                file.name.endsWith(".gz")
        }

        val finalPath = if (archiveFile != null) {
            onStateChange(InstallState.Extracting(0f))
            try {
                cleanDirectory(installDirFile)
                installDirFile.mkdirs()
                extractModel(archiveFile.absolutePath, installDirFile.absolutePath) { progress ->
                    onStateChange(InstallState.Extracting(progress))
                }
                downloadedFiles.filter { it != archiveFile }.forEach { file ->
                    copyRecursively(file, PlatformFile(installDirFile, file.name))
                }
                installDirFile.absolutePath
            } catch (e: Exception) {
                onStateChange(InstallState.Failed("解压失败: ${e.message ?: "未知错误"}", canRetry = true))
                return
            } finally {
                try { downloadDirFile.deleteRecursively() } catch (_: Exception) {}
            }
        } else {
            try {
                cleanDirectory(installDirFile)
                installDirFile.mkdirs()
                downloadedFiles.forEach { file ->
                    copyRecursively(file, PlatformFile(installDirFile, file.name))
                }
                downloadDirFile.deleteRecursively()
                installDirFile.absolutePath
            } catch (e: Exception) {
                onStateChange(InstallState.Failed("移动文件失败: ${e.message ?: "未知错误"}", canRetry = true))
                return
            }
        }

        verifyAndMarkInstalled(provider, finalPath, onStateChange)
    }

    private suspend fun verifyAndMarkInstalled(
        provider: LocalTTSProvider,
        finalPath: String,
        onStateChange: (InstallState) -> Unit,
    ) {
        onStateChange(InstallState.Verifying)
        try {
            verifyModel(finalPath)
        } catch (e: Exception) {
            onStateChange(InstallState.Failed("验证失败: ${e.message ?: "未知错误"}", canRetry = true))
            return
        }

        val installedSize = repository.sizeOf(PlatformFile(finalPath))
        localModelManager.markAsInstalled(provider, installedSize)
        onStateChange(InstallState.Completed(finalPath))
    }

    suspend fun reinstallModel(
        provider: LocalTTSProvider,
        onStateChange: (InstallState) -> Unit,
    ) {
        if (isInstalled(provider)) {
            localModelManager.uninstallModel(provider)
        }
        installModel(provider, onStateChange = onStateChange)
    }

    suspend fun installWithSource(
        provider: LocalTTSProvider,
        source: DownloadSource,
        onStateChange: (InstallState) -> Unit,
    ) {
        if (isInstalled(provider)) {
            localModelManager.uninstallModel(provider)
        }
        installModel(provider, selectedSource = source, onStateChange = onStateChange)
    }

    fun isInstalled(provider: LocalTTSProvider): Boolean {
        return repository.isInstalled(provider)
    }

    fun getInstallPath(provider: LocalTTSProvider): String {
        return repository.modelPath(provider)
    }

    private fun getDownloadTempPath(provider: LocalTTSProvider): String {
        return repository.downloadPath(provider)
    }

    fun getDownloadSources(provider: LocalTTSProvider): List<DownloadSource> {
        val mirrors = provider.downloadMirrors
        return if (mirrors.isNotEmpty()) {
            mirrors.mapIndexed { index, mirror ->
                DownloadSource(
                    name = mirror.name,
                    url = mirror.url,
                    priority = if (mirror.isDefault) 0 else index + 1,
                    description = if (mirror.isDefault) "推荐" else "",
                )
            }.sortedBy { it.priority }
        } else {
            downloadManager.getDefaultSources(provider.displayName)
        }
    }

    private suspend fun extractModel(
        archivePath: String,
        extractDir: String,
        onProgress: (Float) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val archiveFile = PlatformFile(archivePath)
            val destDir = PlatformFile(extractDir)
            destDir.mkdirs()

            when {
                archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz") -> {
                    extractTarGz(archiveFile, destDir, onProgress)
                }
                archivePath.endsWith(".zip") -> {
                    extractZip(archiveFile, destDir, onProgress)
                }
                archivePath.endsWith(".gz") -> {
                    extractGz(archiveFile, destDir, onProgress)
                }
                else -> {
                    throw Exception("不支持的压缩格式: ${archiveFile.name}")
                }
            }
        }
    }

    private fun extractTarGz(
        archiveFile: PlatformFile,
        destDir: PlatformFile,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        val gzis = platformGzipInputStream(platformFileInputStream(archiveFile))
        try {
            val buffer = ByteArray(8192)
            val headerBuffer = ByteArray(512)

            while (true) {
                val headerRead = readFully(gzis, headerBuffer)
                if (headerRead < 512) break

                if (headerBuffer.all { it == 0.toByte() }) break

                val nameBytes = headerBuffer.copyOfRange(0, 100)
                val name = String(nameBytes, Charsets.US_ASCII).trim('\u0000')

                val sizeBytes = headerBuffer.copyOfRange(124, 136)
                val sizeStr = String(sizeBytes, Charsets.US_ASCII).trim('\u0000', ' ')
                val fileSize = sizeStr.toLongOrNull(8) ?: 0L

                if (name.isEmpty() || name == "./" || name == ".") {
                    continue
                }

                val outputFile = PlatformFile(destDir, name.removePrefix("/"))
                if (name.endsWith("/")) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    val fos = platformFileOutputStream(outputFile, false)
                    try {
                        var remaining = fileSize
                        while (remaining > 0) {
                            val toRead = minOf(remaining.toLong(), buffer.size.toLong()).toInt()
                            val read = gzis.read(buffer, 0, toRead)
                            if (read == -1) break
                            fos.write(buffer, 0, read)
                            remaining -= read
                            bytesRead += read

                            if (totalSize > 0) {
                                onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                            }
                        }
                        fos.flush()
                    } finally {
                        fos.close()
                    }
                }

                val padding = (512 - (fileSize % 512)).toInt()
                if (padding < 512) {
                    var skipRemaining = padding.toLong()
                    while (skipRemaining > 0) {
                        val skipped = gzis.skip(skipRemaining)
                        if (skipped <= 0) break
                        skipRemaining -= skipped
                    }
                }

                bytesRead += 512
                if (totalSize > 0) {
                    onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                }
            }
        } finally {
            gzis.close()
        }

        onProgress(1.0f)
    }

    private fun readFully(input: PlatformInputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) return offset
            offset += read
        }
        return offset
    }

    private fun extractZip(
        archiveFile: PlatformFile,
        destDir: PlatformFile,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        val zis = platformZipInputStream(platformFileInputStream(archiveFile))
        try {
            val buffer = ByteArray(8192)
            var entry = zis.nextEntry

            while (entry != null) {
                val outputFile = PlatformFile(destDir, entry.name)

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    val fos = platformFileOutputStream(outputFile, false)
                    try {
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            bytesRead += read
                            if (totalSize > 0) {
                                onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                            }
                        }
                        fos.flush()
                    } finally {
                        fos.close()
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        } finally {
            zis.close()
        }

        onProgress(1.0f)
    }

    private fun extractGz(
        archiveFile: PlatformFile,
        destDir: PlatformFile,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        val outputName = archiveFile.name.removeSuffix(".gz")
        val outputFile = PlatformFile(destDir, outputName)

        val gzis = platformGzipInputStream(platformFileInputStream(archiveFile))
        try {
            val fos = platformFileOutputStream(outputFile, false)
            try {
                val buffer = ByteArray(8192)
                var read: Int
                while (gzis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                    bytesRead += read
                    if (totalSize > 0) {
                        onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                    }
                }
                fos.flush()
            } finally {
                fos.close()
            }
        } finally {
            gzis.close()
        }

        onProgress(1.0f)
    }

    private suspend fun verifyModel(modelPath: String) {
        withContext(Dispatchers.IO) {
            val path = PlatformFile(modelPath)

            if (!path.exists()) {
                throw Exception("模型文件不存在: $modelPath")
            }

            if (path.isDirectory) {
                val files = path.listFiles()
                if (files == null || files.isEmpty()) {
                    throw Exception("模型目录为空: $modelPath")
                }

                val totalSize = repository.sizeOf(path)
                if (totalSize < 1024) {
                    throw Exception("模型文件过小 (${totalSize} bytes)，可能下载不完整")
                }
            } else {
                if (path.length() < 1024) {
                    throw Exception("模型文件过小 (${path.length()} bytes)，可能下载不完整")
                }
            }
        }
    }

    private suspend fun cleanDirectory(directory: PlatformFile) {
        withContext(Dispatchers.IO) {
            if (directory.exists()) {
                directory.deleteRecursively()
            }
            directory.mkdirs()
        }
    }

    private fun copyRecursively(source: PlatformFile, target: PlatformFile) {
        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursively(child, PlatformFile(target, child.name))
            }
        } else {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
        }
    }
}
