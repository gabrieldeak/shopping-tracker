package com.inventory.barcodetracker.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.model.BomItem
import com.inventory.barcodetracker.ui.theme.StockLowRed
import com.inventory.barcodetracker.ui.theme.StockOkGreen
import com.inventory.barcodetracker.ui.theme.StockOkGreenContainer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val product by viewModel.product.collectAsStateWithLifecycle()
    val bomItems by viewModel.bomItems.collectAsStateWithLifecycle()
    val availablePieces by viewModel.allAvailablePieces.collectAsStateWithLifecycle()
    val buildCapacity by viewModel.buildCapacity.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var showAddBomDialog by remember { mutableStateOf(false) }
    var showBuildDialog by remember { mutableStateOf(false) }
    var buildQuantity by remember { mutableIntStateOf(1) }
    var itemToRemove by remember { mutableStateOf<BomItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBomDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Component")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Calculate components total unit cost
            val totalBomUnitCost = bomItems.sumOf { it.quantityRequired * it.unitPrice }

            // Product info & Build Capacity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product?.name ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SKU: ${product?.productSku ?: ""} • Built Stock: ${product?.stockQuantity ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if ((product?.unitPrice ?: 0.0) > 0) {
                                Text(
                                    text = String.format(Locale.US, "Price: %s%.2f", currencySymbol, product?.unitPrice ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (totalBomUnitCost > 0) {
                                Text(
                                    text = String.format(Locale.US, "Component Cost: %s%.2f", currencySymbol, totalBomUnitCost),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Build Capacity",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (buildCapacity > 0) StockOkGreen else StockLowRed
                            )
                            Text(
                                text = "$buildCapacity units",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (buildCapacity > 0) StockOkGreen else StockLowRed
                            )
                            Text(
                                text = if (bomItems.isEmpty()) "Add parts to calculate capacity" else "Based on current piece stocks",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = {
                                buildQuantity = 1
                                showBuildDialog = true
                            },
                            enabled = buildCapacity > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deduct Stock for Build")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bundle Components (${bomItems.size} parts)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (bomItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No components linked to this product yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showAddBomDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Component")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(bomItems, key = { it.barcode }) { item ->
                        val singleCapacity = if (item.quantityRequired > 0) item.stockQuantity / item.quantityRequired else 0
                        val isBottleneck = singleCapacity == buildCapacity && buildCapacity >= 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBottleneck && buildCapacity == 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.pieceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    val priceText = if (item.unitPrice > 0) String.format(Locale.US, " • %s%.2f/ea", currencySymbol, item.unitPrice) else ""
                                    Text("Barcode: ${item.barcode} • Loc: ${item.location}$priceText", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Req: ${item.quantityRequired} / unit", style = MaterialTheme.typography.labelMedium)
                                        Text("In stock: ${item.stockQuantity}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Supports: $singleCapacity",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (singleCapacity == 0) StockLowRed else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { itemToRemove = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove component", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Component Dialog
    if (showAddBomDialog) {
        val alreadyLinked = bomItems.map { it.barcode }.toSet()
        AddBomItemDialog(
            availablePieces = availablePieces,
            alreadyLinkedBarcodes = alreadyLinked,
            onDismiss = { showAddBomDialog = false },
            onConfirm = { barcode, qty ->
                viewModel.addPieceToBom(barcode, qty)
                showAddBomDialog = false
            }
        )
    }

    // Confirm Remove Dialog
    itemToRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("Remove Component") },
            text = { Text("Remove \"${item.pieceName}\" from the recipe components for this product?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removePieceFromBom(item.barcode)
                        itemToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Build Execution Dialog
    if (showBuildDialog) {
        AlertDialog(
            onDismissRequest = { showBuildDialog = false },
            title = { Text("Assemble Product & Deduct Stock") },
            text = {
                Column {
                    Text(
                        "Specify how many units of \"${product?.name}\" to assemble. Part inventory will be decremented accordingly and product stock will increase.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { if (buildQuantity > 1) buildQuantity-- },
                            enabled = buildQuantity > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1")
                        }

                        Text(
                            text = "$buildQuantity",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        FilledIconButton(
                            onClick = { if (buildQuantity < buildCapacity) buildQuantity++ },
                            enabled = buildQuantity < buildCapacity
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maximum build capacity right now: $buildCapacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeBuild(buildQuantity)
                        showBuildDialog = false
                    }
                ) {
                    Text("Confirm Build")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuildDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
