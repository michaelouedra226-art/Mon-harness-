package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.workspace.WorkspaceFile
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCodeBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TechCyan
import com.example.ui.viewmodel.HermesViewModel

@Composable
fun WorkspaceScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val files by viewModel.workspaceFiles.collectAsState()
    val openedFilePath by viewModel.openedFilePath.collectAsState()
    val openedFileContent by viewModel.openedFileContent.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    // Zip Export & Import Activity Launchers
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportProjectZip(it) }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importProjectZip(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Workspace Header / Project Selector
        Surface(
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { projectDropdownExpanded = true }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = NeonEmerald)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeProject?.name ?: "Sélectionner un projet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                        }

                        DropdownMenu(
                            expanded = projectDropdownExpanded,
                            onDismissRequest = { projectDropdownExpanded = false }
                        ) {
                            allProjects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    onClick = {
                                        viewModel.selectProject(proj)
                                        projectDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Nouveau Projet...", color = NeonEmerald) },
                                onClick = {
                                    projectDropdownExpanded = false
                                    showNewProjectDialog = true
                                }
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { viewModel.refreshWorkspaceFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNewFileDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald),
                        modifier = Modifier.testTag("new_file_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nouveau fichier", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val name = "${activeProject?.rootDirectoryName ?: "project"}.zip"
                            exportZipLauncher.launch(name)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TechCyan),
                        modifier = Modifier.testTag("export_zip_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export ZIP", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        modifier = Modifier.testTag("import_zip_btn")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import ZIP", fontSize = 12.sp)
                    }
                }
            }
        }

        // Main content: either File Editor or File List
        if (openedFilePath != null) {
            CodeEditorView(
                filePath = openedFilePath!!,
                initialContent = openedFileContent,
                onSave = { updated -> viewModel.saveOpenedFile(updated) },
                onClose = { viewModel.closeOpenedFile() }
            )
        } else {
            // Files Tree View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Arborescence des fichiers (${files.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucun fichier dans ce projet. Créez-en un ou demandez à Hermes !", color = Color.Gray)
                        }
                    }
                }

                items(files, key = { it.relativePath }) { file ->
                    WorkspaceFileRow(
                        file = file,
                        onClick = {
                            if (!file.isDirectory) {
                                viewModel.openFile(file.relativePath)
                            }
                        },
                        onDelete = {
                            viewModel.deleteWorkspaceFile(file.relativePath)
                        }
                    )
                }
            }
        }
    }

    // Dialog: New Project
    if (showNewProjectDialog) {
        var newProjName by remember { mutableStateOf("") }
        var newProjDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Créer un nouveau projet", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("Nom du projet") },
                        modifier = Modifier.fillMaxWidth().testTag("new_project_name_field")
                    )
                    OutlinedTextField(
                        value = newProjDesc,
                        onValueChange = { newProjDesc = it },
                        label = { Text("Description courte (optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjName.isNotBlank()) {
                            viewModel.createProject(newProjName, newProjDesc)
                            showNewProjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF022C1A)),
                    modifier = Modifier.testTag("confirm_create_project_btn")
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = StudioSurface
        )
    }

    // Dialog: New File
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Nouveau fichier", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Chemin relatif (ex: script.js, css/theme.css)") },
                    modifier = Modifier.fillMaxWidth().testTag("new_file_name_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createWorkspaceFile(newFileName)
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF022C1A))
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = StudioSurface
        )
    }
}

@Composable
fun WorkspaceFileRow(
    file: WorkspaceFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("file_row_${file.relativePath}"),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (file.isDirectory) TechCyan else NeonEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = file.relativePath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    if (!file.isDirectory) {
                        Text(
                            text = "${file.sizeBytes} octets",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CodeEditorView(
    filePath: String,
    initialContent: String,
    onSave: (String) -> Unit,
    onClose: () -> Unit
) {
    var editorContent by remember(filePath, initialContent) { mutableStateOf(initialContent) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioCodeBg)
    ) {
        // Editor Top Bar
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
                    Icon(Icons.Default.Code, contentDescription = null, tint = TechCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = filePath,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Row {
                    Button(
                        onClick = { onSave(editorContent) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF022C1A)),
                        modifier = Modifier.height(34.dp).testTag("save_file_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enregistrer", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fermer", fontSize = 12.sp)
                    }
                }
            }
        }

        // Monospace text editor
        OutlinedTextField(
            value = editorContent,
            onValueChange = { editorContent = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("code_editor_textarea"),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = StudioCodeBg,
                unfocusedContainerColor = StudioCodeBg
            )
        )
    }
}
