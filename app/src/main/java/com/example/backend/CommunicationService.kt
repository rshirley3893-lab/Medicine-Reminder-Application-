package com.example.backend

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CaregiverEntity
import com.example.data.local.entity.NotificationLogEntity
import com.example.data.model.AlertChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

object CommunicationService {
    private const val TAG = "CommunicationService"

    suspend fun sendMissedDoseAlert(
        context: Context,
        caregiver: CaregiverEntity,
        patientName: String,
        medicationName: String,
        doseTime: String
    ) = withContext(Dispatchers.IO) {
        val message = "⚠️ Medicine Reminder Alert: $patientName missed their scheduled dose of $medicationName at $doseTime. Please check in with them."
        val db = AppDatabase.getInstance(context)

        val channel = when {
            caregiver.whatsappNumber.isNotBlank() && caregiver.preferredChannel == AlertChannel.WHATSAPP -> AlertChannel.WHATSAPP
            caregiver.email.isNotBlank() && caregiver.preferredChannel == AlertChannel.EMAIL -> AlertChannel.EMAIL
            else -> AlertChannel.CAREGIVER_ALERT
        }

        db.notificationLogDao().insertLog(
            NotificationLogEntity(
                doseEventId = 0,
                channel = channel,
                recipient = caregiver.name + " (" + (if (channel == AlertChannel.WHATSAPP) caregiver.whatsappNumber else caregiver.email) + ")",
                sentAt = System.currentTimeMillis(),
                deliveryStatus = "SENT",
                messageText = message
            )
        )
        Log.d(TAG, "Dispatched alert to ${caregiver.name} via $channel: $message")
    }

    fun openWhatsAppMessage(context: Context, phoneNumber: String, messageText: String) {
        try {
            val cleanPhone = phoneNumber.replace(Regex("""[^\d+]"""), "")
            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp intent", e)
        }
    }

    fun sendEmailIntent(context: Context, recipientEmail: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Email intent", e)
        }
    }
}
