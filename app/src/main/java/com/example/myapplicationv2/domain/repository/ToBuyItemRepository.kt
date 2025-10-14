package com.example.myapplicationv2.domain.repository

import com.example.myapplicationv2.domain.model.ToBuyItem
import kotlinx.coroutines.flow.Flow

interface ToBuyItemRepository {
    fun getAllToBuyItems(): Flow<List<ToBuyItem>>
    suspend fun getToBuyItemById(id: Int): ToBuyItem?
    suspend fun upsertToBuyItem(toBuyItem: ToBuyItem)
    suspend fun deleteToBuyItem(toBuyItem: ToBuyItem)

}