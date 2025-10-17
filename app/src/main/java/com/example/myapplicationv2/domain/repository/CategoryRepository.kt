package com.example.myapplicationv2.domain.repository

import com.example.myapplicationv2.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun insertCategory(category: Category)

    suspend fun insertAll(categories: List<Category>)
}