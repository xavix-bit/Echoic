package com.echoic.shared.engine

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalModelInstallRepository
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.platform.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 桌面端本地 TTS 引擎，通过 Sherpa-ONNX JNI 原生库进行语音合成。
 *
 * 原生库（libsherpa-onnx-jni + libonnxruntime）内嵌于应用中，
 * 首次使用时自动提取到 ~/.echoic/native/ 并加载。
 */
class DesktopLocalEngine : LocalTTSEngine {

    private val modelRepository = LocalModelInstallRepository()
    private var tts: OfflineTts? = null
    @Volatile private var cancelled = false

    override fun isAvailable(): Boolean {
        return try {
            NativeLibLoader.load()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun supports(provider: LocalTTSProvider): Boolean {
        return when (provider) {
            LocalTTSProvider.SHERPA -> true
            LocalTTSProvider.KOKORO -> true
            // VoxCPM 需要 Python + GPU，不适合内嵌
            LocalTTSProvider.VOXCPM -> false
            // VibeVoice 通过本地 REST API 调用
            LocalTTSProvider.VIBEVOICE -> true
        }
    }

    override suspend fun synthesize(
        text: String,
        provider: LocalTTSProvider,
        voiceId: Int,
        format: AudioFormat,
    ): LocalSynthesisResult {
        cancelled = false
        require(text.isNotBlank()) { "输入文本不能为空" }
        require(supports(provider)) {
            "当前不支持 ${provider.displayName} 的本地合成。"
        }

        return when (provider) {
            LocalTTSProvider.SHERPA -> synthesizeViaSherpaOnnx(text, provider, voiceId, format)
            LocalTTSProvider.KOKORO -> synthesizeViaKokoro(text, provider, voiceId, format)
            LocalTTSProvider.VIBEVOICE -> synthesizeViaVibeVoice(text, provider, format)
            else -> throw UnsupportedOperationException("${provider.displayName} 暂不支持本地合成")
        }
    }

    private suspend fun synthesizeViaKokoro(
        text: String,
        provider: LocalTTSProvider,
        voiceId: Int,
        format: AudioFormat,
    ): LocalSynthesisResult = withContext(Dispatchers.IO) {
        NativeLibLoader.load()

        val modelDir = getModelDir(provider)
        val modelFile = findFile(modelDir, ".onnx")
            ?: throw IllegalStateException("找不到 ONNX 模型文件（.onnx）于 $modelDir")
        val voicesFile = findFile(modelDir, "voices.bin")
            ?: throw IllegalStateException("找不到 voices.bin 于 $modelDir")
        val tokensFile = findFile(modelDir, "tokens.txt")
            ?: throw IllegalStateException("找不到 tokens.txt 于 $modelDir")
        val dataDir = findDataDir(modelDir)
        val lexiconFiles = findFiles(modelDir) {
            it.isFile && it.name.startsWith("lexicon", ignoreCase = true) && it.name.endsWith(".txt", ignoreCase = true)
        }
        val ruleFstsFiles = findFiles(modelDir) {
            it.isFile && it.name.endsWith(".fst", ignoreCase = true)
        }
        val dictDir = findDictDir(modelDir)

        val kokoroConfig = OfflineTtsKokoroModelConfig(
            model = modelFile.absolutePath,
            voices = voicesFile.absolutePath,
            tokens = tokensFile.absolutePath,
            dataDir = dataDir?.absolutePath ?: "",
            lexicon = lexiconFiles.joinToString(",") { it.absolutePath },
            dictDir = dictDir?.absolutePath ?: "",
            lengthScale = 1.0f,
        )

        val modelConfig = OfflineTtsModelConfig(
            kokoro = kokoroConfig,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )

        val config = OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = ruleFstsFiles.joinToString(",") { it.absolutePath },
            maxNumSentences = 1,
        )

        val instance = OfflineTts(config)
        tts = instance
        try {
            if (cancelled) throw kotlinx.coroutines.CancellationException("合成已取消")

            val audio = instance.generate(text, sid = voiceId.coerceIn(0, 52), speed = 1.0f)

            if (cancelled) throw kotlinx.coroutines.CancellationException("合成已取消")

            LocalSynthesisResult(
                audioData = floatArrayToWav(audio.samples, audio.sampleRate),
                sampleRate = audio.sampleRate,
                format = AudioFormat.WAV,
            )
        } finally {
            instance.free()
            tts = null
        }
    }

    override fun cancel() {
        cancelled = true
        // 注意：Sherpa-ONNX JNI 的 generate() 是同步阻塞调用，无法中途取消。
        // 我们通过设置 cancelled 标志让协程在返回后丢弃结果。
    }

    // ─── Sherpa-ONNX JNI 合成 ─────────────────────────────────────

    private suspend fun synthesizeViaSherpaOnnx(
        text: String,
        provider: LocalTTSProvider,
        voiceId: Int,
        format: AudioFormat,
    ): LocalSynthesisResult = withContext(Dispatchers.IO) {
        // 确保原生库已加载
        NativeLibLoader.load()

        val modelDir = getModelDir(provider)
        val modelFile = findFile(modelDir, ".onnx")
            ?: throw IllegalStateException("找不到 ONNX 模型文件（.onnx）于 $modelDir")
        val tokensFile = findFile(modelDir, "tokens.txt")
            ?: throw IllegalStateException("找不到 tokens.txt 于 $modelDir")
        val dataDir = findDataDir(modelDir)
        val lexiconFile = findFile(modelDir, "lexicon.txt")
        val ruleFstsFile = findFile(modelDir, "rule.fst")

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelFile.absolutePath,
            lexicon = lexiconFile?.absolutePath ?: "",
            tokens = tokensFile.absolutePath,
            dataDir = dataDir?.absolutePath ?: "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )

        val modelConfig = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )

        val config = OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = ruleFstsFile?.absolutePath ?: "",
            maxNumSentences = 1,
        )

