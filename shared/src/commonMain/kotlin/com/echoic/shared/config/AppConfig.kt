package com.echoic.shared.config

import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.platformConfigDirectory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Application configuration stored as JSON.
 */
@Serializable
data class AppConfigData(
    val appearance: String = "system",     // "light", "dark", "system"
    val language: String = "en",           // "en", "zh"
    val providerKeys: Map<String, String> = emptyMap(),  // provider name -> API key
    val providerURLs: Map<String, String> = emptyMap(),  // provider name -> custom base URL
    val hasCompletedOnboarding: Boolean = false,
    val defaultOutputFormat: String = "mp3",
    val saveDirectory: String = "",
    val defaultCloudProvider: String? = null,   // TTSProvider.name
    val defaultLocalProvider: String? = null,   // LocalTTSProvider.name
)

/**
 * Manages app configuration with JSON file persistence.
 */
class AppConfig(private val configFile: PlatformFile) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var _data: AppConfigData = load()

    val data: AppConfigData get() = _data

    fun getApiKey(provider: TTSProvider): String? {
        val key = _data.providerKeys[provider.name] ?: return null
        return key.ifBlank { null }
    }

    fun getBaseURL(provider: TTSProvider): String {
        return _data.providerURLs[provider.name] ?: provider.defaultBaseURL
    }

    fun setApiKey(provider: TTSProvider, key: String) {
        _data = _data.copy(
            providerKeys = _data.providerKeys + (provider.name to key)
        )
        save()
    }

    fun setBaseURL(provider: TTSProvider, url: String) {
        _data = _data.copy(
            providerURLs = _data.providerURLs + (provider.name to url)
        )
        save()
    }

    fun updateAppearance(appearance: String) {
        _data = _data.copy(appearance = appearance)
        save()
    }

    fun updateLanguage(language: String) {
        _data = _data.copy(language = language)
        save()
    }

    fun completeOnboarding() {
        _data = _data.copy(hasCompletedOnboarding = true)
        save()
    }

    fun getDefaultCloudProvider(): TTSProvider? {
        val name = _data.defaultCloudProvider ?: return null
        return try { TTSProvider.valueOf(name) } catch (_: Exception) { null }
    }

    fun getDefaultLocalProvider(): LocalTTSProvider? {
        val name = _data.defaultLocalProvider ?: return null
        return try { LocalTTSProvider.valueOf(name) } catch (_: Exception) { null }
    }

    fun setDefaultCloudProvider(provider: TTSProvider?) {
        _data = _data.copy(defaultCloudProvider = provider?.name)
        save()
    }

    fun setDefaultLocalProvider(provider: LocalTTSProvider?) {
        _data = _data.copy(defaultLocalProvider = provider?.name)
        save()
    }

    fun isProviderConfigured(provider: TTSProvider): Boolean {
        if (!provider.requiresAPIKey) return true
        return getApiKey(provider) != null
    }

    fun reset() {
        _data = AppConfigData()
        save()
    }

    private fun load(): AppConfigData {
        return try {
            if (configFile.exists()) {
                json.decodeFromString<AppConfigData>(configFile.readText())
            } else {
                AppConfigData()
            }
        } catch (_: Exception) {
            AppConfigData()
        }
    }

    private fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(AppConfigData.serializer(), _data))
        } catch (_: Exception) {
            // Silently fail — config is not critical
        }
    }

    companion object {
        fun defaultPath(): PlatformFile {
            val configDir = platformConfigDirectory()
            return PlatformFile("$configDir/config.json")
        }
    }
}
