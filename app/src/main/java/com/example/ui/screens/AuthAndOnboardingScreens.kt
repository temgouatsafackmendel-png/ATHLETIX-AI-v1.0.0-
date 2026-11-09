package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.NutritionCalculator
import com.example.ui.AthletixUiState
import com.example.ui.PublicRoute
import com.example.ui.components.AthletixCard
import com.example.ui.components.MetricPill
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicAuthFlow(
    state: AthletixUiState,
    onNavigate: (PublicRoute) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onStartDemoAthlete: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val isEn = state.languageCode == "EN"

    if (state.publicRoute != PublicRoute.WELCOME) {
        BackHandler { onNavigate(PublicRoute.WELCOME) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar with Language & Theme switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.publicRoute != PublicRoute.WELCOME) {
                        IconButton(
                            onClick = { onNavigate(PublicRoute.WELCOME) },
                            modifier = Modifier.testTag("auth_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    }
                    Text(
                        text = "ATHLETIX AI",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleLanguage,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("lang_toggle_button")
                    ) {
                        Text(text = state.languageCode, style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Changer le thème"
                        )
                    }
                }
            }

            if (state.authError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.authError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (state.publicRoute) {
                PublicRoute.WELCOME -> WelcomeContent(
                    isEn = isEn,
                    onLoginClick = { onNavigate(PublicRoute.LOGIN) },
                    onRegisterClick = { onNavigate(PublicRoute.REGISTER) },
                    onDemoClick = onStartDemoAthlete
                )
                PublicRoute.LOGIN -> LoginContent(
                    isEn = isEn,
                    onSubmit = onLogin,
                    onForgotClick = { onNavigate(PublicRoute.FORGOT_PASSWORD) },
                    onRegisterClick = { onNavigate(PublicRoute.REGISTER) },
                    onDemoClick = onStartDemoAthlete
                )
                PublicRoute.REGISTER -> RegisterContent(
                    isEn = isEn,
                    onSubmit = onRegister,
                    onLoginClick = { onNavigate(PublicRoute.LOGIN) }
                )
                PublicRoute.FORGOT_PASSWORD -> ForgotPasswordContent(
                    isEn = isEn,
                    onSubmit = onResetPassword
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    isEn: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_athletix_1794216600675),
            contentDescription = "Athletix AI Training Studio",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE60B0D0F))
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Surface(
                    color = AthletixPrimaryAccent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "TRAIN SMARTER. LIVE STRONGER.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0B0D0F),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEn) "Personal Fitness Intelligence Platform" else "Plateforme Personnelle d'Intelligence Sportive",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    AthletixCard {
        Text(
            text = if (isEn) "Complete Ecosystem" else "Écosystème Complet & Déterministe",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isEn) {
                "• Workout Engine: Exercise library, custom builder & live chronometer.\n• Nutrition Engine: Cameroonian, African & International food database.\n• Progress Analytics, Body Tracking, Sleep & Habits.\n• ATHLETIX AI Coach with data classification."
            } else {
                "• Moteur d'entraînement complet : bibliothèque, créateur de programmes et chronomètre actif.\n• Nutrition précise : base alimentaire Camerounaise, Africaine et Internationale.\n• Analytiques, suivi corporel, récupération et habitudes.\n• Coach IA distinguant données mesurées, déclarées, estimations et recommandations."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Button(
        onClick = onRegisterClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("welcome_register_button"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isEn) "Create My Athlete Account" else "Créer mon compte Athlète",
            style = MaterialTheme.typography.titleMedium
        )
    }

    OutlinedButton(
        onClick = onLoginClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("welcome_login_button"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Default.Login, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (isEn) "Sign In to Existing Account" else "Se connecter à mon compte")
    }

    FilledTonalButton(
        onClick = onDemoClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("welcome_demo_button"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isEn) "Explore Demo Athlete Profile (Pre-loaded Data)" else "Explorer le Profil Athlète Démo (Données pré-chargées)"
        )
    }
}

@Composable
private fun LoginContent(
    isEn: Boolean,
    onSubmit: (String, String) -> Unit,
    onForgotClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AthletixCard {
        Text(
            text = if (isEn) "Sign In" else "Connexion",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (isEn) "Password" else "Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_submit_button")
        ) {
            Text(text = if (isEn) "Sign In" else "Se connecter")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onForgotClick) {
                Text(if (isEn) "Forgot password?" else "Mot de passe oublié ?")
            }
            TextButton(onClick = onRegisterClick) {
                Text(if (isEn) "Create account" else "Créer un compte")
            }
        }
    }

    OutlinedButton(
        onClick = onDemoClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isEn) "Quick Access: Demo Athlete" else "Accès rapide : Compte Démo")
    }
}

@Composable
private fun RegisterContent(
    isEn: Boolean,
    onSubmit: (String, String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AthletixCard {
        Text(
            text = if (isEn) "Create Athlete Account" else "Inscription Athlète",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text(if (isEn) "First Name" else "Prénom") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_firstname_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (isEn) "Password (min. 6 chars)" else "Mot de passe (min. 6 caractères)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(firstName, email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("register_submit_button")
        ) {
            Text(text = if (isEn) "Continue to Onboarding" else "Continuer vers le profil sportif")
        }
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isEn) "Already registered? Sign In" else "Déjà inscrit ? Se connecter")
        }
    }
}

