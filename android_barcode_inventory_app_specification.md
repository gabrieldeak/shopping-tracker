# End-to-End Implementation Specification: Android Barcode Inventory App

## 1. Project Overview & Goal
Build a local-first, offline-ready Android application in Kotlin using Jetpack Compose, Room Database, and the Google Code Scanner API. The app enables users to scan barcodes/QR codes, maintain an inventory of individual **pieces/parts**, define assembled **products**, and track **Bill of Materials (BOM)** relationships linking pieces to products, with import/export capabilities.

---

## 2. Technical Stack & Requirements

* **Language:** Kotlin 2.0+
* **UI Toolkit:** Jetpack Compose (Material3)
* **Architecture:** MVVM + Clean Architecture / Repository Pattern with Kotlin Coroutines and `Flow`
* **Local Database:** Room Database (SQLite abstraction)
* **Barcode Scanning:** Google Code Scanner API (`com.google.android.gms:play-services-code-scanner`)
  * *Why:* Requires zero runtime camera permissions, handles auto-focus/flash automatically, zero app size bloat (uses Play Services model).
* **Dependency Injection:** Hilt or Koin (prefer Hilt for standard Jetpack integration, or clean manual DI if keeping boilerplate minimal).
* **CSV Export/Import:** Native Kotlin / Android Storage Access Framework (`ActivityResultContracts.CreateDocument`).

---

## 3. Dependency Configuration (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.inventory.barcodetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.inventory.barcodetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose & Material 3
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Google Code Scanner (Play Services)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
```

---

## 4. Database Schema & Data Models

### 4.1 Entities

#### 1. `PieceEntity` (Individual parts/components)
```kotlin
package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pieces")
data class PieceEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val description: String = "",
    val location: String = "Default Bin", // e.g., Shelf 2, Drawer B
    val stockQuantity: Int = 0,
    val minStockThreshold: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 2. `ProductEntity` (Finished assemblies or products)
```kotlin
package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val productSku: String, // Can also be a scanned barcode or custom SKU
    val name: String,
    val description: String = "",
    val targetProductionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 3. `ProductPieceCrossRef` (BOM / Junction Table)
```kotlin
package com.inventory.barcodetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_pieces",
    primaryKeys = ["productSku", "barcode"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["productSku"],
            childColumns = ["productSku"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PieceEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["productSku"]), Index(value = ["barcode"])]
)
data class ProductPieceCrossRef(
    val productSku: String,
    val barcode: String,
    val quantityRequiredPerProduct: Int = 1
)
```

### 4.2 Relations & POJOs
```kotlin
package com.inventory.barcodetracker.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef

data class ProductWithPieces(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "productSku",
        entityColumn = "barcode",
        associateBy = Junction(ProductPieceCrossRef::class)
    )
    val pieces: List<PieceEntity>
)

data class BomItem(
    val barcode: String,
    val pieceName: String,
    val location: String,
    val stockQuantity: Int,
    val quantityRequired: Int
)
```

---

## 5. Data Access Objects (DAOs)

```kotlin
package com.inventory.barcodetracker.data.local.dao

import androidx.room.*
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.model.BomItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    // Pieces
    @Query("SELECT * FROM pieces ORDER BY name ASC")
    fun getAllPieces(): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces WHERE barcode = :barcode LIMIT 1")
    suspend fun getPieceByBarcode(barcode: String): PieceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPiece(piece: PieceEntity)

    @Delete
    suspend fun deletePiece(piece: PieceEntity)

    @Query("UPDATE pieces SET stockQuantity = stockQuantity + :delta WHERE barcode = :barcode")
    suspend fun adjustPieceStock(barcode: String, delta: Int)

    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE productSku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // BOM Relationships
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductPiece(crossRef: ProductPieceCrossRef)

    @Query("DELETE FROM product_pieces WHERE productSku = :sku AND barcode = :barcode")
    suspend fun removePieceFromProduct(sku: String, barcode: String)

    @Query("""
        SELECT p.barcode, p.name AS pieceName, p.location, p.stockQuantity, pp.quantityRequiredPerProduct AS quantityRequired
        FROM product_pieces pp
        INNER JOIN pieces p ON pp.barcode = p.barcode
        WHERE pp.productSku = :sku
    """)
    fun getBomForProduct(sku: String): Flow<List<BomItem>>
}
```

---

## 6. Scanner Service Implementation

```kotlin
package com.inventory.barcodetracker.scanner

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.tasks.await

