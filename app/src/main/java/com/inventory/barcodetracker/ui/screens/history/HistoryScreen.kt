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
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.AppSearchBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import com.inventory.barcodetracker.data.local.entity.TripBasketSnapshot
import com.inventory.barcodetracker.data.local.entity.TripSnapshotItem
import com.inventory.barcodetracker.data.local.entity.calculateBasketSummary
import com.inventory.barcodetracker.data.local.entity.calculateItemSavings
import com.inventory.barcodetracker.data.local.entity.calculateItemTotal
import com.inventory.barcodetracker.data.local.entity.formatOfferBadge
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
    var tripToReload by remember { mutableStateOf<ShoppingTripEntity?>(null) }
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

                    if (selectedTripIds.isNotEmpty()) {
                        AppButton(
                            onClick = { showConfirmDeleteBatch = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format(Locale.US, strings.deleteCountFmt, selectedTripIds.size), fontWeight = FontWeight.Bold, maxLines = 1)
                        }
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
                            onReload = { tripToReload = trip }
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

    // Make Active Confirmation Dialog
    tripToReload?.let { trip ->
        val snapshot = trip.parseSnapshot()
        val firstLine = trip.itemsSummary.lineSequence().firstOrNull()?.trim() ?: ""
        val basketName = when {
            snapshot != null && snapshot.basketName.isNotBlank() -> snapshot.basketName
            firstLine.startsWith("Basket:") || firstLine.startsWith("List:") -> firstLine.substringAfter(":").trim()
            else -> strings.defaultHistoryBasketName
        }
        AlertDialog(
            onDismissRequest = { tripToReload = null },
            title = { Text(strings.reloadBasketTitle) },
            text = { Text(String.format(Locale.US, strings.reloadBasketConfirmFmt, basketName)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.reloadTripIntoCart(trip)
                        tripToReload = null
                    }
                ) {
                    Text(strings.reloadBasket)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToReload = null }) {
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

@OptIn(ExperimentalLayoutApi::class)
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

    val snapshot = remember(trip.id, trip.snapshotJson) { trip.parseSnapshot() }

    val firstLine = trip.itemsSummary.lineSequence().firstOrNull()?.trim() ?: ""
    val basketName = when {
        snapshot != null && snapshot.basketName.isNotBlank() -> snapshot.basketName
        firstLine.startsWith("Basket:") || firstLine.startsWith("List:") -> firstLine.substringAfter(":").trim()
        else -> strings.defaultHistoryBasketName
    }

    val effectiveBudgetLimit = snapshot?.budgetLimit?.takeIf { it > 0.0 } ?: 0.0
    val preVoucherSubtotal = snapshot?.subtotalBeforeVouchers ?: trip.totalCost
    val hasVoucherDiscount = (snapshot?.voucherDiscountAmount ?: 0.0) > 0.0
    val totalSavings = snapshot?.totalSavings ?: 0.0
    val isOverBudget = effectiveBudgetLimit > 0.0 && preVoucherSubtotal > effectiveBudgetLimit
    val budgetProgress = if (effectiveBudgetLimit > 0.0) {
        (preVoucherSubtotal / effectiveBudgetLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer
            else if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Compact Primary Row (matches ShoppingListCard header)
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = basketName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
                        else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (trip.itemCount == 1) strings.oneItem else String.format(Locale.US, strings.itemsCountFmt, trip.itemCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (totalSavings > 0.0) {
                            Text(
                                text = String.format(Locale.US, strings.bulletSavedFmt, trip.currencySymbol, totalSavings),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = formatDate(trip.tripDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isOverBudget) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = strings.overBudget,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                maxLines = 1
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f", trip.currencySymbol, trip.totalCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
                                else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.primary
                            )
                            if (hasVoucherDiscount && preVoucherSubtotal > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", trip.currencySymbol, preVoucherSubtotal),
                                    style = MaterialTheme.typography.labelSmall,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) strings.collapseItemsDesc else strings.expandItemsDesc,
                                tint = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Compact Budget Meter (matches ShoppingListCard)
            if (effectiveBudgetLimit > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                val remaining = effectiveBudgetLimit - preVoucherSubtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = if (remaining >= 0) String.format(Locale.US, strings.leftFmt, trip.currencySymbol, remaining)
                            else String.format(Locale.US, strings.overFmt, trip.currencySymbol, -remaining),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Expandable Itemized List & Actions
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.delete,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AppButton(
                            onClick = onReload,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (snapshot != null && snapshot.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HistorySnapshotContent(
                            snapshot = snapshot,
                            currencySymbol = trip.currencySymbol,
                        )
                    } else if (trip.itemsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val rawLines = trip.itemsSummary.split("\n").map { it.trim() }
                        val blocks = mutableListOf<HistoryReceiptBlock>()
                        var currentGroupHeader: String? = null
                        var currentGroupItems = mutableListOf<ParsedHistoryItem>()

                        fun flushGroup() {
                            currentGroupHeader?.let { hdr ->
                                blocks.add(HistoryReceiptBlock.MixMatchGroup(hdr, currentGroupItems.toList()))
                            }
                            currentGroupHeader = null
                            currentGroupItems = mutableListOf()
                        }

                        for (line in rawLines) {
                            if (line.startsWith("Basket:", ignoreCase = true) || line.startsWith("List:", ignoreCase = true)) continue
                            if (line.isBlank()) {
                                flushGroup()
                                continue
                            }
                            if (line.startsWith("🏷️ Mix & Match:")) {
                                flushGroup()
                                currentGroupHeader = line.removePrefix("🏷️ Mix & Match:").trim()
                                continue
                            }
                            val parsed = parseHistoryItemLine(line)
                            if (currentGroupHeader != null) {
                                currentGroupItems.add(parsed)
                            } else {
                                blocks.add(HistoryReceiptBlock.SingleItem(parsed))
                            }
                        }
                        flushGroup()

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            blocks.forEach { block ->
                                when (block) {
                                    is HistoryReceiptBlock.MixMatchGroup -> {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Text(
                                                            text = "🏷️ ${strings.mixAndMatchBadge}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Text(
                                                        text = block.headerDetail,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                block.items.forEach { parsed ->
                                                    HistoryItemRow(parsed = parsed, qtyLabel = strings.qtyLabel)
                                                }
                                            }
                                        }
                                    }
                                    is HistoryReceiptBlock.SingleItem -> {
                                        HistoryItemRow(parsed = block.item, qtyLabel = strings.qtyLabel)
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

@Composable
private fun HistorySnapshotContent(
    snapshot: TripBasketSnapshot,
    currencySymbol: String,
) {
    val strings = LocalAppStrings.current
    val listItems = remember(snapshot) {
        snapshot.items.map { s ->
            ShoppingListItemEntity(
                listId = 0L,
                barcode = s.barcode,
                quantity = s.quantity,
                unitPrice = s.unitPrice,
                offerType = s.offerType,
                offerBuyQty = s.offerBuyQty,
                offerPayQty = s.offerPayQty,
                offerFixedPrice = s.offerFixedPrice,
                offerPercentOff = s.offerPercentOff,
                isChecked = s.isChecked,
                offerGroupId = s.offerGroupId,
                checkedQuantity = s.checkedQuantity,
            )
        }
    }
    val snapshotPieceMap = remember(snapshot) {
        snapshot.items
            .filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
            .associate { s ->
                s.barcode to PieceEntity(
                    barcode = s.barcode,
                    name = s.name.ifBlank { s.barcode },
                    stockQuantity = s.quantity,
                    unitPrice = s.unitPrice,
                    offerType = if (s.offerType == "NO_OFFER") "NONE" else s.offerType,
                    offerBuyQty = s.offerBuyQty,
                    offerPayQty = s.offerPayQty,
                    offerFixedPrice = s.offerFixedPrice,
                    offerPercentOff = s.offerPercentOff,
                )
            }
    }
    val summary = remember(listItems, snapshotPieceMap, currencySymbol) {
        calculateBasketSummary(listItems, snapshotPieceMap, currencySymbol)
    }

    val regularItems = listItems.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
    val voucherItems = listItems.filter { it.offerType == "VOUCHER" || it.unitPrice < 0 }
    val groupedIds = regularItems.mapNotNull { it.offerGroupId.takeIf { g -> g.isNotBlank() } }.distinct()
    val ungroupedRegular = regularItems.filter { it.offerGroupId.isBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 1. Mix & Match groups (exact ShoppingListCard style)
        groupedIds.forEach { groupId ->
            val groupPricing = summary.groupPricings[groupId] ?: return@forEach
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "🏷️ ${strings.mixAndMatchBadge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = groupPricing.offerDescription,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currencySymbol, groupPricing.discountedTotal),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (groupPricing.savings > 0) {
                                Text(
                                    text = String.format(Locale.US, strings.savedFmt, currencySymbol, groupPricing.savings),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                    groupPricing.items.forEach { item ->
                        val piece = snapshotPieceMap[item.barcode]
                        HistorySnapshotItemRow(
                            item = item,
                            piece = piece,
                            currencySymbol = currencySymbol,
                            showOfferBadge = false,
                        )
                    }
                }
            }
        }

        // 2. Ungrouped regular items (exact BasketItemRow style)
        ungroupedRegular.forEach { item ->
            val piece = snapshotPieceMap[item.barcode]
            HistorySnapshotItemRow(
                item = item,
                piece = piece,
                currencySymbol = currencySymbol,
                showOfferBadge = true,
            )
        }

        // 3. Vouchers (exact ShoppingListCard voucher style)
        if (voucherItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            voucherItems.forEach { item ->
                val rawName = item.barcode
                    .removePrefix("VOUCHER_")
                    .replace(Regex("""^\d+_"""), "")
                    .replace("_", " ")
                    .replace(Regex("""(\$|£|€|RON)\s+"""), "$1")
                    .trim()
                val voucherName = if (rawName.contains("Voucher", ignoreCase = true) || rawName.contains("Coupon", ignoreCase = true) || rawName.contains("Cupon", ignoreCase = true)) {
                    rawName
                } else {
                    String.format(Locale.US, strings.voucherLabelFmt, rawName)
                }
                val discountVal = kotlin.math.abs(item.unitPrice)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎟️ $voucherName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "-$currencySymbol${String.format(Locale.US, "%.2f", discountVal)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySnapshotItemRow(
    item: ShoppingListItemEntity,
    piece: PieceEntity?,
    currencySymbol: String,
    showOfferBadge: Boolean,
) {
    val strings = LocalAppStrings.current
    var itemExpanded by remember { mutableStateOf(false) }

    val displayName = piece?.name ?: item.barcode
    val lineTotal = if (showOfferBadge) item.calculateItemTotal(piece) else (item.unitPrice * item.quantity)
    val lineSavings = if (showOfferBadge) item.calculateItemSavings(piece) else 0.0
    val effOfferType = when (item.offerType) {
        "NO_OFFER" -> "NONE"
        "NONE" -> piece?.offerType ?: "NONE"
        else -> item.offerType
    }
    val offerDesc = if (effOfferType != "NONE") {
        formatOfferBadge(
            offerType = effOfferType,
            buyQty = if (item.offerBuyQty > 0) item.offerBuyQty else (piece?.offerBuyQty ?: 0),
            payQty = if (item.offerPayQty > 0) item.offerPayQty else (piece?.offerPayQty ?: 0),
            fixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else (piece?.offerFixedPrice ?: 0.0),
            percentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else (piece?.offerPercentOff ?: 0.0),
            currencySymbol = currencySymbol,
        )
    } else ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { itemExpanded = !itemExpanded },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${strings.qtyLabel} ${item.quantity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (showOfferBadge && offerDesc.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🏷️ $offerDesc",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
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

                if (lineTotal > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currencySymbol, lineTotal),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { itemExpanded = !itemExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (itemExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (itemExpanded) strings.collapseItemDesc else strings.expandItemDesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = itemExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.barcodeLabel} ${item.barcode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lineSavings > 0) {
                        Text(
                            text = String.format(Locale.US, strings.savedWithDealFmt, currencySymbol, lineSavings),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

private sealed interface HistoryReceiptBlock {
    data class MixMatchGroup(val headerDetail: String, val items: List<ParsedHistoryItem>) : HistoryReceiptBlock
    data class SingleItem(val item: ParsedHistoryItem) : HistoryReceiptBlock
}

@Composable
private fun HistoryItemRow(
    parsed: ParsedHistoryItem,
    qtyLabel: String,
) {
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
                                text = "$qtyLabel ${parsed.qty}",
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
                                    text = if (parsed.offerTag.startsWith("🏷️")) parsed.offerTag else "🏷️ ${parsed.offerTag}",
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
                    color = if (parsed.isVoucher) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class ParsedHistoryItem(
    val name: String,
    val qty: Int? = null,
    val offerTag: String = "",
    val price: String = "",
    val isVoucher: Boolean = false,
)

private val HISTORY_ITEM_REGEX = Regex("""^[•\-]?\s*(.+?)\s*\(x(\d+)\)(?:\s*\[(.+?)])?(?:\s*[—\-]\s*(.+))?$""")
private val HISTORY_VOUCHER_REGEX = Regex("""^[•\-]?\s*(🎟️\s*Voucher\s*\(.+?\))\s*[—\-]\s*(.+)$""")

private fun parseHistoryItemLine(raw: String): ParsedHistoryItem {
    val cleaned = raw.trim()
    val voucherMatch = HISTORY_VOUCHER_REGEX.find(cleaned)
    if (voucherMatch != null) {
        return ParsedHistoryItem(
            name = voucherMatch.groupValues[1].trim(),
            price = voucherMatch.groupValues[2].trim(),
            isVoucher = true,
        )
    }
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
