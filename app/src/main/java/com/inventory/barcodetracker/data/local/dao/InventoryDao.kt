package com.inventory.barcodetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

@Dao
abstract class InventoryDao {

    // --- Pieces ---
    @Query("SELECT * FROM pieces ORDER BY name ASC")
    abstract fun getAllPieces(): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces ORDER BY name ASC")
    abstract suspend fun getAllPiecesSync(): List<PieceEntity>

    @Query("SELECT * FROM pieces WHERE barcode = :barcode LIMIT 1")
    abstract suspend fun getPieceByBarcode(barcode: String): PieceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPiece(piece: PieceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPieces(pieces: List<PieceEntity>)

    @Delete
    abstract suspend fun deletePiece(piece: PieceEntity)

    @Query("DELETE FROM pieces WHERE barcode IN (:barcodes)")
    abstract suspend fun deletePiecesByBarcodes(barcodes: List<String>)

    @Query("UPDATE pieces SET stockQuantity = MAX(0, stockQuantity + :delta) WHERE barcode = :barcode")
    abstract suspend fun adjustPieceStock(barcode: String, delta: Int)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    abstract fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    abstract suspend fun getAllProductsSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE productSku = :sku LIMIT 1")
    abstract suspend fun getProductBySku(sku: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProducts(products: List<ProductEntity>)

    @Delete
    abstract suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity + :delta) WHERE productSku = :sku")
    abstract suspend fun adjustProductStock(sku: String, delta: Int)

    // --- BOM Relationships ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProductPiece(crossRef: ProductPieceCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProductPieces(crossRefs: List<ProductPieceCrossRef>)

    @Query("DELETE FROM product_pieces WHERE productSku = :sku AND barcode = :barcode")
    abstract suspend fun removePieceFromProduct(sku: String, barcode: String)

    @Query("""
        SELECT p.barcode, p.name AS pieceName, p.location, p.stockQuantity, pp.quantityRequiredPerProduct AS quantityRequired, p.unitPrice
        FROM product_pieces pp
        INNER JOIN pieces p ON pp.barcode = p.barcode
        WHERE pp.productSku = :sku
        ORDER BY p.name ASC
    """)
    abstract fun getBomForProduct(sku: String): Flow<List<BomItem>>

    @Query("""
        SELECT p.barcode, p.name AS pieceName, p.location, p.stockQuantity, pp.quantityRequiredPerProduct AS quantityRequired, p.unitPrice
        FROM product_pieces pp
        INNER JOIN pieces p ON pp.barcode = p.barcode
        WHERE pp.productSku = :sku
    """)
    abstract suspend fun getBomForProductSync(sku: String): List<BomItem>

    @Query("SELECT * FROM product_pieces")
    abstract suspend fun getAllProductPiecesSync(): List<ProductPieceCrossRef>

    // --- Transactional Build / Assembly ---
    @Transaction
    open suspend fun executeProductBuild(sku: String, buildCount: Int): Boolean {
        if (buildCount <= 0) return false
        val bom = getBomForProductSync(sku)
        if (bom.isEmpty()) return false

        // Check if all pieces have sufficient stock
        for (item in bom) {
            val requiredTotal = item.quantityRequired * buildCount
            if (item.stockQuantity < requiredTotal) {
                return false
            }
        }

        // Deduct pieces
        for (item in bom) {
            val deduction = item.quantityRequired * buildCount
            adjustPieceStock(item.barcode, -deduction)
        }

        // Increment finished product stock
        adjustProductStock(sku, buildCount)
        return true
    }

    @Query("DELETE FROM product_pieces")
    abstract suspend fun clearProductPieces()

    @Query("DELETE FROM pieces")
    abstract suspend fun clearPieces()

    @Query("DELETE FROM products")
    abstract suspend fun clearProducts()

    // --- Shopping Trips History ---
    @Query("SELECT * FROM shopping_trips ORDER BY tripDate DESC")
    abstract fun getAllShoppingTrips(): Flow<List<ShoppingTripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertShoppingTrip(trip: ShoppingTripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertShoppingTrips(trips: List<ShoppingTripEntity>)

    @Delete
    abstract suspend fun deleteShoppingTrip(trip: ShoppingTripEntity)

    @Query("DELETE FROM shopping_trips WHERE id IN (:ids)")
    abstract suspend fun deleteShoppingTripsByIds(ids: List<Long>)

    @Query("DELETE FROM shopping_trips")
    abstract suspend fun clearShoppingTrips()

    @Query("UPDATE pieces SET stockQuantity = 0")
    abstract suspend fun resetPiecesCartQuantity()

    @Query("UPDATE products SET stockQuantity = 0")
    abstract suspend fun resetProductsCartQuantity()

    @Transaction
    open suspend fun resetCartQuantities() {
        resetPiecesCartQuantity()
        resetProductsCartQuantity()
    }

    @Transaction
    open suspend fun clearAllData() {
        clearProductPieces()
        clearPieces()
        clearProducts()
        clearShoppingTrips()
        clearOffers()
        clearShoppingLists()
        clearAllShoppingListItems()
    }

    // --- Saved Offers ---
    @Query("SELECT * FROM offers ORDER BY createdAt DESC")
    abstract fun getAllOffers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers ORDER BY createdAt DESC")
    abstract suspend fun getAllOffersSync(): List<OfferEntity>

    @Query("SELECT * FROM offers WHERE id = :id LIMIT 1")
    abstract suspend fun getOfferById(id: Long): OfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertOffer(offer: OfferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertOffers(offers: List<OfferEntity>)

    @Delete
    abstract suspend fun deleteOffer(offer: OfferEntity)

    @Query("DELETE FROM offers WHERE id IN (:ids)")
    abstract suspend fun deleteOffersByIds(ids: List<Long>)

    @Query("DELETE FROM offers")
    abstract suspend fun clearOffers()

    // --- Product Offer History ---
    @Query("""
        SELECT DISTINCT o.* FROM offers o
        INNER JOIN product_offer_history h ON o.id = h.offerId
        WHERE h.barcode = :barcode
        ORDER BY h.appliedAt DESC
    """)
    abstract fun getOfferHistoryForPiece(barcode: String): Flow<List<OfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun recordOfferHistory(history: ProductOfferHistoryEntity)

    @Query("SELECT * FROM product_offer_history ORDER BY appliedAt DESC")
    abstract suspend fun getAllOfferHistorySync(): List<ProductOfferHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertOfferHistories(history: List<ProductOfferHistoryEntity>)

    // --- Shopping Lists ---
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getAllShoppingLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 ORDER BY createdAt DESC")
    abstract suspend fun getAllShoppingListsSync(): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    abstract suspend fun getShoppingListById(id: Long): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertShoppingList(list: ShoppingListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertShoppingLists(lists: List<ShoppingListEntity>)

    @Delete
    abstract suspend fun deleteShoppingList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id IN (:ids)")
    abstract suspend fun deleteShoppingListsByIds(ids: List<Long>)

    @Query("DELETE FROM shopping_lists")
    abstract suspend fun clearShoppingLists()

    // --- Shopping List Items ---
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId")
    abstract fun getItemsForList(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId")
    abstract suspend fun getItemsForListSync(listId: Long): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items")
    abstract fun getAllShoppingListItems(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items")
    abstract suspend fun getAllShoppingListItemsSync(): List<ShoppingListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertShoppingListItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET unitPrice = :newPrice WHERE barcode = :barcode")
    abstract suspend fun updateShoppingListItemPrice(barcode: String, newPrice: Double)

    @Query("UPDATE shopping_list_items SET isChecked = :isChecked WHERE listId = :listId AND barcode = :barcode")
    abstract suspend fun setShoppingListItemChecked(listId: Long, barcode: String, isChecked: Boolean)

    @Query("UPDATE shopping_list_items SET quantity = :quantity WHERE listId = :listId AND barcode = :barcode")
    abstract suspend fun updateShoppingListItemQuantity(listId: Long, barcode: String, quantity: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertShoppingListItems(items: List<ShoppingListItemEntity>)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId AND barcode = :barcode")
    abstract suspend fun removeShoppingListItem(listId: Long, barcode: String)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    abstract suspend fun clearShoppingListItems(listId: Long)

    @Query("DELETE FROM shopping_list_items")
    abstract suspend fun clearAllShoppingListItems()

    @Transaction
    open suspend fun copyShoppingList(sourceListId: Long, newName: String): Long {
        val newListId = upsertShoppingList(ShoppingListEntity(name = newName))
        val sourceItems = getItemsForListSync(sourceListId)
        val newItems = sourceItems.map { item ->
            item.copy(listId = newListId, addedAt = System.currentTimeMillis(), isChecked = false)
        }
        upsertShoppingListItems(newItems)
        return newListId
    }
}
