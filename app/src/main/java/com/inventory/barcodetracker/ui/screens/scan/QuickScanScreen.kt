package com.inventory.barcodetracker.ui.screens.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.R
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
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
    val basketSummary by viewModel.basketSummary.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimit.collectAsStateWithLifecycle()
    val savedOffers by viewModel.savedOffers.collectAsStateWithLifecycle()
    val appliedCartVouchers by viewModel.appliedCartVouchers.collectAsStateWithLifecycle()

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showFinishSaveDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var showVoucherDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }
    var budgetInputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Shopping Basket & Budget Tracker Summary Card
            val (totalBasketValue, totalItemCount) = basketSummary
            val isOverBudget = budgetLimit > 0 && totalBasketValue >= budgetLimit
            val budgetProgress = if (budgetLimit > 0) (totalBasketValue / budgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOverBudget) "Shopping Basket (Budget Reached!)" else "Shopping Basket Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currencySymbol, totalBasketValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "$totalItemCount items in cart",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            TextButton(
                                onClick = {
                                    budgetInputText = if (budgetLimit > 0) String.format(Locale.US, "%.2f", budgetLimit) else ""
                                    showBudgetDialog = true
                                }
                            ) {
                                val budgetLabel = if (budgetLimit > 0) String.format(Locale.US, "Limit: %s%.2f", currencySymbol, budgetLimit) else "Set Budget Limit"
                                Text(
                                    text = budgetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (budgetLimit > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f spent", currencySymbol, totalBasketValue),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val remaining = budgetLimit - totalBasketValue
                            Text(
                                text = if (remaining >= 0) String.format(Locale.US, "%s%.2f remaining", currencySymbol, remaining)
                                else String.format(Locale.US, "Exceeded by %s%.2f", currencySymbol, -remaining),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Applied Basket Vouchers Row
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appliedCartVouchers.isEmpty()) {
                            TextButton(onClick = { showVoucherDialog = true }) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Apply Voucher / Coupon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                appliedCartVouchers.forEach { voucher ->
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.removeCartVoucher(voucher) },
                                        label = { Text("🎟️ ${voucher.title} (-$currencySymbol${String.format(Locale.US, "%.2f", voucher.fixedPrice)})") },
                                        trailingIcon = { Icon(Icons.Default.Clear, contentDescription = "Remove Voucher", modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                            IconButton(onClick = { showVoucherDialog = true }) {
                                Icon(Icons.Default.LocalOffer, contentDescription = "Add Voucher", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (totalItemCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showConfirmClearDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Basket")
                            }

                            Button(
                                onClick = { showFinishSaveDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Finish & Save")
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search by barcode, name or location...") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            val (matchingPieces, matchingProducts) = searchResults

            if (searchQuery.isNotBlank()) {
                if (matchingPieces.isEmpty() && matchingProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No items match \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = { viewModel.resolveBarcode(searchQuery.trim()) }
                            ) {
                                Text("Add \"$searchQuery\" to Inventory")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (matchingPieces.isNotEmpty()) {
                            item {
                                Text(
                                    "Parts & Pieces (${matchingPieces.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(matchingPieces, key = { it.barcode }) { piece ->
                                SearchPieceCard(
                                    piece = piece,
                                    currencySymbol = currencySymbol,
                                    onClick = {
                                        viewModel.resolveBarcode(piece.barcode)
                                    }
                                )
                            }
                        }

                        if (matchingProducts.isNotEmpty()) {
                            item {
                                Text(
                                    "Assembled Products (${matchingProducts.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(matchingProducts, key = { it.productSku }) { prod ->
                                SearchProductCard(
                                    product = prod,
                                    currencySymbol = currencySymbol,
                                    onClick = {
                                        viewModel.resolveBarcode(prod.productSku)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty search prompt & Quick Instructions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Scan Any Barcode or QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap the button below to launch Google Code Scanner, or use the keyboard icon above to enter codes manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { showManualInputDialog = true }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Barcode Manually")
                    }
                }
            }
        }
    }

    // Finish & Save Basket Dialog
    if (showFinishSaveDialog) {
        val (totalBasketValue, totalItemCount) = basketSummary
        AlertDialog(
            onDismissRequest = { showFinishSaveDialog = false },
            title = { Text("Finish & Save Shopping Basket") },
            text = {
                Text(
                    String.format(
                        Locale.US,
                        "Save this shopping basket (%s%.2f, %d items) to History and reset your cart? (Scanned products will remain saved in 'My Items').",
                        currencySymbol,
                        totalBasketValue,
                        totalItemCount
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishAndSaveBasket(totalBasketValue, totalItemCount)
                        showFinishSaveDialog = false
                    }
                ) {
                    Text("Save & Reset Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Basket Dialog
    if (showConfirmClearDialog) {
        val (_, totalItemCount) = basketSummary
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Clear Basket") },
            text = {
                Text("Clear all $totalItemCount items from your current shopping cart? (Scanned products will remain saved in 'My Items').")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearBasketOnly()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Budget Limit Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Target Shopping Budget") },
            text = {
                Column {
                    Text(
                        "Enter a target limit for your shopping basket. The screen will notify you when your total scanned items reach or exceed this limit.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text("Budget Limit ($currencySymbol)") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetInputText.toDoubleOrNull() ?: 0.0
                        viewModel.setBudgetLimit(limit)
                        showBudgetDialog = false
                    }
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                Row {
                    if (budgetLimit > 0) {
                        TextButton(
                            onClick = {
                                viewModel.setBudgetLimit(0.0)
                                showBudgetDialog = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

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
                    "${piece.stockQuantity} in cart",
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
                    if (piece.savings > 0) {
                        Text(
                            String.format(Locale.US, "Saved %s%.2f", currencySymbol, piece.savings),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
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
                    "${product.stockQuantity} units",
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