@Composable
private fun ForgotPasswordContent(
    isEn: Boolean,
    onSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AthletixCard {
        Text(
            text = if (isEn) "Password Recovery" else "Récupération du mot de passe",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email du compte") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(if (isEn) "New password" else "Nouveau mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(email, newPassword) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isEn) "Update Password" else "Mettre à jour le mot de passe")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlowScreen(
    initialFirstName: String,
    onCompleteOnboarding: (
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
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var firstName by remember { mutableStateOf(initialFirstName) }
    var ageText by remember { mutableStateOf("27") }
    var heightText by remember { mutableStateOf("176") }
    var weightText by remember { mutableStateOf("74") }

    val goals = listOf(
        "Fat Loss", "Muscle Gain", "Strength", "Endurance",
        "General Fitness", "Mobility", "Maintenance"
    )
    var selectedGoal by remember { mutableStateOf("Muscle Gain") }

    val levels = listOf("Débutant", "Intermédiaire", "Avancé")
    var selectedLevel by remember { mutableStateOf("Intermédiaire") }

    var frequencyDays by remember { mutableIntStateOf(4) }
    var durationMin by remember { mutableIntStateOf(60) }

    val equipmentOptions = listOf("Salle complète", "Haltères", "Poids du corps", "Home Gym", "Élastiques")
    var selectedEquipment by remember { mutableStateOf("Salle complète") }

    val locationOptions = listOf("Salle de sport", "Domicile", "Extérieur")
    var selectedLocation by remember { mutableStateOf("Salle de sport") }

    var dietaryPref by remember { mutableStateOf("Cuisine Camerounaise & Internationale riche en protéines") }
    var dietaryRestr by remember { mutableStateOf("Aucune") }
    var limitations by remember { mutableStateOf("Aucune") }

    val ageVal = ageText.toIntOrNull()?.coerceIn(14, 95) ?: 27
    val heightVal = heightText.toDoubleOrNull()?.coerceIn(120.0, 230.0) ?: 176.0
    val weightVal = weightText.toDoubleOrNull()?.coerceIn(35.0, 220.0) ?: 74.0

    val previewPlan = remember(weightVal, heightVal, ageVal, frequencyDays, selectedGoal) {
        NutritionCalculator.calculateTargetNutrition(
            weightKg = weightVal,
            heightCm = heightVal,
            age = ageVal,
            frequencyDays = frequencyDays,
            goal = selectedGoal
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PROFIL ATHLÉTIQUE • ÉTAPE $step / 4",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(
            progress = { step / 4f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )

        when (step) {
            1 -> AthletixCard {
                Text("1. Identité & Données Biométriques", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Âge") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Taille (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Poids (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            2 -> AthletixCard {
                Text("2. Objectif Principal & Niveau Sportif", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Sélectionnez votre objectif prioritaire :", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { goal ->
                        FilterChip(
                            selected = selectedGoal == goal,
                            onClick = { selectedGoal = goal },
                            label = { Text(goal) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Niveau sportif actuel :", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = lvl },
                            label = { Text(lvl) }
                        )
                    }
                }
            }

            3 -> AthletixCard {
                Text("3. Fréquence, Disponibilité & Équipement", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Fréquence : $frequencyDays séances / semaine", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = frequencyDays.toFloat(),
                    onValueChange = { frequencyDays = it.toInt() },
                    valueRange = 2f..6f,
                    steps = 3
                )
                Text("Disponibilité par séance : $durationMin minutes", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = durationMin.toFloat(),
                    onValueChange = { durationMin = (it.toInt() / 5) * 5 },
                    valueRange = 25f..90f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Équipement disponible :", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    equipmentOptions.forEach { eq ->
                        FilterChip(
                            selected = selectedEquipment == eq,
                            onClick = { selectedEquipment = eq },
                            label = { Text(eq) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Lieu d'entraînement :", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    locationOptions.forEach { loc ->
                        FilterChip(
                            selected = selectedLocation == loc,
                            onClick = { selectedLocation = loc },
                            label = { Text(loc) }
                        )
                    }
                }
            }

            4 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AthletixCard {
                    Text("4. Préférences Alimentaires & Santé", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dietaryPref,
                        onValueChange = { dietaryPref = it },
                        label = { Text("Préférences alimentaires") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dietaryRestr,
                        onValueChange = { dietaryRestr = it },
                        label = { Text("Restrictions / Allergies") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limitations,
                        onValueChange = { limitations = it },
                        label = { Text("Limitations physiques pertinentes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AthletixCard(borderColor = AthletixPrimaryAccent) {
                    Text(
                        text = "PROPOSITION INITIALE ATHLETIX AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Programme recommandé : ${previewPlan.recommendedProgramCategory} ($frequencyDays j/sem • $durationMin min)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill("Cible Kcal", "${previewPlan.targetCalories}", Modifier.weight(1f))
                        MetricPill("Protéines", "${previewPlan.targetProteinG}g", Modifier.weight(1f))
                        MetricPill("Glucides", "${previewPlan.targetCarbsG}g", Modifier.weight(1f))
                        MetricPill("Eau", "${previewPlan.targetWaterMl}ml", Modifier.weight(1f))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Précédent")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (step < 4) {
                        step++
                    } else {
                        onCompleteOnboarding(
                            firstName,
                            ageVal,
                            heightVal,
                            weightVal,
                            selectedGoal,
                            selectedLevel,
                            frequencyDays,
                            durationMin,
                            selectedEquipment,
                            selectedLocation,
                            dietaryPref,
                            dietaryRestr,
                            limitations
                        )
                    }
                },
                modifier = Modifier
                    .height(50.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(if (step < 4) "Continuer" else "Accepter & Lancer ATHLETIX AI")
            }
        }
    }
}
