package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.UserAuthState
import com.example.data.local.dao.DoseWithMedication
import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.entity.CaregiverEntity
import com.example.data.local.entity.EmergencyAccessLogEntity
import com.example.data.local.entity.EmergencyAllergyEntity
import com.example.data.local.entity.EmergencyConditionEntity
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.EmergencyProfileEntity
import com.example.data.local.entity.MedicationChangeEntity
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.local.entity.MedicationVersionEntity
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.local.entity.PrescriptionRecordEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.ChangeReviewStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.EmergencySnapshot
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.Medicine
import com.example.data.model.UserRole
import com.example.data.reconciliation.ReconciliationItem
import com.example.data.reconciliation.ReconciliationResult
import com.example.data.repository.MedicineRepository
import com.example.ocr.ParsedPrescriptionResult
import com.example.report.EmergencyCardPdfGenerator
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ReconciliationSummaryBanner(
    val prescriptionId: Long,
    val newCount: Int,
    val changedCount: Int,
    val unchangedCount: Int,
    val notFoundCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: MedicineRepository) : ViewModel() {

    val authState: StateFlow<UserAuthState> = repository.userAuthManager.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAuthState())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMedicines: StateFlow<List<Medicine>> = repository.allMedicinesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMedicines: StateFlow<List<Medicine>> = repository.activeMedicinesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMedications: StateFlow<List<MedicationEntity>> = repository.activeMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationsWithSchedules: StateFlow<List<MedicationWithSchedules>> = repository.medicationsWithSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPrescriptions: StateFlow<List<PrescriptionRecordEntity>> = repository.allPrescriptionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedicationChanges: StateFlow<List<MedicationChangeEntity>> = repository.allMedicationChangesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedicationVersions: StateFlow<List<MedicationVersionEntity>> = repository.allMedicationVersionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Emergency Medical ID & Snapshot Flows ---

    val emergencySnapshot: StateFlow<EmergencySnapshot> = repository.emergencySnapshotFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmergencySnapshot())

    val emergencyProfile: StateFlow<EmergencyProfileEntity?> = repository.emergencyProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emergencyConditions: StateFlow<List<EmergencyConditionEntity>> = repository.emergencyConditionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyAllergies: StateFlow<List<EmergencyAllergyEntity>> = repository.emergencyAllergiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContactEntity>> = repository.emergencyContactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyAccessLogs: StateFlow<List<EmergencyAccessLogEntity>> = repository.emergencyAccessLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentReconciliationResult = MutableStateFlow<ReconciliationResult?>(null)
    val currentReconciliationResult: StateFlow<ReconciliationResult?> = _currentReconciliationResult.asStateFlow()

    private val _reconciliationBanner = MutableStateFlow<ReconciliationSummaryBanner?>(null)
    val reconciliationBanner: StateFlow<ReconciliationSummaryBanner?> = _reconciliationBanner.asStateFlow()


    val todayDoses: StateFlow<List<DoseWithMedication>> = repository.getTodayDoses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    val dosesForSelectedDate: StateFlow<List<DoseWithMedication>> = _selectedDateMillis
        .flatMapLatest { dateMillis -> repository.getDosesForDate(dateMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockMedications: StateFlow<List<MedicationEntity>> = repository.lowStockMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val caregivers: StateFlow<List<CaregiverEntity>> = repository.caregivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationLogs: StateFlow<List<NotificationLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyDoses: StateFlow<List<DoseWithMedication>> = repository.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentOcrResult = MutableStateFlow<ParsedPrescriptionResult?>(null)
    val currentOcrResult: StateFlow<ParsedPrescriptionResult?> = _currentOcrResult.asStateFlow()

    fun selectDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    fun setOcrResult(result: ParsedPrescriptionResult) {
        _currentOcrResult.value = result
    }

    fun takeDose(doseId: Long) {
        viewModelScope.launch {
            repository.markDoseTaken(doseId)
        }
    }

    fun snoozeDose(doseId: Long, minutes: Int = 15) {
        viewModelScope.launch {
            repository.markDoseSnoozed(doseId, minutes)
        }
    }

    fun skipDose(doseId: Long) {
        viewModelScope.launch {
            repository.markDoseSkipped(doseId)
        }
    }

    fun rescheduleDose(doseId: Long, newTimeMillis: Long) {
        viewModelScope.launch {
            repository.rescheduleDose(doseId, newTimeMillis)
        }
    }

    fun saveMedication(medication: MedicationEntity, schedules: List<MedicationScheduleEntity>, onDone: () -> Unit) {
        viewModelScope.launch {
            if (medication.id > 0) {
                repository.updateMedication(medication, schedules)
            } else {
                repository.addMedicationWithSchedules(medication, schedules)
            }
            onDone()
        }
    }

    fun saveMedicine(medicine: Medicine, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (medicine.id > 0) {
                repository.updateMedicine(medicine)
            } else {
                repository.createMedicine(medicine)
            }
            onDone()
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
        }
    }

    fun deleteMedicineById(id: Long) {
        viewModelScope.launch {
            repository.deleteMedicineById(id)
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    fun toggleMedicationActive(medication: MedicationEntity) {
        viewModelScope.launch {
            val updated = medication.copy(active = !medication.active)
            repository.updateMedication(updated, emptyList())
        }
    }

    fun toggleMedicineActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            repository.toggleMedicineActive(id, active)
        }
    }

    fun refillStock(medicationId: Long, quantity: Double) {
        viewModelScope.launch {
            repository.refillStock(medicationId, quantity)
        }
    }

    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    fun addCaregiver(caregiver: CaregiverEntity) {
        viewModelScope.launch {
            repository.addCaregiver(caregiver)
        }
    }

    fun updateCaregiver(caregiver: CaregiverEntity) {
        viewModelScope.launch {
            repository.updateCaregiver(caregiver)
        }
    }

    fun deleteCaregiver(caregiver: CaregiverEntity) {
        viewModelScope.launch {
            repository.deleteCaregiver(caregiver)
        }
    }

    fun confirmOcrCandidates(candidates: List<OcrCandidateEntity>, onDone: () -> Unit) {
        viewModelScope.launch {
            for (candidate in candidates) {
                repository.confirmOcrCandidateAsMedication(candidate)
            }
            onDone()
        }
    }

    fun startReconciliation(
        candidates: List<OcrCandidateEntity>,
        doctorName: String = "",
        clinicName: String = "",
        prescriptionDate: Long = System.currentTimeMillis(),
        onReady: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.comparePrescriptionWithCurrentPlan(
                candidates = candidates,
                doctorName = doctorName,
                clinicName = clinicName,
                prescriptionDate = prescriptionDate
            )
            _currentReconciliationResult.value = result
            onReady()
        }
    }

    fun updateReconciliationDecision(
        itemId: String,
        decision: ChangeReviewStatus,
        markDiscontinued: Boolean = false
    ) {
        val current = _currentReconciliationResult.value ?: return
        val updatedItems = current.items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    reviewDecision = decision,
                    markDiscontinued = markDiscontinued
                )
            } else {
                item
            }
        }
        _currentReconciliationResult.value = current.copy(items = updatedItems)
    }

    fun updateReconciliationItemCustomEdit(
        itemId: String,
        customName: String?,
        customStrength: String?,
        customDoseAmount: Double?,
        customFrequency: FrequencyType?,
        customInstructions: String?
    ) {
        val current = _currentReconciliationResult.value ?: return
        val updatedItems = current.items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    customMedicineName = customName,
                    customStrength = customStrength,
                    customDoseAmount = customDoseAmount,
                    customFrequency = customFrequency,
                    customInstructions = customInstructions,
                    reviewDecision = ChangeReviewStatus.EDITED
                )
            } else {
                item
            }
        }
        _currentReconciliationResult.value = current.copy(items = updatedItems)
    }

    fun confirmReconciliation(
        reviewedBy: String = "Patient",
        onSuccess: (Long) -> Unit = {}
    ) {
        val current = _currentReconciliationResult.value ?: return
        viewModelScope.launch {
            val rxId = repository.confirmMedicationReconciliation(current, reviewedBy)
            _reconciliationBanner.value = ReconciliationSummaryBanner(
                prescriptionId = rxId,
                newCount = current.newItems.size,
                changedCount = current.changedItems.size,
                unchangedCount = current.unchangedItems.size,
                notFoundCount = current.notFoundItems.size
            )
            _currentReconciliationResult.value = null
            onSuccess(rxId)
        }
    }

    fun dismissReconciliationBanner() {
        _reconciliationBanner.value = null
    }

    fun generatePassportPdf(onReady: (File) -> Unit) {
        viewModelScope.launch {
            val file = repository.generateMedicationPassportPdf()
            onReady(file)
        }
    }

    suspend fun generatePdfReport(days: Int): File {
        return repository.generateAdherenceReportPdf(days)
    }


    fun checkMissedDoses() {
        viewModelScope.launch {
            repository.checkAndEscalateMissedDoses()
        }
    }

    fun triggerTestReminder(doseId: Long) {
        viewModelScope.launch {
            repository.triggerTestReminderNotification(doseId)
        }
    }

    fun triggerTestMissedAlert(doseId: Long) {
        viewModelScope.launch {
            repository.triggerTestMissedAlert(doseId)
        }
    }

    fun resetSampleDemoData() {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // --- Emergency Medical ID Actions ---

    fun updateEmergencyProfile(profile: EmergencyProfileEntity) {
        viewModelScope.launch {
            repository.updateEmergencyProfile(profile)
        }
    }

    fun markEmergencyReviewed() {
        viewModelScope.launch {
            repository.markEmergencyReviewed()
        }
    }

    fun toggleEmergencyQr(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleEmergencyQr(enabled)
        }
    }

    fun revokeAndReissueEmergencyQr(onNewQr: (String) -> Unit = {}) {
        viewModelScope.launch {
            val newIdentifier = repository.revokeAndReissueEmergencyQr()
            onNewQr(newIdentifier)
        }
    }

    fun addEmergencyCondition(name: String, notes: String = "", diagnosedYear: String = "") {
        viewModelScope.launch {
            repository.addEmergencyCondition(name, notes, diagnosedYear)
        }
    }

    fun updateEmergencyCondition(condition: EmergencyConditionEntity) {
        viewModelScope.launch {
            repository.updateEmergencyCondition(condition)
        }
    }

    fun deleteEmergencyCondition(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyCondition(id)
        }
    }

    fun addEmergencyAllergy(allergen: String, reaction: String = "", severity: String = "Severe") {
        viewModelScope.launch {
            repository.addEmergencyAllergy(allergen, reaction, severity)
        }
    }

    fun updateEmergencyAllergy(allergy: EmergencyAllergyEntity) {
        viewModelScope.launch {
            repository.updateEmergencyAllergy(allergy)
        }
    }

    fun deleteEmergencyAllergy(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyAllergy(id)
        }
    }

    fun addEmergencyContact(
        name: String,
        relationship: String,
        phone: String,
        email: String = "",
        priority: Int = 1,
        isPrimary: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addEmergencyContact(name, relationship, phone, email, priority, isPrimary)
        }
    }

    fun updateEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repository.updateEmergencyContact(contact)
        }
    }

    fun deleteEmergencyContact(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
        }
    }

    fun logEmergencyAccess(emergencyIdentifier: String, accessType: String, notes: String = "") {
        viewModelScope.launch {
            repository.logEmergencyAccess(emergencyIdentifier, accessType, notes)
        }
    }

    suspend fun getEmergencySnapshotByIdentifier(identifier: String): EmergencySnapshot? {
        return repository.getEmergencySnapshotByIdentifier(identifier)
    }

    fun exportEmergencyCardPdf(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            val snapshot = repository.getEmergencySnapshotDirect()
            val file = EmergencyCardPdfGenerator.generateEmergencyIdCardPdf(context, snapshot)
            onReady(file)
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }
}

class MainViewModelFactory(private val repository: MedicineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
