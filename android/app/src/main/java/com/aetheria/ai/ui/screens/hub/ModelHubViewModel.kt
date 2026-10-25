package com.aetheria.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetheria.ai.data.local.dao.CustomModelDao
import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.data.model.DefaultOnDemandCatalog
import com.aetheria.ai.data.model.DownloadStatus
import com.aetheria.ai.data.model.OnDemandModel
import com.aetheria.ai.data.network.CloudApiClient
import com.aetheria.ai.data.network.ModelDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelHubViewModel(
    private val customModelDao: CustomModelDao,
    private val modelDownloader: ModelDownloader,
    val cloudApiClient: CloudApiClient
) : ViewModel() {

    private val _onDemandModels = MutableStateFlow<List<OnDemandModel>>(
        DefaultOnDemandCatalog.models.map { model ->
            if (modelDownloader.isModelDownloaded(model)) {
                val file = modelDownloader.getLocalModelFile(model)
                model.copy(
                    status = DownloadStatus.DOWNLOADED,
                    downloadProgress = 1f,
                    downloadedBytes = if (model.isBuiltIn) 0L else file.length(),
                    totalBytes = if (model.isBuiltIn) 0L else file.length(),
                    localFilePath = file.absolutePath
                )
            } else {
                model
            }
        }
    )
    val onDemandModels: StateFlow<List<OnDemandModel>> = _onDemandModels.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val onlineModels: StateFlow<List<CustomModelEntity>> = combine(
        customModelDao.getAllModels(),
        _searchQuery,
        _selectedCategory
    ) { models, query, category ->
        models.filter { model ->
            val matchesQuery = query.isBlank() ||
                model.name.contains(query, ignoreCase = true) ||
                model.modelId.contains(query, ignoreCase = true) ||
                model.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" ||
                (category == "⭐ Favorites" && model.isFavorite) ||
                model.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalModelCount: StateFlow<Int> = customModelDao.getAllModels()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _testResults = MutableStateFlow<Map<String, Pair<Boolean, String>>>(emptyMap())
    val testResults: StateFlow<Map<String, Pair<Boolean, String>>> = _testResults.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun downloadOnDemandModel(model: OnDemandModel) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                updateOnDemandModelInList(
                    model.copy(status = DownloadStatus.DOWNLOADING, downloadProgress = 0f, errorMessage = null)
                )
            }

            modelDownloader.downloadModel(model) { progress, downloaded, total ->
                withContext(Dispatchers.Main) {
                    updateOnDemandModelInList(
                        model.copy(
                            status = DownloadStatus.DOWNLOADING,
                            downloadProgress = progress,
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            errorMessage = null
                        )
                    )
                }
            }.onSuccess { file ->
                withContext(Dispatchers.Main) {
                    updateOnDemandModelInList(
                        model.copy(
                            status = DownloadStatus.DOWNLOADED,
                            downloadProgress = 1f,
                            downloadedBytes = file.length(),
                            totalBytes = file.length(),
                            localFilePath = file.absolutePath,
                            errorMessage = null
                        )
                    )
                }
            }.onFailure { err ->
                val errorMsg = err.localizedMessage ?: err.message ?: "Download failed"
                withContext(Dispatchers.Main) {
                    updateOnDemandModelInList(
                        model.copy(
                            status = DownloadStatus.ERROR,
                            downloadProgress = 0f,
                            errorMessage = errorMsg
                        )
                    )
                }
            }
        }
    }

    fun refreshModelStatus() {
        val updatedList = _onDemandModels.value.map { model ->
            if (modelDownloader.isModelDownloaded(model)) {
                val file = modelDownloader.getLocalModelFile(model)
                model.copy(
                    status = DownloadStatus.DOWNLOADED,
                    downloadProgress = 1f,
                    downloadedBytes = if (model.isBuiltIn) 0L else file.length(),
                    totalBytes = if (model.isBuiltIn) 0L else file.length(),
                    localFilePath = file.absolutePath
                )
            } else {
                model
            }
        }
        _onDemandModels.value = updatedList
    }

    fun cancelDownload(model: OnDemandModel) {
        modelDownloader.cancelDownload(model.id)
        updateOnDemandModelInList(
            model.copy(
                status = DownloadStatus.NOT_DOWNLOADED,
                downloadProgress = 0f,
                errorMessage = "Download canceled"
            )
        )
    }

    fun deleteOnDemandModel(model: OnDemandModel) {
        viewModelScope.launch {
            modelDownloader.deleteModel(model)
            withContext(Dispatchers.Main) {
                updateOnDemandModelInList(
                    model.copy(
                        status = DownloadStatus.NOT_DOWNLOADED,
                        downloadProgress = 0f,
                        downloadedBytes = 0L,
                        localFilePath = null,
                        errorMessage = null
                    )
                )
            }
        }
    }

    private fun updateOnDemandModelInList(updated: OnDemandModel) {
        val list = _onDemandModels.value.toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            list[index] = updated
            _onDemandModels.value = list
        }
    }

    fun addCustomModel(model: CustomModelEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val count = customModelDao.getModelCount()
            if (count >= 100) {
                onResult(false, "Model limit reached (Maximum 100 cloud models supported).")
                return@launch
            }
            customModelDao.insertModel(model)
            onResult(true, "Model \"${model.name}\" added successfully!")
        }
    }

    fun toggleFavorite(model: CustomModelEntity) {
        viewModelScope.launch {
            customModelDao.updateModel(model.copy(isFavorite = !model.isFavorite))
        }
    }

    fun deleteCustomModel(model: CustomModelEntity) {
        viewModelScope.launch {
            customModelDao.deleteModel(model)
        }
    }

    fun testConnection(model: CustomModelEntity) {
        viewModelScope.launch {
            val result = cloudApiClient.testConnection(model)
            val current = _testResults.value.toMutableMap()
            current[model.id] = result
            _testResults.value = current
        }
    }
}
