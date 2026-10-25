package com.aetheria.ai.data.model

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}

data class OnDemandModel(
    val id: String,
    val name: String,
    val sourceRepo: String,
    val fileName: String,
    val downloadUrl: String,
    val quantization: String,
    val sizeFormatted: String,
    val ramRequired: String,
    val category: String,
    val description: String,
    val isUncensored: Boolean = false,
    val isBuiltIn: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val localFilePath: String? = null,
    val errorMessage: String? = null
)

object DefaultOnDemandCatalog {
    val models = listOf(
        OnDemandModel(
            id = "raven-nano-cortex",
            name = "⚡ Raven Nano Cortex (Built-in)",
            sourceRepo = "Raven Embedded Core",
            fileName = "raven_nano_embedded.bin",
            downloadUrl = "",
            quantization = "Embedded INT4",
            sizeFormatted = "Built-in (0 MB)",
            ramRequired = "0.5 GB",
            category = "Instant Offline",
            description = "Instant on-device intelligence ready immediately out of the box. Zero download required, works 100% offline anywhere.",
            isUncensored = false,
            isBuiltIn = true,
            status = DownloadStatus.DOWNLOADED,
            downloadProgress = 1f
        ),
        OnDemandModel(
            id = "smollm2-360m",
            name = "SmolLM2 360M Instruct",
            sourceRepo = "HuggingFaceTB/SmolLM2-360M-Instruct-GGUF",
            fileName = "smollm2-360m-instruct-q8_0.gguf",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
            quantization = "Q8_0",
            sizeFormatted = "386 MB",
            ramRequired = "1.5 GB",
            category = "Ultra-Fast",
            description = "Lightning-fast responses on any Android phone. Compact reasoning and instruction following.",
            isUncensored = false,
            isBuiltIn = false
        ),
        OnDemandModel(
            id = "smollm2-1.7b",
            name = "SmolLM2 1.7B Instruct",
            sourceRepo = "HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF",
            fileName = "smollm2-1.7b-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            quantization = "Q4_K_M",
            sizeFormatted = "1.06 GB",
            ramRequired = "3.0 GB",
            category = "General Reasoning",
            description = "High accuracy reasoning, coding, and problem-solving in a lightweight footprint.",
            isUncensored = false,
            isBuiltIn = false
        ),
        OnDemandModel(
            id = "dolphin-3.0-1b",
            name = "Dolphin 3.0 1B RP Uncensored",
            sourceRepo = "bartowski/Dolphin3.0-Llama3.2-1B-GGUF",
            fileName = "Dolphin3.0-Llama3.2-1B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Dolphin3.0-Llama3.2-1B-GGUF/resolve/main/Dolphin3.0-Llama3.2-1B-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            sizeFormatted = "810 MB",
            ramRequired = "2.0 GB",
            category = "Roleplay / Uncensored",
            description = "Unfiltered conversational freedom and creative roleplay fine-tuned on Llama 3.2 1B.",
            isUncensored = true,
            isBuiltIn = false
        ),
        OnDemandModel(
            id = "gemma-2-2b",
            name = "Gemma 2 2B Instruct",
            sourceRepo = "bartowski/gemma-2-2b-it-GGUF",
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            sizeFormatted = "1.65 GB",
            ramRequired = "3.5 GB",
            category = "Deep Reasoning",
            description = "Google DeepMind's state-of-the-art architecture with advanced instruction following.",
            isUncensored = false,
            isBuiltIn = false
        )
    )
}
