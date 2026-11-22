package com.inventory.barcodetracker.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.barcodetracker.di.AppContainer
import com.inventory.barcodetracker.ui.AppViewModelFactory
import com.inventory.barcodetracker.ui.components.AppTutorialDialog
import com.inventory.barcodetracker.ui.components.FirstLaunchSetupDialog
import com.inventory.barcodetracker.ui.components.LocalCapitalizationMode
import com.inventory.barcodetracker.ui.i18n.AppStrings
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
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
    val appLanguage by appContainer.currencyPreferences.appLanguage.collectAsStateWithLifecycle()
    val currentCurrency by appContainer.currencyPreferences.currencySymbol.collectAsStateWithLifecycle()
    val capitalizationMode by appContainer.currencyPreferences.capitalizationMode.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by appContainer.currencyPreferences.hasCompletedOnboarding.collectAsStateWithLifecycle()
    var showTutorialThisSession by remember {
        mutableStateOf(!appContainer.currencyPreferences.hideTutorialOnStartup.value)
    }
    val strings = remember(appLanguage) { AppStrings.forLanguage(appLanguage) }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalCapitalizationMode provides capitalizationMode,
    ) {
        if (!hasCompletedOnboarding) {
            FirstLaunchSetupDialog(
                initialLanguage = appLanguage,
                initialCurrency = currentCurrency,
                onComplete = { language, currency ->
                    appContainer.currencyPreferences.setAppLanguage(language)
                    appContainer.currencyPreferences.setCurrencySymbol(currency)
                    appContainer.currencyPreferences.setOnboardingCompleted(true)
                    showTutorialThisSession = !appContainer.currencyPreferences.hideTutorialOnStartup.value
                }
            )
        } else if (showTutorialThisSession) {
            AppTutorialDialog(
                currencySymbol = currentCurrency,
                onDismiss = { dontShowAgain ->
                    if (dontShowAgain) {
                        appContainer.currencyPreferences.setHideTutorialOnStartup(true)
                    }
                    showTutorialThisSession = false
                }
            )
        }
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    bottomNavScreens.forEachIndexed { index, screen ->
                        val selected = pagerState.currentPage == index
                        val label = when (screen) {
                            Screen.QuickScan -> strings.tabQuickScan
                            Screen.Items -> strings.tabItems
                            Screen.Offers -> strings.tabOffers
                            Screen.History -> strings.tabHistory
                            Screen.Settings -> strings.tabSettings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (pagerState.currentPage != index) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = label) },
                            label = { Text(label) }
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
}
