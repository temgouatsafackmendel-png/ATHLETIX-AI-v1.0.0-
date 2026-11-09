package com.example.domain

import com.example.data.local.ExerciseSetEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.MealLogEntity
import java.security.MessageDigest
import kotlin.math.roundToInt

data class MacroNutrients(
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double
)

data class TargetNutritionPlan(
    val bmrCalories: Int,
    val tdeeCalories: Int,
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val targetWaterMl: Int,
    val recommendedProgramCategory: String
)

object NutritionCalculator {

    /**
     * Calcul déterministe des macronutriments pour une portion donnée en grammes.
     * Ne jamais inventer de valeurs inconnues : utilise uniquement le ratio exact par rapport à referenceGrams.
     */
    fun calculatePortionMacros(food: FoodItemEntity, portionGrams: Double): MacroNutrients {
        if (portionGrams <= 0.0 || food.referenceGrams <= 0.0) {
            return MacroNutrients(0, 0.0, 0.0, 0.0, 0.0)
        }
        val ratio = portionGrams / food.referenceGrams
        val calories = (food.caloriesPerRef * ratio).roundToInt()
        val protein = round1Decimal(food.proteinPerRef * ratio)
        val carbs = round1Decimal(food.carbsPerRef * ratio)
        val fat = round1Decimal(food.fatPerRef * ratio)
        val fiber = round1Decimal(food.fiberPerRef * ratio)
        return MacroNutrients(
            calories = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            fiberG = fiber
        )
    }

    /**
     * Additionne de façon déterministe toutes les entrées alimentaires d'une journée ou d'un repas.
     */
    fun calculateTotals(meals: List<MealLogEntity>): MacroNutrients {
        var totalCal = 0
        var totalProt = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        for (m in meals) {
            totalCal += m.calories
            totalProt += m.proteinG
            totalCarbs += m.carbsG
            totalFat += m.fatG
            totalFiber += m.fiberG
        }
        return MacroNutrients(
            calories = totalCal,
            proteinG = round1Decimal(totalProt),
            carbsG = round1Decimal(totalCarbs),
            fatG = round1Decimal(totalFat),
            fiberG = round1Decimal(totalFiber)
        )
    }

    /**
     * Formule déterministe de Mifflin-St Jeor + multiplicateur d'activité.
     */
    fun calculateTargetNutrition(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        frequencyDays: Int,
        goal: String
    ): TargetNutritionPlan {
        val safeWeight = weightKg.coerceIn(35.0, 250.0)
        val safeHeight = heightCm.coerceIn(120.0, 230.0)
        val safeAge = age.coerceIn(14, 100)

        val bmr = (10.0 * safeWeight + 6.25 * safeHeight - 5.0 * safeAge + 5.0).roundToInt()
        val activityMultiplier = when {
            frequencyDays <= 1 -> 1.25
            frequencyDays in 2..3 -> 1.42
            frequencyDays in 4..5 -> 1.58
            else -> 1.75
        }
        val tdee = (bmr * activityMultiplier).roundToInt()

        val targetCal = when (goal) {
            "Fat Loss" -> (tdee - 450).coerceAtLeast(1300)
            "Muscle Gain" -> tdee + 350
            "Strength" -> tdee + 250
            "Endurance" -> tdee + 200
            else -> tdee
        }

        val proteinPerKg = when (goal) {
            "Muscle Gain", "Strength", "Fat Loss" -> 2.0
            "Endurance" -> 1.7
            else -> 1.6
        }
        val proteinG = (safeWeight * proteinPerKg).roundToInt()
        val fatG = ((targetCal * 0.27) / 9.0).roundToInt()
        val remainingCal = (targetCal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(200)
        val carbsG = (remainingCal / 4.0).roundToInt()
        val waterMl = (safeWeight * 35.0 + (frequencyDays * 120)).roundToInt().coerceIn(2000, 4500)

        val recommendedProgram = when (goal) {
            "Fat Loss" -> "HIIT"
            "Muscle Gain" -> "Hypertrophy"
            "Strength" -> "Strength"
            "Endurance" -> "Cardio"
            "Mobility" -> "Full Body"
            else -> if (frequencyDays >= 4) "Upper/Lower" else "Full Body"
        }

        return TargetNutritionPlan(
            bmrCalories = bmr,
            tdeeCalories = tdee,
            targetCalories = targetCal,
            targetProteinG = proteinG,
            targetCarbsG = carbsG,
            targetFatG = fatG,
            targetWaterMl = waterMl,
            recommendedProgramCategory = recommendedProgram
        )
    }

    private fun round1Decimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }
}

object WorkoutCalculator {

    /**
     * Calcule le volume total d'entraînement (kg) uniquement à partir des séries réellement complétées.
     */
    fun calculateSessionVolume(sets: List<ExerciseSetEntity>): Double {
        var volume = 0.0
        for (set in sets) {
            if (set.isCompleted && set.reps > 0 && set.weightKg >= 0.0) {
                // Pour les exercices au poids du corps (0 kg), compte un volume minimal nominal ou 0 kg de charge externe
                volume += set.reps * set.weightKg
            }
        }
        return (volume * 10.0).roundToInt() / 10.0
    }

