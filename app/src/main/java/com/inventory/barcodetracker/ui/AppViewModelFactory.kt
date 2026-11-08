package com.inventory.barcodetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.ui.screens.offers.OffersViewModel
import com.inventory.barcodetracker.ui.screens.pieces.PiecesViewModel
import com.inventory.barcodetracker.ui.screens.products.ProductDetailViewModel
import com.inventory.barcodetracker.ui.screens.products.ProductsViewModel
import com.inventory.barcodetracker.ui.screens.scan.ScanViewModel
import com.inventory.barcodetracker.ui.screens.settings.SettingsViewModel
import com.inventory.barcodetracker.ui.screens.trips.TripsViewModel

class AppViewModelFactory(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
    private val scannerService: BarcodeScannerService? = null,
    private val productSku: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ScanViewModel::class.java) -> {
                ScanViewModel(repository, requireNotNull(scannerService), currencyPreferences) as T
            }
            modelClass.isAssignableFrom(OffersViewModel::class.java) -> {
                OffersViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(PiecesViewModel::class.java) -> {
                PiecesViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(TripsViewModel::class.java) -> {
                TripsViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(ProductsViewModel::class.java) -> {
                ProductsViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(ProductDetailViewModel::class.java) -> {
                ProductDetailViewModel(requireNotNull(productSku), repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository, currencyPreferences) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
