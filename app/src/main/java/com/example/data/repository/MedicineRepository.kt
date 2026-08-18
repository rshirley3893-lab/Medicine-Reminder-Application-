package com.example.data.repository

import android.content.Context
import com.example.backend.CommunicationService
import com.example.data.auth.UserAuthManager
import com.example.data.auth.UserAuthState
import com.example.data.local.AppDatabase
import com.example.data.local.dao.DoseWithMedication
import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.dao.PrescriptionWithCandidates
import com.example.data.local.entity.AuthSessionEntity
import com.example.data.local.entity.CaregiverEntity
import com.example.data.local.entity.DoseEventEntity
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
import com.example.data.local.entity.PrescriptionScanEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AlertChannel
import com.example.data.model.ChangeReviewStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DetailedChangeType
import com.example.data.model.DoseStatus
import com.example.data.model.DoseUnit
import com.example.data.model.EmergencyMedicationItem
import com.example.data.model.EmergencySnapshot
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicationStatus
import com.example.data.model.Medicine
import com.example.data.model.MedicineForm
import com.example.data.model.PrescriptionStatus
import com.example.data.model.StockTransactionType
import com.example.data.model.UserRole
import com.example.data.model.toDomain
import com.example.data.model.toEntity
import com.example.data.model.toScheduleEntities
import com.example.data.reconciliation.MedicationReconciliationService
import com.example.data.reconciliation.ReconciliationItem
import com.example.data.reconciliation.ReconciliationResult
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.report.AdherenceReportData
import com.example.report.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineRepository(private val context: Context) : IMedicineRepository {
    private val db = AppDatabase.getInstance(context)
    val userAuthManager = UserAuthManager.getInstance(context)

    val currentSessionFlow: Flow<AuthSessionEntity?> = db.authSessionDao().getSessionFlow().distinctUntilChanged()

    val currentUserIdFlow: Flow<String?> = userAuthManager.currentUserId

    val currentUserAccount: Flow<UserAccountEntity?> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            db.userAccountDao().getUserByIdFlow(userId)
        }
    }

    val userProfile: Flow<UserProfileEntity?> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            db.userProfileDao().getUserProfile(userId)
        }
    }

    val caregivers: Flow<List<CaregiverEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.caregiverDao().getAllCaregivers(userId)
        }
    }

    val activeMedications: Flow<List<MedicationEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getActiveMedications(userId)
        }
    }

    val medicationsWithSchedules: Flow<List<MedicationWithSchedules>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getAllMedicationsWithSchedules(userId)
        }
    }

    val lowStockMedications: Flow<List<MedicationEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getLowStockMedications(userId)
        }
    }

    // --- IMedicineRepository Domain Flows ---

    override val allMedicinesFlow: Flow<List<Medicine>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getAllMedicationsWithSchedules(userId).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override val activeMedicinesFlow: Flow<List<Medicine>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getAllMedicationsWithSchedules(userId).map { list ->
                list.filter { it.medication.active }.map { it.toDomain() }
            }
        }
    }

    override val lowStockMedicinesFlow: Flow<List<Medicine>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.medicationDao().getLowStockMedications(userId).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override val medicationsWithSchedulesFlow: Flow<List<MedicationWithSchedules>> = medicationsWithSchedules

    override fun getMedicineById(id: Long): Flow<Medicine?> {
        return db.medicationDao().getMedicationByIdFlow(id).map { entity ->
            if (entity == null) null
            else {
                val schedules = db.medicationScheduleDao().getSchedulesForMedicationDirect(id)
                entity.toDomain(schedules)
            }
        }
    }

    override suspend fun getMedicineByIdDirect(id: Long): Medicine? = withContext(Dispatchers.IO) {
        val entity = db.medicationDao().getMedicationById(id) ?: return@withContext null
        val schedules = db.medicationScheduleDao().getSchedulesForMedicationDirect(id)
        entity.toDomain(schedules)
    }

    override fun searchMedicines(query: String): Flow<List<Medicine>> {
        return currentUserIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                db.medicationDao().searchMedications(userId, query.trim()).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }
    }

    // --- Medication Continuity & Passport Flows ---

    override val allPrescriptionsFlow: Flow<List<PrescriptionRecordEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.prescriptionRecordDao().getAllPrescriptions(userId)
    }

    override val allMedicationChangesFlow: Flow<List<MedicationChangeEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.medicationChangeDao().getAllChanges(userId)
    }

    override val allMedicationVersionsFlow: Flow<List<MedicationVersionEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.medicationVersionDao().getAllVersionsForUser(userId)
    }

    override fun getMedicationVersions(medicationId: Long): Flow<List<MedicationVersionEntity>> {
        return db.medicationVersionDao().getVersionsForMedication(medicationId)
    }

    override fun getMedicationChanges(medicationId: Long): Flow<List<MedicationChangeEntity>> {
        return db.medicationChangeDao().getChangesForMedication(medicationId)
    }

    override fun getChangesForPrescription(prescriptionId: Long): Flow<List<MedicationChangeEntity>> {
        return db.medicationChangeDao().getChangesForPrescription(prescriptionId)
    }

    // --- Emergency Medical ID & Emergency Snapshot Flows ---

    override val emergencyProfileFlow: Flow<EmergencyProfileEntity?> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(null)
        else db.emergencyProfileDao().getEmergencyProfile(userId)
    }

    override val emergencyConditionsFlow: Flow<List<EmergencyConditionEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.emergencyConditionDao().getConditions(userId)
    }

    override val emergencyAllergiesFlow: Flow<List<EmergencyAllergyEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.emergencyAllergyDao().getAllergies(userId)
    }

    override val emergencyContactsFlow: Flow<List<EmergencyContactEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.emergencyContactDao().getEmergencyContacts(userId)
    }

    override val emergencyAccessLogsFlow: Flow<List<EmergencyAccessLogEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) flowOf(emptyList())
        else db.emergencyAccessLogDao().getAccessLogs(userId)
    }

    override val emergencySnapshotFlow: Flow<EmergencySnapshot> = combine(
        currentUserAccount,
        userProfile,
        activeMedicinesFlow,
        emergencyProfileFlow,
        emergencyConditionsFlow,
        emergencyAllergiesFlow,
        emergencyContactsFlow
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val account = args[0] as? UserAccountEntity
        @Suppress("UNCHECKED_CAST")
        val profile = args[1] as? UserProfileEntity
        @Suppress("UNCHECKED_CAST")
        val meds = args[2] as? List<Medicine> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val eProfile = args[3] as? EmergencyProfileEntity
        @Suppress("UNCHECKED_CAST")
        val conditions = args[4] as? List<EmergencyConditionEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val allergies = args[5] as? List<EmergencyAllergyEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val contacts = args[6] as? List<EmergencyContactEntity> ?: emptyList()

        val patientName = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientName.isNotBlank() -> profile.managedPatientName
            account != null && account.name.isNotBlank() -> account.name
            profile != null && profile.name.isNotBlank() -> profile.name
            else -> "User"
        }
        val age = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientAge.isNotBlank() -> profile.managedPatientAge
            profile != null && profile.age.isNotBlank() -> profile.age
            else -> ""
        }
        val dob = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientDob.isNotBlank() -> profile.managedPatientDob
            profile != null && profile.dob.isNotBlank() -> profile.dob
            else -> ""
        }
        val gender = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientGender.isNotBlank() -> profile.managedPatientGender
            profile != null && profile.gender.isNotBlank() -> profile.gender
            else -> ""
        }
        val medItems = meds.filter { it.isActive }.map {
            EmergencyMedicationItem(
                id = it.id,
                name = it.name,
                genericName = it.genericName,
                strength = it.strength,
                form = it.dosage.form.displayName,
                route = it.dosage.route,
                instructions = it.dosage.instructions,
                frequency = it.schedule.frequency.displayName,
                doseAmount = it.dosage.amount,
                doseUnit = it.dosage.unit.symbol
            )
        }

        EmergencySnapshot(
            patientName = patientName,
            preferredName = eProfile?.preferredName ?: "",
            age = age,
            dob = dob,
            gender = gender,
            bloodGroup = eProfile?.bloodGroup ?: "Unknown",
            medicalConditions = conditions.map { it.name },
            allergies = allergies,
            currentMedications = medItems,
            emergencyContacts = contacts,
            primaryDoctorName = eProfile?.primaryDoctorName ?: "",
            primaryDoctorPhone = eProfile?.primaryDoctorPhone ?: "",
            hospitalClinicName = eProfile?.hospitalClinicName ?: "",
            importantNotes = eProfile?.importantNotes ?: "",
            organDonor = eProfile?.organDonor ?: false,
            emergencyIdentifier = eProfile?.emergencyIdentifier ?: "",
            qrEnabled = eProfile?.qrEnabled ?: false,
            isEnabled = eProfile?.enabled ?: true,
            lastUpdated = eProfile?.lastUpdatedAt ?: System.currentTimeMillis(),
            lastReviewedAt = eProfile?.lastReviewedAt ?: System.currentTimeMillis(),
            qrCreatedAt = eProfile?.qrCreatedAt,
            qrRevokedAt = eProfile?.qrRevokedAt,
            updatedBy = eProfile?.updatedBy ?: "Patient"
        )
    }



    val recentLogs: Flow<List<NotificationLogEntity>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.notificationLogDao().getAllLogs(userId)
        }
    }

    val prescriptionScans: Flow<List<PrescriptionWithCandidates>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.prescriptionScanDao().getAllScansWithCandidates(userId)
        }
    }

    suspend fun getActiveUserId(): String = withContext(Dispatchers.IO) {
        val session = db.authSessionDao().getSessionDirect()
        session?.currentUserId ?: "default_user"
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String = "",
        role: UserRole = UserRole.PATIENT
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val existing = db.userAccountDao().getUserByEmail(cleanEmail)
            if (existing != null) {
                return@withContext Result.failure(Exception("An account with email $cleanEmail already exists. Please log in."))
            }

            val userId = UUID.randomUUID().toString()
            val hashedPassword = hashPassword(password)
            val newAccount = UserAccountEntity(
                id = userId,
                email = cleanEmail,
                passwordHash = hashedPassword,
                name = name.trim(),
                phone = phone.trim(),
                role = role,
                onboardingCompleted = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            db.userAccountDao().insertOrUpdate(newAccount)

            // Set current session in DB and DataStore
            db.authSessionDao().setSession(AuthSessionEntity(id = 1, currentUserId = userId))
            userAuthManager.setAuthenticatedUser(
                userId = userId,
                email = cleanEmail,
                name = name.trim(),
                role = role,
                onboardingCompleted = false
            )

            // Initialize base user profile
            val initialProfile = UserProfileEntity(
                id = userId,
                name = name.trim(),
                email = cleanEmail,
                phone = phone.trim(),
                role = role,
                userEmail = cleanEmail,
                userPhone = phone.trim()
            )
            db.userProfileDao().insertOrUpdate(initialProfile)

            Result.success(newAccount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(
        identifier: String,
        password: String
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanIdentifier = identifier.trim()
            val user = db.userAccountDao().getUserByIdentifier(cleanIdentifier)
                ?: return@withContext Result.failure(Exception("No account found for '$cleanIdentifier'. Please create an account."))

            val hashedInput = hashPassword(password)
            if (user.passwordHash != hashedInput) {
                return@withContext Result.failure(Exception("Incorrect password. Please try again."))
            }

            // Update login time and set session in DB and DataStore
            db.userAccountDao().updateLastLogin(user.id)
            db.authSessionDao().setSession(AuthSessionEntity(id = 1, currentUserId = user.id))
            userAuthManager.setAuthenticatedUser(
                userId = user.id,
                email = user.email,
                name = user.name,
                role = user.role,
                onboardingCompleted = user.onboardingCompleted
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        db.authSessionDao().clearSession()
        userAuthManager.logout()
    }

    suspend fun markOnboardingComplete(userId: String, role: UserRole) = withContext(Dispatchers.IO) {
        db.userAccountDao().setOnboardingCompleted(userId, completed = true, role = role)
        userAuthManager.setOnboardingCompleted(completed = true, role = role)
    }

    override fun getTodayDoses(): Flow<List<DoseWithMedication>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1
        return currentUserIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                db.doseEventDao().getDosesBetween(userId, startOfDay, endOfDay)
            }
        }
    }

    override fun getDosesForDate(dateMillis: Long): Flow<List<DoseWithMedication>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1
        return currentUserIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                db.doseEventDao().getDosesBetween(userId, startOfDay, endOfDay)
            }
        }
    }

    override fun getRecentHistory(): Flow<List<DoseWithMedication>> {
        return currentUserIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                db.doseEventDao().getRecentDoseHistory(userId, 150)
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        val activeUserId = profile.id.ifBlank { getActiveUserId() }
        db.userProfileDao().insertOrUpdate(profile.copy(id = activeUserId))
    }

    suspend fun addCaregiver(caregiver: CaregiverEntity): Long = withContext(Dispatchers.IO) {
        val activeUserId = if (caregiver.userId.isBlank() || caregiver.userId == "default_user") getActiveUserId() else caregiver.userId
        db.caregiverDao().insertCaregiver(caregiver.copy(userId = activeUserId))
    }

    suspend fun updateCaregiver(caregiver: CaregiverEntity) = withContext(Dispatchers.IO) {
        db.caregiverDao().updateCaregiver(caregiver)
    }

    suspend fun deleteCaregiver(caregiver: CaregiverEntity) = withContext(Dispatchers.IO) {
        db.caregiverDao().deleteCaregiver(caregiver)
    }

    // =========================================================================
    // CRUD Operations (Domain Model: Medicine)
    // =========================================================================

    override suspend fun createMedicine(medicine: Medicine): Long = withContext(Dispatchers.IO) {
        val activeUserId = if (medicine.userId.isBlank() || medicine.userId == "default_user") getActiveUserId() else medicine.userId
        val entity = medicine.copy(userId = activeUserId).toEntity()
        val schedules = medicine.toScheduleEntities()
        addMedicationWithSchedules(entity, schedules)
    }

    override suspend fun updateMedicine(medicine: Medicine) = withContext(Dispatchers.IO) {
        val entity = medicine.toEntity()
        val schedules = medicine.toScheduleEntities()
        updateMedication(entity, schedules)
    }

    override suspend fun toggleMedicineActive(id: Long, active: Boolean) = withContext(Dispatchers.IO) {
        db.medicationDao().updateMedicationActive(id, active)
        val currentTime = System.currentTimeMillis()
        db.doseEventDao().deleteFutureScheduledDoses(id, currentTime)
        if (active) {
            generateUpcomingDosesForMedication(id, daysAhead = 7)
        }
        val med = db.medicationDao().getMedicationById(id)
        if (med != null) {
            touchEmergencyProfile(med.userId)
        }
    }

    override suspend fun deleteMedicine(medicine: Medicine) = withContext(Dispatchers.IO) {
        deleteMedication(medicine.toEntity())
    }

    override suspend fun deleteMedicineById(id: Long) = withContext(Dispatchers.IO) {
        val med = db.medicationDao().getMedicationById(id) ?: return@withContext
        deleteMedication(med)
    }

    // =========================================================================
    // CRUD Operations (Room Entities)
    // =========================================================================

    override suspend fun addMedicationWithSchedules(
        medication: MedicationEntity,
        schedules: List<MedicationScheduleEntity>
    ): Long = withContext(Dispatchers.IO) {
        val activeUserId = if (medication.userId.isBlank() || medication.userId == "default_user") getActiveUserId() else medication.userId
        val medWithUser = medication.copy(userId = activeUserId)
        val medId = db.medicationDao().insertMedication(medWithUser)
        val schedulesWithId = schedules.map { it.copy(medicationId = medId) }
        db.medicationScheduleDao().insertSchedules(schedulesWithId)

        // Record initial stock transaction
        db.stockTransactionDao().insertTransaction(
            StockTransactionEntity(
                medicationId = medId,
                quantityChange = medication.stockQuantity,
                balanceAfter = medication.stockQuantity,
                type = StockTransactionType.INITIAL_STOCK,
                note = "Initial stock recorded"
            )
        )

        // Generate upcoming doses for next 7 days
        generateUpcomingDosesForMedication(medId, daysAhead = 7)
        touchEmergencyProfile(activeUserId)
        medId
    }

    override suspend fun updateMedication(medication: MedicationEntity, schedules: List<MedicationScheduleEntity>) = withContext(Dispatchers.IO) {
        db.medicationDao().updateMedication(medication)
        if (schedules.isNotEmpty()) {
            db.medicationScheduleDao().deleteSchedulesForMedication(medication.id)
            val schedulesWithId = schedules.map { it.copy(medicationId = medication.id) }
            db.medicationScheduleDao().insertSchedules(schedulesWithId)
        }

        // Re-generate future doses
        val currentTime = System.currentTimeMillis()
        db.doseEventDao().deleteFutureScheduledDoses(medication.id, currentTime)
        if (medication.active) {
            generateUpcomingDosesForMedication(medication.id, daysAhead = 7)
        }
        touchEmergencyProfile(medication.userId)
    }

    override suspend fun deleteMedication(medication: MedicationEntity) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        db.doseEventDao().deleteFutureScheduledDoses(medication.id, currentTime)
        db.medicationDao().deleteMedication(medication)
        touchEmergencyProfile(medication.userId)
    }

    override suspend fun refillStock(medicationId: Long, additionalQuantity: Double) = withContext(Dispatchers.IO) {
        val med = db.medicationDao().getMedicationById(medicationId) ?: return@withContext
        val newStock = med.stockQuantity + additionalQuantity
        db.medicationDao().updateStock(medicationId, newStock)
        db.stockTransactionDao().insertTransaction(
            StockTransactionEntity(
                medicationId = medicationId,
                quantityChange = additionalQuantity,
                balanceAfter = newStock,
                type = StockTransactionType.REFILL,
                note = "Manual refill added"
            )
        )
    }

    override suspend fun markDoseTaken(doseId: Long): Boolean = withContext(Dispatchers.IO) {
        com.example.notification.DoseNotificationHandler.handleTakeNow(context, doseId)
    }

    override suspend fun markDoseSnoozed(doseId: Long, snoozeMinutes: Int): Boolean = withContext(Dispatchers.IO) {
        com.example.notification.DoseNotificationHandler.handleSnooze(context, doseId, snoozeMinutes)
    }

    override suspend fun markDoseSkipped(doseId: Long, skipReason: String?): Boolean = withContext(Dispatchers.IO) {
        com.example.notification.DoseNotificationHandler.handleSkip(context, doseId, skipReason)
    }

    override suspend fun rescheduleDose(doseId: Long, newTimeMillis: Long) = withContext(Dispatchers.IO) {
        val dose = db.doseEventDao().getDoseEventById(doseId) ?: return@withContext
        val updated = dose.copy(
            scheduledAt = newTimeMillis,
            status = DoseStatus.SCHEDULED,
            snoozedUntil = null
        )
        db.doseEventDao().updateDoseEvent(updated)
        com.example.notification.MedicationReminderScheduler.cancelReminder(context, doseId)
        com.example.notification.MedicationReminderScheduler.scheduleReminder(context, doseId, newTimeMillis)
    }

    suspend fun checkAndEscalateMissedDoses() = withContext(Dispatchers.IO) {
        com.example.notification.MissedDoseProcessor.checkAllPendingGracePeriods(context)
    }

    suspend fun triggerTestReminderNotification(doseId: Long) = withContext(Dispatchers.IO) {
        val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseId) ?: return@withContext
        val dose = doseWithMed.doseEvent
        val med = doseWithMed.medication
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(dose.scheduledAt))
        NotificationHelper.showMedicationReminder(
            context = context,
            doseEventId = dose.id,
            medicationName = med.name,
            strength = med.strength,
            doseAmount = med.doseAmount,
            doseUnit = med.doseUnit.symbol,
            instructions = med.instructions,
            scheduledTime = timeStr,
            snoozeDurationMinutes = 10
        )
    }

    suspend fun triggerTestMissedAlert(doseId: Long) = withContext(Dispatchers.IO) {
        com.example.notification.MissedDoseProcessor.processGracePeriodExpired(context, doseId)
    }

    override suspend fun generateUpcomingDosesForMedication(medicationId: Long, daysAhead: Int) = withContext(Dispatchers.IO) {
        val med = db.medicationDao().getMedicationById(medicationId) ?: return@withContext
        if (!med.active) return@withContext

        val schedules = db.medicationScheduleDao().getSchedulesForMedicationDirect(medicationId)
        val now = Calendar.getInstance()

        for (dayOffset in 0..daysAhead) {
            val targetCal = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val dayOfWeekStr = when (targetCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "SUN"
                Calendar.MONDAY -> "MON"
                Calendar.TUESDAY -> "TUE"
                Calendar.WEDNESDAY -> "WED"
                Calendar.THURSDAY -> "THU"
                Calendar.FRIDAY -> "FRI"
                Calendar.SATURDAY -> "SAT"
                else -> ""
            }

            for (sched in schedules) {
                if (!sched.enabled) continue
                if (!sched.daysOfWeek.contains(dayOfWeekStr)) continue

                val timeParts = sched.timeString.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                val doseCal = (targetCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val scheduledTimeMs = doseCal.timeInMillis
                val existing = db.doseEventDao().getDoseEventByTime(medicationId, scheduledTimeMs)

                if (existing == null) {
                    val doseId = db.doseEventDao().insertDoseEvent(
                        DoseEventEntity(
                            userId = med.userId,
                            medicationId = medicationId,
                            scheduleId = sched.id,
                            scheduledAt = scheduledTimeMs,
                            status = DoseStatus.SCHEDULED
                        )
                    )
                    // If in the future, schedule reminder alarm
                    if (scheduledTimeMs > System.currentTimeMillis()) {
                        com.example.notification.MedicationReminderScheduler.scheduleReminder(context, doseId, scheduledTimeMs)
                    }
                }
            }
        }
    }

    suspend fun savePrescriptionScan(
        imageUri: String,
        rawText: String,
        candidates: List<OcrCandidateEntity>,
        userId: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val targetUserId = userId ?: getActiveUserId()
        val scanId = db.prescriptionScanDao().insertScan(
            PrescriptionScanEntity(
                userId = targetUserId,
                imageUri = imageUri,
                rawOcrText = rawText,
                processingStatus = "PROCESSED"
            )
        )
        val candidatesWithScanId = candidates.map { it.copy(scanId = scanId) }
        db.prescriptionScanDao().insertCandidates(candidatesWithScanId)
        scanId
    }

    suspend fun confirmOcrCandidateAsMedication(candidate: OcrCandidateEntity): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val med = MedicationEntity(
            userId = currentUserId,
            name = candidate.medicineName,
            strength = candidate.strength,
            doseAmount = candidate.dose,
            doseUnit = candidate.doseUnit,
            form = if (candidate.doseUnit == DoseUnit.CAPSULE) MedicineForm.CAPSULE else MedicineForm.TABLET,
            route = candidate.route,
            instructions = candidate.instructions,
            source = "OCR",
            stockQuantity = 30.0,
            lowStockThreshold = 5.0
        )

        val scheduleTimes = when (candidate.frequency) {
            FrequencyType.THREE_TIMES_DAILY -> listOf("08:00", "14:00", "20:00")
            FrequencyType.TWICE_DAILY -> listOf("08:00", "20:00")
            FrequencyType.DAILY -> listOf("08:00")
            else -> listOf("09:00")
        }

        val schedules = scheduleTimes.map { timeStr ->
            MedicationScheduleEntity(
                medicationId = 0,
                frequencyType = candidate.frequency,
                timeString = timeStr,
                doseAmount = candidate.dose
            )
        }

        addMedicationWithSchedules(med, schedules)
    }

    override suspend fun savePrescriptionRecord(
        prescription: PrescriptionRecordEntity
    ): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val toSave = prescription.copy(userId = currentUserId)
        if (toSave.id > 0) {
            db.prescriptionRecordDao().updatePrescription(toSave)
            toSave.id
        } else {
            db.prescriptionRecordDao().insertPrescription(toSave)
        }
    }

    override suspend fun comparePrescriptionWithCurrentPlan(
        candidates: List<OcrCandidateEntity>,
        doctorName: String,
        clinicName: String,
        prescriptionDate: Long
    ): ReconciliationResult = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val activeMedications = db.medicationDao().getActiveMedicationsWithSchedulesDirect(currentUserId)
        
        MedicationReconciliationService.compare(
            activeMedications = activeMedications,
            incomingCandidates = candidates,
            doctorName = doctorName,
            clinicName = clinicName,
            prescriptionDate = prescriptionDate
        )
    }

    override suspend fun confirmMedicationReconciliation(
        reconciliationResult: ReconciliationResult,
        reviewedBy: String
    ): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val now = System.currentTimeMillis()

        // 1. Create or Update Prescription Record
        val prescriptionRecord = PrescriptionRecordEntity(
            id = reconciliationResult.prescriptionId,
            userId = currentUserId,
            doctorName = reconciliationResult.doctorName,
            clinicName = reconciliationResult.clinicName,
            prescriptionDate = reconciliationResult.prescriptionDate,
            source = "RECONCILIATION",
            status = PrescriptionStatus.CONFIRMED,
            capturedAt = reconciliationResult.createdAt,
            confirmedAt = now,
            notes = "Reconciled ${reconciliationResult.items.size} medicines"
        )
        val savedPrescriptionId = if (prescriptionRecord.id > 0) {
            db.prescriptionRecordDao().updatePrescription(prescriptionRecord)
            prescriptionRecord.id
        } else {
            db.prescriptionRecordDao().insertPrescription(prescriptionRecord)
        }

        // 2. Process each Reconciliation Item
        for (item in reconciliationResult.items) {
            when (item.category) {
                MedicationChangeType.UNCHANGED -> {
                    // Record unchanged change log
                    val medId = item.existingMedication?.id
                    db.medicationChangeDao().insertChange(
                        MedicationChangeEntity(
                            userId = currentUserId,
                            prescriptionId = savedPrescriptionId,
                            medicationId = medId,
                            medicineName = item.medicineName,
                            changeType = MedicationChangeType.UNCHANGED,
                            fieldChanged = "None",
                            previousValue = item.existingMedication?.strength ?: "",
                            newValue = item.existingMedication?.strength ?: "",
                            matchConfidence = item.confidence,
                            reviewStatus = ChangeReviewStatus.ACCEPTED,
                            reviewedBy = reviewedBy,
                            reviewedAt = now
                        )
                    )
                }

                MedicationChangeType.NEW -> {
                    if (item.reviewDecision != ChangeReviewStatus.REJECTED) {
                        val newMed = MedicationEntity(
                            userId = currentUserId,
                            name = item.displayMedicineName,
                            genericName = item.genericName,
                            brandName = item.brandName,
                            strength = item.displayStrength,
                            doseAmount = item.displayDoseAmount,
                            doseUnit = item.displayDoseUnit,
                            form = item.displayForm,
                            route = item.proposedCandidate?.route ?: "Oral",
                            instructions = item.displayInstructions,
                            source = "PRESCRIPTION_RECONCILIATION",
                            active = true,
                            stockQuantity = 30.0,
                            lowStockThreshold = 5.0,
                            createdAt = now,
                            updatedAt = now
                        )
                        val newMedId = db.medicationDao().insertMedication(newMed)

                        // Create schedule
                        val scheduleTimes = when (item.displayFrequency) {
                            FrequencyType.THREE_TIMES_DAILY -> listOf("08:00", "14:00", "20:00")
                            FrequencyType.TWICE_DAILY -> listOf("08:00", "20:00")
                            FrequencyType.DAILY -> listOf("08:00")
                            else -> listOf("09:00")
                        }
                        val schedules = scheduleTimes.map { timeStr ->
                            MedicationScheduleEntity(
                                medicationId = newMedId,
                                userId = currentUserId,
                                frequencyType = item.displayFrequency,
                                timeString = timeStr,
                                doseAmount = item.displayDoseAmount
                            )
                        }
                        db.medicationScheduleDao().insertSchedules(schedules)

                        // Create Version 1 record
                        db.medicationVersionDao().insertVersion(
                            MedicationVersionEntity(
                                medicationId = newMedId,
                                userId = currentUserId,
                                versionNumber = 1,
                                prescriptionId = savedPrescriptionId,
                                name = item.displayMedicineName,
                                genericName = item.genericName,
                                brandName = item.brandName,
                                strength = item.displayStrength,
                                doseAmount = item.displayDoseAmount,
                                doseUnit = item.displayDoseUnit,
                                form = item.displayForm,
                                frequencyType = item.displayFrequency,
                                instructions = item.displayInstructions,
                                startDate = now,
                                status = MedicationStatus.ACTIVE,
                                changeReason = "Initial prescription version"
                            )
                        )

                        // Generate future intake schedule
                        generateUpcomingDosesForMedication(newMedId, 7)

                        // Record change log
                        db.medicationChangeDao().insertChange(
                            MedicationChangeEntity(
                                userId = currentUserId,
                                prescriptionId = savedPrescriptionId,
                                medicationId = newMedId,
                                medicineName = item.displayMedicineName,
                                changeType = MedicationChangeType.NEW,
                                fieldChanged = "New Medicine Added",
                                previousValue = "-",
                                newValue = "${item.displayStrength}, ${item.displayFrequency.displayName}",
                                matchConfidence = item.confidence,
                                reviewStatus = item.reviewDecision,
                                reviewedBy = reviewedBy,
                                reviewedAt = now
                            )
                        )
                    } else {
                        db.medicationChangeDao().insertChange(
                            MedicationChangeEntity(
                                userId = currentUserId,
                                prescriptionId = savedPrescriptionId,
                                medicineName = item.medicineName,
                                changeType = MedicationChangeType.NEW,
                                fieldChanged = "New Medicine",
                                previousValue = "-",
                                newValue = item.displayStrength,
                                matchConfidence = item.confidence,
                                reviewStatus = ChangeReviewStatus.REJECTED,
                                reviewedBy = reviewedBy,
                                reviewedAt = now,
                                notes = "Rejected by user during reconciliation"
                            )
                        )
                    }
                }

                MedicationChangeType.CHANGED, MedicationChangeType.UNCERTAIN, MedicationChangeType.POSSIBLE_DUPLICATE -> {
                    val existing = item.existingMedication
                    if (existing != null) {
                        if (item.reviewDecision == ChangeReviewStatus.ACCEPTED || item.reviewDecision == ChangeReviewStatus.EDITED) {
                            val currentVer = db.medicationVersionDao().getLatestVersionNumber(existing.id) ?: 1
                            val newVerNumber = currentVer + 1

                            // 1. Create new MedicationVersion
                            db.medicationVersionDao().insertVersion(
                                MedicationVersionEntity(
                                    medicationId = existing.id,
                                    userId = currentUserId,
                                    versionNumber = newVerNumber,
                                    prescriptionId = savedPrescriptionId,
                                    name = item.displayMedicineName,
                                    genericName = existing.genericName,
                                    brandName = existing.brandName,
                                    strength = item.displayStrength,
                                    doseAmount = item.displayDoseAmount,
                                    doseUnit = item.displayDoseUnit,
                                    form = item.displayForm,
                                    frequencyType = item.displayFrequency,
                                    instructions = item.displayInstructions,
                                    startDate = now,
                                    status = MedicationStatus.ACTIVE,
                                    changeReason = "Updated via prescription reconciliation"
                                )
                            )

                            // 2. Update current MedicationEntity
                            val updatedMed = existing.copy(
                                name = item.displayMedicineName,
                                strength = item.displayStrength,
                                doseAmount = item.displayDoseAmount,
                                doseUnit = item.displayDoseUnit,
                                form = item.displayForm,
                                instructions = item.displayInstructions,
                                updatedAt = now
                            )
                            db.medicationDao().updateMedication(updatedMed)

                            // 3. Recalculate schedules & Future reminders
                            val scheduleTimes = when (item.displayFrequency) {
                                FrequencyType.THREE_TIMES_DAILY -> listOf("08:00", "14:00", "20:00")
                                FrequencyType.TWICE_DAILY -> listOf("08:00", "20:00")
                                FrequencyType.DAILY -> listOf("08:00")
                                else -> listOf("09:00")
                            }
                            db.medicationScheduleDao().deleteSchedulesForMedication(existing.id)
                            val schedules = scheduleTimes.map { timeStr ->
                                MedicationScheduleEntity(
                                    medicationId = existing.id,
                                    userId = currentUserId,
                                    frequencyType = item.displayFrequency,
                                    timeString = timeStr,
                                    doseAmount = item.displayDoseAmount
                                )
                            }
                            db.medicationScheduleDao().insertSchedules(schedules)

                            // Delete obsolete future scheduled doses (Preserve past doses!)
                            db.doseEventDao().deleteFutureScheduledDoses(existing.id, now)
                            // Generate new future doses
                            generateUpcomingDosesForMedication(existing.id, 7)

                            // 4. Record change log
                            val detailsStr = item.detailedChanges.joinToString(",") { it.name }
                            val prevSummary = "${existing.strength}, ${existing.doseAmount} ${existing.doseUnit.symbol}, ${existing.instructions}"
                            val newSummary = "${item.displayStrength}, ${item.displayDoseAmount} ${item.displayDoseUnit.symbol}, ${item.displayInstructions}"

                            db.medicationChangeDao().insertChange(
                                MedicationChangeEntity(
                                    userId = currentUserId,
                                    prescriptionId = savedPrescriptionId,
                                    medicationId = existing.id,
                                    medicineName = item.displayMedicineName,
                                    changeType = MedicationChangeType.CHANGED,
                                    detailedChanges = detailsStr,
                                    fieldChanged = if (detailsStr.isNotBlank()) detailsStr else "Dosage / Plan Changed",
                                    previousValue = prevSummary,
                                    newValue = newSummary,
                                    matchConfidence = item.confidence,
                                    reviewStatus = item.reviewDecision,
                                    reviewedBy = reviewedBy,
                                    reviewedAt = now
                                )
                            )
                        } else {
                            // User selected Kept / Defer / Reject
                            db.medicationChangeDao().insertChange(
                                MedicationChangeEntity(
                                    userId = currentUserId,
                                    prescriptionId = savedPrescriptionId,
                                    medicationId = existing.id,
                                    medicineName = item.medicineName,
                                    changeType = MedicationChangeType.CHANGED,
                                    fieldChanged = "Dosage Change Proposed",
                                    previousValue = existing.strength,
                                    newValue = item.displayStrength,
                                    matchConfidence = item.confidence,
                                    reviewStatus = item.reviewDecision,
                                    reviewedBy = reviewedBy,
                                    reviewedAt = now,
                                    notes = "User chose: ${item.reviewDecision.displayName}"
                                )
                            )
                        }
                    }
                }

                MedicationChangeType.NOT_FOUND -> {
                    val existing = item.existingMedication
                    if (existing != null) {
                        if (item.markDiscontinued) {
                            // Explicit user choice to discontinue
                            db.medicationDao().updateMedicationActive(existing.id, false)
                            db.medicationVersionDao().insertVersion(
                                MedicationVersionEntity(
                                    medicationId = existing.id,
                                    userId = currentUserId,
                                    versionNumber = (db.medicationVersionDao().getLatestVersionNumber(existing.id) ?: 1) + 1,
                                    prescriptionId = savedPrescriptionId,
                                    name = existing.name,
                                    strength = existing.strength,
                                    doseAmount = existing.doseAmount,
                                    doseUnit = existing.doseUnit,
                                    form = existing.form,
                                    instructions = existing.instructions,
                                    startDate = existing.startDate,
                                    endDate = now,
                                    status = MedicationStatus.DISCONTINUED,
                                    changeReason = "Discontinued by user after new prescription review"
                                )
                            )
                            db.doseEventDao().deleteFutureScheduledDoses(existing.id, now)

                            db.medicationChangeDao().insertChange(
                                MedicationChangeEntity(
                                    userId = currentUserId,
                                    prescriptionId = savedPrescriptionId,
                                    medicationId = existing.id,
                                    medicineName = existing.name,
                                    changeType = MedicationChangeType.NOT_FOUND,
                                    fieldChanged = "Status",
                                    previousValue = "Active",
                                    newValue = "Discontinued",
                                    matchConfidence = ConfidenceLevel.HIGH,
                                    reviewStatus = ChangeReviewStatus.ACCEPTED,
                                    reviewedBy = reviewedBy,
                                    reviewedAt = now,
                                    notes = "Marked discontinued by user"
                                )
                            )
                        } else {
                            // CRITICAL RULE: Absence is not discontinuation.
                            // Kept active or deferred
                            db.medicationChangeDao().insertChange(
                                MedicationChangeEntity(
                                    userId = currentUserId,
                                    prescriptionId = savedPrescriptionId,
                                    medicationId = existing.id,
                                    medicineName = existing.name,
                                    changeType = MedicationChangeType.NOT_FOUND,
                                    fieldChanged = "Not found in new prescription",
                                    previousValue = "Active (${existing.strength})",
                                    newValue = "Remains Active",
                                    matchConfidence = ConfidenceLevel.HIGH,
                                    reviewStatus = item.reviewDecision,
                                    reviewedBy = reviewedBy,
                                    reviewedAt = now,
                                    notes = if (item.reviewDecision == ChangeReviewStatus.DEFERRED) "User chose: Review Later" else "User chose: Keep Active"
                                )
                            )
                        }
                    }
                }
            }
        }

        touchEmergencyProfile(currentUserId)
        savedPrescriptionId
    }

    suspend fun generateMedicationPassportPdf(): File = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val profile = db.userProfileDao().getUserProfileDirect(userId) ?: UserProfileEntity(name = "User")
        val activeMeds = db.medicationDao().getActiveMedicationsDirect(userId)
        val allMeds = db.medicationDao().getAllMedicationsDirect(userId)
        val versions = db.medicationVersionDao().getAllVersionsForUserDirect(userId)
        val changes = db.medicationChangeDao().getAllChanges(userId).firstOrNull() ?: emptyList()
        val prescriptions = db.prescriptionRecordDao().getAllPrescriptions(userId).firstOrNull() ?: emptyList()

        // Calculate 30-day adherence
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (30 * 24 * 60 * 60 * 1000L)
        val doses = db.doseEventDao().getDosesBetweenDirect(userId, startTime, currentTime)
        val totalTaken = doses.count { it.doseEvent.status == DoseStatus.TAKEN }
        val totalMissed = doses.count { it.doseEvent.status == DoseStatus.MISSED }
        val eligible = totalTaken + totalMissed
        val adherencePct = if (eligible > 0) (totalTaken.toDouble() / eligible) * 100.0 else 100.0

        com.example.report.MedicationPassportPdfGenerator.generatePassportPdf(
            context = context,
            userProfile = profile,
            activeMedications = activeMeds,
            allMedications = allMeds,
            versions = versions,
            changes = changes,
            prescriptions = prescriptions,
            adherencePercentage = adherencePct,
            totalTaken = totalTaken,
            totalMissed = totalMissed
        )
    }

    suspend fun generateAdherenceReportPdf(days: Int = 30): File = withContext(Dispatchers.IO) {

        val userId = getActiveUserId()
        val profile = db.userProfileDao().getUserProfileDirect(userId) ?: UserProfileEntity(name = "User")
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (days * 24 * 60 * 60 * 1000L)

        val doses = db.doseEventDao().getDosesBetweenDirect(userId, startTime, currentTime)
        val medications = db.medicationDao().getActiveMedicationsDirect(userId)

        val totalScheduled = doses.size
        val totalTaken = doses.count { it.doseEvent.status == DoseStatus.TAKEN }
        val totalMissed = doses.count { it.doseEvent.status == DoseStatus.MISSED }
        val totalSkipped = doses.count { it.doseEvent.status == DoseStatus.SKIPPED }
        val totalSnoozed = doses.count { it.doseEvent.status == DoseStatus.SNOOZED }

        val eligibleDoses = totalTaken + totalMissed + totalSkipped
        val adherencePct = if (eligibleDoses > 0) {
            (totalTaken.toDouble() / eligibleDoses) * 100.0
        } else {
            100.0
        }

        val reportData = AdherenceReportData(
            userProfile = profile,
            startDate = startTime,
            endDate = currentTime,
            totalScheduled = totalScheduled,
            totalTaken = totalTaken,
            totalMissed = totalMissed,
            totalSkipped = totalSkipped,
            totalSnoozed = totalSnoozed,
            adherencePercentage = adherencePct,
            medications = medications,
            doseLogs = doses.reversed()
        )

        PdfReportGenerator.generateAdherencePdf(context, reportData)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // No automatic demo data seeding to prevent polluting authenticated user sessions
    }

    // --- Emergency Medical ID Implementation Methods ---

    private suspend fun touchEmergencyProfile(userId: String) {
        val profile = db.emergencyProfileDao().getEmergencyProfileDirect(userId)
        if (profile != null) {
            db.emergencyProfileDao().updateEmergencyProfile(
                profile.copy(lastUpdatedAt = System.currentTimeMillis())
            )
        }
    }

    private suspend fun getOrCreateEmergencyProfile(userId: String): EmergencyProfileEntity {
        var profile = db.emergencyProfileDao().getEmergencyProfileDirect(userId)
        if (profile == null) {
            val newProfile = EmergencyProfileEntity(
                userId = userId,
                emergencyIdentifier = UUID.randomUUID().toString(),
                lastReviewedAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            val id = db.emergencyProfileDao().insertEmergencyProfile(newProfile)
            profile = newProfile.copy(id = id)
        }
        return profile
    }

    override suspend fun getEmergencySnapshotDirect(userId: String?): EmergencySnapshot = withContext(Dispatchers.IO) {
        val targetUserId = userId ?: getActiveUserId()
        val account = db.userAccountDao().getUserById(targetUserId)
        val profile = db.userProfileDao().getUserProfileDirect(targetUserId)
        val eProfile = getOrCreateEmergencyProfile(targetUserId)
        val meds = db.medicationDao().getActiveMedicationsWithSchedulesDirect(targetUserId).map { it.toDomain() }
        val conditions = db.emergencyConditionDao().getConditionsDirect(targetUserId)
        val allergies = db.emergencyAllergyDao().getAllergiesDirect(targetUserId)
        val contacts = db.emergencyContactDao().getEmergencyContactsDirect(targetUserId)

        val patientName = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientName.isNotBlank() -> profile.managedPatientName
            account != null && account.name.isNotBlank() -> account.name
            profile != null && profile.name.isNotBlank() -> profile.name
            else -> "User"
        }
        val age = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientAge.isNotBlank() -> profile.managedPatientAge
            profile != null && profile.age.isNotBlank() -> profile.age
            else -> ""
        }
        val dob = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientDob.isNotBlank() -> profile.managedPatientDob
            profile != null && profile.dob.isNotBlank() -> profile.dob
            else -> ""
        }
        val gender = when {
            profile != null && profile.role == UserRole.CAREGIVER && profile.managedPatientGender.isNotBlank() -> profile.managedPatientGender
            profile != null && profile.gender.isNotBlank() -> profile.gender
            else -> ""
        }

        val medItems = meds.filter { it.isActive }.map {
            EmergencyMedicationItem(
                id = it.id,
                name = it.name,
                genericName = it.genericName,
                strength = it.strength,
                form = it.dosage.form.displayName,
                route = it.dosage.route,
                instructions = it.dosage.instructions,
                frequency = it.schedule.frequency.displayName,
                doseAmount = it.dosage.amount,
                doseUnit = it.dosage.unit.symbol
            )
        }

        EmergencySnapshot(
            patientName = patientName,
            preferredName = eProfile.preferredName,
            age = age,
            dob = dob,
            gender = gender,
            bloodGroup = eProfile.bloodGroup,
            medicalConditions = conditions.map { it.name },
            allergies = allergies,
            currentMedications = medItems,
            emergencyContacts = contacts,
            primaryDoctorName = eProfile.primaryDoctorName,
            primaryDoctorPhone = eProfile.primaryDoctorPhone,
            hospitalClinicName = eProfile.hospitalClinicName,
            importantNotes = eProfile.importantNotes,
            organDonor = eProfile.organDonor,
            emergencyIdentifier = eProfile.emergencyIdentifier,
            qrEnabled = eProfile.qrEnabled,
            isEnabled = eProfile.enabled,
            lastUpdated = eProfile.lastUpdatedAt,
            lastReviewedAt = eProfile.lastReviewedAt,
            qrCreatedAt = eProfile.qrCreatedAt,
            qrRevokedAt = eProfile.qrRevokedAt,
            updatedBy = eProfile.updatedBy
        )
    }

    override suspend fun getEmergencySnapshotByIdentifier(identifier: String): EmergencySnapshot? = withContext(Dispatchers.IO) {
        val eProfile = db.emergencyProfileDao().getEmergencyProfileByIdentifier(identifier) ?: return@withContext null
        if (!eProfile.enabled || !eProfile.qrEnabled || eProfile.qrRevokedAt != null) {
            return@withContext null
        }
        // Log access
        db.emergencyAccessLogDao().insertAccessLog(
            EmergencyAccessLogEntity(
                userId = eProfile.userId,
                emergencyIdentifier = identifier,
                accessedAt = System.currentTimeMillis(),
                accessType = "QR_SCAN",
                success = true,
                ipOrDeviceHint = "Emergency Web/QR Scan",
                notes = "Emergency ID accessed via QR"
            )
        )
        getEmergencySnapshotDirect(eProfile.userId)
    }

    override suspend fun updateEmergencyProfile(profile: EmergencyProfileEntity): Unit = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val toSave = profile.copy(
            userId = currentUserId,
            lastUpdatedAt = System.currentTimeMillis()
        )
        if (toSave.id > 0) {
            db.emergencyProfileDao().updateEmergencyProfile(toSave)
        } else {
            db.emergencyProfileDao().insertEmergencyProfile(toSave)
        }
        Unit
    }

    override suspend fun markEmergencyReviewed() = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        getOrCreateEmergencyProfile(currentUserId)
        db.emergencyProfileDao().markReviewed(currentUserId)
    }

    override suspend fun toggleEmergencyQr(enabled: Boolean) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val profile = getOrCreateEmergencyProfile(currentUserId)
        val revokedAt = if (!enabled) System.currentTimeMillis() else null
        db.emergencyProfileDao().updateQrStatus(currentUserId, enabled, revokedAt)
        if (enabled && profile.qrCreatedAt == null) {
            db.emergencyProfileDao().reIssueEmergencyQr(currentUserId, profile.emergencyIdentifier)
        }
    }

    override suspend fun revokeAndReissueEmergencyQr(): String = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val newIdentifier = UUID.randomUUID().toString()
        getOrCreateEmergencyProfile(currentUserId)
        db.emergencyProfileDao().reIssueEmergencyQr(currentUserId, newIdentifier)
        newIdentifier
    }

    override suspend fun addEmergencyCondition(name: String, notes: String, diagnosedYear: String): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val id = db.emergencyConditionDao().insertCondition(
            EmergencyConditionEntity(
                userId = currentUserId,
                name = name.trim(),
                notes = notes.trim(),
                diagnosedYear = diagnosedYear.trim()
            )
        )
        touchEmergencyProfile(currentUserId)
        id
    }

    override suspend fun updateEmergencyCondition(condition: EmergencyConditionEntity) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyConditionDao().updateCondition(condition.copy(userId = currentUserId))
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun deleteEmergencyCondition(id: Long) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyConditionDao().deleteConditionById(id)
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun addEmergencyAllergy(allergen: String, reaction: String, severity: String): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val id = db.emergencyAllergyDao().insertAllergy(
            EmergencyAllergyEntity(
                userId = currentUserId,
                allergen = allergen.trim(),
                reaction = reaction.trim(),
                severity = severity
            )
        )
        touchEmergencyProfile(currentUserId)
        id
    }

    override suspend fun updateEmergencyAllergy(allergy: EmergencyAllergyEntity) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyAllergyDao().updateAllergy(allergy.copy(userId = currentUserId))
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun deleteEmergencyAllergy(id: Long) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyAllergyDao().deleteAllergyById(id)
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun addEmergencyContact(
        name: String,
        relationship: String,
        phone: String,
        email: String,
        priority: Int,
        isPrimary: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        val id = db.emergencyContactDao().insertContact(
            EmergencyContactEntity(
                userId = currentUserId,
                name = name.trim(),
                relationship = relationship.trim(),
                phone = phone.trim(),
                email = email.trim(),
                priority = priority,
                isPrimary = isPrimary,
                enabled = true
            )
        )
        touchEmergencyProfile(currentUserId)
        id
    }

    override suspend fun updateEmergencyContact(contact: EmergencyContactEntity) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyContactDao().updateContact(contact.copy(userId = currentUserId))
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun deleteEmergencyContact(id: Long) = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyContactDao().deleteContactById(id)
        touchEmergencyProfile(currentUserId)
    }

    override suspend fun logEmergencyAccess(emergencyIdentifier: String, accessType: String, notes: String): Long = withContext(Dispatchers.IO) {
        val currentUserId = getActiveUserId()
        db.emergencyAccessLogDao().insertAccessLog(
            EmergencyAccessLogEntity(
                userId = currentUserId,
                emergencyIdentifier = emergencyIdentifier,
                accessedAt = System.currentTimeMillis(),
                accessType = accessType,
                success = true,
                notes = notes
            )
        )
    }
}
