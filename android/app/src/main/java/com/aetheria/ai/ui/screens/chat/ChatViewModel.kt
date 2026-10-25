package com.aetheria.ai.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetheria.ai.data.local.dao.ChatDao
import com.aetheria.ai.data.local.dao.CustomModelDao
import com.aetheria.ai.data.local.entity.ChatMessageEntity
import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.data.model.DefaultOnDemandCatalog
import com.aetheria.ai.data.model.OnDemandModel
import com.aetheria.ai.data.network.CloudApiClient
import com.aetheria.ai.data.network.ModelDownloader
import com.aetheria.ai.data.network.NetworkMonitor
import com.aetheria.ai.domain.engine.OfflineInferenceEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class SelectedModel {
    data class Offline(val model: OnDemandModel) : SelectedModel()
    data class Online(val model: CustomModelEntity) : SelectedModel()
}

class ChatViewModel(
    private val chatDao: ChatDao,
    private val customModelDao: CustomModelDao,
    private val networkMonitor: NetworkMonitor,
    private val modelDownloader: ModelDownloader,
    private val cloudApiClient: CloudApiClient,
    private val offlineEngine: OfflineInferenceEngine
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val currentSessionId = MutableStateFlow(UUID.randomUUID().toString())

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = currentSessionId
        .flatMapLatest { sessionId -> chatDao.getMessagesForSession(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customModels: StateFlow<List<CustomModelEntity>> = customModelDao
        .getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Defaults to the built-in instant nano model so the app works immediately out of the box!
    private val _selectedModel = MutableStateFlow<SelectedModel>(
        SelectedModel.Offline(DefaultOnDemandCatalog.models[0])
    )
    val selectedModel: StateFlow<SelectedModel> = _selectedModel.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    fun selectModel(model: SelectedModel) {
        _selectedModel.value = model
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        val userMessage = ChatMessageEntity(
            sessionId = currentSessionId.value,
            role = "user",
            content = content.trim(),
            modelUsed = when (val m = _selectedModel.value) {
                is SelectedModel.Offline -> m.model.name
                is SelectedModel.Online -> m.model.name
            },
            isOffline = _selectedModel.value is SelectedModel.Offline
        )

        viewModelScope.launch {
            chatDao.insertMessage(userMessage)
            _isGenerating.value = true
            _currentStreamingText.value = ""

            when (val model = _selectedModel.value) {
                is SelectedModel.Offline -> {
                    val file = modelDownloader.getLocalModelFile(model.model)

                    // Check if model needs download and is missing
                    if (!model.model.isBuiltIn && !modelDownloader.isModelDownloaded(model.model)) {
                        val helperMsg = "⚠️ **${model.model.name}** is not downloaded yet.\n\n• Open the **Model Hub** (top-right icon) to download it (${model.model.sizeFormatted}).\n• Or switch to **⚡ Raven Nano Cortex (Built-in)** in the top dropdown for instant offline chat!"
                        val assistantMessage = ChatMessageEntity(
                            sessionId = currentSessionId.value,
                            role = "assistant",
                            content = helperMsg,
                            modelUsed = model.model.name,
                            isOffline = true
                        )
                        chatDao.insertMessage(assistantMessage)
                        _isGenerating.value = false
                        return@launch
                    }

                    val responseBuffer = StringBuilder()

                    offlineEngine.runOfflineInference(model.model, file, content)
                        .catch { e ->
                            _currentStreamingText.value = "Error: ${e.message}"
                            _isGenerating.value = false
                        }
                        .collect { token ->
                            responseBuffer.append(token)
                            _currentStreamingText.value = responseBuffer.toString()
                        }

                    val assistantMessage = ChatMessageEntity(
                        sessionId = currentSessionId.value,
                        role = "assistant",
                        content = responseBuffer.toString(),
                        modelUsed = model.model.name,
                        isOffline = true
                    )
                    chatDao.insertMessage(assistantMessage)
                    _isGenerating.value = false
                    _currentStreamingText.value = ""
                }

                is SelectedModel.Online -> {
                    if (!isOnline.value) {
                        val offlineMsg = "📴 **Device is currently Offline**.\n\nPlease connect to Wi-Fi/data, or switch to an Offline model from the top dropdown."
                        val assistantMessage = ChatMessageEntity(
                            sessionId = currentSessionId.value,
                            role = "assistant",
                            content = offlineMsg,
                            modelUsed = model.model.name,
                            isOffline = true
                        )
                        chatDao.insertMessage(assistantMessage)
                        _isGenerating.value = false
                        return@launch
                    }

                    if (model.model.apiKey.isBlank()) {
                        val keyMsg = "⚠️ **API Key Required** for ${model.model.name}.\n\nPlease open the **Model Hub**, tap edit on this model, and paste your API key to chat with cloud models."
                        val assistantMessage = ChatMessageEntity(
                            sessionId = currentSessionId.value,
                            role = "assistant",
                            content = keyMsg,
                            modelUsed = model.model.name,
                            isOffline = false
                        )
                        chatDao.insertMessage(assistantMessage)
                        _isGenerating.value = false
                        return@launch
                    }

                    val responseBuffer = StringBuilder()
                    val chatHistory = listOf(Pair("user", content))

                    cloudApiClient.streamChatCompletion(model.model, chatHistory)
                        .catch { e ->
                            _currentStreamingText.value = "Cloud Streaming Error: ${e.message}"
                            _isGenerating.value = false
                        }
                        .collect { token ->
                            responseBuffer.append(token)
                            _currentStreamingText.value = responseBuffer.toString()
                        }

                    val assistantMessage = ChatMessageEntity(
                        sessionId = currentSessionId.value,
                        role = "assistant",
                        content = responseBuffer.toString(),
                        modelUsed = model.model.name,
                        isOffline = false
                    )
                    chatDao.insertMessage(assistantMessage)
                    _isGenerating.value = false
                    _currentStreamingText.value = ""
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.clearSession(currentSessionId.value)
        }
    }
}
