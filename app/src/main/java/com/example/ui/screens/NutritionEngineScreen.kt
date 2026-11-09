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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FoodItemEntity
import com.example.domain.NutritionCalculator
import com.example.ui.AthletixUiState
import com.example.ui.components.AthletixCard
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AthletixDanger
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixSecondaryAccent
import com.example.ui.theme.AthletixWarning

@Composable
fun NutritionEngineScreen(
    state: AthletixUiState,
    onLogFoodToMeal: (String, FoodItemEntity, Double) -> Unit,
    onDeleteMealLog: (Long) -> Unit,
    onAddCustomFood: (
        name: String,
        originRegion: String,
        referenceGrams: Double,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        source: String
    ) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var foodToLog by remember { mutableStateOf<FoodItemEntity?>(null) }
    var showAddFoodDialog by remember { mutableStateOf(false) }

    val subTabs = listOf(
        "Journal & Repas",
        "Base Cameroun & Monde (${state.foods.size})",
        "Calculateur & Recettes"
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
                    modifier = Modifier.testTag("nutrition_subtab_$idx")
                )
            }
        }

        when (selectedSubTab) {
            0 -> DailyMealJournalSubScreen(
                state = state,
                onSelectFoodToLog = { foodToLog = it },
                onDeleteMealLog = onDeleteMealLog,
                onGoToDatabase = { selectedSubTab = 1 }
            )
            1 -> FoodDatabaseSubScreen(
                foods = state.foods,
                onSelectFoodToLog = { foodToLog = it },
                onOpenCreateFood = { showAddFoodDialog = true }
            )
            2 -> DeterministicMacroCalculatorSubScreen(
                state = state,
                onLogFoodToMeal = onLogFoodToMeal,
                onOpenCreateRecipe = { showAddFoodDialog = true }
            )
        }
    }

    if (foodToLog != null) {
        val food = foodToLog!!
        var selectedMealType by remember { mutableStateOf("Lunch") }
        var gramsStr by remember { mutableStateOf("200") }
        val portionGrams = gramsStr.toDoubleOrNull()?.coerceIn(1.0, 2000.0) ?: 200.0
        val preview = remember(food, portionGrams) {
            NutritionCalculator.calculatePortionMacros(food, portionGrams)
        }

        AlertDialog(
            onDismissRequest = { foodToLog = null },
            title = { Text("Ajouter au repas : ${food.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Origine : ${food.originRegion} • Source : ${food.nutritionalSource}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { mType ->
                            FilterChip(
                                selected = selectedMealType == mType,
                                onClick = { selectedMealType = mType },
                                label = { Text(mType) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = gramsStr,
                        onValueChange = { gramsStr = it },
                        label = { Text("Portion consommée (grammes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Calcul déterministe (${portionGrams.toInt()}g) :",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${preview.calories} kcal • ${preview.proteinG}g Protéines • ${preview.carbsG}g Glucides • ${preview.fatG}g Lipides • ${preview.fiberG}g Fibres",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogFoodToMeal(selectedMealType, food, portionGrams)
                        foodToLog = null
                    },
                    modifier = Modifier.testTag("confirm_log_food_button")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToLog = null }) { Text("Annuler") }
            }
        )
    }

    if (showAddFoodDialog) {
        AddCustomFoodOrRecipeDialog(
            onDismiss = { showAddFoodDialog = false },
            onSave = { name, region, refG, cal, prot, carbs, fat, fiber, src ->
                onAddCustomFood(name, region, refG, cal, prot, carbs, fat, fiber, src)
                showAddFoodDialog = false
            }
        )
    }
}

@Composable
private fun DailyMealJournalSubScreen(
    state: AthletixUiState,
    onSelectFoodToLog: (FoodItemEntity) -> Unit,
    onDeleteMealLog: (Long) -> Unit,
    onGoToDatabase: () -> Unit
) {
    val user = state.currentUser
    val macros = state.todayMacros
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

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
                    Column {
                        Text("BILAN NUTRITIONNEL DU JOUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "${macros.calories} / ${user?.targetCalories ?: 2500} kcal",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Button(onClick = onGoToDatabase) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter Aliment")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (macros.calories.toFloat() / (user?.targetCalories ?: 2500).coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("Protéines", "${macros.proteinG}g", Modifier.weight(1f))
                    MetricPill("Glucides", "${macros.carbsG}g", AthletixSecondaryAccent, Modifier.weight(1f))
                    MetricPill("Lipides", "${macros.fatG}g", AthletixWarning, Modifier.weight(1f))
                    MetricPill("Fibres", "${macros.fiberG}g", Modifier.weight(1f))
                }
            }
        }

        // Quick Cameroonian & Fitness Picks
        item {
            Text("Ajouts Rapides (Cameroun & Fitness) :", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.foods.take(6), key = { it.id }) { food ->
                    SuggestionChip(
                        onClick = { onSelectFoodToLog(food) },
                        label = { Text("${food.name.take(22)} (${food.caloriesPerRef} kcal)") }
                    )
                }
            }
        }

        mealTypes.forEach { mealType ->
            val entries = state.todayMeals.filter { it.mealType.equals(mealType, ignoreCase = true) }
            val mealMacros = NutritionCalculator.calculateTotals(entries)
            item(key = mealType) {
                AthletixCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = mealType, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${mealMacros.calories} kcal (${mealMacros.proteinG.toInt()}g P)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (entries.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aucun aliment enregistré pour $mealType.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        entries.forEach { item ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.foodName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${item.portionGrams.toInt()}g • ${item.calories} kcal • ${item.proteinG}g P / ${item.carbsG}g G / ${item.fatG}g L",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteMealLog(item.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = AthletixDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodDatabaseSubScreen(
    foods: List<FoodItemEntity>,
    onSelectFoodToLog: (FoodItemEntity) -> Unit,
    onOpenCreateFood: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf("Tous") }
    val origins = listOf("Tous", "Cameroun", "Afrique", "International", "Recette")

    val filtered = remember(foods, searchQuery, selectedOrigin) {
        foods.filter { f ->
            val matchQ = searchQuery.isBlank() || f.name.contains(searchQuery, ignoreCase = true)
            val matchO = selectedOrigin == "Tous" || f.originRegion.equals(selectedOrigin, ignoreCase = true)
            matchQ && matchO
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Base Alimentaire Vérifiée",
                    subtitle = "Spécialités du Cameroun, d'Afrique et standards internationaux"
                )
                Button(
                    onClick = onOpenCreateFood,
                    modifier = Modifier.testTag("add_custom_food_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer")
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher (ex: Ndolé, Poulet DG, Koki, Plantain, Riz...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(origins) { orig ->
                    FilterChip(
                        selected = selectedOrigin == orig,
                        onClick = { selectedOrigin = orig },
                        label = { Text(orig) }
                    )
                }
            }
        }
        items(filtered, key = { it.id }) { food ->
            AthletixCard(onClick = { onSelectFoodToLog(food) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${food.originRegion.uppercase()} • Pour ${food.referenceGrams.toInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${food.caloriesPerRef} kcal | P: ${food.proteinPerRef}g • G: ${food.carbsPerRef}g • L: ${food.fatPerRef}g • Fibres: ${food.fiberPerRef}g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Source : ${food.nutritionalSource}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(onClick = { onSelectFoodToLog(food) }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter au repas")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeterministicMacroCalculatorSubScreen(
    state: AthletixUiState,
    onLogFoodToMeal: (String, FoodItemEntity, Double) -> Unit,
    onOpenCreateRecipe: () -> Unit
) {
    val user = state.currentUser
    var weightStr by remember { mutableStateOf((user?.weightKg ?: 75.0).toString()) }
    var heightStr by remember { mutableStateOf((user?.heightCm ?: 176.0).toString()) }
    var ageStr by remember { mutableStateOf((user?.age ?: 27).toString()) }
    var freqDays by remember { mutableIntStateOf(user?.frequencyDaysPerWeek ?: 4) }
    var selectedGoal by remember { mutableStateOf(user?.fitnessGoal ?: "Muscle Gain") }
    var plannerStyle by remember { mutableStateOf("Cameroun & Afrique") }

    val plan = remember(weightStr, heightStr, ageStr, freqDays, selectedGoal) {
        NutritionCalculator.calculateTargetNutrition(
            weightKg = weightStr.toDoubleOrNull() ?: 75.0,
            heightCm = heightStr.toDoubleOrNull() ?: 176.0,
            age = ageStr.toIntOrNull() ?: 27,
            frequencyDays = freqDays,
            goal = selectedGoal
        )
    }

    val plannedMeals = remember(state.foods, plannerStyle) {
        if (plannerStyle == "Cameroun & Afrique") {
            listOf(
                Triple("Breakfast", state.foods.firstOrNull { it.name.contains("Koki") } ?: state.foods.firstOrNull(), 220.0),
                Triple("Lunch", state.foods.firstOrNull { it.name.contains("Poulet DG") } ?: state.foods.firstOrNull(), 350.0),
                Triple("Dinner", state.foods.firstOrNull { it.name.contains("Poisson braisé") } ?: state.foods.firstOrNull(), 280.0),
                Triple("Snack", state.foods.firstOrNull { it.name.contains("Soya") } ?: state.foods.firstOrNull(), 150.0)
            )
        } else {
            listOf(
                Triple("Breakfast", state.foods.firstOrNull { it.name.contains("Flocons") } ?: state.foods.firstOrNull(), 120.0),
                Triple("Lunch", state.foods.firstOrNull { it.name.contains("Blanc de poulet") } ?: state.foods.firstOrNull(), 250.0),
                Triple("Dinner", state.foods.firstOrNull { it.name.contains("Saumon") } ?: state.foods.firstOrNull(), 220.0),
                Triple("Snack", state.foods.firstOrNull { it.name.contains("Whey") } ?: state.foods.firstOrNull(), 40.0)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AthletixCard {
            Text("Calculateur Nutritionnel Déterministe", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Calcul exact basé sur l'équation de Mifflin-St Jeor (aucune valeur inventée).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Poids (kg)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text("Taille (cm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Âge") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Séances / semaine : $freqDays")
            Slider(
                value = freqDays.toFloat(),
                onValueChange = { freqDays = it.toInt() },
                valueRange = 1f..7f,
                steps = 5
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Fat Loss", "Muscle Gain", "Strength", "Endurance", "Maintenance")) { g ->
                    FilterChip(
                        selected = selectedGoal == g,
                        onClick = { selectedGoal = g },
                        label = { Text(g) }
                    )
                }
            }
        }

        AthletixCard(borderColor = AthletixPrimaryAccent) {
            Text("RÉSULTATS DÉTERMINISTES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("Métabolisme (BMR)", "${plan.bmrCalories} kcal", Modifier.weight(1f))
                MetricPill("Dépense (TDEE)", "${plan.tdeeCalories} kcal", Modifier.weight(1f))
                MetricPill("Cible Objectif", "${plan.targetCalories} kcal", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("Protéines", "${plan.targetProteinG} g", Modifier.weight(1f))
                MetricPill("Glucides", "${plan.targetCarbsG} g", AthletixSecondaryAccent, Modifier.weight(1f))
                MetricPill("Lipides", "${plan.targetFatG} g", AthletixWarning, Modifier.weight(1f))
            }
        }

        // MEAL PLANNER V2.0
        AthletixCard {
            SectionHeader(
                title = "Planificateur de Repas (Meal Planner v2.0)",
                subtitle = "Plan journalier prêt à être ajouté en 1 clic à votre journal"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cameroun & Afrique", "International Fitness").forEach { style ->
                    FilterChip(
                        selected = plannerStyle == style,
                        onClick = { plannerStyle = style },
                        label = { Text(style) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            plannedMeals.forEach { (mealType, foodItem, grams) ->
                if (foodItem != null) {
                    val m = NutritionCalculator.calculatePortionMacros(foodItem, grams)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$mealType • ${foodItem.name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${grams.toInt()}g → ${m.calories} kcal (${m.proteinG}g P / ${m.carbsG}g G / ${m.fatG}g L)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = { onLogFoodToMeal(mealType, foodItem, grams) }) {
                            Text("+ Journal")
                        }
                    }
                }
            }
        }

        AthletixCard {
            SectionHeader(
                title = "Créateur de Recettes & Aliments",
                subtitle = "Ajoutez vos propres recettes camerounaises ou internationales avec leurs valeurs vérifiées"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenCreateRecipe, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Créer une Recette ou un Aliment")
            }
        }
    }
}

@Composable
private fun AddCustomFoodOrRecipeDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        originRegion: String,
        referenceGrams: Double,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        source: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var originRegion by remember { mutableStateOf("Cameroun") }
    var caloriesStr by remember { mutableStateOf("180") }
    var proteinStr by remember { mutableStateOf("15.0") }
    var carbsStr by remember { mutableStateOf("18.0") }
    var fatStr by remember { mutableStateOf("6.0") }
    var fiberStr by remember { mutableStateOf("3.0") }
    var source by remember { mutableStateOf("Recette Maison Vérifiée") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel Aliment / Recette (pour 100g)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'aliment ou de la recette") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cameroun", "Afrique", "International", "Recette").forEach { reg ->
                        FilterChip(
                            selected = originRegion == reg,
                            onClick = { originRegion = reg },
                            label = { Text(reg) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = caloriesStr,
                        onValueChange = { caloriesStr = it },
                        label = { Text("Kcal/100g") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = proteinStr,
                        onValueChange = { proteinStr = it },
                        label = { Text("Prot (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = carbsStr,
                        onValueChange = { carbsStr = it },
                        label = { Text("Gluc (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatStr,
                        onValueChange = { fatStr = it },
                        label = { Text("Lip (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiberStr,
                        onValueChange = { fiberStr = it },
                        label = { Text("Fibres (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source nutritionnelle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            originRegion,
                            100.0,
                            caloriesStr.toIntOrNull() ?: 0,
                            proteinStr.toDoubleOrNull() ?: 0.0,
                            carbsStr.toDoubleOrNull() ?: 0.0,
                            fatStr.toDoubleOrNull() ?: 0.0,
                            fiberStr.toDoubleOrNull() ?: 0.0,
                            source
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
