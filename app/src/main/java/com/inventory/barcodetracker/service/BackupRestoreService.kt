package com.inventory.barcodetracker.service

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductOfferHistoryEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

data class FullBackupPayload(
    val pieces: List<PieceEntity>,
    val products: List<ProductEntity>,
    val productPieces: List<ProductPieceCrossRef>,
    val shoppingTrips: List<ShoppingTripEntity>,
    val offers: List<OfferEntity> = emptyList(),
    val offerHistory: List<ProductOfferHistoryEntity> = emptyList(),
    val shoppingLists: List<ShoppingListEntity> = emptyList(),
    val shoppingListItems: List<ShoppingListItemEntity> = emptyList(),
    val currencySymbol: String,
    val budgetLimit: Double,
    val themeMode: String,
    val capitalizationMode: String = "TITLE_CASE",
)

object BackupRestoreService {

    fun exportFullBackup(
        outputStream: OutputStream,
        pieces: List<PieceEntity>,
        products: List<ProductEntity>,
        productPieces: List<ProductPieceCrossRef>,
        shoppingTrips: List<ShoppingTripEntity>,
        offers: List<OfferEntity> = emptyList(),
        offerHistory: List<ProductOfferHistoryEntity> = emptyList(),
        shoppingLists: List<ShoppingListEntity> = emptyList(),
        shoppingListItems: List<ShoppingListItemEntity> = emptyList(),
        currencySymbol: String,
        budgetLimit: Double,
        themeMode: String,
        capitalizationMode: String = "TITLE_CASE",
    ) {
        val rootJson = JSONObject()
        rootJson.put("version", 2)
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("currencySymbol", currencySymbol)
        rootJson.put("budgetLimit", budgetLimit)
        rootJson.put("themeMode", themeMode)
        rootJson.put("capitalizationMode", capitalizationMode)

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

        // 6. Offer History
        val historyArr = JSONArray()
        for (h in offerHistory) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("barcode", h.barcode)
            obj.put("offerId", h.offerId)
            obj.put("appliedAt", h.appliedAt)
            historyArr.put(obj)
        }
        rootJson.put("offerHistory", historyArr)

        // 7. Shopping Lists (Baskets)
        val listsArr = JSONArray()
        for (list in shoppingLists) {
            val obj = JSONObject()
            obj.put("id", list.id)
            obj.put("name", list.name)
            obj.put("budgetLimit", list.budgetLimit)
            obj.put("isArchived", list.isArchived)
            obj.put("createdAt", list.createdAt)
            listsArr.put(obj)
        }
        rootJson.put("shoppingLists", listsArr)

        // 8. Shopping List Items
        val listItemsArr = JSONArray()
        for (item in shoppingListItems) {
            val obj = JSONObject()
            obj.put("listId", item.listId)
            obj.put("barcode", item.barcode)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("offerType", item.offerType)
            obj.put("offerBuyQty", item.offerBuyQty)
            obj.put("offerPayQty", item.offerPayQty)
            obj.put("offerFixedPrice", item.offerFixedPrice)
            obj.put("offerPercentOff", item.offerPercentOff)
            obj.put("addedAt", item.addedAt)
            obj.put("isChecked", item.isChecked)
            listItemsArr.put(obj)
        }
        rootJson.put("shoppingListItems", listItemsArr)

