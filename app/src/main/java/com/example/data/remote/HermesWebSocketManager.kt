package com.example.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class WsEvent {
    data class TextDelta(val delta: String) : WsEvent()
    data class ToolCallStart(val callId: String, val name: String, val argsJson: String) : WsEvent()
    data class DiffUpdate(val path: String, val patch: String) : WsEvent()
    data class ToolResult(val callId: String, val status: String, val result: String) : WsEvent()
    data class Status(val status: String) : WsEvent()
    data class RawMessage(val isSent: Boolean, val message: String, val timestamp: Long = System.currentTimeMillis()) : WsEvent()
    data class Error(val error: String) : WsEvent()
}

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

class HermesWebSocketManager(
    private val coroutineScope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val _trafficLogs = MutableStateFlow<List<WsEvent.RawMessage>>(emptyList())
    val trafficLogs: StateFlow<List<WsEvent.RawMessage>> = _trafficLogs.asStateFlow()

    var currentUrl: String = "ws://10.0.2.2:8000/ws"
        private set

    fun connect(url: String) {
        disconnect()
        currentUrl = url.trim()
        _connectionState.value = WsConnectionState.CONNECTING

        val request = try {
            Request.Builder().url(currentUrl).build()
        } catch (e: Exception) {
            _connectionState.value = WsConnectionState.DISCONNECTED
            logMessage(isSent = false, "[Erreur URL] ${e.message}")
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = WsConnectionState.CONNECTED
                logMessage(isSent = false, "[Connecté] WebSocket établi avec $currentUrl")
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                logMessage(isSent = false, text)
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = WsConnectionState.DISCONNECTED
                logMessage(isSent = false, "[Fermeture] Code $code: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = WsConnectionState.DISCONNECTED
                logMessage(isSent = false, "[Échec connexion] ${t.message}")
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        pingJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
        _latencyMs.value = null
    }

    fun sendMessage(json: JSONObject): Boolean {
        val str = json.toString()
        val success = webSocket?.send(str) ?: false
        if (success) {
            logMessage(isSent = true, str)
        }
        return success
    }

    fun sendPrompt(prompt: String, mode: String, model: String) {
        val payload = JSONObject().apply {
            put("type", "prompt")
            put("content", prompt)
            put("mode", mode)
            put("model", model)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(payload)
    }

    fun sendApproval(toolCallId: String, approved: Boolean) {
        val payload = JSONObject().apply {
            put("type", "tool_approval")
            put("tool_call_id", toolCallId)
            put("approved", approved)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(payload)
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "text_delta" -> {
                    val delta = json.optString("delta", "")
                    _events.tryEmit(WsEvent.TextDelta(delta))
                }
                "tool_call_start" -> {
                    val id = json.optString("id", "")
                    val name = json.optString("name", "")
                    val args = json.optString("args", "{}")
                    _events.tryEmit(WsEvent.ToolCallStart(id, name, args))
                }
                "diff" -> {
                    val path = json.optString("path", "")
                    val patch = json.optString("patch", "")
                    _events.tryEmit(WsEvent.DiffUpdate(path, patch))
                }
                "tool_result" -> {
                    val id = json.optString("id", "")
                    val status = json.optString("status", "executed")
                    val result = json.optString("result", "")
                    _events.tryEmit(WsEvent.ToolResult(id, status, result))
                }
                "status" -> {
                    val status = json.optString("status", "")
                    _events.tryEmit(WsEvent.Status(status))
                }
                "pong" -> {
                    val sentTime = json.optLong("timestamp", 0L)
                    if (sentTime > 0) {
                        _latencyMs.value = System.currentTimeMillis() - sentTime
                    }
                }
                else -> {
                    // Generic text
                    if (json.has("content")) {
                        _events.tryEmit(WsEvent.TextDelta(json.optString("content")))
                    }
                }
            }
        } catch (e: Exception) {
            // Raw text fallback
            _events.tryEmit(WsEvent.TextDelta(text))
        }
    }

    private fun startHeartbeat() {
        pingJob?.cancel()
        pingJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive && _connectionState.value == WsConnectionState.CONNECTED) {
                val pingMsg = JSONObject().apply {
                    put("type", "ping")
                    put("timestamp", System.currentTimeMillis())
                }
                sendMessage(pingMsg)
                delay(3000)
            }
        }
    }

    private fun scheduleReconnect() {
        if (_connectionState.value == WsConnectionState.DISCONNECTED) return
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch(Dispatchers.IO) {
            _connectionState.value = WsConnectionState.RECONNECTING
            delay(4000)
            if (isActive && _connectionState.value == WsConnectionState.RECONNECTING) {
                connect(currentUrl)
            }
        }
    }

    private fun logMessage(isSent: Boolean, message: String) {
        val item = WsEvent.RawMessage(isSent, message)
        coroutineScope.launch(Dispatchers.Main) {
            val current = _trafficLogs.value.toMutableList()
            if (current.size > 80) current.removeAt(0)
            current.add(item)
            _trafficLogs.value = current
        }
    }
}
