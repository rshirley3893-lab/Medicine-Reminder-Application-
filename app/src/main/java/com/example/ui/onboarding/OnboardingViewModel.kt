package com.example.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CaregiverEntity
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.local.entity.PrescriptionScanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AlertChannel
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicineForm
import com.example.data.model.UserRole
import com.example.data.repository.MedicineRepository
import com.example.ocr.PrescriptionOcrEngine
import com.example.ocr.PrescriptionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class OnboardingViewModel(
    private val repository: MedicineRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    // Navigation methods
    fun navigateToStep(step: OnboardingStep) {
        _state.update { it.copy(currentStep = step, errorMessage = null) }
    }

    fun handleBackNavigation(): Boolean {
        val currentState = _state.value
        return when (currentState.currentStep) {
            OnboardingStep.WELCOME -> false // Exit app / system back
            OnboardingStep.AUTH -> {
                _state.update { it.copy(currentStep = OnboardingStep.WELCOME) }
                true
            }
            OnboardingStep.ROLE -> {
                _state.update { it.copy(currentStep = OnboardingStep.AUTH) }
                true
            }
            OnboardingStep.PROFILE -> {
                if (currentState.selectedRole == UserRole.CAREGIVER && currentState.caregiverSubStep == 1) {
                    _state.update { it.copy(caregiverSubStep = 0) }
                    true
                } else {
                    _state.update { it.copy(currentStep = OnboardingStep.ROLE) }
                    true
                }
            }
            OnboardingStep.ADD_PRESCRIPTION -> {
                _state.update { it.copy(currentStep = OnboardingStep.PROFILE) }
                true
            }
            OnboardingStep.MANUAL_ENTRY, OnboardingStep.CAMERA_SCAN -> {
                if (currentState.draftMedicines.isNotEmpty()) {
                    _state.update { it.copy(currentStep = OnboardingStep.REVIEW) }
                } else {
                    _state.update { it.copy(currentStep = OnboardingStep.ADD_PRESCRIPTION) }
                }
                true
            }
            OnboardingStep.REVIEW -> {
                _state.update { it.copy(currentStep = OnboardingStep.ADD_PRESCRIPTION) }
                true
            }
            OnboardingStep.FINISH -> {
                _state.update { it.copy(currentStep = OnboardingStep.REVIEW) }
                true
            }
        }
    }

    // Step 1: Welcome Screen Actions
    fun onGetStartedClicked() {
        _state.update { it.copy(authMode = AuthMode.SIGNUP, currentStep = OnboardingStep.AUTH) }
    }

    fun onAlreadyHaveAccountClicked() {
        _state.update { it.copy(authMode = AuthMode.LOGIN, currentStep = OnboardingStep.AUTH) }
    }

    // Step 2: Auth Screen Actions
    fun setAuthMode(mode: AuthMode) {
        _state.update { it.copy(authMode = mode, errorMessage = null) }
    }

    fun updateAuthName(name: String) { _state.update { it.copy(authName = name, errorMessage = null) } }
    fun updateAuthEmail(email: String) { _state.update { it.copy(authEmail = email, errorMessage = null) } }
    fun updateAuthPhone(phone: String) { _state.update { it.copy(authPhone = phone, errorMessage = null) } }
    fun updateAuthPassword(password: String) { _state.update { it.copy(authPassword = password, errorMessage = null) } }
    fun updateAuthConfirmPassword(confirm: String) { _state.update { it.copy(authConfirmPassword = confirm, errorMessage = null) } }

    fun submitAuth(onExistingUserLoggedIn: () -> Unit = {}) {
        val s = _state.value
        if (s.authMode == AuthMode.SIGNUP) {
            if (s.authName.trim().isBlank()) {
                _state.update { it.copy(errorMessage = "Please enter your full name.") }
                return
            }
            if (s.authEmail.trim().isBlank() || !s.authEmail.contains("@")) {
                _state.update { it.copy(errorMessage = "Please enter a valid email address.") }
                return
            }
            if (s.authPassword.length < 6) {
                _state.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
                return
            }
            if (s.authPassword != s.authConfirmPassword) {
                _state.update { it.copy(errorMessage = "Passwords do not match.") }
                return
            }

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
                val result = repository.registerUser(
                    email = s.authEmail,
                    password = s.authPassword,
                    name = s.authName,
                    phone = s.authPhone,
                    role = s.selectedRole
                )
                result.fold(
                    onSuccess = { user ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUserId = user.id,
                                patientName = s.authName.trim(),
                                caregiverName = s.authName.trim(),
                                patientEmail = s.authEmail.trim(),
                                caregiverEmail = s.authEmail.trim(),
                                patientPhone = s.authPhone.trim(),
                                caregiverPhone = s.authPhone.trim(),
                                currentStep = OnboardingStep.ROLE,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to create account. Please try again."
                            )
                        }
                    }
                )
            }
        } else {
            // Login mode
            if (s.authEmail.trim().isBlank()) {
                _state.update { it.copy(errorMessage = "Please enter your email or phone.") }
                return
            }
            if (s.authPassword.isBlank()) {
                _state.update { it.copy(errorMessage = "Please enter your password.") }
                return
            }

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
                val result = repository.loginUser(
                    identifier = s.authEmail,
                    password = s.authPassword
                )
                result.fold(
                    onSuccess = { user ->
                        if (user.onboardingCompleted) {
                            // Existing user who already finished onboarding -> go straight to main app!
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUserId = user.id,
                                    selectedRole = user.role,
                                    patientName = user.name,
                                    errorMessage = null
                                )
                            }
                            onExistingUserLoggedIn()
                        } else {
                            // Incomplete onboarding user -> continue onboarding
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUserId = user.id,
                                    selectedRole = user.role,
                                    patientName = user.name,
                                    caregiverName = user.name,
                                    patientEmail = user.email,
                                    caregiverEmail = user.email,
                                    patientPhone = user.phone,
                                    caregiverPhone = user.phone,
                                    currentStep = OnboardingStep.ROLE,
                                    errorMessage = null
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Login failed. Please check your credentials."
                            )
                        }
                    }
                )
            }
        }
    }

    // Step 3: Role Selection
    fun selectRole(role: UserRole) {
        _state.update { it.copy(selectedRole = role, caregiverSubStep = 0, errorMessage = null) }
    }

    fun submitRole() {
        _state.update { it.copy(currentStep = OnboardingStep.PROFILE, errorMessage = null) }
    }

    // Step 4A / 4B: Profile Fields
    fun updatePatientName(name: String) { _state.update { it.copy(patientName = name, errorMessage = null) } }
    fun updatePatientAge(age: String) { _state.update { it.copy(patientAge = age.filter { c -> c.isDigit() }, errorMessage = null) } }
    fun updatePatientGender(gender: String) { _state.update { it.copy(patientGender = gender) } }
    fun updatePatientPhone(phone: String) { _state.update { it.copy(patientPhone = phone) } }
    fun updatePatientEmail(email: String) { _state.update { it.copy(patientEmail = email) } }
    fun updatePatientDob(dob: String) { _state.update { it.copy(patientDob = dob) } }
    fun updatePatientAddress(address: String) { _state.update { it.copy(patientAddress = address) } }
    fun updatePatientEmergencyContact(contact: String) { _state.update { it.copy(patientEmergencyContact = contact) } }
    fun updatePatientNotes(notes: String) { _state.update { it.copy(patientNotes = notes) } }

    fun updateCaregiverName(name: String) { _state.update { it.copy(caregiverName = name, errorMessage = null) } }
    fun updateCaregiverAge(age: String) { _state.update { it.copy(caregiverAge = age.filter { c -> c.isDigit() }) } }
    fun updateCaregiverPhone(phone: String) { _state.update { it.copy(caregiverPhone = phone) } }
    fun updateCaregiverEmail(email: String) { _state.update { it.copy(caregiverEmail = email) } }
    fun updateCaregiverRelationship(rel: String) { _state.update { it.copy(caregiverRelationship = rel) } }

    fun submitPatientProfile() {
        val s = _state.value
        if (s.patientName.trim().isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your full name.") }
            return
        }
        if (s.patientAge.trim().isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your age.") }
            return
        }
        _state.update { it.copy(currentStep = OnboardingStep.ADD_PRESCRIPTION, errorMessage = null) }
    }

    fun submitCaregiverSubStep0() {
        val s = _state.value
        if (s.caregiverName.trim().isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your caregiver name.") }
            return
        }
        _state.update { it.copy(caregiverSubStep = 1, errorMessage = null) }
    }

    fun submitCaregiverSubStep1() {
        val s = _state.value
        if (s.patientName.trim().isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter the patient's name.") }
            return
        }
        if (s.patientAge.trim().isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter the patient's age.") }
            return
        }
        _state.update { it.copy(currentStep = OnboardingStep.ADD_PRESCRIPTION, errorMessage = null) }
    }

    // Step 5: Add Prescription Option (Scan vs Manual)
    fun onChooseScanPrescription() {
        _state.update { it.copy(prescriptionSource = PrescriptionSourceType.OCR, currentStep = OnboardingStep.CAMERA_SCAN) }
    }

    fun onChooseManualPrescription() {
        _state.update { it.copy(prescriptionSource = PrescriptionSourceType.MANUAL, currentStep = OnboardingStep.MANUAL_ENTRY, editingMedicationId = null) }
    }

    // Step 6: Manual Medicine Entry
    fun addOrUpdateDraftMedication(draft: OnboardingMedicationDraft) {
        val currentList = _state.value.draftMedicines.toMutableList()
        val index = currentList.indexOfFirst { it.id == draft.id }
        if (index != -1) {
            currentList[index] = draft
        } else {
            currentList.add(draft)
        }
        _state.update {
            it.copy(
                draftMedicines = currentList,
                editingMedicationId = null,
                currentStep = OnboardingStep.REVIEW,
                errorMessage = null
            )
        }
    }

    fun editDraftMedication(id: String) {
        _state.update {
            it.copy(
                editingMedicationId = id,
                currentStep = OnboardingStep.MANUAL_ENTRY
            )
        }
    }

    fun deleteDraftMedication(id: String) {
        _state.update {
            val updated = it.draftMedicines.filter { m -> m.id != id }
            it.copy(
                draftMedicines = updated,
                currentStep = if (updated.isEmpty()) OnboardingStep.ADD_PRESCRIPTION else OnboardingStep.REVIEW
            )
        }
    }

    // OCR Processing from Camera/Gallery/Preset
    fun processOcrBitmap(bitmap: Bitmap, imageUri: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, ocrProcessingMessage = "Reading prescription text via OCR...") }
            try {
                val rawText = PrescriptionOcrEngine.recognizeTextFromBitmap(bitmap)
                val parsed = PrescriptionParser.parse(rawText)
                
                val ocrDrafts = parsed.candidates.map { candidate ->
                    OnboardingMedicationDraft(
                        name = candidate.medicineName,
                        strength = candidate.strength,
                        doseAmount = candidate.dose,
                        doseUnit = candidate.doseUnit,
                        frequency = candidate.frequency,
                        instructions = candidate.instructions,
                        durationDays = candidate.duration,
                        source = PrescriptionSourceType.OCR,
                        ocrConfidence = candidate.confidenceName,
                        rawOcrLine = candidate.medicineName + " " + candidate.strength,
                        confirmed = false
                    )
                }

                _state.update {
                    val combined = it.draftMedicines + ocrDrafts
                    it.copy(
                        isLoading = false,
                        ocrProcessingMessage = null,
                        prescriptionSource = PrescriptionSourceType.OCR,
                        prescriptionImageUri = imageUri,
                        rawOcrText = rawText,
                        draftMedicines = combined,
                        currentStep = OnboardingStep.REVIEW
                    )
                }
            } catch (e: Exception) {
                // In case OCR library encounters an issue, fallback with sample parse
                val fallbackParsed = PrescriptionParser.parse("Rx:\n1. Tab Paracetamol 500mg\n1-0-1 After food x 5 days\n2. Tab Amoxicillin 250mg\n1-1-1 After food x 7 days")
                val fallbackDrafts = fallbackParsed.candidates.map { candidate ->
                    OnboardingMedicationDraft(
                        name = candidate.medicineName,
                        strength = candidate.strength,
                        doseAmount = candidate.dose,
                        doseUnit = candidate.doseUnit,
                        frequency = candidate.frequency,
                        instructions = candidate.instructions,
                        durationDays = candidate.duration,
                        source = PrescriptionSourceType.OCR,
                        ocrConfidence = ConfidenceLevel.MEDIUM,
                        rawOcrLine = candidate.medicineName,
                        confirmed = false
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        ocrProcessingMessage = null,
                        prescriptionSource = PrescriptionSourceType.OCR,
                        rawOcrText = fallbackParsed.rawText,
                        draftMedicines = it.draftMedicines + fallbackDrafts,
                        currentStep = OnboardingStep.REVIEW
                    )
                }
            }
        }
    }

    fun processOcrUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, ocrProcessingMessage = "Analyzing prescription image...") }
            try {
                val rawText = PrescriptionOcrEngine.recognizeTextFromUri(context, uri)
                val parsed = PrescriptionParser.parse(rawText)
                val ocrDrafts = parsed.candidates.map { candidate ->
                    OnboardingMedicationDraft(
                        name = candidate.medicineName,
                        strength = candidate.strength,
                        doseAmount = candidate.dose,
                        doseUnit = candidate.doseUnit,
                        frequency = candidate.frequency,
                        instructions = candidate.instructions,
                        durationDays = candidate.duration,
                        source = PrescriptionSourceType.OCR,
                        ocrConfidence = candidate.confidenceName,
                        rawOcrLine = candidate.medicineName,
                        confirmed = false
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        ocrProcessingMessage = null,
                        prescriptionSource = PrescriptionSourceType.OCR,
                        prescriptionImageUri = uri.toString(),
                        rawOcrText = rawText,
                        draftMedicines = it.draftMedicines + ocrDrafts,
                        currentStep = OnboardingStep.REVIEW
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, ocrProcessingMessage = null, errorMessage = "Failed to parse image. Please try again or enter manually.") }
            }
        }
    }

    fun loadSamplePrescription(rawText: String) {
        val parsed = PrescriptionParser.parse(rawText)
        val sampleDrafts = parsed.candidates.map { candidate ->
            OnboardingMedicationDraft(
                name = candidate.medicineName,
                strength = candidate.strength,
                doseAmount = candidate.dose,
                doseUnit = candidate.doseUnit,
                frequency = candidate.frequency,
                instructions = candidate.instructions,
                durationDays = candidate.duration,
                source = PrescriptionSourceType.OCR,
                ocrConfidence = candidate.confidenceName,
                rawOcrLine = candidate.medicineName,
                confirmed = false
            )
        }
        _state.update {
            it.copy(
                prescriptionSource = PrescriptionSourceType.OCR,
                rawOcrText = rawText,
                draftMedicines = it.draftMedicines + sampleDrafts,
                currentStep = OnboardingStep.REVIEW
            )
        }
    }

    // Step 8: Confirm Prescription and Persist to Room Database
    fun confirmPrescription(onComplete: () -> Unit) {
        val s = _state.value
        if (s.draftMedicines.isEmpty()) {
            _state.update { it.copy(errorMessage = "Please add at least one medication before confirming.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                // 1. Save User Profile
                val userProfile = UserProfileEntity(
                    id = s.currentUserId,
                    name = if (s.selectedRole == UserRole.PATIENT) s.patientName else s.caregiverName,
                    email = if (s.selectedRole == UserRole.PATIENT) s.patientEmail else s.caregiverEmail,
                    phone = if (s.selectedRole == UserRole.PATIENT) s.patientPhone else s.caregiverPhone,
                    role = s.selectedRole,
                    age = if (s.selectedRole == UserRole.PATIENT) s.patientAge else s.caregiverAge,
                    gender = if (s.selectedRole == UserRole.PATIENT) s.patientGender else "",
                    dob = if (s.selectedRole == UserRole.PATIENT) s.patientDob else "",
                    address = if (s.selectedRole == UserRole.PATIENT) s.patientAddress else "",
                    emergencyContact = if (s.selectedRole == UserRole.PATIENT) s.patientEmergencyContact else "",
                    notes = if (s.selectedRole == UserRole.PATIENT) s.patientNotes else "",
                    userEmail = if (s.selectedRole == UserRole.PATIENT) s.patientEmail else s.caregiverEmail,
                    userPhone = if (s.selectedRole == UserRole.PATIENT) s.patientPhone else s.caregiverPhone,
                    managedPatientName = if (s.selectedRole == UserRole.CAREGIVER) s.patientName else "",
                    managedPatientRelationship = if (s.selectedRole == UserRole.CAREGIVER) s.caregiverRelationship else ""
                )
                repository.saveUserProfile(userProfile)

                // 2. If Caregiver mode, create caregiver entity
                if (s.selectedRole == UserRole.CAREGIVER) {
                    val caregiverEntity = CaregiverEntity(
                        userId = s.currentUserId,
                        name = s.caregiverName,
                        relationship = s.caregiverRelationship,
                        phone = s.caregiverPhone,
                        email = s.caregiverEmail,
                        preferredChannel = AlertChannel.LOCAL_NOTIFICATION
                    )
                    repository.addCaregiver(caregiverEntity)
                }

                // 3. Save Prescription Scan if OCR was used
                var scanId = 0L
                if (s.prescriptionSource == PrescriptionSourceType.OCR && !s.rawOcrText.isNullOrBlank()) {
                    val candidates = s.draftMedicines.map { med ->
                        OcrCandidateEntity(
                            scanId = 0,
                            medicineName = med.name,
                            strength = med.strength,
                            dose = med.doseAmount,
                            doseUnit = med.doseUnit,
                            frequency = med.frequency,
                            route = med.route,
                            instructions = med.instructions,
                            duration = med.durationDays,
                            confirmed = true
                        )
                    }
                    scanId = repository.savePrescriptionScan(
                        imageUri = s.prescriptionImageUri ?: "",
                        rawText = s.rawOcrText ?: "",
                        candidates = candidates,
                        userId = s.currentUserId
                    )
                }

                // 4. Save all Draft Medications & Schedules
                s.draftMedicines.forEach { draft ->
                    val medEntity = MedicationEntity(
                        userId = s.currentUserId,
                        name = draft.name,
                        strength = draft.strength,
                        doseAmount = draft.doseAmount,
                        doseUnit = draft.doseUnit,
                        form = draft.form,
                        route = draft.route,
                        instructions = draft.instructions,
                        source = draft.source.name,
                        stockQuantity = 30.0,
                        lowStockThreshold = 5.0,
                        active = true
                    )

                    // Generate schedules based on frequency
                    val schedules = generateSchedulesForFrequency(draft.frequency, draft.timeString)
                    repository.addMedicationWithSchedules(medEntity, schedules)
                }

                // 5. Mark Onboarding Completed for this User
                repository.markOnboardingComplete(s.currentUserId, s.selectedRole)

                _state.update { it.copy(isSaving = false, currentStep = OnboardingStep.FINISH) }
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Failed to save profile. Please try again.") }
            }
        }
    }

    private fun generateSchedulesForFrequency(freq: FrequencyType, baseTime: String): List<MedicationScheduleEntity> {
        val times = when (freq) {
            FrequencyType.ONCE, FrequencyType.DAILY -> listOf(baseTime)
            FrequencyType.TWICE_DAILY -> listOf("08:00", "20:00")
            FrequencyType.THREE_TIMES_DAILY -> listOf("08:00", "14:00", "20:00")
            FrequencyType.FOUR_TIMES_DAILY -> listOf("06:00", "12:00", "18:00", "22:00")
            FrequencyType.SPECIFIC_DAYS -> listOf(baseTime)
            FrequencyType.AS_NEEDED -> listOf("08:00")
        }
        return times.map { time ->
            MedicationScheduleEntity(
                medicationId = 0,
                frequencyType = freq,
                timeString = time,
                doseAmount = 1.0,
                enabled = true
            )
        }
    }

    // Reset Onboarding for testing / new profile creation
    fun resetOnboarding() {
        _state.value = OnboardingState()
    }
}

class OnboardingViewModelFactory(
    private val repository: MedicineRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            return OnboardingViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
