package com.example.myapplicationv2.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore("home_prefs")

object HomeKeys {
    val HOME_LAT = doublePreferencesKey("home_lat")
    val HOME_LNG = doublePreferencesKey("home_lng")
    val HOME_RADIUS_M = intPreferencesKey("home_radius_m")
}

data class HomeLocation(val lat: Double, val lng: Double, val radiusMeters: Int)

class HomePrefs(private val context: Context) {
    val homeFlow: Flow<HomeLocation?> =
        context.dataStore.data.map { prefs ->
            val lat = prefs[HomeKeys.HOME_LAT]
            val lng = prefs[HomeKeys.HOME_LNG]
            val r   = prefs[HomeKeys.HOME_RADIUS_M]
            if (lat != null && lng != null && r != null) HomeLocation(lat, lng, r) else null
        }

    suspend fun saveHome(lat: Double, lng: Double, radiusMeters: Int) {
        context.dataStore.edit { prefs ->
            prefs[HomeKeys.HOME_LAT] = lat
            prefs[HomeKeys.HOME_LNG] = lng
            prefs[HomeKeys.HOME_RADIUS_M] = radiusMeters
        }
    }

    suspend fun clearHome() {
        context.dataStore.edit { prefs ->
            prefs.remove(HomeKeys.HOME_LAT)
            prefs.remove(HomeKeys.HOME_LNG)
            prefs.remove(HomeKeys.HOME_RADIUS_M)
        }
    }
}
