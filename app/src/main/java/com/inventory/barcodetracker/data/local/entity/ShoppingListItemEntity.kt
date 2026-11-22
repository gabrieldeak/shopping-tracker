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
    val addedAt: Long = System.currentTimeMillis(),
    val isChecked: Boolean = false,
    val offerGroupId: String = "",
    val checkedQuantity: Int = 0
) {
    val effectiveCheckedQty: Int
        get() = when {
            checkedQuantity > 0 -> checkedQuantity.coerceIn(0, quantity)
            isChecked -> quantity
            else -> 0
        }

    val uncheckedQty: Int
        get() = maxOf(0, quantity - effectiveCheckedQty)
}

data class BasketGroupPricing(
    val groupId: String,
    val items: List<ShoppingListItemEntity>,
    val offerDescription: String,
    val rawSubtotal: Double,
    val discountedTotal: Double,
    val savings: Double,
)

data class BasketPricingSummary(
    val regularItemsSubtotal: Double,
    val totalSavings: Double,
    val uncheckedTotal: Double,
    val checkedTotal: Double,
    val groupPricings: Map<String, BasketGroupPricing>,
    val subtotalBeforeVouchers: Double = regularItemsSubtotal,
    val voucherDiscountAmount: Double = 0.0,
)

fun formatOfferBadge(
    offerType: String,
    buyQty: Int,
    payQty: Int,
    fixedPrice: Double,
    percentOff: Double,
    currencySymbol: String,
): String = when (offerType) {
    "THREE_FOR_TWO" -> {
        val buy = if (buyQty > 0) buyQty else 3
        val pay = if (payQty > 0) payQty else 2
        "$buy for $pay"
    }
    "MULTI_BUY_FIXED" -> {
        val buy = if (buyQty > 0) buyQty else 2
        String.format(java.util.Locale.US, "%d for %s%.2f", buy, currencySymbol, fixedPrice)
    }
    "PERCENT_OFF" -> String.format(java.util.Locale.US, "%.0f%% Off", percentOff)
    "REDUCED_PRICE" -> String.format(java.util.Locale.US, "Now %s%.2f", currencySymbol, fixedPrice)
    else -> ""
}

fun calculateMixAndMatchGroup(
    groupId: String,
    items: List<ShoppingListItemEntity>,
    pieceMap: Map<String, PieceEntity>,
    currencySymbol: String,
): BasketGroupPricing {
    val rep = items.first()
    val unitPrices = mutableListOf<Double>()
    for (item in items) {
        val piece = pieceMap[item.barcode]
        val basePrice = if (item.unitPrice > 0) item.unitPrice else (piece?.unitPrice ?: 0.0)
        if (basePrice > 0.0 && item.quantity > 0) {
            repeat(item.quantity) { unitPrices.add(basePrice) }
        }
    }

    val rawSubtotal = unitPrices.sum()
    if (unitPrices.isEmpty()) {
        return BasketGroupPricing(groupId, items, "", 0.0, 0.0, 0.0)
    }

    val sortedDesc = unitPrices.sortedDescending()
    val totalUnits = sortedDesc.size
    val discountedTotal = when (rep.offerType) {
        "THREE_FOR_TWO" -> {
            val buy = if (rep.offerBuyQty > 0) rep.offerBuyQty else 3
            val pay = (if (rep.offerPayQty > 0) rep.offerPayQty else 2).coerceIn(1, buy)
            sortedDesc.chunked(buy).sumOf { chunk ->
                if (chunk.size == buy) chunk.take(pay).sum() else chunk.sum()
            }
        }
        "MULTI_BUY_FIXED" -> {
            val buy = if (rep.offerBuyQty > 0) rep.offerBuyQty else 2
            if (rep.offerFixedPrice > 0.0) {
                val sets = totalUnits / buy
                val remPrices = sortedDesc.drop(sets * buy).sum()
                minOf(rawSubtotal, sets * rep.offerFixedPrice + remPrices)
            } else {
                rawSubtotal
            }
        }
        "PERCENT_OFF" -> {
            val pct = rep.offerPercentOff.coerceIn(0.0, 100.0)
            rawSubtotal * (1.0 - pct / 100.0)
        }
        "REDUCED_PRICE" -> {
            if (rep.offerFixedPrice > 0.0) totalUnits * rep.offerFixedPrice else rawSubtotal
        }
        else -> rawSubtotal
    }

    val desc = formatOfferBadge(
        offerType = rep.offerType,
        buyQty = rep.offerBuyQty,
        payQty = rep.offerPayQty,
        fixedPrice = rep.offerFixedPrice,
        percentOff = rep.offerPercentOff,
        currencySymbol = currencySymbol,
    )

    return BasketGroupPricing(
        groupId = groupId,
        items = items,
        offerDescription = desc,
        rawSubtotal = rawSubtotal,
        discountedTotal = discountedTotal,
        savings = maxOf(0.0, rawSubtotal - discountedTotal),
    )
}

