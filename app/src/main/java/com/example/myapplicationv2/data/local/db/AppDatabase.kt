package com.example.myapplicationv2.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem

@Database(
    entities = [
        ToBuyItem::class,
        Category::class
    ],
    version = 1
)
abstract class AppDatabase: RoomDatabase() {

    abstract fun toBuyItemDao(): ToBuyItemDao
    abstract fun categoryDao(): CategoryDao

}