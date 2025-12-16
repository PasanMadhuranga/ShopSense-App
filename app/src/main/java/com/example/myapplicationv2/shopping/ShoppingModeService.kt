package com.example.myapplicationv2.shopping

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplicationv2.R
import com.example.myapplicationv2.ShopSenseApp
import com.example.myapplicationv2.data.local.prefs.HomePrefs
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.domain.repository.CategoryRepository
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@AndroidEntryPoint
class ShoppingModeService : Service() {

    @Inject lateinit var toBuyItemRepository: ToBuyItemRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var homePrefs: HomePrefs

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val httpClient = OkHttpClient()
    private var lastSearchLat: Double? = null
    private var lastSearchLng: Double? = null
    private var lastLocation: Location? = null
    private val lastNotificationTimestamps = mutableMapOf<String, Long>()

    private var snoozeJob: Job? = null
    private var isSnoozed: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        snoozeJob?.cancel()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "ACTION_START received")
                isSnoozed = false
                snoozeJob?.cancel()
                snoozeJob = null

                serviceScope.launch {
                    homePrefs.setShoppingMode(on = true, manual = false)
                }

                startForeground(NOTIFICATION_ID, buildOngoingNotification(isSnoozed = false))
                startLocationUpdates()
            }

            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received")

                isSnoozed = false
                snoozeJob?.cancel()
                snoozeJob = null

                serviceScope.launch {
                    homePrefs.setShoppingMode(on = false, manual = false)
                }

                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                sendBroadcast(Intent(ACTION_UI_STOPPED))
            }

            ACTION_SNOOZE -> {
                Log.d(TAG, "ACTION_SNOOZE received")

                // Cancel any existing snooze timer and enter snooze state
                snoozeJob?.cancel()
                snoozeJob = null
                isSnoozed = true

                // Turn OFF toggle reliably (do it from a coroutine that won't be cancelled by stopSelf, since we DO NOT stopSelf now)
                serviceScope.launch {
                    homePrefs.setShoppingMode(on = false, manual = false)
                }
                sendBroadcast(Intent(ACTION_UI_STOPPED))

                // Pause location updates, keep service alive in foreground with "Snoozed" notification
                stopLocationUpdates()
                startForeground(NOTIFICATION_ID, buildOngoingNotification(isSnoozed = true))

                // Resume after delay inside the same service (most reliable)
                snoozeJob = serviceScope.launch {
                    Log.d(TAG, "Snoozing for ${SNOOZE_DURATION_MS} ms")
                    delay(SNOOZE_DURATION_MS)

                    Log.d(TAG, "Snooze finished, resuming Shopping Mode")
                    isSnoozed = false

                    homePrefs.setShoppingMode(on = true, manual = false)

                    withContext(Dispatchers.Main) {
                        startForeground(NOTIFICATION_ID, buildOngoingNotification(isSnoozed = false))
                        startLocationUpdates()
                    }
                }
            }

            else -> {
                Log.d(TAG, "Unknown action: ${intent?.action}")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                handleNewLocation(location)
            }
        }
    }

    private fun startLocationUpdates() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            Log.w(TAG, "No location permission, stopping updates")
            // Keep service running but reflect OFF to avoid UI mismatch
            serviceScope.launch { homePrefs.setShoppingMode(on = false, manual = false) }
            sendBroadcast(Intent(ACTION_UI_STOPPED))
            return
        }

        Log.d(TAG, "Requesting location updates")

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MS
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
        runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    private fun handleNewLocation(location: Location) {
        if (isSnoozed) return

        val lat = location.latitude
        val lng = location.longitude
        val speedMps = location.speed

        Log.d(TAG, "New location: $lat,$lng speed=${speedMps} m/s")

        serviceScope.launch {
            val prevLat = lastSearchLat
            val prevLng = lastSearchLng

            if (prevLat != null && prevLng != null) {
                val dist = distanceMeters(prevLat, prevLng, lat, lng)
                if (dist < MIN_DISTANCE_METERS) return@launch
            }

            val previousLocation = lastLocation
            val movementBearing: Float? =
                if (previousLocation != null && speedMps >= SPEED_MIN_FOR_HEADING) {
                    previousLocation.bearingTo(location)
                } else null

            val radiusMeters = computeSearchRadiusMeters(speedMps)

            performNearbySearchAndNotify(
                lat = lat,
                lng = lng,
                radiusMeters = radiusMeters,
                movementBearing = movementBearing
            )

            lastSearchLat = lat
            lastSearchLng = lng
            lastLocation = Location(location)
        }
    }

    private fun computeSearchRadiusMeters(speedMps: Float): Int {
        val s = if (speedMps.isNaN() || speedMps < 0f) 0f else speedMps
        return when {
            s < 1f -> 150
            s < 3f -> 300
            else -> 500
        }
    }

    private fun buildOngoingNotification(isSnoozed: Boolean): Notification {
        val stopIntent = Intent(this, ShoppingModeService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, ShoppingModeService::class.java).apply { action = ACTION_SNOOZE }
        val snoozePendingIntent = PendingIntent.getService(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (isSnoozed) "Shopping Mode is snoozed" else "Shopping Mode is active"

        val builder = NotificationCompat.Builder(this, ShopSenseApp.SHOPPING_MODE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ShopSense")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.mipmap.ic_launcher, "Off", stopPendingIntent)

        if (!isSnoozed) {
            builder.addAction(R.mipmap.ic_launcher, "Snooze", snoozePendingIntent)
        }

        return builder.build()
    }

    // ---- Your existing Places logic stays the same below this point ----

    private suspend fun performNearbySearchAndNotify(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        movementBearing: Float?
    ) {
        val items: List<ToBuyItem> = toBuyItemRepository
            .getAllToBuyItems()
            .first()
            .filter { !it.checked }

        if (items.isEmpty()) return

        val categories: List<Category> = categoryRepository.getAllCategories().first()
        val itemsByCategoryId: Map<Int?, List<ToBuyItem>> = items.groupBy { it.categoryId }

        for ((categoryId, catItems) in itemsByCategoryId) {
            val category = categories.find { it.id == categoryId } ?: continue
            if (category.name == "Other") continue

            val place = searchClosestPlace(
                lat = lat,
                lng = lng,
                type = category.name,
                radiusMeters = radiusMeters,
                movementBearingDeg = movementBearing
            ) ?: continue

            showNearbyStoreNotification(
                categoryName = category.name,
                placeName = place.name,
                distanceMeters = place.distanceMeters,
                itemNames = catItems.map { it.name },
                itemIds = catItems.mapNotNull { it.id },
                placeLat = place.lat,
                placeLng = place.lng
            )
        }
    }

    private suspend fun searchClosestPlace(
        lat: Double,
        lng: Double,
        type: String,
        radiusMeters: Int = 200,
        movementBearingDeg: Float? = null
    ): NearbyPlace? {
        val apiKey = getString(R.string.google_places_web_key)
        val url = "https://places.googleapis.com/v1/places:searchNearby"
        val includedTypes = expandPlacesTypes(type)

        val bodyJson = JSONObject().apply {
            put("includedTypes", JSONArray().apply { includedTypes.forEach { put(it) } })
            put("maxResultCount", 5)
            put("locationRestriction", JSONObject().apply {
                put("circle", JSONObject().apply {
                    put("center", JSONObject().apply {
                        put("latitude", lat)
                        put("longitude", lng)
                    })
                    put("radius", radiusMeters)
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = bodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "places.displayName,places.location")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)

                    val places = json.optJSONArray("places") ?: return@withContext null
                    if (places.length() == 0) return@withContext null

                    var best: NearbyPlace? = null
                    for (i in 0 until places.length()) {
                        val placeObj = places.getJSONObject(i)

                        val name = placeObj.optJSONObject("displayName")?.optString("text") ?: continue
                        val locObj = placeObj.getJSONObject("location")
                        val placeLat = locObj.getDouble("latitude")
                        val placeLng = locObj.getDouble("longitude")

                        val dist = distanceMeters(lat, lng, placeLat, placeLng).toInt()

                        if (movementBearingDeg != null) {
                            val bearingToPlace = bearingBetween(lat, lng, placeLat, placeLng)
                            val angleDiff = smallestAngleDiff(movementBearingDeg, bearingToPlace)
                            if (angleDiff > HEADING_MAX_ANGLE_DEG) continue
                        }

                        if (best == null || dist < best!!.distanceMeters) {
                            best = NearbyPlace(name, dist, placeLat, placeLng)
                        }
                    }
                    best
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun expandPlacesTypes(baseType: String): List<String> {
        return when (baseType) {
            "Supermarket" -> listOf("supermarket", "grocery_store")
            "Pharmacy" -> listOf("pharmacy")
            "Bakery" -> listOf("bakery", "cafe")
            "Electronics" -> listOf("electronics_store")
            "Household" -> listOf("home_goods_store", "furniture_store")
            "Stationery" -> listOf("book_store")
            "Pet Store" -> listOf("pet_store", "veterinary_care")
            else -> listOf(baseType)
        }
    }

    private fun bearingBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(2)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[1]
    }

    private fun smallestAngleDiff(aDeg: Float, bDeg: Float): Float {
        var diff = (aDeg - bDeg) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return kotlin.math.abs(diff)
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    private var nearbyNotificationId = 3000

    private fun showNearbyStoreNotification(
        categoryName: String,
        placeName: String,
        distanceMeters: Int,
        itemNames: List<String>,
        itemIds: List<Int>,
        placeLat: Double,
        placeLng: Double
    ) {
        val notificationKey = "$categoryName|$placeName"
        val now = System.currentTimeMillis()
        val lastTime = lastNotificationTimestamps[notificationKey]
        if (lastTime != null && now - lastTime < NOTIFY_COOLDOWN_MS) return
        lastNotificationTimestamps[notificationKey] = now

        val bigText = buildString {
            append("$placeName is $distanceMeters meters away.\n")
            append("You can buy:\n")
            append(itemNames.joinToString("\n") { "• $it" })
        }

        val launchIntent = Intent(this, com.example.myapplicationv2.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 1, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val navUri = Uri.parse("google.navigation:q=$placeLat,$placeLng&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        val finalIntent =
            if (navIntent.resolveActivity(packageManager) != null) navIntent
            else Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$placeLat,$placeLng"))

        val directionPendingIntent = PendingIntent.getActivity(
            this, 0, finalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ShopSenseApp.SHOPPING_MODE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nearby $categoryName")
            .setContentText("$placeName is $distanceMeters meters away")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Show direction", directionPendingIntent)
            .build()

        val hasPermission =
            Build.VERSION.SDK_INT < 33 ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(this).notify(nearbyNotificationId++, notification)
        }
    }

    companion object {
        private const val TAG = "ShoppingModeService"

        const val ACTION_START = "com.example.myapplicationv2.shopping.ACTION_START"
        const val ACTION_STOP = "com.example.myapplicationv2.shopping.ACTION_STOP"
        const val ACTION_SNOOZE = "com.example.myapplicationv2.shopping.ACTION_SNOOZE"

        const val ACTION_UI_STOPPED = "SHOPPING_MODE_STOPPED"

        const val NOTIFICATION_ID = 2001

        private const val UPDATE_INTERVAL_MS: Long = 10 * 1000
        private const val MIN_DISTANCE_METERS = 100f

        const val HEADING_MAX_ANGLE_DEG = 60f
        const val SPEED_MIN_FOR_HEADING = 0.5f
        const val NOTIFY_COOLDOWN_MS = 2 * 60 * 1000L

        const val SNOOZE_DURATION_MS = 2 * 60 * 1000L  // test value (2 minutes)
    }

    private data class NearbyPlace(
        val name: String,
        val distanceMeters: Int,
        val lat: Double,
        val lng: Double
    )
}
