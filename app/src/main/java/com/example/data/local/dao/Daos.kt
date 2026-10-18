package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AiProfileEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.ToolCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProfileDao {
    @Query("SELECT * FROM ai_profiles ORDER BY id DESC")
    fun getAllProfiles(): Flow<List<AiProfileEntity>>

    @Query("SELECT * FROM ai_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): AiProfileEntity?

    @Query("SELECT * FROM ai_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): AiProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AiProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: AiProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: AiProfileEntity)

    @Query("UPDATE ai_profiles SET isDefault = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun setDefaultProfile(selectedId: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(projectId: Long, limit: Int = 20): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearMessagesForProject(projectId: Long)
}

@Dao
interface ToolCallDao {
    @Query("SELECT * FROM tool_calls WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getToolCallsForProject(projectId: Long): Flow<List<ToolCallEntity>>

    @Query("SELECT * FROM tool_calls WHERE id = :id")
    suspend fun getToolCallById(id: Long): ToolCallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolCall(toolCall: ToolCallEntity): Long

    @Update
    suspend fun updateToolCall(toolCall: ToolCallEntity)

    @Query("UPDATE tool_calls SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM tool_calls WHERE projectId = :projectId")
    suspend fun clearToolCallsForProject(projectId: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE key = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItemEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryItemEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryItemEntity)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE isEnabled = 1")
    suspend fun getActiveSkills(): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity): Long

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)
}
