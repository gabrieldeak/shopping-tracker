package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val productSku: String,
    val name: String,
    val description: String = "",
    val stockQuantity: Int = 0,
    val targetProductionCount: Int = 0,
    val unitPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)
