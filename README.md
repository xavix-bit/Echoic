# Echoic

[中文](README_zh.md)

Cross-platform AI Text-to-Speech desktop application built with Kotlin Multiplatform + Compose Desktop.

[![Build macOS DMG](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml/badge.svg)](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml)
[![Latest Release](https://img.shields.io/github/v/release/XiaBiXiang/Echoic)](https://github.com/XiaBiXiang/Echoic/releases/latest)

## Features

- **Cloud TTS** — 11 cloud providers integrated, one-click high-quality speech generation
- **Local Models** — Download and run TTS models offline
- **Multi-format Output** — MP3, WAV, OPUS, FLAC, AAC
- **Bilingual UI** — English / Chinese
- **Dark Mode** — Light and dark themes

## Cloud Providers

| Provider | Models |
|----------|--------|
| OpenAI | TTS-1 / TTS-1 HD |
| Google Cloud | WaveNet / Neural2 |
| Azure | Microsoft Neural Voices |
| ElevenLabs | Ultra-realistic AI voices |
| Baidu | Speech synthesis |
| Tencent | Speech synthesis |
| Aliyun | NLS speech synthesis |
| Fish Audio | High-quality synthesis |
| MiniMax | Speech-01 |
| Zhipu AI | GLM TTS (OpenAI-compatible) |
| Volcano Engine | ByteDance TTS |
| Edge TTS | Free Microsoft voices |

## Local Models

| Model | Size | Description |
|-------|------|-------------|
| Kokoro | 115 MB | Lightweight ONNX neural TTS |
| Sherpa-ONNX | 115 MB | Cross-platform speech toolkit |
| VoxCPM | 4700 MB | Zero-shot voice cloning |
| VibeVoice | 3200 MB | Multilingual TTS |

## Tech Stack

- Kotlin 2.1 + Kotlin Multiplatform
- Compose Desktop (Material 3)
- Ktor HTTP client
- kotlinx.serialization
- Sherpa-ONNX for local TTS
- JVM — macOS / Windows / Linux

## Usage

1. Open **Providers** to configure API keys
2. Select a cloud provider or local model in **Generate**
3. Enter text and click generate

## Project Structure

```
echoic-kmp/
├── composeApp/          # Compose Desktop UI
│   └── src/
│       ├── commonMain/  # Shared UI code
│       └── desktopMain/ # JVM platform implementation
├── shared/              # Business logic
│   └── src/
│       ├── commonMain/  # TTS services, models, download manager
│       └── desktopMain/ # JVM platform, Sherpa-ONNX JNI
└── docs/                # Documentation
```

## License

MIT License
