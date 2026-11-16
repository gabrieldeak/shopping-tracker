package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "pieces")
data class PieceEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val description: String = "",
    val location: String = "Grocery",
    val stockQuantity: Int = 0,
    val minStockThreshold: Int = 0,
    val unitPrice: Double = 0.0,
    val offerType: String = "NONE",
    val offerBuyQty: Int = 0,
    val offerPayQty: Int = 0,
    val offerFixedPrice: Double = 0.0,
    val offerPercentOff: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val rawSubtotal: Double
        get() = stockQuantity * unitPrice

    val calculatedTotal: Double
        get() {
            if (stockQuantity <= 0 || unitPrice <= 0.0) return 0.0
            return when (offerType) {
                "THREE_FOR_TWO" -> {
                    val buy = if (offerBuyQty > 0) offerBuyQty else 3
                    val pay = if (offerPayQty > 0) offerPayQty else 2
                    val sets = stockQuantity / buy
                    val rem = stockQuantity % buy
                    (sets * pay + rem) * unitPrice
                }
                "MULTI_BUY_FIXED" -> {
                    val buy = if (offerBuyQty > 0) offerBuyQty else 2
                    if (offerFixedPrice > 0.0) {
                        val sets = stockQuantity / buy
                        val rem = stockQuantity % buy
                        sets * offerFixedPrice + rem * unitPrice
                    } else {
                        rawSubtotal
                    }
                }
                "PERCENT_OFF" -> {
                    val percent = offerPercentOff.coerceIn(0.0, 100.0)
                    rawSubtotal * (1.0 - (percent / 100.0))
                }
                "REDUCED_PRICE" -> {
                    if (offerFixedPrice > 0.0) {
                        stockQuantity * offerFixedPrice
                    } else {
                        rawSubtotal
                    }
                }
                else -> rawSubtotal
            }
        }

    val savings: Double
        get() = maxOf(0.0, rawSubtotal - calculatedTotal)

    fun getOfferDescription(currencySymbol: String = "$"): String {
        return when (offerType) {
            "THREE_FOR_TWO" -> {
                val buy = if (offerBuyQty > 0) offerBuyQty else 3
                val pay = if (offerPayQty > 0) offerPayQty else 2
                "$buy for $pay Offer"
            }
            "MULTI_BUY_FIXED" -> {
                val buy = if (offerBuyQty > 0) offerBuyQty else 2
                "$buy for $currencySymbol${String.format(Locale.US, "%.2f", offerFixedPrice)}"
            }
            "PERCENT_OFF" -> "${String.format(Locale.US, "%.0f", offerPercentOff)}% Off"
            "REDUCED_PRICE" -> "Reduced to $currencySymbol${String.format(Locale.US, "%.2f", offerFixedPrice)}"
            else -> ""
        }
    }

    val offerDescription: String
        get() = getOfferDescription("$")
}