        val instance = OfflineTts(config)
        tts = instance
        try {
            if (cancelled) throw kotlinx.coroutines.CancellationException("合成已取消")

            val audio = instance.generate(text, sid = voiceId, speed = 1.0f)

            if (cancelled) throw kotlinx.coroutines.CancellationException("合成已取消")

            val wavBytes = floatArrayToWav(audio.samples, audio.sampleRate)

            LocalSynthesisResult(
                audioData = wavBytes,
                sampleRate = audio.sampleRate,
                format = AudioFormat.WAV,
            )
        } finally {
            instance.free()
            tts = null
        }
    }

    // ─── VibeVoice REST API 合成 ─────────────────────────────────

    private suspend fun synthesizeViaVibeVoice(
        text: String,
        provider: LocalTTSProvider,
        format: AudioFormat,
    ): LocalSynthesisResult = withContext(Dispatchers.IO) {
        val baseUrl = "http://localhost:3000"
        val url = URL("$baseUrl/tts")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 60_000
        connection.readTimeout = 120_000

        // JSON payload
        val jsonBody = """{"text":${escapeJson(text)},"speaker":"Wayne","cfg_scale":1.5}"""

        try {
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw RuntimeException("VibeVoice 请求失败 (HTTP $responseCode): $errorBody")
            }

            val audioData = connection.inputStream.use { it.readBytes() }

            LocalSynthesisResult(
                audioData = audioData,
                sampleRate = 24000,
                format = format,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    // ─── 模型文件查找 ────────────────────────────────────────────

    private fun getModelDir(provider: LocalTTSProvider): String {
        return modelRepository.modelPath(provider)
    }

    private fun findFile(directory: String, suffix: String): PlatformFile? {
        val dir = PlatformFile(directory)
        if (!dir.exists()) return null
        return dir.walkTopDown().firstOrNull {
            it.isFile && it.name.endsWith(suffix, ignoreCase = true)
        }
    }

    private fun findFiles(directory: String, predicate: (PlatformFile) -> Boolean): List<PlatformFile> {
        val dir = PlatformFile(directory)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter(predicate).toList()
    }

    private fun findDataDir(modelDir: String): PlatformFile? {
        // 查找 espeak-ng-data 或其他数据目录
        val dir = PlatformFile(modelDir)
        if (!dir.exists()) return null
        return dir.walkTopDown().firstOrNull {
            it.isDirectory &&
                (it.name.contains("espeak", ignoreCase = true) ||
                 it.name.contains("data", ignoreCase = true))
        }
    }

    private fun findDictDir(modelDir: String): PlatformFile? {
        val dir = PlatformFile(modelDir)
        if (!dir.exists()) return null
        return dir.walkTopDown().firstOrNull {
            it.isDirectory && it.name.contains("dict", ignoreCase = true)
        }
    }

    // ─── 音频转换 ────────────────────────────────────────────────

    /**
     * 将 FloatArray 音频采样转换为 WAV 格式字节数组。
     * Sherpa-ONNX 输出样本范围 [-1.0, 1.0]，转为 16-bit PCM。
     */
    private fun floatArrayToWav(samples: FloatArray, sampleRate: Int): ByteArray {
        val numSamples = samples.size
        val bitsPerSample = 16
        val numChannels = 1
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * blockAlign
        val fileSize = 36 + dataSize

        val bos = ByteArrayOutputStream(fileSize + 8)

        fun writeIntLE(value: Int) {
            bos.write(value and 0xFF)
            bos.write((value shr 8) and 0xFF)
            bos.write((value shr 16) and 0xFF)
            bos.write((value shr 24) and 0xFF)
        }

        fun writeShortLE(value: Int) {
            bos.write(value and 0xFF)
            bos.write((value shr 8) and 0xFF)
        }

        // RIFF header
        bos.write("RIFF".toByteArray())
        writeIntLE(fileSize)
        bos.write("WAVE".toByteArray())

        // fmt subchunk
        bos.write("fmt ".toByteArray())
        writeIntLE(16) // subchunk size (PCM = 16)
        writeShortLE(1) // audio format (PCM = 1)
        writeShortLE(numChannels)
        writeIntLE(sampleRate)
        writeIntLE(byteRate)
        writeShortLE(blockAlign)
        writeShortLE(bitsPerSample)

        // data subchunk
        bos.write("data".toByteArray())
        writeIntLE(dataSize)

        // PCM samples
        for (sample in samples) {
            val clamped = sample.coerceIn(-1f, 1f)
            val pcm = (clamped * 32767).toInt()
            writeShortLE(pcm)
        }

        return bos.toByteArray()
    }
}
