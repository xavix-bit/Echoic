package com.echoic.shared

import com.k2fsa.sherpa.onnx.*
import com.echoic.shared.engine.NativeLibLoader
import org.junit.Test
import java.io.File

class SherpaEnglishTest {
    @Test
    fun testEnglishText() {
        NativeLibLoader.load()

        val modelDir = "${System.getProperty("user.home")}/.echoic/models/sherpa"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$modelDir/vits-zh-hf-fanchen-unity.onnx",
                    lexicon = "$modelDir/lexicon.txt",
                    tokens = "$modelDir/tokens.txt",
                ),
                numThreads = 2,
                debug = true,
                provider = "cpu",
            ),
            maxNumSentences = 1,
        )

        val tts = OfflineTts(config)

        // English text (what the user used)
        val audioEn = tts.generate("Got over nine hours of sleep last night.", sid = 0, speed = 1.0f)
        println("English: ${audioEn.samples.size} samples, ${audioEn.sampleRate}Hz, ${audioEn.samples.size.toDouble()/audioEn.sampleRate}s")
        audioEn.save("/tmp/sherpa_english.wav")

        // Chinese text
        val audioZh = tts.generate("昨晚睡了九个多小时，感觉精神还不错。", sid = 0, speed = 1.0f)
        println("Chinese: ${audioZh.samples.size} samples, ${audioZh.sampleRate}Hz, ${audioZh.samples.size.toDouble()/audioZh.sampleRate}s")
        audioZh.save("/tmp/sherpa_chinese.wav")

        tts.free()

        // Print file sizes
        println("English WAV: ${File("/tmp/sherpa_english.wav").length()} bytes")
        println("Chinese WAV: ${File("/tmp/sherpa_chinese.wav").length()} bytes")
    }
}
