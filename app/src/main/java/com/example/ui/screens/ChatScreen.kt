package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.components.ToolCallCard
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCodeBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TechCyan
import com.example.ui.viewmodel.HermesViewModel

import com.example.data.remote.WsConnectionState
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi

@Composable
fun ChatScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.currentProjectMessages.collectAsState()
    val toolCalls by viewModel.currentToolCalls.collectAsState()
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()
    val isPlanMode by viewModel.isPlanMode.collectAsState()
    val isAutoMode by viewModel.isAutoMode.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    val liveStreamingContent by viewModel.liveStreamingContent.collectAsState()
    val liveStreamingMessageId by viewModel.liveStreamingMessageId.collectAsState()
    val isWsMode by viewModel.isWebSocketMode.collectAsState()
    val wsState by viewModel.webSocketManager.connectionState.collectAsState()
    val wsLatency by viewModel.webSocketManager.latencyMs.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages or incoming streaming tokens
    LaunchedEffect(messages.size, isAgentRunning, liveStreamingContent) {
        if (messages.isNotEmpty() || !liveStreamingContent.isNullOrBlank()) {
            val count = messages.size + if (!liveStreamingContent.isNullOrBlank()) 1 else 0
            if (count > 0) {
                listState.animateScrollToItem(count - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .imePadding()
    ) {
        // Control Bar (Mode Toggles + Project Context)
        Surface(
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (selectedProfile != null) NeonEmerald else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedProfile?.let { "${it.name} (${it.selectedModel})" } ?: "Aucun profil configuré",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.size(32.dp).testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Effacer l'historique",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mode Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isPlanMode,
                        onClick = { viewModel.isPlanMode.value = !isPlanMode },
                        label = { Text(if (isPlanMode) "Mode Plan [Actif]" else "Mode Direct", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechCyan.copy(alpha = 0.2f),
                            selectedLabelColor = TechCyan,
                            selectedLeadingIconColor = TechCyan
                        ),
                        modifier = Modifier.testTag("plan_mode_chip")
                    )

                    FilterChip(
                        selected = isAutoMode,
                        onClick = { viewModel.isAutoMode.value = !isAutoMode },
                        label = { Text(if (isAutoMode) "Mode Auto" else "Mode Supervisé (Validation diffs)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = NeonEmerald,
                            selectedLeadingIconColor = NeonEmerald
                        ),
                        modifier = Modifier.testTag("auto_mode_chip")
                    )

                    FilterChip(
                        selected = isWsMode,
                        onClick = { viewModel.isWebSocketMode.value = !isWsMode },
                        label = {
                            Text(
                                if (isWsMode) "Live: WebSocket ${if (wsState == WsConnectionState.CONNECTED) "(${wsLatency ?: 0}ms)" else "[${wsState.name}]"}"
                                else "Live: SSE Streaming (Actif)",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(if (isWsMode) Icons.Default.Wifi else Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (wsState == WsConnectionState.CONNECTED) NeonEmerald.copy(alpha = 0.25f) else TechCyan.copy(alpha = 0.2f),
                            selectedLabelColor = if (wsState == WsConnectionState.CONNECTED) NeonEmerald else TechCyan,
                            selectedLeadingIconColor = if (wsState == WsConnectionState.CONNECTED) NeonEmerald else TechCyan
                        ),
                        modifier = Modifier.testTag("ws_mode_chip")
                    )
                }
            }
        }

        // Messages & Tool calls List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatGreeting(projectName = activeProject?.name ?: "Projet")
                }
            }

            items(messages, key = { it.id }) { msg ->
                // Skip placeholder message if we are actively streaming it live below
                if (msg.id != liveStreamingMessageId || liveStreamingContent.isNullOrBlank()) {
                    ChatMessageItem(message = msg)

                    // Render corresponding tool calls associated with this message
                    val relatedTools = toolCalls.filter { it.messageId == msg.id }
                    relatedTools.forEach { toolCall ->
                        Spacer(modifier = Modifier.height(6.dp))
                        ToolCallCard(
                            toolCall = toolCall,
                            onApprove = { viewModel.approveToolCall(toolCall.id) },
                            onReject = { viewModel.rejectToolCall(toolCall.id) }
                        )
                    }
                }
            }

            // Real-Time Token-by-Token Streaming Bubble
            if (!liveStreamingContent.isNullOrBlank()) {
                item {
                    ChatMessageItem(
                        message = ChatMessageEntity(
                            id = liveStreamingMessageId ?: -1L,
                            projectId = activeProject?.id ?: 0L,
                            role = if (isPlanMode) "plan" else "assistant",
                            content = liveStreamingContent!! + " ▌",
                            isStreaming = true
                        )
                    )
                }
            }

            if (isAgentRunning && liveStreamingContent.isNullOrBlank()) {
                item {
                    AgentThinkingBanner()
                }
            }
        }

        // Quick suggestions carousel
        val suggestions = listOf(
            "Ajoute un Dark Mode toggle dans index.html",
            "Crée un composant TodoList moderne",
            "Améliore le design et les styles CSS",
            "Sauvegarde cette règle en mémoire"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { prompt ->
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.clickable {
                        inputText = prompt
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Input Bar
        Surface(
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text("Demander une modification, création ou audit...", color = Color.Gray, fontSize = 13.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = StudioBackground,
                        unfocusedContainerColor = StudioBackground
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isAgentRunning) {
                            val promptToSend = inputText
                            inputText = ""
                            viewModel.sendUserPrompt(promptToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAgentRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isAgentRunning) NeonEmerald else Color(0xFF1E293B))
                        .testTag("send_prompt_button")
                ) {
                    if (isAgentRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonEmerald,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (inputText.isNotBlank()) Color(0xFF022C1A) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val isPlan = message.role == "plan"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = when {
        isUser -> Color(0xFF1E293B)
        isPlan -> Color(0xFF0C243B)
        else -> StudioSurface
    }
    val borderCol = when {
        isUser -> StudioCardBorder
        isPlan -> TechCyan.copy(alpha = 0.5f)
        else -> StudioCardBorder
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = when {
                    isUser -> Icons.Default.AutoAwesome
                    isPlan -> Icons.AutoMirrored.Filled.ListAlt
                    else -> Icons.Default.Psychology
                },
                contentDescription = null,
                tint = if (isUser) TechCyan else (if (isPlan) TechCyan else NeonEmerald),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    isUser -> "Vous"
                    isPlan -> "Plan Hermes"
                    else -> "Hermes Agent"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }

        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun AgentThinkingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurface)
            .border(1.dp, TechCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = TechCyan,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Hermes analyse le code et exécute les outils autonomes...",
            fontSize = 12.sp,
            color = TechCyan,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun EmptyChatGreeting(projectName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonEmerald.copy(alpha = 0.15f))
                .border(1.dp, NeonEmerald.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Hermes Mobile Agent",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Prêt à coder sur '$projectName'. Demandez la création d'une fonctionnalité ou l'édition de fichiers.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
