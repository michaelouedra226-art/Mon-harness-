package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AiConfigScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MemorySkillsScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TechCyan
import com.example.ui.viewmodel.HermesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HermesApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesApp(viewModel: HermesViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userNotice by viewModel.userNotice.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val openedFilePath by viewModel.openedFilePath.collectAsState()

    // Handle Android hardware/gesture back press when in file editor
    BackHandler(enabled = openedFilePath != null) {
        viewModel.closeOpenedFile()
    }

    // Display transient notifications
    LaunchedEffect(userNotice) {
        userNotice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.userNotice.value = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = "HERMES",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = NeonEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Column {
                            Text(
                                text = activeProject?.name ?: "Hermes Mobile",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedProfile?.let { "${it.name} • ${it.selectedModel}" } ?: "Non configuré",
                                fontSize = 11.sp,
                                color = TechCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    val isWs by viewModel.isWebSocketMode.collectAsState()
                    val wsState by viewModel.webSocketManager.connectionState.collectAsState()
                    val wsLatency by viewModel.webSocketManager.latencyMs.collectAsState()

                    Surface(
                        color = if (isWs) {
                            if (wsState == com.example.data.remote.WsConnectionState.CONNECTED) Color(0xFF064E3B) else Color(0xFF78350F)
                        } else Color(0xFF0C243B),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isWs && wsState == com.example.data.remote.WsConnectionState.CONNECTED) NeonEmerald.copy(alpha = 0.5f) else TechCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isWs && wsState == com.example.data.remote.WsConnectionState.CONNECTED) NeonEmerald else TechCyan
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isWs) {
                                    if (wsState == com.example.data.remote.WsConnectionState.CONNECTED) "WS ${wsLatency ?: 0}ms" else "WS OFF"
                                } else "LIVE SSE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWs && wsState == com.example.data.remote.WsConnectionState.CONNECTED) NeonEmerald else TechCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = StudioCardBorder
                    )
                    .testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    NavigationItem(0, "Agent", Icons.Default.Terminal, "nav_agent"),
                    NavigationItem(1, "Fichiers", Icons.Default.Folder, "nav_files"),
                    NavigationItem(2, "Preview", Icons.Default.PlayArrow, "nav_preview"),
                    NavigationItem(3, "Mémoire", Icons.Default.Psychology, "nav_memory"),
                    NavigationItem(4, "Modèles", Icons.Default.Settings, "nav_settings")
                )

                navItems.forEach { item ->
                    val isSelected = selectedTab == item.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = item.index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF022C1A),
                            selectedTextColor = NeonEmerald,
                            indicatorColor = NeonEmerald,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBackground)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> WorkspaceScreen(viewModel = viewModel)
                2 -> PreviewScreen(viewModel = viewModel)
                3 -> MemorySkillsScreen(viewModel = viewModel)
                4 -> AiConfigScreen(viewModel = viewModel)
            }
        }
    }
}

private data class NavigationItem(
    val index: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
