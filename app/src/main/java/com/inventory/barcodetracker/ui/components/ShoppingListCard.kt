package com.inventory.barcodetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.calculateItemSavings
import com.inventory.barcodetracker.data.local.entity.calculateItemTotal
import java.util.Locale

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCard(
    list: ShoppingListEntity,
    isSelected: Boolean,
    itemsInList: List<ShoppingListItemEntity>,
    pieceMap: Map<String, PieceEntity> = emptyMap(),
    currencySymbol: String = "$",
    defaultBudgetLimit: Double = 0.0,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onApplyVoucher: () -> Unit = {},
    onSetBudget: () -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
    onToggleItemChecked: (String, Boolean) -> Unit = { _, _ -> },
    onAdjustItemQuantity: (String, Int) -> Unit = { _, _ -> },
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showCheckedItems by remember { mutableStateOf(false) }
    var itemPendingDelete by remember { mutableStateOf<Pair<ShoppingListItemEntity, String>?>(null) }

    val totalCount = itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }.sumOf { it.quantity }

    val itemsSubtotal = maxOf(
        0.0,
        itemsInList.sumOf { item ->
            if (item.offerType == "VOUCHER" || item.unitPrice < 0) {
                item.unitPrice
            } else {
                val piece = pieceMap[item.barcode]
                if (piece != null) {
                    val effOfferType = if (item.offerType != "NONE") item.offerType else piece.offerType
                    val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else piece.offerBuyQty
                    val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else piece.offerPayQty
                    val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else piece.offerFixedPrice
                    val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else piece.offerPercentOff

                    piece.copy(
                        stockQuantity = item.quantity,
                        unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
                        offerType = effOfferType,
                        offerBuyQty = effBuyQty,
                        offerPayQty = effPayQty,
                        offerFixedPrice = effFixedPrice,
                        offerPercentOff = effPercentOff
                    ).calculatedTotal
                } else {
                    item.calculateItemTotal(null)
                }
            }
        }
    )

    val totalSavings = itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }.sumOf { item ->
        val piece = pieceMap[item.barcode]
        if (piece != null) {
            val effOfferType = if (item.offerType != "NONE") item.offerType else piece.offerType
            val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else piece.offerBuyQty
            val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else piece.offerPayQty
            val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else piece.offerFixedPrice
            val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else piece.offerPercentOff

            piece.copy(
                stockQuantity = item.quantity,
                unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
                offerType = effOfferType,
                offerBuyQty = effBuyQty,
                offerPayQty = effPayQty,
                offerFixedPrice = effFixedPrice,
                offerPercentOff = effPercentOff
            ).savings
        } else {
            item.calculateItemSavings(null)
        }
    }

    val effectiveBudgetLimit = when {
        list.budgetLimit < 0.0 -> 0.0
        list.budgetLimit > 0.0 -> list.budgetLimit
        else -> defaultBudgetLimit
    }
    val isOverBudget = effectiveBudgetLimit > 0 && itemsSubtotal >= effectiveBudgetLimit
    val budgetProgress = if (effectiveBudgetLimit > 0) (itemsSubtotal / effectiveBudgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCopy()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isExpanded,
        enableDismissFromEndToStart = !isExpanded,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy List", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Duplicate", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Delete, contentDescription = "Delete List", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isOverBudget) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Over Budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (totalCount == 1) "1 item" else "$totalCount items",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (totalSavings > 0) {
                                Text(
                                    text = String.format(Locale.US, "• Saved %s%.2f", currencySymbol, totalSavings),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = String.format(Locale.US, "%s%.2f", currencySymbol, itemsSubtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Compact Budget Meter (always visible when a budget is set)
                if (effectiveBudgetLimit > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val remaining = effectiveBudgetLimit - itemsSubtotal
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
                                text = if (remaining >= 0) String.format(Locale.US, "%s%.2f left", currencySymbol, remaining)
                                else String.format(Locale.US, "+%s%.2f over", currencySymbol, -remaining),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Expanded Details Section
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        val regularItems = remember(itemsInList) {
                            itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
                        }
                        val uncheckedItems = remember(regularItems) { regularItems.filter { !it.isChecked } }
                        val checkedItems = remember(regularItems) { regularItems.filter { it.isChecked } }
                        val voucherItems = remember(itemsInList) {
                            itemsInList.filter { it.offerType == "VOUCHER" || it.unitPrice < 0 }
                        }

                        fun itemLineTotal(item: ShoppingListItemEntity): Double {
                            val piece = pieceMap[item.barcode] ?: return item.calculateItemTotal(null)
                            val effOfferType = if (item.offerType != "NONE") item.offerType else piece.offerType
                            val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else piece.offerBuyQty
                            val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else piece.offerPayQty
                            val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else piece.offerFixedPrice
                            val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else piece.offerPercentOff
                            return piece.copy(
                                stockQuantity = item.quantity,
                                unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
                                offerType = effOfferType,
                                offerBuyQty = effBuyQty,
                                offerPayQty = effPayQty,
                                offerFixedPrice = effFixedPrice,
                                offerPercentOff = effPercentOff
                            ).calculatedTotal
                        }

                        val uncheckedTotal = uncheckedItems.sumOf { itemLineTotal(it) }
                        val checkedTotal = checkedItems.sumOf { itemLineTotal(it) }

                        if (regularItems.isEmpty() && voucherItems.isEmpty()) {
                            Text(
                                "No items in this basket yet. Check box above and scan barcodes or search items to populate!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Unchecked Items
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uncheckedItems.forEach { item ->
                                    val piece = pieceMap[item.barcode]
                                    val displayName = piece?.name ?: item.barcode
                                    BasketItemRow(
                                        item = item,
                                        piece = piece,
                                        currencySymbol = currencySymbol,
                                        onToggleChecked = { onToggleItemChecked(item.barcode, !item.isChecked) },
                                        onRequestDelete = { itemPendingDelete = item to displayName },
                                        onAdjustQuantity = { delta -> onAdjustItemQuantity(item.barcode, delta) }
                                    )
                                }
                            }

                            // Collapsible Checked Items Section
                            if (checkedItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    onClick = { showCheckedItems = !showCheckedItems },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "(${checkedItems.size}) ${if (showCheckedItems) "Hide checked items" else "Show checked items"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = if (showCheckedItems) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = showCheckedItems) {
                                    Column(
                                        modifier = Modifier.padding(top = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        checkedItems.forEach { item ->
                                            val piece = pieceMap[item.barcode]
                                            val displayName = piece?.name ?: item.barcode
                                            BasketItemRow(
                                                item = item,
                                                piece = piece,
                                                currencySymbol = currencySymbol,
                                                onToggleChecked = { onToggleItemChecked(item.barcode, !item.isChecked) },
                                                onRequestDelete = { itemPendingDelete = item to displayName },
                                                onAdjustQuantity = { delta -> onAdjustItemQuantity(item.barcode, delta) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Unchecked / Checked / Total Summary Strip
                            if (regularItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Unchecked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                String.format(Locale.US, "%s%.2f", currencySymbol, uncheckedTotal),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Checked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                String.format(Locale.US, "%s%.2f", currencySymbol, checkedTotal),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                String.format(Locale.US, "%s%.2f", currencySymbol, itemsSubtotal),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Vouchers
                            if (voucherItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                voucherItems.forEach { item ->
                                    val rawName = item.barcode
                                        .removePrefix("VOUCHER_")
                                        .replace(Regex("""^\d+_"""), "")
                                        .replace("_", " ")
                                        .replace(Regex("""(\$|£|€|RON)\s+"""), "$1")
                                        .trim()

                                    val voucherName = if (rawName.contains("Voucher", ignoreCase = true) || rawName.contains("Coupon", ignoreCase = true)) {
                                        rawName
                                    } else {
                                        "Voucher ($rawName)"
                                    }

                                    val discountVal = kotlin.math.abs(item.unitPrice)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "🎟️ $voucherName",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "-$currencySymbol${String.format(Locale.US, "%.2f", discountVal)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            IconButton(
                                                onClick = { onRemoveItem(item.barcode) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "Remove Voucher", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Streamlined Full-Width Actions Row
                        val actionColors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        val actionBorder = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppOutlinedButton(
                                onClick = onSetBudget,
                                colors = actionColors,
                                border = actionBorder,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Budget", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            AppOutlinedButton(
                                onClick = onApplyVoucher,
                                colors = actionColors,
                                border = actionBorder,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voucher", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            AppOutlinedButton(
                                onClick = onRename,
                                colors = actionColors,
                                border = actionBorder,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Rename", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            AppOutlinedButton(
                                onClick = onCopy,
                                colors = actionColors,
                                border = actionBorder,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Swipe Gesture Instruction Hint
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "↔ Swipe item: Right to Check • Left to Delete",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Item Deletion
    itemPendingDelete?.let { (item, displayName) ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text("Remove Item") },
            text = { Text("Remove \"$displayName\" from \"${list.name}\"?") },
            confirmButton = {
                AppButton(
                    onClick = {
                        onRemoveItem(item.barcode)
                        itemPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasketItemRow(
    item: ShoppingListItemEntity,
    piece: PieceEntity?,
    currencySymbol: String,
    onToggleChecked: () -> Unit,
    onRequestDelete: () -> Unit,
    onAdjustQuantity: (Int) -> Unit,
) {
    var itemExpanded by remember { mutableStateOf(false) }

    val effOfferType = if (item.offerType != "NONE") item.offerType else (piece?.offerType ?: "NONE")
    val effBuyQty = if (item.offerBuyQty > 0) item.offerBuyQty else (piece?.offerBuyQty ?: 0)
    val effPayQty = if (item.offerPayQty > 0) item.offerPayQty else (piece?.offerPayQty ?: 0)
    val effFixedPrice = if (item.offerFixedPrice > 0.0) item.offerFixedPrice else (piece?.offerFixedPrice ?: 0.0)
    val effPercentOff = if (item.offerPercentOff > 0.0) item.offerPercentOff else (piece?.offerPercentOff ?: 0.0)

    val mergedPiece = piece?.copy(
        stockQuantity = item.quantity,
        unitPrice = if (item.unitPrice > 0) item.unitPrice else piece.unitPrice,
        offerType = effOfferType,
        offerBuyQty = effBuyQty,
        offerPayQty = effPayQty,
        offerFixedPrice = effFixedPrice,
        offerPercentOff = effPercentOff
    )

    val displayName = piece?.name ?: item.barcode
    val lineTotal = mergedPiece?.calculatedTotal ?: item.calculateItemTotal(null)
    val lineSavings = mergedPiece?.savings ?: item.calculateItemSavings(null)
    val offerDesc = mergedPiece?.getOfferDescription(currencySymbol) ?: ""

    val itemDismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleChecked()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onRequestDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = itemDismissState,
        backgroundContent = {
            val direction = itemDismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.isChecked) "Uncheck" else "Check",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { itemExpanded = !itemExpanded },
            shape = RoundedCornerShape(10.dp),
            color = if (item.isChecked) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleChecked,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (item.isChecked) "Uncheck item" else "Check item",
                            tint = if (item.isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Qty: ${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (offerDesc.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = offerDesc,
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

                    if (lineTotal > 0) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f", currencySymbol, lineTotal),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { itemExpanded = !itemExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (itemExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (itemExpanded) "Collapse item" else "Expand item",
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
                            text = "Barcode: ${item.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (piece != null && piece.location.isNotBlank() && piece.location != "Default Bin") {
                            Text(
                                text = "Category: ${piece.location}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lineSavings > 0) {
                            Text(
                                text = String.format(Locale.US, "Saved %s%.2f with deal", currencySymbol, lineSavings),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onRequestDelete,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove item",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Qty:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                FilledIconButton(
                                    onClick = { onAdjustQuantity(-1) },
                                    modifier = Modifier.size(30.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(15.dp))
                                }
                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                FilledIconButton(
                                    onClick = { onAdjustQuantity(1) },
                                    modifier = Modifier.size(30.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
