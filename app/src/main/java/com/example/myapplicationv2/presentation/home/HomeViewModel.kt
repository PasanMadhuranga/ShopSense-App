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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
            HomeEvent.DeleteItem -> TODO()
            HomeEvent.SaveCategory -> saveCategory()
            HomeEvent.SaveItem -> saveItem()
            is HomeEvent.onCheckBoxClick -> { /* your logic */ }
            is HomeEvent.onCategoryChange -> {
                _state.update { it.copy(itemCategoryId = event.categoryId) }
            }
            is HomeEvent.onNameChange -> {
                _state.update { it.copy(itemName = event.name) }
            }
            is HomeEvent.onNewCategoryNameChange -> _state.update { it.copy(newCategoryName = event.name) }
            is HomeEvent.onQuantityChange -> {
                _state.update { it.copy(itemQuantity = event.quantity) }
            }
        }
    }

    private fun saveItem() {
        val quantity = state.value.itemQuantity.toIntOrNull()
        val categoryId = state.value.itemCategoryId
        val name = state.value.itemName.trim()

        if (name.isBlank()) return
        if (quantity == null) return
        if (categoryId == null) return

        viewModelScope.launch {
            try {
                toBuyItemRepository.upsertToBuyItem(
                    ToBuyItem(
                        name = name,
                        quantity = quantity,
                        categoryId = categoryId,
                        checked = false
                    )
                )
                _state.update { it.copy(itemName = "", itemQuantity = "", itemCategoryId = null) }
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(message = "Item added successfully.")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = "Couldn't add item. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }


    private fun saveCategory() {
        val name = state.value.newCategoryName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                categoryRepository.insertCategory(Category(name = name))
                _state.update { it.copy(newCategoryName = "") }
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(message = "Category added successfully.")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackBar(
                        message = "Couldn't add category. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }
}
