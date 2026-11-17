package com.inventory.barcodetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.ui.screens.history.HistoryViewModel
import com.inventory.barcodetracker.ui.screens.items.ItemsViewModel
import com.inventory.barcodetracker.ui.screens.offers.OffersViewModel
import com.inventory.barcodetracker.ui.screens.quickscan.QuickScanViewModel
import com.inventory.barcodetracker.ui.screens.settings.SettingsViewModel

class AppViewModelFactory(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
    private val scannerService: BarcodeScannerService? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(QuickScanViewModel::class.java) -> {
                QuickScanViewModel(repository, requireNotNull(scannerService), currencyPreferences) as T
            }
            modelClass.isAssignableFrom(OffersViewModel::class.java) -> {
                OffersViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(ItemsViewModel::class.java) -> {
                ItemsViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(repository, currencyPreferences) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository, currencyPreferences) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
