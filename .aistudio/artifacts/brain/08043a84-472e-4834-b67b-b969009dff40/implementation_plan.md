# Hermes Mobile – Agent de Codage Autonome

Application Android native en Jetpack Compose permettant de piloter un agent de développement autonome connecté à toute API compatible OpenAI (OpenAI, Ollama, LM Studio, Groq, OpenRouter, vLLM, DeepSeek), avec découverte dynamique des modèles, exécution de tool calls en streaming, système de Skills évolutifs, mémoire persistante (Room), workspace local de fichiers avec gestion de diffs et prévisualisation WebView en temps réel.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les trois orientations clés clarifiées lors de la phase 1 sont intégrées dans cette architecture :

- **Protocole IA & Connectivité** : Connexion directe en streaming SSE / HTTP vers tout endpoint compatible OpenAI (`/v1/chat/completions` avec `stream: true` et support des tool calls), découverte automatique des modèles via `GET /v1/models`, test de latence en temps réel et support optionnel d'une passerelle WebSocket pour backend distant.
- **Identité Visuelle** : Thème *Modern Dev Studio* sombre haute précision, contrastes soignés, accents néon menthe/émeraude (`#00F5A0` / `#00D26A`), panneaux de code monospace, cartes d'exécution d'outils interactives et visualiseur de diffs coloré (rouge/vert).
- **Workspace & Fichiers** : Espace de travail de code local intégré par projet avec explorateur arborescent réactif, éditeur de fichiers, gestionnaire de diffs avant application, prévisualisation WebView HTML/CSS/JS en direct et import/export ZIP complet.

---

## 1. Overview & Core Concept

- **Fonctionnalité principale** : Un environnement de développement assisté par IA autonome sur mobile. L'agent peut concevoir, décomposer en plans, lire et éditer des fichiers, créer de nouveaux outils/skills et afficher le résultat en direct.
- **Public cible** : Développeurs, créateurs et ingénieurs souhaitant concevoir, prototyper, modifier et débugger du code n'importe où, avec leur propre fournisseur d'IA souverain ou cloud.
- **Valeur ajoutée** : Autonomie complète sur Android sans dépendre d'un fournisseur unique, transparence absolue sur les tool calls (vue avant/après, approbation ou mode automatique), mémoire inter-sessions et compétences persistantes.

---

## 2. User Experience & Visual Design

### Parcours Utilisateur Clés
1. **Démarrage & Configuration de l'IA** :
   - Formulaire épuré pour saisir la **Base URL** (ex: `https://api.openai.com/v1`, `http://10.0.2.2:11434/v1`, `https://openrouter.ai/api/v1`) et l'**API Key**.
   - Bouton d'action **"Découvrir les modèles"** qui interroge automatiquement l'endpoint `GET /v1/models` et peuple un sélecteur déroulant interactif.
   - Test de connexion instantané avec affichage de la latence (ping ms).
   - Gestion de profils multiples (ex: "Local Ollama", "Groq Fast", "OpenAI GPT-4o", "DeepSeek Coder").
2. **Gestion de Workspace & Projets** :
   - Sélection ou création d'un projet local (ex: "Web App React", "Landing Page Portfolio", "Script Python").
   - Explorateur arborescent de fichiers avec création, suppression, renommage et visualisation de contenu.
   - Export ZIP du projet vers le stockage Android et import depuis ZIP.
3. **Session de Codage avec l'Agent (Chat & Live Tool Calls)** :
   - Console de chat réactive avec bascule de mode : **Mode Plan** (l'agent soumet d'abord ses étapes) et **Mode Auto / Supervisé** (approbation requise avant écriture).
   - Cartes de tool calls en direct : `read_file`, `write_file`, `edit_file`, `list_files`, `save_memory`, `create_skill`.
   - Inspection des diffs avec surlignage visuel des ajouts et suppressions, bouton d'acceptation / rejet.
4. **Live Web Preview** :
   - Onglet Preview avec WebView intégrée exécutant le code HTML/CSS/JS du workspace.
   - Bouton de rechargement à chaud automatique dès qu'un fichier est modifié par l'agent.
   - Sélecteur de viewport : Mode Mobile vs Mode Desktop, console logs intégrée.
5. **Mémoire & Système de Skills (Hermes)** :
   - Onglet dédié visualisant la mémoire à long terme de l'agent (habitudes de code du développeur, règles projet, préférences).
   - Bibliothèque de Skills créées et affinées par l'agent (instructions spécialisées, snippets réutilisables, workflows).

### Identité Visuelle & Thème
- **Palette Dev Studio** :
  - Fond immersif : Slate sombre (`#0B0F17` / `#121824`)
  - Cartes et panneaux de surface : `#1A2234` avec bordures subtiles `#2D3B55`
  - Accents primaires : Neon Emerald / Mint (`#00F5A0`), Cyan (`#00C9FF`)
  - Statuts d'exécution : Succès (`#10B981`), Diff Ajout (`#059669`), Diff Suppression (`#EF4444`), Warning (`#F59E0B`)
- **Typographie** : Famille système sans-serif lisible pour l'UI couplée à une typographie monospace pour les noms de fichiers, snippets de code, logs de tool calls et diffs.

---

## 3. Key Product Decisions & Trade-Offs

