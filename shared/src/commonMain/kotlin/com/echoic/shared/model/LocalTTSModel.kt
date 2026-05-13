package com.echoic.shared.model

/**
 * Local TTS models that can run on-device.
 * Each model is associated with a LocalTTSProvider.
 */
enum class LocalTTSModel(
    val rawValue: String,
    val displayName: String,
    val provider: LocalTTSProvider,
    val language: String,
    val modelSizeMB: Int?,
    val tags: List<TTSTag>,
    val modelURL: String?,
    val notes: String,
) {
    // Kokoro models
    KOKORO_V1(
        rawValue = "kokoro-v1_0",
        displayName = "Kokoro v1.0 (82M)",
        provider = LocalTTSProvider.KOKORO,
        language = "multi",
        modelSizeMB = 316,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.FAST, TTSTag.LIGHTWEIGHT, TTSTag.NEURAL),
        modelURL = "https://huggingface.co/hexgrad/Kokoro-82M",
        notes = "StyleTTS2-based model. Supports English, Chinese, Japanese. Very fast inference, ~82M params.",
    ),

    // Sherpa-ONNX models
    SHERPA_VITS_ZH(
        rawValue = "vits-zh-aishell3",
        displayName = "VITS Chinese (AISHELL3)",
        provider = LocalTTSProvider.SHERPA,
        language = "zh",
        modelSizeMB = 116,
        tags = listOf(TTSTag.CHINESE, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Chinese TTS model optimized for Sherpa-ONNX. Fast inference.",
    ),
    SHERPA_VITS_EN(
        rawValue = "vits-en-vctk",
        displayName = "VITS English (VCTK)",
        provider = LocalTTSProvider.SHERPA,
        language = "en",
        modelSizeMB = 100,
        tags = listOf(TTSTag.ENGLISH, TTSTag.MULTILINGUAL, TTSTag.FAST),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Multi-speaker English TTS via Sherpa-ONNX.",
    ),

    // VoxCPM models
    VOXCPM2(
        rawValue = "voxcpm2",
        displayName = "VoxCPM2 (2B)",
        provider = LocalTTSProvider.VOXCPM,
        language = "multi",
        modelSizeMB = 4000,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.VOICE_CLONING, TTSTag.NEURAL),
        modelURL = "https://huggingface.co/openbmb/VoxCPM2",
        notes = "VoxCPM2 2B参数模型，支持30种语言，声音克隆，声音设计。需要约8GB显存。",
    ),

    // VibeVoice models
    VIBEVOICE_REALTIME(
        rawValue = "vibevoice-realtime-0.5b",
        displayName = "VibeVoice Realtime (500M)",
        provider = LocalTTSProvider.VIBEVOICE,
        language = "multi",
        modelSizeMB = 1500,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.NEURAL),
        modelURL = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
        notes = "微软实时流式 TTS 模型，支持中英双语，延迟约 300ms。",
    ),
    ;

    /** Get the language display name. */
    val languageDisplayName: String
        get() = when (language) {
            "en" -> "English"
            "zh" -> "Chinese (Mandarin)"
            "de" -> "German"
            "fr" -> "French"
            "es" -> "Spanish"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            "sv" -> "Swedish"
            "te" -> "Telugu"
            "fi" -> "Finnish"
            "multi" -> "Multilingual"
            else -> language
        }

    /** Check if this model has a specific tag. */
    fun hasTag(tag: TTSTag): Boolean = tag in tags

    /** Check if this model has all of the given tags. */
    fun hasAllTags(vararg tagList: TTSTag): Boolean = tagList.all { it in tags }

    /** Check if this model has any of the given tags. */
    fun hasAnyTag(vararg tagList: TTSTag): Boolean = tagList.any { it in tags }
}
