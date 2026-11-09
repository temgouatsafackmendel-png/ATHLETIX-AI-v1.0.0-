package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.domain.WorkoutCalculator
import com.example.ui.AthletixUiState
import com.example.ui.components.*
import com.example.ui.theme.AthletixDanger
import com.example.ui.theme.AthletixInfo
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixSecondaryAccent
import com.example.ui.theme.AthletixWarning

@Composable
fun ProgressAndRecoveryScreen(
    state: AthletixUiState,
    onLogBodyMeasurement: (Double, Double, Double, Double, Double, Double, Double) -> Unit,
    onLogSleep: (Double, Int, Int, String) -> Unit,
    onToggleHabit: (HabitEntity) -> Unit,
    onAddHabit: (String, String, String) -> Unit,
    onDeleteHabit: (Long) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        "Analytiques & Records",
        "Suivi Corporel",
        "Récupération & Sommeil",
        "Habitudes (${state.completedHabitsTodayCount}/${state.habits.size})"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSubTab == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("progress_subtab_$idx")
                )
            }
        }

        when (selectedSubTab) {
            0 -> AnalyticsAndRecordsSubScreen(state = state)
            1 -> BodyTrackingSubScreen(state = state, onLogMeasurement = onLogBodyMeasurement)
            2 -> RecoveryAndSleepSubScreen(state = state, onLogSleep = onLogSleep)
            3 -> HabitsSubScreen(
                state = state,
                onToggleHabit = onToggleHabit,
                onAddHabit = onAddHabit,
                onDeleteHabit = onDeleteHabit
            )
        }
    }
}

