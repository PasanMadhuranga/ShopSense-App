package com.example.myapplicationv2.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "to_buy_item",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class ToBuyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val categoryId: Int, // foreign key
    val quantity: Int,
    val checked: Boolean
)
