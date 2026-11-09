package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderSettingEntity
import com.example.ui.AthletixUiState
import com.example.ui.components.AthletixCard
import com.example.ui.components.FirebaseDeclinedNoticeBanner
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AthletixDanger
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCoachBottomSheetOrScreen(
    state: AthletixUiState,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showPromptVersionDialog by remember { mutableStateOf(false) }

    val quickPrompts = listOf(
        "Génère un programme adapté à mon objectif",
        "Analyse mes dernières séances et mon volume",
        "Idée de repas sportif avec plats du Cameroun",
        "Conseils de récupération et sommeil"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AthletixPrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ATHLETIX AI COACH",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Prompt Système : ATHLETIX-COACH-SYS-v1.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = { showPromptVersionDialog = true }) {
                        Icon(Icons.Default.Code, contentDescription = "Voir System Prompt")
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_ai_coach_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Mandatory in-app notice explaining that AI features require Firebase
            FirebaseDeclinedNoticeBanner()

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { prompt ->
                    SuggestionChip(
                        onClick = { onSendMessage(prompt) },
                        label = { Text(prompt) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.aiMessages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    AthletixCard(
                        borderColor = if (isUser) MaterialTheme.colorScheme.outline else AthletixPrimaryAccent.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isUser) "VOUS" else "ATHLETIX AI COACH",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else AthletixPrimaryAccent
                            )
                            Text(
                                text = msg.dataClassification,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Posez votre question sportive ou nutritionnelle...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_coach_input")
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("ai_coach_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
                }
            }
        }
    }

    if (showPromptVersionDialog) {
        AlertDialog(
            onDismissRequest = { showPromptVersionDialog = false },
            title = { Text("Prompt Système Versionné (v1.0)") },
            text = {
                Text(
                    "Version : ATHLETIX-COACH-SYS-v1.0\n\n" +
                        "• Séparation stricte : Données mesurées / Données déclarées / Estimations / Recommandations.\n" +
                        "• Protection anti-injection active (filtrage des commandes système).\n" +
                        "• Quota : 30 requêtes max par session.\n" +
                        "• Éthique : Aucun diagnostic médical ni pratique dangereuse."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPromptVersionDialog = false }) { Text("Fermer") }
            }
        )
    }
}

