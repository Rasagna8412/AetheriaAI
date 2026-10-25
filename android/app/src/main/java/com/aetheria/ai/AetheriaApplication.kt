package com.aetheria.ai

import android.app.Application
import com.aetheria.ai.data.local.AetheriaDatabase
import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.data.network.CloudApiClient
import com.aetheria.ai.data.network.ModelDownloader
import com.aetheria.ai.data.network.NetworkMonitor
import com.aetheria.ai.domain.engine.OfflineInferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AetheriaApplication : Application() {

    val database by lazy { AetheriaDatabase.getDatabase(this) }
    val networkMonitor by lazy { NetworkMonitor(this) }
    val modelDownloader by lazy { ModelDownloader(this) }
    val cloudApiClient by lazy { CloudApiClient() }
    val offlineEngine by lazy { OfflineInferenceEngine() }

    override fun onCreate() {
        super.onCreate()

        // Seed initial popular cloud presets if database is empty
        CoroutineScope(Dispatchers.IO).launch {
            val count = database.customModelDao().getModelCount()
            if (count == 0) {
                database.customModelDao().insertModel(
                    CustomModelEntity(
                        name = "DeepSeek V3 (Reasoning)",
                        providerType = "OPENAI_COMPATIBLE",
                        baseUrl = "https://api.deepseek.com/v1",
                        modelId = "deepseek-chat",
                        apiKey = "",
                        systemPrompt = "You are a concise, helpful assistant.",
                        category = "Coding",
                        isFavorite = true
                    )
                )
                database.customModelDao().insertModel(
                    CustomModelEntity(
                        name = "Groq Llama 3.3 70B",
                        providerType = "OPENAI_COMPATIBLE",
                        baseUrl = "https://api.groq.com/openai/v1",
                        modelId = "llama-3.3-70b-versatile",
                        apiKey = "",
                        systemPrompt = "You are an ultra-fast helpful assistant.",
                        category = "General",
                        isFavorite = true
                    )
                )
                database.customModelDao().insertModel(
                    CustomModelEntity(
                        name = "Google Gemini 2.5 Flash",
                        providerType = "GEMINI",
                        baseUrl = "https://generativelanguage.googleapis.com/v1beta",
                        modelId = "gemini-2.5-flash",
                        apiKey = "",
                        systemPrompt = "You are a helpful assistant.",
                        category = "General",
                        isFavorite = false
                    )
                )
            }
        }
    }
}