fun calculateBasketSummary(
    itemsInList: List<ShoppingListItemEntity>,
    pieceMap: Map<String, PieceEntity>,
    currencySymbol: String,
): BasketPricingSummary {
    val regularItems = itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
    val grouped = regularItems.filter { it.offerGroupId.isNotBlank() }.groupBy { it.offerGroupId }
    val ungrouped = regularItems.filter { it.offerGroupId.isBlank() }

    val groupPricings = mutableMapOf<String, BasketGroupPricing>()
    var regularSubtotal = 0.0
    var totalSavings = 0.0
    var uncheckedTotal = 0.0
    var checkedTotal = 0.0

    for ((groupId, groupItems) in grouped) {
        val pricing = calculateMixAndMatchGroup(groupId, groupItems, pieceMap, currencySymbol)
        groupPricings[groupId] = pricing
        regularSubtotal += pricing.discountedTotal
        totalSavings += pricing.savings

        // Attribute group total proportionally by checked/unchecked raw value
        val uncheckedRaw = groupItems.sumOf { item ->
            val p = pieceMap[item.barcode]
            val price = if (item.unitPrice > 0) item.unitPrice else (p?.unitPrice ?: 0.0)
            price * item.uncheckedQty
        }
        val checkedRaw = pricing.rawSubtotal - uncheckedRaw
        if (pricing.rawSubtotal > 0.0) {
            val ratio = pricing.discountedTotal / pricing.rawSubtotal
            uncheckedTotal += uncheckedRaw * ratio
            checkedTotal += checkedRaw * ratio
        }
    }

    for (item in ungrouped) {
        val piece = pieceMap[item.barcode]
        val lineTotal = item.calculateItemTotal(piece)
        val lineSavings = item.calculateItemSavings(piece)
        regularSubtotal += lineTotal
        totalSavings += lineSavings
        if (item.quantity > 0) {
            val checkedFrac = item.effectiveCheckedQty.toDouble() / item.quantity.toDouble()
            checkedTotal += lineTotal * checkedFrac
            uncheckedTotal += lineTotal * (1.0 - checkedFrac)
        }
    }

    val voucherDiscount = itemsInList
        .filter { it.offerType == "VOUCHER" || it.unitPrice < 0 }
        .sumOf { it.unitPrice } // negative values

    val voucherDiscountAmount = maxOf(0.0, -voucherDiscount)
    val appliedVoucherSavings = minOf(regularSubtotal, voucherDiscountAmount)
    val netSubtotal = maxOf(0.0, regularSubtotal + voucherDiscount)

    return BasketPricingSummary(
        regularItemsSubtotal = netSubtotal,
        totalSavings = totalSavings + appliedVoucherSavings,
        uncheckedTotal = uncheckedTotal,
        checkedTotal = checkedTotal,
        groupPricings = groupPricings,
        subtotalBeforeVouchers = regularSubtotal,
        voucherDiscountAmount = voucherDiscountAmount,
    )
}

fun ShoppingListItemEntity.calculateItemTotal(piece: PieceEntity?): Double {
    val qty = this.quantity
    val basePrice = if (this.unitPrice > 0) this.unitPrice else (piece?.unitPrice ?: 0.0)
    if (qty <= 0 || basePrice <= 0.0) return 0.0

    val effOfferType = when (this.offerType) {
        "NO_OFFER" -> "NONE"
        "NONE" -> piece?.offerType ?: "NONE"
        else -> this.offerType
    }
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
