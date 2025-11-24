package com.example.myapplicationv2.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceManager @Inject constructor(
    private val context: Context
) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
    }

    fun setHomeGeofence(lat: Double, lng: Double, radiusMeters: Int) {
        geofencingClient.removeGeofences(geofencePendingIntent)

        val geofence = Geofence.Builder()
            .setRequestId(HOME_GEOFENCE_ID)
            .setCircularRegion(lat, lng, radiusMeters.toFloat())
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    android.util.Log.d("GeofenceManager", "Home geofence added successfully")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("GeofenceManager", "Failed to add geofence: ${e.message}", e)
                }
        } catch (se: SecurityException) {
            android.util.Log.e("GeofenceManager", "SecurityException adding geofence", se)
        }
    }

    fun clearHomeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    companion object {
        private const val HOME_GEOFENCE_ID = "HOME_GEOFENCE"
    }
}
