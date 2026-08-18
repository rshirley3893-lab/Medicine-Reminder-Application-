package com.example

import android.app.Application
import com.example.data.repository.MedicineRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicineApplication : Application() {
    lateinit var repository: MedicineRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        repository = MedicineRepository(this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.seedSampleDataIfEmpty()
            repository.checkAndEscalateMissedDoses()
            AlarmScheduler.rescheduleAllFutureDoses(this@MedicineApplication)
        }
    }
}
