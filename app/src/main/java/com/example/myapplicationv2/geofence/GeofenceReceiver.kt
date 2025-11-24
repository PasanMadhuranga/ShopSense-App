package com.example.myapplicationv2.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplicationv2.R
import com.example.myapplicationv2.ShopSenseApp
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.myapplicationv2.MainActivity
import com.example.myapplicationv2.shopping.ShoppingModeService

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                // Already showing "Activate Shopping Mode?" notification here
                showShoppingModeNotification(context)
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // Automatically turn Shopping Mode OFF
                val stopIntent = Intent(context, ShoppingModeService::class.java).apply {
                    action = ShoppingModeService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
    }

    private fun showShoppingModeNotification(context: Context) {
        // Main tap → open MainActivity
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_shopping_mode_prompt", true)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else 0
        )

        // YES action
        val yesIntent = Intent(context, ShoppingModeActionReceiver::class.java).apply {
            action = ShoppingModeActionReceiver.ACTION_YES
        }

        val yesFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            yesIntent,
            yesFlags
        )

        // NO action
        val noIntent = Intent(context, ShoppingModeActionReceiver::class.java).apply {
            action = ShoppingModeActionReceiver.ACTION_NO
        }

        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            noIntent,
            yesFlags   // same flags as YES
        )

        val notification = NotificationCompat.Builder(
            context,
            ShopSenseApp.SHOPPING_MODE_CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher) // or any valid small icon
            .setContentTitle("ShopSense")
            .setContentText("Activate Shopping Mode?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                /* icon = */ 0,
                /* title = */ "Yes",
                /* intent = */ yesPendingIntent
            )
            .addAction(
                0,
                "No",
                noPendingIntent
            )
            .build()

        val hasPermission =
            Build.VERSION.SDK_INT < 33 ||
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(context)
                .notify(ShoppingModeActionReceiver.NOTIFICATION_ID, notification)
        }
    }
}
