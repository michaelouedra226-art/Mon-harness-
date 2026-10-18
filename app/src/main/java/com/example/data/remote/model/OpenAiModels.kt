package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModelsResponse(
    @Json(name = "data") val data: List<ModelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelItem(
    @Json(name = "id") val id: String,
    @Json(name = "owned_by") val ownedBy: String? = null,
    @Json(name = "created") val created: Long? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenAiMessageDto>,
    @Json(name = "tools") val tools: List<ToolDefinitionDto>? = null,
    @Json(name = "stream") val stream: Boolean = true,
    @Json(name = "temperature") val temperature: Double = 0.2
)

@JsonClass(generateAdapter = true)
data class OpenAiMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "tool_call_id") val toolCallId: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<ToolCallDto>? = null
)

@JsonClass(generateAdapter = true)
data class ToolDefinitionDto(
    @Json(name = "type") val type: String = "function",
    @Json(name = "function") val function: FunctionSpecDto
)

@JsonClass(generateAdapter = true)
data class FunctionSpecDto(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class ToolCallDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String = "function",
    @Json(name = "function") val function: FunctionCallDto
)

@JsonClass(generateAdapter = true)
data class FunctionCallDto(
    @Json(name = "name") val name: String,
    @Json(name = "arguments") val arguments: String
)

// Streaming Chunk DTOs
@JsonClass(generateAdapter = true)
data class StreamChunk(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<StreamChoice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StreamChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "delta") val delta: StreamDelta? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamDelta(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<StreamToolCallDelta>? = null
)

@JsonClass(generateAdapter = true)
data class StreamToolCallDelta(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "id") val id: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "function") val function: StreamFunctionDelta? = null
)

@JsonClass(generateAdapter = true)
data class StreamFunctionDelta(
    @Json(name = "name") val name: String? = null,
    @Json(name = "arguments") val arguments: String? = null
)
