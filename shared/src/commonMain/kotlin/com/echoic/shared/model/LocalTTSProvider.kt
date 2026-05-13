package com.echoic.shared.model

import com.echoic.shared.platform.platformCurrentOSName

/**
 * Represents a download mirror for TTS models.
 *
 * @param name Display name of the mirror (e.g., "HuggingFace", "hf-mirror.com", "GitHub")
 * @param url Direct URL to download or browse the model
 * @param isDefault Whether this mirror should be used as the default download source
 */
data class DownloadMirror(
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
)

/**
 * Local TTS providers that run entirely on-device without requiring API keys or internet.
 */
enum class LocalTTSProvider(
    val displayName: String,
    val subtitle: String,
    val downloadURL: String?,
    val githubURL: String?,
    val documentationURL: String?,
    val tags: List<TTSTag>,
    val modelSizeMB: Int?,
    val supportedLanguages: List<String>,
    val integrationMethod: String,
    val notes: String,
    val platformSupport: List<Platform>,
    val downloadMirrors: List<DownloadMirror> = emptyList(),
    val requiresGPU: Boolean = false,
    val minVRAM: Int? = null, // GB
) {
    KOKORO(
        displayName = "Kokoro",
        subtitle = "Lightweight StyleTTS2-based TTS (82M params, ONNX)",
        downloadURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        githubURL = "https://github.com/hexgrad/kokoro",
        documentationURL = "https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.LIGHTWEIGHT,
            TTSTag.FAST,
            TTSTag.MULTILINGUAL,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 316,
        supportedLanguages = listOf("en", "zh", "ja", "ko", "fr", "de", "es", "it", "pt", "ru"),
        integrationMethod = "Sherpa-ONNX JNI",
        notes = "StyleTTS2-based, ~82M params, Apache-2.0. ONNX 格式通过 Sherpa-ONNX 加载。支持中英日韩法德西意葡俄等语言。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
        ),
        downloadMirrors = listOf(
            DownloadMirror(
                name = "GitHub Releases (Recommended)",
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.gz",
                isDefault = true,
            ),
            DownloadMirror(
                name = "ghfast.top 加速",
                url = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.gz",
            ),
            DownloadMirror(
                name = "ghproxy 加速",
                url = "https://mirror.ghproxy.com/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.gz",
            ),
        ),
    ),
    SHERPA(
        displayName = "Sherpa-ONNX",
        subtitle = "Cross-platform speech toolkit with Java bindings",
        downloadURL = "https://github.com/k2-fsa/sherpa-onnx/releases",
        githubURL = "https://github.com/k2-fsa/sherpa-onnx",
        documentationURL = "https://k2-fsa.github.io/sherpa/onnx/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.LIGHTWEIGHT,
            TTSTag.FAST,
            TTSTag.MULTILINGUAL,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 116,
        supportedLanguages = listOf("en", "zh", "de", "fr", "es", "ja", "ko", "ru"),
        integrationMethod = "JNI Java bindings (official)",
        notes = "Official Java/Kotlin bindings via JNI. Supports multiple TTS models (VITS, Piper, etc.). Best option for JVM/KMP integration. Also supports ASR and speaker identification.",
        platformSupport = listOf(
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
            Platform.WINDOWS_X64,
            Platform.WINDOWS_ARM64,
            Platform.LINUX_X64,
            Platform.LINUX_ARM64,
            Platform.LINUX_ARMV7,
            Platform.ANDROID_ARM64,
            Platform.ANDROID_ARMV7,
            Platform.IOS_ARM64,
            Platform.WEB_WASM,
        ),
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-unity",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/csukuangfj/vits-zh-hf-fanchen-unity",
            ),
            DownloadMirror(
                name = "hf-mirror (MeloTTS Chinese-English)",
                url = "https://hf-mirror.com/csukuangfj/vits-melo-tts-zh_en",
            ),
            DownloadMirror(
                name = "hf-mirror (Piper Chinese Medium)",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx",
            ),
        ),
    ),
    VOXCPM(
        displayName = "VoxCPM",
        subtitle = "Tokenizer-free TTS with voice cloning (OpenBMB)",
        downloadURL = "https://huggingface.co/openbmb/VoxCPM2",
        githubURL = "https://github.com/OpenBMB/VoxCPM",
        documentationURL = "https://voxcpm.readthedocs.io/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.VOICE_CLONING,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 4700,
        supportedLanguages = listOf("zh", "en", "ja", "ko", "de", "fr", "es", "it", "ru", "ar", "hi", "th", "vi", "id"),
        integrationMethod = "Python API or REST API",
        notes = "VoxCPM2: 2B参数模型，支持30种语言，可从自然语言描述创建新声音，支持声音克隆。Apache-2.0开源协议。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
        ),
        requiresGPU = true,
        minVRAM = 8,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/openbmb/VoxCPM2/resolve/main/model.safetensors",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/openbmb/VoxCPM2/resolve/main/model.safetensors",
            ),
        ),
    ),
    VIBEVOICE(
        displayName = "VibeVoice",
        subtitle = "Microsoft Realtime TTS (500M params)",
        downloadURL = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
        githubURL = "https://github.com/microsoft/VibeVoice",
        documentationURL = "https://github.com/microsoft/VibeVoice/blob/main/docs/vibevoice-tts.md",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 1500,
        supportedLanguages = listOf("en", "zh"),
        integrationMethod = "REST API (localhost:3000)",
        notes = "微软开源的实时流式 TTS，支持中英双语，延迟约 300ms。需要启动本地 Python 服务。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
        ),
        requiresGPU = true,
        minVRAM = 4,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
            ),
        ),
    ),
    ;

    val requiresAPIKey: Boolean get() = false
    val isLocal: Boolean get() = true

    /** Check if this provider has a specific tag. */
    fun hasTag(tag: TTSTag): Boolean = tag in tags

    /** Check if this provider has all of the given tags. */
    fun hasAllTags(vararg tagList: TTSTag): Boolean = tagList.all { it in tags }

    /** Check if this provider has any of the given tags. */
    fun hasAnyTag(vararg tagList: TTSTag): Boolean = tagList.any { it in tags }

    /** Check if this provider supports a given language code. */
    fun supportsLanguage(languageCode: String): Boolean =
        languageCode in supportedLanguages

    /** Available local models for this provider. */
    val availableModels: List<LocalTTSModel>
        get() = LocalTTSModel.entries.filter { it.provider == this }

    companion object {
        /** Detect the current operating system at runtime. */
        val currentOS: Platform.OS
            get() {
                val osName = platformCurrentOSName()
                return when {
                    osName == "macos" -> Platform.OS.MACOS
                    osName == "windows" -> Platform.OS.WINDOWS
                    osName == "linux" -> Platform.OS.LINUX
                    else -> Platform.OS.MACOS
                }
            }

        /** Only return providers that support the current platform. */
        val supportedEntries: List<LocalTTSProvider>
            get() = values().toList().filter { provider ->
                provider.platformSupport.any { it.os == currentOS }
            }
    }
}
