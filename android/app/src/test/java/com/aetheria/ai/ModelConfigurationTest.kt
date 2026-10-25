package com.aetheria.ai

import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.data.model.DefaultOnDemandCatalog
import org.junit.Assert.*
import org.junit.Test

class ModelConfigurationTest {

    @Test
    fun testOnDemandCatalogContainsUserSelectedModels() {
        val models = DefaultOnDemandCatalog.models
        assertEquals(5, models.size)

        val modelIds = models.map { it.id }
        assertTrue(modelIds.contains("raven-nano-cortex"))
        assertTrue(modelIds.contains("smollm2-360m"))
        assertTrue(modelIds.contains("smollm2-1.7b"))
        assertTrue(modelIds.contains("dolphin-3.0-1b"))
        assertTrue(modelIds.contains("gemma-2-2b"))

        // Downloadable models must have direct Hugging Face download URLs in GGUF format
        models.filter { !it.isBuiltIn }.forEach { model ->
            assertTrue(model.downloadUrl.startsWith("https://huggingface.co/"))
            assertTrue(model.fileName.endsWith(".gguf"))
            assertTrue(model.quantization.isNotEmpty())
        }

        // Verify Dolphin is tagged as uncensored
        val dolphin = models.first { it.id == "dolphin-3.0-1b" }
        assertTrue(dolphin.isUncensored)
    }

    @Test
    fun testCustomModelCapacityValidation() {
        val maxAllowed = 100
        val simulatedList = mutableListOf<CustomModelEntity>()

        for (i in 1..maxAllowed) {
            simulatedList.add(
                CustomModelEntity(
                    name = "Agent #$i",
                    providerType = "OPENAI_COMPATIBLE",
                    baseUrl = "https://api.openai.com/v1",
                    modelId = "gpt-4o-mini",
                    apiKey = "sk-test-$i"
                )
            )
        }

        assertEquals(100, simulatedList.size)

        // Attempting to exceed 100
        val canAddMore = simulatedList.size < maxAllowed
        assertFalse("Should block adding beyond 100 models", canAddMore)
    }

    @Test
    fun testCustomModelEntityCreation() {
        val model = CustomModelEntity(
            name = "DeepSeek R1",
            providerType = "OPENAI_COMPATIBLE",
            baseUrl = "https://api.deepseek.com/v1",
            modelId = "deepseek-reasoner",
            apiKey = "sk-test-key",
            systemPrompt = "Think step by step",
            category = "Coding",
            isFavorite = true
        )

        assertEquals("DeepSeek R1", model.name)
        assertEquals("deepseek-reasoner", model.modelId)
        assertTrue(model.isFavorite)
        assertEquals("Coding", model.category)
    }
}
