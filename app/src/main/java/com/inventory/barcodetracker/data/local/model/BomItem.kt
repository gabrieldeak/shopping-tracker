package com.inventory.barcodetracker.data.local.model

data class BomItem(
    val barcode: String,
    val pieceName: String,
    val location: String,
    val stockQuantity: Int,
    val quantityRequired: Int,
    val unitPrice: Double = 0.0,
)
