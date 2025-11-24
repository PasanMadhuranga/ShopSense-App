package com.example.myapplicationv2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShopSenseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SHOPPING_MODE_CHANNEL_ID,
                "Shopping Mode",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to activate shopping mode near stores"
            }

            val manager: NotificationManager? = getSystemService()
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val SHOPPING_MODE_CHANNEL_ID = "shopping_mode_channel"
    }
}
