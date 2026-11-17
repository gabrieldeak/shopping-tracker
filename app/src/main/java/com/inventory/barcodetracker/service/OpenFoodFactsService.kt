package com.inventory.barcodetracker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

data class BarcodeLookupOutcome(
    val product: OpenFoodFactsProduct?,
    val apisUnavailable: Boolean,
)

data class EndpointHealthStatus(
    val name: String,
    val isHealthy: Boolean,
    val latencyMs: Long,
    val statusCode: Int? = null,
    val sampleBarcode: String? = null,
    val sampleProductName: String? = null,
    val errorMessage: String? = null,
)

data class OpenFoodFactsHealthResult(
    val isHealthy: Boolean,
    val endpoints: List<EndpointHealthStatus>,
    val latencyMs: Long,
)

object OpenFoodFactsService {

    private const val FIELDS_QUERY = "fields=product_name,product_name_en,generic_name,brands,categories"

    private data class HostConfig(
        val host: String,
        val label: String,
        val sampleBarcode: String,
    )

    private val LOOKUP_HOSTS = listOf(
        HostConfig("world.openfoodfacts.org", "OpenFoodFacts (.org)", "3017620422003"),
        HostConfig("world.openfoodfacts.net", "OpenFoodFacts (.net)", "3017620422003"),
        HostConfig("world.openproductsfacts.org", "OpenProductsFacts", "7896226100319"),
        HostConfig("world.openbeautyfacts.org", "OpenBeautyFacts", "8003170094833"),
    )

    suspend fun healthCheck(): OpenFoodFactsHealthResult = withContext(Dispatchers.IO) {
        val totalStart = System.currentTimeMillis()
        val results = LOOKUP_HOSTS.map { config ->
            checkHost(config)
        }
        OpenFoodFactsHealthResult(
            isHealthy = results.any { it.isHealthy },
            endpoints = results,
            latencyMs = System.currentTimeMillis() - totalStart,
        )
    }

    private fun checkHost(config: HostConfig): EndpointHealthStatus {
        val startMs = System.currentTimeMillis()
        return try {
            val url = URL("https://${config.host}/api/v2/product/${config.sampleBarcode}.json?$FIELDS_QUERY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty(
                "User-Agent",
                "ShoppingTracker - Android - Version ${UpdateCheckService.CURRENT_VERSION}"
            )

            val code = connection.responseCode
            val elapsed = System.currentTimeMillis() - startMs
            if (code == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val product = json.optJSONObject("product")
                val sampleName = product?.let {
                    listOf("product_name", "product_name_en", "generic_name")
                        .firstNotNullOfOrNull { key ->
                            if (it.isNull(key)) null else it.optString(key, "").trim().takeIf { s -> s.isNotEmpty() }
                        }
                }
                EndpointHealthStatus(
                    name = config.label,
                    isHealthy = json.optInt("status", 0) == 1 && !sampleName.isNullOrBlank(),
                    latencyMs = elapsed,
                    statusCode = code,
                    sampleBarcode = config.sampleBarcode,
                    sampleProductName = sampleName,
                    errorMessage = if (sampleName.isNullOrBlank()) "Product payload empty" else null,
                )
            } else {
                EndpointHealthStatus(
                    name = config.label,
                    isHealthy = false,
                    latencyMs = elapsed,
                    statusCode = code,
                    sampleBarcode = config.sampleBarcode,
                    errorMessage = "HTTP $code",
                )
            }
        } catch (e: Exception) {
            EndpointHealthStatus(
                name = config.label,
                isHealthy = false,
                latencyMs = System.currentTimeMillis() - startMs,
                sampleBarcode = config.sampleBarcode,
                errorMessage = e.message ?: e::class.java.simpleName,
            )
        }
    }

    private sealed interface HostLookupResult {
        data class Found(val product: OpenFoodFactsProduct) : HostLookupResult
        data object NotFound : HostLookupResult
        data object Unavailable : HostLookupResult
    }

    suspend fun lookupBarcode(barcode: String): OpenFoodFactsProduct? =
        lookupBarcodeWithStatus(barcode).product

    suspend fun lookupBarcodeWithStatus(barcode: String): BarcodeLookupOutcome = coroutineScope {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) return@coroutineScope BarcodeLookupOutcome(null, apisUnavailable = false)

        val outcomes = LOOKUP_HOSTS.map { config ->
            async(Dispatchers.IO) { lookupOnHost(config.host, cleanCode) }
        }.awaitAll()

        val found = outcomes.filterIsInstance<HostLookupResult.Found>().firstOrNull()
        if (found != null) {
            return@coroutineScope BarcodeLookupOutcome(found.product, apisUnavailable = false)
        }
        val anyResponded = outcomes.any { it is HostLookupResult.NotFound }
        BarcodeLookupOutcome(null, apisUnavailable = !anyResponded)
    }

    private fun lookupOnHost(host: String, cleanCode: String): HostLookupResult {
        try {
            val url = URL("https://$host/api/v2/product/$cleanCode.json?$FIELDS_QUERY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.setRequestProperty(
                "User-Agent",
                "ShoppingTracker - Android - Version ${UpdateCheckService.CURRENT_VERSION}"
            )

            val code = connection.responseCode
            if (code == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.optInt("status", 0) == 1) {
                    val product = json.optJSONObject("product") ?: return HostLookupResult.NotFound
                    fun JSONObject.optCleanString(key: String): String =
                        if (isNull(key)) "" else optString(key, "").trim()

                    val name = product.optCleanString("product_name")
                        .ifEmpty { product.optCleanString("product_name_en") }
                        .ifEmpty { product.optCleanString("generic_name") }

                    val brands = product.optCleanString("brands")
                    val categories = product.optCleanString("categories")
                    val primaryCategory = if (categories.isNotBlank()) {
                        categories.split(",").firstOrNull()?.trim() ?: "Grocery"
                    } else {
                        "Grocery"
                    }

                    if (name.isNotBlank()) {
                        return HostLookupResult.Found(
                            OpenFoodFactsProduct(
                                barcode = cleanCode,
                                name = name,
                                brand = brands,
                                category = primaryCategory,
                            )
                        )
                    }
                }
                return HostLookupResult.NotFound
            } else if (code == 404) {
                return HostLookupResult.NotFound
            }
        } catch (_: Exception) {
            // Network unavailable or timeout -> try next host
        }
        return HostLookupResult.Unavailable
    }
}
