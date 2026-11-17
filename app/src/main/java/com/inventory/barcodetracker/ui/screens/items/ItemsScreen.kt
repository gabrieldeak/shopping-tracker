package com.inventory.barcodetracker.ui.screens.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.ui.components.AddItemsToBasketDialog
import com.inventory.barcodetracker.ui.components.AppSearchBar
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val strings = LocalAppStrings.current
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val lowStockOnly by viewModel.lowStockOnly.collectAsStateWithLifecycle()
    val piecesList by viewModel.piecesList.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val selectedBarcodes by viewModel.selectedBarcodes.collectAsStateWithLifecycle()
    val allShoppingLists by viewModel.allShoppingLists.collectAsStateWithLifecycle()
    val savedOffers by viewModel.savedOffers.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPiece by remember { mutableStateOf<PieceEntity?>(null) }
    var pieceToDelete by remember { mutableStateOf<PieceEntity?>(null) }
    var showConfirmDeleteBatch by remember { mutableStateOf(false) }
    var showAddToBasketDialog by remember { mutableStateOf(false) }

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
                                contentDescription = strings.appLogoDesc,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.itemsTitle)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = strings.addItem) },
                text = { Text(strings.addItem, fontWeight = FontWeight.Bold) },
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
            // 1. Full-Width Search Input
            AppSearchBar(
                query = searchFilter,
                onQueryChange = viewModel::onSearchFilterChanged,
                placeholder = strings.searchCatalogPlaceholder,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // 2. Control Action Row
            if (piecesList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.selectAllPieces(piecesList.map { it.barcode }) }
                    ) {
                        Text(
                            text = if (selectedBarcodes.size == piecesList.size) String.format(Locale.US, strings.deselectAllFmt, piecesList.size) else String.format(Locale.US, strings.selectAllFmt, piecesList.size),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    if (selectedBarcodes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppButton(
                                onClick = { showAddToBasketDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format(Locale.US, strings.addCountFmt, selectedBarcodes.size), fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            AppButton(
                                onClick = { showConfirmDeleteBatch = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format(Locale.US, strings.deleteCountFmt, selectedBarcodes.size), fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Master Catalog Summary Banner
            val catalogCount = piecesList.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.savedItemsCatalog,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, strings.savedProductsCountFmt, catalogCount),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.itemsRoutingHint,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (piecesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lowStockOnly) strings.noPartsBelowThreshold else strings.noItemsInCatalog,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(piecesList, key = { it.barcode }) { piece ->
                        PieceItemCard(
                            piece = piece,
                            isSelected = selectedBarcodes.contains(piece.barcode),
                            currencySymbol = currencySymbol,
                            onToggleSelect = { viewModel.toggleSelectPiece(piece.barcode) },
                            onAdjustStock = { delta -> viewModel.adjustStock(piece.barcode, delta) },
                            onEdit = { editingPiece = piece },
                            onDelete = { pieceToDelete = piece }
                        )
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        ItemDialog(
            initialPiece = null,
            currencySymbol = currencySymbol,
            savedOffers = savedOffers,
            onDismiss = { showAddDialog = false },
            onSave = { newPiece, updateBasketsPrice ->
                viewModel.savePiece(newPiece, updateBasketsPrice)
                showAddDialog = false
            }
        )
    }

    // Edit Dialog
    editingPiece?.let { piece ->
        ItemDialog(
            initialPiece = piece,
            currencySymbol = currencySymbol,
            savedOffers = savedOffers,
            onDismiss = { editingPiece = null },
            onSave = { updated, updateBasketsPrice ->
                viewModel.savePiece(updated, updateBasketsPrice)
                editingPiece = null
            }
        )
    }

    // Delete Single Confirmation Dialog
    pieceToDelete?.let { piece ->
        AlertDialog(
            onDismissRequest = { pieceToDelete = null },
            title = { Text(strings.deleteItemTitle) },
            text = { Text(String.format(Locale.US, strings.deleteItemConfirmFmt, piece.name)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deletePiece(piece)
                        pieceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pieceToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Batch Confirmation Dialog
    if (showConfirmDeleteBatch) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteBatch = false },
            title = { Text(strings.deleteSelectedItemsTitle) },
            text = { Text(String.format(Locale.US, strings.deleteSelectedItemsConfirmFmt, selectedBarcodes.size)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deleteSelectedPieces()
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

    // Add Items to Basket(s) Dialog
    if (showAddToBasketDialog) {
        AddItemsToBasketDialog(
            shoppingLists = allShoppingLists,
            selectedCount = selectedBarcodes.size,
            onDismiss = { showAddToBasketDialog = false },
            onConfirmAdd = { targetListIds ->
                viewModel.addSelectedPiecesToShoppingLists(targetListIds)
                showAddToBasketDialog = false
            }
        )
    }
}

@Composable
private fun PieceItemCard(
    piece: PieceEntity,
    isSelected: Boolean,
    currencySymbol: String = "$",
    onToggleSelect: () -> Unit,
    onAdjustStock: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Compact Primary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = piece.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (piece.unitPrice > 0) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currencySymbol, piece.unitPrice),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (piece.offerType != "NONE" && piece.offerDescription.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🏷️ ${piece.getOfferDescription(currencySymbol)}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) LocalAppStrings.current.collapse else LocalAppStrings.current.expand,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Secondary Details & Controls
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${LocalAppStrings.current.barcodeLabel} ${piece.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (piece.location.isNotBlank() && piece.location != "Default Bin") {
                        Text(
                            text = "${LocalAppStrings.current.categoryLabel} ${piece.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (piece.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = piece.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = LocalAppStrings.current.edit,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = LocalAppStrings.current.delete,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
