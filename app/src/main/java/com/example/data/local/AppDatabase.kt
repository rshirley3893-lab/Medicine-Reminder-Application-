package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AuthSessionDao
import com.example.data.local.dao.CaregiverDao
import com.example.data.local.dao.DoseEventDao
import com.example.data.local.dao.EmergencyAccessLogDao
import com.example.data.local.dao.EmergencyAllergyDao
import com.example.data.local.dao.EmergencyConditionDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.EmergencyProfileDao
import com.example.data.local.dao.MedicationChangeDao
import com.example.data.local.dao.MedicationDao
import com.example.data.local.dao.MedicationScheduleDao
import com.example.data.local.dao.MedicationVersionDao
import com.example.data.local.dao.NotificationLogDao
import com.example.data.local.dao.PrescriptionRecordDao
import com.example.data.local.dao.PrescriptionScanDao
import com.example.data.local.dao.StockTransactionDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.dao.UserProfileDao
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
import com.example.data.model.DoseStatus
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicationStatus
import com.example.data.model.MedicineForm
import com.example.data.model.PrescriptionStatus
import com.example.data.model.StockTransactionType
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.PATIENT
    }

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = try {
        DoseStatus.valueOf(value)
    } catch (e: Exception) {
        DoseStatus.SCHEDULED
    }

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name

    @TypeConverter
    fun toFrequencyType(value: String): FrequencyType = try {
        FrequencyType.valueOf(value)
    } catch (e: Exception) {
        FrequencyType.DAILY
    }

    @TypeConverter
    fun fromMedicineForm(value: MedicineForm): String = value.name

    @TypeConverter
    fun toMedicineForm(value: String): MedicineForm = try {
        MedicineForm.valueOf(value)
    } catch (e: Exception) {
        MedicineForm.TABLET
    }

    @TypeConverter
    fun fromDoseUnit(value: DoseUnit): String = value.name

    @TypeConverter
    fun toDoseUnit(value: String): DoseUnit = try {
        DoseUnit.valueOf(value)
    } catch (e: Exception) {
        DoseUnit.TABLET
    }

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String = value.name

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel = try {
        ConfidenceLevel.valueOf(value)
    } catch (e: Exception) {
        ConfidenceLevel.HIGH
    }

    @TypeConverter
    fun fromAlertChannel(value: AlertChannel): String = value.name

    @TypeConverter
    fun toAlertChannel(value: String): AlertChannel = try {
        AlertChannel.valueOf(value)
    } catch (e: Exception) {
        AlertChannel.LOCAL_NOTIFICATION
    }

    @TypeConverter
    fun fromStockTransactionType(value: StockTransactionType): String = value.name

    @TypeConverter
    fun toStockTransactionType(value: String): StockTransactionType = try {
        StockTransactionType.valueOf(value)
    } catch (e: Exception) {
        StockTransactionType.DOSE_TAKEN
    }

    @TypeConverter
    fun fromPrescriptionStatus(value: PrescriptionStatus): String = value.name

    @TypeConverter
    fun toPrescriptionStatus(value: String): PrescriptionStatus = try {
        PrescriptionStatus.valueOf(value)
    } catch (e: Exception) {
        PrescriptionStatus.CONFIRMED
    }

    @TypeConverter
    fun fromMedicationChangeType(value: MedicationChangeType): String = value.name

    @TypeConverter
    fun toMedicationChangeType(value: String): MedicationChangeType = try {
        MedicationChangeType.valueOf(value)
    } catch (e: Exception) {
        MedicationChangeType.CHANGED
    }

    @TypeConverter
    fun fromChangeReviewStatus(value: ChangeReviewStatus): String = value.name

    @TypeConverter
    fun toChangeReviewStatus(value: String): ChangeReviewStatus = try {
        ChangeReviewStatus.valueOf(value)
    } catch (e: Exception) {
        ChangeReviewStatus.ACCEPTED
    }

    @TypeConverter
    fun fromMedicationStatus(value: MedicationStatus): String = value.name

    @TypeConverter
    fun toMedicationStatus(value: String): MedicationStatus = try {
        MedicationStatus.valueOf(value)
    } catch (e: Exception) {
        MedicationStatus.ACTIVE
    }
}

@Database(
    entities = [
        UserAccountEntity::class,
        AuthSessionEntity::class,
        UserProfileEntity::class,
        CaregiverEntity::class,
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        DoseEventEntity::class,
        PrescriptionScanEntity::class,
        OcrCandidateEntity::class,
        NotificationLogEntity::class,
        StockTransactionEntity::class,
        PrescriptionRecordEntity::class,
        MedicationChangeEntity::class,
        MedicationVersionEntity::class,
        EmergencyProfileEntity::class,
        EmergencyConditionEntity::class,
        EmergencyAllergyEntity::class,
        EmergencyContactEntity::class,
        EmergencyAccessLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAccountDao(): UserAccountDao
    abstract fun authSessionDao(): AuthSessionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun caregiverDao(): CaregiverDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun prescriptionScanDao(): PrescriptionScanDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun prescriptionRecordDao(): PrescriptionRecordDao
    abstract fun medicationChangeDao(): MedicationChangeDao
    abstract fun medicationVersionDao(): MedicationVersionDao
    abstract fun emergencyProfileDao(): EmergencyProfileDao
    abstract fun emergencyConditionDao(): EmergencyConditionDao
    abstract fun emergencyAllergyDao(): EmergencyAllergyDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun emergencyAccessLogDao(): EmergencyAccessLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicine_reminder_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
