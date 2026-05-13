package com.echoic.shared

import com.k2fsa.sherpa.onnx.*
import com.echoic.shared.engine.NativeLibLoader
import org.junit.Test

class SherpaJniTest {
    @Test
    fun testCreateOfflineTts() {
        // 加载原生库
        NativeLibLoader.load()
        println("Native libs loaded successfully")

        val modelDir = "${System.getProperty("user.home")}/.echoic/models/sherpa"
        val modelFile = "$modelDir/vits-zh-hf-fanchen-unity.onnx"
        val tokensFile = "$modelDir/tokens.txt"
        val lexiconFile = "$modelDir/lexicon.txt"

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelFile,
            lexicon = lexiconFile,
            tokens = tokensFile,
            dataDir = "",
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
            ruleFsts = "",
            maxNumSentences = 1,
        )

        println("Creating OfflineTts...")
        val tts = OfflineTts(config)
        println("OfflineTts created! sampleRate=${tts.sampleRate()}, numSpeakers=${tts.numSpeakers()}")

        println("Generating speech...")
        val audio = tts.generate("你好世界", sid = 0, speed = 1.0f)
        println("Generated: ${audio.samples.size} samples at ${audio.sampleRate}Hz")

        tts.free()
        println("Done!")
    }
}
