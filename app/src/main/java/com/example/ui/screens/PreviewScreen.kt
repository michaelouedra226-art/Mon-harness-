package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCodeBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TechCyan
import com.example.ui.viewmodel.HermesViewModel

data class JsLogItem(
    val level: String,
    val message: String,
    val sourceId: String?,
    val lineNumber: Int
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val reloadKey by viewModel.previewReloadKey.collectAsState()

    var isMobileFrame by remember { mutableStateOf(false) }
    var showConsoleLogs by remember { mutableStateOf(false) }
    val jsLogs = remember { mutableStateListOf<JsLogItem>() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val previewUrl = remember(activeProject, reloadKey) {
        if (activeProject != null) {
            viewModel.workspaceManager.getPreviewUrl(activeProject!!.rootDirectoryName)
        } else {
            "about:blank"
        }
    }

    // Real-Time automatic hot-reload when files are written or changed
    LaunchedEffect(reloadKey) {
        if (reloadKey > 0) {
            webViewRef?.reload()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Preview Header & Viewport switcher
        Surface(
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !isMobileFrame,
                        onClick = { isMobileFrame = false },
                        label = { Text("Plein écran", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechCyan.copy(alpha = 0.2f),
                            selectedLabelColor = TechCyan
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = isMobileFrame,
                        onClick = { isMobileFrame = true },
                        label = { Text("Mobile (375px)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = NeonEmerald
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = NeonEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(NeonEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            jsLogs.clear()
                            webViewRef?.reload()
                        },
                        modifier = Modifier.size(36.dp).testTag("reload_preview_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recharger", tint = NeonEmerald)
                    }
                }
            }
        }

        // WebView Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(if (isMobileFrame) 16.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            val frameModifier = if (isMobileFrame) {
                Modifier
                    .width(375.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            } else {
                Modifier.fillMaxSize()
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE

                        webViewClient = object : WebViewClient() {}
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    jsLogs.add(
                                        JsLogItem(
                                            level = it.messageLevel().name,
                                            message = it.message(),
                                            sourceId = it.sourceId(),
                                            lineNumber = it.lineNumber()
                                        )
                                    )
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }
                        webViewRef = this
                        loadUrl(previewUrl)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    webView.loadUrl(previewUrl)
                },
                modifier = frameModifier
            )
        }

        // Live JS Console Logs Drawer
        Surface(
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConsoleLogs = !showConsoleLogs }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = TechCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Console JS (${jsLogs.size} logs)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = if (showConsoleLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showConsoleLogs) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(StudioCodeBg)
                            .padding(8.dp)
                    ) {
                        if (jsLogs.isEmpty()) {
                            Text("Aucun message console", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            LazyColumn {
                                items(jsLogs) { log ->
                                    val color = when (log.level) {
                                        "ERROR" -> Color(0xFFEF4444)
                                        "WARNING" -> Color(0xFFFBBF24)
                                        else -> NeonEmerald
                                    }
                                    Text(
                                        text = "[${log.level}] ${log.message} (l.${log.lineNumber})",
                                        color = color,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
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
