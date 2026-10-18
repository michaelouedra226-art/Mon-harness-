package com.example.data.remote

import com.example.data.remote.model.ChatCompletionRequest
import com.example.data.remote.model.FunctionCallDto
import com.example.data.remote.model.ModelItem
import com.example.data.remote.model.ModelsResponse
import com.example.data.remote.model.StreamChunk
import com.example.data.remote.model.ToolCallDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class AgentStreamEvent {
    data class ContentChunk(val text: String) : AgentStreamEvent()
    data class ToolCallChunk(val index: Int, val id: String?, val name: String?, val argumentsDelta: String) : AgentStreamEvent()
    data class Complete(val fullContent: String, val toolCalls: List<ToolCallDto>) : AgentStreamEvent()
    data class Error(val message: String) : AgentStreamEvent()
}

class OpenAiClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val modelsAdapter = moshi.adapter(ModelsResponse::class.java)
    private val requestAdapter = moshi.adapter(ChatCompletionRequest::class.java)
    private val chunkAdapter = moshi.adapter(StreamChunk::class.java)

    /**
     * Cleans up baseUrl ensuring proper format without trailing slash.
     */
    private fun sanitizeBaseUrl(baseUrl: String): String {
        var url = baseUrl.trim().trimEnd('/')
        // If user enters host only without /v1, and not custom, let's keep it safe
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    /**
     * Fetches available models from GET /v1/models (or /models)
     */
    suspend fun discoverModels(baseUrl: String, apiKey: String): Result<List<ModelItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeBaseUrl(baseUrl)
            val endpoint = if (cleanUrl.endsWith("/v1")) "$cleanUrl/models" else "$cleanUrl/v1/models"

            val reqBuilder = Request.Builder().url(endpoint).get()
            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body received"))
                val parsed = modelsAdapter.fromJson(body)
                val models = parsed?.data?.sortedBy { it.id } ?: emptyList()
                Result.success(models)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tests latency and connectivity to the configured endpoint.
     */
    suspend fun testConnection(baseUrl: String, apiKey: String): Result<Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = discoverModels(baseUrl, apiKey)
        val elapsed = System.currentTimeMillis() - start
        if (result.isSuccess) {
            Result.success(elapsed)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    /**
     * Performs streaming chat completion SSE.
     */
    fun streamChatCompletion(
        baseUrl: String,
        apiKey: String,
        requestPayload: ChatCompletionRequest
    ): Flow<AgentStreamEvent> = callbackFlow {
        val cleanUrl = sanitizeBaseUrl(baseUrl)
        val endpoint = if (cleanUrl.endsWith("/v1")) "$cleanUrl/chat/completions" else "$cleanUrl/v1/chat/completions"

        val jsonBody = requestAdapter.toJson(requestPayload)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
        }

        var call = client.newCall(reqBuilder.build())

        try {
            val response: Response = call.execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "No error body"
                trySend(AgentStreamEvent.Error("HTTP ${response.code}: $errBody"))
                close()
                return@callbackFlow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                trySend(AgentStreamEvent.Error("Null response stream"))
                close()
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val fullContentBuilder = StringBuilder()
            val toolCallMap = mutableMapOf<Int, MutableToolCall>()

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val data = trimmed.substring(5).trim()
                    if (data == "[DONE]") {
                        break
                    }
                    if (data.isNotEmpty()) {
                        try {
                            val chunk = chunkAdapter.fromJson(data)
                            val choice = chunk?.choices?.firstOrNull()
                            val delta = choice?.delta

                            // Text content
                            val textDelta = delta?.content
                            if (!textDelta.isNullOrEmpty()) {
                                fullContentBuilder.append(textDelta)
                                trySend(AgentStreamEvent.ContentChunk(textDelta))
                            }

                            // Tool calls delta
                            delta?.toolCalls?.forEach { tcDelta ->
                                val index = tcDelta.index
                                val existing = toolCallMap.getOrPut(index) {
                                    MutableToolCall(
                                        id = tcDelta.id ?: "call_$index",
                                        name = tcDelta.function?.name ?: ""
                                    )
                                }
                                if (!tcDelta.id.isNullOrEmpty()) {
                                    existing.id = tcDelta.id
                                }
                                tcDelta.function?.name?.let { fnName ->
                                    if (fnName.isNotEmpty()) existing.name = fnName
                                }
                                tcDelta.function?.arguments?.let { argDelta ->
                                    existing.argumentsBuilder.append(argDelta)
                                    trySend(AgentStreamEvent.ToolCallChunk(
                                        index = index,
                                        id = existing.id,
                                        name = existing.name,
                                        argumentsDelta = argDelta
                                    ))
                                }
                            }
                        } catch (e: Exception) {
                            // Non-fatal parse warning for keep-alive or provider-specific chunk
                        }
                    }
                }
                line = reader.readLine()
            }

            val finalToolCalls = toolCallMap.values.map {
                ToolCallDto(
                    id = it.id,
                    type = "function",
                    function = FunctionCallDto(
                        name = it.name,
                        arguments = it.argumentsBuilder.toString()
                    )
                )
            }

            trySend(AgentStreamEvent.Complete(
                fullContent = fullContentBuilder.toString(),
                toolCalls = finalToolCalls
            ))
            close()
        } catch (e: Exception) {
            trySend(AgentStreamEvent.Error(e.message ?: "Connection error"))
            close()
        }

        awaitClose {
            call.cancel()
        }
    }

    private data class MutableToolCall(
        var id: String,
        var name: String,
        val argumentsBuilder: StringBuilder = StringBuilder()
    )
}
