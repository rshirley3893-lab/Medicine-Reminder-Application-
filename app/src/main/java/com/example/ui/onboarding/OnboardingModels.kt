package com.example.ui.onboarding

import com.example.data.model.ConfidenceLevel
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicineForm
import com.example.data.model.UserRole
import java.util.UUID

/**
 * Steps in the guided onboarding flow
 */
enum class OnboardingStep(val stepNumber: Int, val title: String) {
    WELCOME(0, "Welcome"),
    AUTH(1, "Account"),
    ROLE(2, "Role"),
    PROFILE(3, "Profile"),
    ADD_PRESCRIPTION(4, "Prescription"),
    MANUAL_ENTRY(4, "Prescription"),
    CAMERA_SCAN(4, "Prescription"),
    REVIEW(5, "Review"),
    FINISH(5, "Completed")
}

enum class AuthMode {
    LOGIN,
    SIGNUP
}

enum class PrescriptionSourceType(val displayName: String) {
    OCR("Camera OCR Scan"),
    MANUAL("Manual Entry")
}

/**
 * In-memory draft for a medication added during onboarding
 */
data class OnboardingMedicationDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val genericName: String = "",
    val strength: String = "",
    val doseAmount: Double = 1.0,
    val doseUnit: DoseUnit = DoseUnit.TABLET,
    val form: MedicineForm = MedicineForm.TABLET,
    val frequency: FrequencyType = FrequencyType.DAILY,
    val timeString: String = "08:00",
    val route: String = "Oral",
    val instructions: String = "After food",
    val durationDays: String = "7 days",
    val startDate: Long = System.currentTimeMillis(),
    val source: PrescriptionSourceType = PrescriptionSourceType.MANUAL,
    val ocrConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val rawOcrLine: String = "",
    val confirmed: Boolean = true
)

/**
 * Complete onboarding session state preserved across navigation steps
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    
    // Auth State
    val authMode: AuthMode = AuthMode.SIGNUP,
    val authName: String = "",
    val authEmail: String = "",
    val authPhone: String = "",
    val authPassword: String = "",
    val authConfirmPassword: String = "",
    val isLoggedIn: Boolean = false,
    val currentUserId: String = "user_" + UUID.randomUUID().toString().take(8),
    
    // Role Selection
    val selectedRole: UserRole = UserRole.PATIENT,
    
    // Patient Profile Fields (Screen 4A or Caregiver's Patient in 4B)
    val patientName: String = "",
    val patientAge: String = "",
    val patientGender: String = "Male",
    val patientPhone: String = "",
    val patientEmail: String = "",
    val patientDob: String = "",
    val patientAddress: String = "",
    val patientEmergencyContact: String = "",
    val patientNotes: String = "",
    
    // Caregiver Profile Fields (Screen 4B)
    val caregiverName: String = "",
    val caregiverAge: String = "",
    val caregiverPhone: String = "",
    val caregiverEmail: String = "",
    val caregiverRelationship: String = "Spouse",
    
    // Caregiver Sub-Step (0: Caregiver Info, 1: Patient Info)
    val caregiverSubStep: Int = 0,
    
    // Prescription Draft
    val prescriptionSource: PrescriptionSourceType = PrescriptionSourceType.MANUAL,
    val prescriptionImageUri: String? = null,
    val rawOcrText: String? = null,
    val draftMedicines: List<OnboardingMedicationDraft> = emptyList(),
    
    // Editing existing draft
    val editingMedicationId: String? = null,
    
    // UI state
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val ocrProcessingMessage: String? = null,
    val isSaving: Boolean = false
)
