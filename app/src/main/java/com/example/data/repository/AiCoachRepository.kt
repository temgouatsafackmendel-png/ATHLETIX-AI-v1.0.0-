package com.example.data.repository

import com.example.data.local.UserEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.domain.MacroNutrients
import com.example.domain.SecurityUtils

data class CoachResponsePayload(
    val text: String,
    val classificationBadge: String,
    val promptVersion: String,
    val remainingQuota: Int,
    val isFlaggedInjection: Boolean = false
)

/**
 * Interface unique pour toutes les requêtes IA d'ATHLETIX AI COACH.
 */
interface AiCoachRepository {
    val systemPromptVersion: String
    val systemPromptTemplate: String
    suspend fun askCoach(
        userMessage: String,
        userProfile: UserEntity?,
        recentSessions: List<WorkoutSessionEntity>,
        todayMacros: MacroNutrients,
        todayWaterMl: Int,
        lastSleepHours: Double
    ): CoachResponsePayload
}

/**
 * Implémentation locale d'exemples déterministes utilisée lorsque la configuration Firebase AI Logic
 * a été déclinée par l'utilisateur.
 * Distingue explicitement : Données mesurées, Données déclarées, Estimations et Recommandations.
 * Ne fournit jamais de diagnostic médical ni de pratique sportive dangereuse.
 */
class PlaceholderAiCoachRepository : AiCoachRepository {

    override val systemPromptVersion: String = "ATHLETIX-COACH-SYS-v1.0"

    override val systemPromptTemplate: String = """
        [SYSTEM PROMPT VERSION: ATHLETIX-COACH-SYS-v1.0]
        Rôle : Tu es ATHLETIX AI COACH, assistant personnel d'intelligence sportive.
        Règles strictes :
        1. Toujours distinguer : [DONNÉES MESURÉES], [DONNÉES DÉCLARÉES], [ESTIMATIONS], [RECOMMANDATIONS].
        2. Ne jamais poser de diagnostic médical ni prescrire de traitement médical.
        3. Ne jamais encourager la douleur aiguë, le surentraînement ou la déshydratation.
        4. Adapter chaque conseil au profil réel, aux objectifs, aux équipements et aux habitudes alimentaires (incluant la gastronomie camerounaise et internationale).
    """.trimIndent()

    private var requestsUsedInSession: Int = 0
    private val maxQuotaPerSession: Int = 30

