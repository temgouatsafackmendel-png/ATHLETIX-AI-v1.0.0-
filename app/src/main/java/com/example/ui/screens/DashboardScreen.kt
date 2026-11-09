package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkoutProgramEntity
import com.example.ui.AthletixUiState
import com.example.ui.MainTab
import com.example.ui.components.*
import com.example.ui.theme.AthletixInfo
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixSecondaryAccent
import com.example.ui.theme.AthletixWarning

@Composable
fun DashboardScreen(
    state: AthletixUiState,
    onNavigateTab: (MainTab) -> Unit,
    onOpenAiCoach: () -> Unit,
    onStartWorkoutProgram: (WorkoutProgramEntity) -> Unit,
    onLogWater: (Int) -> Unit,
    onToggleOffline: () -> Unit
) {
    val user = state.currentUser
    val isEn = state.languageCode == "EN"
    var selectedPeriod by remember { mutableStateOf("7J") } // "7J", "30J", "1A"

    val filteredSessions = remember(state.sessions, selectedPeriod) {
        when (selectedPeriod) {
            "7J" -> state.sessions.take(7)
            "30J" -> state.sessions.take(30)
            else -> state.sessions
        }
    }

    val recommendedProgram = state.programs.firstOrNull {
        it.category.equals(user?.fitnessGoal, ignoreCase = true)
    } ?: state.programs.firstOrNull()

    val pendingOfflineCount = state.syncQueue.count { it.status == "QUEUED_OFFLINE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. WELCOME HEADER & SYNC STATUS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEn) "Welcome back," else "Bonjour,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user?.firstName ?: "Athlète",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (user?.isDemoAccount == true) {
                    Text(
                        text = "MODE PROFIL DÉMONSTRATION ACTIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = AthletixWarning
                    )
                }
            }
            SyncStatusBadge(
                isOffline = state.isOfflineMode,
                pendingCount = pendingOfflineCount,
                onToggleOffline = onToggleOffline
            )
        }

        // 2. DAILY FITNESS SCORE & 3. MAIN FITNESS GOAL
        AthletixCard(borderColor = AthletixPrimaryAccent.copy(alpha = 0.7f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularScoreGauge(
                    score = state.dailyFitnessScore,
                    label = if (isEn) "DAILY SCORE" else "SCORE JOUR"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isEn) "Main Goal: ${user?.fitnessGoal ?: "Muscle Gain"}" else "Objectif : ${user?.fitnessGoal ?: "Muscle Gain"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${user?.weightKg ?: 74.0} kg • ${user?.fitnessLevel ?: "Intermédiaire"} • ${user?.frequencyDaysPerWeek ?: 4} séances/sem",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val weeklyDone = state.sessions.take(7).size
                    val weeklyTarget = (user?.frequencyDaysPerWeek ?: 4).coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { (weeklyDone.toFloat() / weeklyTarget.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "$weeklyDone / $weeklyTarget séances cette semaine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. WEEKLY PROGRESS & PERIOD FILTER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEn) "Performance Overview" else "Progression & Volume",
                style = MaterialTheme.typography.titleLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("7J", "30J", "1A").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val totalVol = filteredSessions.sumOf { it.totalVolumeKg }.toInt()
            MetricPill(
                label = if (isEn) "Training Volume" else "Volume Total",
                value = "$totalVol kg",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = if (isEn) "Sessions" else "Séances",
                value = "${filteredSessions.size}",
                accentColor = AthletixSecondaryAccent,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = if (isEn) "Habits Today" else "Habitudes",
                value = "${state.completedHabitsTodayCount}/${state.habits.size}",
                accentColor = AthletixInfo,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. TODAY'S WORKOUT CARD
        if (recommendedProgram != null) {
            AthletixCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "TODAY'S WORKOUT" else "SÉANCE DU JOUR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recommendedProgram.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${recommendedProgram.estimatedDurationMin} min • ${recommendedProgram.category} • ${recommendedProgram.defaultSets}x${recommendedProgram.defaultReps}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onStartWorkoutProgram(recommendedProgram) },
                        modifier = Modifier.testTag("dashboard_start_workout_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Start" else "Démarrer")
                    }
                }
            }
        }

        // 6. NUTRITION SUMMARY & 7. WATER INTAKE + 8. SLEEP SUMMARY
        AthletixCard(
            onClick = { onNavigateTab(MainTab.NUTRITION) }
        ) {
            SectionHeader(
                title = if (isEn) "Nutrition & Macros Today" else "Résumé Nutritionnel du Jour",
                subtitle = "${state.todayMacros.calories} / ${user?.targetCalories ?: 2500} kcal",
                actionLabel = if (isEn) "Add Meal" else "Journal →",
                onActionClick = { onNavigateTab(MainTab.NUTRITION) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            val calRatio = (state.todayMacros.calories.toFloat() / (user?.targetCalories ?: 2500).coerceAtLeast(1)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { calRatio },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Protéines",
                    value = "${state.todayMacros.proteinG.toInt()} / ${user?.targetProteinG ?: 150}g",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Glucides",
                    value = "${state.todayMacros.carbsG.toInt()} / ${user?.targetCarbsG ?: 280}g",
                    accentColor = AthletixSecondaryAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Lipides",
                    value = "${state.todayMacros.fatG.toInt()} / ${user?.targetFatG ?: 75}g",
                    accentColor = AthletixWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 7. WATER INTAKE
            AthletixCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = AthletixInfo)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Hydration" else "Hydratation",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${state.todayWaterMl} / ${user?.targetWaterMl ?: 2800} ml",
                    style = MaterialTheme.typography.titleLarge.copy(color = AthletixInfo, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onLogWater(250) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).testTag("water_add_250_button")
                    ) {
                        Text("+250ml", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { onLogWater(500) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).testTag("water_add_500_button")
                    ) {
                        Text("+500ml", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 8. SLEEP SUMMARY
            AthletixCard(
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab(MainTab.PROGRESS) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = AthletixPrimaryAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Sleep & Recovery" else "Sommeil & Récup",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val sleepH = state.lastSleepLog?.durationHours ?: 0.0
                val recScore = state.lastSleepLog?.recoveryScore ?: 80
                Text(
                    text = "${sleepH}h (${recScore}%)",
                    style = MaterialTheme.typography.titleLarge.copy(color = AthletixPrimaryAccent, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEn) "Quality: ${state.lastSleepLog?.qualityScore ?: 8}/10" else "Qualité : ${state.lastSleepLog?.qualityScore ?: 8}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // INTERACTIVE CHARTS (Training Volume & Weight Evolution)
        AthletixCard {
            SectionHeader(
                title = if (isEn) "Training Volume & Weight Evolution" else "Évolution du Volume & du Poids",
                subtitle = if (isEn) "Real recorded data ($selectedPeriod)" else "Données réelles mesurées ($selectedPeriod)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            val volumePoints = filteredSessions.reversed().map {
                Pair(it.dateIso, it.totalVolumeKg.toFloat())
            }
            InteractiveLineChart(
                points = volumePoints,
                lineColor = AthletixPrimaryAccent,
                unitSuffix = "kg"
            )
        }

        // 10. AI COACH INSIGHTS
        AthletixCard(
            borderColor = AthletixPrimaryAccent,
            onClick = onOpenAiCoach
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AthletixPrimaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ATHLETIX AI COACH",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn) {
                                "Tap for personalized workout, recovery & Cameroonian/International nutrition analysis."
                            } else {
                                "Analyse des séances, nutrition Camerounaise/Internationale et conseils de récupération."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onOpenAiCoach,
                    modifier = Modifier.testTag("dashboard_open_coach_button")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Ouvrir Coach IA")
                }
            }
        }

        // 9. RECENT ACTIVITIES
        AthletixCard {
            SectionHeader(
                title = if (isEn) "Recent Workout Sessions" else "Activités Sportives Récentes",
                actionLabel = if (isEn) "All" else "Tout voir",
                onActionClick = { onNavigateTab(MainTab.WORKOUT) }
            )
            if (state.sessions.isEmpty()) {
                Text(
                    text = if (isEn) "No workout sessions recorded yet." else "Aucune séance enregistrée pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.sessions.take(4).forEach { s ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s.programTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${s.dateIso} • ${s.durationSeconds / 60} min • ${s.completedSetsCount} séries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${s.totalVolumeKg.toInt()} kg",
                                style = MaterialTheme.typography.titleMedium.copy(color = AthletixPrimaryAccent, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = s.syncStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
