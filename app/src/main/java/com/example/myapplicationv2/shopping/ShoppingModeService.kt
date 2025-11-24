package com.example.myapplicationv2.shopping

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.myapplicationv2.R
import com.example.myapplicationv2.ShopSenseApp
import com.google.android.gms.location.*

class ShoppingModeService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildOngoingNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                handleNewLocation(location.latitude, location.longitude)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,   // priority FIRST
            UPDATE_INTERVAL_MS                           // interval SECOND (ms)
        )
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        runCatching {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun handleNewLocation(lat: Double, lng: Double) {
        // This is where you:
        // 1. Read current to-buy items + categories from Room (later, via Hilt injection)
        // 2. Call a Places API "nearby search" for relevant store types
        // 3. If a relevant store within Z meters is found, send a notification like:
        //    "There is a supermarket in 500 m. You can buy milk, eggs, onion"

        // For now, we keep it as a stub:
        performNearbySearchStub(lat, lng)
    }

    private fun performNearbySearchStub(lat: Double, lng: Double) {
        // TODO: Replace this with real Places API search.
        // Keeping a stub to avoid breaking the build.
    }

    private fun buildOngoingNotification(): Notification {
        // Small persistent notification showing Shopping Mode is active
        return NotificationCompat.Builder(this, ShopSenseApp.SHOPPING_MODE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ShopSense")
            .setContentText("Shopping Mode is active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.myapplicationv2.shopping.ACTION_START"
        const val ACTION_STOP = "com.example.myapplicationv2.shopping.ACTION_STOP"
        const val NOTIFICATION_ID = 2001

        // Tunable params
        private const val UPDATE_INTERVAL_MS: Long = 2 * 60 * 1000   // every 2 minutes
        private const val MIN_DISTANCE_METERS = 100f            // or after 100 m moved
    }
}
