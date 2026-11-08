package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_pieces",
    primaryKeys = ["productSku", "barcode"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["productSku"],
            childColumns = ["productSku"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PieceEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["productSku"]), Index(value = ["barcode"])]
)
data class ProductPieceCrossRef(
    val productSku: String,
    val barcode: String,
    val quantityRequiredPerProduct: Int = 1
)
