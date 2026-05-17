package com.echoic.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderCatalogTest {
    @Test
    fun exposesCloudAndLocalOptionsThroughOneInterface() {
        val catalog = ProviderCatalog()

        val options = catalog.options()

        assertTrue(options.any { it.id == "cloud:openai" && it.displayName == "OpenAI" })
        assertTrue(options.any { it.id == "local:sherpa" && it.displayName == "Sherpa-ONNX" })
    }

    @Test
    fun filtersOptionsByAllSelectedTags() {
        val catalog = ProviderCatalog()

        val options = catalog.options(tags = setOf(TTSTag.OFFLINE, TTSTag.NO_API_KEY))

        assertTrue(options.isNotEmpty())
        assertTrue(options.all { TTSTag.OFFLINE in it.tags && TTSTag.NO_API_KEY in it.tags })
    }

    @Test
    fun resolvesCloudDefaultsForSynthesis() {
        val catalog = ProviderCatalog()

        val defaults = catalog.cloudDefaults(TTSProvider.OPENAI)

        assertEquals(TTSModel.OPENAI_TTS_1, defaults.model)
        assertEquals(Voice.OPENAI_ALLOY, defaults.voice)
    }
}
