package com.example.myapplicationv2.presentation.home

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.domain.repository.CategoryRepository
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import com.example.myapplicationv2.util.SnackBarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val toBuyItemRepository: ToBuyItemRepository,
    private val categoryRepository: CategoryRepository
): ViewModel() {

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
        }
    }

    private val defaultCategories = listOf(
        Category(name = "Supermarket"),
        Category(name = "Pharmacy"),
        Category(name = "Bakery"),
        Category(name = "Butchery"),
        Category(name = "Greengrocer"),
        Category(name = "Electronics"),
        Category(name = "Household"),
        Category(name = "Stationery"),
        Category(name = "Pet Store"),
        Category(name = "Other")
    )

    init {
        seedCategoriesIfEmpty()
    }

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val existing = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                try {
                    categoryRepository.insertAll(defaultCategories)
                } catch (e: Exception) {
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackBar("Failed to add default categories: ${e.message}")
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
                    checked = s.toBuyItems.firstOrNull { it.id == s.editingItemId }?.checked ?: false
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
                        message = if (s.editingItemId == null) "Item added successfully." else "Item updated successfully."
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
