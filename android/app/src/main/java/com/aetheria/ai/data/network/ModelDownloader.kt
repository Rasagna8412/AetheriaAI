package com.aetheria.ai.data.network

import android.content.Context
import com.aetheria.ai.data.model.DownloadStatus
import com.aetheria.ai.data.model.OnDemandModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelsDir: File
        get() {
            val dir = File(context.filesDir, "models")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val _downloadStates = MutableStateFlow<Map<String, OnDemandModel>>(emptyMap())
    val downloadStates: StateFlow<Map<String, OnDemandModel>> = _downloadStates.asStateFlow()

    fun getLocalModelFile(model: OnDemandModel): File {
        return File(modelsDir, model.fileName)
    }

    fun isModelDownloaded(model: OnDemandModel): Boolean {
        val file = getLocalModelFile(model)
        return file.exists() && file.length() > 1_000_000 // greater than 1MB
    }

    suspend fun downloadModel(
        model: OnDemandModel,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getLocalModelFile(model)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")

        try {
            updateModelState(model.copy(status = DownloadStatus.DOWNLOADING, downloadProgress = 0f))

            val request = Request.Builder()
                .url(model.downloadUrl)
                .header("User-Agent", "Aetheria-AI-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorState = model.copy(status = DownloadStatus.ERROR)
                    updateModelState(errorState)
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
                val contentLength = body.contentLength()
                var bytesCopied: Long = 0

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val progress = if (contentLength > 0) bytesCopied.toFloat() / contentLength else 0f
                    onProgress(progress, bytesCopied, contentLength)

                    updateModelState(
                        model.copy(
                            status = DownloadStatus.DOWNLOADING,
                            downloadProgress = progress,
                            downloadedBytes = bytesCopied,
                            totalBytes = contentLength
                        )
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.renameTo(targetFile)) {
                    val readyState = model.copy(
                        status = DownloadStatus.DOWNLOADED,
                        downloadProgress = 1f,
                        localFilePath = targetFile.absolutePath
                    )
                    updateModelState(readyState)
                    return@withContext Result.success(targetFile)
                } else {
                    return@withContext Result.failure(Exception("Failed to rename temporary model file"))
                }
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            updateModelState(model.copy(status = DownloadStatus.ERROR))
            return@withContext Result.failure(e)
        }
    }

    suspend fun deleteModel(model: OnDemandModel): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalModelFile(model)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            updateModelState(model.copy(status = DownloadStatus.NOT_DOWNLOADED, downloadProgress = 0f, localFilePath = null))
        }
        deleted
    }

    private fun updateModelState(model: OnDemandModel) {
        val current = _downloadStates.value.toMutableMap()
        current[model.id] = model
        _downloadStates.value = current
    }
}
