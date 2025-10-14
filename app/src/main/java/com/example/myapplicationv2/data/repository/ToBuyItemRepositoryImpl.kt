package com.example.myapplicationv2.data.repository

import com.example.myapplicationv2.data.local.ToBuyItemDao
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ToBuyItemRepositoryImpl @Inject constructor(
    private val toBuyItemDao: ToBuyItemDao
): ToBuyItemRepository {
    override fun getAllToBuyItems(): Flow<List<ToBuyItem>> {
        return toBuyItemDao.getAllToBuyItems()
    }

    override suspend fun getToBuyItemById(id: Int): ToBuyItem? {
        TODO("Not yet implemented")
    }

    override suspend fun upsertToBuyItem(toBuyItem: ToBuyItem) {
        toBuyItemDao.upsertToBuyItem(toBuyItem)
    }

    override suspend fun deleteToBuyItem(toBuyItem: ToBuyItem) {
        TODO("Not yet implemented")
    }
}