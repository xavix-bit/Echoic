package com.echoic.shared.engine

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files

/**
 * 从 classpath 中的 sherpa-onnx native-lib JAR 提取原生库并加载。
 *
 * JAR 内部路径结构（来自 k2-fsa 官方发布）：
 *   sherpa-onnx/native/{platform}/libonnxruntime.{version}.dylib
 *   sherpa-onnx/native/{platform}/libsherpa-onnx-jni.dylib
 */
object NativeLibLoader {

    @Volatile
    private var loaded = false

    /** 确保原生库已加载（幂等，线程安全） */
    @Synchronized
    fun load() {
        if (loaded) return

        val platform = detectPlatform()
            ?: throw UnsupportedOperationException("不支持的操作系统/架构: ${System.getProperty("os.name")} / ${System.getProperty("os.arch")}")

        val libDir = getLibDir()

        // 提取并加载 onnxruntime（依赖库）
        val ortName = onnxruntimeLibName(platform)
        val ortPath = extractLib(platform, ortName, libDir)
        System.load(ortPath)

        // 提取并加载 sherpa-onnx-jni（JNI 库）
        val jniName = jniLibName(platform)
        val jniPath = extractLib(platform, jniName, libDir)
        System.load(jniPath)

        loaded = true
    }

    // ─── 平台检测 ────────────────────────────────────────────────

    private fun detectPlatform(): String? {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        return when {
            os.contains("mac") || os.contains("darwin") -> {
                when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "osx-aarch64"
                    arch.contains("x86_64") || arch.contains("amd64") -> "osx-x64"
                    else -> null
                }
            }
            os.contains("linux") -> {
                when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "linux-aarch64"
                    arch.contains("x86_64") || arch.contains("amd64") -> "linux-x64"
                    else -> null
                }
            }
            os.contains("win") -> {
                when {
                    arch.contains("x86_64") || arch.contains("amd64") -> "win-x64"
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun onnxruntimeLibName(platform: String): String {
        return when {
            platform.startsWith("osx") -> "libonnxruntime.1.24.4.dylib"
            platform.startsWith("linux") -> "libonnxruntime.so.1.24.4"
            platform.startsWith("win") -> "onnxruntime.dll"
            else -> throw UnsupportedOperationException("Unknown platform: $platform")
        }
    }

    private fun jniLibName(platform: String): String {
        return when {
            platform.startsWith("osx") -> "libsherpa-onnx-jni.dylib"
            platform.startsWith("linux") -> "libsherpa-onnx-jni.so"
            platform.startsWith("win") -> "sherpa-onnx-jni.dll"
            else -> throw UnsupportedOperationException("Unknown platform: $platform")
        }
    }

    // ─── 文件提取 ────────────────────────────────────────────────

    private fun getLibDir(): File {
        val home = System.getProperty("user.home")
        val dir = File(home, ".echoic/native")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun extractLib(platform: String, libName: String, libDir: File): String {
        val destFile = File(libDir, libName)
        val resourcePath = "/sherpa-onnx/native/$platform/$libName"

        // 如果已存在且大小>0，复用
        if (destFile.exists() && destFile.length() > 0) {
            return destFile.absolutePath
        }

        val inputStream: InputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException(
                "找不到原生库资源: $resourcePath\n" +
                    "请确保 sherpa-onnx-native-lib JAR 在 classpath 中。"
            )

        inputStream.use { input ->
            // 写入临时文件，然后原子性重命名
            val tmpFile = File.createTempFile("echoic_native_", ".tmp", libDir)
            try {
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
                // 设置可执行权限
                tmpFile.setExecutable(true)
                tmpFile.setReadable(true)
                // 原子性移动
                Files.move(tmpFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                tmpFile.delete()
                throw e
            }
        }

        return destFile.absolutePath
    }
}
