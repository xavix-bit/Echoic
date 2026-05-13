package com.echoic.shared.download

import com.echoic.shared.model.LocalTTSProvider

/**
 * A single file that belongs to a local model installation.
 *
 * [relativePath] is preserved under the target directory. This matters for
 * repositories such as Kokoro where voice files live under `voices/`.
 */
data class DownloadFile(
    val relativePath: String,
    val url: String,
    val sizeBytes: Long? = null,
)

data class ModelRepository(
    val hostBaseUrl: String,
    val repoId: String,
)

/**
 * Download metadata for local TTS models.
 *
 * The installer prefers live HuggingFace/hf-mirror repository metadata and
 * falls back to these curated manifests when the metadata API is unavailable.
 */
object DownloadConfig {
    // Kokoro ONNX 模型来自 sherpa-onnx releases（tar.gz 归档）
    // 内含: model.onnx, voices.bin, tokens.txt, espeak-ng-data/, lexicon-*.txt, dict/
    private val kokoroReleaseUrl =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.gz"

    private val sherpaFiles = listOf(
        "vits-zh-hf-fanchen-unity.onnx",
        "tokens.txt",
    )

    private val voxcpmFiles = listOf(
        "model.safetensors",
        "audiovae.pth",
        "config.json",
        "special_tokens_map.json",
        "tokenization_voxcpm2.py",
        "tokenizer.json",
        "tokenizer_config.json",
    )

    fun getFallbackDownloadFiles(
        provider: LocalTTSProvider,
        repositoryUrl: String,
    ): List<DownloadFile> {
        if (repositoryUrl.contains("/releases/download/")) {
            val fileName = repositoryUrl.substringAfterLast("/").substringBefore("?")
            return listOf(DownloadFile(relativePath = fileName, url = repositoryUrl))
        }
        extractDirectResolvePath(repositoryUrl)?.let { relativePath ->
            return listOf(
                DownloadFile(
                    relativePath = relativePath,
                    url = repositoryUrl,
                )
            )
        }

        val repository = parseHuggingFaceRepository(repositoryUrl)
            ?: return listOf(
                DownloadFile(
                    relativePath = repositoryUrl.substringAfterLast("/").substringBefore("?"),
                    url = repositoryUrl,
                )
            )

        val files = when (provider) {
            LocalTTSProvider.KOKORO -> {
                // Kokoro 从 sherpa-onnx releases 下载归档包
                return listOf(
                    DownloadFile(
                        relativePath = "kokoro-multi-lang-v1_0.tar.gz",
                        url = kokoroReleaseUrl,
                    )
                )
            }
            LocalTTSProvider.SHERPA -> sherpaFiles
            LocalTTSProvider.VOXCPM -> voxcpmFiles
            LocalTTSProvider.VIBEVOICE -> emptyList() // VibeVoice 通过 Python 环境独立安装
        }

        return files.map { relativePath ->
            DownloadFile(
                relativePath = relativePath,
                url = buildResolveUrl(repository, relativePath),
            )
        }
    }

    fun parseHuggingFaceRepository(url: String): ModelRepository? {
        val normalized = url.trim().trimEnd('/')
        if (normalized.isBlank()) return null

        val withoutScheme = normalized
            .removePrefix("https://")
            .removePrefix("http://")
        val host = withoutScheme.substringBefore("/")
        if (host.isBlank()) return null

        val path = withoutScheme.substringAfter("/", missingDelimiterValue = "")
        if (path.isBlank()) return null

        val repoPath = path
            .substringBefore("/resolve/")
            .substringBefore("/tree/")
            .substringBefore("/blob/")
            .substringBefore("/files/")
            .substringBefore("?")
            .trim('/')

        val parts = repoPath.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val scheme = if (normalized.startsWith("http://")) "http" else "https"
        return ModelRepository(
            hostBaseUrl = "$scheme://$host",
            repoId = "${parts[0]}/${parts[1]}",
        )
    }

    fun buildResolveUrl(repository: ModelRepository, relativePath: String): String {
        return "${repository.hostBaseUrl}/${repository.repoId}/resolve/main/${relativePath.trimStart('/')}"
    }

    fun buildApiUrl(repository: ModelRepository): String {
        return "${repository.hostBaseUrl}/api/models/${repository.repoId}"
    }

    private fun extractDirectResolvePath(url: String): String? {
        val marker = "/resolve/main/"
        val index = url.indexOf(marker)
        if (index == -1) return null
        return url.substring(index + marker.length).substringBefore("?").takeIf { it.isNotBlank() }
    }
}