- **Client-Side Autonomous Engine** :
  - *Choix* : Moteur de boucle d'agent autonome directement embarqué dans l'application Android (Kotlin Coroutines) exécutant le protocole tool-calling OpenAI standard.
  - *Pourquoi* : Fonctionne immédiatement avec n'importe quel endpoint (Ollama local, OpenRouter, vLLM, OpenAI), ne nécessite aucun serveur tiers intermédiaire pour fonctionner, tout en restant compatible avec une passerelle WebSocket externe si configurée.
- **Stockage Local Room + Fichiers Sandboxés** :
  - *Choix* : Base Room pour les profils, sessions de chat, logs de tool calls, mémoire long terme et skills ; stockage sandbox interne pour l'arborescence des fichiers de code avec compresseur ZIP intégré.
  - *Pourquoi* : Vitesse maximale d'accès I/O sur mobile, aucune permission intrusive obligatoire au premier lancement, persistance robuste et isolation sécurisée.
- **WebView Sandboxée pour la Preview** :
  - *Choix* : WebView Android avec chargement dynamique des assets locaux du projet (HTML/CSS/JS) et interception des logs console JS.
  - *Pourquoi* : Rendu instantané du code généré par l'agent sans dépendre d'un serveur web externe.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HERMES MOBILE (Compose UI)                      │
│                                                                        │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │ Chat & Agent │  │ File Explorer │  │ Live Preview │  │ Memory &  │  │
│  │  Tool Calls  │  │ & Diff Viewer │  │ (WebView M3) │  │  Skills   │  │
│  └──────┬───────┘  └───────┬───────┘  └──────┬───────┘  └─────┬─────┘  │
│         │                  │                 │                │        │
│         └───────────┬──────┴─────────────────┴────────────────┘        │
│                     ▼                                                  │
│          HermesAgentViewModel / WorkspaceViewModel                     │
│                     │                                                  │
│         ┌───────────┴────────────────────────┐                         │
│         ▼                                    ▼                         │
│  AgentEngine (Orchestrator)          WorkspaceManager (Files)          │
│  - Tool Parsing & Execution          - Project File Tree               │
│  - Plan / Supervised Mode            - Diff Generator                  │
│  - Streaming SSE / OkHttp            - ZIP Importer / Exporter         │
│         │                                    │                         │
│         ▼                                    ▼                         │
│  OpenAiApiClient (Retrofit/OkHttp)    Room Database (Local SQLite)     │
│  - GET /v1/models                     - AI Profiles (BaseUrl, Keys)    │
│  - POST /v1/chat/completions (stream) - Chat History & Tool Logs       │
│  - Dynamic Bearer Auth                - Hermes Memory & Skills         │
└────────────────────────────────────────────────────────────────────────┘
```

### Modèle de Données (Room Entities)
1. **AiProfileEntity** : `id`, `name`, `baseUrl`, `apiKey`, `selectedModel`, `isDefault`, `createdAt`.
2. **ProjectEntity** : `id`, `name`, `description`, `rootDirectoryPath`, `lastModified`.
3. **ChatMessageEntity** : `id`, `projectId`, `role` (user/assistant/tool/plan), `content`, `toolCallsJson`, `timestamp`.
4. **ToolCallLogEntity** : `id`, `messageId`, `toolName`, `argumentsJson`, `resultJson`, `status` (pending/approved/rejected/executed), `diffPatch`.
5. **MemoryItemEntity** : `id`, `category` (user_preference/project_context/rule), `key`, `value`, `updatedAt`.
6. **SkillEntity** : `id`, `name`, `description`, `triggerPrompt`, `toolDefinitionJson`, `codeSnippet`, `updatedAt`.

### Outils Autonomes Implémentés pour l'Agent
- `read_file(path)` : Lecture d'un fichier du workspace
- `write_file(path, content)` : Écriture avec génération de diff et validation
- `list_files(directory)` : Exploration des fichiers du projet
- `delete_file(path)` : Suppression sécurisée
- `create_skill(name, description, instructions)` : Auto-apprentissage de l'agent
- `save_memory(key, value, category)` : Stockage persistant des faits clés

---

## 5. Étapes d'Exécution & Vérification

1. **Phase Config & Dépendances** :
   - Mise à jour de `metadata.json` ("Hermes Mobile") et `strings.xml`.
   - Vérification des permissions dans `AndroidManifest.xml` (`INTERNET`, `ACCESS_NETWORK_STATE`).
2. **Phase Données & Réseau** :
   - Entités Room, DAOs et Database (`AppDatabase`).
   - Client HTTP OkHttp / Retrofit avec intercepteur d'authentification dynamique et streaming SSE pour `/v1/chat/completions` et découverte `/v1/models`.
3. **Phase Moteur Agent & Workspace** :
   - Gestionnaire de fichiers de projet (arbre de fichiers, lecture, écriture, zip export/import).
   - Boucle d'agent autonome Hermes (orchestration des tool calls, génération de diffs, gestion des modes Auto vs Supervisé).
4. **Phase Composants UI (Jetpack Compose M3)** :
   - Écran de configuration des profils IA avec découverte en direct des modèles.
   - Écran principal de Chat avec flux temps réel, cartes de tool calls et diff viewer interactif.
   - Écran Workspace (explorateur de fichiers & éditeur de code).
   - Écran Preview (WebView live avec rechargement automatique).
   - Écran Mémoire & Skills (gestion des acquis de l'agent).
5. **Tests & Validation de Build** :
   - Tests unitaires et Robolectric pour les DAOs, le parser de tool calls et le gestionnaire de fichiers.
   - Compilation complète via `compile_applet`.
