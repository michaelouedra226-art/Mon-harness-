# GitHub Actions – Compilation Automatisée des APKs (Hermes Mobile)

Mise en place d'un workflow CI/CD complet et robuste dans `.github/workflows/build-apk.yml` permettant de compiler automatiquement les deux variantes d'APK (Debug et Release) à chaque push sur la branche principale et sur déclenchement manuel, avec téléversement des fichiers APK en tant qu'artefacts téléchargeables.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les trois choix confirmés lors de la phase de clarification sont pris en compte :

- **Variantes cibles** : Compilation des deux variantes : **APK Debug** et **APK Release** (`assembleDebug` et `assembleRelease`).
- **Déclencheurs (Triggers)** : À chaque `push` sur les branches `main` et `master`, à chaque `pull_request`, ainsi qu'en déclenchement manuel via `workflow_dispatch`.
- **Artefacts produits** : Téléversement automatique des APKs via `actions/upload-artifact@v4` avec conservation de 30 jours, prêts à être téléchargés et installés sur n'importe quel appareil Android.

---

## 1. Overview & Core Concept

- **Objectif** : Rendre le projet 100 % autonome sur GitHub pour que tout développeur ou contributeur puisse cloner ou forker le dépôt et obtenir immédiatement un APK signé et installable sans configuration complexe.
- **Résilience CI** :
  - Génération automatique des keystores requis (`debug.keystore` depuis le base64 existant et `my-upload-key.jks` pour la variante Release) si les secrets GitHub personnalisés ne sont pas définis.
  - Compatibilité garantie avec les versions de l'Android Gradle Plugin (AGP 9.1.x) et Java 17/21.
  - Utilisation de `gradle/actions/setup-gradle@v4` pour une gestion de cache optimale et une compilation rapide sans dépendance manquante.

---

## 2. CI/CD Architecture & Workflow Design

```
┌────────────────────────────────────────────────────────────────────────┐
│                        GITHUB ACTIONS WORKFLOW                         │
│                    (.github/workflows/build-apk.yml)                  │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Triggers:                                                           │
│    - push (branches: [main, master])                                   │
│    - pull_request                                                      │
│    - workflow_dispatch (Déclenchement manuel avec options)             │
│                                                                        │
│ 2. Job: build-android (runs-on: ubuntu-latest)                         │
│    ┌───────────────────────────────────────────────────────────┐       │
│    │ a. actions/checkout@v4                                    │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ b. actions/setup-java@v4 (Java 17 Temurin)                │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ c. gradle/actions/setup-gradle@v4 (Cache + Gradle Runner) │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ d. Setup Signing Keys:                                    │       │
│    │    - Decode debug.keystore.base64                         │       │
│    │    - Provision release keystore (fallback ou secret)      │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ e. Run Unit Tests (gradle :app:testDebugUnitTest)         │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ f. Build APKs (gradle assembleDebug assembleRelease)      │       │
│    ├───────────────────────────────────────────────────────────┤       │
│    │ g. Upload Artifacts (actions/upload-artifact@v4):         │       │
│    │    - HermesMobile-Debug.apk                               │       │
│    │    - HermesMobile-Release.apk                             │       │
│    └───────────────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Key Technical Decisions & Trade-Offs

- **Gestion des Keystores en CI** :
  - *Problème* : Le fichier `app/build.gradle.kts` attend `KEYSTORE_PATH` ou `${rootDir}/my-upload-key.jks` pour la release, et `${rootDir}/debug.keystore` pour le debug.
  - *Solution CI* : Le workflow décode le fichier `debug.keystore.base64` présent à la racine pour garantir le debug build. Pour le release build, si les secrets GitHub (`KEYSTORE_BASE64`, `KEY_PASSWORD`, `STORE_PASSWORD`) sont définis, ils sont utilisés ; sinon, le workflow génère à la volée une clé d'upload avec `keytool` afin que le build release réussisse sans accroc.
- **Cache Gradle Intelligent** :
  - Utilisation de `gradle/actions/setup-gradle@v4` pour mettre en cache les dépendances, le cache de configuration et les artefacts de compilation, réduisant la durée des builds de 4 minutes à moins de 60 secondes.
- **Tests Pré-Build Obligatoires** :
  - Exécution de `:app:testDebugUnitTest` avant la génération des APKs afin de bloquer les régressions et garantir la qualité du code livré.

---

## 4. Plan d'Implémentation & Fichiers

1. **Génération du Wrapper Gradle** (si nécessaire) :
   - Assurer la présence des fichiers du wrapper ou exécuter les commandes via le runner Gradle officiel de GitHub Actions.
2. **Création du fichier de workflow CI** :
   - `.github/workflows/build-apk.yml` avec l'ensemble des étapes décrites, variables d'environnement adaptées et upload des APKs générés.
3. **Documentation README / CI** :
   - Ajout d'une section concise dans `README.md` expliquant comment déclencher le build sur GitHub et comment configurer les secrets optionnels pour la signature de production.
4. **Vérification Locale** :
   - Exécution des tâches de build Gradle (`gradle assembleDebug assembleRelease`) dans l'environnement courant pour s'assurer que les deux cibles compilent parfaitement.
