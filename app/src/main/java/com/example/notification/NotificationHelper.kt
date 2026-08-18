package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_REMINDERS_ID = "med_reminders_channel"
    const val CHANNEL_REMINDERS_NAME = "Medication Reminders"

    const val CHANNEL_MISSED_ID = "med_missed_channel"
    const val CHANNEL_MISSED_NAME = "Missed Dose Alerts"

    const val CHANNEL_CAREGIVER_ID = "med_caregiver_channel"
    const val CHANNEL_CAREGIVER_NAME = "Caregiver Escalation Alerts"

    const val CHANNEL_ALERTS_ID = "med_alerts_channel"
    const val CHANNEL_ALERTS_NAME = "Stock & System Alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: reminderSound
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Reminder channel (High importance with sound and vibration)
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical daily medication reminder notifications"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Missed Dose channel (High importance)
            val missedChannel = NotificationChannel(
                CHANNEL_MISSED_ID,
                CHANNEL_MISSED_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for unconfirmed and missed medication doses"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600)
                setSound(reminderSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Caregiver Escalation channel (High importance)
            val caregiverChannel = NotificationChannel(
                CHANNEL_CAREGIVER_ID,
                CHANNEL_CAREGIVER_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Caregiver notifications when a patient misses a scheduled dose"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 700)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. System and Stock Alerts channel
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                CHANNEL_ALERTS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Medicine stock depletion warnings and refill notices"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(missedChannel)
            notificationManager.createNotificationChannel(caregiverChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun showMedicationReminder(
        context: Context,
        doseEventId: Long,
        medicationName: String,
        strength: String,
        doseAmount: Double,
        doseUnit: String,
        instructions: String,
        scheduledTime: String,
        snoozeDurationMinutes: Int = 10
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
            putExtra("EXTRA_NAV_TARGET", "SCHEDULE")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            doseEventId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Take Now" Action
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_TAKE_MEDICINE"
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            (doseEventId * 10 + 1).toInt(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Snooze" Action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_SNOOZE_MEDICINE"
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
            putExtra("EXTRA_SNOOZE_MINUTES", snoozeDurationMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (doseEventId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Skip" Action
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_SKIP_MEDICINE"
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (doseEventId * 10 + 3).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val strengthDisplay = if (strength.isNotBlank()) " $strength" else ""
        val doseDisplay = "$doseAmount $doseUnit".trim()
        val instructionDisplay = if (instructions.isNotBlank()) "\nInstructions: $instructions" else ""

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 Medicine Reminder: $medicationName$strengthDisplay")
            .setContentText("Dose: $doseDisplay • Scheduled for $scheduledTime$instructionDisplay")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Medicine: $medicationName$strengthDisplay\n" +
                    "Dose: $doseDisplay\n" +
                    "Scheduled: $scheduledTime\n" +
                    "It's time for your scheduled dose." +
                    instructionDisplay
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Take Now", takePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze (${snoozeDurationMinutes}m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Skip", skipPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(doseEventId.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS may not be granted yet
        }
    }

    fun showPatientMissedDoseNotification(
        context: Context,
        doseEventId: Long,
        medicationName: String,
        strength: String,
        doseDisplay: String,
        scheduledTime: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DOSE_EVENT_ID", doseEventId)
            putExtra("EXTRA_NAV_TARGET", "SCHEDULE")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            (doseEventId + 10000).toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val strengthDisplay = if (strength.isNotBlank()) " $strength" else ""
        val content = "You did not confirm your $medicationName$strengthDisplay dose scheduled for $scheduledTime. Please check your medication plan."

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Missed Medicine: $medicationName$strengthDisplay")
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚠️ Missed Medicine\n\n" +
                    "You did not confirm your $medicationName$strengthDisplay ($doseDisplay) scheduled for $scheduledTime.\n\n" +
                    "Please check your medication plan in the app."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify((doseEventId + 10000).toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS may not be granted
        }
    }

    fun showCaregiverMissedDoseAlert(
        context: Context,
        alertId: Int,
        patientName: String,
        medicationName: String,
        strength: String,
        doseDisplay: String,
        scheduledTime: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TARGET", "DASHBOARD")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            alertId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val strengthDisplay = if (strength.isNotBlank()) " $strength" else ""
        val builder = NotificationCompat.Builder(context, CHANNEL_CAREGIVER_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Missed Dose Alert: $patientName")
            .setContentText("$medicationName$strengthDisplay scheduled for $scheduledTime was missed.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚠️ Missed Dose Alert\n\n" +
                    "Patient: $patientName\n" +
                    "Medicine: $medicationName$strengthDisplay ($doseDisplay)\n" +
                    "Scheduled: $scheduledTime\n" +
                    "Status: Missed\n\n" +
                    "No confirmation was received within the configured reminder period."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(alertId, builder.build())
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS missing
        }
    }

    fun showCaregiverMultipleMissedDosesAlert(
        context: Context,
        alertId: Int,
        patientName: String,
        consecutiveCount: Int,
        latestMedName: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TARGET", "DASHBOARD")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            alertId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CAREGIVER_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ Attention: Multiple Missed Doses ($patientName)")
            .setContentText("$consecutiveCount consecutive doses missed. Latest: $latestMedName.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚠️ Multiple Medication Doses Require Attention\n\n" +
                    "Patient: $patientName\n" +
                    "Count: $consecutiveCount consecutive unconfirmed scheduled doses\n" +
                    "Latest: $latestMedName\n\n" +
                    "Please check in with $patientName to review their medication schedule."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(alertId, builder.build())
        } catch (e: SecurityException) {
            // Ignored
        }
    }

    fun showLowStockAlert(
        context: Context,
        medicationId: Long,
        medicationName: String,
        remainingStock: Double,
        threshold: Double
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TARGET", "STOCK")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            (medicationId + 50000).toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Low Stock Alert: $medicationName")
            .setContentText("Only $remainingStock left (Threshold: $threshold). Tap to review and refill.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify((medicationId + 50000).toInt(), builder.build())
        } catch (e: SecurityException) {
            // Ignore if notification permission missing
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}

