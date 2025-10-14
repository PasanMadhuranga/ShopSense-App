package com.example.myapplicationv2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.myapplicationv2.domain.model.ToBuyItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ToBuyItemDao {

    @Query("SELECT * FROM to_buy_item")
    fun getAllToBuyItems(): Flow<List<ToBuyItem>>

    @Query("SELECT * FROM to_buy_item WHERE id = :id")
    suspend fun getToBuyItemById(id: Int): ToBuyItem?

    @Upsert
    suspend fun upsertToBuyItem(toBuyItem: ToBuyItem)

    @Delete
    suspend fun deleteToBuyItem(toBuyItem: ToBuyItem)

}