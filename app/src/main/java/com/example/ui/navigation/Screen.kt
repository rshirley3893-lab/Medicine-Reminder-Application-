package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Medications : Screen("medications", "Medicines", Icons.Default.Medication)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.CalendarMonth)
    object Reports : Screen("reports", "Reports", Icons.Default.Insights)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    object AddEditMedication : Screen("medication_add_edit?medId={medId}", "Medicine Form") {
        fun createRoute(medId: Long = -1L) = "medication_add_edit?medId=$medId"
    }

    object MedicationDetail : Screen("medication_detail/{medId}", "Medicine Details") {
        fun createRoute(medId: Long) = "medication_detail/$medId"
    }

    object CameraScan : Screen("camera_scan", "Scan Prescription")
    object OcrReview : Screen("ocr_review/{scanId}", "Review Prescription") {
        fun createRoute(scanId: Long = 0L) = "ocr_review/$scanId"
    }
    object PrescriptionReconciliation : Screen("prescription_reconciliation", "Prescription Reconciliation")
    object PrescriptionHistory : Screen("prescription_history", "Prescriptions & Changes")
    object MedicationPassport : Screen("medication_passport", "Medication Passport")
    object Caregivers : Screen("caregivers", "Caregivers & Alerts")
    object AiAdvice : Screen("ai_advice", "AI Advice")
    object StockAlerts : Screen("stock_alerts", "Stock Alerts")
    object EmergencyMedicalId : Screen("emergency_medical_id", "Emergency Medical ID")
    object EmergencyModePreview : Screen("emergency_mode_preview", "Emergency Mode (First Responder)")
    object EmergencyWebPreview : Screen("emergency_web_preview?identifier={identifier}", "Emergency Web Snapshot") {
        fun createRoute(identifier: String = "") = "emergency_web_preview?identifier=$identifier"
    }
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Medications,
    Screen.Schedule,
    Screen.Reports,
    Screen.Profile
)