@Composable
private fun AnalyticsAndRecordsSubScreen(state: AthletixUiState) {
    var selectedPeriod by remember { mutableStateOf("30 days") }
    val periods = listOf("7 days", "30 days", "90 days", "6 months", "1 year")

    val maxCount = when (selectedPeriod) {
        "7 days" -> 7
        "30 days" -> 30
        "90 days" -> 90
        "6 months" -> 180
        else -> 365
    }
    val sessions = state.sessions.take(maxCount)
    val measurements = state.measurements.takeLast(maxCount)

    // Compute Personal Records (1RM Epley) per exercise
    val personalRecords = remember(state.allSets) {
        state.allSets
            .filter { it.isCompleted && it.weightKg > 0 }
            .groupBy { it.exerciseName }
            .mapValues { (_, sets) ->
                sets.maxByOrNull { WorkoutCalculator.calculateEstimated1RM(it.weightKg, it.reps) }
            }
            .values
            .filterNotNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(periods) { p ->
                    FilterChip(
                        selected = selectedPeriod == p,
                        onClick = { selectedPeriod = p },
                        label = { Text(p) }
                    )
                }
            }
        }

        item {
            val totalVolume = sessions.sumOf { it.totalVolumeKg }.toInt()
            val avgDurationMin = if (sessions.isNotEmpty()) {
                sessions.sumOf { it.durationSeconds } / 60 / sessions.size
            } else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill("Fréquence", "${sessions.size} séances", Modifier.weight(1f))
                MetricPill("Volume", "$totalVolume kg", AthletixSecondaryAccent, Modifier.weight(1f))
                MetricPill("Durée Moy.", "$avgDurationMin min", AthletixInfo, Modifier.weight(1f))
            }
        }

        item {
            AthletixCard {
                SectionHeader(
                    title = "Évolution du Poids Corporel (kg)",
                    subtitle = "Période : $selectedPeriod"
                )
                val weightPoints = measurements.map { Pair(it.dateIso, it.weightKg.toFloat()) }
                InteractiveLineChart(
                    points = weightPoints,
                    lineColor = AthletixPrimaryAccent,
                    unitSuffix = "kg"
                )
            }
        }

        item {
            AthletixCard {
                SectionHeader(
                    title = "Volume d'Entraînement par Séance (kg)",
                    subtitle = "Calculé à partir des séries réellement complétées"
                )
                val bars = sessions.reversed().takeLast(7).map {
                    Pair(it.dateIso.takeLast(5), it.totalVolumeKg.toFloat())
                }
                InteractiveBarChart(bars = bars, barColor = AthletixSecondaryAccent)
            }
        }

        item {
            AthletixCard {
                SectionHeader(
                    title = "Records Personnels (1RM Estimé Epley)",
                    subtitle = "Meilleures performances enregistrées"
                )
                if (personalRecords.isEmpty()) {
                    Text(
                        text = "Complétez une séance avec charges pour calculer vos records personnels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    personalRecords.forEach { bestSet ->
                        val est1Rm = WorkoutCalculator.calculateEstimated1RM(bestSet.weightKg, bestSet.reps)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bestSet.exerciseName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Série réalisée : ${bestSet.weightKg.toInt()} kg x ${bestSet.reps} reps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MetricPill(label = "1RM Estimé", value = "${est1Rm.toInt()} kg")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyTrackingSubScreen(
    state: AthletixUiState,
    onLogMeasurement: (Double, Double, Double, Double, Double, Double, Double) -> Unit
) {
    val latest = state.measurements.lastOrNull()
    var weightStr by remember(latest) { mutableStateOf((latest?.weightKg ?: 75.0).toString()) }
    var fatStr by remember(latest) { mutableStateOf((latest?.bodyFatPct ?: 16.0).toString()) }
    var chestStr by remember(latest) { mutableStateOf((latest?.chestCm ?: 102.0).toString()) }
    var waistStr by remember(latest) { mutableStateOf((latest?.waistCm ?: 80.0).toString()) }
    var hipsStr by remember(latest) { mutableStateOf((latest?.hipsCm ?: 97.0).toString()) }
    var armStr by remember(latest) { mutableStateOf((latest?.armCm ?: 37.5).toString()) }
    var thighStr by remember(latest) { mutableStateOf((latest?.thighCm ?: 58.0).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AthletixCard {
            SectionHeader(
                title = "Enregistrer Mes Mensurations",
                subtitle = "Suivi précis du poids et des circonférences musculaires"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Poids (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fatStr,
                    onValueChange = { fatStr = it },
                    label = { Text("Masse grasse (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = chestStr,
                    onValueChange = { chestStr = it },
                    label = { Text("Poitrine (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = waistStr,
                    onValueChange = { waistStr = it },
                    label = { Text("Taille (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hipsStr,
                    onValueChange = { hipsStr = it },
                    label = { Text("Hanches (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = armStr,
                    onValueChange = { armStr = it },
                    label = { Text("Bras (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = thighStr,
                    onValueChange = { thighStr = it },
                    label = { Text("Cuisse (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    onLogMeasurement(
                        weightStr.toDoubleOrNull() ?: 75.0,
                        fatStr.toDoubleOrNull() ?: 16.0,
                        chestStr.toDoubleOrNull() ?: 100.0,
                        waistStr.toDoubleOrNull() ?: 80.0,
                        hipsStr.toDoubleOrNull() ?: 96.0,
                        armStr.toDoubleOrNull() ?: 36.0,
                        thighStr.toDoubleOrNull() ?: 56.0
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_measurement_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enregistrer les mensurations")
            }
        }

        AthletixCard {
            SectionHeader(title = "Historique Corporel")
            state.measurements.reversed().forEach { m ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(m.dateIso, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${m.weightKg} kg • ${m.bodyFatPct}% BF")
                    Text("Taille: ${m.waistCm}cm • Bras: ${m.armCm}cm", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RecoveryAndSleepSubScreen(
    state: AthletixUiState,
    onLogSleep: (Double, Int, Int, String) -> Unit
) {
    var hours by remember { mutableFloatStateOf(7.5f) }
    var quality by remember { mutableIntStateOf(8) }
    var fatigue by remember { mutableIntStateOf(3) }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AthletixCard(borderColor = AthletixPrimaryAccent) {
            SectionHeader(
                title = "Centre de Récupération & Sommeil",
                subtitle = "Évaluez votre récupération nerveuse et musculaire"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Durée du sommeil : ${"%.1f".format(hours)} heures", fontWeight = FontWeight.SemiBold)
            Slider(value = hours, onValueChange = { hours = it }, valueRange = 4f..11f)

            Text("Qualité du sommeil : $quality / 10", fontWeight = FontWeight.SemiBold)
            Slider(
                value = quality.toFloat(),
                onValueChange = { quality = it.toInt() },
                valueRange = 1f..10f,
                steps = 8
            )

            Text("Niveau de fatigue ressentie : $fatigue / 10", fontWeight = FontWeight.SemiBold)
            Slider(
                value = fatigue.toFloat(),
                onValueChange = { fatigue = it.toInt() },
                valueRange = 1f..10f,
                steps = 8
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (courbatures, stress, jour de repos)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onLogSleep(hours.toDouble(), quality, fatigue, notes)
                    notes = ""
                },
                modifier = Modifier.fillMaxWidth().testTag("save_sleep_button")
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enregistrer le bilan de récupération")
            }
        }

        AthletixCard {
            SectionHeader(title = "Journaux de Sommeil Récents")
            state.sleepLogs.forEach { log ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${log.dateIso} • ${log.durationHours}h de sommeil", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Qualité ${log.qualityScore}/10 • Fatigue ${log.fatigueLevel}/10",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MetricPill("Récupération", "${log.recoveryScore}%")
                }
            }
        }
    }
}

@Composable
private fun HabitsSubScreen(
    state: AthletixUiState,
    onToggleHabit: (HabitEntity) -> Unit,
    onAddHabit: (String, String, String) -> Unit,
    onDeleteHabit: (Long) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Hydratation") }
    var targetTime by remember { mutableStateOf("08:00") }
    val today = state.todayIso

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AthletixCard {
                Text("Nouvelle Habitude Quotidienne", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Intitulé de l'habitude") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Hydratation", "Nutrition", "Mobilité", "Sommeil").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddHabit(newTitle, selectedCategory, targetTime)
                            newTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_habit_button")
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter l'habitude")
                }
            }
        }

        items(state.habits, key = { it.id }) { habit ->
            val isDoneToday = habit.completedDatesCsv.split(",").contains(today)
            AthletixCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isDoneToday,
                            onCheckedChange = { onToggleHabit(habit) }
                        )
                        Column {
                            Text(habit.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${habit.category} • Série : 🔥 ${habit.streakCount} jours",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDoneToday) AthletixPrimaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteHabit(habit.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = AthletixDanger)
                    }
                }
            }
        }
    }
}
