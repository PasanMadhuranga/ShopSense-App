package com.example.myapplicationv2.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.myapplicationv2.shopping.ShoppingModeService

class ShoppingModeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            ACTION_YES -> {
                val startIntent = Intent(context, ShoppingModeService::class.java).apply {
                    this.action = ShoppingModeService.ACTION_START
                }
                androidx.core.content.ContextCompat.startForegroundService(context, startIntent)
            }
            ACTION_NO -> {
                // Optionally: stop service if running
                val stopIntent = Intent(context, ShoppingModeService::class.java).apply {
                    this.action = ShoppingModeService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    }

    companion object {
        const val ACTION_YES =
            "com.example.myapplicationv2.ACTION_ACTIVATE_SHOPPING_MODE"
        const val ACTION_NO =
            "com.example.myapplicationv2.ACTION_DISMISS_SHOPPING_MODE"

        const val NOTIFICATION_ID = 1001
    }
}
