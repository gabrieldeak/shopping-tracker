package com.inventory.barcodetracker.ui.screens.settings

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.AppDatabase
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.service.BackupRestoreService
import com.inventory.barcodetracker.service.CsvService
import com.inventory.barcodetracker.service.OpenFoodFactsHealthResult
import com.inventory.barcodetracker.service.OpenFoodFactsService
import com.inventory.barcodetracker.ui.i18n.AppStrings
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    private val strings: AppStrings
        get() = AppStrings.forLanguage(currencyPreferences.appLanguage.value)

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol
    val budgetLimit: StateFlow<Double> = currencyPreferences.budgetLimit
    val themeMode: StateFlow<String> = currencyPreferences.themeMode
    val capitalizationMode: StateFlow<String> = currencyPreferences.capitalizationMode
    val appLanguage: StateFlow<String> = currencyPreferences.appLanguage
    val hideTutorialOnStartup: StateFlow<Boolean> = currencyPreferences.hideTutorialOnStartup

    fun setHideTutorialOnStartup(hide: Boolean) {
        currencyPreferences.setHideTutorialOnStartup(hide)
    }

    fun setCurrencySymbol(symbol: String) {
        currencyPreferences.setCurrencySymbol(symbol)
        viewModelScope.launch {
            _snackbarMessages.emit(String.format(Locale.US, strings.vmCurrencyUpdatedFmt, symbol))
        }
    }

    fun setBudgetLimit(limit: Double) {
        currencyPreferences.setBudgetLimit(limit)
        viewModelScope.launch {
            _snackbarMessages.emit(strings.vmBudgetLimitUpdated)
        }
    }

    fun setThemeMode(mode: String) {
        currencyPreferences.setThemeMode(mode)
        viewModelScope.launch {
            _snackbarMessages.emit(String.format(Locale.US, strings.vmThemeUpdatedFmt, mode))
        }
    }

    fun setCapitalizationMode(mode: String) {
        currencyPreferences.setCapitalizationMode(mode)
        viewModelScope.launch {
            _snackbarMessages.emit(strings.vmCapModeUpdated)
        }
    }

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    sealed interface BarcodeHealthState {
        data object Idle : BarcodeHealthState
        data object Checking : BarcodeHealthState
        data class Result(val result: OpenFoodFactsHealthResult) : BarcodeHealthState
    }

    enum class BackupFileType(val extension: String, val label: String) {
        JSON("json", "JSON"),
        DB("db", ".DB"),
        CSV("csv", "CSV"),
    }

    data class AvailableBackupFile(
        val uri: Uri,
        val displayName: String,
        val type: BackupFileType,
        val modifiedAtMillis: Long,
        val sizeBytes: Long,
    )

    private val _availableBackups = MutableStateFlow<List<AvailableBackupFile>>(emptyList())
    val availableBackups: StateFlow<List<AvailableBackupFile>> = _availableBackups.asStateFlow()

    private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri, fallback: String): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    private fun recordRecentBackupUri(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        val prefs = context.getSharedPreferences("recent_backup_uris", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.add(uri.toString())
        prefs.edit().putStringSet("uris", existing).apply()
    }

    private fun isUriReadable(cr: ContentResolver, uri: Uri): Boolean {
        return try {
            cr.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.length != 0L
            } ?: cr.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun loadAvailableBackups(context: Context, filterType: BackupFileType) {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                val results = linkedMapOf<String, AvailableBackupFile>()
                val cr = context.contentResolver

                // 1. Check persisted SAF URIs (from previous saves/restores)
                val prefs = context.getSharedPreferences("recent_backup_uris", Context.MODE_PRIVATE)
                val savedUris = prefs.getStringSet("uris", emptySet()) ?: emptySet()
                val validUris = mutableSetOf<String>()
                for (uriStr in savedUris) {
                    try {
                        val uri = Uri.parse(uriStr)
                        if (!isUriReadable(cr, uri)) continue
                        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                            if (c.moveToFirst()) {
                                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                                val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                                val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                                if (size > 0L) {
                                    validUris.add(uriStr)
                                    if (name.lowercase(Locale.ROOT).endsWith(".${filterType.extension}")) {
                                        results[name.lowercase(Locale.ROOT)] = AvailableBackupFile(
                                            uri = uri,
                                            displayName = name,
                                            type = filterType,
                                            modifiedAtMillis = System.currentTimeMillis(),
                                            sizeBytes = size,
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (validUris.size != savedUris.size) {
                    prefs.edit().putStringSet("uris", validUris).apply()
                }

                // 2. Query MediaStore.Downloads (API 29+) for matching non-trashed, readable files
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val projection = arrayOf(
                            MediaStore.Downloads._ID,
                            MediaStore.Downloads.DISPLAY_NAME,
                            MediaStore.Downloads.DATE_MODIFIED,
                            MediaStore.Downloads.SIZE,
                        )
                        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? AND ${MediaStore.Downloads.SIZE} > 0 AND ${MediaStore.MediaColumns.IS_TRASHED} = 0 AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
                        } else {
                            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? AND ${MediaStore.Downloads.SIZE} > 0"
                        }
                        val selectionArgs = arrayOf("%. ${filterType.extension}".replace(" ", ""))
                        cr.query(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val name = cursor.getString(nameCol) ?: continue
                                val modifiedSec = cursor.getLong(dateCol)
                                val size = cursor.getLong(sizeCol)
                                val contentUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                                if (!isUriReadable(cr, contentUri)) continue
                                results[name.lowercase(Locale.ROOT)] = AvailableBackupFile(
                                    uri = contentUri,
                                    displayName = name,
                                    type = filterType,
                                    modifiedAtMillis = modifiedSec * 1000L,
                                    sizeBytes = size,
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                results.values.sortedByDescending { it.modifiedAtMillis }
            }
            _availableBackups.value = found
        }
    }

    fun validateAndNotifyWrongType(context: Context, uri: Uri, expectedType: BackupFileType): Boolean {
        val name = resolveDisplayName(context.contentResolver, uri, "")
        if (name.isNotBlank() && !name.lowercase(Locale.ROOT).endsWith(".${expectedType.extension}")) {
            viewModelScope.launch {
                _snackbarMessages.emit(String.format(Locale.US, strings.wrongFileTypeFmt, name, expectedType.label))
            }
            return false
        }
        recordRecentBackupUri(context, uri)
        return true
    }

    private val _barcodeHealthState = MutableStateFlow<BarcodeHealthState>(BarcodeHealthState.Idle)
    val barcodeHealthState: StateFlow<BarcodeHealthState> = _barcodeHealthState.asStateFlow()

    fun runBarcodeApiHealthCheck() {
        viewModelScope.launch {
            _barcodeHealthState.value = BarcodeHealthState.Checking
            val res = OpenFoodFactsService.healthCheck()
            _barcodeHealthState.value = BarcodeHealthState.Result(res)
            val healthyCount = res.endpoints.count { it.isHealthy }
            val totalCount = res.endpoints.size
            val msg = if (healthyCount == totalCount) {
                String.format(Locale.US, strings.vmAllApisHealthyFmt, totalCount)
            } else {
                String.format(Locale.US, strings.vmSomeApisReachableFmt, healthyCount, totalCount)
            }
            _snackbarMessages.emit(msg)
        }
    }

    fun clearBarcodeApiHealthCheck() {
        _barcodeHealthState.value = BarcodeHealthState.Idle
    }

    fun exportItems(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pieces = repository.getAllPiecesSync()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        CsvService.exportPieces(outputStream, pieces)
                    }
                    recordRecentBackupUri(context, uri)
                }
                val savedName = resolveDisplayName(contentResolver, uri, "shopping_items.csv")
                _snackbarMessages.emit(String.format(Locale.US, strings.vmExportedFileFmt, savedName))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedExportItemsFmt, e.message ?: ""))
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
                _snackbarMessages.emit(String.format(Locale.US, strings.vmImportedItemsFmt, count))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedImportItemsFmt, e.message ?: ""))
            }
        }
    }

    fun exportHistory(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        CsvService.exportTrips(outputStream, trips)
                    }
                    recordRecentBackupUri(context, uri)
                }
                val savedName = resolveDisplayName(contentResolver, uri, "shopping_history.csv")
                _snackbarMessages.emit(String.format(Locale.US, strings.vmExportedFileFmt, savedName))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedExportHistoryFmt, e.message ?: ""))
            }
        }
    }

    fun importHistory(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val trips = CsvService.importTrips(inputStream)
                        repository.importShoppingTrips(trips)
                        trips.size
                    } ?: 0
                }
                _snackbarMessages.emit(String.format(Locale.US, strings.vmImportedReceiptsFmt, count))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedImportHistoryFmt, e.message ?: ""))
            }
        }
    }

    fun exportFullBackup(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pieces = repository.getAllPiecesSync()
                    val products = repository.getAllProductsSync()
                    val crossRefs = repository.getAllProductPiecesSync()
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    val offers = repository.getAllOffersSync()
                    val history = repository.getAllOfferHistorySync()
                    val shoppingLists = repository.getAllShoppingListsSync()
                    val shoppingListItems = repository.getAllShoppingListItemsSync()

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BackupRestoreService.exportFullBackup(
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            offerHistory = history,
                            shoppingLists = shoppingLists,
                            shoppingListItems = shoppingListItems,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                            capitalizationMode = capitalizationMode.value,
                            appLanguage = appLanguage.value,
                        )
                    }
                    recordRecentBackupUri(context, uri)
                }
                val savedName = resolveDisplayName(contentResolver, uri, "barcode_tracker_backup.json")
                _snackbarMessages.emit(String.format(Locale.US, strings.vmSavedBackupFileFmt, savedName))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedExportBackupFmt, e.message ?: ""))
            }
        }
    }

    private fun applyRestoredPreferences(payload: com.inventory.barcodetracker.service.FullBackupPayload) {
        if (payload.currencySymbol.isNotBlank()) {
            currencyPreferences.setCurrencySymbol(payload.currencySymbol)
        }
        if (payload.budgetLimit > 0) {
            currencyPreferences.setBudgetLimit(payload.budgetLimit)
        }
        if (payload.themeMode in listOf("light", "dark", "system")) {
            currencyPreferences.setThemeMode(payload.themeMode)
        }
        if (payload.capitalizationMode in listOf("TITLE_CASE", "FIRST_LETTER", "NONE")) {
            currencyPreferences.setCapitalizationMode(payload.capitalizationMode)
        }
        if (payload.appLanguage in listOf("en", "ro")) {
            currencyPreferences.setAppLanguage(payload.appLanguage)
        }
    }

    fun restoreFullBackup(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val payload = BackupRestoreService.parseBackupStream(context, inputStream) ?: return@use null
                        applyRestoredPreferences(payload)

                        repository.importPieces(payload.pieces)
                        repository.importProducts(payload.products)
                        repository.importProductPieces(payload.productPieces)
                        repository.importOffers(payload.offers)
                        repository.importShoppingTrips(payload.shoppingTrips)
                        repository.importOfferHistory(payload.offerHistory)
                        repository.importShoppingLists(payload.shoppingLists)
                        repository.importShoppingListItems(payload.shoppingListItems)
                        String.format(Locale.US, strings.vmRestoredSummaryFmt, payload.pieces.size, payload.shoppingLists.size, payload.offers.size, payload.shoppingTrips.size)
                    }
                }
                _snackbarMessages.emit(summary ?: strings.vmCouldNotParseBackup)
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedRestoreBackupFmt, e.message ?: ""))
            }
        }
    }

    fun exportRawDatabase(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pieces = repository.getAllPiecesSync()
                    val products = repository.getAllProductsSync()
                    val crossRefs = repository.getAllProductPiecesSync()
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    val offers = repository.getAllOffersSync()
                    val history = repository.getAllOfferHistorySync()
                    val shoppingLists = repository.getAllShoppingListsSync()
                    val shoppingListItems = repository.getAllShoppingListItemsSync()

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BackupRestoreService.exportDatabaseFile(
                            context = context,
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            offerHistory = history,
                            shoppingLists = shoppingLists,
                            shoppingListItems = shoppingListItems,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                            capitalizationMode = capitalizationMode.value,
                            appLanguage = appLanguage.value,
                        )
                    }
                    recordRecentBackupUri(context, uri)
                }
                val savedName = resolveDisplayName(contentResolver, uri, "shopping_tracker.db")
                _snackbarMessages.emit(String.format(Locale.US, strings.vmSavedDbFileFmt, savedName))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedExportDbFmt, e.message ?: ""))
            }
        }
    }

    fun importRawDatabase(context: Context, contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val payload = BackupRestoreService.parseBackupStream(context, inputStream) ?: return@use null
                        applyRestoredPreferences(payload)
                        repository.importPieces(payload.pieces)
                        repository.importProducts(payload.products)
                        repository.importProductPieces(payload.productPieces)
                        repository.importOffers(payload.offers)
                        repository.importShoppingTrips(payload.shoppingTrips)
                        repository.importOfferHistory(payload.offerHistory)
                        repository.importShoppingLists(payload.shoppingLists)
                        repository.importShoppingListItems(payload.shoppingListItems)
                        String.format(Locale.US, strings.vmRestoredSummaryFmt, payload.pieces.size, payload.shoppingLists.size, payload.offers.size, payload.shoppingTrips.size)
                    }
                }
                _snackbarMessages.emit(summary ?: strings.vmCouldNotParseDb)
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedRestoreDbFmt, e.message ?: ""))
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
                    val history = repository.getAllOfferHistorySync()
                    val shoppingLists = repository.getAllShoppingListsSync()
                    val shoppingListItems = repository.getAllShoppingListItemsSync()

                    FileOutputStream(backupFile).use { outputStream ->
                        BackupRestoreService.exportFullBackup(
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            offerHistory = history,
                            shoppingLists = shoppingLists,
                            shoppingListItems = shoppingListItems,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                            capitalizationMode = capitalizationMode.value,
                            appLanguage = appLanguage.value,
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
                    putExtra(Intent.EXTRA_SUBJECT, strings.vmShareBackupSubject)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, strings.vmShareBackupChooser))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedShareBackupFmt, e.message ?: ""))
            }
        }
    }

    fun shareRawDatabase(context: Context) {
        viewModelScope.launch {
            try {
                val dbFile = withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "shared_backups")
                    cacheDir.mkdirs()
                    val targetFile = File(cacheDir, "shopping_tracker.db")

                    val pieces = repository.getAllPiecesSync()
                    val products = repository.getAllProductsSync()
                    val crossRefs = repository.getAllProductPiecesSync()
                    val trips = repository.allShoppingTrips.firstOrNull() ?: emptyList()
                    val offers = repository.getAllOffersSync()
                    val history = repository.getAllOfferHistorySync()
                    val shoppingLists = repository.getAllShoppingListsSync()
                    val shoppingListItems = repository.getAllShoppingListItemsSync()

                    FileOutputStream(targetFile).use { outputStream ->
                        BackupRestoreService.exportDatabaseFile(
                            context = context,
                            outputStream = outputStream,
                            pieces = pieces,
                            products = products,
                            productPieces = crossRefs,
                            shoppingTrips = trips,
                            offers = offers,
                            offerHistory = history,
                            shoppingLists = shoppingLists,
                            shoppingListItems = shoppingListItems,
                            currencySymbol = currencySymbol.value,
                            budgetLimit = budgetLimit.value,
                            themeMode = themeMode.value,
                            capitalizationMode = capitalizationMode.value,
                            appLanguage = appLanguage.value,
                        )
                    }
                    targetFile
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    dbFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, strings.vmShareDbSubject)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, strings.vmShareDbChooser))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedShareDbFmt, e.message ?: ""))
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
                    putExtra(Intent.EXTRA_SUBJECT, strings.vmShareApkSubject)
                    putExtra(Intent.EXTRA_TEXT, strings.vmShareApkText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, strings.vmShareApkChooser))
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedShareApkFmt, e.message ?: ""))
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
                repository.seedSampleData()
                _snackbarMessages.emit(strings.vmSampleDataLoaded)
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedSeedDataFmt, e.message ?: ""))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _snackbarMessages.emit(strings.vmAllDataCleared)
            } catch (e: Exception) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFailedClearDataFmt, e.message ?: ""))
            }
        }
    }
}
