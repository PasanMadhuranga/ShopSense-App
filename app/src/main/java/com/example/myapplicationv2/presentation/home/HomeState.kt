package com.example.myapplicationv2.presentation.home

import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem

data class HomeState(
    val toBuyItems: List<ToBuyItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val itemName: String = "",
    val itemQuantity: String = "",
    val itemCategory: String = "",
    val newCategoryName: String = ""
)
