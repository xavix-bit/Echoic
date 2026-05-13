package com.echoic.shared.model

enum class Voice(
    val id: String,
    val displayName: String,
    val provider: TTSProvider,
) {
    // OpenAI voices
    OPENAI_ALLOY("alloy", "Alloy", TTSProvider.OPENAI),
    OPENAI_ECHO("echo", "Echo", TTSProvider.OPENAI),
    OPENAI_FABLE("fable", "Fable", TTSProvider.OPENAI),
    OPENAI_ONYX("onyx", "Onyx", TTSProvider.OPENAI),
    OPENAI_NOVA("nova", "Nova", TTSProvider.OPENAI),
    OPENAI_SHIMMER("shimmer", "Shimmer", TTSProvider.OPENAI),

    // Google Cloud voices
    GOOGLE_EN_US_D("en-US-Wavenet-D", "English (US) Male", TTSProvider.GOOGLE),
    GOOGLE_EN_US_F("en-US-Wavenet-F", "English (US) Female", TTSProvider.GOOGLE),
    GOOGLE_ZH_CN("cmn-CN-Wavenet-C", "Chinese (Mandarin)", TTSProvider.GOOGLE),

    // Azure voices
    AZURE_JENNY("en-US-JennyNeural", "Jenny", TTSProvider.AZURE),
    AZURE_GUY("en-US-GuyNeural", "Guy", TTSProvider.AZURE),
    AZURE_XIAOXIAO("zh-CN-XiaoxiaoNeural", "Xiaoxiao", TTSProvider.AZURE),

    // ElevenLabs voices
    ELEVENLABS_RACHEL("21m00Tcm4TlvDq8ikWAM", "Rachel", TTSProvider.ELEVENLABS),
    ELEVENLABS_DOMI("AZnzlk1XvdvUeBnXmlld", "Domi", TTSProvider.ELEVENLABS),
    ELEVENLABS_BELLA("EXAVITQu4vr4xnSDxMaL", "Bella", TTSProvider.ELEVENLABS),

    // Baidu voices
    BAIDU_STANDARD("0", "Standard Female", TTSProvider.BAIDU),
    BAIDU_MALE("1", "Standard Male", TTSProvider.BAIDU),
    BAIDU_HIGH("2", "High Quality Female", TTSProvider.BAIDU),

    // Tencent voices
    TENCENT_STANDARD("101001", "Standard Female", TTSProvider.TENCENT),
    TENCENT_MALE("101002", "Standard Male", TTSProvider.TENCENT),
    TENCENT_HIGH("101003", "High Quality Female", TTSProvider.TENCENT),

    // Aliyun voices
    ALIYUN_XIAOYUN("xiaoyun", "Xiaoyun", TTSProvider.ALIYUN),
    ALIYUN_XIAOGANG("xiaogang", "Xiaogang", TTSProvider.ALIYUN),
    ALIYUN_XIAOMEI("xiaomei", "Xiaomei", TTSProvider.ALIYUN),

    // Fish Audio — default voice
    FISH_DEFAULT("Default", "Default", TTSProvider.FISH_AUDIO),

    // MiniMax voices
    MINIMAX_FEMALE_SHAONV("female-shaonv", "少女", TTSProvider.MINIMAX),
    MINIMAX_MALE_QN_QINGSE("male-qn-qingse", "青涩青年", TTSProvider.MINIMAX),
    MINIMAX_FEMALE_YEYU("female-yeyu", "御姐", TTSProvider.MINIMAX),
    MINIMAX_MALE_GAOLENG("male-gaoleng", "高冷青年", TTSProvider.MINIMAX),
    MINIMAX_FEMALE_LAOPOLUO("female-laopoluo", "老婆婆", TTSProvider.MINIMAX),
    MINIMAX_MALE_CHAUNSHU("male-chuanmei", "川妹", TTSProvider.MINIMAX),

    // Zhipu AI (GLM) voices
    ZHIPU_ALLOY("alloy", "Alloy", TTSProvider.ZHIPU),
    ZHIPU_ECHO("echo", "Echo", TTSProvider.ZHIPU),
    ZHIPU_FABLE("fable", "Fable", TTSProvider.ZHIPU),
    ZHIPU_ONYX("onyx", "Onyx", TTSProvider.ZHIPU),
    ZHIPU_NOVA("nova", "Nova", TTSProvider.ZHIPU),
    ZHIPU_SHIMMER("shimmer", "Shimmer", TTSProvider.ZHIPU),

    // Volcano Engine (火山引擎) voices
    VOLCENGINE_BV001_V2("BV001_V2", "通用女声", TTSProvider.VOLCENGINE),
    VOLCENGINE_BV700_V2("BV700_V2", "灿灿", TTSProvider.VOLCENGINE),
    VOLCENGINE_BV705_V2("BV705_V2", "炀炀", TTSProvider.VOLCENGINE),
    VOLCENGINE_BV406_V2("BV406_V2", "擎苍", TTSProvider.VOLCENGINE),
    VOLCENGINE_BV407_V2("BV407_V2", "通用男声", TTSProvider.VOLCENGINE),
    
    // Edge TTS voices
    EDGETTS_XIAOXIAO("zh-CN-XiaoxiaoNeural", "晓晓 (Chinese)", TTSProvider.EDGETTS),
    EDGETTS_YUNXI("zh-CN-YunxiNeural", "云希 (Chinese)", TTSProvider.EDGETTS),
    EDGETTS_JENNY("en-US-JennyNeural", "Jenny (English)", TTSProvider.EDGETTS),
    EDGETTS_GUY("en-US-GuyNeural", "Guy (English)", TTSProvider.EDGETTS),
}