    override suspend fun askCoach(
        userMessage: String,
        userProfile: UserEntity?,
        recentSessions: List<WorkoutSessionEntity>,
        todayMacros: MacroNutrients,
        todayWaterMl: Int,
        lastSleepHours: Double
    ): CoachResponsePayload {
        if (requestsUsedInSession >= maxQuotaPerSession) {
            return CoachResponsePayload(
                text = "Quota de requêtes atteint ($maxQuotaPerSession/$maxQuotaPerSession pour cette session). Veuillez patienter avant d'envoyer une nouvelle demande.",
                classificationBadge = "QUOTA LIMITÉ",
                promptVersion = systemPromptVersion,
                remainingQuota = 0
            )
        }

        val (sanitized, flagged) = SecurityUtils.sanitizeCoachPrompt(userMessage)
        if (flagged) {
            return CoachResponsePayload(
                text = "Alerte Sécurité & Éthique : Votre message contient une instruction système non autorisée ou une demande de diagnostic médical. ATHLETIX AI COACH fournit uniquement un accompagnement sportif et nutritionnel éducatif.",
                classificationBadge = "PROTECTION PROMPT INJECTION",
                promptVersion = systemPromptVersion,
                remainingQuota = maxQuotaPerSession - requestsUsedInSession,
                isFlaggedInjection = true
            )
        }

        requestsUsedInSession++
        val remaining = (maxQuotaPerSession - requestsUsedInSession).coerceAtLeast(0)

        val name = userProfile?.firstName?.ifBlank { "Athlète" } ?: "Athlète"
        val goal = userProfile?.fitnessGoal ?: "General Fitness"
        val weight = userProfile?.weightKg ?: 74.0
        val targetCal = userProfile?.targetCalories ?: 2500
        val targetProt = userProfile?.targetProteinG ?: 150
        val totalSessions = recentSessions.size
        val lastVolume = recentSessions.firstOrNull()?.totalVolumeKg ?: 0.0
        val lower = sanitized.lowercase()

        val focusSection = when {
            lower.contains("programme") || lower.contains("séance") || lower.contains("workout") -> {
                "Proposition de structure hebdomadaire adaptée à votre objectif ($goal) sur ${userProfile?.frequencyDaysPerWeek ?: 4} jours/semaine (${userProfile?.equipment ?: "Salle complète"}) :\n" +
                    "• Jour 1 : Polyarticulaire Haut du corps (Développé couché 4x8, Rowing barre 4x10)\n" +
                    "• Jour 2 : Bas du corps & Gainage (Squat 4x8, Hip Thrust 4x10, Planche 3x60s)\n" +
                    "• Jour 3 : Récupération active & Mobilité thoracique/hanches (25 min)\n" +
                    "• Jour 4 : Hypertrophie / Métabolique (Développé incliné, Tractions, Thrusters)"
            }
            lower.contains("nutrition") || lower.contains("repas") || lower.contains("cameroun") || lower.contains("manger") || lower.contains("protéine") -> {
                "Optimisation nutritionnelle (Base Cameroun & Internationale) :\n" +
                    "• Au déjeuner : Associez 180g de Poisson braisé de Douala ou Blanc de poulet grillé avec 150g de Plantain mûr bouilli ou Riz Basmati et une portion de Ndolé peu gras.\n" +
                    "• Au dîner : 150g de Koki de niébé ou Haricots rouges avec légumes verts pour un apport riche en fibres et protéines végétales/animales.\n" +
                    "• Il vous reste ${(targetProt - todayMacros.proteinG.toInt()).coerceAtLeast(0)}g de protéines à consommer aujourd'hui pour atteindre votre cible."
            }
            lower.contains("récupération") || lower.contains("sommeil") || lower.contains("fatigue") || lower.contains("courbature") -> {
                "Protocole de récupération sportive :\n" +
                    "• Sommeil : Visez 7h30 à 8h30 de sommeil dans une pièce fraîche.\n" +
                    "• Hydratation : Complétez votre apport jusqu'à ${userProfile?.targetWaterMl ?: 2800} ml d'eau répartis sur la journée.\n" +
                    "• Mobilité : Effectuez 10 minutes de Flow Mobilité Hanche 90/90 et ouverture thoracique sans forcer dans la douleur."
            }
            else -> {
                "Bilan global personnalisé pour $name :\n" +
                    "• Maintenez une surcharge progressive contrôlée (+2,5 kg ou +1 répétition lorsque toutes vos séries sont validées avec une technique irréprochable).\n" +
                    "• Répartissez vos ${targetProt}g de protéines sur 3 à 4 repas quotidiens."
            }
        }

        val responseText = buildString {
            appendLine("⚠️ [MODE EXEMPLE LOCAL — Activez Firebase pour connecter Gemini en direct via Firebase AI Logic]")
            appendLine()
            appendLine("📊 1. DONNÉES MESURÉES (Séances & Journaux enregistrés) :")
            appendLine("• Séances complétées : $totalSessions séance(s) | Dernier volume mesuré : ${lastVolume.toInt()} kg")
            appendLine("• Apport journalier enregistré : ${todayMacros.calories} kcal (${todayMacros.proteinG}g P / ${todayMacros.carbsG}g G / ${todayMacros.fatG}g L)")
            appendLine("• Hydratation mesurée : $todayWaterMl ml | Dernier sommeil : ${lastSleepHours}h")
            appendLine()
            appendLine("📝 2. DONNÉES DÉCLARÉES (Profil $name) :")
            appendLine("• Poids déclaré : $weight kg | Objectif : $goal | Niveau : ${userProfile?.fitnessLevel ?: "Intermédiaire"}")
            appendLine("• Limitations déclarées : ${userProfile?.physicalLimitations ?: "Aucune"}")
            appendLine()
            appendLine("📐 3. ESTIMATIONS DÉTERMINISTES :")
            appendLine("• Cible énergétique estimée (Mifflin-St Jeor) : $targetCal kcal/jour | Protéines : ${targetProt}g/jour")
            appendLine()
            appendLine("🎯 4. RECOMMANDATIONS SPORTIVES (Non médicales) :")
            appendLine(focusSection)
            appendLine()
            append("Note de sécurité : Ces conseils sont purement sportifs et éducatifs et ne constituent jamais un diagnostic médical.")
        }

        return CoachResponsePayload(
            text = responseText,
            classificationBadge = "MESURÉ • DÉCLARÉ • ESTIMATION • RECOMMANDATION",
            promptVersion = systemPromptVersion,
            remainingQuota = remaining
        )
    }
}
