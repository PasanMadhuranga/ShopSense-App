package com.example.myapplicationv2.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplicationv2.data.local.prefs.HomePrefs
import com.example.myapplicationv2.shopping.ShoppingModeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingModeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("ShoppingModeActionReceiver", "onReceive action=$action")

        val prefs = HomePrefs(context)

        when (action) {
            ACTION_YES -> {
                CoroutineScope(Dispatchers.IO).launch {
                    prefs.setShoppingMode(on = true, manual = false)
                }

                val startIntent = Intent(context, ShoppingModeService::class.java).apply {
                    this.action = ShoppingModeService.ACTION_START
                }
                ContextCompat.startForegroundService(context, startIntent)
            }

            ACTION_NO -> {
                CoroutineScope(Dispatchers.IO).launch {
                    prefs.setShoppingMode(on = false, manual = false)
                }

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
