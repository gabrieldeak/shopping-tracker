package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "shopping_trips")
data class ShoppingTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripDate: Long = System.currentTimeMillis(),
    val totalCost: Double,
    val itemCount: Int,
    val itemsSummary: String,
    val currencySymbol: String = "$",
    val snapshotJson: String = "",
) {
    fun parseSnapshot(): TripBasketSnapshot? {
        if (snapshotJson.isBlank()) return null
        return try {
            val root = JSONObject(snapshotJson)
            val itemsArr = root.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<TripSnapshotItem>()
            for (i in 0 until itemsArr.length()) {
                val obj = itemsArr.getJSONObject(i)
                items.add(
                    TripSnapshotItem(
                        barcode = obj.optString("barcode", ""),
                        name = obj.optString("name", ""),
                        quantity = obj.optInt("quantity", 1),
                        checkedQuantity = obj.optInt("checkedQuantity", 0),
                        isChecked = obj.optBoolean("isChecked", false),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        offerType = obj.optString("offerType", "NONE"),
                        offerBuyQty = obj.optInt("offerBuyQty", 0),
                        offerPayQty = obj.optInt("offerPayQty", 0),
                        offerFixedPrice = obj.optDouble("offerFixedPrice", 0.0),
                        offerPercentOff = obj.optDouble("offerPercentOff", 0.0),
                        offerGroupId = obj.optString("offerGroupId", ""),
                    )
                )
            }
            TripBasketSnapshot(
                basketName = root.optString("basketName", ""),
                budgetLimit = root.optDouble("budgetLimit", 0.0),
                subtotalBeforeVouchers = root.optDouble("subtotalBeforeVouchers", totalCost),
                voucherDiscountAmount = root.optDouble("voucherDiscountAmount", 0.0),
                totalSavings = root.optDouble("totalSavings", 0.0),
                items = items,
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class TripBasketSnapshot(
    val basketName: String,
    val budgetLimit: Double = 0.0,
    val subtotalBeforeVouchers: Double = 0.0,
    val voucherDiscountAmount: Double = 0.0,
    val totalSavings: Double = 0.0,
    val items: List<TripSnapshotItem> = emptyList(),
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("basketName", basketName)
        root.put("budgetLimit", budgetLimit)
        root.put("subtotalBeforeVouchers", subtotalBeforeVouchers)
        root.put("voucherDiscountAmount", voucherDiscountAmount)
        root.put("totalSavings", totalSavings)
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("barcode", item.barcode)
            obj.put("name", item.name)
            obj.put("quantity", item.quantity)
            obj.put("checkedQuantity", item.checkedQuantity)
            obj.put("isChecked", item.isChecked)
            obj.put("unitPrice", item.unitPrice)
            obj.put("offerType", item.offerType)
            obj.put("offerBuyQty", item.offerBuyQty)
            obj.put("offerPayQty", item.offerPayQty)
            obj.put("offerFixedPrice", item.offerFixedPrice)
            obj.put("offerPercentOff", item.offerPercentOff)
            obj.put("offerGroupId", item.offerGroupId)
            arr.put(obj)
        }
        root.put("items", arr)
        return root.toString()
    }
}

data class TripSnapshotItem(
    val barcode: String,
    val name: String,
    val quantity: Int,
    val checkedQuantity: Int = 0,
    val isChecked: Boolean = false,
    val unitPrice: Double = 0.0,
    val offerType: String = "NONE",
    val offerBuyQty: Int = 0,
    val offerPayQty: Int = 0,
    val offerFixedPrice: Double = 0.0,
    val offerPercentOff: Double = 0.0,
    val offerGroupId: String = "",
)