        outputStream.write(rootJson.toString(2).toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun parseBackupStream(context: Context, inputStream: InputStream): FullBackupPayload? {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return null

        val headerString = if (bytes.size >= 15) String(bytes, 0, 15, StandardCharsets.US_ASCII) else ""
        return if (headerString.startsWith("SQLite format 3")) {
            importDatabasePayload(context, bytes.inputStream())
        } else {
            val jsonText = String(bytes, StandardCharsets.UTF_8)
            parseJsonText(jsonText)
        }
    }

    fun parseJsonText(jsonText: String): FullBackupPayload {
        val root = JSONObject(jsonText)

        val currencySymbol = root.optString("currencySymbol", "$")
        val budgetLimit = root.optDouble("budgetLimit", 0.0)
        val themeMode = root.optString("themeMode", "system")
        val capitalizationMode = root.optString("capitalizationMode", "TITLE_CASE")

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

        // Parse Offer History
        val historyList = mutableListOf<ProductOfferHistoryEntity>()
        val historyArr = root.optJSONArray("offerHistory")
        if (historyArr != null) {
            for (i in 0 until historyArr.length()) {
                val obj = historyArr.getJSONObject(i)
                historyList.add(
                    ProductOfferHistoryEntity(
                        id = obj.optLong("id", 0),
                        barcode = obj.optString("barcode", ""),
                        offerId = obj.optLong("offerId", 0),
                        appliedAt = obj.optLong("appliedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Parse Shopping Lists
        val shoppingLists = mutableListOf<ShoppingListEntity>()
        val listsArr = root.optJSONArray("shoppingLists")
        if (listsArr != null) {
            for (i in 0 until listsArr.length()) {
                val obj = listsArr.getJSONObject(i)
                shoppingLists.add(
                    ShoppingListEntity(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "Shopping Basket"),
                        budgetLimit = obj.optDouble("budgetLimit", 0.0),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Parse Shopping List Items
        val shoppingListItems = mutableListOf<ShoppingListItemEntity>()
        val listItemsArr = root.optJSONArray("shoppingListItems")
        if (listItemsArr != null) {
            for (i in 0 until listItemsArr.length()) {
                val obj = listItemsArr.getJSONObject(i)
                val barcode = obj.optString("barcode", "")
                if (barcode.isBlank()) continue
                shoppingListItems.add(
                    ShoppingListItemEntity(
                        listId = obj.optLong("listId", 0),
                        barcode = barcode,
                        quantity = obj.optInt("quantity", 1),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        offerType = obj.optString("offerType", "NONE"),
                        offerBuyQty = obj.optInt("offerBuyQty", 0),
                        offerPayQty = obj.optInt("offerPayQty", 0),
                        offerFixedPrice = obj.optDouble("offerFixedPrice", 0.0),
                        offerPercentOff = obj.optDouble("offerPercentOff", 0.0),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                        isChecked = obj.optBoolean("isChecked", false)
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
            offerHistory = historyList,
            shoppingLists = shoppingLists,
            shoppingListItems = shoppingListItems,
            currencySymbol = currencySymbol,
            budgetLimit = budgetLimit,
            themeMode = themeMode,
            capitalizationMode = capitalizationMode,
        )
    }

    fun exportDatabaseFile(
        context: Context,
        outputStream: OutputStream,
        pieces: List<PieceEntity>,
        products: List<ProductEntity>,
        productPieces: List<ProductPieceCrossRef>,
        shoppingTrips: List<ShoppingTripEntity>,
        offers: List<OfferEntity>,
        offerHistory: List<ProductOfferHistoryEntity>,
        shoppingLists: List<ShoppingListEntity> = emptyList(),
        shoppingListItems: List<ShoppingListItemEntity> = emptyList()
    ) {
        val tempFile = File.createTempFile("export_db", ".db", context.cacheDir)
        if (tempFile.exists()) tempFile.delete()

        val db = SQLiteDatabase.openOrCreateDatabase(tempFile, null)

        db.beginTransaction()
        try {
            // 1. Pieces Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pieces` (
                    `barcode` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `location` TEXT NOT NULL,
                    `stockQuantity` INTEGER NOT NULL,
                    `minStockThreshold` INTEGER NOT NULL,
                    `unitPrice` REAL NOT NULL,
                    `offerType` TEXT NOT NULL,
                    `offerBuyQty` INTEGER NOT NULL,
                    `offerPayQty` INTEGER NOT NULL,
                    `offerFixedPrice` REAL NOT NULL,
                    `offerPercentOff` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`barcode`)
                )
                """.trimIndent()
            )
            val stmtPiece = db.compileStatement("INSERT OR REPLACE INTO `pieces` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            for (p in pieces) {
                stmtPiece.clearBindings()
                stmtPiece.bindString(1, p.barcode)
                stmtPiece.bindString(2, p.name)
                stmtPiece.bindString(3, p.description)
                stmtPiece.bindString(4, p.location)
                stmtPiece.bindLong(5, p.stockQuantity.toLong())
                stmtPiece.bindLong(6, p.minStockThreshold.toLong())
                stmtPiece.bindDouble(7, p.unitPrice)
                stmtPiece.bindString(8, p.offerType)
                stmtPiece.bindLong(9, p.offerBuyQty.toLong())
                stmtPiece.bindLong(10, p.offerPayQty.toLong())
                stmtPiece.bindDouble(11, p.offerFixedPrice)
                stmtPiece.bindDouble(12, p.offerPercentOff)
                stmtPiece.bindLong(13, p.createdAt)
                stmtPiece.executeInsert()
            }

            // 2. Products Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `products` (
                    `productSku` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `stockQuantity` INTEGER NOT NULL,
                    `targetProductionCount` INTEGER NOT NULL,
                    `unitPrice` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`productSku`)
                )
                """.trimIndent()
            )
            val stmtProduct = db.compileStatement("INSERT OR REPLACE INTO `products` VALUES (?, ?, ?, ?, ?, ?, ?)")
            for (prod in products) {
                stmtProduct.clearBindings()
                stmtProduct.bindString(1, prod.productSku)
                stmtProduct.bindString(2, prod.name)
                stmtProduct.bindString(3, prod.description)
                stmtProduct.bindLong(4, prod.stockQuantity.toLong())
                stmtProduct.bindLong(5, prod.targetProductionCount.toLong())
                stmtProduct.bindDouble(6, prod.unitPrice)
                stmtProduct.bindLong(7, prod.createdAt)
                stmtProduct.executeInsert()
            }

            // 3. Product Pieces Cross Ref
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `product_pieces` (
                    `productSku` TEXT NOT NULL,
                    `barcode` TEXT NOT NULL,
                    `quantityRequiredPerProduct` INTEGER NOT NULL,
                    PRIMARY KEY(`productSku`, `barcode`)
                )
                """.trimIndent()
            )
            val stmtRef = db.compileStatement("INSERT OR REPLACE INTO `product_pieces` VALUES (?, ?, ?)")
            for (ref in productPieces) {
                stmtRef.clearBindings()
                stmtRef.bindString(1, ref.productSku)
                stmtRef.bindString(2, ref.barcode)
                stmtRef.bindLong(3, ref.quantityRequiredPerProduct.toLong())
                stmtRef.executeInsert()
            }

            // 4. Shopping Trips Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_trips` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripDate` INTEGER NOT NULL,
                    `totalCost` REAL NOT NULL,
                    `itemCount` INTEGER NOT NULL,
                    `itemsSummary` TEXT NOT NULL,
                    `currencySymbol` TEXT NOT NULL
                )
                """.trimIndent()
            )
            val stmtTrip = db.compileStatement("INSERT OR REPLACE INTO `shopping_trips` VALUES (?, ?, ?, ?, ?, ?)")
            for (t in shoppingTrips) {
                stmtTrip.clearBindings()
                if (t.id != 0L) stmtTrip.bindLong(1, t.id) else stmtTrip.bindNull(1)
                stmtTrip.bindLong(2, t.tripDate)
                stmtTrip.bindDouble(3, t.totalCost)
                stmtTrip.bindLong(4, t.itemCount.toLong())
                stmtTrip.bindString(5, t.itemsSummary)
                stmtTrip.bindString(6, t.currencySymbol)
                stmtTrip.executeInsert()
            }

            // 5. Offers Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `offers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `offerType` TEXT NOT NULL,
                    `buyQty` INTEGER NOT NULL,
                    `payQty` INTEGER NOT NULL,
                    `fixedPrice` REAL NOT NULL,
                    `percentOff` REAL NOT NULL,
                    `storeName` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            val stmtOffer = db.compileStatement("INSERT OR REPLACE INTO `offers` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            for (off in offers) {
                stmtOffer.clearBindings()
                if (off.id != 0L) stmtOffer.bindLong(1, off.id) else stmtOffer.bindNull(1)
                stmtOffer.bindString(2, off.title)
                stmtOffer.bindString(3, off.offerType)
                stmtOffer.bindLong(4, off.buyQty.toLong())
                stmtOffer.bindLong(5, off.payQty.toLong())
                stmtOffer.bindDouble(6, off.fixedPrice)
                stmtOffer.bindDouble(7, off.percentOff)
                stmtOffer.bindString(8, off.storeName)
                stmtOffer.bindString(9, off.notes)
                stmtOffer.bindLong(10, off.createdAt)
                stmtOffer.executeInsert()
            }

            // 6. Product Offer History Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `product_offer_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `barcode` TEXT NOT NULL,
                    `offerId` INTEGER NOT NULL,
                    `appliedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            val stmtHist = db.compileStatement("INSERT OR REPLACE INTO `product_offer_history` VALUES (?, ?, ?, ?)")
            for (h in offerHistory) {
                stmtHist.clearBindings()
                if (h.id != 0L) stmtHist.bindLong(1, h.id) else stmtHist.bindNull(1)
                stmtHist.bindString(2, h.barcode)
                stmtHist.bindLong(3, h.offerId)
                stmtHist.bindLong(4, h.appliedAt)
                stmtHist.executeInsert()
            }

            // 7. Shopping Lists Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_lists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `budgetLimit` REAL NOT NULL,
                    `isArchived` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            val stmtList = db.compileStatement("INSERT OR REPLACE INTO `shopping_lists` VALUES (?, ?, ?, ?, ?)")
            for (list in shoppingLists) {
                stmtList.clearBindings()
                if (list.id != 0L) stmtList.bindLong(1, list.id) else stmtList.bindNull(1)
                stmtList.bindString(2, list.name)
                stmtList.bindDouble(3, list.budgetLimit)
                stmtList.bindLong(4, if (list.isArchived) 1L else 0L)
                stmtList.bindLong(5, list.createdAt)
                stmtList.executeInsert()
            }

            // 8. Shopping List Items Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_list_items` (
                    `listId` INTEGER NOT NULL,
                    `barcode` TEXT NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `unitPrice` REAL NOT NULL,
                    `offerType` TEXT NOT NULL,
                    `offerBuyQty` INTEGER NOT NULL,
                    `offerPayQty` INTEGER NOT NULL,
                    `offerFixedPrice` REAL NOT NULL,
                    `offerPercentOff` REAL NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `isChecked` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`listId`, `barcode`)
                )
                """.trimIndent()
            )
            val stmtListItem = db.compileStatement("INSERT OR REPLACE INTO `shopping_list_items` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            for (item in shoppingListItems) {
                stmtListItem.clearBindings()
                stmtListItem.bindLong(1, item.listId)
                stmtListItem.bindString(2, item.barcode)
                stmtListItem.bindLong(3, item.quantity.toLong())
                stmtListItem.bindDouble(4, item.unitPrice)
                stmtListItem.bindString(5, item.offerType)
                stmtListItem.bindLong(6, item.offerBuyQty.toLong())
                stmtListItem.bindLong(7, item.offerPayQty.toLong())
                stmtListItem.bindDouble(8, item.offerFixedPrice)
                stmtListItem.bindDouble(9, item.offerPercentOff)
                stmtListItem.bindLong(10, item.addedAt)
                stmtListItem.bindLong(11, if (item.isChecked) 1L else 0L)
                stmtListItem.executeInsert()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }

        tempFile.inputStream().use { input ->
            input.copyTo(outputStream)
        }
        outputStream.flush()
        tempFile.delete()
    }

    fun importDatabasePayload(context: Context, inputStream: InputStream): FullBackupPayload {
        val tempFile = File.createTempFile("temp_restore", ".db", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val db = SQLiteDatabase.openDatabase(
            tempFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        fun Cursor.optStr(col: String, default: String = ""): String {
            val idx = getColumnIndex(col)
            if (idx < 0 || isNull(idx)) return default
            return try { getString(idx) ?: default } catch (_: Exception) { default }
        }

        fun Cursor.optInt(col: String, default: Int = 0): Int {
            val idx = getColumnIndex(col)
            if (idx < 0 || isNull(idx)) return default
            return try { getInt(idx) } catch (_: Exception) { default }
        }

        fun Cursor.optLong(col: String, default: Long = 0L): Long {
            val idx = getColumnIndex(col)
            if (idx < 0 || isNull(idx)) return default
            return try { getLong(idx) } catch (_: Exception) { default }
        }

        fun Cursor.optDbl(col: String, default: Double = 0.0): Double {
            val idx = getColumnIndex(col)
            if (idx < 0 || isNull(idx)) return default
            return try { getDouble(idx) } catch (_: Exception) { default }
        }

        val piecesList = mutableListOf<PieceEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM pieces", null)
            while (cursor.moveToNext()) {
                val barcode = cursor.optStr("barcode")
                if (barcode.isBlank()) continue
                piecesList.add(
                    PieceEntity(
                        barcode = barcode,
                        name = cursor.optStr("name", barcode),
                        description = cursor.optStr("description"),
                        location = cursor.optStr("location", "Grocery"),
                        stockQuantity = cursor.optInt("stockQuantity"),
                        minStockThreshold = cursor.optInt("minStockThreshold"),
                        unitPrice = cursor.optDbl("unitPrice"),
                        offerType = cursor.optStr("offerType", "NONE"),
                        offerBuyQty = cursor.optInt("offerBuyQty"),
                        offerPayQty = cursor.optInt("offerPayQty"),
                        offerFixedPrice = cursor.optDbl("offerFixedPrice"),
                        offerPercentOff = cursor.optDbl("offerPercentOff"),
                        createdAt = cursor.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val productsList = mutableListOf<ProductEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM products", null)
            while (cursor.moveToNext()) {
                val sku = cursor.optStr("productSku")
                if (sku.isBlank()) continue
                productsList.add(
                    ProductEntity(
                        productSku = sku,
                        name = cursor.optStr("name", sku),
                        description = cursor.optStr("description"),
                        stockQuantity = cursor.optInt("stockQuantity"),
                        targetProductionCount = cursor.optInt("targetProductionCount"),
                        unitPrice = cursor.optDbl("unitPrice"),
                        createdAt = cursor.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val refList = mutableListOf<ProductPieceCrossRef>()
        try {
            val cursor = db.rawQuery("SELECT * FROM product_pieces", null)
            while (cursor.moveToNext()) {
                val sku = cursor.optStr("productSku")
                val barcode = cursor.optStr("barcode")
                if (sku.isBlank() || barcode.isBlank()) continue
                refList.add(
                    ProductPieceCrossRef(
                        productSku = sku,
                        barcode = barcode,
                        quantityRequiredPerProduct = cursor.optInt("quantityRequiredPerProduct", 1)
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val tripsList = mutableListOf<ShoppingTripEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM shopping_trips", null)
            while (cursor.moveToNext()) {
                tripsList.add(
                    ShoppingTripEntity(
                        id = cursor.optLong("id", 0),
                        tripDate = cursor.optLong("tripDate", System.currentTimeMillis()),
                        totalCost = cursor.optDbl("totalCost"),
                        itemCount = cursor.optInt("itemCount"),
                        itemsSummary = cursor.optStr("itemsSummary"),
                        currencySymbol = cursor.optStr("currencySymbol", "$")
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val offersList = mutableListOf<OfferEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM offers", null)
            while (cursor.moveToNext()) {
                val title = cursor.optStr("title")
                offersList.add(
                    OfferEntity(
                        id = cursor.optLong("id", 0),
                        title = title,
                        offerType = cursor.optStr("offerType", "THREE_FOR_TWO"),
                        buyQty = cursor.optInt("buyQty"),
                        payQty = cursor.optInt("payQty"),
                        fixedPrice = cursor.optDbl("fixedPrice"),
                        percentOff = cursor.optDbl("percentOff"),
                        storeName = cursor.optStr("storeName"),
                        notes = cursor.optStr("notes"),
                        createdAt = cursor.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val historyList = mutableListOf<ProductOfferHistoryEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM product_offer_history", null)
            while (cursor.moveToNext()) {
                val barcode = cursor.optStr("barcode")
                if (barcode.isBlank()) continue
                historyList.add(
                    ProductOfferHistoryEntity(
                        id = cursor.optLong("id", 0),
                        barcode = barcode,
                        offerId = cursor.optLong("offerId"),
                        appliedAt = cursor.optLong("appliedAt", System.currentTimeMillis())
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val shoppingLists = mutableListOf<ShoppingListEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM shopping_lists", null)
            while (cursor.moveToNext()) {
                shoppingLists.add(
                    ShoppingListEntity(
                        id = cursor.optLong("id", 0),
                        name = cursor.optStr("name", "Shopping Basket"),
                        budgetLimit = cursor.optDbl("budgetLimit"),
                        isArchived = cursor.optInt("isArchived") != 0,
                        createdAt = cursor.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        val shoppingListItems = mutableListOf<ShoppingListItemEntity>()
        try {
            val cursor = db.rawQuery("SELECT * FROM shopping_list_items", null)
            while (cursor.moveToNext()) {
                val barcode = cursor.optStr("barcode")
                if (barcode.isBlank()) continue
                shoppingListItems.add(
                    ShoppingListItemEntity(
                        listId = cursor.optLong("listId"),
                        barcode = barcode,
                        quantity = cursor.optInt("quantity", 1),
                        unitPrice = cursor.optDbl("unitPrice"),
                        offerType = cursor.optStr("offerType", "NONE"),
                        offerBuyQty = cursor.optInt("offerBuyQty"),
                        offerPayQty = cursor.optInt("offerPayQty"),
                        offerFixedPrice = cursor.optDbl("offerFixedPrice"),
                        offerPercentOff = cursor.optDbl("offerPercentOff"),
                        addedAt = cursor.optLong("addedAt", System.currentTimeMillis()),
                        isChecked = cursor.optInt("isChecked") != 0
                    )
                )
            }
            cursor.close()
        } catch (_: Exception) {}

        db.close()
        tempFile.delete()

        return FullBackupPayload(
            pieces = piecesList,
            products = productsList,
            productPieces = refList,
            shoppingTrips = tripsList,
            offers = offersList,
            offerHistory = historyList,
            shoppingLists = shoppingLists,
            shoppingListItems = shoppingListItems,
            currencySymbol = "$",
            budgetLimit = 0.0,
            themeMode = "system"
        )
    }
}
