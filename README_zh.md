# Echoic

[English](README.md)

跨平台 AI 语音合成桌面应用，基于 Kotlin Multiplatform + Compose Desktop 构建。

[![Build macOS DMG](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml/badge.svg)](https://github.com/XiaBiXiang/Echoic/actions/workflows/build-dmg.yml)
[![Latest Release](https://img.shields.io/github/v/release/XiaBiXiang/Echoic)](https://github.com/XiaBiXiang/Echoic/releases/latest)

## 功能特性

- **云端 TTS** — 集成 11 家云端语音合成 API，一键生成高质量语音
- **本地模型** — 支持下载和运行本地 TTS 模型，离线可用
- **多格式输出** — 支持 MP3、WAV、OPUS、FLAC、AAC 格式
- **中英双语** — UI 支持中文和英文切换
- **深色模式** — 支持亮色/暗色主题

## 云端提供商

| 提供商 | 说明 |
|--------|------|
| OpenAI | TTS-1 / TTS-1 HD |
| Google Cloud | WaveNet / Neural2 |
| Azure | Microsoft Neural Voices |
| ElevenLabs | 超逼真 AI 语音 |
| Baidu | 百度语音合成 |
| Tencent | 腾讯云语音合成 |
| Aliyun | 阿里云 NLS 语音合成 |
| Fish Audio | 高质量语音合成 |
| MiniMax | Speech-01 |
| Zhipu AI | 智谱 GLM TTS（OpenAI 兼容） |
| Volcano Engine | 火山引擎（字节跳动） |
| Edge TTS | 免费 Microsoft 语音 |

## 本地模型

| 模型 | 大小 | 说明 |
|------|------|------|
| Kokoro | 115 MB | 轻量级 ONNX 神经网络 TTS |
| Sherpa-ONNX | 115 MB | 跨平台语音工具包 |
| VoxCPM | 4700 MB | 零样本声音克隆 |
| VibeVoice | 3200 MB | 多语言 TTS |

## 技术栈

- Kotlin 2.1 + Kotlin Multiplatform
- Compose Desktop (Material 3)
- Ktor HTTP 客户端
- kotlinx.serialization
- Sherpa-ONNX 本地 TTS
- JVM — macOS / Windows / Linux

## 使用方式

1. 进入 **Providers** 页面配置 API Key
2. 在 **Generate** 页面选择云端提供商或本地模型
3. 输入文本，点击生成即可获得语音

## 项目结构

```
echoic-kmp/
├── composeApp/          # Compose Desktop UI
│   └── src/
│       ├── commonMain/  # 共享 UI 代码
│       └── desktopMain/ # JVM 桌面平台实现
├── shared/              # 业务逻辑模块
│   └── src/
│       ├── commonMain/  # TTS 服务、模型定义、下载管理
│       └── desktopMain/ # JVM 平台实现、Sherpa-ONNX JNI
└── docs/                # 文档
```

## 许可证

MIT License
