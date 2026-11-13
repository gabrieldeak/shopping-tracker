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
        inventoryDao.deletePiece(piece)
    }

    suspend fun deletePiecesByBarcodes(barcodes: List<String>) {
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
        inventoryDao.upsertOffers(offers)
    }

    suspend fun importShoppingTrips(trips: List<ShoppingTripEntity>) {
        inventoryDao.insertShoppingTrips(trips)
    }

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
        inventoryDao.upsertOfferHistories(history)
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
        inventoryDao.upsertShoppingLists(lists)
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

    suspend fun importShoppingListItems(items: List<ShoppingListItemEntity>) {
        inventoryDao.upsertShoppingListItems(items)
    }

    suspend fun removeShoppingListItem(listId: Long, barcode: String) {
        inventoryDao.removeShoppingListItem(listId, barcode)
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

        for (line in summaryLines) {
            val trimmed = line.trim()
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
                    addedAt = System.currentTimeMillis()
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
