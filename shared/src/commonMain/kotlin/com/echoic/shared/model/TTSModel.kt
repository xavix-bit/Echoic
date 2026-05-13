package com.echoic.shared.model

enum class TTSModel(
    val rawValue: String,
    val displayName: String,
    val provider: TTSProvider,
) {
    // OpenAI
    OPENAI_TTS_1("tts-1", "TTS-1", TTSProvider.OPENAI),
    OPENAI_TTS_1_HD("tts-1-hd", "TTS-1 HD", TTSProvider.OPENAI),

    // Google Cloud
    GOOGLE_WAVENET("en-US-Wavenet-D", "WaveNet", TTSProvider.GOOGLE),
    GOOGLE_NEURAL2("en-US-Neural2-D", "Neural2", TTSProvider.GOOGLE),
    GOOGLE_STANDARD("en-US-Standard-D", "Standard", TTSProvider.GOOGLE),

    // Azure
    AZURE_NEURAL("en-US-JennyNeural", "Neural", TTSProvider.AZURE),
    AZURE_STANDARD("en-US-JennyStandard", "Standard", TTSProvider.AZURE),

    // ElevenLabs
    ELEVENLABS_MULTILINGUAL("eleven_multilingual_v2", "Multilingual v2", TTSProvider.ELEVENLABS),
    ELEVENLABS_TURBO("eleven_turbo_v2", "Turbo v2", TTSProvider.ELEVENLABS),
    ELEVENLABS_MONO("eleven_monolingual_v1", "Monolingual v1", TTSProvider.ELEVENLABS),

    // Baidu
    BAIDU_STANDARD("0", "Standard", TTSProvider.BAIDU),
    BAIDU_HIGH("1", "High Quality", TTSProvider.BAIDU),

    // Tencent
    TENCENT_STANDARD("101001", "Standard", TTSProvider.TENCENT),
    TENCENT_HIGH("101003", "High Quality", TTSProvider.TENCENT),

    // Aliyun
    ALIYUN_STANDARD("xiaoyun", "Standard", TTSProvider.ALIYUN),
    ALIYUN_HIGH("xiaogang", "High Quality", TTSProvider.ALIYUN),

    // Fish Audio
    FISH_TTS1("fish-tts1", "Fish TTS v1", TTSProvider.FISH_AUDIO),

    // MiniMax
    MINIMAX_SPEECH01("speech-01", "Speech-01", TTSProvider.MINIMAX),
    MINIMAX_SPEECH01_2025("speech-01-2025", "Speech-01 2025", TTSProvider.MINIMAX),

    // Zhipu AI (GLM)
    ZHIPU_TTS1("tts-1", "TTS-1", TTSProvider.ZHIPU),
    ZHIPU_TTS1_HD("tts-1-hd", "TTS-1 HD", TTSProvider.ZHIPU),

    // Volcano Engine (火山引擎)
    VOLCENGINE_TTS("volcano-tts", "Volcano TTS", TTSProvider.VOLCENGINE),
    VOLCENGINE_MEGA("volcano-mega", "Volcano Mega", TTSProvider.VOLCENGINE),

    // Edge TTS
    EDGETTS_DEFAULT("edge-tts", "Edge TTS", TTSProvider.EDGETTS),
}
