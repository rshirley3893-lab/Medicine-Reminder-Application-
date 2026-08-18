package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotifActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val doseEventId = intent.getLongExtra("EXTRA_DOSE_EVENT_ID", -1L)
        val action = intent.action ?: return
        if (doseEventId == -1L) return

        Log.d(TAG, "Notification action received: $action for dose $doseEventId")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "com.example.ACTION_TAKE_MEDICINE" -> {
                        DoseNotificationHandler.handleTakeNow(context, doseEventId)
                    }

                    "com.example.ACTION_SNOOZE_MEDICINE" -> {
                        val snoozeMinutes = intent.getIntExtra("EXTRA_SNOOZE_MINUTES", 10)
                        DoseNotificationHandler.handleSnooze(context, doseEventId, snoozeMinutes)
                    }

                    "com.example.ACTION_SKIP_MEDICINE" -> {
                        val skipReason = intent.getStringExtra("EXTRA_SKIP_REASON")
                        DoseNotificationHandler.handleSkip(context, doseEventId, skipReason)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing notification action $action for dose $doseEventId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
