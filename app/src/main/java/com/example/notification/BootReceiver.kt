package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received system broadcast: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    MissedDoseProcessor.checkAllPendingGracePeriods(context)
                    MedicationReminderScheduler.rescheduleAllFutureDoses(context)
                    Log.d(TAG, "Successfully processed grace period sweeps and rescheduled all alarms after $action")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot/time change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
