package com.example.data.workspace

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class WorkspaceFile(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

class WorkspaceManager(private val context: Context) {

    private val workspacesRoot: File
        get() {
            val dir = File(context.filesDir, "hermes_workspaces")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getProjectDir(projectDirName: String): File {
        val projectDir = File(workspacesRoot, projectDirName.replace(Regex("[^a-zA-Z0-9_-]"), "_"))
        if (!projectDir.exists()) {
            projectDir.mkdirs()
            seedDefaultProjectFiles(projectDir)
        }
        return projectDir
    }

    private fun seedDefaultProjectFiles(dir: File) {
        val indexHtml = File(dir, "index.html")
        if (!indexHtml.exists()) {
            indexHtml.writeText(
                """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Hermes Workspace App</title>
                    <link rel="stylesheet" href="styles.css">
                </head>
                <body>
                    <div class="container">
                        <header>
                            <div class="badge">HERMES LIVE PREVIEW</div>
                            <h1>Espace de Travail Actif</h1>
                            <p>Modifié et généré en temps réel par l'agent de codage autonome.</p>
                        </header>
                        
                        <div class="card">
                            <h2 id="counter-title">Compteur Réactif</h2>
                            <div class="display" id="count">0</div>
                            <div class="actions">
                                <button id="btn-dec" class="btn secondary">-1</button>
                                <button id="btn-inc" class="btn primary">+1</button>
                                <button id="btn-reset" class="btn outline">Reset</button>
                            </div>
                        </div>

                        <div class="card terminal">
                            <div class="terminal-bar">
                                <span class="dot red"></span>
                                <span class="dot yellow"></span>
                                <span class="dot green"></span>
                                <span class="title">Console Agent</span>
                            </div>
                            <pre id="logs">> Application initialisée avec succès.\n> Prêt à recevoir des instructions de code.</pre>
                        </div>
                    </div>
                    <script src="app.js"></script>
                </body>
                </html>
                """.trimIndent()
            )
        }

        val stylesCss = File(dir, "styles.css")
        if (!stylesCss.exists()) {
            stylesCss.writeText(
                """
                :root {
                    --bg: #0B0F17;
                    --card: #131B2A;
                    --card-border: #2E3E58;
                    --primary: #00F5A0;
                    --primary-hover: #00D26A;
                    --secondary: #00C9FF;
                    --text: #F8FAFC;
                    --text-muted: #94A3B8;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background: var(--bg);
                    color: var(--text);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    padding: 20px;
                    min-height: 100vh;
                    display: flex;
                    justify-content: center;
                }
                .container { width: 100%; max-width: 500px; display: flex; flex-direction: column; gap: 20px; }
                header { text-align: center; margin-bottom: 10px; }
                .badge {
                    display: inline-block;
                    background: rgba(0, 245, 160, 0.15);
                    color: var(--primary);
                    font-size: 11px;
                    font-weight: 700;
                    letter-spacing: 1px;
                    padding: 4px 10px;
                    border-radius: 20px;
                    margin-bottom: 12px;
                    border: 1px solid rgba(0, 245, 160, 0.3);
                }
                h1 { font-size: 24px; margin-bottom: 8px; color: #fff; }
                p { color: var(--text-muted); font-size: 14px; }
                .card {
                    background: var(--card);
                    border: 1px solid var(--card-border);
                    border-radius: 16px;
                    padding: 20px;
                    text-align: center;
                }
                .card h2 { font-size: 16px; color: var(--text-muted); margin-bottom: 16px; }
                .display { font-size: 54px; font-weight: 800; color: var(--primary); margin-bottom: 20px; font-variant-numeric: tabular-nums; }
                .actions { display: flex; gap: 10px; justify-content: center; }
                .btn {
                    padding: 10px 18px;
                    border-radius: 8px;
                    font-weight: 600;
                    font-size: 14px;
                    cursor: pointer;
                    border: none;
                    transition: all 0.2s;
                }
                .btn.primary { background: var(--primary); color: #022C1A; }
                .btn.secondary { background: #1E293B; color: #F1F5F9; border: 1px solid var(--card-border); }
                .btn.outline { background: transparent; color: var(--text-muted); border: 1px solid var(--card-border); }
                .card.terminal { text-align: left; padding: 0; overflow: hidden; }
                .terminal-bar {
                    background: #080C14;
                    padding: 8px 14px;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    border-bottom: 1px solid var(--card-border);
                }
                .dot { width: 10px; height: 10px; border-radius: 50%; }
                .dot.red { background: #EF4444; }
                .dot.yellow { background: #FBBF24; }
                .dot.green { background: #10B981; }
                .terminal-bar .title { font-size: 12px; color: var(--text-muted); margin-left: 6px; font-family: monospace; }
                pre {
                    padding: 14px;
                    font-family: monospace;
                    font-size: 12px;
                    color: var(--primary);
                    background: #06090F;
                    white-space: pre-wrap;
                }
                """.trimIndent()
            )
        }

        val appJs = File(dir, "app.js")
        if (!appJs.exists()) {
            appJs.writeText(
                """
                let count = 0;
                const countEl = document.getElementById('count');
                const logsEl = document.getElementById('logs');
                
                function addLog(msg) {
                    const time = new Date().toLocaleTimeString();
                    logsEl.textContent += '\n[' + time + '] ' + msg;
                }
                
                document.getElementById('btn-inc').addEventListener('click', () => {
                    count++;
                    countEl.textContent = count;
                    addLog('Incrément: ' + count);
                });
                
                document.getElementById('btn-dec').addEventListener('click', () => {
                    count--;
                    countEl.textContent = count;
                    addLog('Décrément: ' + count);
                });
                
                document.getElementById('btn-reset').addEventListener('click', () => {
                    count = 0;
                    countEl.textContent = count;
                    addLog('Réinitialisation du compteur');
                });
                """.trimIndent()
            )
        }
    }

    suspend fun listWorkspaceFiles(projectDirName: String): List<WorkspaceFile> = withContext(Dispatchers.IO) {
        val root = getProjectDir(projectDirName)
        val filesList = mutableListOf<WorkspaceFile>()
        root.walkTopDown().forEach { file ->
            if (file != root) {
                val relPath = file.relativeTo(root).path
                filesList.add(
                    WorkspaceFile(
                        relativePath = relPath,
                        name = file.name,
                        isDirectory = file.isDirectory,
                        sizeBytes = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified()
                    )
                )
            }
        }
        filesList.sortedWith(compareBy({ !it.isDirectory }, { it.relativePath }))
    }

    suspend fun readFile(projectDirName: String, relativePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = getProjectDir(projectDirName)
            val file = File(root, relativePath.trim().removePrefix("/"))
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $relativePath"))
            }
            Result.success(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFile(projectDirName: String, relativePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = getProjectDir(projectDirName)
            val cleanRel = relativePath.trim().removePrefix("/")
            val file = File(root, cleanRel)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(projectDirName: String, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = getProjectDir(projectDirName)
            val file = File(root, relativePath.trim().removePrefix("/"))
            if (file.exists()) {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportProjectToZip(projectDirName: String, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = getProjectDir(projectDirName)
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                root.walkTopDown().forEach { file ->
                    if (file != root) {
                        val relPath = file.relativeTo(root).path
                        if (file.isDirectory) {
                            zipOut.putNextEntry(ZipEntry("$relPath/"))
                            zipOut.closeEntry()
                        } else {
                            zipOut.putNextEntry(ZipEntry(relPath))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProjectFromZip(projectDirName: String, inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = getProjectDir(projectDirName)
            root.mkdirs()
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val file = File(root, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zipIn.copyTo(fos)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPreviewUrl(projectDirName: String): String {
        val root = getProjectDir(projectDirName)
        val index = File(root, "index.html")
        return if (index.exists()) "file://${index.absolutePath}" else "about:blank"
    }
}
