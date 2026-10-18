package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiProfileEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.ToolCallEntity
import com.example.data.remote.HermesWebSocketManager
import com.example.data.remote.WsConnectionState
import com.example.data.remote.WsEvent
import com.example.data.remote.OpenAiClient
import com.example.data.remote.model.ModelItem
import com.example.data.workspace.WorkspaceFile
import com.example.data.workspace.WorkspaceManager
import com.example.engine.HermesOrchestrator
import com.example.engine.OrchestratorEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConnectionStatus(
    val isTesting: Boolean = false,
    val isSuccess: Boolean? = null,
    val latencyMs: Long? = null,
    val message: String? = null
)

class HermesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val workspaceManager = WorkspaceManager(application)
    private val openAiClient = OpenAiClient()
    val webSocketManager = HermesWebSocketManager(viewModelScope)
    private val orchestrator = HermesOrchestrator(database, workspaceManager, openAiClient)

    // Real-Time Streaming & WebSocket State
    val isWebSocketMode = MutableStateFlow(false)
    val liveStreamingContent = MutableStateFlow<String?>(null)
    val liveStreamingMessageId = MutableStateFlow<Long?>(null)

    // Profiles
    val allProfiles: StateFlow<List<AiProfileEntity>> = database.aiProfileDao().getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfile = MutableStateFlow<AiProfileEntity?>(null)
    val selectedProfile: StateFlow<AiProfileEntity?> = _selectedProfile.asStateFlow()

    // Projects
    val allProjects: StateFlow<List<ProjectEntity>> = database.projectDao().getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Chat Messages for active project
    private val _currentProjectMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentProjectMessages: StateFlow<List<ChatMessageEntity>> = _currentProjectMessages.asStateFlow()

    // Tool calls for active project
    private val _currentToolCalls = MutableStateFlow<List<ToolCallEntity>>(emptyList())
    val currentToolCalls: StateFlow<List<ToolCallEntity>> = _currentToolCalls.asStateFlow()

    // Workspace Files for active project
    private val _workspaceFiles = MutableStateFlow<List<WorkspaceFile>>(emptyList())
    val workspaceFiles: StateFlow<List<WorkspaceFile>> = _workspaceFiles.asStateFlow()

    // Currently opened file in Code Editor
    private val _openedFilePath = MutableStateFlow<String?>(null)
    val openedFilePath: StateFlow<String?> = _openedFilePath.asStateFlow()

    private val _openedFileContent = MutableStateFlow<String>("")
    val openedFileContent: StateFlow<String> = _openedFileContent.asStateFlow()

    // Memory & Skills
    val allMemories: StateFlow<List<MemoryItemEntity>> = database.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkills: StateFlow<List<SkillEntity>> = database.skillDao().getAllSkills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Agent Controls
    val isPlanMode = MutableStateFlow(true)
    val isAutoMode = MutableStateFlow(false)
    val isAgentRunning = MutableStateFlow(false)

    // Model Discovery & Ping
    val discoveredModels = MutableStateFlow<List<ModelItem>>(emptyList())
    val isDiscoveringModels = MutableStateFlow(false)
    val connectionStatus = MutableStateFlow(ConnectionStatus())

    // Preview Reload Signal
    val previewReloadKey = MutableStateFlow(0)

    // User feedback / snackbar
    val userNotice = MutableStateFlow<String?>(null)

    init {
        // Observe initial default profile
        viewModelScope.launch {
            val defaultProf = database.aiProfileDao().getDefaultProfile()
            if (defaultProf != null) {
                _selectedProfile.value = defaultProf
            } else {
                val list = database.aiProfileDao().getAllProfiles().firstOrNull()
                _selectedProfile.value = list?.firstOrNull()
            }
        }

        // Observe initial active project
        viewModelScope.launch {
            val list = database.projectDao().getAllProjects().firstOrNull()
            if (!list.isNullOrEmpty()) {
                selectProject(list.first())
            }
        }

        // Observe Real-Time WebSocket events
        viewModelScope.launch {
            webSocketManager.events.collect { wsEvent ->
                val proj = _activeProject.value ?: return@collect
                when (wsEvent) {
                    is WsEvent.TextDelta -> {
                        val current = liveStreamingContent.value ?: ""
                        liveStreamingContent.value = current + wsEvent.delta
                    }
                    is WsEvent.ToolCallStart -> {
                        val assistantId = liveStreamingMessageId.value ?: 0L
                        val entity = ToolCallEntity(
                            projectId = proj.id,
                            messageId = assistantId,
                            callId = wsEvent.callId,
                            toolName = wsEvent.name,
                            argumentsJson = wsEvent.argsJson,
                            status = "pending"
                        )
                        database.toolCallDao().insertToolCall(entity)
                    }
                    is WsEvent.DiffUpdate -> {
                        refreshWorkspaceFiles()
                        userNotice.value = "Diff temps réel reçu pour: ${wsEvent.path}"
                    }
                    is WsEvent.ToolResult -> {
                        refreshWorkspaceFiles()
                    }
                    is WsEvent.Status -> {
                        if (wsEvent.status == "completed") {
                            isAgentRunning.value = false
                            liveStreamingContent.value = null
                            liveStreamingMessageId.value = null
                            refreshWorkspaceFiles()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun selectProfile(profile: AiProfileEntity) {
        _selectedProfile.value = profile
        discoveredModels.value = emptyList()
        connectionStatus.value = ConnectionStatus()
    }

    fun saveProfile(
        id: Long = 0,
        name: String,
        baseUrl: String,
        apiKey: String,
        selectedModel: String,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            val entity = AiProfileEntity(
                id = id,
                name = name.trim(),
                baseUrl = baseUrl.trim(),
                apiKey = apiKey.trim(),
                selectedModel = selectedModel.trim(),
                isDefault = isDefault
            )
            val savedId = database.aiProfileDao().insertProfile(entity)
            if (isDefault) {
                database.aiProfileDao().setDefaultProfile(if (id == 0L) savedId else id)
            }
            _selectedProfile.value = entity.copy(id = if (id == 0L) savedId else id)
            userNotice.value = "Profil '$name' sauvegardé."
        }
    }

    fun deleteProfile(profile: AiProfileEntity) {
        viewModelScope.launch {
            database.aiProfileDao().deleteProfile(profile)
            val remaining = database.aiProfileDao().getAllProfiles().firstOrNull()
            _selectedProfile.value = remaining?.firstOrNull()
            userNotice.value = "Profil supprimé."
        }
    }

    fun discoverModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            isDiscoveringModels.value = true
            val result = openAiClient.discoverModels(baseUrl, apiKey)
            isDiscoveringModels.value = false
            if (result.isSuccess) {
                val models = result.getOrNull() ?: emptyList()
                discoveredModels.value = models
                userNotice.value = "${models.size} modèles découverts avec succès !"
            } else {
                userNotice.value = "Échec découverte: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun testConnection(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            connectionStatus.value = ConnectionStatus(isTesting = true)
            val result = openAiClient.testConnection(baseUrl, apiKey)
            if (result.isSuccess) {
                val ms = result.getOrNull() ?: 0L
                connectionStatus.value = ConnectionStatus(
                    isTesting = false,
                    isSuccess = true,
                    latencyMs = ms,
                    message = "Connecté ($ms ms)"
                )
            } else {
                connectionStatus.value = ConnectionStatus(
                    isTesting = false,
                    isSuccess = false,
                    message = result.exceptionOrNull()?.message ?: "Connexion échouée"
                )
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        // Refresh project files and messages
        viewModelScope.launch {
            refreshWorkspaceFiles()
            database.chatMessageDao().getMessagesForProject(project.id).collect {
                _currentProjectMessages.value = it
            }
        }
        viewModelScope.launch {
            database.toolCallDao().getToolCallsForProject(project.id).collect {
                _currentToolCalls.value = it
            }
        }
    }

    fun createProject(name: String, description: String = "") {
        viewModelScope.launch {
            val dirName = name.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
            val entity = ProjectEntity(
                name = name,
                description = description,
                rootDirectoryName = dirName
            )
            val id = database.projectDao().insertProject(entity)
            val created = entity.copy(id = id)
            // Ensure dir is created and seeded
            workspaceManager.getProjectDir(dirName)
            selectProject(created)
            userNotice.value = "Projet '$name' créé."
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            database.projectDao().deleteProject(project)
            val list = database.projectDao().getAllProjects().firstOrNull()
            if (!list.isNullOrEmpty()) {
                selectProject(list.first())
            } else {
                _activeProject.value = null
            }
            userNotice.value = "Projet '${project.name}' supprimé."
        }
    }

    fun refreshWorkspaceFiles() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            _workspaceFiles.value = workspaceManager.listWorkspaceFiles(proj.rootDirectoryName)
            previewReloadKey.value += 1
        }
    }

    fun openFile(relativePath: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val res = workspaceManager.readFile(proj.rootDirectoryName, relativePath)
            if (res.isSuccess) {
                _openedFilePath.value = relativePath
                _openedFileContent.value = res.getOrNull() ?: ""
            } else {
                userNotice.value = "Impossible de lire le fichier: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveOpenedFile(newContent: String) {
        val proj = _activeProject.value ?: return
        val path = _openedFilePath.value ?: return
        viewModelScope.launch {
            val res = workspaceManager.writeFile(proj.rootDirectoryName, path, newContent)
            if (res.isSuccess) {
                _openedFileContent.value = newContent
                refreshWorkspaceFiles()
                userNotice.value = "Fichier '$path' sauvegardé."
            } else {
                userNotice.value = "Erreur sauvegarde: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun closeOpenedFile() {
        _openedFilePath.value = null
        _openedFileContent.value = ""
    }

    fun createWorkspaceFile(relativePath: String, initialContent: String = "") {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val res = workspaceManager.writeFile(proj.rootDirectoryName, relativePath, initialContent)
            if (res.isSuccess) {
                refreshWorkspaceFiles()
                openFile(relativePath)
                userNotice.value = "Fichier créé: $relativePath"
            }
        }
    }

    fun deleteWorkspaceFile(relativePath: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val res = workspaceManager.deleteFile(proj.rootDirectoryName, relativePath)
            if (res.isSuccess) {
                if (_openedFilePath.value == relativePath) {
                    closeOpenedFile()
                }
                refreshWorkspaceFiles()
                userNotice.value = "Fichier supprimé: $relativePath"
            }
        }
    }

    fun exportProjectZip(uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                    workspaceManager.exportProjectToZip(proj.rootDirectoryName, os)
                }
                userNotice.value = "Export ZIP réussi !"
            } catch (e: Exception) {
                userNotice.value = "Erreur export: ${e.message}"
            }
        }
    }

    fun importProjectZip(uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { `is` ->
                    workspaceManager.importProjectFromZip(proj.rootDirectoryName, `is`)
                }
                refreshWorkspaceFiles()
                userNotice.value = "Projet importé avec succès depuis le ZIP !"
            } catch (e: Exception) {
                userNotice.value = "Erreur import: ${e.message}"
            }
        }
    }

    // Agent Chat & Tool Orchestration
    fun sendUserPrompt(promptText: String) {
        val profile = _selectedProfile.value
        val project = _activeProject.value
        if (profile == null) {
            userNotice.value = "Veuillez d'abord configurer un profil IA."
            return
        }
        if (project == null) {
            userNotice.value = "Veuillez d'abord sélectionner un projet."
            return
        }
        if (promptText.isBlank()) return

        // If WebSocket mode is active and connected, route in real-time over WebSocket
        if (isWebSocketMode.value && webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            viewModelScope.launch {
                isAgentRunning.value = true
                liveStreamingContent.value = ""
                database.chatMessageDao().insertMessage(
                    ChatMessageEntity(
                        projectId = project.id,
                        role = "user",
                        content = promptText.trim()
                    )
                )
                val assistantMsgId = database.chatMessageDao().insertMessage(
                    ChatMessageEntity(
                        projectId = project.id,
                        role = if (isPlanMode.value) "plan" else "assistant",
                        content = "",
                        isStreaming = true
                    )
                )
                liveStreamingMessageId.value = assistantMsgId
                webSocketManager.sendPrompt(
                    prompt = promptText.trim(),
                    mode = if (isPlanMode.value) "plan" else (if (isAutoMode.value) "auto" else "supervised"),
                    model = profile.selectedModel
                )
            }
            return
        }

        // Otherwise route in real-time over direct OpenAI SSE streaming
        viewModelScope.launch {
            isAgentRunning.value = true
            liveStreamingContent.value = ""
            orchestrator.executeUserPrompt(
                profile = profile,
                project = project,
                userPrompt = promptText.trim(),
                isPlanMode = isPlanMode.value,
                isAutoMode = isAutoMode.value
            ).collect { event ->
                when (event) {
                    is OrchestratorEvent.AssistantText -> {
                        liveStreamingMessageId.value = event.messageId
                        liveStreamingContent.value = event.fullTextSoFar
                    }
                    is OrchestratorEvent.ToolCallStarted -> {
                        // Real-time tool call initiated
                    }
                    is OrchestratorEvent.ToolCallUpdated -> {
                        refreshWorkspaceFiles()
                    }
                    is OrchestratorEvent.ToolCallNeedsApproval -> {
                        userNotice.value = "Diff généré : approbation requise (Mode Supervisé)."
                    }
                    is OrchestratorEvent.Complete -> {
                        isAgentRunning.value = false
                        liveStreamingContent.value = null
                        liveStreamingMessageId.value = null
                        refreshWorkspaceFiles()
                    }
                    is OrchestratorEvent.Error -> {
                        isAgentRunning.value = false
                        liveStreamingContent.value = null
                        liveStreamingMessageId.value = null
                        userNotice.value = "Erreur: ${event.error}"
                    }
                }
            }
            isAgentRunning.value = false
            liveStreamingContent.value = null
            liveStreamingMessageId.value = null
            refreshWorkspaceFiles()
        }
    }

    fun approveToolCall(toolCallId: Long) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val res = orchestrator.approvePendingWrite(toolCallId, proj)
            if (res.isSuccess) {
                refreshWorkspaceFiles()
                userNotice.value = "Modifications approuvées et appliquées !"
            } else {
                userNotice.value = "Erreur lors de l'application: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun rejectToolCall(toolCallId: Long) {
        viewModelScope.launch {
            orchestrator.rejectPendingWrite(toolCallId)
            userNotice.value = "Modifications rejetées."
        }
    }

    fun clearChatHistory() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            database.chatMessageDao().clearMessagesForProject(proj.id)
            database.toolCallDao().clearToolCallsForProject(proj.id)
            userNotice.value = "Historique de discussion effacé."
        }
    }

    // Memory operations
    fun addMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            database.memoryDao().insertMemory(
                MemoryItemEntity(category = category, key = key.trim(), value = value.trim())
            )
            userNotice.value = "Fait mémorisé."
        }
    }

    fun deleteMemory(memory: MemoryItemEntity) {
        viewModelScope.launch {
            database.memoryDao().deleteMemory(memory)
            userNotice.value = "Mémoire supprimée."
        }
    }

    // Skills operations
    fun addSkill(name: String, description: String, trigger: String, instructions: String) {
        viewModelScope.launch {
            database.skillDao().insertSkill(
                SkillEntity(
                    name = name.trim(),
                    description = description.trim(),
                    triggerPrompt = trigger.trim(),
                    systemInstructions = instructions.trim()
                )
            )
            userNotice.value = "Skill '$name' ajouté."
        }
    }

    fun toggleSkill(skill: SkillEntity) {
        viewModelScope.launch {
            database.skillDao().updateSkill(skill.copy(isEnabled = !skill.isEnabled))
        }
    }

    fun deleteSkill(skill: SkillEntity) {
        viewModelScope.launch {
            database.skillDao().deleteSkill(skill)
            userNotice.value = "Skill supprimé."
        }
    }
}
