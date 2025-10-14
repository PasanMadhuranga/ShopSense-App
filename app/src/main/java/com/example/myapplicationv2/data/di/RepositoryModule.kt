package com.example.myapplicationv2.data.di

import com.example.myapplicationv2.data.repository.CategoryRepositoryImpl
import com.example.myapplicationv2.data.repository.ToBuyItemRepositoryImpl
import com.example.myapplicationv2.domain.repository.CategoryRepository
import com.example.myapplicationv2.domain.repository.ToBuyItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindToBuyItemRepository(
        impl: ToBuyItemRepositoryImpl
    ): ToBuyItemRepository

    @Singleton
    @Binds
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository
}