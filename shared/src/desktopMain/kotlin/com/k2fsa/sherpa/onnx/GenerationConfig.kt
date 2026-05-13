package com.k2fsa.sherpa.onnx

data class GenerationConfig(
    var silenceScale: Float = 0.2f,
    var speed: Float = 1.0f,
    var sid: Int = 0,
    var referenceAudio: FloatArray? = null,
    var referenceSampleRate: Int = 0,
    var referenceText: String? = null,
    var numSteps: Int = 5,
    var extra: Map<String, String>? = null,
)
