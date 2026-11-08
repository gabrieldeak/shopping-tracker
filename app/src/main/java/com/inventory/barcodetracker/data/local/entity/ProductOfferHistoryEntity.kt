package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_offer_history")
data class ProductOfferHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val offerId: Long,
    val appliedAt: Long = System.currentTimeMillis()
)
