package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import com.echoic.shared.service.TTSResponseException
import com.echoic.shared.service.TTSServiceFactory
import io.ktor.client.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Cloud-based TTS engine that delegates to provider-specific services.
 */
class CloudTTSEngine(
    private val httpClient: HttpClient,
    private val apiKeyProvider: (TTSProvider) -> String?,
    private val baseURLProvider: (TTSProvider) -> String,
) : TTSEngine {

    private var currentJob: Job? = null

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        require(text.isNotBlank()) { "Text must not be blank" }

        val provider = model.provider
        val apiKey = if (provider.requiresAPIKey) {
            apiKeyProvider(provider) ?: throw TTSError.MissingAPIKey(provider.displayName)
        } else {
            "no-key-required"
        }
        val baseURL = baseURLProvider(provider)

        val service = TTSServiceFactory.create(provider, httpClient, apiKey, baseURL)

        currentJob = currentCoroutineContext()[Job]
        currentCoroutineContext().ensureActive()

        return try {
            service.synthesize(text, model, voice, format)
        } catch (e: Exception) {
            if (e is CancellationError) throw e
            throw TTSError.fromException(e)
        }
    }

    override fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }
}

/** Marker exception for cancellation that should propagate. */
class CancellationError : Exception("Synthesis cancelled")

sealed class TTSError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingAPIKey(providerName: String) : TTSError("API key required for $providerName. Configure it in Settings.")
    class Unauthorized : TTSError("Invalid API key. Please check your settings.")
    class RateLimited : TTSError("Rate limit exceeded. Please try again later.")
    class ServerError(val code: Int) : TTSError("Server error ($code). Please try again.")
    class NetworkError(cause: Throwable) : TTSError("Network error: ${cause.message}", cause)
    class InvalidResponse(detail: String) : TTSError("Invalid response: $detail")
    class EmptyText : TTSError("Text must not be empty.")

    companion object {
        fun fromException(e: Throwable): TTSError = when (e) {
            is TTSError -> e
            is TTSResponseException -> InvalidResponse(e.message ?: "TTS error")
            is ClientRequestException -> when (e.response.status.value) {
                401 -> Unauthorized()
                429 -> RateLimited()
                in 500..599 -> ServerError(e.response.status.value)
                else -> InvalidResponse(e.message ?: "Unknown error")
            }
            is ServerResponseException -> ServerError(e.response.status.value)
            else -> NetworkError(e)
        }
    }
}
