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
import com.inventory.barcodetracker.ui.screens.history.HistoryScreen
import com.inventory.barcodetracker.ui.screens.history.HistoryViewModel
import com.inventory.barcodetracker.ui.screens.items.ItemsScreen
import com.inventory.barcodetracker.ui.screens.items.ItemsViewModel
import com.inventory.barcodetracker.ui.screens.offers.OffersScreen
import com.inventory.barcodetracker.ui.screens.offers.OffersViewModel
import com.inventory.barcodetracker.ui.screens.quickscan.QuickScanScreen
import com.inventory.barcodetracker.ui.screens.quickscan.QuickScanViewModel
import com.inventory.barcodetracker.ui.screens.settings.SettingsScreen
import com.inventory.barcodetracker.ui.screens.settings.SettingsViewModel
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
    val quickScanViewModel: QuickScanViewModel = viewModel(
        factory = AppViewModelFactory(
            repository = appContainer.inventoryRepository,
            currencyPreferences = appContainer.currencyPreferences,
            scannerService = appContainer.barcodeScannerService
        )
    )
    val itemsViewModel: ItemsViewModel = viewModel(
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
    val historyViewModel: HistoryViewModel = viewModel(
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
                    viewModel = quickScanViewModel,
                    onNavigateToProductBom = { _ -> },
                    snackbarHostState = snackbarHostState
                )
                1 -> ItemsScreen(
                    viewModel = itemsViewModel,
                    snackbarHostState = snackbarHostState
                )
                2 -> OffersScreen(
                    viewModel = offersViewModel,
                    snackbarHostState = snackbarHostState
                )
                3 -> HistoryScreen(
                    viewModel = historyViewModel,
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
