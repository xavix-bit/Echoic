# 模型安装问题调查报告

**日期**：2026-05-02  
**调查范围**：模型安装取消、模型大小显示、下载完成性

---

## 问题一：无法取消模型安装

### 现象
安装模型时，没有可见的取消按钮。用户只能等待安装完成或关闭应用。

### 根因分析

**底层取消机制已实现，但 UI 层未接入。**

| 层级 | 状态 | 文件 |
|------|------|------|
| DownloadManager | ✅ 已实现 `cancelDownload()` | `DownloadManager.kt:327-330` |
| DownloadManager | ✅ 下载循环检查 `isCancelled` 标志 | `DownloadManager.kt:216, 414, 429, 443` |
| ModelInstaller | ✅ 捕获 `CancellationException` 并发射 `InstallState.Cancelled` | `ModelInstaller.kt:142-144` |
| ProvidersScreen | ❌ 无取消按钮 | `ProvidersScreen.kt` |
| LocalProviderCard | ❌ 无 `onCancel` 回调 | `ProvidersScreen.kt:644-668` |
| LocalTabContent | ❌ 未传递取消回调 | `ProvidersScreen.kt:568-586` |

**具体缺失**：
1. `LocalTabContent` 函数签名没有 `onCancel` 参数
2. `LocalProviderCard` 函数签名没有 `onCancel` 参数
3. `InlineDownloadProgress` 组件没有取消按钮（`ProvidersScreen.kt:849-864`）
4. 安装中时，安装按钮被禁用（`enabled = !isInstalling`），但没有替代的取消按钮

### 修复方案

需要在以下位置添加取消功能：

1. **InlineDownloadProgress** 组件：添加"取消"按钮
2. **LocalProviderCard**：添加 `onCancel` 回调参数
3. **LocalTabContent**：添加 `onCancel` 回调参数
4. **ProvidersScreen**：在 `onInstall` 逻辑旁添加取消逻辑：
   ```kotlin
   onCancel = {
       installer.downloadManager.cancelDownload()
   }
   ```

---

## 问题二：模型大小显示不准确

### 现象
Kokoro 显示为 82MB，但实际文件大小远不止 82MB。

### 根因分析

**`modelSizeMB` 字段存储的是参数量（parameter count），而非文件大小（file size）。**

| Provider | modelSizeMB 值 | 含义 | 实际文件大小（估算） |
|----------|---------------|------|---------------------|
| Kokoro | 82 | 82M 参数 | ~150-330MB（取决于格式） |
| Sherpa | 115 | 不明确 | ~80-120MB（ONNX 模型） |
| VoxCPM | 4700 | 不明确 | ~4-9GB（2B 参数模型） |
| VibeVoice | 1500 | 不明确 | ~1.5GB（500M 参数模型） |

**数据来源**：
- `LocalTTSProvider.kt` 第 43 行：Kokoro `modelSizeMB = 82`，但注释说 "82M params"
- `LocalTTSModel.kt` 第 22 行：KOKORO_V1 `modelSizeMB = 82`，注释说 "~82M params"

**影响范围**：
- 安装前显示的模型大小（`ProvidersScreen.kt:778-780`）：
  ```kotlin
  Text(if (isInstalled) manager.formatModelSize(status.sizeInBytes)
  else "${provider.modelSizeMB ?: 0} MB")
  ```
- 卸载对话框中的大小（`ProvidersScreen.kt:192-194`）：
  ```kotlin
  val size = if (status.isInstalled) localManager.formatModelSize(status.sizeInBytes)
  else "${provider.modelSizeMB ?: 0} MB"
  ```

### 修复方案

1. **修正 `modelSizeMB` 值**：改为实际文件大小（MB）
2. **或重命名字段**：改为 `modelParamsMB` 并添加新字段 `estimatedSizeMB`
3. **建议实际文件大小**：
   - Kokoro 82M (ONNX): ~150MB
   - Sherpa VITS: ~80MB
   - VoxCPM 2B: ~4700MB（保持不变）
   - VibeVoice 0.5B: ~1500MB（保持不变）

---

## 问题三：模型下载无法正常完成

### 现象
用户报告下载无法完成。可能表现为：卡在某个百分比、进度条不动、或报错。

### 根因分析

#### 3.1 文件过滤器 `isModelAsset()` 可能遗漏文件

**位置**：`DownloadManager.kt:469-487`

```kotlin
private fun isModelAsset(relativePath: String): Boolean {
    val lower = relativePath.lowercase()
    return listOf(
        ".bin", ".json", ".model", ".onnx", ".pth", ".pt",
        ".safetensors", ".spm", ".py", ".txt", ".yaml", ".yml"
    ).any { lower.endsWith(it) }
}
```

