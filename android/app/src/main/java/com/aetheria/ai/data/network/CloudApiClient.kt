package com.aetheria.ai.data.network

import com.aetheria.ai.data.local.entity.CustomModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(client)

    suspend fun testConnection(model: CustomModelEntity): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val isGemini = model.providerType.equals("GEMINI", ignoreCase = true)
            val request = if (isGemini) {
                val url = "${model.baseUrl.trimEnd('/')}/models/${model.modelId}:generateContent?key=${model.apiKey}"
                val json = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", "ping")))
                    }))
                    put("generationConfig", JSONObject().put("maxOutputTokens", 5))
                }
                Request.Builder()
                    .url(url)
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
            } else {
                val url = "${model.baseUrl.trimEnd('/')}/chat/completions"
                val json = JSONObject().apply {
                    put("model", model.modelId)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    }))
                    put("max_tokens", 5)
                }
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer ${model.apiKey}")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
            }

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Pair(true, "Connected in ${elapsed}ms")
                } else {
                    val body = response.body?.string() ?: ""
                    Pair(false, "HTTP ${response.code}: ${body.take(120)}")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Connection failed")
        }
    }

    fun streamChatCompletion(
        model: CustomModelEntity,
        messages: List<Pair<String, String>> // role to content
    ): Flow<String> = callbackFlow {
        val isGemini = model.providerType.equals("GEMINI", ignoreCase = true)

        val request = if (isGemini) {
            val url = "${model.baseUrl.trimEnd('/')}/models/${model.modelId}:streamGenerateContent?alt=sse&key=${model.apiKey}"
            val contentsArray = JSONArray()
            messages.forEach { (role, text) ->
                val geminiRole = if (role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }
            val json = JSONObject().apply {
                put("contents", contentsArray)
                if (model.systemPrompt.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", model.systemPrompt)))
                    })
                }
                put("generationConfig", JSONObject().apply {
                    put("temperature", model.temperature.toDouble())
                    put("maxOutputTokens", model.maxTokens)
                })
            }
            Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
        } else {
            val url = "${model.baseUrl.trimEnd('/')}/chat/completions"
            val messagesArray = JSONArray()
            if (model.systemPrompt.isNotBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", model.systemPrompt)
                })
            }
            messages.forEach { (role, content) ->
                messagesArray.put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            val json = JSONObject().apply {
                put("model", model.modelId)
                put("messages", messagesArray)
                put("temperature", model.temperature.toDouble())
                put("max_tokens", model.maxTokens)
                put("stream", true)
            }
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${model.apiKey}")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
        }

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val jsonObj = JSONObject(data)
                    if (isGemini) {
                        val candidates = jsonObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotEmpty()) trySend(text)
                            }
                        }
                    } else {
                        val choices = jsonObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content")
                            if (!content.isNullOrEmpty()) {
                                trySend(content)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (t != null) {
                    trySend("\n[Error: ${t.message}]")
                }
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = sseFactory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}
