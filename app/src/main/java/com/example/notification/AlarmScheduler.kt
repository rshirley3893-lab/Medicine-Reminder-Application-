package com.example.notification

import android.content.Context

object AlarmScheduler {
    fun scheduleDoseAlarm(context: Context, doseEventId: Long, scheduledTimeMs: Long) {
        MedicationReminderScheduler.scheduleReminder(context, doseEventId, scheduledTimeMs)
    }

    fun cancelDoseAlarm(context: Context, doseEventId: Long) {
        MedicationReminderScheduler.cancelReminder(context, doseEventId)
    }

    suspend fun rescheduleAllFutureDoses(context: Context) {
        MedicationReminderScheduler.rescheduleAllFutureDoses(context)
    }
}
