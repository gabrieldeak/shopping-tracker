package com.inventory.barcodetracker.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol

    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(value = false)
    val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _selectedBarcodes = MutableStateFlow<Set<String>>(emptySet())
    val selectedBarcodes: StateFlow<Set<String>> = combine(_selectedBarcodes, repository.allPieces) { selected, pieces ->
        selected.intersect(pieces.map { it.barcode }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allShoppingLists: StateFlow<List<ShoppingListEntity>> = repository.allShoppingLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedOffers: StateFlow<List<OfferEntity>> = repository.allOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val piecesList = combine(
        repository.allPieces,
        _searchFilter,
        _lowStockOnly,
    ) { pieces, filter, isLowStockOnly ->
        val trimmed = filter.trim()
        pieces.filter { piece ->
            val matchesFilter = trimmed.isEmpty() ||
                    piece.name.contains(trimmed, ignoreCase = true) ||
                    piece.barcode.contains(trimmed, ignoreCase = true) ||
                    piece.location.contains(trimmed, ignoreCase = true) ||
                    piece.description.contains(trimmed, ignoreCase = true) ||
                    piece.offerDescription.contains(trimmed, ignoreCase = true) ||
                    piece.offerType.contains(trimmed, ignoreCase = true)
            val matchesLowStock = !isLowStockOnly || (piece.stockQuantity <= piece.minStockThreshold)
            matchesFilter && matchesLowStock
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    fun onSearchFilterChanged(filter: String) {
        _searchFilter.value = filter
    }

    fun toggleLowStockOnly() {
        _lowStockOnly.value = !_lowStockOnly.value
    }

    fun toggleSelectPiece(barcode: String) {
        if (_selectedBarcodes.value.contains(barcode)) {
            _selectedBarcodes.value = _selectedBarcodes.value - barcode
        } else {
            _selectedBarcodes.value = _selectedBarcodes.value + barcode
        }
    }

    fun selectAllPieces(barcodes: List<String>) {
        if (_selectedBarcodes.value.size == barcodes.size && barcodes.isNotEmpty()) {
            _selectedBarcodes.value = emptySet()
        } else {
            _selectedBarcodes.value = barcodes.toSet()
        }
    }

    fun clearSelections() {
        _selectedBarcodes.value = emptySet()
    }

    fun adjustStock(barcode: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustPieceStock(barcode, delta)
        }
    }

    fun savePiece(piece: PieceEntity, updateBasketsPrice: Boolean = false) {
        viewModelScope.launch {
            val formattedPiece = piece.copy(
                name = currencyPreferences.formatText(piece.name),
                location = currencyPreferences.formatText(piece.location)
            )
            repository.upsertPiece(formattedPiece)
            if (updateBasketsPrice) {
                repository.updateShoppingListItemPrice(formattedPiece.barcode, formattedPiece.unitPrice)
                _snackbarMessages.emit("Saved ${formattedPiece.name} & updated price in active baskets")
            } else {
                _snackbarMessages.emit("Saved ${formattedPiece.name}")
            }
        }
    }

    fun deletePiece(piece: PieceEntity) {
        viewModelScope.launch {
            try {
                repository.deletePiece(piece)
                _snackbarMessages.emit("Deleted ${piece.name}")
            } catch (_: Exception) {
                _snackbarMessages.emit("Cannot delete item: it is currently linked to an assembled product.")
            }
        }
    }

    fun deleteSelectedPieces() {
        viewModelScope.launch {
            val selected = _selectedBarcodes.value.toList()
            if (selected.isNotEmpty()) {
                try {
                    repository.deletePiecesByBarcodes(selected)
                    _selectedBarcodes.value = emptySet()
                    _snackbarMessages.emit("Deleted ${selected.size} selected item(s)")
                } catch (_: Exception) {
                    _snackbarMessages.emit("Could not delete some items linked to product BOMs")
                }
            }
        }
    }

    fun addSelectedPiecesToShoppingLists(targetListIds: List<Long>) {
        viewModelScope.launch {
            val selectedBarcodesList = _selectedBarcodes.value.toList()
            if (selectedBarcodesList.isEmpty() || targetListIds.isEmpty()) return@launch

            val piecesMap = repository.getAllPiecesSync().associateBy { it.barcode }

            for (listId in targetListIds) {
                val existingItems = repository.getItemsForListSync(listId).associateBy { it.barcode }
                for (barcode in selectedBarcodesList) {
                    val piece = piecesMap[barcode] ?: continue
                    val existing = existingItems[barcode]
                    val qty = (existing?.quantity ?: 0) + 1
                    repository.upsertShoppingListItem(
                        ShoppingListItemEntity(
                            listId = listId,
                            barcode = piece.barcode,
                            quantity = qty,
                            unitPrice = piece.unitPrice,
                            offerType = piece.offerType,
                            offerBuyQty = piece.offerBuyQty,
                            offerPayQty = piece.offerPayQty,
                            offerFixedPrice = piece.offerFixedPrice,
                            offerPercentOff = piece.offerPercentOff,
                            isChecked = existing?.isChecked ?: false
                        )
                    )
                }
            }
            val totalAdded = selectedBarcodesList.size
            _selectedBarcodes.value = emptySet()
            _snackbarMessages.emit("Added $totalAdded item(s) to selected shopping basket(s)!")
        }
    }
}
