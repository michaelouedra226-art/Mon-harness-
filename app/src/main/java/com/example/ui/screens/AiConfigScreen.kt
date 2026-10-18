package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.remote.WsConnectionState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AiProfileEntity
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCodeBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TechCyan
import com.example.ui.viewmodel.HermesViewModel

@Composable
fun AiConfigScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.allProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val discoveredModels by viewModel.discoveredModels.collectAsState()
    val isDiscovering by viewModel.isDiscoveringModels.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var profileName by remember(selectedProfile) { mutableStateOf(selectedProfile?.name ?: "") }
    var baseUrl by remember(selectedProfile) { mutableStateOf(selectedProfile?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember(selectedProfile) { mutableStateOf(selectedProfile?.apiKey ?: "") }
    var selectedModel by remember(selectedProfile) { mutableStateOf(selectedProfile?.selectedModel ?: "gpt-4o") }
    var isDefault by remember(selectedProfile) { mutableStateOf(selectedProfile?.isDefault ?: false) }
    var isApiKeyMasked by remember { mutableStateOf(true) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Text(
            text = "Configuration des Fournisseurs IA",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Text(
            text = "Compatible avec tout serveur OpenAI : OpenAI, Ollama, OpenRouter, Groq, LM Studio, vLLM.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Profiles Carousel / Switcher
        Text(
            text = "Profils Enregistrés",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TechCyan,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            profiles.forEach { profile ->
                val isCurrent = selectedProfile?.id == profile.id
                Surface(
                    color = if (isCurrent) StudioSurfaceVariant else StudioSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrent) NeonEmerald else StudioCardBorder
                    ),
                    modifier = Modifier
                        .clickable { viewModel.selectProfile(profile) }
                        .testTag("profile_pill_${profile.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) NeonEmerald else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        if (profiles.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteProfile(profile) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // New profile button
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.clickable {
                    profileName = "Nouveau Profil"
                    baseUrl = "https://api.openai.com/v1"
                    apiKey = ""
                    selectedModel = "gpt-4o"
                    isDefault = false
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouveau", fontSize = 12.sp, color = NeonEmerald)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Helper Buttons
        Text(
            text = "Préréglages rapides d'endpoints :",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                Pair("OpenAI Cloud", "https://api.openai.com/v1"),
                Pair("Ollama (10.0.2.2:11434)", "http://10.0.2.2:11434/v1"),
                Pair("OpenRouter", "https://openrouter.ai/api/v1"),
                Pair("Groq Cloud", "https://api.groq.com/openai/v1"),
                Pair("LM Studio (10.0.2.2:1234)", "http://10.0.2.2:1234/v1")
            )
            presets.forEach { (label, url) ->
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.clickable {
                        baseUrl = url
                        if (label.contains("Ollama")) {
                            apiKey = "ollama"
                            selectedModel = "qwen2.5-coder:latest"
                        }
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Edit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Nom du profil") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (compatible OpenAI)") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = TechCyan) },
                    modifier = Modifier.fillMaxWidth().testTag("base_url_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )

                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (conservée localement sur l'appareil)") },
                    placeholder = { Text("sk-...") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonEmerald) },
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyMasked = !isApiKeyMasked }) {
                            Icon(
                                imageVector = if (isApiKeyMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Afficher/Masquer",
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (isApiKeyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth().testTag("api_key_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )

                // Actions: Discover Models & Test Connection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.discoverModels(baseUrl, apiKey) },
                        modifier = Modifier.weight(1f).testTag("discover_models_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TechCyan)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TechCyan, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Découvrir modèles", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.testConnection(baseUrl, apiKey) },
                        modifier = Modifier.weight(1f).testTag("test_connection_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald)
                    ) {
                        if (connectionStatus.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonEmerald, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test connexion", fontSize = 11.sp)
                    }
                }

                // Connection status badge
                if (connectionStatus.isSuccess != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (connectionStatus.isSuccess == true) Color(0xFF064E3B) else Color(0xFF7F1D1D))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (connectionStatus.isSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (connectionStatus.isSuccess == true) NeonEmerald else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionStatus.message ?: "",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // Model Selection (with dropdown from discovered models)
                Box {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        label = { Text("Modèle sélectionné") },
                        trailingIcon = {
                            IconButton(onClick = { modelDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir", tint = NeonEmerald)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("selected_model_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = StudioCardBorder
                        )
                    )

                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        if (discoveredModels.isEmpty()) {
                            val defaultSuggestions = listOf("gpt-4o", "gpt-4o-mini", "claude-3-5-sonnet", "qwen2.5-coder:latest", "deepseek-coder", "llama-3.3-70b-versatile")
                            defaultSuggestions.forEach { modelName ->
                                DropdownMenuItem(
                                    text = { Text(modelName) },
                                    onClick = {
                                        selectedModel = modelName
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        } else {
                            discoveredModels.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.id) },
                                    onClick = {
                                        selectedModel = item.id
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Default toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Définir comme profil par défaut au lancement", color = Color.LightGray, fontSize = 12.sp)
                }

                // Save Profile Button
                Button(
                    onClick = {
                        viewModel.saveProfile(
                            id = selectedProfile?.id ?: 0L,
                            name = profileName.ifBlank { "Mon Profil" },
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            selectedModel = selectedModel,
                            isDefault = isDefault
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF022C1A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // WebSocket Real-Time Backend Section
        val wsState by viewModel.webSocketManager.connectionState.collectAsState()
        val wsLatency by viewModel.webSocketManager.latencyMs.collectAsState()
        val isWsMode by viewModel.isWebSocketMode.collectAsState()
        val trafficLogs by viewModel.webSocketManager.trafficLogs.collectAsState()
        var wsUrl by remember { mutableStateOf(viewModel.webSocketManager.currentUrl) }
        var showTrafficLogs by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (wsState == WsConnectionState.CONNECTED) NeonEmerald else StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Passerelle WebSocket Temps Réel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Protocole bidirectionnel pour agents distants",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Status Pill
                    val (badgeBg, badgeText, label) = when (wsState) {
                        WsConnectionState.CONNECTED -> Triple(Color(0xFF064E3B), NeonEmerald, "Connecté (${wsLatency ?: 0}ms)")
                        WsConnectionState.CONNECTING, WsConnectionState.RECONNECTING -> Triple(Color(0xFF78350F), TechCyan, "Connexion...")
                        WsConnectionState.DISCONNECTED -> Triple(Color(0xFF1E293B), Color.Gray, "Déconnecté")
                    }
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    label = { Text("URL du serveur WebSocket") },
                    placeholder = { Text("ws://10.0.2.2:8000/ws ou wss://...") },
                    modifier = Modifier.fillMaxWidth().testTag("ws_url_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (wsState == WsConnectionState.CONNECTED) {
                        OutlinedButton(
                            onClick = { viewModel.webSocketManager.disconnect() },
                            modifier = Modifier.weight(1f).testTag("ws_disconnect_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Déconnecter WebSocket", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.webSocketManager.connect(wsUrl) },
                            modifier = Modifier.weight(1f).testTag("ws_connect_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color(0xFF003644))
                        ) {
                            Text("Connecter WebSocket", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showTrafficLogs = !showTrafficLogs },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text(if (showTrafficLogs) "Masquer logs" else "Trames (${trafficLogs.size})", fontSize = 12.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isWsMode,
                        onCheckedChange = { viewModel.isWebSocketMode.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prioriser WebSocket sur SSE pour le chat et les tool calls",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                // Traffic Log Viewer
                if (showTrafficLogs) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioCodeBg)
                            .padding(8.dp)
                    ) {
                        if (trafficLogs.isEmpty()) {
                            Text("Aucune trame WebSocket échangée", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            LazyColumn {
                                items(trafficLogs.reversed()) { log ->
                                    val prefix = if (log.isSent) "▲ OUT:" else "▼ IN :"
                                    val col = if (log.isSent) TechCyan else NeonEmerald
                                    Text(
                                        text = "$prefix ${log.message}",
                                        color = col,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 2,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