@Composable
fun ProfileAndSettingsScreen(
    state: AthletixUiState,
    onUpdateProfile: (
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
    ) -> Unit,
    onToggleReminder: (ReminderSettingEntity, Boolean, Boolean) -> Unit,
    onSyncOfflineQueueNow: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onExportJsonData: () -> Unit,
    onDismissExportDialog: () -> Unit,
    onSwitchToDemoAthlete: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val user = state.currentUser
    var firstName by remember(user) { mutableStateOf(user?.firstName ?: "") }
    var ageStr by remember(user) { mutableStateOf((user?.age ?: 27).toString()) }
    var heightStr by remember(user) { mutableStateOf((user?.heightCm ?: 176.0).toString()) }
    var weightStr by remember(user) { mutableStateOf((user?.weightKg ?: 74.0).toString()) }
    var selectedGoal by remember(user) { mutableStateOf(user?.fitnessGoal ?: "Muscle Gain") }
    var selectedLevel by remember(user) { mutableStateOf(user?.fitnessLevel ?: "Intermédiaire") }
    var freqDays by remember(user) { mutableIntStateOf(user?.frequencyDaysPerWeek ?: 4) }
    var dietPref by remember(user) { mutableStateOf(user?.dietaryPreferences ?: "") }
    var limitations by remember(user) { mutableStateOf(user?.physicalLimitations ?: "Aucune") }
    var confirmDeleteDialog by remember { mutableStateOf(false) }

    var notificationConsentGranted by remember { mutableStateOf(true) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationConsentGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. PROFILE EDITOR
        AthletixCard {
            SectionHeader(
                title = "Profil Sportif & Objectifs",
                subtitle = "Modifiez vos paramètres biométriques pour recalculer vos cibles"
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Prénom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Âge") },
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
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Poids (kg)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Fat Loss", "Muscle Gain", "Strength", "Endurance", "General Fitness", "Mobility", "Maintenance")) { g ->
                    FilterChip(
                        selected = selectedGoal == g,
                        onClick = { selectedGoal = g },
                        label = { Text(g) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dietPref,
                onValueChange = { dietPref = it },
                label = { Text("Préférences alimentaires") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = limitations,
                onValueChange = { limitations = it },
                label = { Text("Limitations physiques") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onUpdateProfile(
                        firstName,
                        ageStr.toIntOrNull() ?: 27,
                        heightStr.toDoubleOrNull() ?: 176.0,
                        weightStr.toDoubleOrNull() ?: 74.0,
                        selectedGoal,
                        selectedLevel,
                        freqDays,
                        user?.sessionDurationMin ?: 60,
                        user?.equipment ?: "Salle complète",
                        user?.trainingLocation ?: "Salle de sport",
                        dietPref,
                        user?.dietaryRestrictions ?: "Aucune",
                        limitations
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mettre à jour mon profil & mes cibles")
            }
        }

        // 2. CONFIGURABLE NOTIFICATIONS & CONSENT
        AthletixCard {
            SectionHeader(
                title = "Notifications & Rappels Configurables",
                subtitle = "Aucune notification n'est envoyée sans votre consentement explicite"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Consentement global aux notifications", fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = notificationConsentGranted,
                    onCheckedChange = { wantGrant ->
                        if (wantGrant && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationConsentGranted = wantGrant
                        }
                    }
                )
            }
            state.reminders.forEach { reminder ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Horaire programmé : ${reminder.scheduledTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminder.isEnabled && notificationConsentGranted,
                        onCheckedChange = { enabled ->
                            onToggleReminder(reminder, enabled, notificationConsentGranted)
                        }
                    )
                }
            }
        }

        // 3. OFFLINE & SYNCHRONIZATION QUEUE
        AthletixCard {
            SectionHeader(
                title = "Mode Hors-Ligne & File de Synchronisation",
                subtitle = "Ne déclare jamais une donnée synchronisée avant validation effective"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Simuler saisie hors-ligne (File d'attente)")
                Switch(
                    checked = state.isOfflineMode,
                    onCheckedChange = { onToggleOfflineMode() }
                )
            }
            val pending = state.syncQueue.count { it.status == "QUEUED_OFFLINE" }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("En attente", "$pending", if (pending > 0) AthletixWarning else AthletixPrimaryAccent, Modifier.weight(1f))
                MetricPill("Confirmées", "${state.syncQueue.size - pending}", Modifier.weight(1f))
            }
            if (pending > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSyncOfflineQueueNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Synchroniser les $pending modification(s) en attente")
                }
            }
        }

        // 4. APP SETTINGS, DEMO DATA & SECURITY / PRIVACY
        AthletixCard {
            SectionHeader(
                title = "Préférences, Confidentialité & Sécurité",
                subtitle = "Contrôle total sur votre thème, votre langue et vos données personnelles"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onToggleTheme, modifier = Modifier.weight(1f)) {
                    Icon(if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isDarkTheme) "Mode Clair" else "Mode Sombre")
                }
                OutlinedButton(onClick = onToggleLanguage, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Translate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Langue : ${state.languageCode}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onExportJsonData,
                modifier = Modifier.fillMaxWidth().testTag("export_json_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exporter toutes mes données (JSON)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onSwitchToDemoAthlete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Basculer sur le Profil Athlète de Démonstration")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.weight(1f).testTag("logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Déconnexion")
                }
                Button(
                    onClick = { confirmDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AthletixDanger),
                    modifier = Modifier.weight(1f).testTag("delete_account_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Supprimer Compte")
                }
            }
        }

        // 5. PROJECT CREDITS, APK VERSION & GITHUB PUBLICATION INFO
        AthletixCard(borderColor = AthletixPrimaryAccent.copy(alpha = 0.6f)) {
            SectionHeader(
                title = "À Propos du Projet & Version APK",
                subtitle = "ATHLETIX AI v2.0.0 (Build 2) • Prêt pour installation mobile & GitHub"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Conçu et réalisé par : Mendel Temgoua",
                style = MaterialTheme.typography.titleMedium.copy(color = AthletixPrimaryAccent, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Contact : mendeltemgouabusiness@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Version : 2.0.0 (Outils Force 2.0, Meal Planner Cameroun & Monde, Disques Olympiques)\n" +
                    "• Package Android : com.aistudio.athletixai.vpxkrm\n" +
                    "• Base de données : Room SQLite (14 tables locales synchronisées)\n" +
                    "• Export APK & GitHub : Utilisez le menu Paramètres / Export d'AI Studio pour télécharger directement le fichier APK v2.0.0 ou publier sur GitHub.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.exportedJsonData != null) {
        AlertDialog(
            onDismissRequest = onDismissExportDialog,
            title = { Text("Exportation des Données Personnelles (JSON)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = state.exportedJsonData,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissExportDialog) { Text("Fermer") }
            }
        )
    }

    if (confirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            title = { Text("Confirmer la suppression du compte") },
            text = {
                Text("Cette action supprimera définitivement votre profil ainsi que tous vos entraînements, repas et mesures enregistrés sur cet appareil.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthletixDanger)
                ) {
                    Text("Supprimer définitivement")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}
