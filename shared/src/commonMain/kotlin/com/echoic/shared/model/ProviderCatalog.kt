package com.echoic.shared.model

sealed class ProviderOption {
    abstract val id: String
    abstract val displayName: String
    abstract val subtitle: String
    abstract val tags: List<TTSTag>

    data class Cloud(
        val provider: TTSProvider,
    ) : ProviderOption() {
        override val id: String = "cloud:${provider.name.lowercase()}"
        override val displayName: String = provider.displayName
        override val subtitle: String = provider.subtitle
        override val tags: List<TTSTag> = provider.tags
    }

    data class Local(
        val provider: LocalTTSProvider,
    ) : ProviderOption() {
        override val id: String = "local:${provider.name.lowercase()}"
        override val displayName: String = provider.displayName
        override val subtitle: String = provider.subtitle
        override val tags: List<TTSTag> = provider.tags
    }
}

data class CloudProviderDefaults(
    val model: TTSModel,
    val voice: Voice,
)

class ProviderCatalog {
    fun options(tags: Set<TTSTag> = emptySet()): List<ProviderOption> {
        val allOptions = TTSProvider.entries.map { ProviderOption.Cloud(it) } +
            LocalTTSProvider.supportedEntries.map { ProviderOption.Local(it) }

        if (tags.isEmpty()) return allOptions
        return allOptions.filter { option -> tags.all { it in option.tags } }
    }

    fun cloudDefaults(provider: TTSProvider): CloudProviderDefaults {
        val model = provider.availableModels.firstOrNull()
            ?: throw IllegalArgumentException("${provider.displayName} has no available models")
        val voice = provider.availableVoices.firstOrNull()
            ?: throw IllegalArgumentException("${provider.displayName} has no available voices")

        return CloudProviderDefaults(model, voice)
    }
}
