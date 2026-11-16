package com.inventory.barcodetracker.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.barcodetracker.di.AppContainer
import com.inventory.barcodetracker.ui.AppViewModelFactory
import com.inventory.barcodetracker.ui.screens.offers.OffersScreen
import com.inventory.barcodetracker.ui.screens.offers.OffersViewModel
import com.inventory.barcodetracker.ui.screens.pieces.PiecesScreen
import com.inventory.barcodetracker.ui.screens.pieces.PiecesViewModel
import com.inventory.barcodetracker.ui.screens.scan.QuickScanScreen
import com.inventory.barcodetracker.ui.screens.scan.ScanViewModel
import com.inventory.barcodetracker.ui.screens.settings.SettingsScreen
import com.inventory.barcodetracker.ui.screens.settings.SettingsViewModel
import com.inventory.barcodetracker.ui.screens.trips.TripsScreen
import com.inventory.barcodetracker.ui.screens.trips.TripsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })

    // Instantiating ViewModels
    val scanViewModel: ScanViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences,
            scannerService = appContainer.barcodeScannerService
        )
    )
    val piecesViewModel: PiecesViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences
        )
    )
    val offersViewModel: OffersViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences
        )
    )
    val tripsViewModel: TripsViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences
        )
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences
        )
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                bottomNavScreens.forEachIndexed { index, screen ->
                    val selected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) { page ->
            when (page) {
                0 -> QuickScanScreen(
                    viewModel = scanViewModel,
                    onNavigateToProductBom = { _ -> },
                    snackbarHostState = snackbarHostState
                )
                1 -> PiecesScreen(
                    viewModel = piecesViewModel,
                    snackbarHostState = snackbarHostState
                )
                2 -> OffersScreen(
                    viewModel = offersViewModel,
                    snackbarHostState = snackbarHostState
                )
                3 -> TripsScreen(
                    viewModel = tripsViewModel,
                    snackbarHostState = snackbarHostState
                )
                4 -> SettingsScreen(
                    viewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
