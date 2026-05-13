# Echoic

Cross-platform AI Text-to-Speech (TTS) desktop application built with Kotlin Multiplatform + Compose Desktop.

跨平台 AI 语音合成（TTS）桌面应用，基于 Kotlin Multiplatform + Compose Desktop 构建。

[![Build macOS DMG](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml/badge.svg)](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml)
[![Latest Release](https://img.shields.io/github/v/release/XiaBiXiang/Echoic)](https://github.com/XiaBiXiang/Echoic/releases/latest)

## Features

- **Cloud TTS** — Integrated with 11 cloud providers, generate high-quality speech with one click
- **Local Models** — Download and run TTS models offline
- **Multi-format Output** — MP3, WAV, OPUS, FLAC, AAC
- **Bilingual UI** — English / Chinese interface
- **Dark Mode** — Light and dark theme support

## 功能特性

- **云端 TTS** — 集成 11 家云端语音合成 API，一键生成高质量语音
- **本地模型** — 支持下载和运行本地 TTS 模型，离线可用
- **多格式输出** — 支持 MP3、WAV、OPUS、FLAC、AAC 格式
- **中英双语** — UI 支持中文和英文切换
- **深色模式** — 支持亮色/暗色主题

## Cloud Providers

| Provider | Models |
|----------|--------|
| OpenAI | TTS-1 / TTS-1 HD |
| Google Cloud | WaveNet / Neural2 |
| Azure | Microsoft Neural Voices |
| ElevenLabs | Ultra-realistic AI voices |
| Baidu | 百度语音合成 |
| Tencent | 腾讯云语音合成 |
| Aliyun | 阿里云 NLS |
| Fish Audio | High-quality synthesis |
| MiniMax | Speech-01 |
| Zhipu AI | GLM TTS (OpenAI-compatible) |
| Volcano Engine | ByteDance TTS |

## Local Models

| Model | Size | Description |
|-------|------|-------------|
| Piper | 60 MB | Lightweight ONNX neural TTS |
| Sherpa-ONNX | 115 MB | Cross-platform speech toolkit |
| eSpeak NG | 10 MB | Multilingual formant synthesis |
| VoxCPM | 4700 MB | Zero-shot voice cloning |
| CosyVoice | 3200 MB | Multilingual TTS (Alibaba) |
| ChatTTS | 1200 MB | Conversational speech |
| GPT-SoVITS | 2000 MB | Few-shot voice cloning |

## Tech Stack

- Kotlin 2.1 + Kotlin Multiplatform
- Compose Desktop (Material 3)
- Ktor HTTP client
- kotlinx.serialization
- JVM — macOS / Windows / Linux

## Getting Started

Download the latest DMG from [Releases](https://github.com/XiaBiXiang/Echoic/releases), or build from source:

```bash
git clone https://github.com/XiaBiXiang/Echoic.git
cd Echoic
./gradlew :composeApp:run
```

Requires JDK 17+.

## 使用方式

1. 启动应用后，进入 **Providers** 页面配置 API Key
2. 在 **Generate** 页面选择云端提供商或本地模型
3. 输入文本，点击生成即可获得语音

## Usage

1. Go to **Providers** to configure your API keys
2. Select a cloud provider or local model in **Generate**
3. Enter text and click generate

## Project Structure

```
echoic-kmp/
├── composeApp/          # Compose Desktop UI
│   └── src/
│       ├── commonMain/  # Shared UI code
│       └── desktopMain/ # JVM platform implementation
├── shared/              # Business logic module
│   └── src/
│       ├── commonMain/  # TTS services, model definitions, download manager
│       └── desktopMain/ # JVM platform (audio, Sherpa-ONNX JNI)
└── docs/                # Documentation
```

## License

MIT License
