package com.inventory.barcodetracker.ui.screens.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.TripBasketSnapshot
import com.inventory.barcodetracker.data.local.entity.TripSnapshotItem
import com.inventory.barcodetracker.data.local.entity.calculateBasketSummary
import com.inventory.barcodetracker.data.local.entity.calculateItemSavings
import com.inventory.barcodetracker.data.local.entity.calculateItemTotal
import com.inventory.barcodetracker.data.local.entity.formatOfferBadge
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.scanner.ScanResult
import com.inventory.barcodetracker.service.AppUpdateInfo
import com.inventory.barcodetracker.service.OpenFoodFactsService
import com.inventory.barcodetracker.service.UpdateCheckService
import com.inventory.barcodetracker.ui.i18n.AppStrings
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScannedItemState {
    object Idle : ScannedItemState()
    data class Loading(val barcode: String) : ScannedItemState()
    data class FoundPiece(val piece: PieceEntity) : ScannedItemState()
    data class FoundProduct(val product: ProductEntity) : ScannedItemState()
    data class NotFound(
        val barcode: String,
        val initialName: String = "",
        val initialBrand: String = "",
        val initialCategory: String = "Grocery",
        val apisUnavailable: Boolean = false,
    ) : ScannedItemState()
}

class QuickScanViewModel(
    private val repository: InventoryRepository,
    private val scannerService: BarcodeScannerService,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    private val strings: AppStrings
        get() = AppStrings.forLanguage(currencyPreferences.appLanguage.value)

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol
    val budgetLimit: StateFlow<Double> = currencyPreferences.budgetLimit
    val appLanguage: StateFlow<String> = currencyPreferences.appLanguage

    fun setAppLanguage(language: String) {
        currencyPreferences.setAppLanguage(language)
    }

    fun setCurrencySymbol(symbol: String) {
        currencyPreferences.setCurrencySymbol(symbol)
    }

    val savedOffers: StateFlow<List<OfferEntity>> = repository.allOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBudgetLimit(limit: Double) {
        currencyPreferences.setBudgetLimit(limit)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _scannedItemState = MutableStateFlow<ScannedItemState>(ScannedItemState.Idle)
    val scannedItemState: StateFlow<ScannedItemState> = _scannedItemState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _appliedCartVouchers = MutableStateFlow<List<OfferEntity>>(emptyList())
    val appliedCartVouchers: StateFlow<List<OfferEntity>> = _appliedCartVouchers.asStateFlow()

    val allShoppingLists: StateFlow<List<ShoppingListEntity>> = repository.allShoppingLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShoppingListItems: StateFlow<List<ShoppingListItemEntity>> = repository.allShoppingListItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPiecesMap: StateFlow<Map<String, PieceEntity>> = repository.allPieces
        .map { pieces -> pieces.associateBy { it.barcode } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _activeListId = MutableStateFlow<Long?>(null)
    val activeListId: StateFlow<Long?> = _activeListId.asStateFlow()

    private val _selectedListIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedListIds: StateFlow<Set<Long>> = _selectedListIds.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    data class PendingBasketItem(
        val piece: PieceEntity,
        val quantity: Int = 1,
        val fromScanner: Boolean = false,
    )

    private val _pendingBasketItem = MutableStateFlow<PendingBasketItem?>(null)
    val pendingBasketItem: StateFlow<PendingBasketItem?> = _pendingBasketItem.asStateFlow()

    private val _pendingBasketBarcodes = MutableStateFlow<List<String>>(emptyList())
    val pendingBasketBarcodes: StateFlow<List<String>> = _pendingBasketBarcodes.asStateFlow()

    private val _scanAnotherPromptItem = MutableStateFlow<String?>(null)
    val scanAnotherPromptItem: StateFlow<String?> = _scanAnotherPromptItem.asStateFlow()

    private var isCurrentFlowFromScanner = false

    fun clearPendingBasketItem() {
        _pendingBasketItem.value = null
        _pendingBasketBarcodes.value = emptyList()
    }

    fun dismissScanAnotherPrompt() {
        _scanAnotherPromptItem.value = null
    }

    init {
        viewModelScope.launch {
            val lists = repository.getAllShoppingListsSync()
            if (lists.isNotEmpty()) {
                _activeListId.value = lists.first().id
                _selectedListIds.value = setOf(lists.first().id)
            }

            val update = UpdateCheckService.checkForUpdates()
            if (update.hasUpdate) {
                _appUpdateInfo.value = update
            }
        }

        viewModelScope.launch {
            repository.allShoppingLists.collect { lists ->
                val validIds = lists.map { it.id }.toSet()
                val pruned = _selectedListIds.value.intersect(validIds)
                if (pruned.size != _selectedListIds.value.size) {
                    _selectedListIds.value = pruned
                }
                val activeId = _activeListId.value
                if (activeId != null && !validIds.contains(activeId)) {
                    _activeListId.value = lists.firstOrNull()?.id
                }
            }
        }
    }

    fun dismissUpdateBanner() {
        _appUpdateInfo.value = null
    }

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            val defaultBudget = budgetLimit.value
            val formattedName = currencyPreferences.formatText(name)
            val id = repository.upsertShoppingList(ShoppingListEntity(name = formattedName, budgetLimit = defaultBudget))
            _activeListId.value = id
            _selectedListIds.value = _selectedListIds.value + id

            val pending = _pendingBasketItem.value
            val pendingBatch = _pendingBasketBarcodes.value
            if (pending != null) {
                _pendingBasketItem.value = null
                addPieceToList(id, pending.piece, pending.quantity)
                _snackbarMessages.emit(String.format(Locale.US, strings.vmCreatedBasketAddedFmt, formattedName, pending.piece.name))
                if (pending.fromScanner) {
                    _scanAnotherPromptItem.value = pending.piece.name
                }
            } else if (pendingBatch.isNotEmpty()) {
                _pendingBasketBarcodes.value = emptyList()
                var addedCount = 0
                for (barcode in pendingBatch) {
                    val piece = repository.getPieceByBarcode(barcode) ?: continue
                    repository.adjustPieceStock(barcode, 1)
                    addPieceToList(id, piece, 1)
                    addedCount++
                }
                if (addedCount > 0) {
                    _snackbarMessages.emit(String.format(Locale.US, strings.vmAddedItemsToBasketsFmt, addedCount))
                } else {
                    _snackbarMessages.emit(String.format(Locale.US, strings.vmCreatedBasketFmt, formattedName))
                }
            } else {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmCreatedBasketFmt, formattedName))
            }
        }
    }

    private suspend fun addPieceToList(listId: Long, piece: PieceEntity, deltaQty: Int) {
        val existing = repository.getItemsForListSync(listId).firstOrNull { it.barcode == piece.barcode }
        val newQty = (existing?.quantity ?: 0) + deltaQty
        if (newQty > 0) {
            val prevChecked = existing?.effectiveCheckedQty ?: 0
            val newChecked = prevChecked.coerceIn(0, newQty)
            repository.upsertShoppingListItem(
                ShoppingListItemEntity(
                    listId = listId,
                    barcode = piece.barcode,
                    quantity = newQty,
                    unitPrice = piece.unitPrice,
                    offerType = piece.offerType,
                    offerBuyQty = piece.offerBuyQty,
                    offerPayQty = piece.offerPayQty,
                    offerFixedPrice = piece.offerFixedPrice,
                    offerPercentOff = piece.offerPercentOff,
                    isChecked = newChecked >= newQty,
                    offerGroupId = existing?.offerGroupId ?: "",
                    checkedQuantity = newChecked
                )
            )
        } else {
            repository.removeShoppingListItem(listId, piece.barcode)
        }
    }

    fun copyShoppingList(listId: Long, newName: String) {
        viewModelScope.launch {
            val formattedName = currencyPreferences.formatText(newName)
            val newId = repository.copyShoppingList(listId, formattedName)
            _activeListId.value = newId
            _selectedListIds.value = _selectedListIds.value + newId
            _snackbarMessages.emit(String.format(Locale.US, strings.vmDuplicatedBasketFmt, formattedName))
        }
    }

    fun renameShoppingList(listId: Long, newName: String) {
        viewModelScope.launch {
            val existing = repository.getAllShoppingListsSync().firstOrNull { it.id == listId }
            if (existing != null) {
                val formattedName = currencyPreferences.formatText(newName)
                repository.upsertShoppingList(existing.copy(name = formattedName))
                _snackbarMessages.emit(String.format(Locale.US, strings.vmRenamedBasketFmt, formattedName))
            }
        }
    }

    fun setBasketBudgetLimit(listId: Long, limit: Double) {
        viewModelScope.launch {
            val existing = repository.getAllShoppingListsSync().firstOrNull { it.id == listId }
            if (existing != null) {
                repository.upsertShoppingList(existing.copy(budgetLimit = limit))
                val msg = if (limit > 0) String.format(Locale.US, strings.vmBudgetSetFmt, currencySymbol.value, limit, existing.name) else String.format(Locale.US, strings.vmBudgetClearedFmt, existing.name)
                _snackbarMessages.emit(msg)
            }
        }
    }

    fun setBudgetLimitForSelectedLists(limit: Double) {
        viewModelScope.launch {
            val selected = _selectedListIds.value.toList()
            if (selected.isNotEmpty()) {
                val lists = repository.getAllShoppingListsSync().filter { selected.contains(it.id) }
                for (list in lists) {
                    repository.upsertShoppingList(list.copy(budgetLimit = limit))
                }
                val symbol = currencySymbol.value
                val msg = if (limit > 0) String.format(Locale.US, strings.vmBudgetSetMultiFmt, symbol, limit, lists.size) else String.format(Locale.US, strings.vmBudgetClearedMultiFmt, lists.size)
                _snackbarMessages.emit(msg)
            }
        }
    }

    fun applyVoucherToShoppingList(listId: Long, voucher: OfferEntity) {
        viewModelScope.launch {
            val titleClean = (if (voucher.title.isNotBlank()) voucher.title else "${currencySymbol.value}${String.format(Locale.US, "%.2f", voucher.fixedPrice)} Off")
                .replace(Regex("""(\$|£|€|RON)\s+"""), "$1")
                .trim()
            val voucherBarcode = "VOUCHER_${titleClean.replace(" ", "_")}"
            val discountAmount = -kotlin.math.abs(voucher.fixedPrice)
            repository.upsertShoppingListItem(
                ShoppingListItemEntity(
                    listId = listId,
                    barcode = voucherBarcode,
                    quantity = 1,
                    unitPrice = discountAmount,
                    offerType = "VOUCHER",
                    offerFixedPrice = voucher.fixedPrice,
                    addedAt = System.currentTimeMillis()
                )
            )
            val symbol = currencySymbol.value
            _snackbarMessages.emit(String.format(Locale.US, strings.vmAppliedVoucherBasketFmt, titleClean, symbol, voucher.fixedPrice))
        }
    }

    fun applyMixAndMatchOffer(
        listId: Long,
        barcodes: List<String>,
        offerType: String,
        buyQty: Int,
        payQty: Int,
        fixedPrice: Double,
        percentOff: Double,
    ) {
        if (barcodes.size < 2 || offerType == "NONE") return
        viewModelScope.launch {
            val groupId = "MM_${System.currentTimeMillis()}"
            val items = repository.getItemsForListSync(listId)
            val targetSet = barcodes.toSet()
            val updated = items.filter { targetSet.contains(it.barcode) }.map { item ->
                item.copy(
                    offerGroupId = groupId,
                    offerType = offerType,
                    offerBuyQty = buyQty,
                    offerPayQty = payQty,
                    offerFixedPrice = fixedPrice,
                    offerPercentOff = percentOff,
                )
            }
            for (u in updated) {
                repository.upsertShoppingListItem(u)
            }
            repository.ensureOfferSavedInLibrary(
                offerType = offerType,
                buyQty = buyQty,
                payQty = payQty,
                fixedPrice = fixedPrice,
                percentOff = percentOff,
                currencySymbol = currencySymbol.value,
            )
            val badge = formatOfferBadge(offerType, buyQty, payQty, fixedPrice, percentOff, currencySymbol.value)
            _snackbarMessages.emit(String.format(Locale.US, strings.vmAppliedMixAndMatchFmt, badge, updated.size))
        }
    }

    fun clearMixAndMatchGroup(listId: Long, groupId: String) {
        viewModelScope.launch {
            val items = repository.getItemsForListSync(listId).filter { it.offerGroupId == groupId }
            for (item in items) {
                repository.upsertShoppingListItem(
                    item.copy(
                        offerGroupId = "",
                        offerType = "NO_OFFER",
                        offerBuyQty = 0,
                        offerPayQty = 0,
                        offerFixedPrice = 0.0,
                        offerPercentOff = 0.0,
                    )
                )
            }
            _snackbarMessages.emit(strings.vmUngroupedDeal)
        }
    }

    fun toggleBasketItemChecked(listId: Long, barcode: String, isChecked: Boolean) {
        viewModelScope.launch {
            repository.setShoppingListItemChecked(listId, barcode, isChecked)
        }
    }

    fun adjustBasketItemQuantity(listId: Long, barcode: String, delta: Int, forCheckedSection: Boolean = false) {
        viewModelScope.launch {
            val existing = repository.getItemsForListSync(listId).firstOrNull { it.barcode == barcode } ?: return@launch
            val newQty = existing.quantity + delta
            if (newQty <= 0) {
                repository.removeShoppingListItem(listId, barcode)
            } else if (forCheckedSection) {
                val newChecked = (existing.effectiveCheckedQty + delta).coerceIn(0, newQty)
                repository.upsertShoppingListItem(
                    existing.copy(
                        quantity = newQty,
                        checkedQuantity = newChecked,
                        isChecked = newChecked >= newQty
                    )
                )
            } else {
                repository.updateShoppingListItemQuantity(listId, barcode, newQty)
            }
        }
    }

    fun removeShoppingListItem(listId: Long, barcode: String) {
        viewModelScope.launch {
            repository.removeShoppingListItem(listId, barcode)
            _snackbarMessages.emit(strings.vmRemovedItemOrVoucher)
        }
    }

    fun clearOfferFromBasketItem(listId: Long, barcode: String) {
        viewModelScope.launch {
            val piece = repository.getPieceByBarcode(barcode)
            if (piece != null) {
                val clearedPiece = piece.copy(
                    offerType = "NONE",
                    offerBuyQty = 0,
                    offerPayQty = 0,
                    offerFixedPrice = 0.0,
                    offerPercentOff = 0.0,
                )
                repository.upsertPiece(clearedPiece)
                repository.syncPieceToActiveBaskets(clearedPiece)
            } else {
                val existing = repository.getItemsForListSync(listId).firstOrNull { it.barcode == barcode } ?: return@launch
                repository.upsertShoppingListItem(
                    existing.copy(
                        offerGroupId = "",
                        offerType = "NO_OFFER",
                        offerBuyQty = 0,
                        offerPayQty = 0,
                        offerFixedPrice = 0.0,
                        offerPercentOff = 0.0,
                    )
                )
            }
            _snackbarMessages.emit(strings.vmUngroupedDeal)
        }
    }

    fun updatePieceFromBasket(listId: Long, piece: PieceEntity, updateAllBasketsPrice: Boolean) {
        viewModelScope.launch {
            val formattedPiece = piece.copy(
                name = currencyPreferences.formatText(piece.name),
                location = currencyPreferences.formatText(piece.location),
            )
            repository.upsertPiece(formattedPiece)
            repository.syncPieceToActiveBaskets(formattedPiece)
            repository.ensureOfferSavedInLibrary(formattedPiece, currencySymbol.value)
            _snackbarMessages.emit(String.format(Locale.US, strings.vmSavedPieceUpdatedBasketsFmt, formattedPiece.name))
        }
    }

    fun deletePiece(piece: PieceEntity) {
        viewModelScope.launch {
            try {
                repository.deletePiece(piece)
                _snackbarMessages.emit(String.format(Locale.US, strings.vmDeletedPieceFmt, piece.name))
            } catch (_: Exception) {
                _snackbarMessages.emit(strings.vmCannotDeleteLinkedItem)
            }
        }
    }

    fun deleteShoppingList(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.deleteShoppingList(list)
            if (_activeListId.value == list.id) _activeListId.value = null
            _selectedListIds.value = _selectedListIds.value - list.id
            _snackbarMessages.emit(String.format(Locale.US, strings.vmDeletedBasketFmt, list.name))
        }
    }

    fun deleteSelectedShoppingLists() {
        viewModelScope.launch {
            val selected = _selectedListIds.value.toList()
            if (selected.isNotEmpty()) {
                repository.deleteShoppingListsByIds(selected)
                _selectedListIds.value = emptySet()
                _snackbarMessages.emit(String.format(Locale.US, strings.vmDeletedBasketsFmt, selected.size))
            }
        }
    }

    fun toggleSelectList(listId: Long) {
        if (_selectedListIds.value.contains(listId)) {
            _selectedListIds.value = _selectedListIds.value - listId
        } else {
            _selectedListIds.value = _selectedListIds.value + listId
        }
    }

    fun applyCartVoucher(voucher: OfferEntity) {
        if (_appliedCartVouchers.value.none { it.id == voucher.id && voucher.id != 0L }) {
            _appliedCartVouchers.value = _appliedCartVouchers.value + voucher
            viewModelScope.launch {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmAppliedVoucherFmt, voucher.title, currencySymbol.value, voucher.fixedPrice))
            }
        }
    }

    fun removeCartVoucher(voucher: OfferEntity) {
        _appliedCartVouchers.value = _appliedCartVouchers.value.filter { it != voucher }
        viewModelScope.launch {
            _snackbarMessages.emit(String.format(Locale.US, strings.vmRemovedVoucherFmt, voucher.title))
        }
    }

    val basketSummary: StateFlow<Pair<Double, Int>> = combine(
        repository.allPieces,
        repository.allProducts,
        _appliedCartVouchers
    ) { pieces, products, vouchers ->
        val pieceValue = pieces.sumOf { it.calculatedTotal }
        val pieceCount = pieces.sumOf { it.stockQuantity }
        val productValue = products.sumOf { it.stockQuantity * it.unitPrice }
        val productCount = products.sumOf { it.stockQuantity }

        val itemsSubtotal = pieceValue + productValue
        val vouchersDiscount = vouchers.sumOf { it.fixedPrice }
        val netBasketTotal = maxOf(0.0, itemsSubtotal - vouchersDiscount)

        Pair(netBasketTotal, pieceCount + productCount)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Pair(0.0, 0),
    )

    val totalSavings: StateFlow<Double> = repository.allPieces.map { pieces ->
        pieces.sumOf { it.savings }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.0,
    )

    val searchResults = combine(
        _searchQuery,
        repository.allPieces,
        repository.allProducts,
    ) { query, pieces, products ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            Pair(emptyList(), emptyList())
        } else {
            val matchingPieces = pieces.filter {
                it.barcode.contains(trimmed, ignoreCase = true) ||
                        it.name.contains(trimmed, ignoreCase = true) ||
                        it.location.contains(trimmed, ignoreCase = true) ||
                        it.description.contains(trimmed, ignoreCase = true) ||
                        it.offerDescription.contains(trimmed, ignoreCase = true) ||
                        it.offerType.contains(trimmed, ignoreCase = true)
            }
            val matchingProducts = products.filter {
                it.productSku.contains(trimmed, ignoreCase = true) ||
                        it.name.contains(trimmed, ignoreCase = true) ||
                        it.description.contains(trimmed, ignoreCase = true)
            }
            Pair(matchingPieces, matchingProducts)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Pair(emptyList(), emptyList()),
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun startScanner(onNeedManualInputFallback: () -> Unit) {
        viewModelScope.launch {
            when (val result = scannerService.startScan()) {
                is ScanResult.Success -> {
                    resolveBarcode(result.barcode, fromScanner = true)
                }
                is ScanResult.Cancelled -> {
                    // User closed scanner without scanning
                }
                is ScanResult.Failure -> {
                    _snackbarMessages.emit(result.message)
                    if (result.canFallbackToManual) {
                        onNeedManualInputFallback()
                    }
                }
            }
        }
    }

    fun resolveBarcode(barcode: String, fromScanner: Boolean = false) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        isCurrentFlowFromScanner = fromScanner
        viewModelScope.launch {
            val symbol = currencySymbol.value
            // 1. First check local SQLite database
            val piece = repository.getPieceByBarcode(trimmed)
            if (piece != null) {
                val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
                if (targetLists.isEmpty()) {
                    _pendingBasketItem.value = PendingBasketItem(piece, 1, fromScanner = fromScanner)
                    return@launch
                }

                repository.adjustPieceStock(piece.barcode, 1)
                val updated = repository.getPieceByBarcode(piece.barcode) ?: piece

                for (listId in targetLists) {
                    addPieceToList(listId, piece, 1)
                }

                val priceInfo = if (updated.unitPrice > 0) String.format(Locale.US, " (%s%.2f)", symbol, updated.unitPrice) else ""
                val basketCount = targetLists.size
                val msg = if (basketCount > 1) {
                    String.format(Locale.US, strings.vmAddedToBasketsMultiFmt, updated.name, priceInfo, basketCount)
                } else {
                    String.format(Locale.US, strings.vmAddedToBasketSingleFmt, updated.name, priceInfo)
                }
                _snackbarMessages.emit(msg)
                if (fromScanner) {
                    _scanAnotherPromptItem.value = updated.name
                }
                return@launch
            }
            // 2. Check product table
            val product = repository.getProductBySku(trimmed)
            if (product != null) {
                repository.adjustProductStock(product.productSku, 1)
                val updated = repository.getProductBySku(product.productSku) ?: product
                val priceInfo = if (updated.unitPrice > 0) String.format(Locale.US, " (%s%.2f)", symbol, updated.unitPrice) else ""
                _snackbarMessages.emit(String.format(Locale.US, strings.vmAddedToBasketCartFmt, updated.name, priceInfo, updated.stockQuantity))
                return@launch
            }

            // 3. Not in local database -> Show loading & query Open*Facts APIs in parallel
            _scannedItemState.value = ScannedItemState.Loading(trimmed)
            val outcome = OpenFoodFactsService.lookupBarcodeWithStatus(trimmed)
            // Only update if user hasn't dismissed or switched to manual entry
            if (_scannedItemState.value !is ScannedItemState.Loading) return@launch
            val onlineProduct = outcome.product
            if (onlineProduct != null) {
                val formattedName = currencyPreferences.formatText(onlineProduct.name)
                val formattedBrand = currencyPreferences.formatText(onlineProduct.brand)
                val formattedCategory = currencyPreferences.formatText(onlineProduct.category)
                _snackbarMessages.emit(String.format(Locale.US, strings.vmFoundOnlineFmt, formattedName))
                _scannedItemState.value = ScannedItemState.NotFound(
                    barcode = trimmed,
                    initialName = formattedName,
                    initialBrand = formattedBrand,
                    initialCategory = formattedCategory,
                    apisUnavailable = false,
                )
            } else {
                _scannedItemState.value = ScannedItemState.NotFound(
                    barcode = trimmed,
                    apisUnavailable = outcome.apisUnavailable,
                )
            }
        }
    }

    fun dismissScannedSheet() {
        _scannedItemState.value = ScannedItemState.Idle
    }

    fun skipLookupToManualEntry() {
        val current = _scannedItemState.value
        if (current is ScannedItemState.Loading) {
            _scannedItemState.value = ScannedItemState.NotFound(
                barcode = current.barcode,
                apisUnavailable = false,
            )
        }
    }

    fun adjustPieceStock(barcode: String, delta: Int) {
        viewModelScope.launch {
            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            val piece = repository.getPieceByBarcode(barcode) ?: return@launch
            if (targetLists.isEmpty() && delta > 0) {
                _pendingBasketItem.value = PendingBasketItem(piece, delta)
                return@launch
            }

            for (listId in targetLists) {
                addPieceToList(listId, piece, delta)
            }
            val basketQty = targetLists.sumOf { listId ->
                repository.getItemsForListSync(listId).firstOrNull { it.barcode == barcode }?.quantity ?: 0
            }
            _scannedItemState.value = ScannedItemState.FoundPiece(piece.copy(stockQuantity = basketQty))
            _snackbarMessages.emit(String.format(Locale.US, strings.vmCartQtyUpdatedFmt, delta.toString()))
        }
    }

    fun addPiecesToSelectedBaskets(barcodes: List<String>) {
        if (barcodes.isEmpty()) return
        viewModelScope.launch {
            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            if (targetLists.isEmpty()) {
                _pendingBasketBarcodes.value = barcodes
                return@launch
            }
            var addedCount = 0
            for (barcode in barcodes) {
                val piece = repository.getPieceByBarcode(barcode) ?: continue
                for (listId in targetLists) {
                    addPieceToList(listId, piece, 1)
                }
                addedCount++
            }
            if (addedCount > 0) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmAddedItemsToBasketsFmt, addedCount))
            }
        }
    }

    fun addPiecesToBaskets(barcodes: List<String>, targetListIds: List<Long>, quantityPerItem: Int = 1) {
        if (barcodes.isEmpty() || targetListIds.isEmpty()) return
        val safeQty = maxOf(1, quantityPerItem)
        viewModelScope.launch {
            var addedCount = 0
            for (barcode in barcodes) {
                val piece = repository.getPieceByBarcode(barcode) ?: continue
                for (listId in targetListIds) {
                    addPieceToList(listId, piece, safeQty)
                }
                addedCount++
            }
            if (addedCount > 0) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmAddedItemsToBasketsFmt, addedCount))
            }
        }
    }

    fun adjustProductStock(sku: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustProductStock(sku, delta)
            val updated = repository.getProductBySku(sku)
            updated?.let {
                _scannedItemState.value = ScannedItemState.FoundProduct(it)
            }
            _snackbarMessages.emit(String.format(Locale.US, strings.vmProductStockUpdatedFmt, delta.toString()))
        }
    }

    fun createPiece(piece: PieceEntity) {
        viewModelScope.launch {
            val fromScanner = isCurrentFlowFromScanner
            isCurrentFlowFromScanner = false
            val formattedPiece = piece.copy(
                name = currencyPreferences.formatText(piece.name),
                location = currencyPreferences.formatText(piece.location),
            )
            repository.upsertPiece(formattedPiece)
            repository.ensureOfferSavedInLibrary(formattedPiece, currencySymbol.value)
            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            if (targetLists.isEmpty()) {
                _scannedItemState.value = ScannedItemState.Idle
                _pendingBasketItem.value = PendingBasketItem(formattedPiece, maxOf(1, formattedPiece.stockQuantity), fromScanner = fromScanner)
                return@launch
            }

            _scannedItemState.value = ScannedItemState.Idle
            for (listId in targetLists) {
                addPieceToList(listId, formattedPiece, maxOf(1, formattedPiece.stockQuantity))
            }
            _snackbarMessages.emit(String.format(Locale.US, strings.vmAddedPieceToBasketFmt, formattedPiece.name))
            if (fromScanner) {
                _scanAnotherPromptItem.value = formattedPiece.name
            }
        }
    }

    fun applySavedOfferToPiece(barcode: String, offer: OfferEntity) {
        viewModelScope.launch {
            repository.applyOfferToPiece(barcode, offer)
            val updated = repository.getPieceByBarcode(barcode)
            updated?.let {
                val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { id -> setOf(id) } ?: emptySet() }
                val basketQty = targetLists.sumOf { listId ->
                    repository.getItemsForListSync(listId).firstOrNull { item -> item.barcode == barcode }?.quantity ?: 0
                }
                _scannedItemState.value = ScannedItemState.FoundPiece(it.copy(stockQuantity = basketQty))
            }
            _snackbarMessages.emit(String.format(Locale.US, strings.vmAppliedDealFmt, offer.displayBadge))
        }
    }

    fun getOfferHistoryForPiece(barcode: String): Flow<List<OfferEntity>> {
        return repository.getOfferHistoryForPiece(barcode)
    }

    fun finishAndSaveBasketForList(list: ShoppingListEntity) {
        viewModelScope.launch {
            val listItems = repository.getItemsForListSync(list.id)
            if (listItems.isEmpty()) {
                _snackbarMessages.emit(String.format(Locale.US, strings.vmListEmptyFmt, list.name))
                return@launch
            }

            val symbol = currencySymbol.value
            val pieceMap = repository.getAllPiecesSync().associateBy { it.barcode }
            val basketSummary = calculateBasketSummary(listItems, pieceMap, symbol)

            val itemLines = mutableListOf<String>()
            // 1. Mix & Match groups
            for (group in basketSummary.groupPricings.values) {
                val savingsPart = if (group.savings > 0) " (-$symbol${String.format(Locale.US, "%.2f", group.savings)})" else ""
                itemLines.add("🏷️ Mix & Match: ${group.offerDescription}$savingsPart")
                for (item in group.items) {
                    val piece = pieceMap[item.barcode]
                    val displayName = piece?.name ?: item.barcode
                    val unitP = if (item.unitPrice > 0) item.unitPrice else (piece?.unitPrice ?: 0.0)
                    itemLines.add("• $displayName (x${item.quantity}) — $symbol${String.format(Locale.US, "%.2f", unitP * item.quantity)}")
                }
                itemLines.add("")
            }

            // 2. Ungrouped regular items & vouchers
            for (item in listItems.filter { it.offerGroupId.isBlank() }) {
                if (item.offerType == "VOUCHER" || item.unitPrice < 0) {
                    val voucherName = item.barcode
                        .removePrefix("VOUCHER_")
                        .replace(Regex("""^\d+_"""), "")
                        .replace("_", " ")
                        .replace(Regex("""(\$|£|€|RON)\s+"""), "$1")
                        .trim()
                    val discountVal = kotlin.math.abs(item.unitPrice)
                    itemLines.add("• 🎟️ Voucher ($voucherName) — -$symbol${String.format(Locale.US, "%.2f", discountVal)}")
                } else {
                    val piece = pieceMap[item.barcode]
                    val effOfferType = when (item.offerType) {
                        "NO_OFFER" -> "NONE"
                        "NONE" -> piece?.offerType ?: "NONE"
                        else -> item.offerType
                    }
                    val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else (piece?.offerBuyQty ?: 0)
                    val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else (piece?.offerPayQty ?: 0)
                    val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else (piece?.offerFixedPrice ?: 0.0)
                    val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else (piece?.offerPercentOff ?: 0.0)

                    val mergedPiece = piece?.copy(
                        stockQuantity = item.quantity,
                        unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
                        offerType = effOfferType,
                        offerBuyQty = effBuyQty,
                        offerPayQty = effPayQty,
                        offerFixedPrice = effFixedPrice,
                        offerPercentOff = effPercentOff
                    )

                    val displayName = piece?.name ?: item.barcode
                    val lineTotal = mergedPiece?.calculatedTotal ?: item.calculateItemTotal(null)
                    val lineSavings = mergedPiece?.savings ?: item.calculateItemSavings(null)
                    val offerDesc = mergedPiece?.getOfferDescription(symbol) ?: ""

                    val savingTag = if (offerDesc.isNotBlank()) " [$offerDesc" + (if (lineSavings > 0) " — -$symbol${String.format(Locale.US, "%.2f", lineSavings)}" else "") + "]" else ""
                    itemLines.add("• $displayName (x${item.quantity})$savingTag — $symbol${String.format(Locale.US, "%.2f", lineTotal)}")
                }
            }

            val summaryText = "Basket: ${list.name}\n" + itemLines.joinToString("\n")
            val netTotalCost = basketSummary.regularItemsSubtotal

            val itemCount = listItems.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }.sumOf { it.quantity }

            val snapshotItems = listItems.map { item ->
                val isVoucher = item.offerType == "VOUCHER" || item.unitPrice < 0
                val piece = if (isVoucher) null else pieceMap[item.barcode]
                val effOfferType = when {
                    isVoucher -> "VOUCHER"
                    item.offerType == "NO_OFFER" -> "NO_OFFER"
                    item.offerType == "NONE" -> piece?.offerType ?: "NONE"
                    else -> item.offerType
                }
                val effBuyQty = if (isVoucher) 0 else if (item.offerBuyQty > 0) item.offerBuyQty else (piece?.offerBuyQty ?: 0)
                val effPayQty = if (isVoucher) 0 else if (item.offerPayQty > 0) item.offerPayQty else (piece?.offerPayQty ?: 0)
                val effFixedPrice = if (isVoucher) item.offerFixedPrice else if (item.offerFixedPrice > 0.0) item.offerFixedPrice else (piece?.offerFixedPrice ?: 0.0)
                val effPercentOff = if (isVoucher) 0.0 else if (item.offerPercentOff > 0.0) item.offerPercentOff else (piece?.offerPercentOff ?: 0.0)
                val effUnitPrice = if (isVoucher) item.unitPrice else if (item.unitPrice > 0) item.unitPrice else (piece?.unitPrice ?: 0.0)
                TripSnapshotItem(
                    barcode = item.barcode,
                    name = piece?.name ?: item.barcode,
                    quantity = item.quantity,
                    checkedQuantity = item.effectiveCheckedQty,
                    isChecked = item.isChecked,
                    unitPrice = effUnitPrice,
                    offerType = effOfferType,
                    offerBuyQty = effBuyQty,
                    offerPayQty = effPayQty,
                    offerFixedPrice = effFixedPrice,
                    offerPercentOff = effPercentOff,
                    offerGroupId = item.offerGroupId,
                )
            }
            val snapshotJson = TripBasketSnapshot(
                basketName = list.name,
                budgetLimit = list.budgetLimit,
                subtotalBeforeVouchers = basketSummary.subtotalBeforeVouchers,
                voucherDiscountAmount = basketSummary.voucherDiscountAmount,
                totalSavings = basketSummary.totalSavings,
                items = snapshotItems,
            ).toJsonString()

            repository.checkoutCartAndSaveTrip(
                totalCost = netTotalCost,
                itemCount = itemCount,
                itemsSummary = summaryText,
                currencySymbol = symbol,
                snapshotJson = snapshotJson,
            )
            resetClearedBasketPieceQuantities(list.id)
            repository.clearShoppingListItems(list.id)
            repository.deleteShoppingList(list)
            if (_activeListId.value == list.id) _activeListId.value = null
            _selectedListIds.value = _selectedListIds.value - list.id
            _snackbarMessages.emit(String.format(Locale.US, strings.vmSavedBasketToHistoryFmt, list.name, symbol, netTotalCost))
        }
    }

    private suspend fun resetClearedBasketPieceQuantities(listId: Long) {
        val listItems = repository.getItemsForListSync(listId)
        for (item in listItems) {
            if (item.offerType == "VOUCHER" || item.unitPrice < 0) continue
            val piece = repository.getPieceByBarcode(item.barcode) ?: continue
            if (piece.stockQuantity > 0) {
                repository.upsertPiece(piece.copy(stockQuantity = 0))
            }
        }
    }

    fun clearListBasket(list: ShoppingListEntity) {
        viewModelScope.launch {
            resetClearedBasketPieceQuantities(list.id)
            repository.clearShoppingListItems(list.id)
            _snackbarMessages.emit(String.format(Locale.US, strings.vmClearedBasketFmt, list.name))
        }
    }

    fun finishAndSaveSelectedBaskets() {
        viewModelScope.launch {
            val selectedIds = _selectedListIds.value.toList()
            if (selectedIds.isEmpty()) return@launch

            val allLists = repository.getAllShoppingListsSync().filter { selectedIds.contains(it.id) }
            var count = 0
            for (list in allLists) {
                val listItems = repository.getItemsForListSync(list.id)
                if (listItems.isNotEmpty()) {
                    finishAndSaveBasketForList(list)
                    count++
                }
            }
            if (count == 0) {
                _snackbarMessages.emit(strings.vmSelectedBasketsEmpty)
            }
        }
    }

    fun clearSelectedBaskets() {
        viewModelScope.launch {
            val selectedIds = _selectedListIds.value.toList()
            if (selectedIds.isEmpty()) return@launch

            for (id in selectedIds) {
                resetClearedBasketPieceQuantities(id)
                repository.clearShoppingListItems(id)
            }
            _snackbarMessages.emit(String.format(Locale.US, strings.vmClearedSelectedBasketsFmt, selectedIds.size))
        }
    }

    fun createProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.upsertProduct(product)
            _scannedItemState.value = ScannedItemState.FoundProduct(product)
            _snackbarMessages.emit(String.format(Locale.US, strings.vmCreatedProductFmt, product.name))
        }
    }

    fun finishAndSaveBasket(totalCost: Double, itemCount: Int) {
        viewModelScope.launch {
            val currentPieces = repository.getAllPiecesSync().filter { it.stockQuantity > 0 }
            val currentProducts = repository.getAllProductsSync().filter { it.stockQuantity > 0 }

            val symbol = currencySymbol.value
            val pieceLines = currentPieces.map {
                val total = it.calculatedTotal
                val offerTag = if (it.offerType != "NONE" && it.offerDescription.isNotBlank()) {
                    " [${it.offerDescription}" + (if (it.savings > 0) " — saved $symbol${String.format(Locale.US, "%.2f", it.savings)}" else "") + "]"
                } else ""
                if (it.unitPrice > 0) {
                    "• ${it.name} (x${it.stockQuantity})$offerTag — $symbol${String.format(Locale.US, "%.2f", total)}"
                } else {
                    "• ${it.name} (x${it.stockQuantity})$offerTag"
                }
            }

            val productLines = currentProducts.map {
                val total = it.stockQuantity * it.unitPrice
                if (it.unitPrice > 0) {
                    "• ${it.name} (x${it.stockQuantity}) — $symbol${String.format(Locale.US, "%.2f", total)}"
                } else {
                    "• ${it.name} (x${it.stockQuantity})"
                }
            }

            val voucherLines = _appliedCartVouchers.value.map {
                "🎟️ Voucher Applied: ${it.title} (-$symbol${String.format(Locale.US, "%.2f", it.fixedPrice)})"
            }

            val summaryText = (pieceLines + productLines + voucherLines).joinToString("\n")
            val itemsCost = currentPieces.sumOf { it.calculatedTotal } + currentProducts.sumOf { it.stockQuantity * it.unitPrice }
            val vouchersDiscount = _appliedCartVouchers.value.sumOf { it.fixedPrice }
            val finalBasketCost = maxOf(0.0, itemsCost - vouchersDiscount)

            repository.checkoutCartAndSaveTrip(
                totalCost = finalBasketCost,
                itemCount = itemCount,
                itemsSummary = summaryText.ifEmpty { strings.vmPurchasedItemsFallback },
                currencySymbol = symbol,
            )
            _appliedCartVouchers.value = emptyList()
            _snackbarMessages.emit(strings.vmSavedToHistoryKeepItems)
        }
    }

    fun clearBasketOnly() {
        viewModelScope.launch {
            repository.resetCartQuantities()
            _appliedCartVouchers.value = emptyList()
            _snackbarMessages.emit(strings.vmBasketClearedKeepItems)
        }
    }
}
