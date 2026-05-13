package com.echoic.shared

import com.k2fsa.sherpa.onnx.*
import com.echoic.shared.engine.NativeLibLoader
import org.junit.Test
import java.io.File

class SherpaAudioDiagTest {
    @Test
    fun diagnoseAudio() {
        NativeLibLoader.load()

        val modelDir = "${System.getProperty("user.home")}/.echoic/models/sherpa"
        val modelFile = "$modelDir/vits-zh-hf-fanchen-unity.onnx"
        val tokensFile = "$modelDir/tokens.txt"
        val lexiconFile = "$modelDir/lexicon.txt"

        // Check files exist
        for (f in listOf(modelFile, tokensFile, lexiconFile)) {
            val file = File(f)
            println("${if (file.exists()) "OK" else "MISSING"}: $f (${file.length()} bytes)")
        }

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelFile,
            lexicon = lexiconFile,
            tokens = tokensFile,
            dataDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 2,
                debug = true,
                provider = "cpu",
            ),
            ruleFsts = "",
            maxNumSentences = 1,
        )

        val tts = OfflineTts(config)
        println("sampleRate=${tts.sampleRate()}, numSpeakers=${tts.numSpeakers()}")

        val audio = tts.generate("你好世界，这是一个测试。", sid = 0, speed = 1.0f)
        println("samples=${audio.samples.size}, sampleRate=${audio.sampleRate}")
        println("duration=${audio.samples.size.toDouble() / audio.sampleRate}s")

        // Check audio quality
        val max = audio.samples.maxOrNull() ?: 0f
        val min = audio.samples.minOrNull() ?: 0f
        val rms = kotlin.math.sqrt(audio.samples.map { it * it }.sum() / audio.samples.size)
        println("min=$min, max=$max, rms=$rms")

        // Print first 20 samples
        println("first 20 samples: ${audio.samples.take(20).toList()}")
        // Print last 20 samples
        println("last 20 samples: ${audio.samples.takeLast(20).toList()}")

        // Save to file for manual check
        val saved = audio.save("/tmp/sherpa_test_output.wav")
        println("saved=$saved, file size=${File("/tmp/sherpa_test_output.wav").length()} bytes")

        tts.free()
    }
}
