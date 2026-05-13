package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import com.echoic.shared.util.base64Decode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.echoic.shared.platform.platformGenerateUUID

/**
 * Volcano Engine (火山引擎) TTS service.
 * API: POST /api/v1/tts
 */
class VolcengineTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.VOLCENGINE.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    // apiKey format: "appid;access_token" (semicolon separated)
    private val appId: String
    private val accessToken: String

    init {
        val parts = apiKey.split(";", limit = 2)
        if (parts.size == 2) {
            appId = parts[0].trim()
            accessToken = parts[1].trim()
        } else {
            // Treat the whole key as access token, use a placeholder appid
            appId = "echoic"
            accessToken = apiKey.trim()
        }
    }

    @Serializable
    private data class AppInfo(
        val appid: String,
        val token: String,
        val cluster: String = "volcano_tts",
    )

    @Serializable
    private data class UserInfo(
        val uid: String = "echoic_user",
    )

    @Serializable
    private data class AudioParams(
        val voice_type: String,
        val encoding: String = "mp3",
        val speed_ratio: Double = 1.0,
        val volume_ratio: Double = 1.0,
        val pitch_ratio: Double = 1.0,
        val rate: Int = 24000,
    )

    @Serializable
    private data class RequestParams(
        val reqid: String,
        val text: String,
        val operation: String = "query",
        val text_type: String = "plain",
    )

    @Serializable
    private data class TTSRequest(
        val app: AppInfo,
        val user: UserInfo = UserInfo(),
        val audio: AudioParams,
        val request: RequestParams,
    )

    @Serializable
    private data class TTSResponse(
        val reqid: String? = null,
        val code: Int = -1,
        val message: String? = null,
        val data: String? = null,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/api/v1/tts"

        val encodingParam = when (format) {
            AudioFormat.MP3 -> "mp3"
            AudioFormat.WAV -> "wav"
            AudioFormat.OPUS -> "ogg_opus"
            AudioFormat.FLAC -> "pcm"
            AudioFormat.AAC -> "mp3"
        }

        val response = httpClient.post(url) {
            header("Authorization", "Bearer;$accessToken")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                TTSRequest(
                    app = AppInfo(appid = appId, token = accessToken),
                    audio = AudioParams(
                        voice_type = voice.id,
                        encoding = encodingParam,
                    ),
                    request = RequestParams(
                        reqid = platformGenerateUUID(),
                        text = text,
                    ),
                )
            )
        }

        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw TTSResponseException("Volcano Engine API error ${response.status.value}: $responseBody")
        }

        val ttsResponse = json.decodeFromString<TTSResponse>(responseBody)

        // code 3000 = success
        if (ttsResponse.code != 3000) {
            throw TTSResponseException(
                "Volcano Engine API error ${ttsResponse.code}: ${ttsResponse.message ?: "Unknown error"}"
            )
        }

        // Decode base64-encoded audio data
        val audioBase64 = ttsResponse.data
            ?: throw TTSResponseException("Volcano Engine returned empty audio data")

        return base64Decode(audioBase64)
    }
}
