package com.example.data.repository

import com.example.data.local.dao.DoseWithMedication
import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining CRUD and management operations for Medicine records.
 */
interface IMedicineRepository {

    // --- READ Operations ---
    val allMedicinesFlow: Flow<List<Medicine>>
    val activeMedicinesFlow: Flow<List<Medicine>>
    val lowStockMedicinesFlow: Flow<List<Medicine>>
    val medicationsWithSchedulesFlow: Flow<List<MedicationWithSchedules>>

    fun getMedicineById(id: Long): Flow<Medicine?>
    suspend fun getMedicineByIdDirect(id: Long): Medicine?
    fun searchMedicines(query: String): Flow<List<Medicine>>

    // --- CREATE Operations ---
    suspend fun createMedicine(medicine: Medicine): Long
    suspend fun addMedicationWithSchedules(
        medication: MedicationEntity,
        schedules: List<MedicationScheduleEntity>
    ): Long

    // --- UPDATE Operations ---
    suspend fun updateMedicine(medicine: Medicine)
    suspend fun updateMedication(
        medication: MedicationEntity,
        schedules: List<MedicationScheduleEntity>
    )
    suspend fun toggleMedicineActive(id: Long, active: Boolean)
    suspend fun refillStock(medicationId: Long, additionalQuantity: Double)

    // --- DELETE Operations ---
    suspend fun deleteMedicine(medicine: Medicine)
    suspend fun deleteMedicineById(id: Long)
    suspend fun deleteMedication(medication: MedicationEntity)

    // --- Schedule & Dose Intake Lifecycle Operations ---
    fun getTodayDoses(): Flow<List<DoseWithMedication>>
    fun getDosesForDate(dateMillis: Long): Flow<List<DoseWithMedication>>
    fun getRecentHistory(): Flow<List<DoseWithMedication>>
    suspend fun markDoseTaken(doseId: Long): Boolean
    suspend fun markDoseSnoozed(doseId: Long, snoozeMinutes: Int = 10): Boolean
    suspend fun markDoseSkipped(doseId: Long, skipReason: String? = null): Boolean
    suspend fun rescheduleDose(doseId: Long, newTimeMillis: Long)
    suspend fun generateUpcomingDosesForMedication(medicationId: Long, daysAhead: Int = 7)

    // --- Prescription Continuity & Reconciliation Operations ---
    val allPrescriptionsFlow: Flow<List<com.example.data.local.entity.PrescriptionRecordEntity>>
    val allMedicationChangesFlow: Flow<List<com.example.data.local.entity.MedicationChangeEntity>>
    val allMedicationVersionsFlow: Flow<List<com.example.data.local.entity.MedicationVersionEntity>>

    fun getMedicationVersions(medicationId: Long): Flow<List<com.example.data.local.entity.MedicationVersionEntity>>
    fun getMedicationChanges(medicationId: Long): Flow<List<com.example.data.local.entity.MedicationChangeEntity>>
    fun getChangesForPrescription(prescriptionId: Long): Flow<List<com.example.data.local.entity.MedicationChangeEntity>>

    suspend fun comparePrescriptionWithCurrentPlan(
        candidates: List<com.example.data.local.entity.OcrCandidateEntity>,
        doctorName: String = "",
        clinicName: String = "",
        prescriptionDate: Long = System.currentTimeMillis()
    ): com.example.data.reconciliation.ReconciliationResult

    suspend fun confirmMedicationReconciliation(
        reconciliationResult: com.example.data.reconciliation.ReconciliationResult,
        reviewedBy: String = "Patient"
    ): Long

    suspend fun savePrescriptionRecord(
        prescription: com.example.data.local.entity.PrescriptionRecordEntity
    ): Long

    // --- Emergency Medical ID & Emergency Snapshot Operations ---
    val emergencySnapshotFlow: Flow<com.example.data.model.EmergencySnapshot>
    val emergencyProfileFlow: Flow<com.example.data.local.entity.EmergencyProfileEntity?>
    val emergencyConditionsFlow: Flow<List<com.example.data.local.entity.EmergencyConditionEntity>>
    val emergencyAllergiesFlow: Flow<List<com.example.data.local.entity.EmergencyAllergyEntity>>
    val emergencyContactsFlow: Flow<List<com.example.data.local.entity.EmergencyContactEntity>>
    val emergencyAccessLogsFlow: Flow<List<com.example.data.local.entity.EmergencyAccessLogEntity>>

    suspend fun getEmergencySnapshotDirect(userId: String? = null): com.example.data.model.EmergencySnapshot
    suspend fun getEmergencySnapshotByIdentifier(identifier: String): com.example.data.model.EmergencySnapshot?
    suspend fun updateEmergencyProfile(profile: com.example.data.local.entity.EmergencyProfileEntity)
    suspend fun markEmergencyReviewed()
    suspend fun toggleEmergencyQr(enabled: Boolean)
    suspend fun revokeAndReissueEmergencyQr(): String
    suspend fun addEmergencyCondition(name: String, notes: String = "", diagnosedYear: String = ""): Long
    suspend fun updateEmergencyCondition(condition: com.example.data.local.entity.EmergencyConditionEntity)
    suspend fun deleteEmergencyCondition(id: Long)
    suspend fun addEmergencyAllergy(allergen: String, reaction: String = "", severity: String = "Severe"): Long
    suspend fun updateEmergencyAllergy(allergy: com.example.data.local.entity.EmergencyAllergyEntity)
    suspend fun deleteEmergencyAllergy(id: Long)
    suspend fun addEmergencyContact(name: String, relationship: String, phone: String, email: String = "", priority: Int = 1, isPrimary: Boolean = false): Long
    suspend fun updateEmergencyContact(contact: com.example.data.local.entity.EmergencyContactEntity)
    suspend fun deleteEmergencyContact(id: Long)
    suspend fun logEmergencyAccess(emergencyIdentifier: String, accessType: String, notes: String = ""): Long
}

