package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.AthletixRepository
import com.example.domain.MacroNutrients
import com.example.domain.NutritionCalculator
import com.example.domain.WorkoutCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class PublicRoute { WELCOME, LOGIN, REGISTER, FORGOT_PASSWORD }
enum class MainTab { HOME, WORKOUT, NUTRITION, PROGRESS, PROFILE }

data class ActiveSetDraft(
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val restSeconds: Int,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false
)

data class ActiveWorkoutState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val programId: Long = 0,
    val programTitle: String = "",
    val startTimeMs: Long = 0L,
    val elapsedSeconds: Int = 0,
    val restRemainingSeconds: Int = 0,
    val sets: List<ActiveSetDraft> = emptyList(),
    val sessionNotes: String = ""
)

data class AthletixUiState(
    val isInitialized: Boolean = false,
    val publicRoute: PublicRoute = PublicRoute.WELCOME,
    val mainTab: MainTab = MainTab.HOME,
    val isAiCoachOpen: Boolean = false,
    val isDarkTheme: Boolean = true,
    val languageCode: String = "FR", // "FR" or "EN"
    val isOfflineMode: Boolean = false,
    val authError: String? = null,
    val statusBannerMessage: String? = null,
    val currentUser: UserEntity? = null,
    val exercises: List<ExerciseEntity> = emptyList(),
    val programs: List<WorkoutProgramEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val allSets: List<ExerciseSetEntity> = emptyList(),
    val foods: List<FoodItemEntity> = emptyList(),
    val meals: List<MealLogEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val sleepLogs: List<SleepLogEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val reminders: List<ReminderSettingEntity> = emptyList(),
    val aiMessages: List<AiMessageEntity> = emptyList(),
    val syncQueue: List<SyncQueueEntity> = emptyList(),
    val activeWorkout: ActiveWorkoutState = ActiveWorkoutState(),
    val exportedJsonData: String? = null
) {
    val todayIso: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    val todayMeals: List<MealLogEntity>
        get() = meals.filter { it.dateIso == todayIso }

    val todayMacros: MacroNutrients
        get() = NutritionCalculator.calculateTotals(todayMeals)

    val todayWaterMl: Int
        get() = waterLogs.filter { it.dateIso == todayIso }.sumOf { it.amountMl }

    val lastSleepLog: SleepLogEntity?
        get() = sleepLogs.firstOrNull()

    val completedHabitsTodayCount: Int
        get() = habits.count { it.completedDatesCsv.split(",").contains(todayIso) }

    val dailyFitnessScore: Int
        get() = WorkoutCalculator.calculateDailyFitnessScore(
            workoutsThisWeek = sessions.take(7).size,
            targetWorkoutsPerWeek = currentUser?.frequencyDaysPerWeek ?: 4,
            todayCalories = todayMacros.calories,
            targetCalories = currentUser?.targetCalories ?: 2500,
            todayWaterMl = todayWaterMl,
            targetWaterMl = currentUser?.targetWaterMl ?: 2800,
            lastSleepHours = lastSleepLog?.durationHours ?: 7.5,
            completedHabitsToday = completedHabitsTodayCount,
            totalHabits = habits.size
        )
}

