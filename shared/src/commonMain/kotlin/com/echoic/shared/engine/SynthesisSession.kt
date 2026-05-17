package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.ProviderCatalog
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice

sealed class SynthesisSelection {
    data class Cloud(
        val provider: TTSProvider,
        val voice: Voice? = null,
    ) : SynthesisSelection()

    data class Local(
        val provider: LocalTTSProvider,
    ) : SynthesisSelection()
}

data class SynthesisRequest(
    val text: String,
    val selection: SynthesisSelection,
)

data class SynthesisResult(
    val audioData: ByteArray,
    val format: AudioFormat,
    val sampleRate: Int? = null,
)

interface CloudSynthesisGateway {
    suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat = AudioFormat.MP3,
    ): ByteArray

    fun cancel()
}

interface LocalSynthesisGateway {
    fun supports(provider: LocalTTSProvider): Boolean

    suspend fun synthesize(
        text: String,
        provider: LocalTTSProvider,
        format: AudioFormat = AudioFormat.WAV,
    ): LocalSynthesisResult

    fun cancel()
}

class EngineCloudSynthesisGateway(
    private val engine: TTSEngine,
) : CloudSynthesisGateway {
    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray = engine.synthesize(text, model, voice, format)

    override fun cancel() {
        engine.cancel()
    }
}

class EngineLocalSynthesisGateway(
    private val engine: LocalTTSEngine,
) : LocalSynthesisGateway {
    override fun supports(provider: LocalTTSProvider): Boolean =
        engine.supports(provider)

    override suspend fun synthesize(
        text: String,
        provider: LocalTTSProvider,
        format: AudioFormat,
    ): LocalSynthesisResult = engine.synthesize(text, provider, format)

    override fun cancel() {
        engine.cancel()
    }
}

class SynthesisSession(
    private val cloudGateway: CloudSynthesisGateway,
    private val localGateway: LocalSynthesisGateway?,
    private val providerCatalog: ProviderCatalog = ProviderCatalog(),
) {
    suspend fun synthesize(request: SynthesisRequest): SynthesisResult {
        require(request.text.isNotBlank()) { "Text must not be blank" }

        return when (val selection = request.selection) {
            is SynthesisSelection.Cloud -> synthesizeCloud(request.text, selection)
            is SynthesisSelection.Local -> synthesizeLocal(request.text, selection)
        }
    }

    fun cancel() {
        cloudGateway.cancel()
        localGateway?.cancel()
    }

    private suspend fun synthesizeCloud(
        text: String,
        selection: SynthesisSelection.Cloud,
    ): SynthesisResult {
        val defaults = providerCatalog.cloudDefaults(selection.provider)
        val model = defaults.model
        val voice = selection.voice ?: defaults.voice
        val audio = cloudGateway.synthesize(text, model, voice, AudioFormat.MP3)

        return SynthesisResult(audioData = audio, format = AudioFormat.MP3)
    }

    private suspend fun synthesizeLocal(
        text: String,
        selection: SynthesisSelection.Local,
    ): SynthesisResult {
        val gateway = localGateway
            ?: throw IllegalStateException("Local synthesis engine is not available")
        require(gateway.supports(selection.provider)) {
            "当前不支持 ${selection.provider.displayName} 的本地合成。"
        }
        val result = gateway.synthesize(text, selection.provider, AudioFormat.WAV)

        return SynthesisResult(
            audioData = result.audioData,
            format = result.format,
            sampleRate = result.sampleRate,
        )
    }
}
