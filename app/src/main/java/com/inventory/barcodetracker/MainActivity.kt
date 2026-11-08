package com.inventory.barcodetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.ui.navigation.AppNavHost
import com.inventory.barcodetracker.ui.theme.BarcodeInventoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as InventoryApplication).container

        setContent {
            val themeMode by appContainer.currencyPreferences.themeMode.collectAsStateWithLifecycle()
            BarcodeInventoryTheme(themeMode = themeMode) {
                AppNavHost(appContainer = appContainer)
            }
        }
    }
}
