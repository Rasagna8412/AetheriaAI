package com.aetheria.ai.data.network

import android.content.Context
import android.os.PowerManager
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
        .readTimeout(0, TimeUnit.MILLISECONDS) // Unlimited chunk streaming timeout
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    val modelsDir: File
        get() {
            val externalDir = context.getExternalFilesDir("models")
            val targetDir = externalDir ?: File(context.filesDir, "models")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
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
        return file.exists() && file.length() > 5_000_000 // Valid model file (>5MB)
    }

    suspend fun downloadModel(
        model: OnDemandModel,
        onProgress: suspend (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (model.isBuiltIn) {
            val dummyFile = File(modelsDir, model.fileName)
            return@withContext Result.success(dummyFile)
        }

        // Acquire partial WakeLock so Android doesn't kill Wi-Fi when screen turns off
        val powerManager = try { context.getSystemService(Context.POWER_SERVICE) as? PowerManager } catch (_: Exception) { null }
        val wakeLock = try { powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Raven::Downloader") } catch (_: Exception) { null }
        try { wakeLock?.acquire(30 * 60 * 1000L) } catch (_: Exception) { }

        val targetFile = getLocalModelFile(model)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")
        tempFile.parentFile?.mkdirs()

        // Check if there is an existing partial download to resume
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        try {
            updateModelState(
                model.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadProgress = if (existingBytes > 0 && model.totalBytes > 0) existingBytes.toFloat() / model.totalBytes else 0f,
                    downloadedBytes = existingBytes,
                    errorMessage = null
                )
            )

            val requestBuilder = Request.Builder()
                .url(model.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; RavenAI Mobile)")
                .header("Accept", "*/*")

            // Enable HTTP Range Resume if partial file exists
            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val request = requestBuilder.build()
            val call = client.newCall(request)
            activeCalls[model.id] = call

            call.execute().use { response ->
                val isPartial = response.code == 206
                val isSuccess = response.isSuccessful

                if (!isSuccess && !isPartial) {
                    val errorMsg = "Server error (HTTP ${response.code}): ${response.message}"
                    updateModelState(model.copy(status = DownloadStatus.ERROR, errorMessage = errorMsg))
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Empty response body from Hugging Face"))
                val contentLength = body.contentLength()
                val totalExpectedBytes = if (isPartial) existingBytes + contentLength else contentLength

                var bytesCopied = if (isPartial) existingBytes else 0L
                val appendMode = isPartial && existingBytes > 0

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile, appendMode)

                val buffer = ByteArray(64 * 1024) // 64KB high-speed buffer
                var bytesRead: Int

                var lastUpdateTime = 0L
                var lastReportedProgress = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val progress = if (totalExpectedBytes > 0) bytesCopied.toFloat() / totalExpectedBytes else 0f
                    val now = System.currentTimeMillis()

                    if (now - lastUpdateTime > 200 || progress - lastReportedProgress >= 0.01f || progress >= 1f) {
                        lastUpdateTime = now
                        lastReportedProgress = progress
                        onProgress(progress, bytesCopied, totalExpectedBytes)
                        updateModelState(
                            model.copy(
                                status = DownloadStatus.DOWNLOADING,
                                downloadProgress = progress,
                                downloadedBytes = bytesCopied,
                                totalBytes = totalExpectedBytes
                            )
                        )
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) targetFile.delete()

                    val finalized = if (!tempFile.renameTo(targetFile)) {
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

                    if (finalized && targetFile.exists()) {
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
            val friendlyMsg = when (e) {
                is UnknownHostException -> "No internet connection. Please verify your Wi-Fi or data."
                is SocketTimeoutException -> "Network timed out. Tap 'Resume' to continue downloading."
                else -> e.localizedMessage ?: "Download interrupted: ${e.message}"
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
            try {
                if (wakeLock != null && wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (_: Exception) { }
        }
    }

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
    }

    suspend fun deleteModel(model: OnDemandModel): Boolean = withContext(Dispatchers.IO) {
        if (model.isBuiltIn) return@withContext false
        val file = getLocalModelFile(model)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")

        if (tempFile.exists()) tempFile.delete()
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
