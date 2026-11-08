package com.inventory.barcodetracker.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef

data class ProductWithPieces(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "productSku",
        entityColumn = "barcode",
        associateBy = Junction(
            value = ProductPieceCrossRef::class,
            parentColumn = "productSku",
            entityColumn = "barcode",
        ),
    )
    val pieces: List<PieceEntity>,
)
