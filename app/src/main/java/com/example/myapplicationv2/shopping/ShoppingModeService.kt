package com.example.myapplicationv2.shopping

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplicationv2.R
import com.example.myapplicationv2.ShopSenseApp
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

    @Inject
    lateinit var toBuyItemRepository: ToBuyItemRepository
    @Inject lateinit var categoryRepository: CategoryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val httpClient = OkHttpClient()

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
                startForeground(NOTIFICATION_ID, buildOngoingNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                Log.d("ShoppingModeService", "ACTION_STOP received")
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
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

    private fun handleNewLocation(lat: Double, lng: Double) {
        Log.d("ShoppingModeService", "New location: $lat,$lng")
        serviceScope.launch {
            performNearbySearchAndNotify(lat, lng)
        }
    }

    private suspend fun performNearbySearchAndNotify(lat: Double, lng: Double) {
        Log.d("ShoppingModeService", "performNearbySearchAndNotify() called")

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

            Log.d(
                "ShoppingModeService",
                "Processing category='${category.name}' with ${catItems.size} items"
            )

            val placesType = mapCategoryToPlacesType(category.name)
            if (placesType == null) {
                Log.w(
                    "ShoppingModeService",
                    "No Places type mapping for category='${category.name}', skipping"
                )
                continue
            }

            val place = searchClosestPlace(lat, lng, placesType)
            if (place == null) {
                Log.d(
                    "ShoppingModeService",
                    "No nearby place found for type='$placesType'"
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
                itemNames = itemNames
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
                R.mipmap.ic_launcher,              // or a custom "stop" icon if you add one
                "Off",
                stopPendingIntent
            )
            .build()
    }


    private suspend fun searchClosestPlace(
        lat: Double,
        lng: Double,
        type: String,
        radiusMeters: Int = 500
    ): NearbyPlace? {
        val apiKey = getString(R.string.google_places_web_key)
        val url = "https://places.googleapis.com/v1/places:searchNearby"

        val bodyJson = JSONObject().apply {
            put("includedTypes", JSONArray().apply { put(type) })  // <-- key fix
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

                        if (best == null || dist < best!!.distanceMeters) {
                            best = NearbyPlace(name = name, distanceMeters = dist)
                        }
                    }

                    Log.d("ShoppingModeService", "Best place for type='$type' -> $best")
                    best
                }
            } catch (e: Exception) {
                Log.e("ShoppingModeService", "Error calling Places API (New)", e)
                null
            }
        }
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

    private fun mapCategoryToPlacesType(categoryName: String): String? {
        val type = when (categoryName.lowercase()) {
            "supermarket", "grocery" -> "supermarket"
            "pharmacy" -> "pharmacy"
            "bakery" -> "bakery"
            "hardware", "hardware store" -> "hardware_store"
            else -> null
        }
        Log.d("ShoppingModeService", "mapCategoryToPlacesType('$categoryName') -> $type")
        return type
    }

    private var nearbyNotificationId = 3000

    private fun showNearbyStoreNotification(
        categoryName: String,
        placeName: String,
        distanceMeters: Int,
        itemNames: List<String>
    ) {
        val itemText = when {
            itemNames.isEmpty() -> ""
            itemNames.size <= 3 -> itemNames.joinToString()
            else -> itemNames.take(3).joinToString() + "…"
        }

        val text =
            "There is a $categoryName in $distanceMeters meters ($placeName). " +
                    "You can buy $itemText."

        Log.d("ShoppingModeService", "Showing nearby-store notification: $text")

        val notification = NotificationCompat.Builder(this, ShopSenseApp.SHOPPING_MODE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nearby $categoryName")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
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

        private const val UPDATE_INTERVAL_MS: Long = 30 * 1000   // every 30 seconds
        private const val MIN_DISTANCE_METERS = 100f            // or after 100 m moved
    }

    private data class NearbyPlace(
        val name: String,
        val distanceMeters: Int
    )
}
