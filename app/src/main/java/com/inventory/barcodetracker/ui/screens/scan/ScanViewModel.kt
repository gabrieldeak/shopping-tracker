package com.inventory.barcodetracker.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
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

class ScanViewModel(
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

    init {
        viewModelScope.launch {
            val lists = repository.getAllShoppingListsSync()
            if (lists.isEmpty()) {
                val defaultId = repository.upsertShoppingList(ShoppingListEntity(name = "Main Shopping Basket"))
                _activeListId.value = defaultId
                _selectedListIds.value = setOf(defaultId)
            } else {
                _activeListId.value = lists.first().id
                _selectedListIds.value = setOf(lists.first().id)
            }

            val update = UpdateCheckService.checkForUpdates()
            if (update.hasUpdate) {
                _appUpdateInfo.value = update
            }
        }
    }

    fun dismissUpdateBanner() {
        _appUpdateInfo.value = null
    }

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            val id = repository.upsertShoppingList(ShoppingListEntity(name = name))
            _activeListId.value = id
            _selectedListIds.value = _selectedListIds.value + id
            _snackbarMessages.emit("Created basket \"$name\"")
        }
    }

    fun copyShoppingList(listId: Long, newName: String) {
        viewModelScope.launch {
            val newId = repository.copyShoppingList(listId, newName)
            _activeListId.value = newId
            _selectedListIds.value = _selectedListIds.value + newId
            _snackbarMessages.emit("Duplicated basket as \"$newName\"")
        }
    }

    fun renameShoppingList(listId: Long, newName: String) {
        viewModelScope.launch {
            val existing = repository.getAllShoppingListsSync().firstOrNull { it.id == listId }
            if (existing != null) {
                repository.upsertShoppingList(existing.copy(name = newName))
                _snackbarMessages.emit("Renamed basket to \"$newName\"")
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
                    resolveBarcode(result.barcode)
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

    fun resolveBarcode(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val symbol = currencySymbol.value
            // 1. First check local SQLite database
            val piece = repository.getPieceByBarcode(trimmed)
            if (piece != null) {
                // Auto-add +1 to basket when scanned
                repository.adjustPieceStock(piece.barcode, 1)
                val updated = repository.getPieceByBarcode(piece.barcode) ?: piece
                val priceInfo = if (updated.unitPrice > 0) String.format(Locale.US, " (%s%.2f)", symbol, updated.unitPrice) else ""
                _snackbarMessages.emit("✓ Added ${updated.name}$priceInfo to Basket (In cart: ${updated.stockQuantity})")
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
            repository.adjustPieceStock(barcode, delta)
            val updated = repository.getPieceByBarcode(barcode)
            updated?.let { piece ->
                _scannedItemState.value = ScannedItemState.FoundPiece(piece)

                val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
                for (listId in targetLists) {
                    if (piece.stockQuantity > 0) {
                        repository.upsertShoppingListItem(
                            ShoppingListItemEntity(
                                listId = listId,
                                barcode = piece.barcode,
                                quantity = piece.stockQuantity,
                                unitPrice = piece.unitPrice,
                                offerType = piece.offerType,
                                offerBuyQty = piece.offerBuyQty,
                                offerPayQty = piece.offerPayQty,
                                offerFixedPrice = piece.offerFixedPrice,
                                offerPercentOff = piece.offerPercentOff
                            )
                        )
                    } else {
                        repository.removeShoppingListItem(listId, piece.barcode)
                    }
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
            repository.upsertPiece(piece)
            _scannedItemState.value = ScannedItemState.FoundPiece(piece)

            val targetLists = _selectedListIds.value.ifEmpty { _activeListId.value?.let { setOf(it) } ?: emptySet() }
            for (listId in targetLists) {
                repository.upsertShoppingListItem(
                    ShoppingListItemEntity(
                        listId = listId,
                        barcode = piece.barcode,
                        quantity = piece.stockQuantity,
                        unitPrice = piece.unitPrice,
                        offerType = piece.offerType,
                        offerBuyQty = piece.offerBuyQty,
                        offerPayQty = piece.offerPayQty,
                        offerFixedPrice = piece.offerFixedPrice,
                        offerPercentOff = piece.offerPercentOff
                    )
                )
            }
            _snackbarMessages.emit("Created item ${piece.name}")
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
                val p = pieceMap[item.barcode]
                val name = p?.name ?: item.barcode
                val total = item.quantity * item.unitPrice
                if (item.unitPrice > 0) {
                    "• $name (x${item.quantity}) — $symbol${String.format(Locale.US, "%.2f", total)}"
                } else {
                    "• $name (x${item.quantity})"
                }
            }

            val summaryText = "Basket: ${list.name}\n" + itemLines.joinToString("\n")
            val totalCost = listItems.sumOf { it.quantity * it.unitPrice }
            val itemCount = listItems.sumOf { it.quantity }

            repository.checkoutCartAndSaveTrip(
                totalCost = totalCost,
                itemCount = itemCount,
                itemsSummary = summaryText,
                currencySymbol = symbol,
            )
            repository.clearShoppingListItems(list.id)
            _snackbarMessages.emit("Saved \"${list.name}\" to History and cleared basket!")
        }
    }

    fun clearListBasket(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.clearShoppingListItems(list.id)
            _snackbarMessages.emit("Cleared basket \"${list.name}\"")
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
            _snackbarMessages.emit("Basket saved to History! Items remain in My Items.")
        }
    }

    fun clearBasketOnly() {
        viewModelScope.launch {
            repository.resetCartQuantities()
            _appliedCartVouchers.value = emptyList()
            _snackbarMessages.emit("Basket cleared. Items remain saved in My Items.")
        }
    }
}
