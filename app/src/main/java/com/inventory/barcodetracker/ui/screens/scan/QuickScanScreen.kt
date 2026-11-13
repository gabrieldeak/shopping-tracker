package com.inventory.barcodetracker.ui.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.inventory.barcodetracker.ui.components.ShoppingListCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScanScreen(
    viewModel: ScanViewModel,
    onNavigateToProductBom: (sku: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val scannedItemState by viewModel.scannedItemState.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val savedOffers by viewModel.savedOffers.collectAsStateWithLifecycle()

    val allShoppingLists by viewModel.allShoppingLists.collectAsStateWithLifecycle()
    val allShoppingListItems by viewModel.allShoppingListItems.collectAsStateWithLifecycle()
    val allPiecesMap by viewModel.allPiecesMap.collectAsStateWithLifecycle()
    val selectedListIds by viewModel.selectedListIds.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showVoucherDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteBatch by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listToCopy by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var listToDelete by remember { mutableStateOf<ShoppingListEntity?>(null) }

    var manualBarcodeText by remember { mutableStateOf("") }
    var newListNameText by remember { mutableStateOf("") }
    var renameListNameText by remember { mutableStateOf("") }
    var copyListNameText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomScrollPadding = if (imeBottomPadding > 0.dp) imeBottomPadding + 100.dp else 120.dp

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
                        Text("Barcode Quick Scan")
                    }
                },
                actions = {
                    IconButton(onClick = { showManualInputDialog = true }) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Manual Barcode Entry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startScanner(
                        onNeedManualInputFallback = {
                            showManualInputDialog = true
                        }
                    )
                },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
                text = { Text("Scan Barcode", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = bottomScrollPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header & New Basket Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shopping Baskets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Check boxes below to select scan targets. Swipe left/right to copy or delete.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedListIds.isNotEmpty()) {
                            Button(
                                onClick = { showConfirmDeleteBatch = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete (${selectedListIds.size})", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                newListNameText = ""
                                showCreateListDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Basket", fontWeight = FontWeight.Bold)
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
                                "No shopping baskets created yet.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap '+ New Basket' above to create custom lists (e.g. Main Basket, Tesco Weekly). Checked baskets will automatically receive scanned or added items!",
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
                        onFinishAndSave = { viewModel.finishAndSaveBasketForList(list) },
                        onClearBasket = { viewModel.clearListBasket(list) },
                        onApplyVoucher = { showVoucherDialog = true }
                    )
                }
            }

            // 3. Search Bar
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search items by barcode, name or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Search Results (if user typed search query)
            if (searchQuery.isNotBlank()) {
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
                                    text = "No items match \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.resolveBarcode(searchQuery.trim()) }
                                ) {
                                    Text("Add \"$searchQuery\" to Catalog")
                                }
                            }
                        }
                    }
                } else {
                    if (matchingPieces.isNotEmpty()) {
                        item {
                            Text(
                                "Parts & Pieces (${matchingPieces.size}) — Tap item to add +1 to selected baskets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(matchingPieces, key = { "piece_${it.barcode}" }) { piece ->
                            SearchPieceCard(
                                piece = piece,
                                currencySymbol = currencySymbol,
                                onClick = { viewModel.adjustPieceStock(piece.barcode, 1) }
                            )
                        }
                    }

                    if (matchingProducts.isNotEmpty()) {
                        item {
                            Text(
                                "Assembled Products (${matchingProducts.size}) — Tap product to add +1 to selected baskets",
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
                // Scan Instructions Prompt
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📷 Ready to Scan Barcodes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Scan Barcode' below to scan physical items into your checked shopping baskets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            onDismiss = { showVoucherDialog = false },
            onApplyVoucher = { voucher ->
                viewModel.applyCartVoucher(voucher)
                showVoucherDialog = false
            }
        )
    }

    // Create List Dialog
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Create New Shopping Basket") },
            text = {
                OutlinedTextField(
                    value = newListNameText,
                    onValueChange = { newListNameText = it },
                    label = { Text("Basket Name") },
                    placeholder = { Text("e.g. Tesco Weekly, BBQ Party") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListNameText.isNotBlank()) {
                            viewModel.createShoppingList(newListNameText.trim())
                            showCreateListDialog = false
                        }
                    },
                    enabled = newListNameText.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename List Dialog
    listToRename?.let { list ->
        AlertDialog(
            onDismissRequest = { listToRename = null },
            title = { Text("Rename Shopping Basket") },
            text = {
                OutlinedTextField(
                    value = renameListNameText,
                    onValueChange = { renameListNameText = it },
                    label = { Text("Basket Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameListNameText.isNotBlank()) {
                            viewModel.renameShoppingList(list.id, renameListNameText.trim())
                            listToRename = null
                        }
                    },
                    enabled = renameListNameText.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Copy / Duplicate List Dialog
    listToCopy?.let { list ->
        AlertDialog(
            onDismissRequest = { listToCopy = null },
            title = { Text("Duplicate Shopping Basket") },
            text = {
                OutlinedTextField(
                    value = copyListNameText,
                    onValueChange = { copyListNameText = it },
                    label = { Text("New Basket Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (copyListNameText.isNotBlank()) {
                            viewModel.copyShoppingList(list.id, copyListNameText.trim())
                            listToCopy = null
                        }
                    },
                    enabled = copyListNameText.isNotBlank()
                ) {
                    Text("Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToCopy = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete List Dialog
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Delete Shopping Basket") },
            text = { Text("Are you sure you want to delete \"${list.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShoppingList(list)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Batch Confirmation Dialog
    if (showConfirmDeleteBatch) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteBatch = false },
            title = { Text("Delete Selected Baskets") },
            text = { Text("Are you sure you want to delete ${selectedListIds.size} selected shopping basket(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedShoppingLists()
                        showConfirmDeleteBatch = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteBatch = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // App Update Available Dialog
    appUpdateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateBanner,
            title = { Text("🚀 Update Available! (${update.latestTag})") },
            text = {
                Column {
                    Text(
                        "A new version of Shopping Tracker (${update.latestTag}) is available on GitHub!",
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
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.downloadUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        viewModel.dismissUpdateBanner()
                    }
                ) {
                    Text("Download Update")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdateBanner) {
                    Text("Later")
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
            title = { Text("Enter Barcode / SKU") },
            text = {
                Column {
                    Text(
                        "Input a barcode or SKU to look up or register new inventory:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualBarcodeText,
                        onValueChange = { manualBarcodeText = it },
                        placeholder = { Text("e.g. PART-001 or 12345678") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
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
                    Text("Resolve")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualInputDialog = false
                    manualBarcodeText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SearchPieceCard(
    piece: PieceEntity,
    currencySymbol: String = "$",
    onClick: () -> Unit,
) {
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
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(piece.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val priceInfo = if (piece.unitPrice > 0) String.format(Locale.US, " • %s%.2f/ea", currencySymbol, piece.unitPrice) else ""
                Text("Code: ${piece.barcode}$priceInfo", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+ Add +1",
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
                val priceInfo = if (product.unitPrice > 0) String.format(Locale.US, " • %s%.2f/ea", currencySymbol, product.unitPrice) else ""
                Text("SKU: ${product.productSku}$priceInfo", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+ Add +1",
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
