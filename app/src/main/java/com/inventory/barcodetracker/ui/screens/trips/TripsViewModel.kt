package com.inventory.barcodetracker.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun deleteTrip(trip: ShoppingTripEntity) {
        viewModelScope.launch {
            repository.deleteShoppingTrip(trip)
            _snackbarMessages.emit("Trip receipt deleted")
        }
    }
}
