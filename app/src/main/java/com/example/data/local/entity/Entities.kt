package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AlertChannel
import com.example.data.model.ChangeReviewStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DetailedChangeType
import com.example.data.model.DoseStatus
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicationStatus
import com.example.data.model.MedicineForm
import com.example.data.model.PrescriptionStatus
import com.example.data.model.StockTransactionType
import com.example.data.model.UserRole

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val name: String,
    val phone: String = "",
    val role: UserRole = UserRole.PATIENT,
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "auth_session")
data class AuthSessionEntity(
    @PrimaryKey val id: Int = 1,
    val currentUserId: String? = null,
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default_user",
    val name: String = "My Health",
    val email: String = "",
    val phone: String = "",
    val age: String = "",
    val gender: String = "Male",
    val dob: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val notes: String = "",
    val role: UserRole = UserRole.PATIENT,
    val managedPatientName: String = "",
    val managedPatientAge: String = "",
    val managedPatientGender: String = "",
    val managedPatientDob: String = "",
    val managedPatientPhone: String = "",
    val managedPatientNotes: String = "",
    val managedPatientRelationship: String = "",
    val remindersEnabled: Boolean = true,
    val missedDoseAlertsEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val gracePeriodMinutes: Int = 60,
    val trustedContactAlertsEnabled: Boolean = true,
    val caregiverMissedDoseAlertsEnabled: Boolean = true,
    val repeatedMissedAlertsEnabled: Boolean = true,
    val lowStockAlertsEnabled: Boolean = true,
    val dailySummaryAlertsEnabled: Boolean = false,
    val escalationEnabled: Boolean = true,
    val emailAlertsEnabled: Boolean = false,
    val whatsappAlertsEnabled: Boolean = false,
    val userEmail: String = "",
    val userPhone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "caregivers",
    indices = [Index("userId")]
)
data class CaregiverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val name: String,
    val relationship: String,
    val phone: String = "",
    val email: String = "",
    val whatsappNumber: String = "",
    val preferredChannel: AlertChannel = AlertChannel.LOCAL_NOTIFICATION,
    val alertMissedDose: Boolean = true,
    val alertLowStock: Boolean = true,
    val alertDailySummary: Boolean = false,
    val alertWeeklySummary: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medications",
    indices = [Index("userId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val name: String,
    val genericName: String = "",
    val brandName: String = "",
    val strength: String = "", // e.g. "500 mg"
    val doseAmount: Double = 1.0,
    val doseUnit: DoseUnit = DoseUnit.TABLET,
    val form: MedicineForm = MedicineForm.TABLET,
    val route: String = "Oral",
    val instructions: String = "After food", // e.g. "After food", "Before food"
    val notes: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val stockQuantity: Double = 30.0,
    val lowStockThreshold: Double = 5.0,
    val active: Boolean = true,
    val source: String = "MANUAL", // "MANUAL" or "OCR"
    val colorHex: String = "#006874",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val userId: String = "default_user",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val timeString: String = "08:00", // HH:mm format
    val daysOfWeek: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val doseAmount: Double = 1.0,
    val enabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10,
    val gracePeriodMinutes: Int = 60
)

@Entity(
    tableName = "dose_events",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId"), Index("scheduledAt"), Index("status"), Index("userId")]
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val medicationId: Long,
    val scheduleId: Long = 0,
    val scheduledAt: Long, // Epoch timestamp in ms
    val reminderSentAt: Long? = null,
    val status: DoseStatus = DoseStatus.SCHEDULED,
    val takenAt: Long? = null,
    val skippedAt: Long? = null,
    val snoozedUntil: Long? = null,
    val missedAt: Long? = null,
    val skipReason: String? = null,
    val source: String = "AUTO",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prescription_scans",
    indices = [Index("userId")]
)
data class PrescriptionScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val capturedAt: Long = System.currentTimeMillis(),
    val imageUri: String = "",
    val rawOcrText: String = "",
    val processingStatus: String = "PROCESSED"
)

@Entity(
    tableName = "ocr_candidates",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scanId")]
)
data class OcrCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val medicineName: String = "",
    val strength: String = "",
    val dose: Double = 1.0,
    val doseUnit: DoseUnit = DoseUnit.TABLET,
    val frequency: FrequencyType = FrequencyType.DAILY,
    val route: String = "Oral",
    val instructions: String = "After food",
    val duration: String = "7 days",
    val confidenceName: ConfidenceLevel = ConfidenceLevel.HIGH,
    val confidenceStrength: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val confidenceFreq: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val confidenceDuration: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val confirmed: Boolean = false
)

