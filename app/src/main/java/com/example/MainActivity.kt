package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AthletixDatabase
import com.example.data.repository.AthletixRepository
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AthletixDatabase.getInstance(applicationContext)
        val repository = AthletixRepository(database.athletixDao())

        setContent {
            val context = LocalContext.current
            val viewModel: AthletixViewModel = viewModel(
                factory = AthletixViewModelFactory(repository, context)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AthletixTheme(darkTheme = state.isDarkTheme) {
                AthletixRootApp(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

private data class NavDestination(
    val tab: MainTab,
    val labelFr: String,
    val labelEn: String,
    val icon: ImageVector,
    val tag: String
)

private val mainDestinations = listOf(
    NavDestination(MainTab.HOME, "Home", "Home", Icons.Default.Dashboard, "nav_tab_home"),
    NavDestination(MainTab.WORKOUT, "Workout", "Workout", Icons.Default.FitnessCenter, "nav_tab_workout"),
    NavDestination(MainTab.NUTRITION, "Nutrition", "Nutrition", Icons.Default.Restaurant, "nav_tab_nutrition"),
    NavDestination(MainTab.PROGRESS, "Progress", "Progress", Icons.Default.Insights, "nav_tab_progress"),
    NavDestination(MainTab.PROFILE, "Profile", "Profile", Icons.Default.Person, "nav_tab_profile")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthletixRootApp(
    state: AthletixUiState,
    viewModel: AthletixViewModel
) {
    if (!state.isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AthletixPrimaryAccent)
        }
        return
    }

    val currentUser = state.currentUser
    if (currentUser == null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                PublicAuthFlow(
                    state = state,
                    onNavigate = viewModel::navigatePublic,
                    onLogin = viewModel::login,
                    onRegister = viewModel::register,
                    onResetPassword = { email, newPass ->
                        viewModel.resetPassword(email, newPass) {}
                    },
                    onStartDemoAthlete = viewModel::startDemoAthleteSession,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onToggleTheme = viewModel::toggleDarkTheme
                )
            }
        }
        return
    }

    if (!currentUser.onboardingCompleted) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                OnboardingFlowScreen(
                    initialFirstName = currentUser.firstName,
                    onCompleteOnboarding = viewModel::submitOnboarding
                )
            }
        }
        return
    }

    if (state.isAiCoachOpen) {
        BackHandler { viewModel.setAiCoachOpen(false) }
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AiCoachBottomSheetOrScreen(
                    state = state,
                    onSendMessage = viewModel::sendCoachMessage,
                    onClose = { viewModel.setAiCoachOpen(false) }
                )
            }
        }
        return
    }

    if (state.mainTab != MainTab.HOME) {
        BackHandler { viewModel.selectMainTab(MainTab.HOME) }
    }

    val isEn = state.languageCode == "EN"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ATHLETIX AI",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${state.mainTab.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { viewModel.setAiCoachOpen(true) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("topbar_ai_coach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Coach",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Coach", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = viewModel::toggleDarkTheme) {
                            Icon(
                                imageVector = if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Thème"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        mainDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = state.mainTab == dest.tab,
                                onClick = { viewModel.selectMainTab(dest.tab) },
                                icon = { Icon(dest.icon, contentDescription = dest.labelFr) },
                                label = { Text(if (isEn) dest.labelEn else dest.labelFr) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        mainDestinations.forEach { dest ->
                            NavigationRailItem(
                                selected = state.mainTab == dest.tab,
                                onClick = { viewModel.selectMainTab(dest.tab) },
                                icon = { Icon(dest.icon, contentDescription = dest.labelFr) },
                                label = { Text(if (isEn) dest.labelEn else dest.labelFr) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (state.statusBannerMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.statusBannerMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearBannerMessage) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            }
                        }
                    }

                    when (state.mainTab) {
                        MainTab.HOME -> DashboardScreen(
                            state = state,
                            onNavigateTab = viewModel::selectMainTab,
                            onOpenAiCoach = { viewModel.setAiCoachOpen(true) },
                            onStartWorkoutProgram = viewModel::startActiveWorkout,
                            onLogWater = viewModel::logWater,
                            onToggleOffline = viewModel::toggleOfflineMode
                        )
                        MainTab.WORKOUT -> WorkoutEngineScreen(
                            state = state,
                            onStartProgram = viewModel::startActiveWorkout,
                            onTogglePauseSession = viewModel::togglePauseActiveWorkout,
                            onUpdateSetValues = viewModel::updateActiveSetValues,
                            onToggleCompleteSet = viewModel::toggleCompleteActiveSet,
                            onSkipExercise = viewModel::skipExerciseInActiveWorkout,
                            onStartRestTimer = viewModel::startRestTimer,
                            onFinishWorkout = viewModel::finishActiveWorkout,
                            onCancelWorkout = viewModel::cancelActiveWorkout,
                            onCreateCustomProgram = viewModel::createCustomProgram,
                            onDeleteCustomProgram = viewModel::deleteCustomProgram
                        )
                        MainTab.NUTRITION -> NutritionEngineScreen(
                            state = state,
                            onLogFoodToMeal = viewModel::logFoodToMeal,
                            onDeleteMealLog = viewModel::deleteMealLog,
                            onAddCustomFood = viewModel::addCustomFoodOrRecipe
                        )
                        MainTab.PROGRESS -> ProgressAndRecoveryScreen(
                            state = state,
                            onLogBodyMeasurement = viewModel::logBodyMeasurement,
                            onLogSleep = viewModel::logSleep,
                            onToggleHabit = viewModel::toggleHabitToday,
                            onAddHabit = viewModel::addHabit,
                            onDeleteHabit = viewModel::deleteHabit
                        )
                        MainTab.PROFILE -> ProfileAndSettingsScreen(
                            state = state,
                            onUpdateProfile = viewModel::submitOnboarding,
                            onToggleReminder = viewModel::toggleReminder,
                            onSyncOfflineQueueNow = viewModel::syncOfflineQueueNow,
                            onToggleOfflineMode = viewModel::toggleOfflineMode,
                            onToggleTheme = viewModel::toggleDarkTheme,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onExportJsonData = viewModel::exportUserDataAsJson,
                            onDismissExportDialog = viewModel::dismissExportedJsonDialog,
                            onSwitchToDemoAthlete = viewModel::startDemoAthleteSession,
                            onLogout = viewModel::logout,
                            onDeleteAccount = viewModel::deleteCurrentAccount
                        )
                    }
                }
            }
        }
    }
}
