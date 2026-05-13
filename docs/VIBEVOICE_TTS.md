# VibeVoice TTS 集成指南

## 概述

VibeVoice 是微软开源的 AI 语音合成模型，支持中英双语，具有高自然度和表现力。

### 模型版本

| 模型 | 状态 | 说明 |
|------|------|------|
| **VibeVoice-Realtime-0.5B** | ✅ 可用 | 实时流式 TTS，延迟约 300ms，代码完整 |
| VibeVoice-TTS-1.5B | ❌ 已禁用 | 长文本 TTS（90分钟），微软已移除代码 |

**推荐使用：VibeVoice-Realtime-0.5B**

---

## 系统要求

### 硬件要求

- **内存**：8GB+ RAM
- **GPU**（推荐）：
  - NVIDIA GPU（CUDA）：4GB+ VRAM
  - Apple Silicon（MPS）：支持
  - CPU：可用但速度较慢
- **磁盘**：2GB+ 可用空间（模型 + 依赖）

### 软件要求

- **Python**：3.10+
- **PyTorch**：2.0+
- **操作系统**：macOS / Windows / Linux

---

## 安装步骤

### 1. 克隆 VibeVoice 仓库

```bash
git clone https://github.com/microsoft/VibeVoice.git
cd VibeVoice
```

### 2. 创建 Python 虚拟环境（推荐）

```bash
python -m venv venv

# macOS/Linux
source venv/bin/activate

# Windows
venv\Scripts\activate
```

### 3. 安装依赖

```bash
pip install -e ".[streamingtts]"
```

这会安装以下主要依赖：
- `torch` (PyTorch)
- `transformers` (HuggingFace Transformers)
- `fastapi` + `uvicorn` (Web 服务器)
- 其他音频处理库

### 4. 下载模型权重

模型会在首次运行时自动从 HuggingFace 下载，或手动下载：

```bash
# 方式一：使用 HuggingFace CLI
pip install huggingface_hub
huggingface-cli download microsoft/VibeVoice-Realtime-0.5B

# 方式二：使用 hf-mirror（国内镜像，推荐）
export HF_ENDPOINT=https://hf-mirror.com
huggingface-cli download microsoft/VibeVoice-Realtime-0.5B
```

### 5. 下载语音预设（可选）

```bash
bash demo/download_experimental_voices.sh
```

---

## 启动 TTS 服务

### 方式一：命令行推理（单次生成）

```bash
python demo/realtime_model_inference_from_file.py \
    --model_path "microsoft/VibeVoice-Realtime-0.5B" \
    --txt_path "demo/text_examples/1p_vibevoice.txt" \
    --speaker_name "Wayne" \
    --output_dir "./outputs" \
    --device "cuda" \
    --cfg_scale 1.5
```

**参数说明：**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--model_path` | `microsoft/VibeVoice-Realtime-0.5B` | HuggingFace 模型路径 |
| `--txt_path` | - | 输入文本文件路径 |
| `--speaker_name` | `Wayne` | 说话人名称（需有对应 .pt 文件） |
| `--output_dir` | `./outputs` | 输出目录 |
| `--device` | 自动检测 | `cuda` / `mps` / `cpu` |
| `--cfg_scale` | `1.5` | CFG 引导强度（1.0-2.0） |

**输出：**
- 24kHz WAV 音频文件
- 生成速度、时长、RTF 等统计信息

### 方式二：启动 Web API 服务（推荐集成方式）

```bash
python demo/vibevoice_realtime_demo.py \
    --model_path "microsoft/VibeVoice-Realtime-0.5B" \
    --device "cuda" \
    --port 3000
```

启动后，服务运行在 `http://localhost:3000`，提供 REST API 接口。

---

## 集成到 Echoic-KMP

### 方案一：通过 REST API 调用（推荐）

#### 1. 启动 VibeVoice 服务

在后台启动 VibeVoice Web 服务：

```bash
# 启动服务（后台运行）
python demo/vibevoice_realtime_demo.py \
    --model_path "microsoft/VibeVoice-Realtime-0.5B" \
    --device "cuda" \
    --port 3000 &
```

#### 2. 创建 VibeVoiceTTSService

```kotlin
// shared/src/commonMain/kotlin/com/echoic/shared/service/VibeVoiceTTSService.kt
package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.Voice
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class VibeVoiceTTSService(
    private val baseUrl: String = "http://localhost:3000"
) : TTSService {

    private val client = HttpClient(CIO)

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val response = client.post("$baseUrl/tts") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "text" to text,
                "speaker" to voice.id,
                "cfg_scale" to 1.5
            ))
        }

        return response.body()
    }
}
```

