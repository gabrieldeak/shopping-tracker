package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetLimit: Double = 0.0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
