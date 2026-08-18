package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ocr.ParsedPrescriptionResult
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.ai.AiAdviceScreen
import com.example.ui.caregiver.CaregiverManagementScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.emergency.EmergencyMedicalIdScreen
import com.example.ui.emergency.EmergencyModePreviewScreen
import com.example.ui.emergency.EmergencyWebPreviewScreen
import com.example.ui.medications.AddEditMedicationScreen
import com.example.ui.medications.MedicationListScreen
import com.example.ui.navigation.BottomNavItems
import com.example.ui.navigation.Screen
import com.example.ui.ocr.CameraScanScreen
import com.example.ui.ocr.OcrReviewScreen
import com.example.ui.onboarding.ModeSelectionScreen
import com.example.ui.passport.MedicationPassportScreen
import com.example.ui.prescription.PrescriptionHistoryScreen
import com.example.ui.prescription.PrescriptionReconciliationScreen
import com.example.ui.profile.ProfileSettingsScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.schedule.ScheduleTimelineScreen
import com.example.ui.stock.StockAlertsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MedicineApplication
        val repository = app.repository

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository)
                )

                val onboardingViewModel: com.example.ui.onboarding.OnboardingViewModel = viewModel(
                    factory = com.example.ui.onboarding.OnboardingViewModelFactory(
                        repository = repository,
                        context = this@MainActivity
                    )
                )

                MainAppContent(
                    viewModel = viewModel,
                    onboardingViewModel = onboardingViewModel,
                    initialIntent = intent
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onboardingViewModel: com.example.ui.onboarding.OnboardingViewModel,
    initialIntent: Intent?
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authState by viewModel.authState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeMedications by viewModel.activeMedications.collectAsState()
    val medicationsWithSchedules by viewModel.medicationsWithSchedules.collectAsState()
    val todayDoses by viewModel.todayDoses.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val dosesForSelectedDate by viewModel.dosesForSelectedDate.collectAsState()
    val lowStockMedications by viewModel.lowStockMedications.collectAsState()
    val caregivers by viewModel.caregivers.collectAsState()
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val historyDoses by viewModel.historyDoses.collectAsState()
    val currentOcrResult by viewModel.currentOcrResult.collectAsState()
    val onboardingState by onboardingViewModel.state.collectAsState()

    var showMainAppHandoff by remember { mutableStateOf(false) }

    // Automatic route guarding based on authState in DataStore
    LaunchedEffect(authState) {
        if (authState.canAccessDashboard) {
            val currentDest = navController.currentDestination?.route ?: currentRoute
            if (currentDest == Screen.Onboarding.route || currentDest == null) {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Handle intent navigation
    LaunchedEffect(initialIntent) {
        val target = initialIntent?.getStringExtra("EXTRA_NAV_TARGET")
        when (target) {
            "SCHEDULE" -> navController.navigate(Screen.Schedule.route)
            "STOCK" -> navController.navigate(Screen.StockAlerts.route)
        }
    }

    val showBottomBar = currentRoute in BottomNavItems.map { it.route } && !showMainAppHandoff && currentRoute != Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        BottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    if (screen.icon != null) {
                                        Icon(imageVector = screen.icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title) },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (showMainAppHandoff) {
            com.example.ui.onboarding.screens.MainAppPlaceholderScreen(
                state = onboardingState,
                onRestartOnboarding = {
                    onboardingViewModel.resetOnboarding()
                    showMainAppHandoff = false
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onEnterDashboard = {
                    showMainAppHandoff = false
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = if (authState.canAccessDashboard) Screen.Dashboard.route else Screen.Onboarding.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Complete Onboarding Flow
                composable(Screen.Onboarding.route) {
                    com.example.ui.onboarding.OnboardingFlowScreen(
                        viewModel = onboardingViewModel,
                        onFinishToMainApp = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

            // Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    userProfile = userProfile,
                    todayDoses = todayDoses,
                    lowStockMedications = lowStockMedications,
                    onTakeDose = { doseId -> viewModel.takeDose(doseId) },
                    onSnoozeDose = { doseId -> viewModel.snoozeDose(doseId, 15) },
                    onSkipDose = { doseId -> viewModel.skipDose(doseId) },
                    onNavigateToAddMedication = { navController.navigate(Screen.AddEditMedication.createRoute(-1L)) },
                    onNavigateToScanOcr = { navController.navigate(Screen.CameraScan.route) },
                    onNavigateToAiAdvice = { navController.navigate(Screen.AiAdvice.route) },
                    onNavigateToStockAlerts = { navController.navigate(Screen.StockAlerts.route) },
                    onNavigateToEmergencyId = { navController.navigate(Screen.EmergencyMedicalId.route) },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onSwitchMode = { navController.navigate(Screen.Onboarding.route) }
                )
            }

            // Medications List
            composable(Screen.Medications.route) {
                MedicationListScreen(
                    medicationsWithSchedules = medicationsWithSchedules,
                    onAddMedication = { navController.navigate(Screen.AddEditMedication.createRoute(-1L)) },
                    onScanOcr = { navController.navigate(Screen.CameraScan.route) },
                    onNavigateToPrescriptions = { navController.navigate(Screen.PrescriptionHistory.route) },
                    onNavigateToPassport = { navController.navigate(Screen.MedicationPassport.route) },
                    onEditMedication = { medId -> navController.navigate(Screen.AddEditMedication.createRoute(medId)) },
                    onToggleActive = { med -> viewModel.toggleMedicationActive(med) },
                    onDeleteMedication = { med -> viewModel.deleteMedication(med) }
                )
            }

            // Add/Edit Medication
            composable(
                route = Screen.AddEditMedication.route,
                arguments = listOf(navArgument("medId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val medId = backStackEntry.arguments?.getLong("medId") ?: -1L
                val medWithSchedules = medicationsWithSchedules.firstOrNull { it.medication.id == medId }

                AddEditMedicationScreen(
                    existingMedication = medWithSchedules?.medication,
                    existingSchedules = medWithSchedules?.schedules ?: emptyList(),
                    onSave = { med, schedules ->
                        viewModel.saveMedication(med, schedules) {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            // Schedule Timeline
            composable(Screen.Schedule.route) {
                ScheduleTimelineScreen(
                    selectedDateMillis = selectedDateMillis,
                    dosesForDay = dosesForSelectedDate,
                    onSelectDate = { dateMillis -> viewModel.selectDate(dateMillis) },
                    onTakeDose = { doseId -> viewModel.takeDose(doseId) },
                    onSnoozeDose = { doseId -> viewModel.snoozeDose(doseId, 15) },
                    onSkipDose = { doseId -> viewModel.skipDose(doseId) },
                    onRescheduleDose = { doseId, newTime -> viewModel.rescheduleDose(doseId, newTime) }
                )
            }

            // Reports & Analytics
            composable(Screen.Reports.route) {
                ReportsScreen(
                    doseHistory = historyDoses,
                    onGeneratePdf = { days -> viewModel.generatePdfReport(days) }
                )
            }

            // Profile & App Settings
            composable(Screen.Profile.route) {
                ProfileSettingsScreen(
                    currentProfile = userProfile,
                    onSaveProfile = { updated -> viewModel.saveUserProfile(updated) },
                    onNavigateToCaregivers = { navController.navigate(Screen.Caregivers.route) },
                    onNavigateToEmergencyId = { navController.navigate(Screen.EmergencyMedicalId.route) },
                    onResetDemoData = { viewModel.resetSampleDemoData() },
                    onLogout = {
                        viewModel.logout {
                            onboardingViewModel.resetOnboarding()
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Camera OCR Scanning
            composable(Screen.CameraScan.route) {
                CameraScanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOcrResultReady = { result ->
                        viewModel.setOcrResult(result)
                        navController.navigate(Screen.OcrReview.createRoute(0L))
                    }
                )
            }

            // OCR Verification & Review
            composable(
                route = Screen.OcrReview.route,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType; defaultValue = 0L })
            ) {
                val ocrResult = currentOcrResult ?: ParsedPrescriptionResult(rawText = "", candidates = emptyList())
                OcrReviewScreen(
                    ocrResult = ocrResult,
                    onReconcile = { candidates, doc, clinic, rxDate ->
                        viewModel.startReconciliation(candidates, doc, clinic, rxDate) {
                            navController.navigate(Screen.PrescriptionReconciliation.route)
                        }
                    },
                    onConfirmAndSave = { candidates ->
                        viewModel.confirmOcrCandidates(candidates) {
                            navController.navigate(Screen.Medications.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Prescription Reconciliation (Clinical Diff & Review)
            composable(Screen.PrescriptionReconciliation.route) {
                PrescriptionReconciliationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onReconciliationConfirmed = { rxId ->
                        navController.navigate(Screen.PrescriptionHistory.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            // Prescriptions & Longitudinal History
            composable(Screen.PrescriptionHistory.route) {
                PrescriptionHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScan = { navController.navigate(Screen.CameraScan.route) },
                    onNavigateToPassport = { navController.navigate(Screen.MedicationPassport.route) }
                )
            }

            // Comprehensive Medication Passport
            composable(Screen.MedicationPassport.route) {
                MedicationPassportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Caregiver Management
            composable(Screen.Caregivers.route) {
                CaregiverManagementScreen(
                    caregivers = caregivers,
                    notificationLogs = notificationLogs,
                    onAddCaregiver = { caregiver -> viewModel.addCaregiver(caregiver) },
                    onUpdateCaregiver = { caregiver -> viewModel.updateCaregiver(caregiver) },
                    onDeleteCaregiver = { caregiver -> viewModel.deleteCaregiver(caregiver) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // AI Advice Assistant
            composable(Screen.AiAdvice.route) {
                val totalDosesCount = historyDoses.size
                val takenCount = historyDoses.count { it.doseEvent.status.name == "TAKEN" }
                val missedCount = historyDoses.count { it.doseEvent.status.name == "MISSED" }
                val adhPct = if (takenCount + missedCount > 0) (takenCount.toDouble() / (takenCount + missedCount)) * 100.0 else 100.0

                AiAdviceScreen(
                    userProfile = userProfile,
                    medications = medicationsWithSchedules,
                    adherencePct = adhPct,
                    lowStockCount = lowStockMedications.size,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Stock Alerts
            composable(Screen.StockAlerts.route) {
                StockAlertsScreen(
                    medications = activeMedications,
                    onRefillStock = { medId, qty -> viewModel.refillStock(medId, qty) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Emergency Medical ID Management
            composable(Screen.EmergencyMedicalId.route) {
                EmergencyMedicalIdScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEmergencyMode = { navController.navigate(Screen.EmergencyModePreview.route) },
                    onNavigateToWebPreview = { identifier -> navController.navigate(Screen.EmergencyWebPreview.createRoute(identifier)) }
                )
            }

            // Emergency Mode (First Responder High-Contrast View)
            composable(Screen.EmergencyModePreview.route) {
                EmergencyModePreviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Emergency Web Preview Simulator
            composable(
                route = Screen.EmergencyWebPreview.route,
                arguments = listOf(navArgument("identifier") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val identifier = backStackEntry.arguments?.getString("identifier") ?: ""
                EmergencyWebPreviewScreen(
                    viewModel = viewModel,
                    identifier = identifier,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