class AthletixViewModel(
    private val repository: AthletixRepository,
    context: Context
) : ViewModel() {

    private val prefs = context.applicationContext.getSharedPreferences("athletix_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        AthletixUiState(
            isDarkTheme = prefs.getBoolean("dark_theme", true),
            languageCode = prefs.getString("lang_code", "FR") ?: "FR"
        )
    )
    val uiState: StateFlow<AthletixUiState> = _uiState.asStateFlow()

    private var userDataJob: Job? = null
    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeedCatalogLoaded()
            // Observe global catalogs
            launch {
                repository.observeAllExercises().collect { list ->
                    _uiState.update { it.copy(exercises = list) }
                }
            }
            launch {
                repository.observeAllFoods().collect { list ->
                    _uiState.update { it.copy(foods = list) }
                }
            }

            val savedUserId = prefs.getLong("active_user_id", -1L)
            if (savedUserId > 0L) {
                val existing = repository.getUserById(savedUserId)
                if (existing != null) {
                    bindUserStreams(existing.id)
                }
            }
            _uiState.update { it.copy(isInitialized = true) }
        }
    }

    private fun bindUserStreams(userId: Long) {
        prefs.edit().putLong("active_user_id", userId).apply()
        userDataJob?.cancel()
        userDataJob = viewModelScope.launch {
            launch {
                repository.observeUser(userId).collect { user ->
                    _uiState.update { it.copy(currentUser = user) }
                }
            }
            launch {
                repository.observePrograms(userId).collect { list ->
                    _uiState.update { it.copy(programs = list) }
                }
            }
            launch {
                repository.observeWorkoutSessions(userId).collect { list ->
                    _uiState.update { it.copy(sessions = list) }
                }
            }
            launch {
                repository.observeAllSets(userId).collect { list ->
                    _uiState.update { it.copy(allSets = list) }
                }
            }
            launch {
                repository.observeMeals(userId).collect { list ->
                    _uiState.update { it.copy(meals = list) }
                }
            }
            launch {
                repository.observeWaterLogs(userId).collect { list ->
                    _uiState.update { it.copy(waterLogs = list) }
                }
            }
            launch {
                repository.observeSleepLogs(userId).collect { list ->
                    _uiState.update { it.copy(sleepLogs = list) }
                }
            }
            launch {
                repository.observeMeasurements(userId).collect { list ->
                    _uiState.update { it.copy(measurements = list) }
                }
            }
            launch {
                repository.observeHabits(userId).collect { list ->
                    _uiState.update { it.copy(habits = list) }
                }
            }
            launch {
                repository.observeReminders(userId).collect { list ->
                    _uiState.update { it.copy(reminders = list) }
                }
            }
            launch {
                repository.observeAiMessages(userId).collect { list ->
                    _uiState.update { it.copy(aiMessages = list) }
                }
            }
            launch {
                repository.observeSyncQueue(userId).collect { list ->
                    _uiState.update { it.copy(syncQueue = list) }
                }
            }
        }
    }

    // --- NAVIGATION & SETTINGS ---
    fun navigatePublic(route: PublicRoute) {
        _uiState.update { it.copy(publicRoute = route, authError = null) }
    }

    fun selectMainTab(tab: MainTab) {
        _uiState.update { it.copy(mainTab = tab) }
    }

    fun setAiCoachOpen(open: Boolean) {
        _uiState.update { it.copy(isAiCoachOpen = open) }
    }

    fun toggleDarkTheme() {
        val next = !_uiState.value.isDarkTheme
        prefs.edit().putBoolean("dark_theme", next).apply()
        _uiState.update { it.copy(isDarkTheme = next) }
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.languageCode == "FR") "EN" else "FR"
        prefs.edit().putString("lang_code", next).apply()
        _uiState.update { it.copy(languageCode = next) }
    }

    fun toggleOfflineMode() {
        val next = !_uiState.value.isOfflineMode
        _uiState.update {
            it.copy(
                isOfflineMode = next,
                statusBannerMessage = if (next) {
                    "Mode Hors-Ligne activé : vos séances seront placées en file d'attente locale."
                } else {
                    "Connexion rétablie : synchronisation de la file d'attente possible."
                }
            )
        }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null, authError = null) }
    }

    // --- AUTH ACTIONS ---
    fun login(email: String, password: String) {
        viewModelScope.launch {
            val res = repository.loginWithEmail(email, password)
            res.onSuccess { user ->
                _uiState.update { it.copy(authError = null, mainTab = MainTab.HOME) }
                bindUserStreams(user.id)
            }.onFailure { err ->
                _uiState.update { it.copy(authError = err.message) }
            }
        }
    }

    fun register(firstName: String, email: String, password: String) {
        viewModelScope.launch {
            val res = repository.registerAccount(firstName, email, password)
            res.onSuccess { user ->
                _uiState.update { it.copy(authError = null, mainTab = MainTab.HOME) }
                bindUserStreams(user.id)
            }.onFailure { err ->
                _uiState.update { it.copy(authError = err.message) }
            }
        }
    }

    fun resetPassword(email: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.resetPassword(email, newPassword)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        authError = null,
                        publicRoute = PublicRoute.LOGIN,
                        statusBannerMessage = "Mot de passe réinitialisé avec succès."
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(authError = err.message) }
            }
        }
    }

    fun startDemoAthleteSession() {
        viewModelScope.launch {
            val demoUser = repository.loginOrSeedDemoAthlete()
            _uiState.update { it.copy(authError = null, mainTab = MainTab.HOME) }
            bindUserStreams(demoUser.id)
        }
    }

    fun logout() {
        stopWorkoutTimers()
        userDataJob?.cancel()
        prefs.edit().remove("active_user_id").apply()
        _uiState.update {
            it.copy(
                currentUser = null,
                publicRoute = PublicRoute.WELCOME,
                mainTab = MainTab.HOME,
                activeWorkout = ActiveWorkoutState()
            )
        }
    }

    fun deleteCurrentAccount() {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.deleteAccountCompletely(uid)
            logout()
            _uiState.update { it.copy(statusBannerMessage = "Votre compte et toutes vos données ont été supprimés.") }
        }
    }

    fun submitOnboarding(
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
    ) {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.completeOnboarding(
                userId = uid,
                firstName = firstName,
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                fitnessGoal = fitnessGoal,
                fitnessLevel = fitnessLevel,
                frequencyDays = frequencyDays,
                sessionDurationMin = sessionDurationMin,
                equipment = equipment,
                trainingLocation = trainingLocation,
                dietaryPreferences = dietaryPreferences,
                dietaryRestrictions = dietaryRestrictions,
                physicalLimitations = physicalLimitations
            )
            _uiState.update {
                it.copy(
                    mainTab = MainTab.HOME,
                    statusBannerMessage = "Profil sportif ATHLETIX généré avec succès !"
                )
            }
        }
    }

    // --- ACTIVE WORKOUT ENGINE ---
    fun startActiveWorkout(program: WorkoutProgramEntity) {
        val allEx = _uiState.value.exercises.associateBy { it.id }
        val exIds = program.exerciseIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val generatedSets = mutableListOf<ActiveSetDraft>()
        for (exId in exIds) {
            val exName = allEx[exId]?.nameFr ?: exId
            for (s in 1..program.defaultSets) {
                generatedSets.add(
                    ActiveSetDraft(
                        exerciseId = exId,
                        exerciseName = exName,
                        setNumber = s,
                        reps = program.defaultReps,
                        weightKg = program.defaultLoadKg,
                        restSeconds = program.defaultRestSec,
                        isCompleted = false
                    )
                )
            }
        }
        _uiState.update {
            it.copy(
                mainTab = MainTab.WORKOUT,
                activeWorkout = ActiveWorkoutState(
                    isActive = true,
                    isPaused = false,
                    programId = program.id,
                    programTitle = program.title,
                    startTimeMs = System.currentTimeMillis(),
                    elapsedSeconds = 0,
                    restRemainingSeconds = 0,
                    sets = generatedSets
                )
            )
        }
        startWorkoutChronometer()
    }

    private fun startWorkoutChronometer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val active = _uiState.value.activeWorkout
                if (active.isActive && !active.isPaused) {
                    _uiState.update {
                        it.copy(activeWorkout = active.copy(elapsedSeconds = active.elapsedSeconds + 1))
                    }
                }
            }
        }
    }

    fun togglePauseActiveWorkout() {
        val active = _uiState.value.activeWorkout
        if (!active.isActive) return
        _uiState.update { it.copy(activeWorkout = active.copy(isPaused = !active.isPaused)) }
    }

    fun updateActiveSetValues(index: Int, reps: Int, weightKg: Double) {
        val active = _uiState.value.activeWorkout
        if (index !in active.sets.indices) return
        val updated = active.sets.toMutableList()
        updated[index] = updated[index].copy(
            reps = reps.coerceIn(1, 200),
            weightKg = weightKg.coerceIn(0.0, 500.0)
        )
        _uiState.update { it.copy(activeWorkout = active.copy(sets = updated)) }
    }

    fun toggleCompleteActiveSet(index: Int) {
        val active = _uiState.value.activeWorkout
        if (index !in active.sets.indices) return
        val updated = active.sets.toMutableList()
        val target = updated[index]
        val nowCompleted = !target.isCompleted
        updated[index] = target.copy(isCompleted = nowCompleted, isSkipped = false)
        _uiState.update { it.copy(activeWorkout = active.copy(sets = updated)) }
        if (nowCompleted) {
            startRestTimer(target.restSeconds)
        }
    }

    fun skipExerciseInActiveWorkout(exerciseId: String) {
        val active = _uiState.value.activeWorkout
        val updated = active.sets.map {
            if (it.exerciseId == exerciseId && !it.isCompleted) {
                it.copy(isSkipped = true)
            } else it
        }
        _uiState.update { it.copy(activeWorkout = active.copy(sets = updated)) }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(activeWorkout = it.activeWorkout.copy(restRemainingSeconds = seconds.coerceAtLeast(0)))
        }
        restTimerJob = viewModelScope.launch {
            while (_uiState.value.activeWorkout.restRemainingSeconds > 0) {
                delay(1000L)
                val currentRest = _uiState.value.activeWorkout.restRemainingSeconds
                if (!_uiState.value.activeWorkout.isPaused && currentRest > 0) {
                    _uiState.update {
                        it.copy(activeWorkout = it.activeWorkout.copy(restRemainingSeconds = currentRest - 1))
                    }
                }
            }
        }
    }

    fun finishActiveWorkout(notes: String) {
        val uid = _uiState.value.currentUser?.id ?: return
        val active = _uiState.value.activeWorkout
        if (!active.isActive) return
        stopWorkoutTimers()

        viewModelScope.launch {
            val entities = active.sets.filter { !it.isSkipped }.map { s ->
                ExerciseSetEntity(
                    sessionId = 0,
                    userId = uid,
                    exerciseId = s.exerciseId,
                    exerciseName = s.exerciseName,
                    setNumber = s.setNumber,
                    reps = s.reps,
                    weightKg = s.weightKg,
                    restSeconds = s.restSeconds,
                    isCompleted = s.isCompleted
                )
            }
            val volume = WorkoutCalculator.calculateSessionVolume(entities)
            repository.saveCompletedWorkoutSession(
                userId = uid,
                programId = active.programId,
                programTitle = active.programTitle,
                startTimeMs = active.startTimeMs,
                endTimeMs = System.currentTimeMillis(),
                durationSeconds = active.elapsedSeconds.coerceAtLeast(60),
                sets = entities,
                notes = notes.ifBlank { active.sessionNotes },
                isOfflineMode = _uiState.value.isOfflineMode
            )
            _uiState.update {
                it.copy(
                    activeWorkout = ActiveWorkoutState(),
                    statusBannerMessage = "Séance enregistrée ! Volume calculé : ${volume.toInt()} kg."
                )
            }
        }
    }

    fun cancelActiveWorkout() {
        stopWorkoutTimers()
        _uiState.update { it.copy(activeWorkout = ActiveWorkoutState()) }
    }

    private fun stopWorkoutTimers() {
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()
    }

    fun createCustomProgram(
        title: String,
        description: String,
        category: String,
        difficulty: String,
        daysPerWeek: Int,
        estimatedDurationMin: Int,
        exerciseIds: List<String>,
        sets: Int,
        reps: Int,
        loadKg: Double,
        restSec: Int,
        notes: String
    ) {
        val uid = _uiState.value.currentUser?.id ?: return
        if (title.isBlank() || exerciseIds.isEmpty()) return
        viewModelScope.launch {
            repository.createCustomProgram(
                userId = uid,
                title = title,
                description = description,
                category = category,
                difficulty = difficulty,
                daysPerWeek = daysPerWeek,
                estimatedDurationMin = estimatedDurationMin,
                exerciseIds = exerciseIds,
                defaultSets = sets,
                defaultReps = reps,
                defaultLoadKg = loadKg,
                defaultRestSec = restSec,
                notes = notes
            )
            _uiState.update { it.copy(statusBannerMessage = "Programme '$title' créé avec succès.") }
        }
    }

    fun deleteCustomProgram(programId: Long) {
        viewModelScope.launch {
            repository.deleteCustomProgram(programId)
        }
    }

    // --- NUTRITION ACTIONS ---
    fun logFoodToMeal(mealType: String, food: FoodItemEntity, portionGrams: Double) {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.logFoodPortion(uid, mealType, food, portionGrams)
            _uiState.update {
                it.copy(statusBannerMessage = "${food.name} (${portionGrams.toInt()}g) ajouté à $mealType.")
            }
        }
    }

    fun addCustomFoodOrRecipe(
        name: String,
        originRegion: String,
        referenceGrams: Double,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        source: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCustomFoodOrRecipe(
                name = name,
                originRegion = originRegion,
                referenceGrams = referenceGrams,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fiberG = fiberG,
                source = source
            )
            _uiState.update { it.copy(statusBannerMessage = "'$name' ajouté à la base alimentaire.") }
        }
    }

    fun deleteMealLog(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMealLog(mealId)
        }
    }

    fun logWater(amountMl: Int) {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.addWaterIntake(uid, amountMl)
        }
    }

    // --- PROGRESS, RECOVERY, HABITS & REMINDERS ---
    fun logSleep(durationHours: Double, qualityScore: Int, fatigueLevel: Int, notes: String) {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.logSleepAndRecovery(uid, durationHours, qualityScore, fatigueLevel, notes)
            _uiState.update { it.copy(statusBannerMessage = "Sommeil et score de récupération enregistrés.") }
        }
    }

    fun logBodyMeasurement(
        weightKg: Double,
        bodyFatPct: Double,
        chestCm: Double,
        waistCm: Double,
        hipsCm: Double,
        armCm: Double,
        thighCm: Double
    ) {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.logBodyMeasurement(uid, weightKg, bodyFatPct, chestCm, waistCm, hipsCm, armCm, thighCm)
            _uiState.update { it.copy(statusBannerMessage = "Mensurations corporelles mises à jour.") }
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleHabitToday(habit)
        }
    }

    fun addHabit(title: String, category: String, targetTime: String) {
        val uid = _uiState.value.currentUser?.id ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(uid, title, category, targetTime)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun toggleReminder(reminder: ReminderSettingEntity, enabled: Boolean, consentGranted: Boolean) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isEnabled = enabled && consentGranted, consentGranted = consentGranted))
        }
    }

    fun syncOfflineQueueNow() {
        val uid = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.syncPendingOfflineQueue(uid)
            _uiState.update {
                it.copy(
                    isOfflineMode = false,
                    statusBannerMessage = "Synchronisation confirmée : toutes les séances hors-ligne sont validées."
                )
            }
        }
    }

    // --- AI FITNESS COACH ---
    fun sendCoachMessage(messageText: String) {
        val state = _uiState.value
        val uid = state.currentUser?.id ?: return
        if (messageText.isBlank()) return
        viewModelScope.launch {
            repository.sendMessageToCoach(
                userId = uid,
                userText = messageText,
                userProfile = state.currentUser,
                recentSessions = state.sessions,
                todayMacros = state.todayMacros,
                todayWaterMl = state.todayWaterMl,
                lastSleepHours = state.lastSleepLog?.durationHours ?: 7.5
            )
        }
    }

    // --- DATA EXPORT (GDPR / PRIVACY) ---
    fun exportUserDataAsJson() {
        val state = _uiState.value
        val user = state.currentUser ?: return
        val root = JSONObject().apply {
            put("platform", "ATHLETIX AI v1.0")
            put("exportedAtIso", state.todayIso)
            put("profile", JSONObject().apply {
                put("firstName", user.firstName)
                put("email", user.email)
                put("age", user.age)
                put("heightCm", user.heightCm)
                put("weightKg", user.weightKg)
                put("goal", user.fitnessGoal)
                put("level", user.fitnessLevel)
                put("targetCalories", user.targetCalories)
                put("targetProteinG", user.targetProteinG)
            })
            put("workoutSessionsCount", state.sessions.size)
            put("totalVolumeKg", state.sessions.sumOf { it.totalVolumeKg })
            put("mealsLoggedCount", state.meals.size)
            put("measurements", JSONArray().apply {
                state.measurements.forEach { m ->
                    put(JSONObject().apply {
                        put("date", m.dateIso)
                        put("weightKg", m.weightKg)
                        put("bodyFatPct", m.bodyFatPct)
                    })
                }
            })
        }
        _uiState.update { it.copy(exportedJsonData = root.toString(2)) }
    }

    fun dismissExportedJsonDialog() {
        _uiState.update { it.copy(exportedJsonData = null) }
    }
}

class AthletixViewModelFactory(
    private val repository: AthletixRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AthletixViewModel(repository, context) as T
    }
}
