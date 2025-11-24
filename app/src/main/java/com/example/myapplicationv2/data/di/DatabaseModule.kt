package com.example.myapplicationv2.data.di

import android.app.Application
import androidx.room.Room
import com.example.myapplicationv2.data.local.db.AppDatabase
import com.example.myapplicationv2.data.local.db.CategoryDao
import com.example.myapplicationv2.data.local.db.ToBuyItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        application: Application
    ): AppDatabase {
        return Room
            .databaseBuilder(
                application,
                AppDatabase::class.java,
                "shopsense.db"
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideToBuyItemDao(database: AppDatabase): ToBuyItemDao {
        return database.toBuyItemDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

}