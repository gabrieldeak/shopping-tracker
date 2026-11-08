package com.inventory.barcodetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
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

    @Delete
    abstract suspend fun deleteShoppingTrip(trip: ShoppingTripEntity)

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
    }
}
