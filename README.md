# Aetheria AI 🌌

> **The Dual-Engine Mobile AI Assistant: 100% Offline On-Device Inference + 100 Cloud Models & Custom Agents**

[![Build and Verify](https://github.com/USER/AetheriaAI/actions/workflows/build_and_release.yml/badge.svg)](https://github.com/USER/AetheriaAI/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688.svg)](https://fastapi.tiangolo.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

---

## 🌟 Highlights

1. **Dual Execution Engine**:
   - **📴 100% Offline Mode**: Zero internet needed. Runs on-device compact GGUF models directly on phone hardware (CPU/GPU) with total privacy and zero API costs.
   - **🌐 Online Cloud Engine**: Connects to frontier intelligence (OpenAI, Gemini, DeepSeek, Groq, Anthropic, or local network Ollama) with real-time token-by-token SSE streaming.
2. **On-Demand Model Hub (Zero Pre-Bundled Bloat)**:
   - Initial app binary stays lightweight (~30MB).
   - Models download directly from Hugging Face CDN on demand:
     - ⚡ **SmolLM2 360M** (~386 MB) — Ultra-fast token responses
     - 🧠 **SmolLM2 1.7B** (~1.06 GB) — Compact general reasoning
     - 🐬 **Dolphin 3.0 1B RP Uncensored** (~810 MB) — Conversational freedom & roleplay
     - 💎 **Gemma 2 2B** (~1.65 GB) — Google DeepMind instruction model
   - One-tap storage manager allows deleting files anytime.
3. **Up to 100 Custom Cloud Models & Agents**:
   - Stored in an on-device Room SQLite database.
   - Real-time search across all 100 models by name, model ID, or persona tag.
   - **"Test Ping" Connection Tester**: Measures real-time latency before saving.
   - 1-Click presets for **DeepSeek**, **Groq**, **Google Gemini**, **OpenAI**, **OpenRouter**, and **Local Ollama**.
4. **Smart Network Monitoring**:
   - Detects network transitions in real-time.
   - When offline (Airplane Mode / No Signal), automatically switches to downloaded local models.

---

## 🏗️ Repository Architecture

```
raven/
├── .github/workflows/
│   └── build_and_release.yml    # Automated CI/CD (Pytest + Android APK build)
├── backend/                     # Python FastAPI Service
│   ├── app/
│   │   ├── api/v1/endpoints/    # REST routes (/models, /chat, /health)
│   │   ├── core/                # Settings & configurations
│   │   ├── schemas/             # Pydantic models
│   │   ├── services/            # Catalog, connection tester, streaming SSE
│   │   └── main.py              # Application entrypoint & CORS
│   ├── tests/                   # Automated pytest suite
│   ├── requirements.txt
│   └── run.py                   # Development runner (0.0.0.0:8000)
└── android/                     # Native Android App (Kotlin + Jetpack Compose)
    ├── app/
    │   ├── src/main/java/com/aetheria/ai/
    │   │   ├── data/            # Room DB, OkHttp SSE Client, Model Downloader
    │   │   ├── domain/          # Offline inference engine
    │   │   ├── ui/              # Compose screens (ChatScreen, ModelHubScreen, Dialogs)
    │   │   └── AetheriaApplication.kt
    │   ├── src/test/            # Android unit tests
    │   └── build.gradle.kts     # App build script (API 35, Jetpack Compose)
    ├── gradle/
    └── build.gradle.kts
```

---

## 🚀 Quick Start Guide

### 1. Running the Backend
```bash
cd backend
python -m pip install -r requirements.txt
python run.py
```
- API Docs: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/api/v1/health`

### 2. Testing the Backend
```bash
python -m pytest backend/tests -v
```

### 3. Building the Android APK
```bash
cd android
./gradlew assembleDebug
```
The compiled APK will be located at:
`android/app/build/outputs/apk/debug/Aetheria-AI-v1.0-debug.apk`

---

## 📜 License
Apache License 2.0
