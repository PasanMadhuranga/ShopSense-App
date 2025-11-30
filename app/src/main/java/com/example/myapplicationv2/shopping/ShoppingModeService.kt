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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@AndroidEntryPoint
class ShoppingModeService : Service() {

    @Inject lateinit var toBuyItemRepository: ToBuyItemRepository
    @Inject lateinit var categoryRepository: CategoryRepository

    // NEW: prefs
    @Inject lateinit var homePrefs: HomePrefs

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val httpClient = OkHttpClient()
    private var lastSearchLat: Double? = null
    private var lastSearchLng: Double? = null
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d("ShoppingModeService", "ACTION_START received")

                // If the service is started from notification / geofence (not from ViewModel),
                // we still want prefs to reflect that Shopping Mode is ON (manual = false).
                serviceScope.launch {
                    homePrefs.setShoppingMode(on = true, manual = false)
                }

                startForeground(NOTIFICATION_ID, buildOngoingNotification())
                startLocationUpdates()
            }

            ACTION_STOP -> {
                Log.d("ShoppingModeService", "ACTION_STOP received")

                // Make sure prefs say OFF
                serviceScope.launch {
                    homePrefs.setShoppingMode(on = false, manual = false)
                }

                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                // Let UI know that the service stopped, so the toggle can turn off
                sendBroadcast(Intent("SHOPPING_MODE_STOPPED"))
            }

            else -> {
                Log.d("ShoppingModeService", "Unknown action: ${intent?.action}")
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ShoppingModeService", "No location permission, stopping service")
            stopSelf()
            return
        }

        Log.d("ShoppingModeService", "Requesting location updates")

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
        runCatching {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun handleNewLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val speedMps = location.speed        // may be 0 or unreliable at very low speed

        Log.d(
            "ShoppingModeService",
            "New location: $lat,$lng speed=${speedMps} m/s"
        )

        serviceScope.launch {
            val prevLat = lastSearchLat
            val prevLng = lastSearchLng

            // Distance gating (same as your current logic)
            if (prevLat != null && prevLng != null) {
                val dist = distanceMeters(prevLat, prevLng, lat, lng)
                Log.d("ShoppingModeService", "Distance from last search location: $dist m")

                if (dist < MIN_DISTANCE_METERS) {
                    Log.d(
                        "ShoppingModeService",
                        "Movement < $MIN_DISTANCE_METERS m, skipping search"
                    )
                    return@launch
                }
            }

            // Compute movement bearing only if we have a previous location and reasonable speed
            val previousLocation = lastLocation
            val movementBearing: Float? =
                if (previousLocation != null && speedMps >= SPEED_MIN_FOR_HEADING) {
                    previousLocation.bearingTo(location)   // 0..360 degrees
                } else {
                    null
                }

            Log.d(
                "ShoppingModeService",
                "Computed movementBearing=$movementBearing"
            )

            val radiusMeters = computeSearchRadiusMeters(speedMps)

            performNearbySearchAndNotify(
                lat = lat,
                lng = lng,
                radiusMeters = radiusMeters,
                movementBearing = movementBearing
            )

            // Update markers for next step
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

    private suspend fun performNearbySearchAndNotify(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        movementBearing: Float?
    ) {
        Log.d("ShoppingModeService", "performNearbySearchAndNotify() called with radius=$radiusMeters")

        val items: List<ToBuyItem> = toBuyItemRepository
            .getAllToBuyItems()
            .first()
            .filter { !it.checked }

        Log.d("ShoppingModeService", "Unchecked items count: ${items.size}")

        if (items.isEmpty()) {
            Log.d("ShoppingModeService", "No unchecked items, skipping search")
            return
        }

        val categories: List<Category> = categoryRepository
            .getAllCategories()
            .first()

        Log.d("ShoppingModeService", "Categories count: ${categories.size}")

        val itemsByCategoryId: Map<Int?, List<ToBuyItem>> =
            items.groupBy { it.categoryId }

        Log.d(
            "ShoppingModeService",
            "Items grouped into ${itemsByCategoryId.size} category groups"
        )

        for ((categoryId, catItems) in itemsByCategoryId) {
            val category = categories.find { it.id == categoryId }
            if (category == null) {
                Log.w(
                    "ShoppingModeService",
                    "No Category found for categoryId=$categoryId, skipping group"
                )
                continue
            }

            if (category.name == "Other") {
                Log.d(
                    "ShoppingModeService",
                    "Skipping search for 'Other' category"
                )
                continue
            }

            Log.d(
                "ShoppingModeService",
                "Processing category='${category.name}' with ${catItems.size} items"
            )

            val place = searchClosestPlace(
                lat = lat,
                lng = lng,
                type = category.name,
                radiusMeters = radiusMeters,
                movementBearingDeg = movementBearing
            )

            if (place == null) {
                Log.d(
                    "ShoppingModeService",
                    "No nearby place found for ${category.name}, skipping"
                )
                continue
            }

            val itemNames = catItems.map { item -> item.name }

            Log.d(
                "ShoppingModeService",
                "Closest place for '${category.name}': ${place.name}, " +
                        "distance=${place.distanceMeters} m, items=${itemNames.joinToString()}"
            )

            showNearbyStoreNotification(
                categoryName = category.name,
                placeName = place.name,
                distanceMeters = place.distanceMeters,
                itemNames = itemNames,
                itemIds = catItems.mapNotNull { it.id },
                placeLat = place.lat,
                placeLng = place.lng
            )
        }
    }

    private fun buildOngoingNotification(): Notification {
        val stopIntent = Intent(this, ShoppingModeService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ShopSenseApp.SHOPPING_MODE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ShopSense")
            .setContentText("Shopping Mode is active")
            .setOngoing(true)                      // keeps it stuck in the shade
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(stopPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Off",
                stopPendingIntent
            )
            .build()
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

        // Expand the base type into a list of related types
        val includedTypes = expandPlacesTypes(type)

        val bodyJson = JSONObject().apply {
            put("includedTypes", JSONArray().apply {
                includedTypes.forEach { put(it) }
            })
            put("maxResultCount", 10)
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

        Log.d("ShoppingModeService", "Calling Places Nearby (New): $url")
        Log.d("ShoppingModeService", "Request body: $bodyJson")

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
                    if (!response.isSuccessful) {
                        Log.w("ShoppingModeService", "Places (New) response not successful: ${response.code}")
                        val errorBody = response.body?.string()
                        Log.w("ShoppingModeService", "Error body: $errorBody")
                        return@withContext null
                    }

                    val body = response.body?.string() ?: run {
                        Log.w("ShoppingModeService", "Places (New) response body is null")
                        return@withContext null
                    }

                    val json = JSONObject(body)
                    Log.d("ShoppingModeService", "Places (New) raw response: $json")

                    val places = json.optJSONArray("places")
                    if (places == null || places.length() == 0) {
                        Log.d("ShoppingModeService", "No Places (New) results")
                        return@withContext null
                    }

                    var best: NearbyPlace? = null
                    for (i in 0 until places.length()) {
                        val placeObj = places.getJSONObject(i)

                        val name = placeObj
                            .optJSONObject("displayName")
                            ?.optString("text")
                            ?: continue

                        val locObj = placeObj.getJSONObject("location")
                        val placeLat = locObj.getDouble("latitude")
                        val placeLng = locObj.getDouble("longitude")

                        val dist = distanceMeters(lat, lng, placeLat, placeLng).toInt()
                        Log.d("ShoppingModeService", "Candidate place: $name, distance=$dist m")

                        if (movementBearingDeg != null) {
                            val bearingToPlace = bearingBetween(lat, lng, placeLat, placeLng)
                            val angleDiff = smallestAngleDiff(movementBearingDeg, bearingToPlace)

                            Log.d(
                                "ShoppingModeService",
                                "Candidate: $name, dist=$dist, bearingToPlace=$bearingToPlace, " +
                                        "movementBearing=$movementBearingDeg, angleDiff=$angleDiff"
                            )

                            if (angleDiff > HEADING_MAX_ANGLE_DEG) {
                                // Mostly to the side or behind the user
                                continue
                            }
                        } else {
                            Log.d(
                                "ShoppingModeService",
                                "Candidate: $name, dist=$dist (no heading filter applied)"
                            )
                        }

                        if (best == null || dist < best!!.distanceMeters) {
                            best = NearbyPlace(
                                name = name,
                                distanceMeters = dist,
                                lat = placeLat,
                                lng = placeLng
                            )
                        }
                    }

                    Log.d(
                        "ShoppingModeService",
                        "Best place for types=${includedTypes.joinToString()} -> $best"
                    )
                    best
                }
            } catch (e: Exception) {
                Log.e("ShoppingModeService", "Error calling Places API (New)", e)
                null
            }
        }
    }

    /**
     * Expand a base Places type into a list of related types to search together.
     * All strings here must be valid Places API types.
     */
    private fun expandPlacesTypes(baseType: String): List<String> {
        return when (baseType){
            "Supermarket" -> listOf(
                "supermarket",
                "grocery_store",
                "department_store"
            )

            "Pharmacy" -> listOf(
                "pharmacy"
            )

            "Bakery" -> listOf(
                "bakery",
                "cafe"
            )

            "Electronics" -> listOf(
                "electronics_store"
            )

            "Household" -> listOf(
                "home_goods_store",
                "furniture_store"
            )

            "Stationery" -> listOf(
                "stationery_store",
                "book_store"
            )

            "Pet Store" -> listOf(
                "pet_store",
                "veterinary_care"
            )

            // Fallback: just use whatever came in
            else -> listOf(baseType)
        }.also {
            Log.d("ShoppingModeService", "expandPlacesTypes('$baseType') -> $it")
        }
    }

    private fun bearingBetween(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val result = FloatArray(2)
        // result[1] will be the initial bearing from (lat1,lng1) to (lat2,lng2)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[1]
    }

    /**
     * Returns the smallest absolute angle difference between two headings in degrees.
     * Both inputs are in degrees, 0..360.
     */
    private fun smallestAngleDiff(aDeg: Float, bDeg: Float): Float {
        var diff = (aDeg - bDeg) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return kotlin.math.abs(diff)
    }

    private fun distanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
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
        val bulletItems = if (itemNames.isEmpty()) {
            "• (no items specified)"
        } else {
            itemNames.joinToString("\n") { "• $it" }
        }

        val bigText = buildString {
            append("$placeName is $distanceMeters meters away.\n")
            append("You can buy:\n")
            append(bulletItems)
        }

        Log.d("ShoppingModeService", "Showing nearby-store notification: $bigText")

        // Intent to open the app and highlight the items
        val launchIntent = Intent(this, com.example.myapplicationv2.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HIGHLIGHT_FROM_NOTIFICATION, true)
            putIntegerArrayListExtra(
                EXTRA_HIGHLIGHT_ITEM_IDS,
                ArrayList(itemIds)
            )
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // navigation intent code stays the same...
        val navUri = Uri.parse("google.navigation:q=$placeLat,$placeLng&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        val finalIntent =
            if (navIntent.resolveActivity(packageManager) != null) {
                navIntent
            } else {
                val mapsUrl =
                    "https://www.google.com/maps/dir/?api=1&destination=$placeLat,$placeLng"
                Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
            }

        val directionPendingIntent = PendingIntent.getActivity(
            this,
            0,
            finalIntent,
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
            .addAction(
                R.mipmap.ic_launcher,
                "Show direction",
                directionPendingIntent
            )
            .build()

        val hasPermission =
            Build.VERSION.SDK_INT < 33 ||
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(this)
                .notify(nearbyNotificationId++, notification)
        } else {
            Log.w("ShoppingModeService", "Notification permission missing, not showing notification")
        }
    }

    companion object {
        const val ACTION_START = "com.example.myapplicationv2.shopping.ACTION_START"
        const val ACTION_STOP = "com.example.myapplicationv2.shopping.ACTION_STOP"
        const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS: Long = 10 * 1000   // every 10 seconds
        private const val MIN_DISTANCE_METERS = 100f            // or after 100 m moved
        const val EXTRA_HIGHLIGHT_FROM_NOTIFICATION =
            "com.example.myapplicationv2.EXTRA_HIGHLIGHT_FROM_NOTIFICATION"
        const val EXTRA_HIGHLIGHT_ITEM_IDS =
            "com.example.myapplicationv2.EXTRA_HIGHLIGHT_ITEM_IDS"
        const val HEADING_MAX_ANGLE_DEG = 60f
        const val SPEED_MIN_FOR_HEADING = 0.5f
    }

    private data class NearbyPlace(
        val name: String,
        val distanceMeters: Int,
        val lat: Double,
        val lng: Double
    )
}
