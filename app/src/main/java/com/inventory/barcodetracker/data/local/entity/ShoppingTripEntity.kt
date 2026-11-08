package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_trips")
data class ShoppingTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripDate: Long = System.currentTimeMillis(),
    val totalCost: Double,
    val itemCount: Int,
    val itemsSummary: String,
    val currencySymbol: String = "$",
)
