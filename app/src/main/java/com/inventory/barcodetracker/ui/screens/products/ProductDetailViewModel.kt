package com.inventory.barcodetracker.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.model.BomItem
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    val productSku: String,
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol

    private val _product = MutableStateFlow<ProductEntity?>(null)
    val product: StateFlow<ProductEntity?> = _product.asStateFlow()

    val bomItems: StateFlow<List<BomItem>> = repository.getBomForProduct(productSku)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allAvailablePieces: StateFlow<List<PieceEntity>> = repository.allPieces
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Build capacity is the minimum of floor(piece.stockQuantity / bom.quantityRequired)
    val buildCapacity: StateFlow<Int> = bomItems.map { items ->
        if (items.isEmpty()) {
            0
        } else {
            items.minOf { item ->
                if (item.quantityRequired <= 0) 0 else item.stockQuantity / item.quantityRequired
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _product.value = repository.getProductBySku(productSku)
        }
    }

    fun executeBuild(count: Int) {
        viewModelScope.launch {
            val success = repository.executeProductBuild(productSku, count)
            if (success) {
                _snackbarMessages.emit("Successfully built $count unit(s) and deducted BOM parts!")
                loadProduct()
            } else {
                _snackbarMessages.emit("Build failed: insufficient stock for required BOM parts.")
            }
        }
    }

    fun addPieceToBom(barcode: String, quantity: Int) {
        viewModelScope.launch {
            val crossRef = ProductPieceCrossRef(
                productSku = productSku,
                barcode = barcode,
                quantityRequiredPerProduct = maxOf(1, quantity)
            )
            repository.upsertProductPiece(crossRef)
            _snackbarMessages.emit("Added part to Bill of Materials")
        }
    }

    fun removePieceFromBom(barcode: String) {
        viewModelScope.launch {
            repository.removePieceFromProduct(productSku, barcode)
            _snackbarMessages.emit("Removed part from BOM")
        }
    }
}
