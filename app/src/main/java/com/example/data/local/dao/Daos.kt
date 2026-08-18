package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AuthSessionEntity
import com.example.data.local.entity.CaregiverEntity
import com.example.data.local.entity.DoseEventEntity
import com.example.data.local.entity.MedicationChangeEntity
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.local.entity.MedicationVersionEntity
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.local.entity.PrescriptionRecordEntity
import com.example.data.local.entity.PrescriptionScanEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.DoseStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

data class MedicationWithSchedules(
    @Embedded val medication: MedicationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId"
    )
    val schedules: List<MedicationScheduleEntity>
)

data class DoseWithMedication(
    @Embedded val doseEvent: DoseEventEntity,
    @Relation(
        parentColumn = "medicationId",
        entityColumn = "id"
    )
    val medication: MedicationEntity
)

data class PrescriptionWithCandidates(
    @Embedded val scan: PrescriptionScanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scanId"
    )
    val candidates: List<OcrCandidateEntity>
)

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(TRIM(email)) = LOWER(TRIM(:identifier)) OR phone = :identifier LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET onboardingCompleted = :completed, role = :role, lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun setOnboardingCompleted(userId: String, completed: Boolean, role: UserRole, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_accounts SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_accounts WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface AuthSessionDao {
    @Query("SELECT * FROM auth_session WHERE id = 1 LIMIT 1")
    fun getSessionFlow(): Flow<AuthSessionEntity?>

    @Query("SELECT * FROM auth_session WHERE id = 1 LIMIT 1")
    suspend fun getSessionDirect(): AuthSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSession(session: AuthSessionEntity)

    @Query("UPDATE auth_session SET currentUserId = NULL, lastActiveAt = :timestamp WHERE id = 1")
    suspend fun clearSession(timestamp: Long = System.currentTimeMillis())
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun getUserProfile(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getUserProfileDirect(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)
}

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregivers WHERE userId = :userId ORDER BY id DESC")
    fun getAllCaregivers(userId: String): Flow<List<CaregiverEntity>>

    @Query("SELECT * FROM caregivers WHERE userId = :userId AND enabled = 1")
    suspend fun getActiveCaregivers(userId: String): List<CaregiverEntity>

    @Query("SELECT * FROM caregivers WHERE id = :id LIMIT 1")
    suspend fun getCaregiverById(id: Long): CaregiverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaregiver(caregiver: CaregiverEntity): Long

    @Update
    suspend fun updateCaregiver(caregiver: CaregiverEntity)

    @Delete
    suspend fun deleteCaregiver(caregiver: CaregiverEntity)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE userId = :userId ORDER BY name ASC")
    fun getAllMedications(userId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllMedicationsDirect(userId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE userId = :userId AND active = 1 ORDER BY name ASC")
    fun getActiveMedications(userId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE userId = :userId AND active = 1")
    suspend fun getActiveMedicationsDirect(userId: String): List<MedicationEntity>

    @Transaction
    @Query("SELECT * FROM medications WHERE userId = :userId AND active = 1")
    suspend fun getActiveMedicationsWithSchedulesDirect(userId: String): List<MedicationWithSchedules>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationByIdFlow(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE userId = :userId AND stockQuantity <= lowStockThreshold AND active = 1")
    fun getLowStockMedications(userId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE userId = :userId AND stockQuantity <= lowStockThreshold AND active = 1")
    suspend fun getLowStockMedicationsDirect(userId: String): List<MedicationEntity>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationWithSchedules(id: Long): MedicationWithSchedules?

    @Transaction
    @Query("SELECT * FROM medications WHERE userId = :userId AND active = 1")
    fun getAllMedicationsWithSchedules(userId: String): Flow<List<MedicationWithSchedules>>

    @Query("SELECT * FROM medications WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR genericName LIKE '%' || :query || '%' OR brandName LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchMedications(userId: String, query: String): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMedicationActive(id: Long, active: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET stockQuantity = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Double)
}

@Dao
interface MedicationScheduleDao {
    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId")
    fun getSchedulesForMedication(medicationId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun getSchedulesForMedicationDirect(medicationId: Long): List<MedicationScheduleEntity>

    @Query("SELECT * FROM medication_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): MedicationScheduleEntity?

    @Query("SELECT * FROM medication_schedules WHERE enabled = 1")
    suspend fun getAllEnabledSchedules(): List<MedicationScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicationScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<MedicationScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: MedicationScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: MedicationScheduleEntity)

    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: Long)
}

@Dao
interface DoseEventDao {
    @Transaction
    @Query("""
        SELECT dose_events.* FROM dose_events 
        INNER JOIN medications ON dose_events.medicationId = medications.id 
        WHERE (medications.userId = :userId OR dose_events.userId = :userId) AND dose_events.scheduledAt >= :startTime AND dose_events.scheduledAt <= :endTime 
        ORDER BY dose_events.scheduledAt ASC
    """)
    fun getDosesBetween(userId: String, startTime: Long, endTime: Long): Flow<List<DoseWithMedication>>

    @Transaction
    @Query("""
        SELECT dose_events.* FROM dose_events 
        INNER JOIN medications ON dose_events.medicationId = medications.id 
        WHERE (medications.userId = :userId OR dose_events.userId = :userId) AND dose_events.scheduledAt >= :startTime AND dose_events.scheduledAt <= :endTime 
        ORDER BY dose_events.scheduledAt ASC
    """)
    suspend fun getDosesBetweenDirect(userId: String, startTime: Long, endTime: Long): List<DoseWithMedication>

    @Transaction
    @Query("""
        SELECT dose_events.* FROM dose_events 
        INNER JOIN medications ON dose_events.medicationId = medications.id 
        WHERE dose_events.scheduledAt >= :startTime AND dose_events.scheduledAt <= :endTime 
        ORDER BY dose_events.scheduledAt ASC
    """)
    suspend fun getAllFutureDosesDirect(startTime: Long, endTime: Long): List<DoseWithMedication>

    @Transaction
    @Query("SELECT * FROM dose_events WHERE id = :id LIMIT 1")
    suspend fun getDoseWithMedicationById(id: Long): DoseWithMedication?

    @Query("SELECT * FROM dose_events WHERE id = :id LIMIT 1")
    suspend fun getDoseEventById(id: Long): DoseEventEntity?

    @Query("SELECT * FROM dose_events WHERE medicationId = :medicationId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun getDoseEventByTime(medicationId: Long, scheduledAt: Long): DoseEventEntity?

    @Query("SELECT * FROM dose_events WHERE status = 'SCHEDULED' AND scheduledAt <= :currentTime")
    suspend fun getPendingDosesDue(currentTime: Long): List<DoseEventEntity>

    @Query("""
        SELECT * FROM dose_events 
        WHERE (status = 'SCHEDULED' OR status = 'REMINDER_SENT' OR status = 'PENDING') 
          AND scheduledAt <= :graceCutoffTime 
          AND (snoozedUntil IS NULL OR snoozedUntil <= :currentTime)
    """)
    suspend fun getExpiredGracePeriodDoses(graceCutoffTime: Long, currentTime: Long): List<DoseEventEntity>

    @Query("""
        SELECT COUNT(*) FROM dose_events 
        WHERE (userId = :userId OR medicationId IN (SELECT id FROM medications WHERE userId = :userId))
          AND status = 'MISSED'
          AND scheduledAt >= :startTime AND scheduledAt <= :endTime
    """)
    suspend fun getMissedDoseCountBetween(userId: String, startTime: Long, endTime: Long): Int

    @Transaction
    @Query("""
        SELECT dose_events.* FROM dose_events 
        INNER JOIN medications ON dose_events.medicationId = medications.id 
        WHERE (medications.userId = :userId OR dose_events.userId = :userId)
        ORDER BY dose_events.scheduledAt DESC LIMIT :limit
    """)
    fun getRecentDoseHistory(userId: String, limit: Int = 150): Flow<List<DoseWithMedication>>

    @Transaction
    @Query("""
        SELECT dose_events.* FROM dose_events 
        INNER JOIN medications ON dose_events.medicationId = medications.id 
        WHERE (medications.userId = :userId OR dose_events.userId = :userId) AND dose_events.status = 'MISSED'
        ORDER BY dose_events.scheduledAt DESC LIMIT :limit
    """)
    fun getMissedDosesFlow(userId: String, limit: Int = 50): Flow<List<DoseWithMedication>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseEvent(doseEvent: DoseEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseEvents(doseEvents: List<DoseEventEntity>)

    @Update
    suspend fun updateDoseEvent(doseEvent: DoseEventEntity)

    @Query("UPDATE dose_events SET status = :status, takenAt = :takenAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDoseStatus(id: Long, status: DoseStatus, takenAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE dose_events SET status = 'REMINDER_SENT', reminderSentAt = :reminderSentAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReminderSent(id: Long, reminderSentAt: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE dose_events SET status = 'SNOOZED', snoozedUntil = :snoozedUntil, updatedAt = :updatedAt WHERE id = :id")
    suspend fun snoozeDose(id: Long, snoozedUntil: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE dose_events SET status = 'SKIPPED', skippedAt = :skippedAt, skipReason = :reason, updatedAt = :updatedAt WHERE id = :id")
    suspend fun skipDose(id: Long, skippedAt: Long, reason: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE dose_events SET status = 'MISSED', missedAt = :missedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDoseMissed(id: Long, missedAt: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM dose_events WHERE medicationId = :medicationId AND scheduledAt > :currentTime AND (status = 'SCHEDULED' OR status = 'PENDING')")
    suspend fun deleteFutureScheduledDoses(medicationId: Long, currentTime: Long)
}

@Dao
interface PrescriptionScanDao {
    @Transaction
    @Query("SELECT * FROM prescription_scans WHERE userId = :userId ORDER BY capturedAt DESC")
    fun getAllScansWithCandidates(userId: String): Flow<List<PrescriptionWithCandidates>>

    @Transaction
    @Query("SELECT * FROM prescription_scans WHERE id = :id LIMIT 1")
    suspend fun getScanWithCandidates(id: Long): PrescriptionWithCandidates?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: PrescriptionScanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidates(candidates: List<OcrCandidateEntity>)

    @Update
    suspend fun updateCandidate(candidate: OcrCandidateEntity)

    @Delete
    suspend fun deleteScan(scan: PrescriptionScanEntity)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE userId = :userId OR userId = '' ORDER BY sentAt DESC LIMIT 100")
    fun getAllLogs(userId: String): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLogEntity): Long
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions WHERE medicationId = :medicationId ORDER BY createdAt DESC")
    fun getTransactionsForMedication(medicationId: Long): Flow<List<StockTransactionEntity>>

    @Query("""
        SELECT stock_transactions.* FROM stock_transactions 
        INNER JOIN medications ON stock_transactions.medicationId = medications.id 
        WHERE medications.userId = :userId 
        ORDER BY stock_transactions.createdAt DESC LIMIT 100
    """)
    fun getAllRecentTransactions(userId: String): Flow<List<StockTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity): Long
}

@Dao
interface PrescriptionRecordDao {
    @Query("SELECT * FROM prescription_records WHERE userId = :userId ORDER BY prescriptionDate DESC, createdAt DESC")
    fun getAllPrescriptions(userId: String): Flow<List<PrescriptionRecordEntity>>

    @Query("SELECT * FROM prescription_records WHERE id = :id LIMIT 1")
    fun getPrescriptionById(id: Long): Flow<PrescriptionRecordEntity?>

    @Query("SELECT * FROM prescription_records WHERE id = :id LIMIT 1")
    suspend fun getPrescriptionByIdDirect(id: Long): PrescriptionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(record: PrescriptionRecordEntity): Long

    @Update
    suspend fun updatePrescription(record: PrescriptionRecordEntity)

    @Delete
    suspend fun deletePrescription(record: PrescriptionRecordEntity)
}

@Dao
interface MedicationChangeDao {
    @Query("SELECT * FROM medication_changes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllChanges(userId: String): Flow<List<MedicationChangeEntity>>

    @Query("SELECT * FROM medication_changes WHERE prescriptionId = :prescriptionId ORDER BY createdAt ASC")
    fun getChangesForPrescription(prescriptionId: Long): Flow<List<MedicationChangeEntity>>

    @Query("SELECT * FROM medication_changes WHERE medicationId = :medicationId ORDER BY createdAt DESC")
    fun getChangesForMedication(medicationId: Long): Flow<List<MedicationChangeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: MedicationChangeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChanges(changes: List<MedicationChangeEntity>)

    @Update
    suspend fun updateChange(change: MedicationChangeEntity)

    @Query("UPDATE medication_changes SET reviewStatus = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt WHERE id = :id")
    suspend fun updateReviewStatus(
        id: Long,
        status: com.example.data.model.ChangeReviewStatus,
        reviewedBy: String = "Patient",
        reviewedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface MedicationVersionDao {
    @Query("SELECT * FROM medication_versions WHERE medicationId = :medicationId ORDER BY versionNumber DESC")
    fun getVersionsForMedication(medicationId: Long): Flow<List<MedicationVersionEntity>>

    @Query("SELECT * FROM medication_versions WHERE userId = :userId ORDER BY medicationId ASC, versionNumber DESC")
    fun getAllVersionsForUser(userId: String): Flow<List<MedicationVersionEntity>>

    @Query("SELECT * FROM medication_versions WHERE userId = :userId ORDER BY medicationId ASC, versionNumber DESC")
    suspend fun getAllVersionsForUserDirect(userId: String): List<MedicationVersionEntity>

    @Query("SELECT MAX(versionNumber) FROM medication_versions WHERE medicationId = :medicationId")
    suspend fun getLatestVersionNumber(medicationId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: MedicationVersionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<MedicationVersionEntity>)
}

@Dao
interface EmergencyProfileDao {
    @Query("SELECT * FROM emergency_profiles WHERE userId = :userId LIMIT 1")
    fun getEmergencyProfile(userId: String): Flow<com.example.data.local.entity.EmergencyProfileEntity?>

    @Query("SELECT * FROM emergency_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getEmergencyProfileDirect(userId: String): com.example.data.local.entity.EmergencyProfileEntity?

    @Query("SELECT * FROM emergency_profiles WHERE emergencyIdentifier = :identifier LIMIT 1")
    suspend fun getEmergencyProfileByIdentifier(identifier: String): com.example.data.local.entity.EmergencyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyProfile(profile: com.example.data.local.entity.EmergencyProfileEntity): Long

    @Update
    suspend fun updateEmergencyProfile(profile: com.example.data.local.entity.EmergencyProfileEntity)

    @Query("UPDATE emergency_profiles SET qrEnabled = :enabled, qrRevokedAt = :revokedAt, lastUpdatedAt = :now WHERE userId = :userId")
    suspend fun updateQrStatus(userId: String, enabled: Boolean, revokedAt: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE emergency_profiles SET emergencyIdentifier = :newIdentifier, qrCreatedAt = :now, qrRevokedAt = null, qrEnabled = true, lastUpdatedAt = :now WHERE userId = :userId")
    suspend fun reIssueEmergencyQr(userId: String, newIdentifier: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE emergency_profiles SET lastReviewedAt = :now, lastUpdatedAt = :now WHERE userId = :userId")
    suspend fun markReviewed(userId: String, now: Long = System.currentTimeMillis())
}

@Dao
interface EmergencyConditionDao {
    @Query("SELECT * FROM emergency_conditions WHERE userId = :userId ORDER BY createdAt ASC")
    fun getConditions(userId: String): Flow<List<com.example.data.local.entity.EmergencyConditionEntity>>

    @Query("SELECT * FROM emergency_conditions WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getConditionsDirect(userId: String): List<com.example.data.local.entity.EmergencyConditionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCondition(condition: com.example.data.local.entity.EmergencyConditionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<com.example.data.local.entity.EmergencyConditionEntity>)

    @Update
    suspend fun updateCondition(condition: com.example.data.local.entity.EmergencyConditionEntity)

    @Delete
    suspend fun deleteCondition(condition: com.example.data.local.entity.EmergencyConditionEntity)

    @Query("DELETE FROM emergency_conditions WHERE id = :id")
    suspend fun deleteConditionById(id: Long)
}

@Dao
interface EmergencyAllergyDao {
    @Query("SELECT * FROM emergency_allergies WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllergies(userId: String): Flow<List<com.example.data.local.entity.EmergencyAllergyEntity>>

    @Query("SELECT * FROM emergency_allergies WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getAllergiesDirect(userId: String): List<com.example.data.local.entity.EmergencyAllergyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergy(allergy: com.example.data.local.entity.EmergencyAllergyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergies(allergies: List<com.example.data.local.entity.EmergencyAllergyEntity>)

    @Update
    suspend fun updateAllergy(allergy: com.example.data.local.entity.EmergencyAllergyEntity)

    @Delete
    suspend fun deleteAllergy(allergy: com.example.data.local.entity.EmergencyAllergyEntity)

    @Query("DELETE FROM emergency_allergies WHERE id = :id")
    suspend fun deleteAllergyById(id: Long)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND enabled = 1 ORDER BY priority ASC, createdAt ASC")
    fun getEmergencyContacts(userId: String): Flow<List<com.example.data.local.entity.EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId ORDER BY priority ASC, createdAt ASC")
    fun getAllEmergencyContacts(userId: String): Flow<List<com.example.data.local.entity.EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND enabled = 1 ORDER BY priority ASC, createdAt ASC")
    suspend fun getEmergencyContactsDirect(userId: String): List<com.example.data.local.entity.EmergencyContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: com.example.data.local.entity.EmergencyContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<com.example.data.local.entity.EmergencyContactEntity>)

    @Update
    suspend fun updateContact(contact: com.example.data.local.entity.EmergencyContactEntity)

    @Delete
    suspend fun deleteContact(contact: com.example.data.local.entity.EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}

@Dao
interface EmergencyAccessLogDao {
    @Query("SELECT * FROM emergency_access_logs WHERE userId = :userId ORDER BY accessedAt DESC LIMIT 50")
    fun getAccessLogs(userId: String): Flow<List<com.example.data.local.entity.EmergencyAccessLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessLog(log: com.example.data.local.entity.EmergencyAccessLogEntity): Long
}

