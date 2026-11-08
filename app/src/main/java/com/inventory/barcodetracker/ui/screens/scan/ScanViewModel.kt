package com.inventory.barcodetracker.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.scanner.ScanResult
import com.inventory.barcodetracker.service.OpenFoodFactsService
import java.util.Locale
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

    fun setBudgetLimit(limit: Double) {
        currencyPreferences.setBudgetLimit(limit)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _scannedItemState = MutableStateFlow<ScannedItemState>(ScannedItemState.Idle)
    val scannedItemState: StateFlow<ScannedItemState> = _scannedItemState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val basketSummary: StateFlow<Pair<Double, Int>> = combine(
        repository.allPieces,
        repository.allProducts,
    ) { pieces, products ->
        val pieceValue = pieces.sumOf { it.calculatedTotal }
        val pieceCount = pieces.sumOf { it.stockQuantity }
        val productValue = products.sumOf { it.stockQuantity * it.unitPrice }
        val productCount = products.sumOf { it.stockQuantity }
        Pair(pieceValue + productValue, pieceCount + productCount)
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
                        it.location.contains(trimmed, ignoreCase = true)
            }
            val matchingProducts = products.filter {
                it.productSku.contains(trimmed, ignoreCase = true) ||
                        it.name.contains(trimmed, ignoreCase = true)
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
            // Refresh current state if piece is still displayed
            val updated = repository.getPieceByBarcode(barcode)
            updated?.let {
                _scannedItemState.value = ScannedItemState.FoundPiece(it)
            }
            _snackbarMessages.emit("Piece stock updated ($delta)")
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
            _snackbarMessages.emit("Created piece ${piece.name}")
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
                val total = it.stockQuantity * it.unitPrice
                if (it.unitPrice > 0) {
                    "${it.name} (x${it.stockQuantity}) — $symbol${String.format(Locale.US, "%.2f", total)}"
                } else {
                    "${it.name} (x${it.stockQuantity})"
                }
            }

            val productLines = currentProducts.map {
                val total = it.stockQuantity * it.unitPrice
                if (it.unitPrice > 0) {
                    "${it.name} (x${it.stockQuantity}) — $symbol${String.format(Locale.US, "%.2f", total)}"
                } else {
                    "${it.name} (x${it.stockQuantity})"
                }
            }

            val summaryText = (pieceLines + productLines).joinToString("\n")

            repository.checkoutCartAndSaveTrip(
                totalCost = totalCost,
                itemCount = itemCount,
                itemsSummary = summaryText.ifEmpty { "Purchased items" },
                currencySymbol = symbol,
            )
            _snackbarMessages.emit("Basket saved to History! Items remain in My Items.")
        }
    }

    fun clearBasketOnly() {
        viewModelScope.launch {
            repository.resetCartQuantities()
            _snackbarMessages.emit("Basket cleared. Items remain saved in My Items.")
        }
    }
}