class BarcodeScannerService(private val context: Context) {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX
        )
        .enableAutoZoom()
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    suspend fun startScan(): String? {
        return try {
            val barcode = scanner.startScan().await()
            barcode.rawValue
        } catch (e: Exception) {
            // Scanner canceled, failed, or module not loaded
            null
        }
    }

    fun preloadScannerModule() {
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()
        ModuleInstall.getClient(context).installModules(moduleInstallRequest)
    }
}
```

---

## 7. App Screens & User Flows

The agent must implement the following Compose screens with a bottom navigation bar:

1. **Dashboard / Quick Scan (`/scan`):**
   * Big "Scan Barcode" Floating Action Button.
   * On scan complete:
     * Check if piece exists: If yes, open a modal with details, allowing quick $+1 / -1$ stock adjustments.
     * If not found: Pre-populate the scanned barcode into an "Add New Piece / Product" bottom-sheet dialog.
   * Quick search bar to manually look up barcodes or names.

2. **Pieces / Parts Catalog (`/pieces`):**
   * Filterable list of all parts showing: Barcode, Name, Location, Stock Count.
   * Color highlight when `stockQuantity <= minStockThreshold`.
   * Swipe-to-delete or edit dialog.

3. **Products & BOMs (`/products`):**
   * List of finished assemblies.
   * Detail view for a product:
     * Shows linked pieces and required quantities.
     * Shows **"Build Capacity"**: Calculates the maximum number of products that can currently be assembled based on smallest `floor(piece.stockQuantity / bom.quantityRequired)`.
     * "Deduct Stock for Build" button: Decrements stock for all linked pieces according to build quantity in a single database transaction.

4. **Settings & Data Management (`/settings`):**
   * **Export CSV:** Dumps `pieces.csv`, `products.csv`, and `bom.csv` to user-selected device storage.
   * **Import CSV:** Parses input CSVs and upserts into Room.

---

## 8. Step-by-Step Agent Execution Checklist

1. [ ] **Project Setup:** Initialize a standard Android Studio project with `minSdk = 26` and target Compose Material 3.
2. [ ] **Dependencies:** Insert Room, KSP, Play Services Code Scanner, and Lifecycle dependencies into `build.gradle.kts`.
3. [ ] **Entity & DAO Layer:** Create `PieceEntity`, `ProductEntity`, `ProductPieceCrossRef`, and `InventoryDao`.
4. [ ] **Database Class:** Build `AppDatabase : RoomDatabase` with fallback migration or version 1 setup.
5. [ ] **Repository Layer:** Create `InventoryRepository` exposing clean coroutine and `Flow` methods.
6. [ ] **Scanner Service:** Implement `BarcodeScannerService` wrapping `GmsBarcodeScanning`.
7. [ ] **ViewModels:**
   * `InventoryViewModel`: Handles piece catalog, scan resolution, and quick stock updates.
   * `ProductDetailViewModel`: Handles BOM calculation and build execution.
8. [ ] **UI Implementation:**
   * Bottom Navigation (`Scaffold`, `NavigationBar`).
   * `PiecesScreen` with search filtering and inline count stepper.
   * `ScanResolutionSheet` (Bottom sheet that prompts to add or edit immediately after scanning).
   * `ProductDetailScreen` with BOM assembly calculations.
9. [ ] **Data Export Service:** Add a simple CSV serializer using Android `ContentResolver` to let the user save to local storage.
10. [ ] **Testing & Verification:** Verify on a device or emulator with Play Services that the code scanner dialog launches, captures, and commits to SQLite.