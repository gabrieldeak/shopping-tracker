package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val offerType: String, // "THREE_FOR_TWO", "MULTI_BUY_FIXED", "REDUCED_PRICE", "PERCENT_OFF", "VOUCHER"
    val buyQty: Int = 0,
    val payQty: Int = 0,
    val fixedPrice: Double = 0.0,
    val percentOff: Double = 0.0,
    val storeName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayBadge(currencySymbol: String = "$"): String {
        return when (offerType) {
            "THREE_FOR_TWO" -> {
                val b = if (buyQty > 0) buyQty else 3
                val p = if (payQty > 0) payQty else 2
                "$b for $p"
            }
            "MULTI_BUY_FIXED" -> {
                val b = if (buyQty > 0) buyQty else 2
                "$b for $currencySymbol${String.format(Locale.US, "%.2f", fixedPrice)}"
            }
            "REDUCED_PRICE" -> "Reduced to $currencySymbol${String.format(Locale.US, "%.2f", fixedPrice)}"
            "PERCENT_OFF" -> "${String.format(Locale.US, "%.0f", percentOff)}% Off"
            "VOUCHER" -> "$currencySymbol${String.format(Locale.US, "%.2f", fixedPrice)} Voucher"
            else -> "Special Offer"
        }
    }

    val displayBadge: String
        get() = getDisplayBadge("$")
}
