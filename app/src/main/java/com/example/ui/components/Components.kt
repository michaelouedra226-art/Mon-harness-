package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.local.entity.ToolCallEntity
import com.example.ui.theme.DiffGreen
import com.example.ui.theme.DiffGreenBg
import com.example.ui.theme.DiffRed
import com.example.ui.theme.DiffRedBg
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCodeBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TechCyan

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "executed" -> Triple(DiffGreenBg, DiffGreen, "Exécuté")
        "pending" -> Triple(Color(0xFF78350F), Color(0xFFFDE68A), "En attente")
        "rejected" -> Triple(DiffRedBg, DiffRed, "Rejeté")
        "error" -> Triple(DiffRedBg, DiffRed, "Erreur")
        else -> Triple(Color(0xFF1E293B), TechCyan, "En cours")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ToolCallCard(
    toolCall: ToolCallEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(toolCall.status == "pending") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_call_${toolCall.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Tool",
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toolCall.toolName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    if (!toolCall.targetPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${toolCall.targetPath})",
                            fontSize = 12.sp,
                            color = TechCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = toolCall.status)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Diff Viewer if available
                    if (!toolCall.diffPatch.isNullOrBlank()) {
                        Text(
                            text = "Modifications proposées :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        DiffViewerBox(patch = toolCall.diffPatch)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (!toolCall.argumentsJson.isBlank()) {
                        Text(
                            text = "Paramètres de l'outil :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCodeBg)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = toolCall.argumentsJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TechCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Result info
                    if (!toolCall.resultJson.isNullOrBlank()) {
                        Text(
                            text = "Résultat : ${toolCall.resultJson}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Action buttons in Supervised Mode when pending
                    if (toolCall.status == "pending") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("reject_diff_${toolCall.id}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DiffRed)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Rejeter", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejeter")
                            }
                            Button(
                                onClick = onApprove,
                                modifier = Modifier.testTag("approve_diff_${toolCall.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF022C1A))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Approuver", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approuver")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiffViewerBox(patch: String) {
    val lines = patch.split("\n")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioCodeBg)
            .border(1.dp, StudioCardBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            lines.forEach { line ->
                val (lineBg, textColor) = when {
                    line.startsWith("+") -> Pair(DiffGreenBg.copy(alpha = 0.4f), DiffGreen)
                    line.startsWith("-") -> Pair(DiffRedBg.copy(alpha = 0.4f), DiffRed)
                    else -> Pair(Color.Transparent, Color(0xFFCBD5E1))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lineBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
