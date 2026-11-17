package com.inventory.barcodetracker.ui.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.OfferEntity
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
import java.util.Locale

class OffersViewModel(
    private val repository: InventoryRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val allPieces: StateFlow<List<PieceEntity>> = repository.allPieces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offersList: StateFlow<List<OfferEntity>> = combine(repository.allOffers, _searchQuery) { offers, query ->
        if (query.isBlank()) {
            offers
        } else {
            val q = query.trim().lowercase(Locale.ROOT)
            offers.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.storeName.lowercase(Locale.ROOT).contains(q) ||
                    it.displayBadge.lowercase(Locale.ROOT).contains(q) ||
                    it.notes.lowercase(Locale.ROOT).contains(q) ||
                    it.offerType.lowercase(Locale.ROOT).contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedOfferIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedOfferIds: StateFlow<Set<Long>> = _selectedOfferIds.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelectOffer(offerId: Long) {
        if (_selectedOfferIds.value.contains(offerId)) {
            _selectedOfferIds.value = _selectedOfferIds.value - offerId
        } else {
            _selectedOfferIds.value = _selectedOfferIds.value + offerId
        }
    }

    fun selectAllOffers(offerIds: List<Long>) {
        if (_selectedOfferIds.value.size == offerIds.size && offerIds.isNotEmpty()) {
            _selectedOfferIds.value = emptySet()
        } else {
            _selectedOfferIds.value = offerIds.toSet()
        }
    }

    fun clearOfferSelections() {
        _selectedOfferIds.value = emptySet()
    }

    fun saveOffer(offer: OfferEntity) {
        viewModelScope.launch {
            val formattedOffer = offer.copy(
                title = currencyPreferences.formatText(offer.title),
                storeName = currencyPreferences.formatText(offer.storeName)
            )
            repository.upsertOffer(formattedOffer)
            val label = formattedOffer.title.ifBlank { formattedOffer.getDisplayBadge(currencySymbol.value) }
            _snackbarMessages.emit("Offer \"$label\" saved successfully.")
        }
    }

    fun deleteOffer(offer: OfferEntity) {
        viewModelScope.launch {
            repository.deleteOffer(offer)
            _selectedOfferIds.value = _selectedOfferIds.value - offer.id
            val label = offer.title.ifBlank { offer.getDisplayBadge(currencySymbol.value) }
            _snackbarMessages.emit("Offer \"$label\" deleted.")
        }
    }

    fun deleteSelectedOffers() {
        viewModelScope.launch {
            val selected = _selectedOfferIds.value.toList()
            if (selected.isNotEmpty()) {
                repository.deleteOffersByIds(selected)
                _selectedOfferIds.value = emptySet()
                _snackbarMessages.emit("Deleted ${selected.size} selected offer(s).")
            }
        }
    }

    fun applyOfferToPiece(barcode: String, offer: OfferEntity) {
        viewModelScope.launch {
            repository.applyOfferToPiece(barcode, offer)
            val piece = repository.getPieceByBarcode(barcode)
            _snackbarMessages.emit("Applied deal \"${offer.displayBadge}\" to ${piece?.name ?: barcode}.")
        }
    }
}
