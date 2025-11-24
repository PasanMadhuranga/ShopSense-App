package com.example.myapplicationv2.presentation.home

import com.example.myapplicationv2.domain.model.ToBuyItem

sealed class HomeEvent {

    data object SaveItem : HomeEvent()
    data object DeleteItem : HomeEvent()
    data class onCheckBoxClick(val toBuyItem: ToBuyItem) : HomeEvent()
    data class StartEdit(val item: ToBuyItem) : HomeEvent()
    data class StartDelete(val item: ToBuyItem) : HomeEvent()
    data class onNameChange(val name: String) : HomeEvent()
    data class onQuantityChange(val quantity: String) : HomeEvent()
    data class onCategoryChange(val categoryId: Int) : HomeEvent()
    data object StartUpdateHome : HomeEvent()
    data object UseCurrentLocation : HomeEvent()
    data class OnTempRadiusChange(val meters: Int) : HomeEvent()
    data object SaveHome : HomeEvent()
    data object ClearHome : HomeEvent()
    data object DismissUpdateHome : HomeEvent()
}