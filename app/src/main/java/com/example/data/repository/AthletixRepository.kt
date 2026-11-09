package com.example.data.repository

import com.example.data.local.*
import com.example.domain.NutritionCalculator
import com.example.domain.SecurityUtils
import com.example.domain.WorkoutCalculator
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AthletixRepository(
    private val dao: AthletixDao,
    val aiCoachRepository: AiCoachRepository = PlaceholderAiCoachRepository()
) {

    fun todayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun dateDaysAgoIso(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    suspend fun ensureSeedCatalogLoaded() {
        if (dao.countExercises() == 0) {
            dao.insertExercises(SeedCatalog.initialExercises)
        }
        if (dao.countGlobalPrograms() == 0) {
            dao.insertPrograms(SeedCatalog.initialPrograms)
        }
        if (dao.countFoods() < SeedCatalog.initialFoods.size) {
            dao.insertFoods(SeedCatalog.initialFoods)
        }
    }

    // --- AUTHENTICATION & ACCOUNT MANAGEMENT ---
    fun observeUser(userId: Long): Flow<UserEntity?> = dao.observeUser(userId)

    suspend fun getUserById(userId: Long): UserEntity? = dao.getUserById(userId)

    suspend fun loginWithEmail(email: String, rawPassword: String): Result<UserEntity> {
        if (!SecurityUtils.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Format d'adresse e-mail invalide."))
        }
        val user = dao.getUserByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("Aucun compte trouvé avec cet e-mail."))
        val expectedHash = SecurityUtils.hashPassword(rawPassword)
        if (user.passwordHash != expectedHash) {
            return Result.failure(IllegalArgumentException("Mot de passe incorrect."))
        }
        return Result.success(user)
    }

    suspend fun registerAccount(
        firstName: String,
        email: String,
        rawPassword: String
    ): Result<UserEntity> {
        if (firstName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Veuillez saisir un prénom valide (min. 2 caractères)."))
        }
        if (!SecurityUtils.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Veuillez saisir une adresse e-mail valide."))
        }
        if (rawPassword.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères."))
        }
        val existing = dao.getUserByEmail(email.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Un compte existe déjà avec cet e-mail."))
        }
        val newUser = UserEntity(
            email = email.trim(),
            passwordHash = SecurityUtils.hashPassword(rawPassword),
            firstName = firstName.trim(),
            onboardingCompleted = false,
            isDemoAccount = false
        )
        val newId = dao.insertUser(newUser)
        seedDefaultHabitsAndReminders(newId)
        val created = dao.getUserById(newId) ?: newUser.copy(id = newId)
        return Result.success(created)
    }

    suspend fun resetPassword(email: String, newRawPassword: String): Result<Unit> {
        if (!SecurityUtils.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Adresse e-mail invalide."))
        }
        if (newRawPassword.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 6 caractères."))
        }
        val user = dao.getUserByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("Aucun compte associé à cet e-mail."))
        dao.updateUser(user.copy(passwordHash = SecurityUtils.hashPassword(newRawPassword)))
        return Result.success(Unit)
    }

    suspend fun completeOnboarding(
        userId: Long,
        firstName: String,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        fitnessGoal: String,
        fitnessLevel: String,
        frequencyDays: Int,
        sessionDurationMin: Int,
        equipment: String,
        trainingLocation: String,
        dietaryPreferences: String,
        dietaryRestrictions: String,
        physicalLimitations: String
    ): UserEntity? {
        val current = dao.getUserById(userId) ?: return null
        val plan = NutritionCalculator.calculateTargetNutrition(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            frequencyDays = frequencyDays,
            goal = fitnessGoal
        )
        val updated = current.copy(
            firstName = firstName.trim().ifBlank { current.firstName },
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            fitnessGoal = fitnessGoal,
            fitnessLevel = fitnessLevel,
            frequencyDaysPerWeek = frequencyDays,
            sessionDurationMin = sessionDurationMin,
            equipment = equipment,
            trainingLocation = trainingLocation,
            dietaryPreferences = dietaryPreferences.ifBlank { "Omnivore équilibré" },
            dietaryRestrictions = dietaryRestrictions.ifBlank { "Aucune" },
            physicalLimitations = physicalLimitations.ifBlank { "Aucune" },
            targetCalories = plan.targetCalories,
            targetProteinG = plan.targetProteinG,
            targetCarbsG = plan.targetCarbsG,
            targetFatG = plan.targetFatG,
            targetWaterMl = plan.targetWaterMl,
            onboardingCompleted = true
        )
        dao.updateUser(updated)
        // Log initial body measurement
        dao.insertMeasurement(
            BodyMeasurementEntity(
                userId = userId,
                dateIso = todayIso(),
                weightKg = weightKg,
                bodyFatPct = 16.5,
                chestCm = 100.0,
                waistCm = 81.0,
                hipsCm = 96.0,
                armCm = 36.5,
                thighCm = 57.0
            )
        )
        dao.insertSyncQueueItem(
            SyncQueueEntity(
                userId = userId,
                entityType = "USER_PROFILE",
                operation = "ONBOARDING_COMPLETED",
                summary = "Profil sportif & plan ${plan.targetCalories} kcal configurés",
                status = "CONFIRMED_LOCAL"
            )
        )
        return updated
    }

    suspend fun deleteAccountCompletely(userId: Long) {
        dao.deleteSessionsByUser(userId)
        dao.deleteSetsByUser(userId)
        dao.deleteMealsByUser(userId)
        dao.deleteWaterByUser(userId)
        dao.deleteSleepByUser(userId)
        dao.deleteMeasurementsByUser(userId)
        dao.deleteHabitsByUser(userId)
        dao.clearAiMessages(userId)
        dao.deleteUserById(userId)
    }

    /**
     * Crée ou charge un profil de démonstration clairement identifié (isDemoAccount = true)
     * avec historique réaliste pour tester immédiatement tous les graphiques et modules.
     */
    suspend fun loginOrSeedDemoAthlete(): UserEntity {
        ensureSeedCatalogLoaded()
        val demoEmail = "demo.athlete@athletix.ai"
        val existing = dao.getUserByEmail(demoEmail)
        if (existing != null) {
            return existing
        }
        val plan = NutritionCalculator.calculateTargetNutrition(
            weightKg = 76.5,
            heightCm = 178.0,
            age = 27,
            frequencyDays = 4,
            goal = "Muscle Gain"
        )
        val demoUser = UserEntity(
            email = demoEmail,
            passwordHash = SecurityUtils.hashPassword("demo1234"),
            firstName = "Franck (Démo)",
            age = 27,
            heightCm = 178.0,
            weightKg = 76.5,
            fitnessGoal = "Muscle Gain",
            fitnessLevel = "Intermédiaire",
            frequencyDaysPerWeek = 4,
            sessionDurationMin = 60,
            equipment = "Salle complète",
            trainingLocation = "Salle de sport",
            dietaryPreferences = "Gastronomie Camerounaise & Sportive",
            dietaryRestrictions = "Aucune",
            physicalLimitations = "Aucune",
            targetCalories = plan.targetCalories,
            targetProteinG = plan.targetProteinG,
            targetCarbsG = plan.targetCarbsG,
            targetFatG = plan.targetFatG,
            targetWaterMl = plan.targetWaterMl,
            onboardingCompleted = true,
            isDemoAccount = true
        )
        val userId = dao.insertUser(demoUser)
        seedDefaultHabitsAndReminders(userId)
        seedDemoHistory(userId)
        return dao.getUserById(userId) ?: demoUser.copy(id = userId)
    }

    private suspend fun seedDefaultHabitsAndReminders(userId: Long) {
        val today = todayIso()
        val habits = listOf(
            HabitEntity(userId = userId, title = "Boire 500ml d'eau au réveil", category = "Hydratation", targetTime = "07:00", streakCount = 5, completedDatesCsv = today),
            HabitEntity(userId = userId, title = "Atteindre la cible de protéines", category = "Nutrition", targetTime = "20:00", streakCount = 4, completedDatesCsv = today),
            HabitEntity(userId = userId, title = "10 min Mobilité Hanches & Dos", category = "Mobilité", targetTime = "19:00", streakCount = 3, completedDatesCsv = ""),
            HabitEntity(userId = userId, title = "Couvre-feu écrans 45 min avant sommeil", category = "Sommeil", targetTime = "22:15", streakCount = 6, completedDatesCsv = today)
        )
        habits.forEach { dao.insertHabit(it) }

        val reminders = listOf(
            ReminderSettingEntity(userId = userId, reminderType = "WORKOUT", title = "Rappel Séance d'Entraînement", scheduledTime = "17:30", isEnabled = true, consentGranted = true),
            ReminderSettingEntity(userId = userId, reminderType = "WATER", title = "Hydratation Régulière (250ml)", scheduledTime = "10:00 / 15:00", isEnabled = true, consentGranted = true),
            ReminderSettingEntity(userId = userId, reminderType = "MEAL", title = "Journal Nutrition & Macros", scheduledTime = "13:00 / 20:00", isEnabled = true, consentGranted = true),
            ReminderSettingEntity(userId = userId, reminderType = "SLEEP", title = "Préparation Sommeil & Récupération", scheduledTime = "22:30", isEnabled = false, consentGranted = true),
            ReminderSettingEntity(userId = userId, reminderType = "WEEKLY_REPORT", title = "Bilan Hebdomadaire ATHLETIX", scheduledTime = "Dimanche 18:00", isEnabled = true, consentGranted = true)
        )
        dao.insertReminders(reminders)
    }

    private suspend fun seedDemoHistory(userId: Long) {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        // Seed 5 weight & body measurements over the last 4 weeks
        val weights = listOf(75.2, 75.6, 75.9, 76.2, 76.5)
        weights.forEachIndexed { idx, w ->
            val daysAgo = (4 - idx) * 6
            dao.insertMeasurement(
                BodyMeasurementEntity(
                    userId = userId,
                    dateIso = dateDaysAgoIso(daysAgo),
                    weightKg = w,
                    bodyFatPct = 15.8 - (idx * 0.2),
                    chestCm = 102.0 + (idx * 0.4),
                    waistCm = 80.5 - (idx * 0.2),
                    hipsCm = 97.0,
                    armCm = 37.5 + (idx * 0.2),
                    thighCm = 58.0 + (idx * 0.3),
                    timestampMs = now - daysAgo * dayMs
                )
            )
        }

        // Seed 4 workout sessions
        val sampleSessions = listOf(
            Triple(5, "Upper / Lower Split Athlétique", 4480.0),
            Triple(3, "ATHLETIX Full Body Élite", 4920.0),
            Triple(1, "Push / Pull / Legs (PPL Pro)", 5360.0)
        )
        sampleSessions.forEach { (daysAgo, title, vol) ->
            val startMs = now - daysAgo * dayMs
            val sessionId = dao.insertWorkoutSession(
                WorkoutSessionEntity(
                    userId = userId,
                    programId = 1,
                    programTitle = title,
                    dateIso = dateDaysAgoIso(daysAgo),
                    startTimeEpochMs = startMs - 3300_000L,
                    endTimeEpochMs = startMs,
                    durationSeconds = 3300,
                    totalVolumeKg = vol,
                    completedSetsCount = 12,
                    totalSetsCount = 12,
                    personalNotes = "Excellente congestion et contrôle excentrique.",
                    syncStatus = "SYNCED"
                )
            )
            dao.insertExerciseSets(
                listOf(
                    ExerciseSetEntity(sessionId = sessionId, userId = userId, exerciseId = "ex_bench_press", exerciseName = "Développé Couché Barre", setNumber = 1, reps = 10, weightKg = 75.0, restSeconds = 90, isCompleted = true),
                    ExerciseSetEntity(sessionId = sessionId, userId = userId, exerciseId = "ex_bench_press", exerciseName = "Développé Couché Barre", setNumber = 2, reps = 10, weightKg = 80.0, restSeconds = 90, isCompleted = true),
                    ExerciseSetEntity(sessionId = sessionId, userId = userId, exerciseId = "ex_back_squat", exerciseName = "Squat Arrière Barre (Back Squat)", setNumber = 1, reps = 8, weightKg = 105.0, restSeconds = 120, isCompleted = true),
                    ExerciseSetEntity(sessionId = sessionId, userId = userId, exerciseId = "ex_barbell_row", exerciseName = "Rowing Barre Buste Penché", setNumber = 1, reps = 10, weightKg = 70.0, restSeconds = 90, isCompleted = true)
                )
            )
        }

        // Seed today's meals (Cameroonian & International)
        val today = todayIso()
        dao.insertMealLog(
            MealLogEntity(
                userId = userId,
                dateIso = today,
                mealType = "Breakfast",
                foodId = 15,
                foodName = "Flocons d'avoine complets & Œufs entiers",
                portionGrams = 180.0,
                calories = 520,
                proteinG = 28.4,
                carbsG = 58.0,
                fatG = 16.2,
                fiberG = 8.1
            )
        )
        dao.insertMealLog(
            MealLogEntity(
                userId = userId,
                dateIso = today,
                mealType = "Lunch",
                foodId = 2,
                foodName = "Poulet DG (Plantain mûr, légumes & volaille)",
                portionGrams = 350.0,
                calories = 623,
                proteinG = 47.3,
                carbsG = 49.7,
                fatG = 25.9,
                fiberG = 7.4
            )
        )
        dao.insertMealLog(
            MealLogEntity(
                userId = userId,
                dateIso = today,
                mealType = "Snack",
                foodId = 4,
                foodName = "Koki de niébé (Gâteau de haricots vapeur)",
                portionGrams = 200.0,
                calories = 384,
                proteinG = 17.8,
                carbsG = 38.8,
                fatG = 17.6,
                fiberG = 8.2
            )
        )

        // Seed water & sleep
        dao.insertWaterLog(WaterLogEntity(userId = userId, dateIso = today, amountMl = 1750))
        dao.insertSleepLog(
            SleepLogEntity(
                userId = userId,
                dateIso = today,
                durationHours = 7.8,
                qualityScore = 8,
                fatigueLevel = 3,
                recoveryScore = 86,
                notes = "Sommeil profond réparateur après séance jambes."
            )
        )

        // Seed initial AI Coach welcome message
        dao.insertAiMessage(
            AiMessageEntity(
                userId = userId,
                role = "assistant",
                content = "Bonjour Franck ! Je suis ATHLETIX AI COACH (v1.0). J'ai analysé vos 3 dernières séances (volume record : 5 360 kg) et votre journal nutritionnel (1 527 kcal enregistrées aujourd'hui dont Poulet DG et Koki de niébé). Posez-moi une question sur votre entraînement, votre nutrition ou votre récupération !",
                dataClassification = "MESURÉ • DÉCLARÉ • ESTIMATION • RECOMMANDATION"
            )
        )
    }

    // --- WORKOUT ENGINE OPERATIONS ---
    fun observeAllExercises(): Flow<List<ExerciseEntity>> = dao.observeAllExercises()
    fun observePrograms(userId: Long): Flow<List<WorkoutProgramEntity>> = dao.observeProgramsForUser(userId)
    fun observeWorkoutSessions(userId: Long): Flow<List<WorkoutSessionEntity>> = dao.observeWorkoutSessions(userId)
    fun observeAllSets(userId: Long): Flow<List<ExerciseSetEntity>> = dao.observeAllSetsForUser(userId)

    suspend fun createCustomProgram(
        userId: Long,
        title: String,
        description: String,
        category: String,
        difficulty: String,
        daysPerWeek: Int,
        estimatedDurationMin: Int,
        exerciseIds: List<String>,
        defaultSets: Int,
        defaultReps: Int,
        defaultLoadKg: Double,
        defaultRestSec: Int,
        notes: String
    ): Long {
        val program = WorkoutProgramEntity(
            userId = userId,
            title = title.trim(),
            description = description.trim().ifBlank { "Programme personnalisé créé dans Workout Builder" },
            category = category,
            difficulty = difficulty,
            daysPerWeek = daysPerWeek,
            estimatedDurationMin = estimatedDurationMin,
            exerciseIdsCsv = exerciseIds.joinToString(","),
            defaultSets = defaultSets,
            defaultReps = defaultReps,
            defaultLoadKg = defaultLoadKg,
            defaultRestSec = defaultRestSec,
            personalNotes = notes.trim(),
            isCustom = true
        )
        val id = dao.insertProgram(program)
        dao.insertSyncQueueItem(
            SyncQueueEntity(
                userId = userId,
                entityType = "WORKOUT_PROGRAM",
                operation = "CREATE_PROGRAM",
                summary = "Programme '$title' (${exerciseIds.size} exercices)",
                status = "CONFIRMED_LOCAL"
            )
        )
        return id
    }

    suspend fun deleteCustomProgram(programId: Long) {
        dao.deleteProgram(programId)
    }

    suspend fun saveCompletedWorkoutSession(
        userId: Long,
        programId: Long,
        programTitle: String,
        startTimeMs: Long,
        endTimeMs: Long,
        durationSeconds: Int,
        sets: List<ExerciseSetEntity>,
        notes: String,
        isOfflineMode: Boolean
    ): Long {
        val totalVolume = WorkoutCalculator.calculateSessionVolume(sets)
        val completedCount = sets.count { it.isCompleted }
        val syncState = if (isOfflineMode) "PENDING_OFFLINE" else "SYNCED"

        val sessionId = dao.insertWorkoutSession(
            WorkoutSessionEntity(
                userId = userId,
                programId = programId,
                programTitle = programTitle,
                dateIso = todayIso(),
                startTimeEpochMs = startTimeMs,
                endTimeEpochMs = endTimeMs,
                durationSeconds = durationSeconds,
                totalVolumeKg = totalVolume,
                completedSetsCount = completedCount,
                totalSetsCount = sets.size,
                personalNotes = notes,
                syncStatus = syncState
            )
        )
        val linkedSets = sets.map { it.copy(sessionId = sessionId, userId = userId) }
        dao.insertExerciseSets(linkedSets)
        dao.insertSyncQueueItem(
            SyncQueueEntity(
                userId = userId,
                entityType = "WORKOUT_SESSION",
                operation = "FINISH_WORKOUT",
                summary = "$programTitle • Volume ${totalVolume.toInt()} kg ($completedCount séries)",
                status = if (isOfflineMode) "QUEUED_OFFLINE" else "CONFIRMED_LOCAL"
            )
        )
        return sessionId
    }

    // --- NUTRITION ENGINE OPERATIONS ---
    fun observeAllFoods(): Flow<List<FoodItemEntity>> = dao.observeAllFoods()
    fun observeMeals(userId: Long): Flow<List<MealLogEntity>> = dao.observeMealsForUser(userId)
    fun observeWaterLogs(userId: Long): Flow<List<WaterLogEntity>> = dao.observeWaterLogs(userId)

    suspend fun logFoodPortion(
        userId: Long,
        mealType: String,
        food: FoodItemEntity,
        portionGrams: Double,
        isRecipe: Boolean = false
    ) {
        val macros = NutritionCalculator.calculatePortionMacros(food, portionGrams)
        dao.insertMealLog(
            MealLogEntity(
                userId = userId,
                dateIso = todayIso(),
                mealType = mealType,
                foodId = food.id,
                foodName = food.name,
                portionGrams = portionGrams,
                calories = macros.calories,
                proteinG = macros.proteinG,
                carbsG = macros.carbsG,
                fatG = macros.fatG,
                fiberG = macros.fiberG,
                isRecipe = isRecipe
            )
        )
    }

    suspend fun addCustomFoodOrRecipe(
        name: String,
        originRegion: String,
        referenceGrams: Double,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        source: String
    ): Long {
        return dao.insertFood(
            FoodItemEntity(
                name = name.trim(),
                originRegion = originRegion,
                referenceGrams = referenceGrams.coerceAtLeast(1.0),
                caloriesPerRef = calories.coerceAtLeast(0),
                proteinPerRef = proteinG.coerceAtLeast(0.0),
                carbsPerRef = carbsG.coerceAtLeast(0.0),
                fatPerRef = fatG.coerceAtLeast(0.0),
                fiberPerRef = fiberG.coerceAtLeast(0.0),
                nutritionalSource = source.ifBlank { "Saisie Utilisateur Vérifiée" },
                isCustom = true
            )
        )
    }

    suspend fun deleteMealLog(mealId: Long) = dao.deleteMealLog(mealId)

    suspend fun addWaterIntake(userId: Long, amountMl: Int) {
        dao.insertWaterLog(
            WaterLogEntity(
                userId = userId,
                dateIso = todayIso(),
                amountMl = amountMl
            )
        )
    }

    // --- ANALYTICS, RECOVERY, HABITS & REMINDERS ---
    fun observeSleepLogs(userId: Long): Flow<List<SleepLogEntity>> = dao.observeSleepLogs(userId)
    fun observeMeasurements(userId: Long): Flow<List<BodyMeasurementEntity>> = dao.observeMeasurements(userId)
    fun observeHabits(userId: Long): Flow<List<HabitEntity>> = dao.observeHabits(userId)
    fun observeReminders(userId: Long): Flow<List<ReminderSettingEntity>> = dao.observeReminders(userId)
    fun observeAiMessages(userId: Long): Flow<List<AiMessageEntity>> = dao.observeAiMessages(userId)
    fun observeSyncQueue(userId: Long): Flow<List<SyncQueueEntity>> = dao.observeSyncQueue(userId)

    suspend fun logSleepAndRecovery(
        userId: Long,
        durationHours: Double,
        qualityScore: Int,
        fatigueLevel: Int,
        notes: String
    ) {
        val recoveryScore = ((durationHours / 8.0).coerceIn(0.0, 1.2) * 45.0 +
            (qualityScore.coerceIn(1, 10) * 3.5) +
            ((11 - fatigueLevel.coerceIn(1, 10)) * 2.0)).toInt().coerceIn(10, 100)
        dao.insertSleepLog(
            SleepLogEntity(
                userId = userId,
                dateIso = todayIso(),
                durationHours = durationHours,
                qualityScore = qualityScore,
                fatigueLevel = fatigueLevel,
                recoveryScore = recoveryScore,
                notes = notes.trim()
            )
        )
    }

    suspend fun logBodyMeasurement(
        userId: Long,
        weightKg: Double,
        bodyFatPct: Double,
        chestCm: Double,
        waistCm: Double,
        hipsCm: Double,
        armCm: Double,
        thighCm: Double
    ) {
        dao.insertMeasurement(
            BodyMeasurementEntity(
                userId = userId,
                dateIso = todayIso(),
                weightKg = weightKg,
                bodyFatPct = bodyFatPct,
                chestCm = chestCm,
                waistCm = waistCm,
                hipsCm = hipsCm,
                armCm = armCm,
                thighCm = thighCm
            )
        )
        val user = dao.getUserById(userId)
        if (user != null) {
            dao.updateUser(user.copy(weightKg = weightKg))
        }
    }

    suspend fun toggleHabitToday(habit: HabitEntity) {
        val today = todayIso()
        val dates = habit.completedDatesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        val newStreak: Int
        if (dates.contains(today)) {
            dates.remove(today)
            newStreak = (habit.streakCount - 1).coerceAtLeast(0)
        } else {
            dates.add(today)
            newStreak = habit.streakCount + 1
        }
        dao.updateHabit(
            habit.copy(
                streakCount = newStreak,
                completedDatesCsv = dates.joinToString(",")
            )
        )
    }

    suspend fun addHabit(userId: Long, title: String, category: String, targetTime: String) {
        dao.insertHabit(
            HabitEntity(
                userId = userId,
                title = title.trim(),
                category = category,
                targetTime = targetTime,
                streakCount = 0,
                completedDatesCsv = ""
            )
        )
    }

    suspend fun deleteHabit(habitId: Long) = dao.deleteHabit(habitId)

    suspend fun updateReminder(reminder: ReminderSettingEntity) = dao.updateReminder(reminder)

    suspend fun syncPendingOfflineQueue(userId: Long) {
        dao.updateAllSessionsSyncStatus(userId, "SYNCED")
        dao.updateSyncQueueStatus(userId, "CONFIRMED_LOCAL")
    }

    suspend fun sendMessageToCoach(
        userId: Long,
        userText: String,
        userProfile: UserEntity?,
        recentSessions: List<WorkoutSessionEntity>,
        todayMacros: com.example.domain.MacroNutrients,
        todayWaterMl: Int,
        lastSleepHours: Double
    ) {
        dao.insertAiMessage(
            AiMessageEntity(
                userId = userId,
                role = "user",
                content = userText.trim(),
                dataClassification = "QUESTION UTILISATEUR"
            )
        )
        val reply = aiCoachRepository.askCoach(
            userMessage = userText,
            userProfile = userProfile,
            recentSessions = recentSessions,
            todayMacros = todayMacros,
            todayWaterMl = todayWaterMl,
            lastSleepHours = lastSleepHours
        )
        dao.insertAiMessage(
            AiMessageEntity(
                userId = userId,
                role = "assistant",
                content = reply.text,
                dataClassification = reply.classificationBadge,
                promptVersion = reply.promptVersion
            )
        )
    }
}
