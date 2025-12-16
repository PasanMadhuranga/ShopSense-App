package com.example.myapplicationv2.presentation.home

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationv2.data.local.prefs.HomePrefs
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.domain.repository.CategoryRepository
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import com.example.myapplicationv2.geofence.GeofenceManager
import com.example.myapplicationv2.location.LocationProvider
import com.example.myapplicationv2.shopping.ShoppingModeService
import com.example.myapplicationv2.util.SnackBarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val app: Application,
    private val toBuyItemRepository: ToBuyItemRepository,
    private val categoryRepository: CategoryRepository,
    private val homePrefs: HomePrefs,
    private val locationProvider: LocationProvider,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())

    val state = combine(
        _state,
        toBuyItemRepository.getAllToBuyItems(),
        categoryRepository.getAllCategories()
    ) { state, toBuyItems, categories ->
        state.copy(
            toBuyItems = toBuyItems,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackBarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            homePrefs.homeFlow.collect { saved ->
                _state.update {
                    it.copy(
                        homeLat = saved?.lat,
                        homeLng = saved?.lng,
                        homeRadiusMeters = saved?.radiusMeters ?: it.homeRadiusMeters
                    )
                }
            }
        }

        viewModelScope.launch {
            homePrefs.shoppingModeOn.collect { isOn ->
                _state.update { it.copy(isShoppingModeActive = isOn) }
            }
        }

        seedCategoriesIfEmpty()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.DeleteItem -> deleteItem()
            HomeEvent.SaveItem -> saveItem()

            is HomeEvent.StartEdit -> {
                _state.update {
                    it.copy(
                        itemName = event.item.name,
                        itemQuantity = event.item.quantity.toString(),
                        itemCategoryId = event.item.categoryId,
                        editingItemId = event.item.id
                    )
                }
            }

            is HomeEvent.StartDelete -> {
                _state.update { it.copy(pendingDeleteItem = event.item) }
            }

            is HomeEvent.onCheckBoxClick -> toggleChecked(event.toBuyItem)

            is HomeEvent.onCategoryChange -> {
                _state.update { it.copy(itemCategoryId = event.categoryId) }
            }

            is HomeEvent.onNameChange -> {
                _state.update { it.copy(itemName = event.name) }
            }

            is HomeEvent.onQuantityChange -> {
                _state.update { it.copy(itemQuantity = event.quantity) }
            }

            HomeEvent.StartUpdateHome -> _state.update {
                it.copy(
                    isUpdatingHome = true,
                    tempRadiusMeters = it.homeRadiusMeters
                )
            }

            is HomeEvent.OnTempRadiusChange ->
                _state.update { it.copy(tempRadiusMeters = event.meters) }

            HomeEvent.UseCurrentLocation -> useCurrentLocation()

            HomeEvent.SaveHome -> saveHome()

            HomeEvent.ClearHome -> clearHome()

            HomeEvent.DismissUpdateHome ->
                _state.update { it.copy(isUpdatingHome = false, isLocationLoading = false) }

            HomeEvent.StartSelectHomeOnMap -> {
                _state.update { it.copy(isSelectingHomeOnMap = true) }
            }

            HomeEvent.ToggleShoppingMode -> {
                val active = state.value.isShoppingModeActive
                if (active) stopShoppingMode()
                else startShoppingMode()
            }

            HomeEvent.SetShoppingModeOn -> {
                _state.update { it.copy(isShoppingModeActive = true) }
                viewModelScope.launch {
                    homePrefs.setShoppingMode(on = true, manual = true)
                }
            }

            HomeEvent.SetShoppingModeOff -> {
                _state.update { it.copy(isShoppingModeActive = false) }
                viewModelScope.launch {
                    homePrefs.setShoppingMode(on = false, manual = false)
                }
            }

            is HomeEvent.OnHomeLocationSelected -> {
                val radius = state.value.homeRadiusMeters
                Log.d("HomeViewModel", "Radius: $radius, lat: ${event.lat}, lng: ${event.lng}")

                viewModelScope.launch {
                    try {
                        homePrefs.saveHome(event.lat, event.lng, radius)

                        if (hasLocationPermission()) {
                            geofenceManager.setHomeGeofence(event.lat, event.lng, radius)
                        } else {
                            _snackbarEventFlow.emit(
                                SnackBarEvent.ShowSnackBar(
                                    "Home saved, but location permission is needed to enable geofence."
                                )
                            )
                        }

                        _state.update {
                            it.copy(
                                homeLat = event.lat,
                                homeLng = event.lng,
                                isSelectingHomeOnMap = false
                            )
                        }

                        _snackbarEventFlow.emit(
                            SnackBarEvent.ShowSnackBar("Home updated from map.")
                        )
                    } catch (e: Exception) {
                        _snackbarEventFlow.emit(
                            SnackBarEvent.ShowSnackBar("Failed to save home: ${e.message}")
                        )
                    }
                }
            }
        }
    }

    private val defaultCategories = listOf(
        Category(name = "Supermarket"),
        Category(name = "Pharmacy"),
        Category(name = "Bakery"),
        Category(name = "Electronics"),
        Category(name = "Household"),
        Category(name = "Stationery"),
        Category(name = "Pet Store"),
        Category(name = "Other")
    )

    private fun startShoppingMode() {
        viewModelScope.launch {
            homePrefs.setShoppingMode(on = true, manual = true)
        }

        _state.update { it.copy(isShoppingModeActive = true) }

        val intent = Intent(app, ShoppingModeService::class.java).apply {
            action = ShoppingModeService.ACTION_START
        }
        ContextCompat.startForegroundService(app, intent)
    }

    private fun stopShoppingMode() {
        viewModelScope.launch {
            homePrefs.setShoppingMode(on = false, manual = false)
        }

        _state.update { it.copy(isShoppingModeActive = false) }

        val intent = Intent(app, ShoppingModeService::class.java).apply {
            action = ShoppingModeService.ACTION_STOP
        }
        app.startService(intent)
    }

    private fun useCurrentLocation() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLocationLoading = true) }
                val loc = locationProvider.getLastLocationOrNull()
                if (loc == null) {
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackBar(
                            "Location unavailable. Open Maps or try outside."
                        )
                    )
                } else {
                    _state.update { it.copy(homeLat = loc.latitude, homeLng = loc.longitude) }
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackBar("Pinned to current location.")
                    )
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar("Failed to get location: ${e.message}")
                )
            } finally {
                _state.update { it.copy(isLocationLoading = false) }
            }
        }
    }

    private fun saveHome() {
        val s = state.value
        val lat = s.homeLat
        val lng = s.homeLng
        if (lat == null || lng == null) {
            viewModelScope.launch {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar("Pick a location first.")
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                homePrefs.saveHome(lat, lng, s.tempRadiusMeters)

                if (hasLocationPermission()) {
                    geofenceManager.setHomeGeofence(lat, lng, s.tempRadiusMeters)
                } else {
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackBar(
                            "Home saved, but location permission is needed to enable geofence."
                        )
                    )
                }

                _state.update {
                    it.copy(
                        isUpdatingHome = false,
                        homeRadiusMeters = s.tempRadiusMeters
                    )
                }
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar("Home updated.")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar("Failed to save home: ${e.message}")
                )
            }
        }
    }

    private fun clearHome() {
        viewModelScope.launch {
            homePrefs.clearHome()
            geofenceManager.clearHomeGeofence()
            _state.update { it.copy(homeLat = null, homeLng = null) }
            _snackbarEventFlow.emit(
                SnackBarEvent.ShowSnackBar("Home cleared.")
            )
        }
    }

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val existing = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                try {
                    categoryRepository.insertAll(defaultCategories)
                    val categories = categoryRepository.getAllCategories().firstOrNull()
                    Log.d("HomeViewModel", "Default categories added: $categories")
                } catch (e: Exception) {
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackBar(
                            "Failed to add default categories: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun saveItem() {
        val s = state.value
        val quantity = s.itemQuantity.toIntOrNull()
        val categoryId = s.itemCategoryId
        val name = s.itemName.trim()

        if (name.isBlank()) return
        if (quantity == null) return
        if (categoryId == null) return

        viewModelScope.launch {
            try {
                val item = ToBuyItem(
                    id = s.editingItemId,
                    name = name,
                    quantity = quantity,
                    categoryId = categoryId,
                    checked = s.toBuyItems.firstOrNull { it.id == s.editingItemId }?.checked
                        ?: false
                )

                toBuyItemRepository.upsertToBuyItem(item)

                _state.update {
                    it.copy(
                        itemName = "",
                        itemQuantity = "",
                        itemCategoryId = null,
                        editingItemId = null
                    )
                }

                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = if (s.editingItemId == null)
                            "Item added successfully."
                        else
                            "Item updated successfully."
                    )
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = "Couldn't save item. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun toggleChecked(item: ToBuyItem) {
        viewModelScope.launch {
            try {
                toBuyItemRepository.upsertToBuyItem(item.copy(checked = !item.checked))
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = "Couldn't update item. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun deleteItem() {
        val item = state.value.pendingDeleteItem ?: return
        viewModelScope.launch {
            try {
                toBuyItemRepository.deleteToBuyItem(item)

                _state.update { it.copy(pendingDeleteItem = null) }

                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(message = "Item deleted.")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = "Couldn't delete item. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }
}
