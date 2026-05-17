package com.echoic.shared

import com.echoic.shared.engine.DesktopLocalEngine
import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalTTSProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KokoroMarkdownInputTest {
    @Test
    fun kokoroGeneratesPlayableWavFromMarkdownInput() = runBlocking {
        val text = """
            # 每日科技速递 - 2026年4月27日

            ## 综合科技

            ### This touchscreen mouse is my over-engineering nightmare
            ⏰ 10:10
            > 📝 Turtle Beach最新推出了一款售价160美元的MC7无线游戏鼠标，其侧面配备了一块2.25英寸的触摸显示屏。这种过度复杂的设计在作者看来是一场“过度设计的噩梦”，极易在游戏使用中引发误触。
            🔗 [原文链接](https://www.theverge.com/gadgets/918919/turtle-beach-mc7-gaming-mouse-touchscreen-command-series)

            ---
        """.trimIndent()

        val result = DesktopLocalEngine().synthesize(
            text = text,
            provider = LocalTTSProvider.KOKORO,
            voiceId = 1,
            format = AudioFormat.WAV,
        )

        assertEquals(AudioFormat.WAV, result.format)
        assertEquals(24000, result.sampleRate)
        assertEquals(24000, readIntLE(result.audioData, 24))
        assertTrue(result.audioData.size > 44)
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
