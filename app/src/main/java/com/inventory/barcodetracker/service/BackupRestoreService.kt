package com.inventory.barcodetracker.service

import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

data class FullBackupPayload(
    val pieces: List<PieceEntity>,
    val products: List<ProductEntity>,
    val productPieces: List<ProductPieceCrossRef>,
    val shoppingTrips: List<ShoppingTripEntity>,
    val offers: List<OfferEntity> = emptyList(),
    val currencySymbol: String,
    val budgetLimit: Double,
    val themeMode: String,
)

object BackupRestoreService {

    fun exportFullBackup(
        outputStream: OutputStream,
        pieces: List<PieceEntity>,
        products: List<ProductEntity>,
        productPieces: List<ProductPieceCrossRef>,
        shoppingTrips: List<ShoppingTripEntity>,
        offers: List<OfferEntity> = emptyList(),
        currencySymbol: String,
        budgetLimit: Double,
        themeMode: String,
    ) {
        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("currencySymbol", currencySymbol)
        rootJson.put("budgetLimit", budgetLimit)
        rootJson.put("themeMode", themeMode)

        // 1. Pieces Array
        val piecesArr = JSONArray()
        for (p in pieces) {
            val obj = JSONObject()
            obj.put("barcode", p.barcode)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("location", p.location)
            obj.put("stockQuantity", p.stockQuantity)
            obj.put("minStockThreshold", p.minStockThreshold)
            obj.put("unitPrice", p.unitPrice)
            obj.put("offerType", p.offerType)
            obj.put("offerBuyQty", p.offerBuyQty)
            obj.put("offerPayQty", p.offerPayQty)
            obj.put("offerFixedPrice", p.offerFixedPrice)
            obj.put("offerPercentOff", p.offerPercentOff)
            obj.put("createdAt", p.createdAt)
            piecesArr.put(obj)
        }
        rootJson.put("pieces", piecesArr)

        // 2. Products Array
        val productsArr = JSONArray()
        for (prod in products) {
            val obj = JSONObject()
            obj.put("productSku", prod.productSku)
            obj.put("name", prod.name)
            obj.put("description", prod.description)
            obj.put("stockQuantity", prod.stockQuantity)
            obj.put("targetProductionCount", prod.targetProductionCount)
            obj.put("unitPrice", prod.unitPrice)
            obj.put("createdAt", prod.createdAt)
            productsArr.put(obj)
        }
        rootJson.put("products", productsArr)

        // 3. Product-Pieces Relationships
        val refArr = JSONArray()
        for (ref in productPieces) {
            val obj = JSONObject()
            obj.put("productSku", ref.productSku)
            obj.put("barcode", ref.barcode)
            obj.put("quantityRequiredPerProduct", ref.quantityRequiredPerProduct)
            refArr.put(obj)
        }
        rootJson.put("productPieces", refArr)

        // 4. Shopping History Receipts
        val tripsArr = JSONArray()
        for (t in shoppingTrips) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("tripDate", t.tripDate)
            obj.put("totalCost", t.totalCost)
            obj.put("itemCount", t.itemCount)
            obj.put("itemsSummary", t.itemsSummary)
            obj.put("currencySymbol", t.currencySymbol)
            tripsArr.put(obj)
        }
        rootJson.put("shoppingTrips", tripsArr)

        // 5. Offers Array
        val offersArr = JSONArray()
        for (off in offers) {
            val obj = JSONObject()
            obj.put("id", off.id)
            obj.put("title", off.title)
            obj.put("offerType", off.offerType)
            obj.put("buyQty", off.buyQty)
            obj.put("payQty", off.payQty)
            obj.put("fixedPrice", off.fixedPrice)
            obj.put("percentOff", off.percentOff)
            obj.put("storeName", off.storeName)
            obj.put("notes", off.notes)
            obj.put("createdAt", off.createdAt)
            offersArr.put(obj)
        }
        rootJson.put("offers", offersArr)

