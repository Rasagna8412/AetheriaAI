package com.aetheria.ai.data.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.aetheria.ai.data.model.DownloadStatus
import com.aetheria.ai.data.model.OnDemandModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    val modelsDir: File
        get() {
            val externalDir = context.getExternalFilesDir("models")
            val targetDir = externalDir ?: File(context.filesDir, "models")
            if (!targetDir.exists()) targetDir.mkdirs()
            return targetDir
        }

    private val _downloadStates = MutableStateFlow<Map<String, OnDemandModel>>(emptyMap())
    val downloadStates: StateFlow<Map<String, OnDemandModel>> = _downloadStates.asStateFlow()

    fun getLocalModelFile(model: OnDemandModel): File {
        return File(modelsDir, model.fileName)
    }

    fun isModelDownloaded(model: OnDemandModel): Boolean {
        if (model.isBuiltIn) return true
        val file = getLocalModelFile(model)
        return file.exists() && file.length() > 10_000_000 // Valid model weights file
    }

    suspend fun downloadModel(
        model: OnDemandModel,
        onProgress: suspend (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (model.isBuiltIn) {
            val dummyFile = File(modelsDir, model.fileName)
            return@withContext Result.success(dummyFile)
        }

        val targetFile = getLocalModelFile(model)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")

        try {
            updateModelState(
                model.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadProgress = 0f,
                    downloadedBytes = 0L,
                    errorMessage = null
                )
            )

            val request = Request.Builder()
                .url(model.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; RavenAI Mobile)")
                .header("Accept", "*/*")
                .build()

            val call = client.newCall(request)
            activeCalls[model.id] = call

            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "Server error (HTTP ${response.code}): ${response.message}"
                    val errorState = model.copy(status = DownloadStatus.ERROR, errorMessage = errorMsg)
                    updateModelState(errorState)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Received empty response from server"))
                val contentLength = body.contentLength()
                var bytesCopied: Long = 0

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int

                var lastUpdateTime = 0L
                var lastReportedProgress = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val progress = if (contentLength > 0) bytesCopied.toFloat() / contentLength else 0f
                    val now = System.currentTimeMillis()

                    if (now - lastUpdateTime > 250 || progress - lastReportedProgress >= 0.01f || progress >= 1f) {
                        lastUpdateTime = now
                        lastReportedProgress = progress
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
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) targetFile.delete()

                    val success = if (!tempFile.renameTo(targetFile)) {
                        // Safe fallback copy
                        tempFile.inputStream().use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        true
                    } else {
                        true
                    }

                    if (success && targetFile.exists()) {
                        val readyState = model.copy(
                            status = DownloadStatus.DOWNLOADED,
                            downloadProgress = 1f,
                            downloadedBytes = targetFile.length(),
                            totalBytes = targetFile.length(),
                            localFilePath = targetFile.absolutePath,
                            errorMessage = null
                        )
                        updateModelState(readyState)
                        return@withContext Result.success(targetFile)
                    } else {
                        return@withContext Result.failure(IOException("Failed to finalize downloaded model file"))
                    }
                } else {
                    return@withContext Result.failure(IOException("Download incomplete: 0 bytes written"))
                }
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()

            val friendlyMsg = when (e) {
                is UnknownHostException -> "No internet connection. Please verify your Wi-Fi or mobile data."
                is SocketTimeoutException -> "Network timed out while downloading. Please retry."
                else -> e.localizedMessage ?: "Download failed: ${e.message}"
            }

            updateModelState(
                model.copy(
                    status = DownloadStatus.ERROR,
                    errorMessage = friendlyMsg
                )
            )
            return@withContext Result.failure(e)
        } finally {
            activeCalls.remove(model.id)
        }
    }

    /**
     * Triggers Android's native system background DownloadManager.
     * Shows a real progress notification in the Android status bar that survives app closing.
     */
    fun enqueueSystemDownload(model: OnDemandModel): Long {
        if (model.isBuiltIn || model.downloadUrl.isBlank()) return -1L
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val targetFile = getLocalModelFile(model)

        val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
            .setTitle("Raven: ${model.name}")
            .setDescription("Downloading Hugging Face weights (${model.sizeFormatted})")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
    }

    suspend fun deleteModel(model: OnDemandModel): Boolean = withContext(Dispatchers.IO) {
        if (model.isBuiltIn) return@withContext false
        val file = getLocalModelFile(model)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            updateModelState(
                model.copy(
                    status = DownloadStatus.NOT_DOWNLOADED,
                    downloadProgress = 0f,
                    downloadedBytes = 0L,
                    localFilePath = null,
                    errorMessage = null
                )
            )
        }
        deleted
    }

    private fun updateModelState(model: OnDemandModel) {
        val current = _downloadStates.value.toMutableMap()
        current[model.id] = model
        _downloadStates.value = current
    }
}
