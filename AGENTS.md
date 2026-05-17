# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Run

```bash
# Run desktop app
./gradlew :composeApp:run

# Run tests
./gradlew :shared:desktopTest

# Run a single test class
./gradlew :shared:desktopTest --tests "com.echoic.shared.download.DownloadConfigTest"

# Package
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageMsi    # Windows
./gradlew :composeApp:packageDeb    # Linux
```

Requires JDK 17+. Gradle wrapper (`./gradlew`) is bundled. Uses Aliyun Maven mirrors for dependency resolution.

## Architecture

Kotlin Multiplatform (JVM-only desktop for now) + Compose Desktop app for cross-platform TTS.

### Two modules

- **`:shared`** — Business logic: TTS services, engine, model definitions, download manager, config, platform abstraction
- **`:composeApp`** — Compose Desktop UI: screens, navigation, theme, localization. Depends on `:shared`

### TTS Engine layering

```
TTSEngine (interface)            ← unified API: synthesize(text, model, voice, format) → ByteArray
├── CloudTTSEngine               ← delegates to TTSService via TTSServiceFactory
│   └── TTSService (per-provider) ← each cloud provider implements this (OpenAI, Azure, Baidu, etc.)
│       └── BaseTTSService        ← abstract base with readAudioResponse() helper
└── LocalTTSEngine               ← expect/actual pattern, desktop uses Sherpa-ONNX JNI
    └── DesktopLocalEngine        ← actual implementation in shared/desktopMain
```

- `TTSEngineFactory` (shared) creates engines. `TTSEngineFactoryDesktop` (desktopMain) provides `createLocalEngineImpl()` via `expect/actual`.
- `TTSServiceFactory` maps `TTSProvider` enum → concrete `TTSService` subclass.
- All cloud services follow: `BaseTTSService(httpClient, apiKey, baseURL)` → override `synthesize()`.

### Data model

- `TTSProvider` enum — cloud providers (12 entries: OpenAI, Google, Azure, ElevenLabs, Baidu, Tencent, Aliyun, Fish Audio, MiniMax, Zhipu, Volcengine, EdgeTTS). Each carries metadata (defaultBaseURL, tags, display info).
- `LocalTTSProvider` enum — on-device providers (Kokoro, Sherpa, VoxCPM, VibeVoice) with download mirrors, platform support, GPU requirements.
- `TTSModel` / `Voice` — enums keyed by provider. `LocalTTSModel` for local models.
- `AudioFormat` enum — MP3, WAV, OPUS, FLAC, AAC.
- `TTSTag` enum — filter tags (HIGH_QUALITY, CHINESE, OFFLINE, etc.).

### Platform abstraction

`shared/commonMain/platform/Platform.kt` defines `expect` declarations for cross-platform I/O:
- `PlatformFile`, `PlatformInputStream/OutputStream`, `PlatformRandomAccessFile`, `PlatformZipInputStream`
- Desktop actuals in `shared/desktopMain/platform/Platform.jvm.kt` wrap `java.io.*`

### Config

`AppConfig` reads/writes `AppConfigData` (serializable data class) as JSON to `~/.config/echoic/config.json` (desktop). Stores API keys, base URLs, appearance, language, default providers.

### Download system

`DownloadManager` handles multi-file downloads with resume support, progress tracking via `StateFlow<DownloadState>`, dynamic size estimation, retry with backoff. `DownloadConfig` parses HuggingFace URLs. `ModelInstaller` orchestrates download + extraction + verification.

### UI structure

- `main.kt` (desktopMain) — entry point, creates `AppConfig`, Compose window (undecorated, transparent, macOS traffic lights)
- `App.kt` — root composable. Creates engines/audio player via `remember`. Three screens: `HOME`, `GENERATE`, `PROVIDERS`. Floating `SettingsOverlay`.
- `Sidebar.kt` — collapsible sidebar navigation.
- `Localization.kt` — `Strings` data class + English/Chinese instances. Accessed via `LocalStrings` composition local.
- `Theme.kt` — Material 3 theme with light/dark variants.

### Sherpa-ONNX local TTS

JNI wrapper classes in `shared/desktopMain/kotlin/com/k2fsa/sherpa/onnx/` mirror the native library API. **These must match the native library version exactly** — the native code uses JNI reflection (`GetObjectClass`/`GetFieldID`) to read fields by name and type. Missing fields cause SIGSEGV crashes.

Key classes: `OfflineTts`, `OfflineTtsConfig`, `OfflineTtsModelConfig` (contains 7 model type configs: vits, matcha, kokoro, zipvoice, kitten, pocket, supertonic), `GeneratedAudio`, `GenerationConfig`.

Native libraries (`libonnxruntime.*.dylib`, `libsherpa-onnx-jni.dylib`) are bundled in `shared/libs/sherpa-onnx-native-lib-osx-aarch64-v*.jar`. `NativeLibLoader` extracts them to `~/.echoic/native/` on first use. When upgrading the native JAR, delete `~/.echoic/native/` to force re-extraction.

The current model `vits-zh-hf-fanchen-unity.onnx` is Chinese-only — English text produces silence/noise (all words become OOV).

### Audio playback

`DesktopAudioPlayer` (desktop actual of `AudioPlayer`) uses `javax.sound.sampled.Clip` for WAV and `SourceDataLine` for streaming. Audio from sherpa-onnx is 16kHz mono 16-bit PCM, wrapped in WAV via `floatArrayToWav()`.

### Stats

`UsageStatsManager` + `StatsStorage` (expect/actual) track usage stats. Desktop uses `DesktopStatsStorage` with file-based persistence.

## Key conventions

- **expect/actual** for all platform-specific code (file I/O, base64, time, local engine)
- **No DI framework** — engines/config created in `App.kt` and passed as parameters
- **kotlinx.serialization** for JSON — models use `@Serializable`
- **Ktor HttpClient** for all HTTP — cloud engines share one client with ContentNegotiation + WebSockets
- **StateFlow** for reactive state (download progress, etc.)
- **Comments in both English and Chinese** — existing code mixes both; match the surrounding style

## Agent skills

### Issue tracker

GitHub Issues via `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
