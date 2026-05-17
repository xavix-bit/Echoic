package com.echoic.shared.audio

import com.echoic.shared.model.AudioFormat as ModelAudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat as JavaxAudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.sound.sampled.SourceDataLine

/**
 * Desktop audio player using javax.sound.
 * Supports WAV natively. For MP3/other formats, falls back to basic playback.
 */
actual class AudioPlayer actual constructor() {
    private var clip: Clip? = null
    private var line: SourceDataLine? = null
    private var stream: javax.sound.sampled.AudioInputStream? = null

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTime = MutableStateFlow(0.0)
    actual val currentTime: StateFlow<Double> = _currentTime

    private val _duration = MutableStateFlow(0.0)
    actual val duration: StateFlow<Double> = _duration

    private val _playbackRate = MutableStateFlow(1.0f)
    actual val playbackRate: StateFlow<Float> = _playbackRate

    private var timerThread: Thread? = null

    actual suspend fun play(data: ByteArray, format: ModelAudioFormat) = withContext(Dispatchers.IO) {
        stop()

        try {
            when (format) {
                ModelAudioFormat.WAV -> playWav(data)
                else -> playRaw(data)
            }
            _isPlaying.value = true
            startTimer()
        } catch (e: Exception) {
            _isPlaying.value = false
            throw e
        }
    }

    private fun playWav(data: ByteArray) {
        val bais = ByteArrayInputStream(data)
        val audioStream = AudioSystem.getAudioInputStream(bais)
        val audioFormat = audioStream.format

        val info = DataLine.Info(Clip::class.java, audioFormat)
        val clip = AudioSystem.getLine(info) as Clip
        clip.open(audioStream)
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP && !clip.isRunning) {
                _isPlaying.value = false
                _currentTime.value = 0.0
                stopTimer()
            }
        }
        clip.start()

        this.clip = clip
        this.stream = audioStream
        _duration.value = clip.microsecondLength / 1_000_000.0
    }

    private fun playRaw(data: ByteArray) {
        val bais = ByteArrayInputStream(data)
        try {
            val audioStream = AudioSystem.getAudioInputStream(bais)
            val baseFormat = audioStream.format
            val decodedFormat = JavaxAudioFormat(
                JavaxAudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream)

            val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(decodedFormat)
            line.start()

            this.line = line
            this.stream = decodedStream
            _duration.value = data.size.toDouble() / (decodedFormat.sampleRate * decodedFormat.frameSize)

            Thread {
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (decodedStream.read(buffer).also { bytesRead = it } != -1) {
                    line.write(buffer, 0, bytesRead)
                }
                line.drain()
                line.close()
                _isPlaying.value = false
            }.start()
        } catch (e: javax.sound.sampled.UnsupportedAudioFileException) {
            _duration.value = 0.0
            throw UnsupportedOperationException(
                "Audio format not supported by desktop player. Try WAV format instead.",
                e
            )
        } catch (_: Exception) {
            _duration.value = 0.0
        }
    }

    actual fun pause() {
        clip?.stop()
        line?.stop()
        _isPlaying.value = false
        stopTimer()
    }

    actual fun resume() {
        clip?.start()
        line?.start()
        _isPlaying.value = true
        startTimer()
    }

    actual fun stop() {
        stopTimer()
        clip?.let { c ->
            c.stop()
            c.close()
        }
        line?.let { l ->
            l.stop()
            l.drain()
            l.close()
        }
        stream?.close()
        clip = null
        line = null
        stream = null
        _isPlaying.value = false
        _currentTime.value = 0.0
    }

    actual fun seek(positionSeconds: Double) {
        clip?.let { c ->
            val microseconds = (positionSeconds * 1_000_000).toLong()
            c.microsecondPosition = microseconds
            _currentTime.value = positionSeconds
        }
    }

    actual fun setRate(rate: Float) {
        _playbackRate.value = rate.coerceIn(0.5f, 2.0f)
    }

    actual fun release() {
        stop()
    }

    private fun startTimer() {
        stopTimer()
        timerThread = Thread {
            try {
                while (_isPlaying.value && !Thread.currentThread().isInterrupted) {
                    _currentTime.value = (clip?.microsecondPosition ?: 0) / 1_000_000.0
                    Thread.sleep(100)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.apply { isDaemon = true; start() }
    }

    private fun stopTimer() {
        timerThread?.interrupt()
        timerThread = null
    }
}
