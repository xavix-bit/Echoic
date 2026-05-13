package com.echoic.shared

import com.k2fsa.sherpa.onnx.*
import com.echoic.shared.engine.NativeLibLoader
import org.junit.Test
import java.io.ByteArrayOutputStream

class SherpaCompareTest {
    @Test
    fun compareWavOutputs() {
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
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 1,
        )

        val tts = OfflineTts(config)
        val audio = tts.generate("你好世界", sid = 0, speed = 1.0f)

        println("samples=${audio.samples.size}, sampleRate=${audio.sampleRate}")

        // Method 1: Native save
        val nativeSaved = audio.save("/tmp/sherpa_native.wav")

        // Method 2: Kotlin floatArrayToWav (same as DesktopLocalEngine)
        val kotlinWav = floatArrayToWav(audio.samples, audio.sampleRate)
        java.io.File("/tmp/sherpa_kotlin.wav").writeBytes(kotlinWav)

        val nativeFile = java.io.File("/tmp/sherpa_native.wav")
        val nativeBytes = nativeFile.readBytes()
        println("Native save: $nativeSaved, size=${nativeBytes.size}")
        println("Kotlin WAV: size=${kotlinWav.size}")

        // Compare headers
        val nativeHeader = nativeBytes.take(44).joinToString(" ") { "%02x".format(it) }
        val kotlinHeader = kotlinWav.take(44).joinToString(" ") { "%02x".format(it) }
        println("Native header: $nativeHeader")
        println("Kotlin header: $kotlinHeader")

        // Compare data
        val nativeData = nativeBytes.drop(44).toByteArray()
        val kotlinData = kotlinWav.drop(44).toByteArray()
        println("Native data: ${nativeData.size} bytes")
        println("Kotlin data: ${kotlinData.size} bytes")
        if (nativeData.contentEquals(kotlinData)) {
            println("Audio data MATCHES!")
        } else {
            println("Audio data DIFFERS!")
            // Find first difference
            for (i in nativeData.indices) {
                if (nativeData[i] != kotlinData.getOrElse(i) { 0 }) {
                    println("First diff at byte $i: native=${"%02x".format(nativeData[i])} kotlin=${"%02x".format(kotlinData.getOrElse(i) { 0 })}")
                    break
                }
            }
        }

        tts.free()
    }

    private fun floatArrayToWav(samples: FloatArray, sampleRate: Int): ByteArray {
        val numSamples = samples.size
        val bitsPerSample = 16
        val numChannels = 1
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * blockAlign
        val fileSize = 36 + dataSize

        val bos = ByteArrayOutputStream(fileSize + 8)

        fun writeIntLE(value: Int) {
            bos.write(value and 0xFF)
            bos.write((value shr 8) and 0xFF)
            bos.write((value shr 16) and 0xFF)
            bos.write((value shr 24) and 0xFF)
        }

        fun writeShortLE(value: Int) {
            bos.write(value and 0xFF)
            bos.write((value shr 8) and 0xFF)
        }

        bos.write("RIFF".toByteArray())
        writeIntLE(fileSize)
        bos.write("WAVE".toByteArray())
        bos.write("fmt ".toByteArray())
        writeIntLE(16)
        writeShortLE(1)
        writeShortLE(numChannels)
        writeIntLE(sampleRate)
        writeIntLE(byteRate)
        writeShortLE(blockAlign)
        writeShortLE(bitsPerSample)
        bos.write("data".toByteArray())
        writeIntLE(dataSize)

        for (sample in samples) {
            val clamped = sample.coerceIn(-1f, 1f)
            val pcm = (clamped * 32767).toInt()
            writeShortLE(pcm)
        }

        return bos.toByteArray()
    }
}
