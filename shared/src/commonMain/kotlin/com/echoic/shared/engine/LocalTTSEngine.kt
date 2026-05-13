package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalTTSProvider

/**
 * 本地 TTS 引擎接口。
 * 每个平台提供各自的实现（桌面端用 CLI/库，移动端可能用其他方案）。
 */
interface LocalTTSEngine {
    /**
     * 检查该引擎是否可用（必要的运行时依赖是否已安装）。
     */
    fun isAvailable(): Boolean

    /**
     * 检查是否支持给定的 provider。
     */
    fun supports(provider: LocalTTSProvider): Boolean

    /**
     * 将文本合成为音频。
     * @param text 输入文本
     * @param provider 本地 TTS provider
     * @param format 期望的输出格式
     * @return 音频数据（PCM/WAV 等格式的字节数组）
     */
    suspend fun synthesize(
        text: String,
        provider: LocalTTSProvider,
        format: AudioFormat = AudioFormat.WAV,
    ): LocalSynthesisResult

    /** 取消正在进行的合成 */
    fun cancel()
}

data class LocalSynthesisResult(
    val audioData: ByteArray,
    val sampleRate: Int,
    val format: AudioFormat,
)