@Entity(
    tableName = "notification_logs",
    indices = [Index("userId"), Index("doseEventId"), Index("sentAt")]
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val doseEventId: Long,
    val channel: AlertChannel = AlertChannel.LOCAL_NOTIFICATION,
    val recipient: String = "",
    val sentAt: Long = System.currentTimeMillis(),
    val deliveryStatus: String = "DELIVERED",
    val messageText: String = "",
    val errorMessage: String? = null
)

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId"), Index("createdAt")]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val quantityChange: Double, // Negative for consumption, positive for refill
    val balanceAfter: Double,
    val type: StockTransactionType = StockTransactionType.DOSE_TAKEN,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceDoseEventId: Long? = null,
    val note: String = ""
)

@Entity(
    tableName = "prescription_records",
    indices = [Index("userId"), Index("prescriptionDate")]
)
data class PrescriptionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val doctorName: String = "",
    val clinicName: String = "",
    val prescriptionDate: Long = System.currentTimeMillis(),
    val source: String = "SCAN", // "SCAN" or "MANUAL"
    val imageUri: String = "",
    val rawOcrText: String = "",
    val status: PrescriptionStatus = PrescriptionStatus.CONFIRMED,
    val version: Int = 1,
    val notes: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medication_changes",
    indices = [Index("userId"), Index("prescriptionId"), Index("medicationId"), Index("createdAt")]
)
data class MedicationChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val prescriptionId: Long = 0L,
    val medicationId: Long? = null,
    val medicineName: String,
    val changeType: MedicationChangeType,
    val detailedChanges: String = "", // e.g. "STRENGTH_CHANGED,FREQUENCY_CHANGED"
    val fieldChanged: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val matchConfidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val reviewStatus: ChangeReviewStatus = ChangeReviewStatus.ACCEPTED,
    val reviewedBy: String = "Patient",
    val reviewedAt: Long? = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medication_versions",
    indices = [Index("medicationId"), Index("userId"), Index("versionNumber")]
)
data class MedicationVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val userId: String = "default_user",
    val versionNumber: Int = 1,
    val prescriptionId: Long? = null,
    val name: String,
    val genericName: String = "",
    val brandName: String = "",
    val strength: String = "",
    val doseAmount: Double = 1.0,
    val doseUnit: DoseUnit = DoseUnit.TABLET,
    val form: MedicineForm = MedicineForm.TABLET,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val route: String = "Oral",
    val instructions: String = "After food",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val status: MedicationStatus = MedicationStatus.ACTIVE,
    val changeReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "emergency_profiles",
    indices = [Index(value = ["userId"], unique = true), Index("emergencyIdentifier")]
)
data class EmergencyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val enabled: Boolean = true,
    val preferredName: String = "",
    val bloodGroup: String = "Unknown",
    val primaryDoctorName: String = "",
    val primaryDoctorPhone: String = "",
    val hospitalClinicName: String = "",
    val importantNotes: String = "",
    val organDonor: Boolean = false,
    val qrEnabled: Boolean = false,
    val emergencyIdentifier: String = java.util.UUID.randomUUID().toString(),
    val qrCreatedAt: Long? = null,
    val qrRevokedAt: Long? = null,
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Patient"
)

@Entity(
    tableName = "emergency_conditions",
    indices = [Index("userId")]
)
data class EmergencyConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val name: String,
    val notes: String = "",
    val diagnosedYear: String = "",
    val verified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "emergency_allergies",
    indices = [Index("userId")]
)
data class EmergencyAllergyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val allergen: String,
    val reaction: String = "",
    val severity: String = "Severe", // Severe, Moderate, Mild
    val verified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "emergency_contacts",
    indices = [Index("userId"), Index("priority")]
)
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val name: String,
    val relationship: String,
    val phone: String,
    val email: String = "",
    val priority: Int = 1,
    val isPrimary: Boolean = true,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "emergency_access_logs",
    indices = [Index("userId"), Index("emergencyIdentifier"), Index("accessedAt")]
)
data class EmergencyAccessLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val emergencyIdentifier: String,
    val accessedAt: Long = System.currentTimeMillis(),
    val accessType: String = "APP_PREVIEW", // "APP_PREVIEW", "QR_SCAN", "SIMULATED_WEB"
    val success: Boolean = true,
    val ipOrDeviceHint: String = "Android Device",
    val notes: String = ""
)

