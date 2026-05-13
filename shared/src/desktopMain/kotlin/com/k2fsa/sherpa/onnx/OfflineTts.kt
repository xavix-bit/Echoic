package com.k2fsa.sherpa.onnx

class OfflineTts(
    var config: OfflineTtsConfig,
) {
    private var ptr: Long

    init {
        ptr = newFromFile(config)
    }

    fun sampleRate() = getSampleRate(ptr)
    fun numSpeakers() = getNumSpeakers(ptr)

    fun generate(
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
    ): GeneratedAudio {
        return generateImpl(ptr, text = text, sid = sid, speed = speed)
    }

    fun generateWithCallback(
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
        callback: (samples: FloatArray) -> Int,
    ): GeneratedAudio {
        return generateWithCallbackImpl(
            ptr,
            text = text,
            sid = sid,
            speed = speed,
            callback = callback,
        )
    }

    fun generateWithConfig(
        text: String,
        config: GenerationConfig,
    ): GeneratedAudio {
        return generateWithConfigImpl(ptr, text, config, null)
    }

    fun generateWithConfigAndCallback(
        text: String,
        config: GenerationConfig,
        callback: (samples: FloatArray) -> Int,
    ): GeneratedAudio {
        return generateWithConfigImpl(ptr, text, config, callback)
    }

    fun allocate() {
        if (ptr == 0L) {
            ptr = newFromFile(config)
        }
    }

    fun free() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    private external fun newFromFile(config: OfflineTtsConfig): Long
    private external fun delete(ptr: Long)
    private external fun getSampleRate(ptr: Long): Int
    private external fun getNumSpeakers(ptr: Long): Int

    private external fun generateImpl(
        ptr: Long,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
    ): GeneratedAudio

    private external fun generateWithCallbackImpl(
        ptr: Long,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
        callback: (samples: FloatArray) -> Int,
    ): GeneratedAudio

    private external fun generateWithConfigImpl(
        ptr: Long,
        text: String,
        config: GenerationConfig,
        callback: ((samples: FloatArray) -> Int)?,
    ): GeneratedAudio

    companion object {
        // Native library load is performed by NativeLibLoader prior to creating any OfflineTts.
        // NativeLibLoader uses System.load(absolutePath) to load both onnxruntime and
        // sherpa-onnx-jni extracted from the bundled native-lib JAR.
        // After that, JNI symbols are available and this class can be used.
    }
}
