package com.example.notification

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.model.AlertChannel
import com.example.data.model.DoseStatus
import com.example.data.model.StockTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DoseNotificationHandler {
    private const val TAG = "DoseNotifHandler"

    suspend fun handleTakeNow(context: Context, doseEventId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseEventId) ?: return@withContext false
            val dose = doseWithMed.doseEvent
            val med = doseWithMed.medication

            val currentTime = System.currentTimeMillis()
            db.doseEventDao().updateDoseStatus(dose.id, DoseStatus.TAKEN, currentTime)

            // Cancel notification and scheduled alarms for this dose
            NotificationHelper.cancelNotification(context, dose.id.toInt())
            NotificationHelper.cancelNotification(context, (dose.id + 10000).toInt())
            MedicationReminderScheduler.cancelReminder(context, dose.id)

            // Deduct stock
            val newStock = (med.stockQuantity - med.doseAmount).coerceAtLeast(0.0)
            db.medicationDao().updateStock(med.id, newStock)

            // Record stock transaction
            db.stockTransactionDao().insertTransaction(
                StockTransactionEntity(
                    medicationId = med.id,
                    quantityChange = -med.doseAmount,
                    balanceAfter = newStock,
                    type = StockTransactionType.DOSE_TAKEN,
                    sourceDoseEventId = dose.id,
                    note = "Marked as Taken (${med.doseAmount} ${med.doseUnit.symbol})"
                )
            )

            // Log action
            db.notificationLogDao().insertLog(
                NotificationLogEntity(
                    userId = med.userId,
                    doseEventId = dose.id,
                    channel = AlertChannel.LOCAL_NOTIFICATION,
                    recipient = "Self (Device)",
                    sentAt = currentTime,
                    deliveryStatus = "CONFIRMED",
                    messageText = "Taken: ${med.name} (${med.doseAmount} ${med.doseUnit.symbol})"
                )
            )

            // Check low stock
            if (newStock <= med.lowStockThreshold) {
                NotificationHelper.showLowStockAlert(
                    context,
                    med.id,
                    med.name,
                    newStock,
                    med.lowStockThreshold
                )
            }
            Log.d(TAG, "Dose $doseEventId successfully marked TAKEN. Remaining stock: $newStock")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleTakeNow for dose $doseEventId", e)
            false
        }
    }

    suspend fun handleSnooze(context: Context, doseEventId: Long, snoozeMinutes: Int = 10): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseEventId) ?: return@withContext false
            val dose = doseWithMed.doseEvent
            val med = doseWithMed.medication

            val currentTime = System.currentTimeMillis()
            val validSnoozeMinutes = snoozeMinutes.coerceIn(1, 180)
            val snoozedUntil = currentTime + (validSnoozeMinutes * 60 * 1000L)

            db.doseEventDao().snoozeDose(dose.id, snoozedUntil)

            // Dismiss active notification
            NotificationHelper.cancelNotification(context, dose.id.toInt())
            NotificationHelper.cancelNotification(context, (dose.id + 10000).toInt())

            // Schedule reminder for the snoozed time
            MedicationReminderScheduler.scheduleReminder(context, dose.id, snoozedUntil)

            // Re-schedule grace period check after snooze time + grace period
            val schedule = db.medicationScheduleDao().getScheduleById(dose.scheduleId)
            val gracePeriodMs = (schedule?.gracePeriodMinutes ?: 60) * 60 * 1000L
            val newGraceCheckTime = snoozedUntil + gracePeriodMs
            MedicationReminderScheduler.scheduleGracePeriodCheck(context, dose.id, newGraceCheckTime)

            // Log snooze
            db.notificationLogDao().insertLog(
                NotificationLogEntity(
                    userId = med.userId,
                    doseEventId = dose.id,
                    channel = AlertChannel.LOCAL_NOTIFICATION,
                    recipient = "Self (Device)",
                    sentAt = currentTime,
                    deliveryStatus = "SNOOZED",
                    messageText = "Snoozed: ${med.name} for $validSnoozeMinutes min"
                )
            )

            Log.d(TAG, "Dose $doseEventId snoozed for $validSnoozeMinutes minutes until $snoozedUntil")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleSnooze for dose $doseEventId", e)
            false
        }
    }

    suspend fun handleSkip(context: Context, doseEventId: Long, skipReason: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseEventId) ?: return@withContext false
            val dose = doseWithMed.doseEvent
            val med = doseWithMed.medication

            val currentTime = System.currentTimeMillis()
            db.doseEventDao().skipDose(dose.id, currentTime, skipReason)

            // Cancel notification and scheduled alarms
            NotificationHelper.cancelNotification(context, dose.id.toInt())
            NotificationHelper.cancelNotification(context, (dose.id + 10000).toInt())
            MedicationReminderScheduler.cancelReminder(context, dose.id)

            // Log skip (Explicitly skipped is NOT marked as missed)
            db.notificationLogDao().insertLog(
                NotificationLogEntity(
                    userId = med.userId,
                    doseEventId = dose.id,
                    channel = AlertChannel.LOCAL_NOTIFICATION,
                    recipient = "Self (Device)",
                    sentAt = currentTime,
                    deliveryStatus = "SKIPPED",
                    messageText = "Skipped: ${med.name}${if (!skipReason.isNullOrBlank()) " ($skipReason)" else ""}"
                )
            )

            Log.d(TAG, "Dose $doseEventId explicitly SKIPPED. Reason: $skipReason")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleSkip for dose $doseEventId", e)
            false
        }
    }
}
