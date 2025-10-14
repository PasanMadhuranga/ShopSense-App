package com.example.myapplicationv2.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.domain.repository.CategoryRepository
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.DeleteItem -> TODO()
            HomeEvent.SaveCategory -> TODO()
            HomeEvent.SaveItem -> saveItem()
            is HomeEvent.onCheckBoxClick -> {}
            is HomeEvent.onCategoryChange -> {
                _state.update {
                    it.copy(
                        itemCategory = event.category
                    )
                }
            }
            is HomeEvent.onNameChange -> {
                _state.update {
                    it.copy(
                        itemName = event.name
                    )
                }
            }
            is HomeEvent.onNewCategoryNameChange -> TODO()
            is HomeEvent.onQuantityChange -> {
                _state.update {
                    it.copy(
                        itemQuantity = event.quantity
                    )
                }
            }
        }
    }

    private fun saveItem() {
        viewModelScope.launch {
            toBuyItemRepository.upsertToBuyItem(
                ToBuyItem(
                    name = state.value.itemName,
                    quantity = state.value.itemQuantity.toInt(),
                    categoryId = state.value.itemCategory.toInt(),
                    checked = false
                )
            )

        }
    }
}