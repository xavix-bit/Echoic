package com.echoic.shared.audio

import com.echoic.shared.model.AudioFormat
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform audio player interface.
 * Platform-specific implementations via expect/actual.
 */
expect class AudioPlayer() {
    /** Whether audio is currently playing */
    val isPlaying: StateFlow<Boolean>

    /** Current playback position in seconds */
    val currentTime: StateFlow<Double>

    /** Total duration in seconds */
    val duration: StateFlow<Double>

    /** Current playback rate (0.5 - 2.0) */
    val playbackRate: StateFlow<Float>

    /** Play audio data in the given format */
    suspend fun play(data: ByteArray, format: AudioFormat)

    /** Pause playback */
    fun pause()

    /** Resume playback */
    fun resume()

    /** Stop playback and reset position */
    fun stop()

    /** Seek to position in seconds */
    fun seek(positionSeconds: Double)

    /** Set playback rate (0.5 - 2.0) */
    fun setRate(rate: Float)

    /** Release resources */
    fun release()
}
