package com.inventory.barcodetracker.ui.screens.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.calculateItemSavings
import com.inventory.barcodetracker.data.local.entity.calculateItemTotal
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.scanner.ScanResult
import com.inventory.barcodetracker.service.AppUpdateInfo
import com.inventory.barcodetracker.service.OpenFoodFactsService
import com.inventory.barcodetracker.service.UpdateCheckService
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
    data class FoundPiece(val piece: PieceEntity) : ScannedItemState()
    data class FoundProduct(val product: ProductEntity) : ScannedItemState()
    data class NotFound(
        val barcode: String,
        val initialName: String = "",
        val initialBrand: String = "",
        val initialCategory: String = "Grocery",
    ) : ScannedItemState()
}

class QuickScanViewModel(
    private val repository: InventoryRepository,
    private val scannerService: BarcodeScannerService,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol
    val budgetLimit: StateFlow<Double> = currencyPreferences.budgetLimit

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

    private val _scanAnotherPromptItem = MutableStateFlow<String?>(null)
    val scanAnotherPromptItem: StateFlow<String?> = _scanAnotherPromptItem.asStateFlow()

    private var isCurrentFlowFromScanner = false

    fun clearPendingBasketItem() {
        _pendingBasketItem.value = null
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
            if (pending != null) {
                _pendingBasketItem.value = null
                addPieceToList(id, pending.piece, pending.quantity)
                _snackbarMessages.emit("Created basket \"$formattedName\" & added ${pending.piece.name}")
                if (pending.fromScanner) {
                    _scanAnotherPromptItem.value = pending.piece.name
                }
            } else {
                _snackbarMessages.emit("Created basket \"$formattedName\"")
            }
        }
    }

    private suspend fun addPieceToList(listId: Long, piece: PieceEntity, deltaQty: Int) {
        val existing = repository.getItemsForListSync(listId).firstOrNull { it.barcode == piece.barcode }
        val newQty = (existing?.quantity ?: 0) + deltaQty
        if (newQty > 0) {
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
                    isChecked = existing?.isChecked ?: false
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
            _snackbarMessages.emit("Duplicated basket as \"$formattedName\"")
        }
    }

    fun renameShoppingList(listId: Long, newName: String) {
        viewModelScope.launch {
            val existing = repository.getAllShoppingListsSync().firstOrNull { it.id == listId }
            if (existing != null) {
                val formattedName = currencyPreferences.formatText(newName)
                repository.upsertShoppingList(existing.copy(name = formattedName))
                _snackbarMessages.emit("Renamed basket to \"$formattedName\"")
            }
        }
    }

    fun setBasketBudgetLimit(listId: Long, limit: Double) {
        viewModelScope.launch {
            val existing = repository.getAllShoppingListsSync().firstOrNull { it.id == listId }
            if (existing != null) {
                repository.upsertShoppingList(existing.copy(budgetLimit = limit))
                val msg = if (limit > 0) "Budget limit set to ${currencySymbol.value}${String.format(Locale.US, "%.2f", limit)} for \"${existing.name}\"" else "Budget cleared for \"${existing.name}\""
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
                val msg = if (limit > 0) "Set ${symbol}${String.format(Locale.US, "%.2f", limit)} budget limit for ${lists.size} selected basket(s)" else "Cleared budget for ${lists.size} selected basket(s)"
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
            _snackbarMessages.emit("Applied $titleClean (-${symbol}${String.format(Locale.US, "%.2f", voucher.fixedPrice)}) to Basket!")
        }
    }

    fun toggleBasketItemChecked(listId: Long, barcode: String, isChecked: Boolean) {
        viewModelScope.launch {
            repository.setShoppingListItemChecked(listId, barcode, isChecked)
        }
    }

    fun adjustBasketItemQuantity(listId: Long, barcode: String, delta: Int) {
        viewModelScope.launch {
            val existing = repository.getItemsForListSync(listId).firstOrNull { it.barcode == barcode } ?: return@launch
            val newQty = existing.quantity + delta
            repository.updateShoppingListItemQuantity(listId, barcode, newQty)
        }
    }

    fun removeShoppingListItem(listId: Long, barcode: String) {
        viewModelScope.launch {
            repository.removeShoppingListItem(listId, barcode)
            _snackbarMessages.emit("Removed item/voucher from basket")
        }
    }

    fun deleteShoppingList(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.deleteShoppingList(list)
            if (_activeListId.value == list.id) _activeListId.value = null
            _selectedListIds.value = _selectedListIds.value - list.id
            _snackbarMessages.emit("Deleted basket \"${list.name}\"")
        }
    }

    fun deleteSelectedShoppingLists() {
        viewModelScope.launch {
            val selected = _selectedListIds.value.toList()
            if (selected.isNotEmpty()) {
                repository.deleteShoppingListsByIds(selected)
                _selectedListIds.value = emptySet()
                _snackbarMessages.emit("Deleted ${selected.size} selected basket(s)")
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
                _snackbarMessages.emit("Applied ${voucher.title} (-${currencySymbol.value}${String.format(Locale.US, "%.2f", voucher.fixedPrice)}) to Basket!")
            }
        }
    }

    fun removeCartVoucher(voucher: OfferEntity) {
        _appliedCartVouchers.value = _appliedCartVouchers.value.filter { it != voucher }
        viewModelScope.launch {
            _snackbarMessages.emit("Removed voucher ${voucher.title}")
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
                val basketMsg = if (basketCount > 1) " to $basketCount selected baskets" else " to Basket"
                _snackbarMessages.emit("✓ Added ${updated.name}$priceInfo$basketMsg")
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
                _snackbarMessages.emit("✓ Added ${updated.name}$priceInfo to Basket (In cart: ${updated.stockQuantity})")
                return@launch
            }

            // 3. Not in local database -> Query Open Food Facts API
            val onlineProduct = OpenFoodFactsService.lookupBarcode(trimmed)
            if (onlineProduct != null) {
                _snackbarMessages.emit("Found online: ${onlineProduct.name}. Enter price to save.")
                _scannedItemState.value = ScannedItemState.NotFound(
                    barcode = trimmed,
                    initialName = onlineProduct.name,
                    initialBrand = onlineProduct.brand,
                    initialCategory = onlineProduct.category,
                )
            } else {
                _scannedItemState.value = ScannedItemState.NotFound(trimmed)
            }
        }
    }

    fun dismissScannedSheet() {
        _scannedItemState.value = ScannedItemState.Idle
    }

    fun adjustPieceStock(barcode: String, delta: Int) {
        viewModelScope.launch {
            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            if (targetLists.isEmpty() && delta > 0) {
                val piece = repository.getPieceByBarcode(barcode)
                if (piece != null) {
                    _pendingBasketItem.value = PendingBasketItem(piece, delta)
                }
                return@launch
            }

            repository.adjustPieceStock(barcode, delta)
            val updated = repository.getPieceByBarcode(barcode)
            updated?.let { piece ->
                _scannedItemState.value = ScannedItemState.FoundPiece(piece)

                for (listId in targetLists) {
                    addPieceToList(listId, piece, delta)
                }
            }
            _snackbarMessages.emit("Cart quantity updated ($delta)")
        }
    }

    fun adjustProductStock(sku: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustProductStock(sku, delta)
            val updated = repository.getProductBySku(sku)
            updated?.let {
                _scannedItemState.value = ScannedItemState.FoundProduct(it)
            }
            _snackbarMessages.emit("Product stock updated ($delta)")
        }
    }

    fun createPiece(piece: PieceEntity) {
        viewModelScope.launch {
            val fromScanner = isCurrentFlowFromScanner
            isCurrentFlowFromScanner = false
            repository.upsertPiece(piece)
            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            if (targetLists.isEmpty()) {
                _scannedItemState.value = ScannedItemState.Idle
                _pendingBasketItem.value = PendingBasketItem(piece, maxOf(1, piece.stockQuantity), fromScanner = fromScanner)
                return@launch
            }

            _scannedItemState.value = ScannedItemState.Idle
            for (listId in targetLists) {
                addPieceToList(listId, piece, maxOf(1, piece.stockQuantity))
            }
            _snackbarMessages.emit("✓ Added ${piece.name} to Basket")
            if (fromScanner) {
                _scanAnotherPromptItem.value = piece.name
            }
        }
    }

    fun applySavedOfferToPiece(barcode: String, offer: OfferEntity) {
        viewModelScope.launch {
            repository.applyOfferToPiece(barcode, offer)
            val updated = repository.getPieceByBarcode(barcode)
            updated?.let {
                _scannedItemState.value = ScannedItemState.FoundPiece(it)
            }
            _snackbarMessages.emit("Applied deal \"${offer.displayBadge}\"")
        }
    }

    fun getOfferHistoryForPiece(barcode: String): Flow<List<OfferEntity>> {
        return repository.getOfferHistoryForPiece(barcode)
    }

    fun finishAndSaveBasketForList(list: ShoppingListEntity) {
        viewModelScope.launch {
            val listItems = repository.getItemsForListSync(list.id)
            if (listItems.isEmpty()) {
                _snackbarMessages.emit("List \"${list.name}\" is empty!")
                return@launch
            }

            val symbol = currencySymbol.value
            val pieceMap = repository.getAllPiecesSync().associateBy { it.barcode }

            val itemLines = listItems.map { item ->
                if (item.offerType == "VOUCHER" || item.unitPrice < 0) {
                    val voucherName = item.barcode
                        .removePrefix("VOUCHER_")
                        .replace(Regex("""^\d+_"""), "")
                        .replace("_", " ")
                        .replace(Regex("""(\$|£|€|RON)\s+"""), "$1")
                        .trim()
                    val discountVal = kotlin.math.abs(item.unitPrice)
                    "• 🎟️ Voucher ($voucherName) — -$symbol${String.format(Locale.US, "%.2f", discountVal)}"
                } else {
                    val piece = pieceMap[item.barcode]
                    val effOfferType = if (item.offerType != "NONE") item.offerType else (piece?.offerType ?: "NONE")
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

                    val savingTag = if (offerDesc.isNotBlank()) " [$offerDesc" + (if (lineSavings > 0) " — saved $symbol${String.format(Locale.US, "%.2f", lineSavings)}" else "") + "]" else ""
                    "• $displayName (x${item.quantity})$savingTag — $symbol${String.format(Locale.US, "%.2f", lineTotal)}"
                }
            }

            val summaryText = "Basket: ${list.name}\n" + itemLines.joinToString("\n")

            val netTotalCost = maxOf(
                0.0,
                listItems.sumOf { item ->
                    if (item.offerType == "VOUCHER" || item.unitPrice < 0) {
                        item.unitPrice
                    } else {
                        val piece = pieceMap[item.barcode]
                        if (piece != null) {
                            val effOfferType = if (item.offerType != "NONE") item.offerType else piece.offerType
                            val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else piece.offerBuyQty
                            val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else piece.offerPayQty
                            val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else piece.offerFixedPrice
                            val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else piece.offerPercentOff

                            piece.copy(
                                stockQuantity = item.quantity,
                                unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
                                offerType = effOfferType,
                                offerBuyQty = effBuyQty,
                                offerPayQty = effPayQty,
                                offerFixedPrice = effFixedPrice,
                                offerPercentOff = effPercentOff
                            ).calculatedTotal
                        } else {
                            item.calculateItemTotal(null)
                        }
                    }
                }
            )

            val itemCount = listItems.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }.sumOf { it.quantity }

            repository.checkoutCartAndSaveTrip(
                totalCost = netTotalCost,
                itemCount = itemCount,
                itemsSummary = summaryText,
                currencySymbol = symbol,
            )
            resetClearedBasketPieceQuantities(list.id)
            repository.clearShoppingListItems(list.id)
            _snackbarMessages.emit("Saved \"${list.name}\" ($symbol${String.format(Locale.US, "%.2f", netTotalCost)}) to History!")
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
            _snackbarMessages.emit("Cleared basket \"${list.name}\"")
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
                _snackbarMessages.emit("Selected basket(s) are empty!")
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
            _snackbarMessages.emit("Cleared items from ${selectedIds.size} selected basket(s)")
        }
    }

    fun createProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.upsertProduct(product)
            _scannedItemState.value = ScannedItemState.FoundProduct(product)
            _snackbarMessages.emit("Created product ${product.name}")
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
                itemsSummary = summaryText.ifEmpty { "Purchased items" },
                currencySymbol = symbol,
            )
            _appliedCartVouchers.value = emptyList()
            _snackbarMessages.emit("Basket saved to History! Items remain in Items.")
        }
    }

    fun clearBasketOnly() {
        viewModelScope.launch {
            repository.resetCartQuantities()
            _appliedCartVouchers.value = emptyList()
            _snackbarMessages.emit("Basket cleared. Items remain saved in Items.")
        }
    }
}
