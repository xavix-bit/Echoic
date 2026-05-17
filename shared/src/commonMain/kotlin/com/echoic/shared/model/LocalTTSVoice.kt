package com.echoic.shared.model

data class LocalTTSVoice(
    val id: Int,
    val name: String,
    val language: String,
) {
    val displayName: String
        get() = "$name · $language"
}

val LocalTTSProvider.availableVoices: List<LocalTTSVoice>
    get() = when (this) {
        LocalTTSProvider.KOKORO -> kokoroVoices
        else -> emptyList()
    }

private val kokoroVoices = listOf(
    LocalTTSVoice(0, "af_alloy", "US English"),
    LocalTTSVoice(1, "af_aoede", "US English"),
    LocalTTSVoice(2, "af_bella", "US English"),
    LocalTTSVoice(3, "af_heart", "US English"),
    LocalTTSVoice(4, "af_jessica", "US English"),
    LocalTTSVoice(5, "af_kore", "US English"),
    LocalTTSVoice(6, "af_nicole", "US English"),
    LocalTTSVoice(7, "af_nova", "US English"),
    LocalTTSVoice(8, "af_river", "US English"),
    LocalTTSVoice(9, "af_sarah", "US English"),
    LocalTTSVoice(10, "af_sky", "US English"),
    LocalTTSVoice(11, "am_adam", "US English"),
    LocalTTSVoice(12, "am_echo", "US English"),
    LocalTTSVoice(13, "am_eric", "US English"),
    LocalTTSVoice(14, "am_fenrir", "US English"),
    LocalTTSVoice(15, "am_liam", "US English"),
    LocalTTSVoice(16, "am_michael", "US English"),
    LocalTTSVoice(17, "am_onyx", "US English"),
    LocalTTSVoice(18, "am_puck", "US English"),
    LocalTTSVoice(19, "am_santa", "US English"),
    LocalTTSVoice(20, "bf_alice", "British English"),
    LocalTTSVoice(21, "bf_emma", "British English"),
    LocalTTSVoice(22, "bf_isabella", "British English"),
    LocalTTSVoice(23, "bf_lily", "British English"),
    LocalTTSVoice(24, "bm_daniel", "British English"),
    LocalTTSVoice(25, "bm_fable", "British English"),
    LocalTTSVoice(26, "bm_george", "British English"),
    LocalTTSVoice(27, "bm_lewis", "British English"),
    LocalTTSVoice(28, "ef_dora", "Spanish"),
    LocalTTSVoice(29, "em_alex", "Spanish"),
    LocalTTSVoice(30, "ff_siwis", "French"),
    LocalTTSVoice(31, "hf_alpha", "Hindi"),
    LocalTTSVoice(32, "hf_beta", "Hindi"),
    LocalTTSVoice(33, "hm_omega", "Hindi"),
    LocalTTSVoice(34, "hm_psi", "Hindi"),
    LocalTTSVoice(35, "if_sara", "Italian"),
    LocalTTSVoice(36, "im_nicola", "Italian"),
    LocalTTSVoice(37, "jf_alpha", "Japanese"),
    LocalTTSVoice(38, "jf_gongitsune", "Japanese"),
    LocalTTSVoice(39, "jf_nezumi", "Japanese"),
    LocalTTSVoice(40, "jf_tebukuro", "Japanese"),
    LocalTTSVoice(41, "jm_kumo", "Japanese"),
    LocalTTSVoice(42, "pf_dora", "Portuguese"),
    LocalTTSVoice(43, "pm_alex", "Portuguese"),
    LocalTTSVoice(44, "pm_santa", "Portuguese"),
    LocalTTSVoice(45, "zf_xiaobei", "Chinese"),
    LocalTTSVoice(46, "zf_xiaoni", "Chinese"),
    LocalTTSVoice(47, "zf_xiaoxiao", "Chinese"),
    LocalTTSVoice(48, "zf_xiaoyi", "Chinese"),
    LocalTTSVoice(49, "zm_yunjian", "Chinese"),
    LocalTTSVoice(50, "zm_yunxi", "Chinese"),
    LocalTTSVoice(51, "zm_yunxia", "Chinese"),
    LocalTTSVoice(52, "zm_yunyang", "Chinese"),
)
