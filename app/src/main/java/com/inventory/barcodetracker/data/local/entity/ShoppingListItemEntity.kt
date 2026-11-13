package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "shopping_list_items",
    primaryKeys = ["listId", "barcode"]
)
data class ShoppingListItemEntity(
    val listId: Long,
    val barcode: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val offerType: String = "NONE",
    val offerBuyQty: Int = 0,
    val offerPayQty: Int = 0,
    val offerFixedPrice: Double = 0.0,
    val offerPercentOff: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis()
)
