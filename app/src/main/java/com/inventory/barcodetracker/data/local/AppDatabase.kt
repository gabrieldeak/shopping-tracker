package com.inventory.barcodetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inventory.barcodetracker.data.local.dao.InventoryDao
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductOfferHistoryEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pieces ADD COLUMN unitPrice REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE products ADD COLUMN unitPrice REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_trips` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tripDate` INTEGER NOT NULL,
                `totalCost` REAL NOT NULL,
                `itemCount` INTEGER NOT NULL,
                `itemsSummary` TEXT NOT NULL,
                `currencySymbol` TEXT NOT NULL DEFAULT '$'
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pieces ADD COLUMN offerType TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE pieces ADD COLUMN offerBuyQty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pieces ADD COLUMN offerPayQty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pieces ADD COLUMN offerFixedPrice REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE pieces ADD COLUMN offerPercentOff REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `offers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `offerType` TEXT NOT NULL,
                `buyQty` INTEGER NOT NULL DEFAULT 0,
                `payQty` INTEGER NOT NULL DEFAULT 0,
                `fixedPrice` REAL NOT NULL DEFAULT 0.0,
                `percentOff` REAL NOT NULL DEFAULT 0.0,
                `storeName` TEXT NOT NULL DEFAULT '',
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
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
    }
}

@Database(
    entities = [
        PieceEntity::class,
        ProductEntity::class,
        ProductPieceCrossRef::class,
        ShoppingTripEntity::class,
        OfferEntity::class,
        ProductOfferHistoryEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barcode_inventory.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
