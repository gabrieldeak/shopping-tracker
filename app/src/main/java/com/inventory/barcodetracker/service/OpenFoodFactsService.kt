package com.inventory.barcodetracker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OpenFoodFactsProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String,
)

object OpenFoodFactsService {

    suspend fun lookupBarcode(barcode: String): OpenFoodFactsProduct? = withContext(Dispatchers.IO) {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) return@withContext null

        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$cleanCode.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "BarcodeInventoryTracker - Android - Version 1.0")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val status = json.optInt("status", 0)
                if (status == 1) {
                    val product = json.optJSONObject("product") ?: return@withContext null
                    val name = product.optString("product_name", "")
                        .ifEmpty { product.optString("product_name_en", "") }
                        .ifEmpty { product.optString("generic_name", "") }

                    val brands = product.optString("brands", "")
                    val categories = product.optString("categories", "")
                    val primaryCategory = if (categories.isNotBlank()) {
                        categories.split(",").firstOrNull()?.trim() ?: "Grocery"
                    } else {
                        "Grocery"
                    }

                    if (name.isNotBlank()) {
                        return@withContext OpenFoodFactsProduct(
                            barcode = cleanCode,
                            name = name,
                            brand = brands,
                            category = primaryCategory,
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network unavailable or timeout -> fallback gracefully
        }
        return@withContext null
    }
}
