package com.inventory.barcodetracker.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppFilledTonalButton
import com.inventory.barcodetracker.ui.components.AppSearchBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val strings = LocalAppStrings.current
    val trips by viewModel.shoppingTrips.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTripIds by viewModel.selectedTripIds.collectAsStateWithLifecycle()

    var tripToDelete by remember { mutableStateOf<ShoppingTripEntity?>(null) }
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
                                Icons.Default.ShoppingCart,
                                contentDescription = strings.appLogoDesc,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.historyTitle)
                    }
                },
                actions = {
                    if (selectedTripIds.isNotEmpty()) {
                        AppButton(
                            onClick = { showConfirmDeleteBatch = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${strings.deleteSelected} (${selectedTripIds.size})", fontWeight = FontWeight.Bold)
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
                .padding(horizontal = 16.dp)
        ) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                placeholder = strings.searchHistoryPlaceholder,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            if (trips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.selectAllTrips(trips.map { it.id }) }
                    ) {
                        Text(
                            text = if (selectedTripIds.size == trips.size) String.format(Locale.US, strings.deselectAllFmt, trips.size) else String.format(Locale.US, strings.selectAllFmt, trips.size),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (trips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.noSavedBasketsYet,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.noSavedBasketsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripItemCard(
                            trip = trip,
                            isSelected = selectedTripIds.contains(trip.id),
                            onToggleSelect = { viewModel.toggleSelectTrip(trip.id) },
                            onDelete = { tripToDelete = trip },
                            onReload = { viewModel.reloadTripIntoCart(trip) }
                        )
                    }
                }
            }
        }
    }

    // Delete Single Confirmation Dialog
    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text(strings.deleteSavedBasketTitle) },
            text = { Text(String.format(Locale.US, strings.deleteSavedBasketConfirmFmt, formatDate(trip.tripDate))) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deleteTrip(trip)
                        tripToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Batch Confirmation Dialog
    if (showConfirmDeleteBatch) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteBatch = false },
            title = { Text(strings.deleteSelectedReceiptsTitle) },
            text = { Text(String.format(Locale.US, strings.deleteSelectedReceiptsConfirmFmt, selectedTripIds.size)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.deleteSelectedTrips()
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
}

@Composable
private fun TripItemCard(
    trip: ShoppingTripEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onReload: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var isExpanded by remember { mutableStateOf(false) }

    val firstLine = trip.itemsSummary.lineSequence().firstOrNull()?.trim() ?: ""
    val basketName = if (firstLine.startsWith("Basket:") || firstLine.startsWith("List:")) {
        firstLine.substringAfter(":").trim()
    } else {
        strings.defaultHistoryBasketName
    }

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
                        text = "🛒 $basketName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, strings.historyDateItemsFmt, formatDate(trip.tripDate), trip.itemCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = String.format(Locale.US, "%s%.2f", trip.currencySymbol, trip.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) strings.collapseItemsDesc else strings.expandItemsDesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Itemized List & Actions
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AppButton(
                            onClick = onReload,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.reloadBasket,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    if (trip.itemsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val itemLines = trip.itemsSummary
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("Basket:", ignoreCase = true) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemLines.forEach { line ->
                                val parsed = parseHistoryItemLine(line)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = parsed.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (parsed.qty != null || parsed.offerTag.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (parsed.qty != null) {
                                                        Text(
                                                            text = "${strings.qtyLabel} ${parsed.qty}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (parsed.offerTag.isNotBlank()) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = parsed.offerTag,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (parsed.price.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = parsed.price,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ParsedHistoryItem(
    val name: String,
    val qty: Int? = null,
    val offerTag: String = "",
    val price: String = "",
)

private val HISTORY_ITEM_REGEX = Regex("""^[•\-]?\s*(.+?)\s*\(x(\d+)\)(?:\s*\[(.+?)])?(?:\s*[—\-]\s*(.+))?$""")

private fun parseHistoryItemLine(raw: String): ParsedHistoryItem {
    val cleaned = raw.trim()
    val match = HISTORY_ITEM_REGEX.find(cleaned)
    if (match != null) {
        val name = match.groupValues[1].trim()
        val qty = match.groupValues[2].toIntOrNull()
        val offerTag = match.groupValues[3].trim()
        val price = match.groupValues[4].trim()
        return ParsedHistoryItem(name = name, qty = qty, offerTag = offerTag, price = price)
    }
    return ParsedHistoryItem(name = cleaned.removePrefix("•").trim())
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
