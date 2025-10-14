package com.example.myapplicationv2.presentation.home

import com.example.myapplicationv2.domain.model.ToBuyItem

sealed class HomeEvent {

    data object SaveItem : HomeEvent()

    data object SaveCategory : HomeEvent()

    data object DeleteItem : HomeEvent()

    data class onCheckBoxClick(val toBuyItem: ToBuyItem) : HomeEvent()

    data class onNameChange(val name: String) : HomeEvent()

    data class onQuantityChange(val quantity: String) : HomeEvent()

    data class onCategoryChange(val categoryId: Int) : HomeEvent()

    data class onNewCategoryNameChange(val name: String) : HomeEvent()

}