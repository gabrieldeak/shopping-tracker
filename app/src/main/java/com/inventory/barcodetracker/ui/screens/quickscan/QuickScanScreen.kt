package com.inventory.barcodetracker.ui.screens.quickscan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppFilledTonalButton
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import com.inventory.barcodetracker.ui.components.AppSearchBar
import com.inventory.barcodetracker.ui.components.ShoppingListCard
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScanScreen(
    viewModel: QuickScanViewModel,
    onNavigateToProductBom: (sku: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val scannedItemState by viewModel.scannedItemState.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimit.collectAsStateWithLifecycle()
    val savedOffers by viewModel.savedOffers.collectAsStateWithLifecycle()

    val allShoppingLists by viewModel.allShoppingLists.collectAsStateWithLifecycle()
    val allShoppingListItems by viewModel.allShoppingListItems.collectAsStateWithLifecycle()
    val allPiecesMap by viewModel.allPiecesMap.collectAsStateWithLifecycle()
    val selectedListIds by viewModel.selectedListIds.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    var showLanguageMenu by remember { mutableStateOf(false) }
    var pendingCurrencySwitchPrompt by remember { mutableStateOf<String?>(null) }
    var listForMixAndMatch by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var selectedSearchBarcodes by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            selectedSearchBarcodes = emptySet()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    BackHandler(enabled = searchQuery.isNotBlank()) {
        viewModel.onSearchQueryChanged("")
        focusManager.clearFocus()
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showVoucherDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteBatch by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listToCopy by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listToDelete by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listForBudgetDialog by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listForVoucherDialog by remember { mutableStateOf<ShoppingListEntity?>(null) }

    var manualBarcodeText by remember { mutableStateOf("") }
    var newListNameText by remember { mutableStateOf("") }
    var renameListNameText by remember { mutableStateOf("") }
    var copyListNameText by remember { mutableStateOf("") }
    var budgetInputText by remember { mutableStateOf("") }
    var includeDateInName by remember { mutableStateOf(true) }
    var selectedBasketDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    val pendingBasketItem by viewModel.pendingBasketItem.collectAsStateWithLifecycle()
    val pendingBasketBarcodes by viewModel.pendingBasketBarcodes.collectAsStateWithLifecycle()
    val scanAnotherPromptItem by viewModel.scanAnotherPromptItem.collectAsStateWithLifecycle()
    LaunchedEffect(pendingBasketItem, pendingBasketBarcodes) {
        if (pendingBasketItem != null || pendingBasketBarcodes.isNotEmpty()) {
            newListNameText = ""
            showCreateListDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "App Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.quickScanTitle)
                    }
                },
                actions = {
                    IconButton(onClick = { showManualInputDialog = true }) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = strings.manualBarcodeEntryDesc,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Surface(
                            onClick = { showLanguageMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = if (appLanguage == "ro") "🇷🇴 RO" else "🇬🇧 EN",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🇬🇧 English (UK)") },
                                onClick = {
                                    viewModel.setAppLanguage("en")
                                    showLanguageMenu = false
                                    if (currencySymbol != "£") {
                                        pendingCurrencySwitchPrompt = "£"
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🇷🇴 Română") },
                                onClick = {
                                    viewModel.setAppLanguage("ro")
                                    showLanguageMenu = false
                                    if (currencySymbol != "RON") {
                                        pendingCurrencySwitchPrompt = "RON"
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Pinned Top Search & Scan Bar
            AppSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                placeholder = strings.searchOrScanPlaceholder,
                onBackClick = if (searchQuery.isNotBlank()) {
                    {
                        viewModel.onSearchQueryChanged("")
                        focusManager.clearFocus()
                    }
                } else null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                trailingContent = {
                    Surface(
                        onClick = {
                            viewModel.startScanner(
                                onNeedManualInputFallback = {
                                    showManualInputDialog = true
                                }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = if (searchQuery.isEmpty()) 14.dp else 10.dp,
                                vertical = 10.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = strings.scanBarcodeDesc,
                                modifier = Modifier.size(18.dp)
                            )
                            AnimatedVisibility(visible = searchQuery.isEmpty()) {
                                Row {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.scanButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // Target basket indicator when searching
            AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                val selectedBaskets = allShoppingLists.filter { selectedListIds.contains(it.id) }
                val targetLabel = when {
                    selectedBaskets.isEmpty() -> strings.noBasketSelectedHint
                    selectedBaskets.size == 1 -> "${strings.addingToPrefix} ${selectedBaskets.first().name}"
                    else -> "${strings.addingToPrefix} ${selectedBaskets.first().name} (+${selectedBaskets.size - 1} ${strings.moreSuffix})"
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedBaskets.isEmpty()) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (selectedBaskets.isEmpty()) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = targetLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedBaskets.isEmpty()) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = {
                                viewModel.onSearchQueryChanged("")
                                focusManager.clearFocus()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.basketsBack,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = searchQuery.isNotBlank() && selectedSearchBarcodes.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedSearchBarcodes.size} ${strings.selectedSuffix}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppOutlinedButton(
                                onClick = { selectedSearchBarcodes = emptySet() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(strings.clearAction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            AppButton(
                                onClick = {
                                    viewModel.addPiecesToSelectedBaskets(selectedSearchBarcodes.toList())
                                    selectedSearchBarcodes = emptySet()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    String.format(Locale.US, strings.addCountFmt, selectedSearchBarcodes.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            if (searchQuery.isNotBlank()) {
                // Focused Search Results
                val (matchingPieces, matchingProducts) = searchResults
                if (matchingPieces.isEmpty() && matchingProducts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, strings.noItemsMatchFmt, searchQuery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                AppFilledTonalButton(
                                    onClick = { viewModel.resolveBarcode(searchQuery.trim()) }
                                ) {
                                    Text(String.format(Locale.US, strings.addToCatalogFmt, searchQuery))
                                }
                            }
                        }
                    }
                } else {
                    if (matchingPieces.isNotEmpty()) {
                        item {
                            Text(
                                String.format(Locale.US, strings.partsAndPiecesHeaderFmt, matchingPieces.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(matchingPieces, key = { "piece_${it.barcode}" }) { piece ->
                            val isSelected = selectedSearchBarcodes.contains(piece.barcode)
                            val toggleSelection = {
                                selectedSearchBarcodes = if (isSelected) {
                                    selectedSearchBarcodes - piece.barcode
                                } else {
                                    selectedSearchBarcodes + piece.barcode
                                }
                            }
                            SearchPieceCard(
                                piece = piece,
                                currencySymbol = currencySymbol,
                                isSelected = isSelected,
                                onToggleSelect = toggleSelection,
                                onClick = {
                                    if (selectedSearchBarcodes.isNotEmpty()) {
                                        toggleSelection()
                                    } else {
                                        viewModel.adjustPieceStock(piece.barcode, 1)
                                    }
                                }
                            )
                        }
                    }

                    if (matchingProducts.isNotEmpty()) {
                        item {
                            Text(
                                String.format(Locale.US, strings.assembledProductsHeaderFmt, matchingProducts.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(matchingProducts, key = { "product_${it.productSku}" }) { prod ->
                            SearchProductCard(
                                product = prod,
                                currencySymbol = currencySymbol,
                                onClick = { viewModel.adjustProductStock(prod.productSku, 1) }
                            )
                        }
                    }
                }
            } else {
                // 1. Header & New Basket Button
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.activeBasketsHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            AppButton(
                                onClick = {
                                    newListNameText = ""
                                    showCreateListDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.newBasket, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        AnimatedVisibility(visible = selectedListIds.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedListIds.size} ${strings.selectedSuffix}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppOutlinedButton(
                                            onClick = { viewModel.clearSelectedBaskets() },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.clearAction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        AppButton(
                                            onClick = { viewModel.finishAndSaveSelectedBaskets() },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.saveAction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        FilledTonalIconButton(
                                            onClick = { showConfirmDeleteBatch = true },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = strings.deleteSelectedBasketsDesc,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Shopping Basket Cards List
                if (allShoppingLists.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    strings.noBasketsTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    strings.noBasketsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(allShoppingLists, key = { it.id }) { list ->
                        val itemsInList = allShoppingListItems.filter { it.listId == list.id }
                        ShoppingListCard(
                            list = list,
                            isSelected = selectedListIds.contains(list.id),
                            itemsInList = itemsInList,
                            pieceMap = allPiecesMap,
                            currencySymbol = currencySymbol,
                            defaultBudgetLimit = budgetLimit,
                            onToggleSelect = { viewModel.toggleSelectList(list.id) },
                            onRename = {
                                listToRename = list
                                renameListNameText = list.name
                            },
                            onCopy = {
                                listToCopy = list
                                copyListNameText = "${list.name} (Copy)"
                            },
                            onDelete = { listToDelete = list },
                            onApplyVoucher = {
                                listForVoucherDialog = list
                                showVoucherDialog = true
                            },
                            onSetBudget = {
                                listForBudgetDialog = list
                                budgetInputText = if (list.budgetLimit > 0) String.format(Locale.US, "%.2f", list.budgetLimit) else if (budgetLimit > 0) String.format(Locale.US, "%.2f", budgetLimit) else ""
                            },
                            onOpenMixAndMatch = { listForMixAndMatch = list },
                            onUngroupMixAndMatch = { groupId ->
                                viewModel.clearMixAndMatchGroup(list.id, groupId)
                            },
                            onRemoveItem = { barcode ->
                                viewModel.removeShoppingListItem(list.id, barcode)
                            },
                            onToggleItemChecked = { barcode, isChecked ->
                                viewModel.toggleBasketItemChecked(list.id, barcode, isChecked)
                            },
                            onAdjustItemQuantity = { barcode, delta ->
                                viewModel.adjustBasketItemQuantity(list.id, barcode, delta)
                            }
                        )
                    }
                }
            }
        }
    }

        // Modal Sheet for Scanned / Resolved Item
        ScanResolutionSheet(
        state = scannedItemState,
        currencySymbol = currencySymbol,
        savedOffers = savedOffers,
        onDismiss = viewModel::dismissScannedSheet,
        onSkipLookupToManual = viewModel::skipLookupToManualEntry,
        onAdjustPieceStock = viewModel::adjustPieceStock,
        onAdjustProductStock = viewModel::adjustProductStock,
        onCreatePiece = viewModel::createPiece,
        onCreateProduct = viewModel::createProduct,
        onNavigateToProductBom = onNavigateToProductBom
    )

    // Apply Voucher Dialog
    if (showVoucherDialog) {
        val savedVoucherList = savedOffers.filter { it.offerType == "VOUCHER" }
        ApplyVoucherDialog(
            savedVouchers = savedVoucherList,
            currencySymbol = currencySymbol,
            onDismiss = {
                showVoucherDialog = false
                listForVoucherDialog = null
            },
            onApplyVoucher = { voucher ->
                val targetList = listForVoucherDialog ?: allShoppingLists.firstOrNull()
                if (targetList != null) {
                    viewModel.applyVoucherToShoppingList(targetList.id, voucher)
                }
                showVoucherDialog = false
                listForVoucherDialog = null
            }
        )
    }

    scanAnotherPromptItem?.let { itemName ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissScanAnotherPrompt() },
            title = { Text(strings.itemAddedTitle) },
            text = {
                Text(
                    if (appLanguage == "ro") {
                        "Produsul \"$itemName\" a fost adăugat în coș.\n\nDoriți să scanați alt produs?"
                    } else {
                        "Added \"$itemName\" to your basket.\n\nWould you like to scan another product?"
                    }
                )
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.dismissScanAnotherPrompt()
                        viewModel.startScanner(
                            onNeedManualInputFallback = { showManualInputDialog = true }
                        )
                    }
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanAnother)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissScanAnotherPrompt() }) {
                    Text(strings.done)
                }
            }
        )
    }

    // Create List Dialog
    if (showCreateListDialog) {
        val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
        val dateStr = dateFormat.format(java.util.Date(selectedBasketDateMillis))

        val generatedName = remember(newListNameText, includeDateInName, selectedBasketDateMillis, strings.defaultBasketName) {
            val base = newListNameText.trim().ifEmpty { strings.defaultBasketName }
            if (includeDateInName) "$base - $dateStr" else base
        }

        AlertDialog(
            onDismissRequest = {
                showCreateListDialog = false
                viewModel.clearPendingBasketItem()
            },
            title = { Text(strings.createBasketTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppOutlinedTextField(
                        value = newListNameText,
                        onValueChange = { newListNameText = it },
                        label = { Text(strings.basketNameLabel) },
                        placeholder = { Text(strings.basketNamePlaceholder) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { includeDateInName = !includeDateInName }
                        ) {
                            Checkbox(
                                checked = includeDateInName,
                                onCheckedChange = { includeDateInName = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.appendDate, style = MaterialTheme.typography.bodyMedium)
                        }

                        AssistChip(
                            onClick = { showDatePickerModal = true },
                            label = { Text("📅 $dateStr") }
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.previewPrefix,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                generatedName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val hadPending = pendingBasketItem != null || pendingBasketBarcodes.isNotEmpty()
                        viewModel.createShoppingList(generatedName)
                        showCreateListDialog = false
                        if (hadPending && searchQuery.isNotBlank()) {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Text(strings.createBasketConfirm)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateListDialog = false
                        viewModel.clearPendingBasketItem()
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedBasketDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedBasketDateMillis = it
                        }
                        showDatePickerModal = false
                    }
                ) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerModal = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Rename List Dialog
    listToRename?.let { list ->
        AlertDialog(
            onDismissRequest = { listToRename = null },
            title = { Text(strings.renameBasketTitle) },
            text = {
                AppOutlinedTextField(
                    value = renameListNameText,
                    onValueChange = { renameListNameText = it },
                    label = { Text(strings.basketNameLabel) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        if (renameListNameText.isNotBlank()) {
                            viewModel.renameShoppingList(list.id, renameListNameText.trim())
                            listToRename = null
                        }
                    },
                    enabled = renameListNameText.isNotBlank()
                ) {
                    Text(strings.rename)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToRename = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Copy / Duplicate List Dialog
    listToCopy?.let { list ->
        AlertDialog(
            onDismissRequest = { listToCopy = null },
            title = { Text(strings.duplicateBasketTitle) },
            text = {
                AppOutlinedTextField(
                    value = copyListNameText,
                    onValueChange = { copyListNameText = it },
                    label = { Text(strings.newBasketNameLabel) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        if (copyListNameText.isNotBlank()) {
                            viewModel.copyShoppingList(list.id, copyListNameText.trim())
                            listToCopy = null
                        }
                    },
                    enabled = copyListNameText.isNotBlank()
                ) {
                    Text(strings.duplicate)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToCopy = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete List Dialog
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text(strings.deleteBasketTitle) },
            text = { Text(String.format(Locale.US, strings.deleteBasketConfirmFmt, list.name)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deleteShoppingList(list)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Set Basket Budget Dialog
    listForBudgetDialog?.let { list ->
        AlertDialog(
            onDismissRequest = { listForBudgetDialog = null },
            title = { Text(String.format(Locale.US, strings.setTargetBudgetTitleFmt, list.name)) },
            text = {
                Column {
                    Text(
                        String.format(Locale.US, strings.setTargetBudgetBodyFmt, list.name),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text(String.format(Locale.US, strings.budgetLimitLabelFmt, currencySymbol)) },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val parsed = budgetInputText.toDoubleOrNull() ?: 0.0
                        val limit = if (parsed > 0.0) parsed else -1.0
                        viewModel.setBasketBudgetLimit(list.id, limit)
                        listForBudgetDialog = null
                    }
                ) {
                    Text(strings.saveBudget)
                }
            },
            dismissButton = {
                Row {
                    if (list.budgetLimit > 0 || (list.budgetLimit == 0.0 && budgetLimit > 0)) {
                        TextButton(
                            onClick = {
                                viewModel.setBasketBudgetLimit(list.id, -1.0)
                                listForBudgetDialog = null
                            }
                        ) {
                            Text(strings.clearAction)
                        }
                    }
                    TextButton(onClick = { listForBudgetDialog = null }) {
                        Text(strings.cancel)
                    }
                }
            }
        )
    }

    // Delete Batch Confirmation Dialog
    if (showConfirmDeleteBatch) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteBatch = false },
            title = { Text(strings.deleteSelectedBasketsTitle) },
            text = { Text(String.format(Locale.US, strings.deleteSelectedBasketsConfirmFmt, selectedListIds.size)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deleteSelectedShoppingLists()
                        showConfirmDeleteBatch = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.deleteSelected)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteBatch = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // App Update Available Dialog
    appUpdateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateBanner,
            title = { Text(String.format(Locale.US, strings.updateAvailableTitleFmt, update.latestTag)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        String.format(Locale.US, strings.updateAvailableBodyFmt, update.latestTag),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (update.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            update.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppOutlinedButton(
                        onClick = viewModel::dismissUpdateBanner,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = strings.later,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    AppButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.downloadUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            viewModel.dismissUpdateBanner()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = strings.downloadUpdate,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        )
    }

    // Manual Barcode Input Dialog (Emulator fallback)
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualInputDialog = false
                manualBarcodeText = ""
            },
            title = { Text(strings.enterBarcodeTitle) },
            text = {
                Column {
                    Text(
                        strings.enterBarcodeBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedTextField(
                        value = manualBarcodeText,
                        onValueChange = { manualBarcodeText = it },
                        placeholder = { Text(strings.enterBarcodePlaceholder) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val code = manualBarcodeText.trim()
                        if (code.isNotEmpty()) {
                            showManualInputDialog = false
                            manualBarcodeText = ""
                            viewModel.resolveBarcode(code)
                        }
                    },
                    enabled = manualBarcodeText.isNotBlank()
                ) {
                    Text(strings.resolve)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualInputDialog = false
                    manualBarcodeText = ""
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    listForMixAndMatch?.let { targetList ->
        MixAndMatchOfferDialog(
            itemsInList = allShoppingListItems.filter { it.listId == targetList.id },
            pieceMap = allPiecesMap,
            savedOffers = savedOffers,
            currencySymbol = currencySymbol,
            onDismiss = { listForMixAndMatch = null },
            onApply = { barcodes, offerType, buyQty, payQty, fixedPrice, percentOff ->
                viewModel.applyMixAndMatchOffer(
                    listId = targetList.id,
                    barcodes = barcodes,
                    offerType = offerType,
                    buyQty = buyQty,
                    payQty = payQty,
                    fixedPrice = fixedPrice,
                    percentOff = percentOff,
                )
                listForMixAndMatch = null
            }
        )
    }

    pendingCurrencySwitchPrompt?.let { targetCurrency ->
        val targetLangName = if (targetCurrency == "RON") strings.languageNameRomanian else strings.languageNameEnglish
        AlertDialog(
            onDismissRequest = { pendingCurrencySwitchPrompt = null },
            title = { Text(strings.switchCurrencyTitle) },
            text = {
                Text(String.format(Locale.US, strings.switchCurrencyBodyFmt, targetLangName, targetCurrency))
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.setCurrencySymbol(targetCurrency)
                        pendingCurrencySwitchPrompt = null
                    }
                ) {
                    Text(String.format(Locale.US, strings.switchCurrencyConfirmFmt, targetCurrency))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrencySwitchPrompt = null }) {
                    Text(String.format(Locale.US, strings.keepCurrencyFmt, currencySymbol))
                }
            }
        )
    }
}
}

@Composable
private fun SearchPieceCard(
    piece: PieceEntity,
    currencySymbol: String = "$",
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(piece.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val priceInfo = if (piece.unitPrice > 0) String.format(Locale.US, " • %s%.2f${strings.eachSuffix}", currencySymbol, piece.unitPrice) else ""
                Text("${strings.codePrefix} ${piece.barcode}$priceInfo", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    strings.addPlusOne,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (piece.unitPrice > 0) {
                    Text(
                        String.format(Locale.US, "%s%.2f", currencySymbol, piece.calculatedTotal),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchProductCard(
    product: ProductEntity,
    currencySymbol: String = "$",
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val priceInfo = if (product.unitPrice > 0) String.format(Locale.US, " • %s%.2f${strings.eachSuffix}", currencySymbol, product.unitPrice) else ""
                Text("SKU: ${product.productSku}$priceInfo", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    strings.addPlusOne,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (product.unitPrice > 0) {
                    Text(
                        String.format(Locale.US, "%s%.2f", currencySymbol, product.stockQuantity * product.unitPrice),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