    /**
     * Estime le 1RM via la formule d'Epley : Charge * (1 + Répétitions / 30).
     */
    fun calculateEstimated1RM(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        val epley = weightKg * (1.0 + reps / 30.0)
        return (epley * 10.0).roundToInt() / 10.0
    }

    /**
     * Calcule le Daily Fitness Score (0 à 100) déterministe.
     */
    fun calculateDailyFitnessScore(
        workoutsThisWeek: Int,
        targetWorkoutsPerWeek: Int,
        todayCalories: Int,
        targetCalories: Int,
        todayWaterMl: Int,
        targetWaterMl: Int,
        lastSleepHours: Double,
        completedHabitsToday: Int,
        totalHabits: Int
    ): Int {
        val workoutRatio = if (targetWorkoutsPerWeek > 0) {
            (workoutsThisWeek.toDouble() / targetWorkoutsPerWeek.toDouble()).coerceIn(0.0, 1.0)
        } else 0.7
        val workoutScore = (workoutRatio * 35.0).roundToInt()

        val nutritionScore = if (targetCalories > 0 && todayCalories > 0) {
            val diffRatio = kotlin.math.abs(todayCalories - targetCalories).toDouble() / targetCalories.toDouble()
            ((1.0 - diffRatio.coerceIn(0.0, 1.0)) * 25.0).roundToInt()
        } else 8

        val waterRatio = if (targetWaterMl > 0) {
            (todayWaterMl.toDouble() / targetWaterMl.toDouble()).coerceIn(0.0, 1.0)
        } else 0.5
        val waterScore = (waterRatio * 15.0).roundToInt()

        val sleepRatio = (lastSleepHours / 8.0).coerceIn(0.0, 1.0)
        val sleepScore = (sleepRatio * 15.0).roundToInt()

        val habitRatio = if (totalHabits > 0) {
            (completedHabitsToday.toDouble() / totalHabits.toDouble()).coerceIn(0.0, 1.0)
        } else 0.5
        val habitScore = (habitRatio * 10.0).roundToInt()

        return (workoutScore + nutritionScore + waterScore + sleepScore + habitScore).coerceIn(0, 100)
    }

    /**
     * Nouveauté v2.0 : Calcule de façon déterministe les disques olympiques à charger de CHAQUE côté de la barre.
     */
    fun calculatePlatesPerSide(targetWeightKg: Double, barWeightKg: Double = 20.0): List<Double> {
        if (targetWeightKg <= barWeightKg) return emptyList()
        var remainingPerSide = (targetWeightKg - barWeightKg) / 2.0
        val availablePlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        val result = mutableListOf<Double>()
        for (plate in availablePlates) {
            while (remainingPerSide + 0.001 >= plate) {
                result.add(plate)
                remainingPerSide -= plate
            }
        }
        return result
    }

    /**
     * Nouveauté v2.0 : Table de charges recommandées selon le 1RM et le RPE cible.
     */
    fun calculateRpeLoadTable(oneRmKg: Double): List<Triple<String, Int, Double>> {
        if (oneRmKg <= 0.0) return emptyList()
        return listOf(
            Triple("Force Max (3 reps @ RPE 9)", 90, ((oneRmKg * 0.90) * 10.0).roundToInt() / 10.0),
            Triple("Force / Masse (5 reps @ RPE 8.5)", 85, ((oneRmKg * 0.85) * 10.0).roundToInt() / 10.0),
            Triple("Hypertrophie Lourde (8 reps @ RPE 8)", 78, ((oneRmKg * 0.78) * 10.0).roundToInt() / 10.0),
            Triple("Hypertrophie Volume (10 reps @ RPE 8)", 73, ((oneRmKg * 0.73) * 10.0).roundToInt() / 10.0),
            Triple("Endurance Musculaire (15 reps @ RPE 7.5)", 65, ((oneRmKg * 0.65) * 10.0).roundToInt() / 10.0)
        )
    }
}

object SecurityUtils {
    fun hashPassword(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawPassword.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.contains("@") && trimmed.contains(".") && trimmed.length >= 5
    }

    /**
     * Protection contre les prompt injections : filtre et neutralise les instructions de contournement système.
     */
    fun sanitizeCoachPrompt(rawInput: String): Pair<String, Boolean> {
        val trimmed = rawInput.trim().take(800)
        val lower = trimmed.lowercase()
        val suspiciousPatterns = listOf(
            "ignore previous instructions",
            "ignore all instructions",
            "oublie les instructions",
            "ignore toutes les instructions",
            "system prompt",
            "jailbreak",
            "act as a doctor",
            "diagnostic médical"
        )
        val flagged = suspiciousPatterns.any { lower.contains(it) }
        return Pair(trimmed, flagged)
    }
}
