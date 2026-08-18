package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.DoseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MedicationReminderScheduler {
    private const val TAG = "MedReminderScheduler"

    const val ACTION_MEDICINE_REMINDER = "com.example.ACTION_MEDICINE_REMINDER"
    const val ACTION_GRACE_PERIOD_CHECK = "com.example.ACTION_GRACE_PERIOD_CHECK"

    fun scheduleReminder(context: Context, doseEventId: Long, scheduledTimeMs: Long) {
        val currentTime = System.currentTimeMillis()
        if (scheduledTimeMs <= currentTime) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MEDICINE_REMINDER
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            doseEventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTimeMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder alarm for dose $doseEventId at $scheduledTimeMs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder alarm for dose $doseEventId", e)
        }
    }

    fun scheduleGracePeriodCheck(context: Context, doseEventId: Long, checkTimeMs: Long) {
        val currentTime = System.currentTimeMillis()
        if (checkTimeMs <= currentTime) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_GRACE_PERIOD_CHECK
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (doseEventId + 200000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        checkTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        checkTimeMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    checkTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    checkTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled grace period check for dose $doseEventId at $checkTimeMs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule grace period alarm for dose $doseEventId", e)
        }
    }

    fun cancelReminder(context: Context, doseEventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel reminder alarm
        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MEDICINE_REMINDER
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }
        val reminderPi = PendingIntent.getBroadcast(
            context,
            doseEventId.toInt(),
            reminderIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (reminderPi != null) {
            alarmManager.cancel(reminderPi)
            reminderPi.cancel()
            Log.d(TAG, "Cancelled reminder alarm for dose $doseEventId")
        }

        // Cancel grace period check alarm
        val graceIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_GRACE_PERIOD_CHECK
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }
        val gracePi = PendingIntent.getBroadcast(
            context,
            (doseEventId + 200000).toInt(),
            graceIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (gracePi != null) {
            alarmManager.cancel(gracePi)
            gracePi.cancel()
            Log.d(TAG, "Cancelled grace period check alarm for dose $doseEventId")
        }
    }

    suspend fun rescheduleAllFutureDoses(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val currentTime = System.currentTimeMillis()
        // Reschedule next 7 days of scheduled and snoozed events
        val futureLimit = currentTime + (7 * 24 * 60 * 60 * 1000L)
        val futureDoses = db.doseEventDao().getAllFutureDosesDirect(currentTime - (2 * 60 * 60 * 1000L), futureLimit)

        for (doseWithMed in futureDoses) {
            val dose = doseWithMed.doseEvent
            val med = doseWithMed.medication
            if (!med.active) continue

            when (dose.status) {
                DoseStatus.SCHEDULED -> {
                    if (dose.scheduledAt > currentTime) {
                        scheduleReminder(context, dose.id, dose.scheduledAt)
                    }
                }
                DoseStatus.SNOOZED -> {
                    val snoozedTime = dose.snoozedUntil
                    if (snoozedTime != null && snoozedTime > currentTime) {
                        scheduleReminder(context, dose.id, snoozedTime)
                    }
                }
                DoseStatus.REMINDER_SENT, DoseStatus.PENDING -> {
                    // Re-schedule grace period check if not yet expired
                    val schedule = db.medicationScheduleDao().getScheduleById(dose.scheduleId)
                    val gracePeriodMs = (schedule?.gracePeriodMinutes ?: 60) * 60 * 1000L
                    val checkTime = dose.scheduledAt + gracePeriodMs
                    if (checkTime > currentTime) {
                        scheduleGracePeriodCheck(context, dose.id, checkTime)
                    }
                }
                else -> {
                    // TAKEN, SKIPPED, MISSED do not need alarms
                }
            }
        }
        Log.d(TAG, "Restored and rescheduled all future medication reminders")
    }
}
