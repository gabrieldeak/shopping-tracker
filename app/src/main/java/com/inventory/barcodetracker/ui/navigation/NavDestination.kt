package com.inventory.barcodetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Scan : Screen("scan", "Quick Scan", Icons.Default.QrCodeScanner)
    object Items : Screen("items", "Items", Icons.Default.Inventory2)
    object Offers : Screen("offers", "Offers", Icons.Default.LocalOffer)
    object Trips : Screen("trips", "History", Icons.AutoMirrored.Filled.ReceiptLong)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Scan,
    Screen.Items,
    Screen.Offers,
    Screen.Trips,
    Screen.Settings,
)
