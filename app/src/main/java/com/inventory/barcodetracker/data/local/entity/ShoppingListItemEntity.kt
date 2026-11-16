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

fun ShoppingListItemEntity.calculateItemTotal(piece: PieceEntity?): Double {
    val qty = this.quantity
    val basePrice = if (this.unitPrice > 0) this.unitPrice else (piece?.unitPrice ?: 0.0)
    if (qty <= 0 || basePrice <= 0.0) return 0.0

    val effOfferType = if (this.offerType != "NONE") this.offerType else (piece?.offerType ?: "NONE")
    val effBuyQty = if (this.offerBuyQty > 0) this.offerBuyQty else (piece?.offerBuyQty ?: 0)
    val effPayQty = if (this.offerPayQty > 0) this.offerPayQty else (piece?.offerPayQty ?: 0)
    val effFixedPrice = if (this.offerFixedPrice > 0.0) this.offerFixedPrice else (piece?.offerFixedPrice ?: 0.0)
    val effPercentOff = if (this.offerPercentOff > 0.0) this.offerPercentOff else (piece?.offerPercentOff ?: 0.0)

    val rawSubtotal = qty * basePrice
    return when (effOfferType) {
        "THREE_FOR_TWO" -> {
            val buy = if (effBuyQty > 0) effBuyQty else 3
            val pay = if (effPayQty > 0) effPayQty else 2
            val sets = qty / buy
            val rem = qty % buy
            (sets * pay + rem) * basePrice
        }
        "MULTI_BUY_FIXED" -> {
            val buy = if (effBuyQty > 0) effBuyQty else 2
            if (effFixedPrice > 0.0) {
                val sets = qty / buy
                val rem = qty % buy
                sets * effFixedPrice + rem * basePrice
            } else {
                rawSubtotal
            }
        }
        "PERCENT_OFF" -> {
            val percent = effPercentOff.coerceIn(0.0, 100.0)
            rawSubtotal * (1.0 - (percent / 100.0))
        }
        "REDUCED_PRICE" -> {
            if (effFixedPrice > 0.0) {
                qty * effFixedPrice
            } else {
                rawSubtotal
            }
        }
        else -> rawSubtotal
    }
}

fun ShoppingListItemEntity.calculateItemSavings(piece: PieceEntity?): Double {
    val basePrice = if (this.unitPrice > 0) this.unitPrice else (piece?.unitPrice ?: 0.0)
    val rawSubtotal = this.quantity * basePrice
    val total = calculateItemTotal(piece)
    return maxOf(0.0, rawSubtotal - total)
}
