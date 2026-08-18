package com.example.notification

import android.content.Context
import android.util.Log
import com.example.backend.CommunicationService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AlertChannel
import com.example.data.model.DoseStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MissedDoseProcessor {
    private const val TAG = "MissedDoseProcessor"

    suspend fun processGracePeriodExpired(context: Context, doseEventId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val doseWithMed = db.doseEventDao().getDoseWithMedicationById(doseEventId) ?: return@withContext false
            val dose = doseWithMed.doseEvent
            val med = doseWithMed.medication

            // Rule: If already taken or explicitly skipped or already marked missed, do not process
            if (dose.status == DoseStatus.TAKEN || dose.status == DoseStatus.SKIPPED || dose.status == DoseStatus.MISSED) {
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()

            // Rule: The dose must NOT become MISSED while an active snooze period exists
            if (dose.status == DoseStatus.SNOOZED && dose.snoozedUntil != null && dose.snoozedUntil > currentTime) {
                Log.d(TAG, "Dose $doseEventId is currently in an active snooze until ${dose.snoozedUntil}. Not marking as missed.")
                return@withContext false
            }

            val schedule = db.medicationScheduleDao().getScheduleById(dose.scheduleId)
            val profile = db.userProfileDao().getUserProfileDirect(med.userId) ?: UserProfileEntity(id = med.userId)
            val gracePeriodMinutes = schedule?.gracePeriodMinutes ?: profile.gracePeriodMinutes
            val gracePeriodMs = gracePeriodMinutes * 60 * 1000L

            // Verify grace period elapsed from scheduled time (or snooze start)
            val referenceTime = if (dose.status == DoseStatus.SNOOZED && dose.snoozedUntil != null) {
                dose.snoozedUntil - (10 * 60 * 1000L)
            } else {
                dose.scheduledAt
            }

            if (currentTime < referenceTime + gracePeriodMs) {
                Log.d(TAG, "Grace period of $gracePeriodMinutes min has not elapsed yet for dose $doseEventId")
                return@withContext false
            }

            // Mark dose as MISSED
            db.doseEventDao().markDoseMissed(dose.id, currentTime)

            // Cancel any active reminder notifications for this dose
            NotificationHelper.cancelNotification(context, dose.id.toInt())
            MedicationReminderScheduler.cancelReminder(context, dose.id)

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val scheduledTimeStr = timeFormat.format(Date(dose.scheduledAt))
            val doseDisplay = "${med.doseAmount} ${med.doseUnit.symbol}".trim()

            // 1. Patient Notification (Always sent if missed-dose alerts enabled)
            if (profile.missedDoseAlertsEnabled) {
                NotificationHelper.showPatientMissedDoseNotification(
                    context = context,
                    doseEventId = dose.id,
                    medicationName = med.name,
                    strength = med.strength,
                    doseDisplay = doseDisplay,
                    scheduledTime = scheduledTimeStr
                )

                db.notificationLogDao().insertLog(
                    NotificationLogEntity(
                        userId = med.userId,
                        doseEventId = dose.id,
                        channel = AlertChannel.PATIENT_MISSED_ALERT,
                        recipient = "Patient (${profile.name.ifBlank { "Self" }})",
                        sentAt = currentTime,
                        deliveryStatus = "DELIVERED",
                        messageText = "Missed dose reminder recorded for ${med.name} ($scheduledTimeStr)"
                    )
                )
            }

            // 2. Caregiver / Trusted Contact Escalation
            val isPatient = profile.role == UserRole.PATIENT
            val isCaregiverMode = profile.role == UserRole.CAREGIVER
            val shouldEscalate = (isPatient && profile.trustedContactAlertsEnabled) || (isCaregiverMode && profile.caregiverMissedDoseAlertsEnabled) || profile.escalationEnabled

            if (shouldEscalate) {
                val activeCaregivers = db.caregiverDao().getActiveCaregivers(med.userId)
                val patientDisplayName = if (isCaregiverMode && profile.managedPatientName.isNotBlank()) {
                    profile.managedPatientName
                } else {
                    profile.name.ifBlank { "Patient" }
                }

                // Check for consecutive missed doses in the last 24 hours
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val recentMissedCount = db.doseEventDao().getMissedDoseCountBetween(med.userId, cal.timeInMillis, currentTime)

                val isMultipleMissed = recentMissedCount >= 2 && profile.repeatedMissedAlertsEnabled

                if (isMultipleMissed) {
                    NotificationHelper.showCaregiverMultipleMissedDosesAlert(
                        context = context,
                        alertId = (dose.id + 40000).toInt(),
                        patientName = patientDisplayName,
                        consecutiveCount = recentMissedCount,
                        latestMedName = med.name
                    )

                    db.notificationLogDao().insertLog(
                        NotificationLogEntity(
                            userId = med.userId,
                            doseEventId = dose.id,
                            channel = AlertChannel.REPEATED_MISSED_ALERT,
                            recipient = "Caregivers / Trusted Contacts",
                            sentAt = currentTime,
                            deliveryStatus = "DELIVERED",
                            messageText = "Escalation: $recentMissedCount consecutive unconfirmed doses for $patientDisplayName"
                        )
                    )
                } else {
                    NotificationHelper.showCaregiverMissedDoseAlert(
                        context = context,
                        alertId = (dose.id + 30000).toInt(),
                        patientName = patientDisplayName,
                        medicationName = med.name,
                        strength = med.strength,
                        doseDisplay = doseDisplay,
                        scheduledTime = scheduledTimeStr
                    )
                }

                for (caregiver in activeCaregivers) {
                    if (caregiver.alertMissedDose) {
                        CommunicationService.sendMissedDoseAlert(
                            context = context,
                            caregiver = caregiver,
                            patientName = patientDisplayName,
                            medicationName = med.name,
                            doseTime = scheduledTimeStr
                        )
                    }
                }
            }

            Log.d(TAG, "Processed expired grace period for dose $doseEventId. Dose marked MISSED.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing missed dose for $doseEventId", e)
            false
        }
    }

    suspend fun checkAllPendingGracePeriods(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val currentTime = System.currentTimeMillis()
            // Default 60 min cutoff for sweep
            val defaultCutoffTime = currentTime - (60 * 60 * 1000L)
            val expiredDoses = db.doseEventDao().getExpiredGracePeriodDoses(defaultCutoffTime, currentTime)

            for (dose in expiredDoses) {
                processGracePeriodExpired(context, dose.id)
            }
            Log.d(TAG, "Completed sweep of ${expiredDoses.size} expired grace period doses.")
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAllPendingGracePeriods sweep", e)
        }
    }
}
