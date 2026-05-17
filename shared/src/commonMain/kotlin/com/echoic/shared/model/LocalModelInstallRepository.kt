package com.echoic.shared.model

import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.platformHomeDirectory

/**
 * Owns local model filesystem layout and installed-state rules.
 */
class LocalModelInstallRepository(
    private val rootDir: PlatformFile = PlatformFile("${platformHomeDirectory()}/.echoic"),
) {
    fun modelDir(provider: LocalTTSProvider): PlatformFile =
        PlatformFile(PlatformFile(rootDir, "models"), provider.key)

    fun downloadDir(provider: LocalTTSProvider): PlatformFile =
        PlatformFile(PlatformFile(rootDir, "downloads"), provider.key)

    fun cacheDir(provider: LocalTTSProvider): PlatformFile =
        PlatformFile(PlatformFile(rootDir, "cache"), provider.key)

    fun modelPath(provider: LocalTTSProvider): String = modelDir(provider).absolutePath

    fun downloadPath(provider: LocalTTSProvider): String = downloadDir(provider).absolutePath

    fun isInstalled(provider: LocalTTSProvider): Boolean {
        val dir = modelDir(provider)
        if (!dir.exists() || !dir.isDirectory) return false
        return !dir.listFiles().isNullOrEmpty()
    }

    fun installedSize(provider: LocalTTSProvider): Long =
        if (isInstalled(provider)) sizeOf(modelDir(provider)) else 0L

    fun uninstall(provider: LocalTTSProvider) {
        val dir = modelDir(provider)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun clearCache(provider: LocalTTSProvider) {
        val dir = cacheDir(provider)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun sizeOf(file: PlatformFile): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()

        var size = 0L
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) sizeOf(child) else child.length()
        }
        return size
    }

    private val LocalTTSProvider.key: String
        get() = name.lowercase()
}
