package com.inventory.barcodetracker.ui.screens.settings

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.service.BackupRestoreService
import com.inventory.barcodetracker.service.CsvService
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol
    val budgetLimit: StateFlow<Double> = currencyPreferences.budgetLimit
    val themeMode: StateFlow<String> = currencyPreferences.themeMode

    fun setCurrencySymbol(symbol: String) {
        currencyPreferences.setCurrencySymbol(symbol)
        viewModelScope.launch {
            _snackbarMessages.emit("Currency symbol updated to $symbol")
        }
    }

    fun setBudgetLimit(limit: Double) {
        currencyPreferences.setBudgetLimit(limit)
        viewModelScope.launch {
            _snackbarMessages.emit("Shopping budget limit updated")
        }
    }

    fun setThemeMode(mode: String) {
        currencyPreferences.setThemeMode(mode)
        viewModelScope.launch {
            _snackbarMessages.emit("App theme updated to $mode")
        }
    }

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun exportItems(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pieces = repository.getAllPiecesSync()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        CsvService.exportPieces(outputStream, pieces)
                    }
                }
                _snackbarMessages.emit("Successfully exported shopping_items.csv")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to export shopping items: ${e.message}")
            }
        }
    }

    fun importItems(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val pieces = CsvService.importPieces(inputStream)
                        repository.importPieces(pieces)
                        pieces.size
                    } ?: 0
                }
                _snackbarMessages.emit("Imported $count shopping item(s) into My Items.")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to import items: ${e.message}")
            }
        }
    }

    fun exportHistory(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        CsvService.exportTrips(outputStream, trips)
                    }
                }
                _snackbarMessages.emit("Successfully exported shopping_history.csv")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to export history: ${e.message}")
            }
        }
    }

    fun importHistory(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val trips = CsvService.importTrips(inputStream)
                        trips.forEach { repository.saveShoppingTrip(it) }
                        trips.size
                    } ?: 0
                }
                _snackbarMessages.emit("Imported $count receipt(s) into History.")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to import history: ${e.message}")
            }
        }
    }

    fun exportFullBackup(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pieces = repository.getAllPiecesSync()
                    val products = repository.getAllProductsSync()
                    val crossRefs = repository.getAllProductPiecesSync()
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    val offers = repository.getAllOffersSync()

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BackupRestoreService.exportFullBackup(
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                        )
                    }
                }
                _snackbarMessages.emit("Full backup file saved successfully!")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to export backup: ${e.message}")
            }
        }
    }

    fun restoreFullBackup(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val payload = BackupRestoreService.importFullBackup(inputStream)
                        currencyPreferences.setCurrencySymbol(payload.currencySymbol)
                        currencyPreferences.setBudgetLimit(payload.budgetLimit)
                        currencyPreferences.setThemeMode(payload.themeMode)

                        repository.importPieces(payload.pieces)
                        repository.importProducts(payload.products)
                        repository.importProductPieces(payload.productPieces)
                        payload.shoppingTrips.forEach { repository.saveShoppingTrip(it) }
                        payload.offers.forEach { repository.upsertOffer(it) }
                    }
                }
                _snackbarMessages.emit("Full backup restored successfully!")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to restore backup: ${e.message}")
            }
        }
    }

    fun shareFullBackup(context: Context) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "shared_backups")
                    cacheDir.mkdirs()
                    val backupFile = File(cacheDir, "barcode_tracker_backup.json")

                    val pieces = repository.getAllPiecesSync()
                    val products = repository.getAllProductsSync()
                    val crossRefs = repository.getAllProductPiecesSync()
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    val offers = repository.getAllOffersSync()

                    FileOutputStream(backupFile).use { outputStream ->
                        BackupRestoreService.exportFullBackup(
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                        )
                    }
                    backupFile
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Shopping Tracker Full Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share Backup via Google Drive, Email, etc."))
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to share backup: ${e.message}")
            }
        }
    }

    fun shareAppApk(context: Context) {
        viewModelScope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    val sourceApk = File(context.applicationInfo.sourceDir)
                    val cacheDir = File(context.cacheDir, "shared_apks")
                    cacheDir.mkdirs()
                    val targetApk = File(cacheDir, "Shopping_Tracker.apk")
                    sourceApk.copyTo(targetApk, overwrite = true)
                    targetApk
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Shopping Tracker App")
                    putExtra(Intent.EXTRA_TEXT, "Here is the Shopping Tracker Android App installer (APK).")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share App APK via Bluetooth, Drive, Email..."))
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to share app APK: ${e.message}")
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
                repository.seedSampleData()
                _snackbarMessages.emit("Sample parts, products, and BOM seeded successfully!")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to seed data: ${e.message}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _snackbarMessages.emit("Cleared all inventory, products, and BOM data.")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to clear data: ${e.message}")
            }
        }
    }
}
