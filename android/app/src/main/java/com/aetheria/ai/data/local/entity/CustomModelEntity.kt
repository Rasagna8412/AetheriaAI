package com.aetheria.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_models")
data class CustomModelEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val providerType: String, // "OPENAI_COMPATIBLE", "GEMINI", "ANTHROPIC", "CUSTOM"
    val baseUrl: String,
    val modelId: String,
    val apiKey: String,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val category: String = "General", // Coding, Creative, Roleplay, General
    val isFavorite: Boolean = false,
    val latencyMs: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