#### 3. 注册到 TTSServiceFactory

```kotlin
// shared/src/commonMain/kotlin/com/echoic/shared/service/TTSServiceFactory.kt
object TTSServiceFactory {
    fun create(provider: TTSProvider): TTSService {
        return when (provider) {
            // ... 其他 provider
            TTSProvider.VIBEVOICE -> VibeVoiceTTSService()
            else -> throw IllegalArgumentException("Unknown provider: $provider")
        }
    }
}
```

### 方案二：通过命令行调用

```kotlin
// shared/src/desktopMain/kotlin/com/echoic/shared/engine/VibeVoiceLocalEngine.kt
package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalTTSProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VibeVoiceLocalEngine : LocalTTSEngine {

    override fun isAvailable(): Boolean {
        // 检查 Python 环境和 VibeVoice 是否安装
        return try {
            val process = ProcessBuilder("python", "-c", "import vibevoice")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun supports(provider: LocalTTSProvider): Boolean {
        return provider == LocalTTSProvider.VIBEVOICE
    }

    override suspend fun synthesize(
        text: String,
        provider: LocalTTSProvider,
        format: AudioFormat,
    ): LocalSynthesisResult = withContext(Dispatchers.IO) {
        val textFile = File.createTempFile("vibevoice_input", ".txt")
        val outputFile = File.createTempFile("vibevoice_output", ".wav")

        try {
            // 写入输入文本
            textFile.writeText(text)

            // 调用 Python 脚本
            val process = ProcessBuilder(
                "python", "demo/realtime_model_inference_from_file.py",
                "--model_path", "microsoft/VibeVoice-Realtime-0.5B",
                "--txt_path", textFile.absolutePath,
                "--output_dir", outputFile.parent,
                "--device", "mps"  // 或 cuda/cpu
            )
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.inputStream.bufferedReader().readText()
                throw RuntimeException("VibeVoice 生成失败: $error")
            }

            // 读取生成的音频
            val audioData = outputFile.readBytes()

            LocalSynthesisResult(
                audioData = audioData,
                sampleRate = 24000,
                format = AudioFormat.WAV,
            )
        } finally {
            textFile.delete()
            outputFile.delete()
        }
    }

    override fun cancel() {
        // TODO: 实现取消逻辑
    }
}
```

---

## 在 Echoic-KMP 中添加 VibeVoice 提供商

### 1. 添加 LocalTTSProvider 枚举

```kotlin
// shared/src/commonMain/kotlin/com/echoic/shared/model/LocalTTSProvider.kt
VIBEVOICE(
    displayName = "VibeVoice",
    subtitle = "Microsoft Realtime TTS (500M params)",
    downloadURL = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
    githubURL = "https://github.com/microsoft/VibeVoice",
    documentationURL = "https://github.com/microsoft/VibeVoice/blob/main/docs/vibevoice-tts.md",
    tags = listOf(
        TTSTag.OFFLINE,
        TTSTag.OPEN_SOURCE,
        TTSTag.MULTILINGUAL,
        TTSTag.HIGH_QUALITY,
        TTSTag.LOCAL_COMPUTE,
        TTSTag.NO_API_KEY,
        TTSTag.NEURAL,
    ),
    modelSizeMB = 1500,
    supportedLanguages = listOf("en", "zh"),
    integrationMethod = "Python API or REST API",
    notes = "微软开源的实时流式 TTS，支持中英双语，延迟约 300ms。需要 Python 环境。",
    platformSupport = listOf(
        Platform.LINUX_X64,
        Platform.WINDOWS_X64,
        Platform.MACOS_X64,
        Platform.MACOS_ARM64,
    ),
    requiresGPU = true,
    minVRAM = 4,
    downloadMirrors = listOf(
        DownloadMirror(
            name = "HuggingFace",
            url = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
        ),
    ),
),
```

### 2. 添加 LocalTTSModel 枚举

```kotlin
// shared/src/commonMain/kotlin/com/echoic/shared/model/LocalTTSModel.kt
VIBEVOICE_REALTIME(
    rawValue = "vibevoice-realtime-0.5b",
    displayName = "VibeVoice Realtime (500M)",
    provider = LocalTTSProvider.VIBEVOICE,
    language = "multi",
    modelSizeMB = 1500,
    tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.NEURAL),
    modelURL = "https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B",
    notes = "微软实时流式 TTS 模型，支持中英双语，延迟约 300ms。",
),
```

