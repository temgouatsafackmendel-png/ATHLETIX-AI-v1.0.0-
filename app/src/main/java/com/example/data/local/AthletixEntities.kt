package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val age: Int = 28,
    val heightCm: Double = 175.0,
    val weightKg: Double = 74.0,
    val fitnessGoal: String = "Muscle Gain", // Fat Loss, Muscle Gain, Strength, Endurance, General Fitness, Mobility, Maintenance
    val fitnessLevel: String = "Intermédiaire", // Débutant, Intermédiaire, Avancé
    val frequencyDaysPerWeek: Int = 4,
    val sessionDurationMin: Int = 60,
    val equipment: String = "Salle complète", // Salle complète, Haltères, Poids du corps, Home Gym, Élastiques
    val trainingLocation: String = "Salle de sport", // Salle de sport, Domicile, Extérieur
    val dietaryPreferences: String = "Omnivore équilibré",
    val dietaryRestrictions: String = "Aucune",
    val physicalLimitations: String = "Aucune",
    val targetCalories: Int = 2550,
    val targetProteinG: Int = 150,
    val targetCarbsG: Int = 280,
    val targetFatG: Int = 75,
    val targetWaterMl: Int = 2800,
    val onboardingCompleted: Boolean = false,
    val isDemoAccount: Boolean = false,
    val emailVerified: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameFr: String,
    val nameEn: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val muscleGroup: String, // Chest, Back, Shoulders, Arms, Legs, Glutes, Abs, Full Body, Cardio, Mobility
    val secondaryMuscles: String,
    val equipment: String, // Barbell, Dumbbells, Machine, Bodyweight, Cable, Kettlebell, None
    val difficulty: String, // Beginner, Intermediate, Advanced
    val exerciseType: String, // Strength, Hypertrophy, Cardio, Mobility, HIIT
    val instructionsFr: String,
    val commonMistakesFr: String,
    val safetyInstructionsFr: String,
    val variationsFr: String
)

@Entity(tableName = "workout_programs")
data class WorkoutProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0, // 0 = global template, >0 = user custom program
    val title: String,
    val description: String,
    val category: String, // Full Body, Upper/Lower, Push/Pull/Legs, Strength, Hypertrophy, Cardio, HIIT, Home Workout, Beginner Workout
    val difficulty: String,
    val durationWeeks: Int = 8,
    val daysPerWeek: Int = 4,
    val estimatedDurationMin: Int = 55,
    val exerciseIdsCsv: String, // comma-separated ExerciseEntity IDs in order
    val defaultSets: Int = 4,
    val defaultReps: Int = 10,
    val defaultLoadKg: Double = 40.0,
    val defaultRestSec: Int = 90,
    val personalNotes: String = "",
    val isCustom: Boolean = false
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val programId: Long,
    val programTitle: String,
    val dateIso: String, // YYYY-MM-DD
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val completedSetsCount: Int,
    val totalSetsCount: Int,
    val personalNotes: String = "",
    val syncStatus: String = "SYNCED" // PENDING_OFFLINE, SYNCED
)

@Entity(tableName = "exercise_sets")
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val userId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val restSeconds: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "foods")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originRegion: String, // Cameroun, Afrique, International, Recette
    val referenceGrams: Double = 100.0,
    val caloriesPerRef: Int,
    val proteinPerRef: Double,
    val carbsPerRef: Double,
    val fatPerRef: Double,
    val fiberPerRef: Double,
    val nutritionalSource: String, // ex: Table FAO / Cameroun, USDA, Recette Personnelle
    val isCustom: Boolean = false
)

@Entity(tableName = "meals")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val dateIso: String, // YYYY-MM-DD
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val foodId: Long,
    val foodName: String,
    val portionGrams: Double,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val isRecipe: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val dateIso: String,
    val amountMl: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val dateIso: String,
    val durationHours: Double,
    val qualityScore: Int, // 1..10
    val fatigueLevel: Int, // 1..10
    val recoveryScore: Int, // 0..100
    val notes: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val dateIso: String,
    val weightKg: Double,
    val bodyFatPct: Double,
    val chestCm: Double,
    val waistCm: Double,
    val hipsCm: Double,
    val armCm: Double,
    val thighCm: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val category: String, // Hydratation, Mobilité, Sommeil, Nutrition, Mental
    val targetTime: String = "08:00",
    val streakCount: Int = 0,
    val completedDatesCsv: String = "" // comma-separated YYYY-MM-DD
)

@Entity(tableName = "notifications")
data class ReminderSettingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val reminderType: String, // WORKOUT, WATER, MEAL, SLEEP, GOAL, WEEKLY_REPORT
    val title: String,
    val scheduledTime: String,
    val isEnabled: Boolean,
    val consentGranted: Boolean
)

@Entity(tableName = "ai_conversations")
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val dataClassification: String, // "MESURÉ + DÉCLARÉ + ESTIMATION + RECOMMANDATION"
    val promptVersion: String = "ATHLETIX-SYS-v1.0",
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val entityType: String,
    val operation: String,
    val summary: String,
    val status: String, // QUEUED_OFFLINE, CONFIRMED_LOCAL
    val timestampMs: Long = System.currentTimeMillis()
)
