package com.echoic.shared.model

import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.platformGenerateUUID
import com.echoic.shared.platform.platformHomeDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalModelInstallRepositoryTest {
    @Test
    fun derivesAllProviderDirectoriesFromOneRoot() {
        withTestRoot { root ->
            val repository = LocalModelInstallRepository(root)

            assertTrue(repository.modelDir(LocalTTSProvider.SHERPA).absolutePath.endsWith("/models/sherpa"))
            assertTrue(repository.downloadDir(LocalTTSProvider.SHERPA).absolutePath.endsWith("/downloads/sherpa"))
            assertTrue(repository.cacheDir(LocalTTSProvider.SHERPA).absolutePath.endsWith("/cache/sherpa"))
        }
    }

    @Test
    fun reportsInstalledOnlyWhenProviderModelDirContainsFiles() {
        withTestRoot { root ->
            val repository = LocalModelInstallRepository(root)
            val modelDir = repository.modelDir(LocalTTSProvider.SHERPA)

            assertFalse(repository.isInstalled(LocalTTSProvider.SHERPA))

            modelDir.mkdirs()
            assertFalse(repository.isInstalled(LocalTTSProvider.SHERPA))

            PlatformFile(modelDir, "tokens.txt").writeText("abc")

            assertTrue(repository.isInstalled(LocalTTSProvider.SHERPA))
            assertEquals(3L, repository.installedSize(LocalTTSProvider.SHERPA))
        }
    }

    @Test
    fun uninstallAndClearCacheOnlyDeleteProviderOwnedDirectories() {
        withTestRoot { root ->
            val repository = LocalModelInstallRepository(root)
            val sherpaModel = repository.modelDir(LocalTTSProvider.SHERPA)
            val kokoroModel = repository.modelDir(LocalTTSProvider.KOKORO)
            val sherpaCache = repository.cacheDir(LocalTTSProvider.SHERPA)

            PlatformFile(sherpaModel, "tokens.txt").also {
                it.parentFile?.mkdirs()
                it.writeText("abc")
            }
            PlatformFile(kokoroModel, "model.onnx").also {
                it.parentFile?.mkdirs()
                it.writeText("abc")
            }
            PlatformFile(sherpaCache, "tmp.bin").also {
                it.parentFile?.mkdirs()
                it.writeText("abc")
            }

            repository.uninstall(LocalTTSProvider.SHERPA)
            repository.clearCache(LocalTTSProvider.SHERPA)

            assertFalse(sherpaModel.exists())
            assertFalse(sherpaCache.exists())
            assertTrue(kokoroModel.exists())
        }
    }

    private fun withTestRoot(block: (PlatformFile) -> Unit) {
        val root = PlatformFile("${platformHomeDirectory()}/.echoic-test/${platformGenerateUUID()}")
        try {
            root.mkdirs()
            block(root)
        } finally {
            if (root.exists()) root.deleteRecursively()
        }
    }
}
