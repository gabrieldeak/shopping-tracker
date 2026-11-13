package com.inventory.barcodetracker.ui.screens.offers

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.entity.OfferEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    viewModel: OffersViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val offersList by viewModel.offersList.collectAsStateWithLifecycle()
    val allPieces by viewModel.allPieces.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val selectedOfferIds by viewModel.selectedOfferIds.collectAsStateWithLifecycle()

    var showAddOfferDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<OfferEntity?>(null) }
    var offerToDelete by remember { mutableStateOf<OfferEntity?>(null) }
    var offerToApplyToItem by remember { mutableStateOf<OfferEntity?>(null) }
    var showConfirmDeleteBatch by remember { mutableStateOf(false) }

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
                                Icons.Default.LocalOffer,
                                contentDescription = "Offers Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Store Offers & Deals")
                    }
                },
                actions = {
                    if (selectedOfferIds.isNotEmpty()) {
                        Button(
                            onClick = { showConfirmDeleteBatch = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Selected (${selectedOfferIds.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddOfferDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Offer") },
                text = { Text("Create Offer", fontWeight = FontWeight.Bold) },
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
            // Search Input & Select All Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search offers by name, store, deal type...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                if (offersList.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.selectAllOffers(offersList.map { it.id }) }
                    ) {
                        Text(if (selectedOfferIds.size == offersList.size) "Deselect All" else "Select All")
                    }
                }
            }

            // Summary Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saved Store Promotions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${offersList.size} Active Deals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    val storeCount = offersList.map { it.storeName.ifBlank { "General" } }.distinct().size
                    Text(
                        text = "$storeCount Stores",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (offersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Sell,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No offers match \"$searchQuery\"" else "No store offers saved yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ Create Offer' below to set up 3 for 2, 2 for $currencySymbol 5, or reduced price deals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(offersList, key = { it.id }) { offer ->
                        OfferCardItem(
                            offer = offer,
                            isSelected = selectedOfferIds.contains(offer.id),
                            currencySymbol = currencySymbol,
                            onToggleSelect = { viewModel.toggleSelectOffer(offer.id) },
                            onApplyToItem = { offerToApplyToItem = offer },
                            onEdit = { editingOffer = offer },
                            onDelete = { offerToDelete = offer }
                        )
                    }
                }
            }
        }
    }

    // Add Offer Dialog
    if (showAddOfferDialog) {
        OfferDialog(
            initialOffer = null,
            currencySymbol = currencySymbol,
            onDismiss = { showAddOfferDialog = false },
            onSave = { newOffer ->
                viewModel.saveOffer(newOffer)
                showAddOfferDialog = false
            }
        )
    }

    // Edit Offer Dialog
    editingOffer?.let { offer ->
        OfferDialog(
            initialOffer = offer,
            currencySymbol = currencySymbol,
            onDismiss = { editingOffer = null },
            onSave = { updated ->
                viewModel.saveOffer(updated)
                editingOffer = null
            }
        )
    }

    // Delete Single Offer Confirmation Dialog
    offerToDelete?.let { offer ->
        AlertDialog(
            onDismissRequest = { offerToDelete = null },
            title = { Text("Delete Offer") },
            text = { Text("Are you sure you want to delete offer \"${offer.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOffer(offer)
                        offerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { offerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Batch Confirmation Dialog
    if (showConfirmDeleteBatch) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteBatch = false },
            title = { Text("Delete Selected Offers") },
            text = { Text("Are you sure you want to delete ${selectedOfferIds.size} selected store offer(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedOffers()
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

    // Apply Offer to Item Dialog
    offerToApplyToItem?.let { offer ->
        ApplyOfferToItemDialog(
            offer = offer,
            pieces = allPieces,
            currencySymbol = currencySymbol,
            onDismiss = { offerToApplyToItem = null },
            onApplyToItem = { barcode ->
                viewModel.applyOfferToPiece(barcode, offer)
                offerToApplyToItem = null
            }
        )
    }
}

@Composable
private fun OfferCardItem(
    offer: OfferEntity,
    isSelected: Boolean,
    currencySymbol: String = "$",
    onToggleSelect: () -> Unit,
    onApplyToItem: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏷️ ${offer.displayBadge}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = offer.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (offer.storeName.isNotBlank()) {
                            Text(
                                text = "Store: ${offer.storeName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (offer.notes.isNotBlank()) {
                            Text(
                                text = offer.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onApplyToItem,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply this Offer to Product...")
            }
        }
    }
}
