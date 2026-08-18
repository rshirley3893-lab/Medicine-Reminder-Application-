package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AlertChannel
import com.example.data.model.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val doseEventId = intent.getLongExtra("EXTRA_DOSE_EVENT_ID", -1L)
        if (doseEventId == -1L) return

        Log.d(TAG, "onReceive action=$action for doseEventId=$doseEventId")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    MedicationReminderScheduler.ACTION_MEDICINE_REMINDER,
                    "com.example.ACTION_MEDICINE_REMINDER" -> {
                        val db = AppDatabase.getInstance(context)
                        val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseEventId)

                        if (doseWithMed != null) {
                            val dose = doseWithMed.doseEvent
                            val med = doseWithMed.medication

                            val schedule = db.medicationScheduleDao().getScheduleById(dose.scheduleId)
                            val profile = db.userProfileDao().getUserProfileDirect(med.userId) ?: UserProfileEntity(id = med.userId)

                            val remindersEnabled = schedule?.reminderEnabled ?: profile.remindersEnabled

                            if (med.active && remindersEnabled && (dose.status == DoseStatus.SCHEDULED || dose.status == DoseStatus.SNOOZED || dose.status == DoseStatus.PENDING)) {
                                val currentTime = System.currentTimeMillis()
                                // Transition state: SCHEDULED -> REMINDER_SENT
                                db.doseEventDao().updateReminderSent(dose.id, currentTime)

                                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val timeStr = timeFormat.format(Date(dose.scheduledAt))
                                val snoozeMin = schedule?.snoozeDurationMinutes ?: profile.defaultSnoozeMinutes

                                NotificationHelper.showMedicationReminder(
                                    context = context,
                                    doseEventId = dose.id,
                                    medicationName = med.name,
                                    strength = med.strength,
                                    doseAmount = med.doseAmount,
                                    doseUnit = med.doseUnit.symbol,
                                    instructions = med.instructions,
                                    scheduledTime = timeStr,
                                    snoozeDurationMinutes = snoozeMin
                                )

                                // Schedule grace period check alarm
                                val graceMinutes = schedule?.gracePeriodMinutes ?: profile.gracePeriodMinutes
                                val gracePeriodMs = graceMinutes * 60 * 1000L
                                val checkTime = currentTime + gracePeriodMs
                                MedicationReminderScheduler.scheduleGracePeriodCheck(context, dose.id, checkTime)

                                // Log notification
                                db.notificationLogDao().insertLog(
                                    NotificationLogEntity(
                                        userId = med.userId,
                                        doseEventId = dose.id,
                                        channel = AlertChannel.LOCAL_NOTIFICATION,
                                        recipient = "Self (Device)",
                                        sentAt = currentTime,
                                        deliveryStatus = "DELIVERED",
                                        messageText = "Medication reminder dispatched for ${med.name} ($timeStr)"
                                    )
                                )
                                Log.d(TAG, "Displayed reminder for dose $doseEventId (${med.name}) with $graceMinutes min grace period")
                            }
                        }
                    }

                    MedicationReminderScheduler.ACTION_GRACE_PERIOD_CHECK,
                    "com.example.ACTION_GRACE_PERIOD_CHECK" -> {
                        MissedDoseProcessor.processGracePeriodExpired(context, doseEventId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling receiver action $action for dose $doseEventId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
