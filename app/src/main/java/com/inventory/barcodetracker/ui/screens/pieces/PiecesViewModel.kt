package com.inventory.barcodetracker.ui.screens.pieces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.PieceEntity
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

class PiecesViewModel(
    private val repository: InventoryRepository,
    currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol

    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(value = false)
    val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

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

    fun adjustStock(barcode: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustPieceStock(barcode, delta)
        }
    }

    fun savePiece(piece: PieceEntity) {
        viewModelScope.launch {
            repository.upsertPiece(piece)
            _snackbarMessages.emit("Saved ${piece.name}")
        }
    }

    fun deletePiece(piece: PieceEntity) {
        viewModelScope.launch {
            try {
                repository.deletePiece(piece)
                _snackbarMessages.emit("Deleted ${piece.name}")
            } catch (_: Exception) {
                // If ForeignKey constraint fails because piece is used in a BOM
                _snackbarMessages.emit("Cannot delete item: it is currently linked to an assembled product.")
            }
        }
    }
}
