package com.example.myapplicationv2.shopping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShoppingModeStatusReceiver(
    private val onStopped: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "SHOPPING_MODE_STOPPED") {
            onStopped()
        }
    }
}
