package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SynthesisSessionTest {
    @Test
    fun cloudRequestUsesProviderDefaultModelAndSelectedVoice() = runBlocking {
        val cloud = RecordingCloudGateway(byteArrayOf(1, 2, 3))
        val local = RecordingLocalGateway()
        val session = SynthesisSession(cloudGateway = cloud, localGateway = local)

        val result = session.synthesize(
            SynthesisRequest(
                text = "hello",
                selection = SynthesisSelection.Cloud(TTSProvider.OPENAI, Voice.OPENAI_NOVA),
            )
        )

        assertEquals(TTSModel.OPENAI_TTS_1, cloud.model)
        assertEquals(Voice.OPENAI_NOVA, cloud.voice)
        assertEquals(AudioFormat.MP3, result.format)
        assertContentEquals(byteArrayOf(1, 2, 3), result.audioData)
        assertFalse(local.wasCalled)
    }

    @Test
    fun cloudRequestFallsBackToProviderFirstVoice() = runBlocking {
        val cloud = RecordingCloudGateway(byteArrayOf(4))
        val session = SynthesisSession(cloudGateway = cloud, localGateway = RecordingLocalGateway())

        session.synthesize(
            SynthesisRequest(
                text = "hello",
                selection = SynthesisSelection.Cloud(TTSProvider.OPENAI),
            )
        )

        assertEquals(Voice.OPENAI_ALLOY, cloud.voice)
    }

    @Test
    fun localRequestChecksSupportThenReturnsWavResult() = runBlocking {
        val local = RecordingLocalGateway(
            supported = setOf(LocalTTSProvider.SHERPA),
            result = LocalSynthesisResult(byteArrayOf(9), 16000, AudioFormat.WAV),
        )
        val session = SynthesisSession(cloudGateway = RecordingCloudGateway(), localGateway = local)

        val result = session.synthesize(
            SynthesisRequest(
                text = "你好",
                selection = SynthesisSelection.Local(LocalTTSProvider.SHERPA),
            )
        )

        assertTrue(local.wasCalled)
        assertEquals(AudioFormat.WAV, result.format)
        assertContentEquals(byteArrayOf(9), result.audioData)
    }

    private class RecordingCloudGateway(
        private val audio: ByteArray = byteArrayOf(1),
    ) : CloudSynthesisGateway {
        var model: TTSModel? = null
        var voice: Voice? = null

        override suspend fun synthesize(
            text: String,
            model: TTSModel,
            voice: Voice,
            format: AudioFormat,
        ): ByteArray {
            this.model = model
            this.voice = voice
            return audio
        }

        override fun cancel() = Unit
    }

    private class RecordingLocalGateway(
        private val supported: Set<LocalTTSProvider> = emptySet(),
        private val result: LocalSynthesisResult = LocalSynthesisResult(byteArrayOf(), 16000, AudioFormat.WAV),
    ) : LocalSynthesisGateway {
        var wasCalled = false

        override fun supports(provider: LocalTTSProvider): Boolean = provider in supported

        override suspend fun synthesize(
            text: String,
            provider: LocalTTSProvider,
            format: AudioFormat,
        ): LocalSynthesisResult {
            wasCalled = true
            return result
        }

        override fun cancel() = Unit
    }
}