        outputStream.write(rootJson.toString(2).toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun importFullBackup(inputStream: InputStream): FullBackupPayload {
        val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)

        val currencySymbol = root.optString("currencySymbol", "$")
        val budgetLimit = root.optDouble("budgetLimit", 0.0)
        val themeMode = root.optString("themeMode", "system")

        // Parse Pieces
        val piecesList = mutableListOf<PieceEntity>()
        val piecesArr = root.optJSONArray("pieces")
        if (piecesArr != null) {
            for (i in 0 until piecesArr.length()) {
                val obj = piecesArr.getJSONObject(i)
                piecesList.add(
                    PieceEntity(
                        barcode = obj.getString("barcode"),
                        name = obj.optString("name", obj.getString("barcode")),
                        description = obj.optString("description", ""),
                        location = obj.optString("location", "Grocery"),
                        stockQuantity = obj.optInt("stockQuantity", 0),
                        minStockThreshold = obj.optInt("minStockThreshold", 0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        offerType = obj.optString("offerType", "NONE"),
                        offerBuyQty = obj.optInt("offerBuyQty", 0),
                        offerPayQty = obj.optInt("offerPayQty", 0),
                        offerFixedPrice = obj.optDouble("offerFixedPrice", 0.0),
                        offerPercentOff = obj.optDouble("offerPercentOff", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        // Parse Products
        val productsList = mutableListOf<ProductEntity>()
        val productsArr = root.optJSONArray("products")
        if (productsArr != null) {
            for (i in 0 until productsArr.length()) {
                val obj = productsArr.getJSONObject(i)
                productsList.add(
                    ProductEntity(
                        productSku = obj.getString("productSku"),
                        name = obj.optString("name", obj.getString("productSku")),
                        description = obj.optString("description", ""),
                        stockQuantity = obj.optInt("stockQuantity", 0),
                        targetProductionCount = obj.optInt("targetProductionCount", 0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        // Parse Product-Pieces
        val refList = mutableListOf<ProductPieceCrossRef>()
        val refArr = root.optJSONArray("productPieces")
        if (refArr != null) {
            for (i in 0 until refArr.length()) {
                val obj = refArr.getJSONObject(i)
                refList.add(
                    ProductPieceCrossRef(
                        productSku = obj.getString("productSku"),
                        barcode = obj.getString("barcode"),
                        quantityRequiredPerProduct = obj.optInt("quantityRequiredPerProduct", 1),
                    )
                )
            }
        }

        // Parse Shopping History
        val tripsList = mutableListOf<ShoppingTripEntity>()
        val tripsArr = root.optJSONArray("shoppingTrips")
        if (tripsArr != null) {
            for (i in 0 until tripsArr.length()) {
                val obj = tripsArr.getJSONObject(i)
                tripsList.add(
                    ShoppingTripEntity(
                        id = obj.optLong("id", 0),
                        tripDate = obj.optLong("tripDate", System.currentTimeMillis()),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        itemCount = obj.optInt("itemCount", 0),
                        itemsSummary = obj.optString("itemsSummary", ""),
                        currencySymbol = obj.optString("currencySymbol", "$"),
                    )
                )
            }
        }

        // Parse Offers
        val offersList = mutableListOf<OfferEntity>()
        val offersArr = root.optJSONArray("offers")
        if (offersArr != null) {
            for (i in 0 until offersArr.length()) {
                val obj = offersArr.getJSONObject(i)
                offersList.add(
                    OfferEntity(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", ""),
                        offerType = obj.optString("offerType", "THREE_FOR_TWO"),
                        buyQty = obj.optInt("buyQty", 0),
                        payQty = obj.optInt("payQty", 0),
                        fixedPrice = obj.optDouble("fixedPrice", 0.0),
                        percentOff = obj.optDouble("percentOff", 0.0),
                        storeName = obj.optString("storeName", ""),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        return FullBackupPayload(
            pieces = piecesList,
            products = productsList,
            productPieces = refList,
            shoppingTrips = tripsList,
            offers = offersList,
            currencySymbol = currencySymbol,
            budgetLimit = budgetLimit,
            themeMode = themeMode,
        )
    }
}
