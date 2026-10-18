package com.example.engine

import com.example.data.remote.model.FunctionSpecDto
import com.example.data.remote.model.ToolDefinitionDto

object AgentTools {

    val DEFINITIONS: List<ToolDefinitionDto> = listOf(
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "read_file",
                description = "Lit le contenu complet d'un fichier textuel existant dans le workspace du projet actuel.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "path" to mapOf(
                            "type" to "string",
                            "description" to "Chemin relatif du fichier (ex: 'index.html', 'src/main.js', 'styles.css')"
                        )
                    ),
                    "required" to listOf("path")
                )
            )
        ),
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "write_file",
                description = "Écrit ou remplace le contenu d'un fichier dans le workspace. En mode supervisé, un diff est généré pour validation par l'utilisateur.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "path" to mapOf(
                            "type" to "string",
                            "description" to "Chemin relatif du fichier à créer ou modifier"
                        ),
                        "content" to mapOf(
                            "type" to "string",
                            "description" to "Contenu textuel complet à écrire dans le fichier"
                        )
                    ),
                    "required" to listOf("path", "content")
                )
            )
        ),
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "list_files",
                description = "Liste tous les fichiers et dossiers présents dans le workspace de code.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "directory" to mapOf(
                            "type" to "string",
                            "description" to "Sous-dossier optionnel ou chaîne vide pour lister tout le projet"
                        )
                    )
                )
            )
        ),
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "delete_file",
                description = "Supprime un fichier spécifique du workspace.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "path" to mapOf(
                            "type" to "string",
                            "description" to "Chemin relatif du fichier à supprimer"
                        )
                    ),
                    "required" to listOf("path")
                )
            )
        ),
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "save_memory",
                description = "Sauvegarde une information persistante dans la mémoire longue de Hermes (préférence utilisateur, convention de code, note projet).",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "category" to mapOf(
                            "type" to "string",
                            "description" to "Catégorie de la mémoire",
                            "enum" to listOf("preference", "project_context", "convention", "fact")
                        ),
                        "key" to mapOf(
                            "type" to "string",
                            "description" to "Identifiant court de la mémoire (ex: 'preferred_framework', 'color_theme')"
                        ),
                        "value" to mapOf(
                            "type" to "string",
                            "description" to "Contenu détaillé de la mémoire"
                        )
                    ),
                    "required" to listOf("category", "key", "value")
                )
            )
        ),
        ToolDefinitionDto(
            type = "function",
            function = FunctionSpecDto(
                name = "create_skill",
                description = "Crée une nouvelle compétence (Skill) persistante que l'agent pourra réutiliser lors des futures requêtes.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "name" to mapOf(
                            "type" to "string",
                            "description" to "Nom de la compétence (ex: 'Chart.js Visualizer')"
                        ),
                        "description" to mapOf(
                            "type" to "string",
                            "description" to "Description du rôle et de l'utilité du skill"
                        ),
                        "trigger" to mapOf(
                            "type" to "string",
                            "description" to "Mots-clés déclencheurs séparés par des virgules"
                        ),
                        "instructions" to mapOf(
                            "type" to "string",
                            "description" to "Directives et règles à injecter dans le prompt système"
                        )
                    ),
                    "required" to listOf("name", "description", "instructions")
                )
            )
        )
    )
}
