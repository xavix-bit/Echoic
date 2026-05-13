package com.echoic.shared.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.echoic.shared.platform.PlatformFile
import com.echoic.shared.platform.platformHomeDirectory
import com.echoic.shared.platform.platformCurrentTimeMillis

/**
 * Data class representing the installation status of a local model.
 */
data class ModelInstallationStatus(
    val provider: LocalTTSProvider,
    val isInstalled: Boolean = false,
    val sizeInBytes: Long = 0,
    val lastUsedTimestamp: Long = 0,
    val isInstalling: Boolean = false,
    val installProgress: Float = 0f,
    val error: String? = null,
)

/**
 * Manager for local TTS models.
 * Handles model installation, uninstallation, and cache management.
 */
class LocalModelManager {
    private val _modelStatuses = MutableStateFlow<Map<LocalTTSProvider, ModelInstallationStatus>>(emptyMap())
    val modelStatuses: StateFlow<Map<LocalTTSProvider, ModelInstallationStatus>> = _modelStatuses.asStateFlow()

    init {
        // 初始化所有 provider 的状态，检查文件系统
        refreshAllStatuses()
    }

    /**
     * 刷新所有 provider 的安装状态
     */
    fun refreshAllStatuses() {
        val initialStatuses = LocalTTSProvider.values().toList().associateWith { provider ->
            val installed = isModelInstalledOnDisk(provider)
            val size = if (installed) getModelSizeOnDisk(provider) else 0L
            ModelInstallationStatus(
                provider = provider,
                isInstalled = installed,
                sizeInBytes = size,
            )
        }
        _modelStatuses.value = initialStatuses
    }

    /**
     * 获取模型的存储路径
     */
    fun getModelPath(provider: LocalTTSProvider): String {
        val home = platformHomeDirectory()
        return "$home/.echoic/models/${provider.name.lowercase()}"
    }

    /**
     * 检查模型是否已安装（基于文件系统）
     */
    fun isModelInstalled(provider: LocalTTSProvider): Boolean {
        return isModelInstalledOnDisk(provider)
    }

    /**
     * 检查文件系统中是否存在模型
     */
    private fun isModelInstalledOnDisk(provider: LocalTTSProvider): Boolean {
        val modelDir = PlatformFile(getModelPath(provider))
        if (!modelDir.exists() || !modelDir.isDirectory) return false
        val files = modelDir.listFiles()
        return files != null && files.isNotEmpty()
    }

    /**
     * 获取已安装模型的大小（字节）
     */
    fun getModelSize(provider: LocalTTSProvider): Long {
        return _modelStatuses.value[provider]?.sizeInBytes ?: 0L
    }

    /**
     * 从文件系统计算模型大小
     */
    private fun getModelSizeOnDisk(provider: LocalTTSProvider): Long {
        val modelDir = PlatformFile(getModelPath(provider))
        return calculateDirectorySize(modelDir)
    }

    /**
     * 计算目录总大小
     */
    private fun calculateDirectorySize(directory: PlatformFile): Long {
        if (!directory.exists()) return 0L
        if (directory.isFile) return directory.length()

        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * 格式化模型大小为人类可读字符串
     */
    fun formatModelSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes * 10 / (1024 * 1024) / 10.0} MB"
            else -> "${bytes * 100 / (1024 * 1024 * 1024) / 100.0} GB"
        }
    }

    /**
     * 标记模型为已安装（由 ModelInstaller 调用）
     */
    fun markAsInstalled(provider: LocalTTSProvider, sizeInBytes: Long = 0) {
        updateStatus(provider) {
            it.copy(
                isInstalled = true,
                isInstalling = false,
                installProgress = 1f,
                sizeInBytes = sizeInBytes,
                error = null,
            )
        }
    }

    /**
     * 安装模型（标记为已安装）
     */
    suspend fun installModel(provider: LocalTTSProvider, url: String? = null) {
        updateStatus(provider) { it.copy(isInstalling = true, installProgress = 0f, error = null) }

        try {
            // 计算实际大小
            val size = getModelSizeOnDisk(provider)
            markAsInstalled(provider, size)
        } catch (e: Exception) {
            updateStatus(provider) {
                it.copy(
                    isInstalling = false,
                    installProgress = 0f,
                    error = e.message ?: "Installation failed",
                )
            }
        }
    }

    /**
     * 卸载模型
     */
    suspend fun uninstallModel(provider: LocalTTSProvider) {
        updateStatus(provider) { it.copy(isInstalling = true) }

        try {
            withContext(Dispatchers.IO) {
                val modelDir = PlatformFile(getModelPath(provider))
                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
                }
            }

            updateStatus(provider) {
                ModelInstallationStatus(provider = provider)
            }
        } catch (e: Exception) {
            updateStatus(provider) { it.copy(isInstalling = false, error = e.message ?: "卸载失败") }
        }
    }

    /**
     * 清除模型缓存
     */
    suspend fun clearCache(provider: LocalTTSProvider) {
        withContext(Dispatchers.IO) {
            val home = platformHomeDirectory()
            val cacheDir = PlatformFile("$home/.echoic/cache/${provider.name.lowercase()}")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        }
    }

    /**
     * 清除所有模型
     */
    suspend fun clearAll() {
        for (provider in LocalTTSProvider.values().toList()) {
            if (isModelInstalled(provider)) {
                uninstallModel(provider)
            }
        }
    }

    /**
     * 检查更新
     */
    suspend fun checkForUpdates(): Map<LocalTTSProvider, Boolean> {
        // 当前版本暂不支持远程更新检查
        return LocalTTSProvider.values().toList().associateWith { false }
    }

    /**
     * 更新最后使用时间
     */
    fun updateLastUsed(provider: LocalTTSProvider) {
        updateStatus(provider) { it.copy(lastUsedTimestamp = platformCurrentTimeMillis()) }
    }

    /**
     * 格式化时间戳为人类可读字符串
     */
    fun formatLastUsed(timestamp: Long): String {
        if (timestamp == 0L) return "Never"

        val now = platformCurrentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            else -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        }
    }

    private fun updateStatus(provider: LocalTTSProvider, update: (ModelInstallationStatus) -> ModelInstallationStatus) {
        val currentStatuses = _modelStatuses.value.toMutableMap()
        val currentStatus = currentStatuses[provider] ?: ModelInstallationStatus(provider = provider)
        currentStatuses[provider] = update(currentStatus)
        _modelStatuses.value = currentStatuses
    }
}
