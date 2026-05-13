package com.echoic.shared.model

enum class TTSProvider(
    val displayName: String,
    val subtitle: String,
    val defaultBaseURL: String,
    val helpURL: String?,
    val websiteURL: String?,
    val apiKeyPlaceholder: String,
    val apiKeyDescription: String,
    val tags: List<TTSTag>,
    val requiresAPIKey: Boolean = true,
) {
    OPENAI(
        displayName = "OpenAI",
        subtitle = "High-quality TTS with natural voices",
        defaultBaseURL = "https://api.openai.com",
        helpURL = "https://platform.openai.com/api-keys",
        websiteURL = "https://openai.com",
        apiKeyPlaceholder = "sk-...",
        apiKeyDescription = "OpenAI API key from platform.openai.com",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.ENGLISH, TTSTag.MULTILINGUAL, TTSTag.FAST, TTSTag.INTERNATIONAL),
    ),
    GOOGLE(
        displayName = "Google Cloud",
        subtitle = "WaveNet & Neural2 voices",
        defaultBaseURL = "https://texttospeech.googleapis.com",
        helpURL = "https://console.cloud.google.com/apis/credentials",
        websiteURL = "https://cloud.google.com/text-to-speech",
        apiKeyPlaceholder = "Your Google Cloud API key",
        apiKeyDescription = "Google Cloud API key with TTS enabled",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.MULTILINGUAL, TTSTag.FREE, TTSTag.INTERNATIONAL),
    ),
    AZURE(
        displayName = "Azure TTS",
        subtitle = "Microsoft Neural voices",
        defaultBaseURL = "https://{region}.tts.speech.microsoft.com",
        helpURL = "https://portal.azure.com/#blade/Microsoft_Azure_ProjectOxford/CognitiveServicesHub/Overview",
        websiteURL = "https://azure.microsoft.com/en-us/products/ai-services/text-to-speech",
        apiKeyPlaceholder = "Your Azure Speech key",
        apiKeyDescription = "Azure Cognitive Services Speech key",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.MULTILINGUAL, TTSTag.FREE, TTSTag.INTERNATIONAL),
    ),
    ELEVENLABS(
        displayName = "ElevenLabs",
        subtitle = "Ultra-realistic AI voices",
        defaultBaseURL = "https://api.elevenlabs.io",
        helpURL = "https://elevenlabs.io/developers",
        websiteURL = "https://elevenlabs.io",
        apiKeyPlaceholder = "Your ElevenLabs API key",
        apiKeyDescription = "ElevenLabs API key from elevenlabs.io",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.ENGLISH, TTSTag.VOICE_CLONING, TTSTag.FAST, TTSTag.INTERNATIONAL),
    ),
    BAIDU(
        displayName = "Baidu TTS",
        subtitle = "Baidu speech synthesis",
        defaultBaseURL = "https://tsn.baidu.com",
        helpURL = "https://ai.baidu.com/tech/speech",
        websiteURL = "https://ai.baidu.com",
        apiKeyPlaceholder = "Your Baidu API key",
        apiKeyDescription = "Baidu AI platform API key",
        tags = listOf(TTSTag.CHINESE, TTSTag.FREE, TTSTag.LOCAL, TTSTag.FAST),
    ),
    TENCENT(
        displayName = "Tencent TTS",
        subtitle = "Tencent Cloud speech synthesis",
        defaultBaseURL = "https://tts.tencentcloudapi.com",
        helpURL = "https://console.cloud.tencent.com/cam/capi",
        websiteURL = "https://cloud.tencent.com/product/tts",
        apiKeyPlaceholder = "Your Tencent Cloud SecretId",
        apiKeyDescription = "Tencent Cloud API SecretId",
        tags = listOf(TTSTag.CHINESE, TTSTag.FREE, TTSTag.LOCAL),
    ),
    ALIYUN(
        displayName = "Aliyun TTS",
        subtitle = "Alibaba Cloud speech synthesis",
        defaultBaseURL = "https://nls-gateway.cn-shanghai.aliyuncs.com",
        helpURL = "https://nls-portal.console.aliyun.com/",
        websiteURL = "https://ai.aliyun.com/nls",
        apiKeyPlaceholder = "Your Aliyun NLS key",
        apiKeyDescription = "Alibaba Cloud NLS API key",
        tags = listOf(TTSTag.CHINESE, TTSTag.FREE, TTSTag.LOCAL, TTSTag.HIGH_QUALITY),
    ),
    FISH_AUDIO(
        displayName = "Fish Audio",
        subtitle = "High-quality voice synthesis",
        defaultBaseURL = "https://api.fish.audio",
        helpURL = "https://fish.audio/settings/api",
        websiteURL = "https://fish.audio",
        apiKeyPlaceholder = "Your Fish Audio API key",
        apiKeyDescription = "Fish Audio API key from fish.audio",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.CHINESE, TTSTag.LOCAL, TTSTag.FAST),
    ),
    MINIMAX(
        displayName = "MiniMax",
        subtitle = "MiniMax Speech-01 high-quality TTS",
        defaultBaseURL = "https://api.minimax.chat",
        helpURL = "https://platform.minimaxi.com/document/guides-get-api-key",
        websiteURL = "https://www.minimaxi.com",
        apiKeyPlaceholder = "Your MiniMax API key",
        apiKeyDescription = "MiniMax API key from platform.minimaxi.com",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.CHINESE, TTSTag.FAST, TTSTag.LOCAL),
    ),
    ZHIPU(
        displayName = "Zhipu AI (GLM)",
        subtitle = "Zhipu GLM TTS (OpenAI-compatible)",
        defaultBaseURL = "https://open.bigmodel.cn",
        helpURL = "https://open.bigmodel.cn/usercenter/apikeys",
        websiteURL = "https://open.bigmodel.cn",
        apiKeyPlaceholder = "Your Zhipu API key",
        apiKeyDescription = "Zhipu AI API key from open.bigmodel.cn",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.CHINESE, TTSTag.FAST, TTSTag.LOCAL),
    ),
    VOLCENGINE(
        displayName = "Volcano Engine (火山引擎)",
        subtitle = "ByteDance Volcano Engine TTS",
        defaultBaseURL = "https://openspeech.bytedance.com",
        helpURL = "https://console.volcengine.com/speech/app",
        websiteURL = "https://www.volcengine.com/product/tts",
        apiKeyPlaceholder = "appid;access_token",
        apiKeyDescription = "Volcano Engine appid and access token (format: appid;token)",
        tags = listOf(TTSTag.HIGH_QUALITY, TTSTag.CHINESE, TTSTag.FAST, TTSTag.LOCAL, TTSTag.MULTILINGUAL),
    ),
    EDGETTS(
        displayName = "Edge TTS",
        subtitle = "Free Microsoft Edge voices (No API Key)",
        defaultBaseURL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1",
        helpURL = null,
        websiteURL = "https://github.com/rany2/edge-tts",
        apiKeyPlaceholder = "Not required",
        apiKeyDescription = "Edge TTS is free and does not require an API key.",
        tags = listOf(TTSTag.FREE, TTSTag.CHINESE, TTSTag.ENGLISH, TTSTag.FAST, TTSTag.MULTILINGUAL),
        requiresAPIKey = false,
    );

    val apiKeyStorageKey: String get() = "${name.lowercase()}_api_key"
    val baseURLStorageKey: String get() = "${name.lowercase()}_base_url"

    val availableModels: List<TTSModel>
        get() = TTSModel.entries.filter { it.provider == this }

    val availableVoices: List<Voice>
        get() = Voice.entries.filter { it.provider == this }

    /** Check if this provider has a specific tag. */
    fun hasTag(tag: TTSTag): Boolean = tag in tags

    /** Check if this provider has all of the given tags. */
    fun hasAllTags(vararg tagList: TTSTag): Boolean = tagList.all { it in tags }

    /** Check if this provider has any of the given tags. */
    fun hasAnyTag(vararg tagList: TTSTag): Boolean = tagList.any { it in tags }
}
