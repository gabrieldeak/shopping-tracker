package com.inventory.barcodetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inventory.barcodetracker.di.AppContainer
import com.inventory.barcodetracker.ui.AppViewModelFactory
import com.inventory.barcodetracker.ui.screens.pieces.PiecesScreen
import com.inventory.barcodetracker.ui.screens.pieces.PiecesViewModel
import com.inventory.barcodetracker.ui.screens.scan.QuickScanScreen
import com.inventory.barcodetracker.ui.screens.scan.ScanViewModel
import com.inventory.barcodetracker.ui.screens.settings.SettingsScreen
import com.inventory.barcodetracker.ui.screens.settings.SettingsViewModel
import com.inventory.barcodetracker.ui.screens.trips.TripsScreen
import com.inventory.barcodetracker.ui.screens.trips.TripsViewModel

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard / Quick Scan
            composable(Screen.Scan.route) {
                val scanViewModel: ScanViewModel = viewModel(
                    factory = AppViewModelFactory(
                        repository = appContainer.inventoryRepository,
                        currencyPreferences = appContainer.currencyPreferences,
                        scannerService = appContainer.barcodeScannerService
                    )
                )
                QuickScanScreen(
                    viewModel = scanViewModel,
                    onNavigateToProductBom = { sku ->
                        navController.navigate("products/$sku")
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            // My Items Catalog
            composable(Screen.Items.route) {
                val piecesViewModel: PiecesViewModel = viewModel(
                    factory = AppViewModelFactory(
                        repository = appContainer.inventoryRepository,
                        currencyPreferences = appContainer.currencyPreferences
                    )
                )
                PiecesScreen(
                    viewModel = piecesViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            // Shopping Trips History
            composable(Screen.Trips.route) {
                val tripsViewModel: TripsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        repository = appContainer.inventoryRepository,
                        currencyPreferences = appContainer.currencyPreferences
                    )
                )
                TripsScreen(
                    viewModel = tripsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            // Settings & Data Management
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        repository = appContainer.inventoryRepository,
                        currencyPreferences = appContainer.currencyPreferences
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
