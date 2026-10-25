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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
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

    val messages: StateFlow<List<ChatMessageEntity>> = chatDao
        .getMessagesForSession(currentSessionId.value)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customModels: StateFlow<List<CustomModelEntity>> = customModelDao
        .getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    if (!modelDownloader.isModelDownloaded(model.model)) {
                        val helperMsg = "⚠️ **${model.model.name}** is not downloaded to this device yet.\n\nTo use this model 100% offline:\n1. Open the **Model Hub** (tap the Hub icon in top right).\n2. Tap **Download Model** (${model.model.sizeFormatted}).\n\n*(Or select a Cloud Model from the top dropdown to chat online right now!)*"
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

                    val file = modelDownloader.getLocalModelFile(model.model)
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
                        _currentStreamingText.value = "Device is currently offline. Please connect to internet or switch to an Offline model."
                        _isGenerating.value = false
                        return@launch
                    }

                    if (model.model.apiKey.isBlank()) {
                        val helperMsg = "⚠️ API Key required for **${model.model.name}**.\n\nPlease open the **Model Hub**, edit this model, and enter your API key to chat with cloud models."
                        val assistantMessage = ChatMessageEntity(
                            sessionId = currentSessionId.value,
                            role = "assistant",
                            content = helperMsg,
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
