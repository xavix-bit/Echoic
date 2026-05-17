package com.echoic.shared.download

import com.echoic.shared.model.LocalTTSProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadConfigTest {
    @Test
    fun buildsKokoroReleaseArchiveFallback() {
        val files = DownloadConfig.getFallbackDownloadFiles(
            provider = LocalTTSProvider.KOKORO,
            repositoryUrl = "https://hf-mirror.com/hexgrad/Kokoro-82M",
        )

        assertEquals(
            listOf(
                DownloadFile(
                    relativePath = "kokoro-multi-lang-v1_0.tar.gz",
                    url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.gz",
                )
            ),
            files,
        )
    }

    @Test
    fun preservesNestedFileNameWhenDerivingDestination() {
        val file = DownloadFile(
            relativePath = "voices/zf_xiaobei.pt",
            url = "https://hf-mirror.com/hexgrad/Kokoro-82M/resolve/main/voices/zf_xiaobei.pt",
            sizeBytes = null,
        )

        assertEquals("voices/zf_xiaobei.pt", file.relativePath)
    }

    @Test
    fun buildsSherpaUrlsFromRepositoryPage() {
        val urls = DownloadConfig.getFallbackDownloadFiles(
            provider = LocalTTSProvider.SHERPA,
            repositoryUrl = "https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-unity",
        ).map { it.url }

        assertTrue("https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-unity/resolve/main/vits-zh-hf-fanchen-unity.onnx" in urls)
        assertTrue("https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-unity/resolve/main/tokens.txt" in urls)
    }

    @Test
    fun directFileFallbackDownloadsOnlyThatFile() {
        val files = DownloadConfig.getFallbackDownloadFiles(
            provider = LocalTTSProvider.VOXCPM,
            repositoryUrl = "https://hf-mirror.com/openbmb/VoxCPM2/resolve/main/model.safetensors",
        )

        assertEquals(
            listOf(
                DownloadFile(
                    relativePath = "model.safetensors",
                    url = "https://hf-mirror.com/openbmb/VoxCPM2/resolve/main/model.safetensors",
                )
            ),
            files,
        )
    }
}
