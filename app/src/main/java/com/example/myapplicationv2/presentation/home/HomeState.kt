package com.example.myapplicationv2.presentation.home

import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem

data class HomeState(
    val toBuyItems: List<ToBuyItem> = emptyList(),
    val categories: List<Category> = emptyList(),

    // fields bound to Add/Edit dialog
    val itemName: String = "",
    val itemQuantity: String = "",
    val itemCategoryId: Int? = null,

    // for creating categories
    val newCategoryName: String = "",

    // editing support
    val editingItemId: Int? = null,
    // deletion support
    val pendingDeleteItem: ToBuyItem? = null
)
