# Résolution du Build GitHub Actions – Robolectric API 34 & Nettoyage des Avertissements

Correction ciblée de l'échec d'exécution du workflow GitHub Actions `Build Android APKs` causé par l'incompatibilité de l'API SDK 36 dans le runner Robolectric (`java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170`), et nettoyage proactif des avertissements de compilation (icônes dépréciées et cibles d'annotations Kotlin).

## User Review & Critical Decisions

> [!IMPORTANT]
> Les deux choix confirmés lors de la clarification sont pris en compte :

- **Version SDK Robolectric** : Bascule de `@Config(sdk = [36])` vers `@Config(sdk = [34])` dans `ExampleRobolectricTest.kt`. L'API 34 (Android 14) est universellement stable et pré-intégrée dans Robolectric 4.14+/4.16+, évitant l'échec de téléchargement ou d'instrumentation sur les VM Ubuntu de GitHub Actions.
- **Nettoyage des Avertissements de Compilation** :
  - Remplacement des icônes Material dépréciées par leurs variantes officielles `Icons.AutoMirrored` (`Icons.AutoMirrored.Filled.ListAlt`, `Icons.AutoMirrored.Filled.InsertDriveFile`).
  - Alignement des annotations `@property:Json` dans `OpenAiModels.kt` pour supprimer les warnings Kotlin 2.x sur les paramètres de constructeur de données.

---

## 1. Overview & Cause de l'Erreur CI

- **Analyse du Run 39167950432** :
  - Dans l'environnement GitHub Actions (`ubuntu-latest`), le runner JVM exécute Robolectric pour `:app:testDebugUnitTest`.
  - Lors de l'initialisation de `ExampleRobolectricTest`, la configuration annotée `@Config(sdk = [36])` a forcé `DefaultSdkProvider` à rechercher les jars d'instrumentation d'Android 16 (Baklava / API 36). Comme cette version préliminaire n'est pas supportée nativement dans la distribution standard de Robolectric, celui-ci a levé `java.lang.UnsupportedOperationException`.
- **Solution Appliquée** :
  - Fixer l'exécution des tests Robolectric sur l'API 34 (`@Config(sdk = [34])`).
  - L'application Android continue de cibler `compileSdk = 36` et `targetSdk = 36` pour les appareils modernes tout en garantissant des tests unitaires déterministes et instantanés en CI.

---

## 2. Plan d'Implémentation Détaillé

### Étape 1 : Correction du test Robolectric
- Dans `app/src/test/java/com/example/ExampleRobolectricTest.kt` :
  - Remplacer `@Config(sdk = [36])` par `@Config(sdk = [34])`.

### Étape 2 : Nettoyage des Icônes Dépréciées
- Dans `app/src/main/java/com/example/ui/screens/ChatScreen.kt` :
  - Remplacer `Icons.Default.ListAlt` par `Icons.AutoMirrored.Filled.ListAlt`.
- Dans `app/src/main/java/com/example/ui/screens/WorkspaceScreen.kt` :
  - Remplacer `Icons.Default.InsertDriveFile` par `Icons.AutoMirrored.Filled.InsertDriveFile`.

### Étape 3 : Nettoyage des Annotations Kotlin 2.x
- Dans `app/src/main/java/com/example/data/remote/model/OpenAiModels.kt` :
  - Expliciter la cible des annotations `@property:Json(name = "...")` sur les data classes Moshi.

### Étape 4 : Validation Locale
- Exécution de `gradle :app:testDebugUnitTest` pour vérifier que 100 % des tests passent sans exception.
- Exécution de `compile_applet` pour garantir l'absence de régression de compilation.
