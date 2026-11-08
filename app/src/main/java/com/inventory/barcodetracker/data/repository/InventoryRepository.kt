package com.inventory.barcodetracker.data.repository

import com.inventory.barcodetracker.data.local.dao.InventoryDao
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductOfferHistoryEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
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

    suspend fun deleteOffer(offer: OfferEntity) {
        inventoryDao.deleteOffer(offer)
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
