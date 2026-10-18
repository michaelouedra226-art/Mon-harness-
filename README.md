# Hermes Mobile 🚀

**Hermes Mobile** est un agent de codage autonome pour Android, inspiré des agents de Nous Research (Hermes), capable d'interagir avec n'importe quelle API compatible OpenAI (OpenAI, Ollama, OpenRouter, Groq, LM Studio, vLLM) en streaming temps réel et via WebSocket bidirectionnel.

---

## ⚡ Fonctionnalités Clés

- **Temps Réel & Dual-Protocol** :
  - **Direct Streaming SSE** : streaming token par token avec affichage immédiat en direct (`▌`).
  - **Passerelle WebSocket Temps Réel** : protocole d'événements typés JSON avec mesure continue de latence (heartbeat ping/pong 3s) et reconnexion automatique.
- **Agent Autonome & Tool Calls** :
  - Lecture, écriture et suppression de fichiers (`read_file`, `write_file`, `list_files`, `delete_file`).
  - **Mode Plan** : formulation d'un plan détaillé avant action.
  - **Mode Supervisé** : calcul automatique de diffs (LCS) avec boutons d'approbation et rejet avant écriture.
- **Espace de Travail de Fichiers & Prévisualisation Web en Direct** :
  - Bac à sable de fichiers par projet avec éditeur de code monospace intégré.
  - Export et import de projets sous forme d'archive `.zip`.
  - Prévisualisation live WebView avec rechargement à chaud automatique (`LIVE SYNC`) et console JavaScript intégrée.
- **Mémoire & Système de Skills** :
  - Mémoire long terme persistante (Room SQLite).
  - Catalogue de compétences modulaires activables à la demande (`create_skill`).

---

## 📦 Compilation Automatisée sur GitHub Actions

Le projet intègre un pipeline CI/CD automatisé dans `.github/workflows/build-apk.yml`.

### Déclenchement automatique :
- À chaque `git push` sur `main` ou `master`
- À chaque `pull_request`
- Manuellement depuis l'onglet **Actions** > **Build Android APKs** > **Run workflow** (`workflow_dispatch`)

### Récupération des APKs générés :
1. Sur GitHub, rendez-vous dans l'onglet **Actions**.
2. Cliquez sur la dernière exécution terminée du workflow **Build Android APKs**.
3. Descendez tout en bas de la page dans la section **Artifacts**.
4. Téléchargez l'archive **`HermesMobile-APKs`** contenant :
   - `HermesMobile-v1.0-debug.apk` (prêt à installer et tester immédiatement)
   - `HermesMobile-v1.0-release.apk` (optimisé pour la production)

### Configuration des Secrets de Signature (Optionnel) :
Si vous souhaitez signer l'APK Release avec votre propre clé officielle plutôt que la clé CI générée automatiquement, configurez ces secrets dans votre dépôt GitHub (**Settings** > **Secrets and variables** > **Actions**) :
- `KEYSTORE_BASE64` : Le contenu encodé en base64 de votre fichier `.jks` (`base64 -w 0 mon-keystore.jks`)
- `STORE_PASSWORD` : Mot de passe du keystore
- `KEY_PASSWORD` : Mot de passe de l'alias de clé