**潜在遗漏**：
- `.wav` / `.mp3` 音频文件（某些模型包含）
- `.md` 说明文件
- `.gitignore` 等配置文件
- 无扩展名的文件（如某些 tokenizer 文件）

**影响**：如果模型仓库中存在必须的文件但不在过滤列表中，下载的文件不完整，验证阶段可能失败。

#### 3.2 HuggingFace API 可能返回空或失败

**位置**：`DownloadManager.kt:96-127`

```kotlin
suspend fun discoverRepositoryFiles(...): List<DownloadFile> {
    // ...
    val response = httpClient.get(DownloadConfig.buildApiUrl(repository))
    if (!response.status.isSuccess()) {
        return DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl)
    }
    // ...
    files.ifEmpty { DownloadConfig.getFallbackDownloadFiles(provider, repositoryUrl) }
}
```

**问题**：
- 如果 API 返回 403/429（限流），会静默回退到 fallback 文件列表
- Fallback 列表中的文件没有 `sizeBytes`，导致进度计算不准确
- 回退过程不通知用户，用户可能不知道实际在下载什么

#### 3.3 网络超时配置

**位置**：`DownloadManager.kt:61-68`

```kotlin
private val httpClient = HttpClient(Java) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30 * 60 * 1000  // 30 分钟
        connectTimeoutMillis = 30_000           // 30 秒
        socketTimeoutMillis = 30 * 60 * 1000   // 30 分钟
    }
}
```

**问题**：
- 对于大模型（如 VoxCPM 4.7GB），30 分钟可能不够
- 如果网络不稳定，30 秒连接超时可能太短
- 没有重试机制

#### 3.4 文件大小不匹配检查

**位置**：`DownloadManager.kt:446-448`

```kotlin
if (file.sizeBytes != null && downloadedBytes != file.sizeBytes) {
    throw Exception("${file.relativePath}: 文件大小不匹配...")
}
```

**问题**：
- 如果服务器在下载过程中返回的 Content-Length 与 API 报告的 sizeBytes 不一致，会直接失败
- 没有重试机会

#### 3.5 hf-mirror.com 可能返回 HTML 而非文件

**位置**：`DownloadManager.kt:366-369`

```kotlin
val contentType = response.headers[HttpHeaders.ContentType]?.lowercase().orEmpty()
if (contentType.contains("text/html")) {
    throw Exception("下载链接返回 HTML 页面...")
}
```

**问题**：
- hf-mirror.com 在某些情况下可能返回验证页面或错误页面
- 检测到后直接抛出异常，不尝试其他源

### 修复建议

1. **扩大 `isModelAsset()` 过滤范围**或改为白名单模式
2. **添加下载重试机制**（至少 3 次）
3. **增加超时时间**或根据文件大小动态调整
4. **在 API 失败时通知用户**正在使用 fallback 源
5. **添加文件完整性校验**（如 SHA256，如果 HuggingFace 提供）

---

## 总结

| 问题 | 严重程度 | 修复难度 | 优先级 |
|------|---------|---------|--------|
| 无法取消安装 | 高 | 低 | P0 |
| 模型大小显示错误 | 中 | 低 | P1 |
| 下载无法完成 | 高 | 中 | P0 |

### 建议修复顺序

1. **P0 - 添加取消按钮**：纯 UI 改动，底层逻辑已就绪
2. **P0 - 修复下载可靠性**：
   - 扩展文件过滤器
   - 添加重试机制
   - 增加超时时间
3. **P1 - 修正模型大小**：更新 `modelSizeMB` 字段为实际文件大小

---

## 附录：相关文件清单

| 文件 | 用途 |
|------|------|
| `shared/src/commonMain/kotlin/com/echoic/shared/download/DownloadManager.kt` | 下载管理器 |
| `shared/src/commonMain/kotlin/com/echoic/shared/installer/ModelInstaller.kt` | 安装管理器 |
| `shared/src/commonMain/kotlin/com/echoic/shared/download/DownloadConfig.kt` | 下载配置和文件列表 |
| `composeApp/src/commonMain/kotlin/com/echoic/app/ProvidersScreen.kt` | UI 界面 |
| `shared/src/commonMain/kotlin/com/echoic/shared/model/LocalTTSProvider.kt` | Provider 定义（含 modelSizeMB） |
| `shared/src/commonMain/kotlin/com/echoic/shared/model/LocalTTSModel.kt` | Model 定义（含 modelSizeMB） |
