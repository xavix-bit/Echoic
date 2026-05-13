package com.echoic.shared.service

import com.echoic.shared.model.TTSProvider
import io.ktor.client.*

/**
 * Factory for creating provider-specific TTS services.
 */
object TTSServiceFactory {
    fun create(
        provider: TTSProvider,
        httpClient: HttpClient,
        apiKey: String,
        baseURL: String,
    ): TTSService = when (provider) {
        TTSProvider.OPENAI -> OpenAITTSService(httpClient, apiKey, baseURL)
        TTSProvider.GOOGLE -> GoogleTTSService(httpClient, apiKey, baseURL)
        TTSProvider.AZURE -> AzureTTSService(httpClient, apiKey, baseURL)
        TTSProvider.ELEVENLABS -> ElevenLabsTTSService(httpClient, apiKey, baseURL)
        TTSProvider.BAIDU -> BaiduTTSService(httpClient, apiKey, baseURL)
        TTSProvider.TENCENT -> TencentTTSService(httpClient, apiKey, baseURL)
        TTSProvider.ALIYUN -> AliyunTTSService(httpClient, apiKey, baseURL)
        TTSProvider.FISH_AUDIO -> FishAudioService(httpClient, apiKey, baseURL)
        TTSProvider.MINIMAX -> MiniMaxTTSService(httpClient, apiKey, baseURL)
        TTSProvider.ZHIPU -> ZhipuTTSService(httpClient, apiKey, baseURL)
        TTSProvider.VOLCENGINE -> VolcengineTTSService(httpClient, apiKey, baseURL)
        TTSProvider.EDGETTS -> EdgeTTSService(httpClient, baseURL)
    }
}
