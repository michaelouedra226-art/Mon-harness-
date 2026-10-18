package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AiProfileDao
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.dao.ToolCallDao
import com.example.data.local.entity.AiProfileEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.ToolCallEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AiProfileEntity::class,
        ProjectEntity::class,
        ChatMessageEntity::class,
        ToolCallEntity::class,
        MemoryItemEntity::class,
        SkillEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiProfileDao(): AiProfileDao
    abstract fun projectDao(): ProjectDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun toolCallDao(): ToolCallDao
    abstract fun memoryDao(): MemoryDao
    abstract fun skillDao(): SkillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hermes_mobile.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default skills and demo profile
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                seedInitialData(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            // Seed a starter profile for OpenAI or local Ollama
            database.aiProfileDao().insertProfile(
                AiProfileEntity(
                    name = "OpenAI Standard",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "",
                    selectedModel = "gpt-4o",
                    isDefault = true
                )
            )
            database.aiProfileDao().insertProfile(
                AiProfileEntity(
                    name = "Ollama Local (10.0.2.2)",
                    baseUrl = "http://10.0.2.2:11434/v1",
                    apiKey = "ollama",
                    selectedModel = "qwen2.5-coder:latest",
                    isDefault = false
                )
            )

            // Seed initial Hermes Skills
            database.skillDao().insertSkill(
                SkillEntity(
                    name = "HTML/CSS/JS Web Builder",
                    description = "Crée des interfaces web réactives et interactives pour la live preview",
                    triggerPrompt = "web, frontend, html, css, javascript, preview, ui",
                    systemInstructions = "Privilégie des fichiers index.html modulaires avec CSS moderne (flex/grid, animations) et JavaScript vanilla ou composants modulaires testables dans la WebView."
                )
            )
            database.skillDao().insertSkill(
                SkillEntity(
                    name = "Safe Refactor & Diff Inspector",
                    description = "Génère des diffs clairs et applique des modifications chirurgicales",
                    triggerPrompt = "refactor, fix, debug, modifier, changer",
                    systemInstructions = "Avant d'écraser un fichier existant, préserve toujours la structure existante et indique explicitement quelles fonctions ont été altérées."
                )
            )
            database.skillDao().insertSkill(
                SkillEntity(
                    name = "Self-Correction & Memory Logger",
                    description = "Enregistre automatiquement les préférences et règles de code de l'utilisateur",
                    triggerPrompt = "toujours, règle, convention, préférence, mémoire",
                    systemInstructions = "Utilise save_memory pour enregistrer les préférences exprimées par l'utilisateur (frameworks favoris, styles, contraintes techniques)."
                )
            )

            // Seed a default project
            database.projectDao().insertProject(
                ProjectEntity(
                    name = "Starter Project",
                    description = "Workspace d'exemple avec application web interactive",
                    rootDirectoryName = "starter_project"
                )
            )
        }
    }
}