### 3. 更新 DesktopLocalEngine

```kotlin
// shared/src/desktopMain/kotlin/com/echoic/shared/engine/DesktopLocalEngine.kt
override fun supports(provider: LocalTTSProvider): Boolean {
    return when (provider) {
        LocalTTSProvider.SHERPA -> true
        LocalTTSProvider.KOKORO -> false
        LocalTTSProvider.VOXCPM -> false
        LocalTTSProvider.VIBEVOICE -> true  // 添加这行
    }
}

override suspend fun synthesize(
    text: String,
    provider: LocalTTSProvider,
    format: AudioFormat,
): LocalSynthesisResult {
    cancelled = false
    require(text.isNotBlank()) { "输入文本不能为空" }
    require(supports(provider)) {
        "当前不支持 ${provider.displayName} 的本地合成。"
    }

    return when (provider) {
        LocalTTSProvider.SHERPA -> synthesizeViaSherpaOnnx(text, provider, format)
        LocalTTSProvider.VIBEVOICE -> synthesizeViaVibeVoice(text, provider, format)  // 添加这行
        else -> throw UnsupportedOperationException("${provider.displayName} 暂不支持本地合成")
    }
}

private suspend fun synthesizeViaVibeVoice(
    text: String,
    provider: LocalTTSProvider,
    format: AudioFormat,
): LocalSynthesisResult {
    // 使用 VibeVoiceLocalEngine 或 REST API
    val engine = VibeVoiceLocalEngine()
    return engine.synthesize(text, provider, format)
}
```

---

## 使用示例

### 启动应用后

1. 进入 **Providers** 页面
2. 找到 **VibeVoice** 提供商
3. 点击 **Download** 下载模型（首次使用）
4. 在 **New Generation** 页面选择 VibeVoice
5. 输入文本，点击生成

### 代码调用示例

```kotlin
// 使用 REST API
val service = VibeVoiceTTSService("http://localhost:3000")
val audioData = service.synthesize(
    text = "你好，今天天气怎么样？",
    model = TTSModel.VIBEVOICE_REALTIME,
    voice = Voice(id = "default", name = "Default"),
    format = AudioFormat.WAV
)

// 使用命令行
val engine = VibeVoiceLocalEngine()
val result = engine.synthesize(
    text = "Hello, how are you today?",
    provider = LocalTTSProvider.VIBEVOICE,
    format = AudioFormat.WAV
)
// result.audioData 是 WAV 字节数组
```

---

## 性能参考

| 指标 | 数值 |
|------|------|
| 模型大小 | ~1.5 GB |
| 首次加载时间 | 10-30 秒 |
| 生成速度（GPU） | ~30x 实时（RTF 0.03） |
| 生成速度（MPS） | ~10x 实时（RTF 0.1） |
| 生成速度（CPU） | ~2x 实时（RTF 0.5） |
| 延迟（首包） | ~300ms |
| 采样率 | 24kHz |

---

## 常见问题

### Q: 模型下载失败怎么办？

使用国内镜像：

```bash
export HF_ENDPOINT=https://hf-mirror.com
huggingface-cli download microsoft/VibeVoice-Realtime-0.5B
```

### Q: macOS 上如何使用 MPS 加速？

```bash
python demo/realtime_model_inference_from_file.py \
    --device mps \
    --model_path "microsoft/VibeVoice-Realtime-0.5B" \
    --txt_path "input.txt"
```

### Q: 如何切换说话人？

查看可用说话人：

```bash
ls demo/voices/streaming_model/
```

然后在代码中指定 `--speaker_name`。

### Q: 生成的音频有杂音？

确保采样率一致（24kHz），播放器支持该采样率。

### Q: 如何提高生成质量？

- 使用 GPU（CUDA/MPS）
- 调整 `cfg_scale` 参数（1.0-2.0）
- 使用英文标点（即使输入中文）

---

## 相关链接

- **VibeVoice GitHub**: https://github.com/microsoft/VibeVoice
- **模型权重**: https://huggingface.co/microsoft/VibeVoice-Realtime-0.5B
- **技术论文**: https://arxiv.org/pdf/2508.19205
- **Echoic-KMP 本地 TTS 文档**: [LOCAL_TTS_MODELS.md](LOCAL_TTS_MODELS.md)

---

## 许可证

VibeVoice 使用 MIT 许可证。
