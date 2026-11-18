package com.inventory.barcodetracker.data.repository

import com.inventory.barcodetracker.data.local.dao.InventoryDao
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductOfferHistoryEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import com.inventory.barcodetracker.data.local.model.BomItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InventoryRepository(
    private val inventoryDao: InventoryDao,
) {
    // --- Pieces ---
    val allPieces: Flow<List<PieceEntity>> = inventoryDao.getAllPieces()

    suspend fun getPieceByBarcode(barcode: String): PieceEntity? {
        return inventoryDao.getPieceByBarcode(barcode)
    }

    suspend fun upsertPiece(piece: PieceEntity) {
        inventoryDao.upsertPiece(piece)
    }

    suspend fun deletePiece(piece: PieceEntity) {
        inventoryDao.removeShoppingListItemsByBarcodes(listOf(piece.barcode))
        for (list in inventoryDao.getAllShoppingListsSync()) {
            pruneInvalidMixAndMatchGroupsForList(list.id)
        }
        inventoryDao.deletePiece(piece)
    }

    suspend fun deletePiecesByBarcodes(barcodes: List<String>) {
        if (barcodes.isEmpty()) return
        inventoryDao.removeShoppingListItemsByBarcodes(barcodes)
        for (list in inventoryDao.getAllShoppingListsSync()) {
            pruneInvalidMixAndMatchGroupsForList(list.id)
        }
        inventoryDao.deletePiecesByBarcodes(barcodes)
    }

    suspend fun adjustPieceStock(barcode: String, delta: Int) {
        inventoryDao.adjustPieceStock(barcode, delta)
    }

    suspend fun getAllPiecesSync(): List<PieceEntity> {
        return inventoryDao.getAllPiecesSync()
    }

    // --- Products ---
    val allProducts: Flow<List<ProductEntity>> = inventoryDao.getAllProducts()

    suspend fun getProductBySku(sku: String): ProductEntity? {
        return inventoryDao.getProductBySku(sku)
    }

    suspend fun upsertProduct(product: ProductEntity) {
        inventoryDao.upsertProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        inventoryDao.deleteProduct(product)
    }

    suspend fun adjustProductStock(sku: String, delta: Int) {
        inventoryDao.adjustProductStock(sku, delta)
    }

    suspend fun getAllProductsSync(): List<ProductEntity> {
        return inventoryDao.getAllProductsSync()
    }

    // --- BOM ---
    fun getBomForProduct(sku: String): Flow<List<BomItem>> {
        return inventoryDao.getBomForProduct(sku)
    }

    suspend fun upsertProductPiece(crossRef: ProductPieceCrossRef) {
        inventoryDao.upsertProductPiece(crossRef)
    }

    suspend fun removePieceFromProduct(sku: String, barcode: String) {
        inventoryDao.removePieceFromProduct(sku, barcode)
    }

    suspend fun executeProductBuild(sku: String, buildCount: Int): Boolean {
        return inventoryDao.executeProductBuild(sku, buildCount)
    }

    suspend fun getAllProductPiecesSync(): List<ProductPieceCrossRef> {
        return inventoryDao.getAllProductPiecesSync()
    }

    // --- CSV Batch Operations ---
    suspend fun importPieces(pieces: List<PieceEntity>) {
        inventoryDao.upsertPieces(pieces)
    }

    suspend fun importProducts(products: List<ProductEntity>) {
        inventoryDao.upsertProducts(products)
    }

    suspend fun importProductPieces(crossRefs: List<ProductPieceCrossRef>) {
        inventoryDao.upsertProductPieces(crossRefs)
    }

    // --- Shopping Trips History ---
    val allShoppingTrips: Flow<List<ShoppingTripEntity>> = inventoryDao.getAllShoppingTrips()

    suspend fun saveShoppingTrip(trip: ShoppingTripEntity) {
        inventoryDao.insertShoppingTrip(trip)
    }

    suspend fun deleteShoppingTrip(trip: ShoppingTripEntity) {
        inventoryDao.deleteShoppingTrip(trip)
    }

    suspend fun deleteShoppingTripsByIds(ids: List<Long>) {
        inventoryDao.deleteShoppingTripsByIds(ids)
    }

    suspend fun resetCartQuantities() {
        inventoryDao.resetCartQuantities()
    }

    suspend fun checkoutCartAndSaveTrip(totalCost: Double, itemCount: Int, itemsSummary: String, currencySymbol: String) {
        if (itemCount > 0) {
            val trip = ShoppingTripEntity(
                tripDate = System.currentTimeMillis(),
                totalCost = totalCost,
                itemCount = itemCount,
                itemsSummary = itemsSummary,
                currencySymbol = currencySymbol,
            )
            inventoryDao.insertShoppingTrip(trip)
        }
        inventoryDao.resetCartQuantities()
    }

    suspend fun clearAllData() {
        inventoryDao.clearAllData()
    }

    // --- Saved Offers ---
    val allOffers: Flow<List<OfferEntity>> = inventoryDao.getAllOffers()

    suspend fun getAllOffersSync(): List<OfferEntity> {
        return inventoryDao.getAllOffersSync()
    }

    suspend fun getOfferById(id: Long): OfferEntity? {
        return inventoryDao.getOfferById(id)
    }

    suspend fun upsertOffer(offer: OfferEntity): Long {
        return inventoryDao.upsertOffer(offer)
    }

    suspend fun importOffers(offers: List<OfferEntity>) {
        if (offers.isEmpty()) return
        val existing = inventoryDao.getAllOffersSync()
        val existingById = existing.associateBy { it.id }
        val existingByKey = existing.associateBy { offerKey(it) }.toMutableMap()

        val toUpsert = mutableListOf<OfferEntity>()
        val seenKeys = mutableSetOf<String>()
        for (offer in offers) {
            val key = offerKey(offer)
            if (!seenKeys.add(key)) continue
            val matchedId = when {
                offer.id > 0 && existingById.containsKey(offer.id) -> offer.id
                existingByKey.containsKey(key) -> existingByKey[key]!!.id
                else -> offer.id
            }
            val resolved = offer.copy(id = matchedId)
            toUpsert.add(resolved)
            existingByKey[key] = resolved
        }
        inventoryDao.upsertOffers(toUpsert)
    }

    suspend fun importShoppingTrips(trips: List<ShoppingTripEntity>) {
        if (trips.isEmpty()) return
        val existing = inventoryDao.getAllShoppingTrips().first()
        val existingById = existing.associateBy { it.id }
        val existingByKey = existing.associateBy { tripKey(it) }.toMutableMap()

        val toUpsert = mutableListOf<ShoppingTripEntity>()
        val seenKeys = mutableSetOf<String>()
        for (trip in trips) {
            val key = tripKey(trip)
            if (!seenKeys.add(key)) continue
            val matchedId = when {
                trip.id > 0 && existingById.containsKey(trip.id) -> trip.id
                existingByKey.containsKey(key) -> existingByKey[key]!!.id
                else -> trip.id
            }
            val resolved = trip.copy(id = matchedId)
            toUpsert.add(resolved)
            existingByKey[key] = resolved
        }
        inventoryDao.insertShoppingTrips(toUpsert)
    }

    private fun offerKey(offer: OfferEntity): String =
        "${offer.title}|${offer.offerType}|${offer.buyQty}|${offer.payQty}|${offer.fixedPrice}|${offer.percentOff}|${offer.storeName}|${offer.notes}"

    private fun tripKey(trip: ShoppingTripEntity): String =
        "${trip.tripDate}|${trip.totalCost}|${trip.itemCount}|${trip.itemsSummary}"

    suspend fun deleteOffer(offer: OfferEntity) {
        inventoryDao.deleteOffer(offer)
    }

    suspend fun deleteOffersByIds(ids: List<Long>) {
        inventoryDao.deleteOffersByIds(ids)
    }

    fun getOfferHistoryForPiece(barcode: String): Flow<List<OfferEntity>> {
        return inventoryDao.getOfferHistoryForPiece(barcode)
    }

    suspend fun applyOfferToPiece(barcode: String, offer: OfferEntity) {
        val piece = inventoryDao.getPieceByBarcode(barcode) ?: return
        val updatedPiece = piece.copy(
            offerType = offer.offerType,
            offerBuyQty = offer.buyQty,
            offerPayQty = offer.payQty,
            offerFixedPrice = offer.fixedPrice,
            offerPercentOff = offer.percentOff
        )
        inventoryDao.upsertPiece(updatedPiece)
        inventoryDao.recordOfferHistory(
            ProductOfferHistoryEntity(
                barcode = barcode,
                offerId = offer.id,
                appliedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAllOfferHistorySync(): List<ProductOfferHistoryEntity> {
        return inventoryDao.getAllOfferHistorySync()
    }

    suspend fun importOfferHistory(history: List<ProductOfferHistoryEntity>) {
        if (history.isEmpty()) return
        val existing = inventoryDao.getAllOfferHistorySync()
        val existingById = existing.associateBy { it.id }
        val existingByKey = existing.associateBy { "${it.barcode}|${it.offerId}|${it.appliedAt}" }.toMutableMap()

        val toUpsert = mutableListOf<ProductOfferHistoryEntity>()
        val seenKeys = mutableSetOf<String>()
        for (item in history) {
            val key = "${item.barcode}|${item.offerId}|${item.appliedAt}"
            if (!seenKeys.add(key)) continue
            val matchedId = when {
                item.id > 0 && existingById.containsKey(item.id) -> item.id
                existingByKey.containsKey(key) -> existingByKey[key]!!.id
                else -> item.id
            }
            val resolved = item.copy(id = matchedId)
            toUpsert.add(resolved)
            existingByKey[key] = resolved
        }
        inventoryDao.upsertOfferHistories(toUpsert)
    }

    // --- Shopping Lists ---
    val allShoppingLists: Flow<List<ShoppingListEntity>> = inventoryDao.getAllShoppingLists()
    val allShoppingListItems: Flow<List<ShoppingListItemEntity>> = inventoryDao.getAllShoppingListItems()

    suspend fun getAllShoppingListsSync(): List<ShoppingListEntity> {
        return inventoryDao.getAllShoppingListsSync()
    }

    suspend fun upsertShoppingList(list: ShoppingListEntity): Long {
        return inventoryDao.upsertShoppingList(list)
    }

    suspend fun importShoppingLists(lists: List<ShoppingListEntity>) {
        if (lists.isEmpty()) return
        val existing = inventoryDao.getAllShoppingListsSync()
        val existingById = existing.associateBy { it.id }
        val existingByKey = existing.associateBy { "${it.name}|${it.createdAt}" }.toMutableMap()

        val toUpsert = mutableListOf<ShoppingListEntity>()
        val seenKeys = mutableSetOf<String>()
        for (list in lists) {
            val key = "${list.name}|${list.createdAt}"
            if (!seenKeys.add(key)) continue
            val matchedId = when {
                list.id > 0 && existingById.containsKey(list.id) -> list.id
                existingByKey.containsKey(key) -> existingByKey[key]!!.id
                else -> list.id
            }
            val resolved = list.copy(id = matchedId)
            toUpsert.add(resolved)
            existingByKey[key] = resolved
        }
        inventoryDao.upsertShoppingLists(toUpsert)
    }

    suspend fun deleteShoppingList(list: ShoppingListEntity) {
        inventoryDao.deleteShoppingList(list)
    }

    suspend fun deleteShoppingListsByIds(ids: List<Long>) {
        inventoryDao.deleteShoppingListsByIds(ids)
    }

    suspend fun copyShoppingList(sourceListId: Long, newName: String): Long {
        return inventoryDao.copyShoppingList(sourceListId, newName)
    }

    fun getItemsForList(listId: Long): Flow<List<ShoppingListItemEntity>> {
        return inventoryDao.getItemsForList(listId)
    }

    suspend fun getItemsForListSync(listId: Long): List<ShoppingListItemEntity> {
        return inventoryDao.getItemsForListSync(listId)
    }

    suspend fun getAllShoppingListItemsSync(): List<ShoppingListItemEntity> {
        return inventoryDao.getAllShoppingListItemsSync()
    }

    suspend fun upsertShoppingListItem(item: ShoppingListItemEntity) {
        inventoryDao.upsertShoppingListItem(item)
    }

    suspend fun updateShoppingListItemPrice(barcode: String, newPrice: Double) {
        inventoryDao.updateShoppingListItemPrice(barcode, newPrice)
    }

    suspend fun setShoppingListItemChecked(listId: Long, barcode: String, isChecked: Boolean) {
        inventoryDao.setShoppingListItemChecked(listId, barcode, isChecked)
    }

    suspend fun updateShoppingListItemQuantity(listId: Long, barcode: String, quantity: Int) {
        if (quantity <= 0) {
            inventoryDao.removeShoppingListItem(listId, barcode)
        } else {
            inventoryDao.updateShoppingListItemQuantity(listId, barcode, quantity)
        }
        pruneInvalidMixAndMatchGroupsForList(listId)
    }

    suspend fun importShoppingListItems(items: List<ShoppingListItemEntity>) {
        inventoryDao.upsertShoppingListItems(items)
    }

    suspend fun removeShoppingListItem(listId: Long, barcode: String) {
        inventoryDao.removeShoppingListItem(listId, barcode)
        pruneInvalidMixAndMatchGroupsForList(listId)
    }

    private suspend fun pruneInvalidMixAndMatchGroupsForList(listId: Long) {
        val items = inventoryDao.getItemsForListSync(listId)
        val grouped = items.filter { it.offerGroupId.isNotBlank() }.groupBy { it.offerGroupId }
        for ((_, groupItems) in grouped) {
            val rep = groupItems.first()
            val totalUnits = groupItems.sumOf { it.quantity }
            val hasEnoughDistinctItems = groupItems.size >= 2
            val hasEnoughTotalUnits = when (rep.offerType) {
                "THREE_FOR_TWO" -> totalUnits >= (if (rep.offerBuyQty > 0) rep.offerBuyQty else 3)
                "MULTI_BUY_FIXED" -> totalUnits >= (if (rep.offerBuyQty > 0) rep.offerBuyQty else 2)
                else -> true
            }
            if (!hasEnoughDistinctItems || !hasEnoughTotalUnits) {
                for (item in groupItems) {
                    inventoryDao.upsertShoppingListItem(
                        item.copy(
                            offerGroupId = "",
                            offerType = "NO_OFFER",
                            offerBuyQty = 0,
                            offerPayQty = 0,
                            offerFixedPrice = 0.0,
                            offerPercentOff = 0.0,
                        )
                    )
                }
            }
        }
    }

    suspend fun clearShoppingListItems(listId: Long) {
        inventoryDao.clearShoppingListItems(listId)
    }

    suspend fun reloadTripIntoCart(trip: ShoppingTripEntity): String {
        val firstLine = trip.itemsSummary.lineSequence().firstOrNull()?.trim() ?: ""
        val extractedName = if (firstLine.startsWith("Basket:") || firstLine.startsWith("List:")) {
            firstLine.substringAfter(":").trim()
        } else {
            "Reloaded " + java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(trip.tripDate))
        }

        val existingLists = inventoryDao.getAllShoppingListsSync()
        val targetList = existingLists.firstOrNull { it.name.equals(extractedName, ignoreCase = true) }
        val targetListId = if (targetList != null) {
            targetList.id
        } else {
            inventoryDao.upsertShoppingList(ShoppingListEntity(name = extractedName))
        }

        val summaryLines = trip.itemsSummary.split("\n")
        val currentPieces = inventoryDao.getAllPiecesSync().associateBy { it.name.lowercase() }

        var currentGroupId = ""
        var currentOfferType = "NONE"
        var currentBuyQty = 0
        var currentPayQty = 0
        var currentFixedPrice = 0.0
        var currentPercentOff = 0.0

        for (line in summaryLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                currentGroupId = ""
                currentOfferType = "NONE"
                currentBuyQty = 0
                currentPayQty = 0
                currentFixedPrice = 0.0
                currentPercentOff = 0.0
                continue
            }
            if (trimmed.startsWith("🏷️ Mix & Match:")) {
                val detail = trimmed.removePrefix("🏷️ Mix & Match:").trim()
                currentGroupId = "MM_${System.currentTimeMillis()}_${(100..999).random()}"
                val multiBuyMatch = Regex("""(\d+)\s+for\s+(?:[£$€]|RON)?\s*([\d.]+)""").find(detail)
                val buyPayMatch = Regex("""(\d+)\s+for\s+(\d+)""").find(detail)
                val pctMatch = Regex("""([\d.]+)%\s*Off""", RegexOption.IGNORE_CASE).find(detail)
                val reducedMatch = Regex("""Now\s+(?:[£$€]|RON)?\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(detail)

                when {
                    multiBuyMatch != null && !detail.matches(Regex("""^\d+\s+for\s+\d+(\s.*)?$""")) -> {
                        currentOfferType = "MULTI_BUY_FIXED"
                        currentBuyQty = multiBuyMatch.groupValues[1].toIntOrNull() ?: 2
                        currentPayQty = 0
                        currentFixedPrice = multiBuyMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                        currentPercentOff = 0.0
                    }
                    buyPayMatch != null -> {
                        currentOfferType = "THREE_FOR_TWO"
                        currentBuyQty = buyPayMatch.groupValues[1].toIntOrNull() ?: 3
                        currentPayQty = buyPayMatch.groupValues[2].toIntOrNull() ?: 2
                        currentFixedPrice = 0.0
                        currentPercentOff = 0.0
                    }
                    pctMatch != null -> {
                        currentOfferType = "PERCENT_OFF"
                        currentBuyQty = 0
                        currentPayQty = 0
                        currentFixedPrice = 0.0
                        currentPercentOff = pctMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    }
                    reducedMatch != null -> {
                        currentOfferType = "REDUCED_PRICE"
                        currentBuyQty = 0
                        currentPayQty = 0
                        currentFixedPrice = reducedMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                        currentPercentOff = 0.0
                    }
                    else -> {
                        currentGroupId = ""
                    }
                }
                continue
            }
            if (!trimmed.startsWith("•")) continue
            val regex = Regex("""•\s*(.*?)\s*\(x(\d+)\)""")
            val match = regex.find(trimmed) ?: continue
            val itemName = match.groupValues[1].trim()
            val qty = match.groupValues[2].toIntOrNull() ?: 1

            val existing = currentPieces[itemName.lowercase()]
            val barcode = if (existing != null) {
                inventoryDao.upsertPiece(existing.copy(stockQuantity = qty))
                existing.barcode
            } else {
                val newBarcode = "RELOAD_" + System.currentTimeMillis() + "_" + (100..999).random()
                inventoryDao.upsertPiece(
                    PieceEntity(
                        barcode = newBarcode,
                        name = itemName,
                        stockQuantity = qty,
                        unitPrice = 0.0,
                        createdAt = System.currentTimeMillis()
                    )
                )
                newBarcode
            }

            inventoryDao.upsertShoppingListItem(
                ShoppingListItemEntity(
                    listId = targetListId,
                    barcode = barcode,
                    quantity = qty,
                    unitPrice = existing?.unitPrice ?: 0.0,
                    offerType = if (currentGroupId.isNotBlank()) currentOfferType else "NONE",
                    offerBuyQty = if (currentGroupId.isNotBlank()) currentBuyQty else 0,
                    offerPayQty = if (currentGroupId.isNotBlank()) currentPayQty else 0,
                    offerFixedPrice = if (currentGroupId.isNotBlank()) currentFixedPrice else 0.0,
                    offerPercentOff = if (currentGroupId.isNotBlank()) currentPercentOff else 0.0,
                    addedAt = System.currentTimeMillis(),
                    offerGroupId = currentGroupId
                )
            )
        }
        return extractedName
    }

    // --- Sample Data Seeding ---
    suspend fun seedSampleData() {
        val samplePieces = listOf(
            PieceEntity(
                barcode = "5099874287742",
                name = "Steak Burgers",
                description = "2 Pack Angus Beef Patties",
                location = "Meat & Poultry",
                stockQuantity = 2,
                minStockThreshold = 0,
                unitPrice = 4.75,
            ),
            PieceEntity(
                barcode = "5099874212041",
                name = "Chilli Cheddar Slices",
                description = "Spicy Mature Cheddar Slices 200g",
                location = "Dairy & Cheese",
                stockQuantity = 1,
                minStockThreshold = 0,
                unitPrice = 1.95,
            ),
            PieceEntity(
                barcode = "5099874378556",
                name = "Cream Cheese Sweet Peppers",
                description = "Stuffed Red Peppers 150g",
                location = "Deli & Snacks",
                stockQuantity = 1,
                minStockThreshold = 0,
                unitPrice = 2.20,
            ),
            PieceEntity(
                barcode = "5000159461122",
                name = "Organic Whole Milk 2L",
                description = "Fresh Dairy Milk 2 Litres",
                location = "Dairy & Cheese",
                stockQuantity = 0,
                minStockThreshold = 0,
                unitPrice = 1.65,
            ),
            PieceEntity(
                barcode = "5010044002102",
                name = "Extra Virgin Olive Oil 500ml",
                description = "Cold Pressed Olive Oil",
                location = "Oils & Condiments",
                stockQuantity = 0,
                minStockThreshold = 0,
                unitPrice = 5.49,
            ),
        )

        inventoryDao.upsertPieces(samplePieces)

        val sampleOffers = listOf(
            OfferEntity(
                title = "3 for 2 Mix & Match Deal",
                offerType = "THREE_FOR_TWO",
                buyQty = 3,
                payQty = 2,
                storeName = "Tesco",
                notes = "Buy 3 items, cheapest is free"
            ),
            OfferEntity(
                title = "2 for £5 Grill Promotion",
                offerType = "MULTI_BUY_FIXED",
                buyQty = 2,
                fixedPrice = 5.0,
                storeName = "Sainsbury's",
                notes = "Select 2 items for £5.00 total"
            ),
            OfferEntity(
                title = "50% Half Price Clearout",
                offerType = "PERCENT_OFF",
                percentOff = 50.0,
                storeName = "Asda",
                notes = "50% off standard shelf price"
            )
        )
        inventoryDao.upsertOffers(sampleOffers)
    }
}
