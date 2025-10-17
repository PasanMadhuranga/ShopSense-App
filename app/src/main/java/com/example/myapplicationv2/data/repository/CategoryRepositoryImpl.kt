package com.example.myapplicationv2.data.repository

import com.example.myapplicationv2.data.local.CategoryDao
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
): CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    override suspend fun insertAll(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }
}