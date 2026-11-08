package com.inventory.barcodetracker

import android.app.Application
import com.inventory.barcodetracker.di.AppContainer
import com.inventory.barcodetracker.di.DefaultAppContainer

class InventoryApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        // Best-effort preload Google Play Services code scanner module
        container.barcodeScannerService.preloadScannerModule()
    }
}
