package com.inventory.barcodetracker.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripsViewModel(
    private val repository: InventoryRepository,
    currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol

    val shoppingTrips: StateFlow<List<ShoppingTripEntity>> = repository.allShoppingTrips
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _selectedTripIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTripIds: StateFlow<Set<Long>> = _selectedTripIds.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun toggleSelectTrip(tripId: Long) {
        if (_selectedTripIds.value.contains(tripId)) {
            _selectedTripIds.value = _selectedTripIds.value - tripId
        } else {
            _selectedTripIds.value = _selectedTripIds.value + tripId
        }
    }

    fun selectAllTrips(tripIds: List<Long>) {
        if (_selectedTripIds.value.size == tripIds.size && tripIds.isNotEmpty()) {
            _selectedTripIds.value = emptySet()
        } else {
            _selectedTripIds.value = tripIds.toSet()
        }
    }

    fun clearTripSelections() {
        _selectedTripIds.value = emptySet()
    }

    fun deleteTrip(trip: ShoppingTripEntity) {
        viewModelScope.launch {
            repository.deleteShoppingTrip(trip)
            _selectedTripIds.value = _selectedTripIds.value - trip.id
            _snackbarMessages.emit("Trip receipt deleted")
        }
    }

    fun deleteSelectedTrips() {
        viewModelScope.launch {
            val selected = _selectedTripIds.value.toList()
            if (selected.isNotEmpty()) {
                repository.deleteShoppingTripsByIds(selected)
                _selectedTripIds.value = emptySet()
                _snackbarMessages.emit("Deleted ${selected.size} selected receipt(s)")
            }
        }
    }

    fun reloadTripIntoCart(trip: ShoppingTripEntity) {
        viewModelScope.launch {
            try {
                val listName = repository.reloadTripIntoCart(trip)
                _snackbarMessages.emit("Reloaded basket \"$listName\" into active cart!")
            } catch (e: Exception) {
                _snackbarMessages.emit("Failed to reload basket: ${e.message}")
            }
        }
    }
}
