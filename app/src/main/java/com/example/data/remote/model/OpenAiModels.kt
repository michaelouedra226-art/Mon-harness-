package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModelsResponse(
    @param:Json(name = "data") val data: List<ModelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelItem(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "owned_by") val ownedBy: String? = null,
    @param:Json(name = "created") val created: Long? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @param:Json(name = "model") val model: String,
    @param:Json(name = "messages") val messages: List<OpenAiMessageDto>,
    @param:Json(name = "tools") val tools: List<ToolDefinitionDto>? = null,
    @param:Json(name = "stream") val stream: Boolean = true,
    @param:Json(name = "temperature") val temperature: Double = 0.2
)

@JsonClass(generateAdapter = true)
data class OpenAiMessageDto(
    @param:Json(name = "role") val role: String,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "tool_call_id") val toolCallId: String? = null,
    @param:Json(name = "tool_calls") val toolCalls: List<ToolCallDto>? = null
)

@JsonClass(generateAdapter = true)
data class ToolDefinitionDto(
    @param:Json(name = "type") val type: String = "function",
    @param:Json(name = "function") val function: FunctionSpecDto
)

@JsonClass(generateAdapter = true)
data class FunctionSpecDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "parameters") val parameters: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class ToolCallDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "type") val type: String = "function",
    @param:Json(name = "function") val function: FunctionCallDto
)

@JsonClass(generateAdapter = true)
data class FunctionCallDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "arguments") val arguments: String
)

// Streaming Chunk DTOs
@JsonClass(generateAdapter = true)
data class StreamChunk(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "choices") val choices: List<StreamChoice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StreamChoice(
    @param:Json(name = "index") val index: Int = 0,
    @param:Json(name = "delta") val delta: StreamDelta? = null,
    @param:Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamDelta(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "tool_calls") val toolCalls: List<StreamToolCallDelta>? = null
)

@JsonClass(generateAdapter = true)
data class StreamToolCallDelta(
    @param:Json(name = "index") val index: Int = 0,
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "function") val function: StreamFunctionDelta? = null
)

@JsonClass(generateAdapter = true)
data class StreamFunctionDelta(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "arguments") val arguments: String? = null
)
