package com.inventory.barcodetracker.di

import android.content.Context
import com.inventory.barcodetracker.data.local.AppDatabase
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService

interface AppContainer {
    val inventoryRepository: InventoryRepository
    val barcodeScannerService: BarcodeScannerService
    val currencyPreferences: CurrencyPreferences
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(database.inventoryDao())
    }

    override val barcodeScannerService: BarcodeScannerService by lazy {
        BarcodeScannerService(context)
    }

    override val currencyPreferences: CurrencyPreferences by lazy {
        CurrencyPreferences(context)
    }
}
