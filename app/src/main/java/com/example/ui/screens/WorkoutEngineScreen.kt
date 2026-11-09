package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.ExerciseEntity
import com.example.data.local.WorkoutProgramEntity
import com.example.ui.AthletixUiState
import com.example.ui.components.AthletixCard
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AthletixDanger
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixSecondaryAccent
import com.example.ui.theme.AthletixWarning

@Composable
fun WorkoutEngineScreen(
    state: AthletixUiState,
    onStartProgram: (WorkoutProgramEntity) -> Unit,
    onTogglePauseSession: () -> Unit,
    onUpdateSetValues: (Int, Int, Double) -> Unit,
    onToggleCompleteSet: (Int) -> Unit,
    onSkipExercise: (String) -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onFinishWorkout: (String) -> Unit,
    onCancelWorkout: () -> Unit,
    onCreateCustomProgram: (
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
    ) -> Unit,
    onDeleteCustomProgram: (Long) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(if (state.activeWorkout.isActive) 0 else 1) }
    LaunchedEffect(state.activeWorkout.isActive) {
        if (state.activeWorkout.isActive) {
            selectedSubTab = 0
        }
    }

    var selectedExerciseDetail by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showProgramBuilderDialog by remember { mutableStateOf(false) }

    val subTabs = listOf(
        "Séance Active",
        "Programmes",
        "Exercices (${state.exercises.size})",
        "Outils Force 2.0",
        "Calendrier"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("workout_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> ActiveWorkoutSubScreen(
                state = state,
                onStartDefaultProgram = {
                    state.programs.firstOrNull()?.let { onStartProgram(it) }
                },
                onTogglePause = onTogglePauseSession,
                onUpdateSetValues = onUpdateSetValues,
                onToggleCompleteSet = onToggleCompleteSet,
                onSkipExercise = onSkipExercise,
                onStartRestTimer = onStartRestTimer,
                onFinishWorkout = onFinishWorkout,
                onCancelWorkout = onCancelWorkout
            )
            1 -> WorkoutProgramsSubScreen(
                state = state,
                onStartProgram = onStartProgram,
                onOpenBuilder = { showProgramBuilderDialog = true },
                onDeleteCustom = onDeleteCustomProgram
            )
            2 -> ExerciseLibrarySubScreen(
                exercises = state.exercises,
                onSelectExercise = { selectedExerciseDetail = it }
            )
            3 -> StrengthToolsV2SubScreen()
            4 -> WorkoutCalendarSubScreen(state = state)
        }
    }

    if (selectedExerciseDetail != null) {
        val ex = selectedExerciseDetail!!
        AlertDialog(
            onDismissRequest = { selectedExerciseDetail = null },
            title = { Text(ex.nameFr, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${ex.muscleGroup} • ${ex.equipment} • ${ex.difficulty}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Muscles secondaires : ${ex.secondaryMuscles}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    Text("Instructions :", fontWeight = FontWeight.Bold)
                    Text(ex.instructionsFr, style = MaterialTheme.typography.bodyMedium)
                    Text("Erreurs fréquentes :", fontWeight = FontWeight.Bold, color = AthletixWarning)
                    Text(ex.commonMistakesFr, style = MaterialTheme.typography.bodyMedium)
                    Text("Consignes de sécurité :", fontWeight = FontWeight.Bold, color = AthletixPrimaryAccent)
                    Text(ex.safetyInstructionsFr, style = MaterialTheme.typography.bodyMedium)
                    Text("Variantes :", fontWeight = FontWeight.Bold)
                    Text(ex.variationsFr, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedExerciseDetail = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showProgramBuilderDialog) {
        WorkoutBuilderDialog(
            exercises = state.exercises,
            onDismiss = { showProgramBuilderDialog = false },
            onSave = { title, desc, cat, diff, days, dur, exIds, sets, reps, load, rest, notes ->
                onCreateCustomProgram(title, desc, cat, diff, days, dur, exIds, sets, reps, load, rest, notes)
                showProgramBuilderDialog = false
            }
        )
    }
}

@Composable
private fun ActiveWorkoutSubScreen(
    state: AthletixUiState,
    onStartDefaultProgram: () -> Unit,
    onTogglePause: () -> Unit,
    onUpdateSetValues: (Int, Int, Double) -> Unit,
    onToggleCompleteSet: (Int) -> Unit,
    onSkipExercise: (String) -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onFinishWorkout: (String) -> Unit,
    onCancelWorkout: () -> Unit
) {
    val active = state.activeWorkout
    var sessionNotes by remember { mutableStateOf("") }

    if (!active.isActive) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aucune séance active en cours",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lancez un programme pour activer le chronomètre, le suivi des charges, le minuteur de repos et le calcul déterministe du volume.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onStartDefaultProgram,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("start_quick_workout_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Démarrer une séance maintenant")
            }
        }
        return
    }

    val completedCount = active.sets.count { it.isCompleted }
    val totalCount = active.sets.count { !it.isSkipped }.coerceAtLeast(1)
    val liveVolume = active.sets.filter { it.isCompleted && !it.isSkipped }.sumOf { it.reps * it.weightKg }
    val mins = active.elapsedSeconds / 60
    val secs = active.elapsedSeconds % 60
    val timeFormatted = "%02d:%02d".format(mins, secs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AthletixCard(borderColor = AthletixPrimaryAccent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (active.isPaused) "SÉANCE EN PAUSE" else "SÉANCE EN COURS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active.isPaused) AthletixWarning else AthletixPrimaryAccent
                        )
                        Text(
                            text = active.programTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(color = AthletixPrimaryAccent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("Progression", "$completedCount/$totalCount séries", Modifier.weight(1f))
                    MetricPill("Volume Réel", "${liveVolume.toInt()} kg", Modifier.weight(1f))
                    MetricPill(
                        label = "Repos",
                        value = if (active.restRemainingSeconds > 0) "${active.restRemainingSeconds}s" else "Prêt",
                        accentColor = if (active.restRemainingSeconds > 0) AthletixWarning else AthletixSecondaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (active.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (active.isPaused) "Reprendre" else "Pause")
                    }
                    OutlinedButton(
                        onClick = { onStartRestTimer(active.restRemainingSeconds + 30) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Repos +30s")
                    }
                    if (active.restRemainingSeconds > 0) {
                        TextButton(onClick = { onStartRestTimer(0) }) {
                            Text("Passer repos")
                        }
                    }
                }
            }
        }

        // Group sets by exercise
        val groupedByExercise = active.sets.withIndex().groupBy { it.value.exerciseId }
        groupedByExercise.forEach { (exerciseId, indexedSets) ->
            val firstSet = indexedSets.first().value
            val allSkipped = indexedSets.all { it.value.isSkipped }
            item(key = exerciseId) {
                AthletixCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = firstSet.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (allSkipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                        )
                        if (!allSkipped) {
                            TextButton(onClick = { onSkipExercise(exerciseId) }) {
                                Text("Passer l'exercice", color = AthletixWarning)
                            }
                        } else {
                            Text("Exercice passé", style = MaterialTheme.typography.labelSmall, color = AthletixWarning)
                        }
                    }

                    if (!allSkipped) {
                        Spacer(modifier = Modifier.height(6.dp))
                        indexedSets.forEach { (globalIndex, setItem) ->
                            var repsStr by remember(globalIndex, setItem.reps) { mutableStateOf(setItem.reps.toString()) }
                            var weightStr by remember(globalIndex, setItem.weightKg) { mutableStateOf(setItem.weightKg.toInt().toString()) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "S${setItem.setNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.width(28.dp)
                                )
                                OutlinedTextField(
                                    value = weightStr,
                                    onValueChange = {
                                        weightStr = it
                                        val w = it.toDoubleOrNull() ?: setItem.weightKg
                                        onUpdateSetValues(globalIndex, setItem.reps, w)
                                    },
                                    label = { Text("kg") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = repsStr,
                                    onValueChange = {
                                        repsStr = it
                                        val r = it.toIntOrNull() ?: setItem.reps
                                        onUpdateSetValues(globalIndex, r, setItem.weightKg)
                                    },
                                    label = { Text("Reps") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = { onToggleCompleteSet(globalIndex) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (setItem.isCompleted) AthletixPrimaryAccent else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.testTag("complete_set_button_$globalIndex")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Valider série",
                                        tint = if (setItem.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AthletixCard {
                OutlinedTextField(
                    value = sessionNotes,
                    onValueChange = { sessionNotes = it },
                    label = { Text("Notes de séance (RPE, sensations, ajustements)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelWorkout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AthletixDanger),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Abandonner")
                    }
                    Button(
                        onClick = { onFinishWorkout(sessionNotes) },
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("finish_workout_button")
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terminer & Enregistrer (${liveVolume.toInt()} kg)")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutProgramsSubScreen(
    state: AthletixUiState,
    onStartProgram: (WorkoutProgramEntity) -> Unit,
    onOpenBuilder: () -> Unit,
    onDeleteCustom: (Long) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Tous") }
    val categories = listOf(
        "Tous", "Full Body", "Upper/Lower", "Push/Pull/Legs",
        "Strength", "Hypertrophy", "HIIT", "Cardio", "Home Workout", "Beginner Workout"
    )

    val filtered = remember(state.programs, selectedCategory) {
        if (selectedCategory == "Tous") state.programs
        else state.programs.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Programmes d'Entraînement",
                    subtitle = "Choisissez ou créez votre programme sur mesure"
                )
                Button(
                    onClick = onOpenBuilder,
                    modifier = Modifier.testTag("open_workout_builder_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Builder")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { program ->
            AthletixCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = program.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (program.isCustom) {
                                Text("• Personnalisé", style = MaterialTheme.typography.labelSmall, color = AthletixPrimaryAccent)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = program.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${program.daysPerWeek} j/sem • ${program.estimatedDurationMin} min • ${program.defaultSets} séries x ${program.defaultReps} reps @ ${program.defaultLoadKg.toInt()}kg (Repos ${program.defaultRestSec}s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (program.isCustom) {
                        IconButton(onClick = { onDeleteCustom(program.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AthletixDanger)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onStartProgram(program) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lancer cette séance")
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibrarySubScreen(
    exercises: List<ExerciseEntity>,
    onSelectExercise: (ExerciseEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("Tous") }
    var selectedDifficulty by remember { mutableStateOf("Tous") }

    val muscles = listOf(
        "Tous", "Chest", "Back", "Shoulders", "Arms",
        "Legs", "Glutes", "Abs", "Full Body", "Cardio", "Mobility"
    )
    val difficulties = listOf("Tous", "Beginner", "Intermediate", "Advanced")

    val filtered = remember(exercises, searchQuery, selectedMuscle, selectedDifficulty) {
        exercises.filter { ex ->
            val matchQuery = searchQuery.isBlank() ||
                ex.nameFr.contains(searchQuery, ignoreCase = true) ||
                ex.nameEn.contains(searchQuery, ignoreCase = true) ||
                ex.equipment.contains(searchQuery, ignoreCase = true)
            val matchMuscle = selectedMuscle == "Tous" || ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            val matchDiff = selectedDifficulty == "Tous" || ex.difficulty.equals(selectedDifficulty, ignoreCase = true)
            matchQuery && matchMuscle && matchDiff
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher un exercice ou équipement...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscles) { m ->
                    FilterChip(
                        selected = selectedMuscle == m,
                        onClick = { selectedMuscle = m },
                        label = { Text(m) }
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(difficulties) { d ->
                    FilterChip(
                        selected = selectedDifficulty == d,
                        onClick = { selectedDifficulty = d },
                        label = { Text(d) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { ex ->
            AthletixCard(onClick = { onSelectExercise(ex) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ex.muscleGroup.uppercase()} • ${ex.equipment} • ${ex.difficulty}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = ex.nameFr, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = ex.descriptionFr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Détails")
                }
            }
        }
    }
}

@Composable
private fun StrengthToolsV2SubScreen() {
    var targetWeightStr by remember { mutableStateOf("100") }
    var barWeightStr by remember { mutableStateOf("20") }
    var oneRmInputStr by remember { mutableStateOf("110") }

    val targetW = targetWeightStr.toDoubleOrNull() ?: 100.0
    val barW = barWeightStr.toDoubleOrNull() ?: 20.0
    val platesPerSide = remember(targetW, barW) {
        com.example.domain.WorkoutCalculator.calculatePlatesPerSide(targetW, barW)
    }
    val rpeTable = remember(oneRmInputStr) {
        com.example.domain.WorkoutCalculator.calculateRpeLoadTable(oneRmInputStr.toDoubleOrNull() ?: 110.0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AthletixCard(borderColor = AthletixPrimaryAccent) {
            SectionHeader(
                title = "Calculateur de Disques Olympiques (v2.0)",
                subtitle = "Répartition exacte des disques à charger de CHAQUE côté de la barre"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetWeightStr,
                    onValueChange = { targetWeightStr = it },
                    label = { Text("Charge totale cible (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = barWeightStr,
                    onValueChange = { barWeightStr = it },
                    label = { Text("Poids barre (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (platesPerSide.isEmpty()) {
                Text(
                    text = "Barre à vide uniquement (${barW.toInt()} kg).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Disques par côté : " + platesPerSide.joinToString(" + ") { "${it}kg" },
                    style = MaterialTheme.typography.titleMedium.copy(color = AthletixPrimaryAccent, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(platesPerSide) { plate ->
                        MetricPill(label = "Disque", value = "${plate} kg")
                    }
                }
            }
        }

        AthletixCard {
            SectionHeader(
                title = "Table d'Intensité RPE & Pourcentages 1RM",
                subtitle = "Calculez vos charges de travail selon votre 1RM"
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = oneRmInputStr,
                onValueChange = { oneRmInputStr = it },
                label = { Text("Votre 1RM de référence (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            rpeTable.forEach { (zoneName, pct, recommendedKg) ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zoneName, style = MaterialTheme.typography.titleMedium)
                        Text("Intensité : $pct% du 1RM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MetricPill("Charge", "$recommendedKg kg", AthletixSecondaryAccent)
                }
            }
        }
    }
}

@Composable
private fun WorkoutCalendarSubScreen(state: AthletixUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Calendrier & Historique des Séances",
                subtitle = "${state.sessions.size} séances enregistrées au total"
            )
        }
        items(state.sessions, key = { it.id }) { session ->
            AthletixCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = session.dateIso, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = session.programTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Durée : ${session.durationSeconds / 60} min • Séries : ${session.completedSetsCount}/${session.totalSetsCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.personalNotes.isNotBlank()) {
                            Text(
                                text = "Note : ${session.personalNotes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    MetricPill(
                        label = "Volume",
                        value = "${session.totalVolumeKg.toInt()} kg"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutBuilderDialog(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hypertrophy") }
    var setsStr by remember { mutableStateOf("4") }
    var repsStr by remember { mutableStateOf("10") }
    var loadStr by remember { mutableStateOf("40") }
    var restStr by remember { mutableStateOf("90") }
    var notes by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>("ex_bench_press", "ex_back_squat", "ex_pull_up") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout Builder • Créer un Programme") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nom du programme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objectif / Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = setsStr,
                        onValueChange = { setsStr = it },
                        label = { Text("Séries") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsStr,
                        onValueChange = { repsStr = it },
                        label = { Text("Reps") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = loadStr,
                        onValueChange = { loadStr = it },
                        label = { Text("Kg") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = restStr,
                        onValueChange = { restStr = it },
                        label = { Text("Repos(s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Sélectionnez et ordonnez les exercices (${selectedIds.size}) :", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercises.forEach { ex ->
                        val isSelected = selectedIds.contains(ex.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedIds.remove(ex.id) else selectedIds.add(ex.id)
                            },
                            label = { Text(ex.nameFr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes personnelles") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedIds.isNotEmpty()) {
                        onSave(
                            title,
                            description,
                            category,
                            "Intermediate",
                            4,
                            55,
                            selectedIds.toList(),
                            setsStr.toIntOrNull() ?: 4,
                            repsStr.toIntOrNull() ?: 10,
                            loadStr.toDoubleOrNull() ?: 40.0,
                            restStr.toIntOrNull() ?: 90,
                            notes
                        )
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
