package com.aetheria.ai.data.network

import android.content.Context
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

    // OkHttpClient configured specifically for multi-gigabyte CDN streaming
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)   // 0 means no timeout while streaming chunks
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    val modelsDir: File
        get() {
            // Prioritize external app storage for gigabytes of free disk space
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
        val file = getLocalModelFile(model)
        // Hugging Face GGUF models are always > 50 MB
        return file.exists() && file.length() > 50_000_000
    }

    suspend fun downloadModel(
        model: OnDemandModel,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
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
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; RavenAI)")
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

                // High-performance 64KB buffer
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int

                var lastUpdateTime = 0L
                var lastReportedProgress = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val progress = if (contentLength > 0) bytesCopied.toFloat() / contentLength else 0f
                    val now = System.currentTimeMillis()

                    // Throttle updates to at most once every 200ms or 1% delta to prevent Compose UI freeze
                    if (now - lastUpdateTime > 200 || progress - lastReportedProgress >= 0.01f || progress >= 1f) {
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
                    if (tempFile.renameTo(targetFile)) {
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
                is UnknownHostException -> "No internet connection. Please connect to Wi-Fi."
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

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
    }

    suspend fun deleteModel(model: OnDemandModel): Boolean = withContext(Dispatchers.IO) {
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
