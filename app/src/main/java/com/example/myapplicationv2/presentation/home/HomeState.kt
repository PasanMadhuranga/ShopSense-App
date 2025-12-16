package com.example.myapplicationv2.presentation.home

import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem

data class HomeState(
    val toBuyItems: List<ToBuyItem> = emptyList(),
    val categories: List<Category> = emptyList(),

    val itemName: String = "",
    val itemQuantity: String = "",
    val itemCategoryId: Int? = null,

    val editingItemId: Int? = null,
    val pendingDeleteItem: ToBuyItem? = null,

    val homeLat: Double? = null,
    val homeLng: Double? = null,
    val homeRadiusMeters: Int = 200,

    val tempRadiusMeters: Int = 200,
    val isUpdatingHome: Boolean = false,
    val isLocationLoading: Boolean = false,

    val isSelectingHomeOnMap: Boolean = false,

    val isShoppingModeActive: Boolean = false
)
