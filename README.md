# 🏋️‍♂️ ATHLETIX AI — Personal Fitness Intelligence Platform

![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024--36)-B7F34A?style=for-the-badge&logo=android&logoColor=black)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-8FCE32?style=for-the-badge&logo=kotlin&logoColor=black)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-0B0D0F?style=for-the-badge&logo=jetpackcompose&logoColor=B7F34A)
![Database](https://img.shields.io/badge/Database-Room%20SQLite-1B2025?style=for-the-badge&logo=sqlite&logoColor=white)

> **Slogan :** *TRAIN SMARTER. LIVE STRONGER.*  
> **Auteur & Architecte Logiciel :** **Mendel Temgoua** (`mendeltemgouabusiness@gmail.com`)  
> **Version :** `2.0.0` (Build 2 — 2026)

---

## 📌 Présentation du Projet

**ATHLETIX AI** est une plateforme personnelle complète d'intelligence sportive conçue et développée par **Mendel Temgoua**. Elle accompagne les débutants, sportifs intermédiaires et athlètes avancés dans la perte de poids, la prise de masse musculaire, le gain de force, l'endurance et la mobilité.

L'application se distingue par :
- **Un moteur de calcul 100 % déterministe** (équation métabolique de *Mifflin-St Jeor*, calcul du volume réel d'entraînement $\sum \text{séries} \times \text{répétitions} \times \text{charge}$, estimation du 1RM via la formule d'*Epley*, et calcul précis des macronutriments au gramme près).
- **Une base alimentaire africaine (Cameroun) et internationale extensible** intégrant les valeurs nutritionnelles vérifiées (tables FAO / INSPC Cameroun & USDA) pour des plats tels que le *Ndolé*, le *Poulet DG*, l'*Eru*, le *Koki de niébé*, le *Poisson braisé de Douala*, le *Plantain mûr*, le *Miondo/Bobolo*, le *Kondrè* et le *Sanga*.
- **Une architecture réactive hors-ligne d'abord (Offline-First)** propulsée par **Room SQLite**, **Kotlin Coroutines / StateFlow** et **Jetpack Compose Material 3**.

---

## 🎨 Design System

| Token | Couleur (Dark Mode) | Usage |
| :--- | :--- | :--- |
| `Primary Background` | `#0B0D0F` | Fond principal haute profondeur |
| `Secondary Background` | `#15191D` | Surfaces secondaires, barres de navigation |
| `Card Background` | `#1B2025` | Cartes modulaires arrondies |
| `Primary Accent` | `#B7F34A` | Vert citron athlétique (actions principales, jauges) |
| `Secondary Accent` | `#8FCE32` | Graphiques secondaires, indicateurs positifs |
| `Primary Text` | `#FFFFFF` | Titres et données clés |
| `Secondary Text` | `#A1A7B0` | Légendes et sous-titres |
| `Border` | `#2B3238` | Contours subtils des cartes |
| `Warning` / `Danger` | `#FFB547` / `#FF5D5D` | Alertes de sécurité, repos et suppressions |

- **Typographie** : Police **Inter** embarquée localement (`app/src/main/res/font/inter.ttf`).
- **Thèmes & Langues** : Bascule instantanée **Dark / Light Mode** et **Français / English**.

---

## 🚀 Fonctionnalités Principales

1. **Authentification & Onboarding Progressif (4 Étapes)**
   - Création de compte local sécurisé (hachage SHA-256), connexion, récupération de mot de passe et **Profil Athlète de Démonstration** pré-rempli.
   - Questionnaire biométrique générant automatiquement le plan calorique, les cibles de macronutriments, l'hydratation quotidienne et le programme sportif initial.
2. **Dashboard Dynamique (10 Sections)**
   - *Daily Fitness Score* (jauge circulaire 0–100), objectif principal, filtres temporels (`7J`, `30J`, `1A`), séance du jour en 1 clic, bilan nutritionnel, suivi d'hydratation (`+250ml`, `+500ml`), résumé du sommeil, activités récentes, graphiques Canvas et aperçu *AI Coach*.
3. **Workout Engine Complet**
   - **Bibliothèque d'exercices** (10 catégories musculaires : *Chest, Back, Shoulders, Arms, Legs, Glutes, Abs, Full Body, Cardio, Mobility*) avec instructions étape par étape, erreurs fréquentes, sécurité et variantes.
   - **Programmes & Workout Builder** : 9 programmes inclus + créateur de programmes personnalisés.
   - **Séance Active (`Active Workout`)** : Chronomètre en temps réel, minuteur de repos automatique, validation série par série et enregistrement du volume réel.
4. **Nutrition Engine (Cameroun, Afrique & International)**
   - Journal quotidien (*Breakfast, Lunch, Dinner, Snack*), recherche filtrée par région, créateur de recettes maison et calculateur métabolique.
5. **Analytiques, Suivi Corporel, Récupération & Habitudes**
   - Graphiques d'évolution du poids et du volume, records personnels (`1RM Epley`), suivi des mensurations, journal de sommeil/fatigue, habitudes quotidiennes avec séries (*streaks*) et rappels configurables avec consentement explicite.
6. **ATHLETIX AI COACH**
   - Assistant sportif structurant ses réponses entre **Données mesurées**, **Données déclarées**, **Estimations** et **Recommandations**, avec prompt système versionné (`ATHLETIX-COACH-SYS-v1.0`) et protection contre les *prompt injections*.

---

## 📂 Architecture & Structure des Dossiers

```text
athletix-ai/
├── .github/workflows/
│   └── android-apk-release.yml                # Pipeline CI/CD GitHub Actions générant l'APK automatiquement
├── app/
│   ├── build.gradle.kts                       # Configuration Gradle, Room KSP, Compose BOM, SigningConfig
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml            # Permissions (POST_NOTIFICATIONS, VIBRATE) & Icône adaptative
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt            # Point d'entrée & Navigation adaptative (Mobile / Tablette)
│       │   │   ├── domain/
│       │   │   │   └── FitnessCalculators.kt  # Calculs déterministes (Nutrition, Volume, 1RM, SHA-256)
│       │   │   ├── data/
│       │   │   │   ├── local/                 # Room Database (14 entités, DAO Flow, SeedCatalog)
│       │   │   │   └── repository/            # AthletixRepository & AiCoachRepository
│       │   │   └── ui/
│       │   │       ├── AthletixViewModel.kt   # StateFlow global, chronomètre & minuteur de repos
│       │   │       ├── components/            # Cartes, jauges circulaires & graphiques Canvas
│       │   │       ├── screens/               # Écrans modulaires (Auth, Dashboard, Workout, Nutrition, etc.)
│       │   │       └── theme/                 # Couleurs (#0B0D0F / #B7F34A), Typographie Inter, Thème M3
│       │   └── res/                           # Polices (Inter), Icônes mipmap PNG & ressources XML
│       └── test/java/com/example/             # Tests unitaires JUnit4 & tests d'intégration Robolectric
├── LICENSE                                    # Licence MIT — Mendel Temgoua
└── README.md                                  # Documentation complète du projet
```

---

## 📱 Comment Installer l'APK sur votre Téléphone & Publier sur GitHub

### Option A — Depuis l'interface AI Studio (En 1 clic)
1. **Télécharger l'APK prêt à installer** :
   - Cliquez sur le menu **Paramètres / Export (icône d'engrenage ou bouton de téléchargement en haut à droite)** dans AI Studio.
   - Sélectionnez **Download APK** (ou **Build APK**).
   - Transférez le fichier `.apk` sur votre téléphone Android et ouvrez-le pour l'installer (autorisez *"Installer des applications inconnues"* si demandé).
2. **Publier directement sur votre compte GitHub** :
   - Cliquez sur l'icône **GitHub** (ou **Push to GitHub** dans le menu d'exportation d'AI Studio).
   - Connectez votre compte GitHub et choisissez le nom du dépôt (ex: `athletix-ai`). Tout le code source, ce `README.md`, la licence et le workflow GitHub Actions seront publiés sous votre nom.

### Option B — Compiler l'APK en local ou via GitHub Actions
- **Via GitHub Actions (Automatique)** : Dès que vous poussez le projet sur GitHub, le workflow `.github/workflows/android-apk-release.yml` compile automatiquement l'APK et le met à disposition dans l'onglet **Actions > Artifacts** (`Athletix-AI-v1.0-Installable-APK`).
- **En ligne de commande locale** :
  ```bash
  gradle assembleDebug
  ```
  L'APK signé prêt à être installé sur téléphone est généré dans :
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Exécution des Tests

Pour lancer la suite complète de tests unitaires (calculs nutritionnels, volume d'entraînement, 1RM, sécurité anti-injection) et les tests Robolectric :

```bash
gradle :app:testDebugUnitTest
```

---

## 👤 Auteur & Licence

- **Créé et développé par :** **Mendel Temgoua**
- **Contact :** [mendeltemgouabusiness@gmail.com](mailto:mendeltemgouabusiness@gmail.com)
- **Licence :** Distribué sous licence **MIT** (voir le fichier [LICENSE](LICENSE)).
