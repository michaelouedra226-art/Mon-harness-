package com.example.engine

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiProfileEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.ToolCallEntity
import com.example.data.remote.AgentStreamEvent
import com.example.data.remote.OpenAiClient
import com.example.data.remote.model.ChatCompletionRequest
import com.example.data.remote.model.OpenAiMessageDto
import com.example.data.remote.model.ToolCallDto
import com.example.data.workspace.WorkspaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class OrchestratorEvent {
    data class AssistantText(val messageId: Long, val fullTextSoFar: String, val chunk: String) : OrchestratorEvent()
    data class ToolCallStarted(val toolCall: ToolCallEntity) : OrchestratorEvent()
    data class ToolCallUpdated(val toolCall: ToolCallEntity) : OrchestratorEvent()
    data class ToolCallNeedsApproval(val toolCall: ToolCallEntity, val diffPatch: String) : OrchestratorEvent()
    data class Complete(val assistantMessageId: Long) : OrchestratorEvent()
    data class Error(val error: String) : OrchestratorEvent()
}

class HermesOrchestrator(
    private val database: AppDatabase,
    private val workspaceManager: WorkspaceManager,
    private val openAiClient: OpenAiClient = OpenAiClient()
) {

    suspend fun executeUserPrompt(
        profile: AiProfileEntity,
        project: ProjectEntity,
        userPrompt: String,
        isPlanMode: Boolean,
        isAutoMode: Boolean
    ): Flow<OrchestratorEvent> = flow {
        // 1. Save User Message
        val userMsgId = database.chatMessageDao().insertMessage(
            ChatMessageEntity(
                projectId = project.id,
                role = "user",
                content = userPrompt
            )
        )

        // 2. Build System Prompt & History
        val systemPrompt = buildSystemPrompt(project, isPlanMode)
        val conversationHistory = mutableListOf<OpenAiMessageDto>()
        conversationHistory.add(OpenAiMessageDto(role = "system", content = systemPrompt))

        val recentEntities = database.chatMessageDao().getRecentMessages(project.id, limit = 15).reversed()
        recentEntities.forEach { entity ->
            conversationHistory.add(
                OpenAiMessageDto(
                    role = entity.role,
                    content = entity.content
                )
            )
        }

        // Loop turns for autonomous agent tool execution
        var turn = 0
        val maxTurns = 5
        var continueLoop = true

        while (continueLoop && turn < maxTurns) {
            turn++

            // Create initial placeholder for Assistant message
            val assistantMsgId = database.chatMessageDao().insertMessage(
                ChatMessageEntity(
                    projectId = project.id,
                    role = if (isPlanMode && turn == 1) "plan" else "assistant",
                    content = "",
                    isStreaming = true
                )
            )

            val contentBuilder = StringBuilder()
            val pendingToolCalls = mutableListOf<ToolCallDto>()

            val request = ChatCompletionRequest(
                model = profile.selectedModel.ifBlank { "gpt-4o" },
                messages = conversationHistory,
                tools = AgentTools.DEFINITIONS,
                stream = true,
                temperature = 0.2
            )

            var streamError: String? = null

            openAiClient.streamChatCompletion(
                baseUrl = profile.baseUrl,
                apiKey = profile.apiKey,
                requestPayload = request
            ).collect { streamEvent ->
                when (streamEvent) {
                    is AgentStreamEvent.ContentChunk -> {
                        contentBuilder.append(streamEvent.text)
                        emit(OrchestratorEvent.AssistantText(
                            messageId = assistantMsgId,
                            fullTextSoFar = contentBuilder.toString(),
                            chunk = streamEvent.text
                        ))
                    }
                    is AgentStreamEvent.ToolCallChunk -> {
                        // Streaming progress
                    }
                    is AgentStreamEvent.Complete -> {
                        pendingToolCalls.addAll(streamEvent.toolCalls)
                    }
                    is AgentStreamEvent.Error -> {
                        streamError = streamEvent.message
                    }
                }
            }

            if (streamError != null) {
                database.chatMessageDao().updateMessage(
                    ChatMessageEntity(
                        id = assistantMsgId,
                        projectId = project.id,
                        role = "assistant",
                        content = contentBuilder.toString() + "\n\n⚠️ Erreur: $streamError",
                        isStreaming = false
                    )
                )
                emit(OrchestratorEvent.Error(streamError ?: "Erreur réseau"))
                return@flow
            }

            // Update finished assistant message
            database.chatMessageDao().updateMessage(
                ChatMessageEntity(
                    id = assistantMsgId,
                    projectId = project.id,
                    role = if (isPlanMode && turn == 1) "plan" else "assistant",
                    content = contentBuilder.toString(),
                    isStreaming = false
                )
            )

            if (pendingToolCalls.isEmpty()) {
                // Agent is finished with this interaction
                emit(OrchestratorEvent.Complete(assistantMsgId))
                continueLoop = false
            } else {
                // Record assistant turn in context
                conversationHistory.add(
                    OpenAiMessageDto(
                        role = "assistant",
                        content = contentBuilder.toString(),
                        toolCalls = pendingToolCalls
                    )
                )

                // Execute each tool call
                for (toolCallDto in pendingToolCalls) {
                    val toolName = toolCallDto.function.name
                    val argsJson = toolCallDto.function.arguments

                    val toolEntityId = database.toolCallDao().insertToolCall(
                        ToolCallEntity(
                            projectId = project.id,
                            messageId = assistantMsgId,
                            callId = toolCallDto.id,
                            toolName = toolName,
                            argumentsJson = argsJson,
                            status = "pending"
                        )
                    )

                    var savedEntity = database.toolCallDao().getToolCallById(toolEntityId)!!
                    emit(OrchestratorEvent.ToolCallStarted(savedEntity))

                    val executionResult = executeTool(
                        project = project,
                        toolEntity = savedEntity,
                        toolName = toolName,
                        argsJson = argsJson,
                        isAutoMode = isAutoMode,
                        onApprovalRequired = { diffPatch ->
                            emit(OrchestratorEvent.ToolCallNeedsApproval(savedEntity, diffPatch))
                        }
                    )

                    savedEntity = database.toolCallDao().getToolCallById(toolEntityId)!!
                    emit(OrchestratorEvent.ToolCallUpdated(savedEntity))

                    // Feed tool response back into conversation history
                    conversationHistory.add(
                        OpenAiMessageDto(
                            role = "tool",
                            name = toolName,
                            toolCallId = toolCallDto.id,
                            content = executionResult
                        )
                    )
                }

                // If any tool is waiting for user approval (in supervised mode), stop loop until user approves
                val hasPendingApproval = pendingToolCalls.any { tc ->
                    val entity = database.toolCallDao().getToolCallsForProject(project.id)
                    // checked via status
                    false
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun buildSystemPrompt(project: ProjectEntity, isPlanMode: Boolean): String {
        val memories = database.memoryDao().getAllMemories()
        val activeSkills = database.skillDao().getActiveSkills()
        val workspaceFiles = workspaceManager.listWorkspaceFiles(project.rootDirectoryName)

        val filesSummary = workspaceFiles.joinToString("\n") { file ->
            "- ${file.relativePath} (${if (file.isDirectory) "dossier" else "${file.sizeBytes} octets"})"
        }

        val skillsSummary = if (activeSkills.isEmpty()) "Aucun skill spécifique" else {
            activeSkills.joinToString("\n\n") { skill ->
                "### Skill: ${skill.name}\n${skill.description}\nDirectives: ${skill.systemInstructions}"
            }
        }

        return """
        Tu es Hermes Mobile, un agent de codage autonome et ingénieur full-stack de niveau senior.
        Tu opères directement dans un espace de travail local Android et tu disposes d'outils (tool calls) pour lire, modifier, créer des fichiers, enregistrer des compétences et retenir les préférences de l'utilisateur.

        === WORKSPACE DU PROJET ACTUEL (${project.name}) ===
        Fichiers présents :
        $filesSummary

        === SYSTÈME DE SKILLS DISPONIBLES ===
        $skillsSummary

        === DIRECTIVES FONDAMENTALES ===
        1. Rends toujours le code complet, fonctionnel et soigné. N'utilise jamais de placeholders ou '// reste du code inchangé'.
        2. Pour prévisualiser une interface web, utilise des fichiers standards (index.html, styles.css, app.js).
        3. Quand l'utilisateur te demande de coder ou modifier un fichier, utilise l'outil `write_file` avec le chemin relatif et le contenu complet.
        4. Si tu apprends une règle, préférence ou convention importante, utilise `save_memory`.
        5. Si tu conçois une méthodologie ou spécialité réutilisable, utilise `create_skill`.
        ${if (isPlanMode) "6. [MODE PLAN ACTIVÉ] : Établis d'abord un plan d'action numéroté détaillé avant d'appeler des outils d'écriture." else ""}
        """.trimIndent()
    }

    private suspend fun executeTool(
        project: ProjectEntity,
        toolEntity: ToolCallEntity,
        toolName: String,
        argsJson: String,
        isAutoMode: Boolean,
        onApprovalRequired: suspend (diffPatch: String) -> Unit
    ): String {
        return try {
            val json = JSONObject(argsJson)
            when (toolName) {
                "read_file" -> {
                    val path = json.optString("path", "")
                    val readRes = workspaceManager.readFile(project.rootDirectoryName, path)
                    val content = readRes.getOrElse { "Erreur lecture: ${it.message}" }
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(
                            status = if (readRes.isSuccess) "executed" else "error",
                            resultJson = content.take(500),
                            targetPath = path
                        )
                    )
                    content
                }
                "write_file" -> {
                    val path = json.optString("path", "")
                    val newContent = json.optString("content", "")
                    val oldContent = workspaceManager.readFile(project.rootDirectoryName, path).getOrDefault("")

                    val diffLines = DiffGenerator.computeDiff(oldContent, newContent)
                    val patch = DiffGenerator.formatPatch(diffLines)

                    if (!isAutoMode) {
                        // Supervised Mode: Wait for approval
                        database.toolCallDao().updateToolCall(
                            toolEntity.copy(
                                status = "pending",
                                diffPatch = patch,
                                targetPath = path,
                                resultJson = "En attente d'approbation par l'utilisateur."
                            )
                        )
                        onApprovalRequired(patch)
                        "Diff généré pour '$path'. En attente de validation utilisateur."
                    } else {
                        // Auto Mode: Write immediately
                        val writeRes = workspaceManager.writeFile(project.rootDirectoryName, path, newContent)
                        database.toolCallDao().updateToolCall(
                            toolEntity.copy(
                                status = if (writeRes.isSuccess) "executed" else "error",
                                diffPatch = patch,
                                targetPath = path,
                                resultJson = if (writeRes.isSuccess) "Fichier '$path' écrit avec succès (${newContent.length} caractères)." else "Erreur écriture: ${writeRes.exceptionOrNull()?.message}"
                            )
                        )
                        if (writeRes.isSuccess) "Fichier '$path' mis à jour avec succès." else "Erreur écriture de '$path'."
                    }
                }
                "list_files" -> {
                    val files = workspaceManager.listWorkspaceFiles(project.rootDirectoryName)
                    val summary = files.joinToString("\n") { "${it.relativePath} (${if (it.isDirectory) "dir" else "${it.sizeBytes}B"})" }
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(status = "executed", resultJson = summary)
                    )
                    summary
                }
                "delete_file" -> {
                    val path = json.optString("path", "")
                    val delRes = workspaceManager.deleteFile(project.rootDirectoryName, path)
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(
                            status = if (delRes.isSuccess) "executed" else "error",
                            targetPath = path,
                            resultJson = if (delRes.isSuccess) "Fichier '$path' supprimé." else "Erreur suppression: ${delRes.exceptionOrNull()?.message}"
                        )
                    )
                    "Fichier '$path' supprimé."
                }
                "save_memory" -> {
                    val category = json.optString("category", "fact")
                    val key = json.optString("key", "")
                    val value = json.optString("value", "")
                    val existing = database.memoryDao().getMemoryByKey(key)
                    if (existing != null) {
                        database.memoryDao().updateMemory(existing.copy(value = value, category = category, updatedAt = System.currentTimeMillis()))
                    } else {
                        database.memoryDao().insertMemory(MemoryItemEntity(category = category, key = key, value = value))
                    }
                    val msg = "Mémoire sauvegardée: [$key] = $value"
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(status = "executed", resultJson = msg)
                    )
                    msg
                }
                "create_skill" -> {
                    val name = json.optString("name", "Nouveau Skill")
                    val desc = json.optString("description", "")
                    val trigger = json.optString("trigger", "")
                    val instructions = json.optString("instructions", "")
                    database.skillDao().insertSkill(
                        SkillEntity(
                            name = name,
                            description = desc,
                            triggerPrompt = trigger,
                            systemInstructions = instructions
                        )
                    )
                    val msg = "Skill créé avec succès: '$name'"
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(status = "executed", resultJson = msg)
                    )
                    msg
                }
                else -> {
                    val err = "Outil inconnu: $toolName"
                    database.toolCallDao().updateToolCall(
                        toolEntity.copy(status = "error", resultJson = err)
                    )
                    err
                }
            }
        } catch (e: Exception) {
            val err = "Exception tool call $toolName: ${e.message}"
            database.toolCallDao().updateToolCall(
                toolEntity.copy(status = "error", resultJson = err)
            )
            err
        }
    }

    /**
     * Applies a pending tool call that required user approval in Supervised Mode.
     */
    suspend fun approvePendingWrite(toolCallId: Long, project: ProjectEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = database.toolCallDao().getToolCallById(toolCallId) ?: return@withContext Result.failure(Exception("Tool call non trouvé"))
        try {
            val json = JSONObject(entity.argumentsJson)
            val path = json.optString("path", "")
            val content = json.optString("content", "")
            val writeRes = workspaceManager.writeFile(project.rootDirectoryName, path, content)
            if (writeRes.isSuccess) {
                database.toolCallDao().updateToolCall(
                    entity.copy(
                        status = "executed",
                        resultJson = "Approuvé et appliqué par l'utilisateur."
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(writeRes.exceptionOrNull() ?: Exception("Échec écriture"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rejects a pending tool call in Supervised Mode.
     */
    suspend fun rejectPendingWrite(toolCallId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = database.toolCallDao().getToolCallById(toolCallId) ?: return@withContext Result.failure(Exception("Tool call non trouvé"))
        database.toolCallDao().updateToolCall(
            entity.copy(
                status = "rejected",
                resultJson = "Rejeté par l'utilisateur."
            )
        )
        Result.success(Unit)
    }
}
