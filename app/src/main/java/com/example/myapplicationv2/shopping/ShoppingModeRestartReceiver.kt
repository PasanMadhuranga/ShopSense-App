package com.example.myapplicationv2.shopping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class ShoppingModeRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("ShoppingModeRestartReceiver", "Alarm fired, restarting Shopping Mode")

        val appContext = context.applicationContext
        val serviceIntent = Intent(appContext, ShoppingModeService::class.java).apply {
            action = ShoppingModeService.ACTION_START
        }

        ContextCompat.startForegroundService(appContext, serviceIntent)
    }
}
