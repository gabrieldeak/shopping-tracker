package com.inventory.barcodetracker.ui.screens.history

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(
    private val repository: InventoryRepository,
    currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    val currencySymbol: StateFlow<String> = currencyPreferences.currencySymbol

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val shoppingTrips: StateFlow<List<ShoppingTripEntity>> = combine(
        repository.allShoppingTrips,
        _searchQuery
    ) { trips, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            trips
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            trips.filter { trip ->
                trip.itemsSummary.contains(trimmed, ignoreCase = true) ||
                    sdf.format(Date(trip.tripDate)).contains(trimmed, ignoreCase = true) ||
                    String.format(Locale.US, "%.2f", trip.totalCost).contains(trimmed)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private val _selectedTripIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTripIds: StateFlow<Set<Long>> = combine(_selectedTripIds, repository.allShoppingTrips) { selected, trips ->
        selected.intersect(trips.map { it.id }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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
