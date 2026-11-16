package com.inventory.barcodetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    onFinishAndSave: () -> Unit,
    onClearBasket: () -> Unit,
    onApplyVoucher: () -> Unit = {},
    onSetBudget: () -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }

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

    val effectiveBudgetLimit = if (list.budgetLimit > 0) list.budgetLimit else defaultBudgetLimit
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Collapsed Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onRename() }
                            ) {
                                Text(
                                    text = if (isOverBudget) "${list.name} (Budget Reached!)" else list.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Basket",
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currencySymbol, itemsSubtotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            if (totalSavings > 0) {
                                Text(
                                    text = String.format(Locale.US, "Saved %s%.2f with deals", currencySymbol, totalSavings),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "$totalCount items in cart",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onError else if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (effectiveBudgetLimit > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "Limit: %s%.2f", currencySymbol, effectiveBudgetLimit),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Budget Limit Visual Progress Bar
                if (effectiveBudgetLimit > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f spent", currencySymbol, itemsSubtotal),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val remaining = effectiveBudgetLimit - itemsSubtotal
                        Text(
                            text = if (remaining >= 0) String.format(Locale.US, "%s%.2f remaining", currencySymbol, remaining)
                            else String.format(Locale.US, "Exceeded by %s%.2f", currencySymbol, -remaining),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
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

                        val sortedItems = remember(itemsInList) {
                            val regularItems = itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
                            val voucherItems = itemsInList.filter { it.offerType == "VOUCHER" || it.unitPrice < 0 }
                            regularItems + voucherItems
                        }

                        if (sortedItems.isEmpty()) {
                            Text(
                                "No items in this basket yet. Check box above and scan barcodes or search items to populate!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            sortedItems.forEach { item ->
                                if (item.offerType == "VOUCHER" || item.unitPrice < 0) {
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "🎟️ $voucherName",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
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
                                } else {
                                    val piece = pieceMap[item.barcode]
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "• $displayName (x${item.quantity})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (offerDesc.isNotBlank()) {
                                                val savingTag = if (lineSavings > 0) " — saved $currencySymbol${String.format(Locale.US, "%.2f", lineSavings)}" else ""
                                                Text(
                                                    "  [$offerDesc$savingTag]",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (lineTotal > 0) {
                                            Text(
                                                "$currencySymbol${String.format(Locale.US, "%.2f", lineTotal)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High-Contrast Main Action Row: Clear Basket & Finish & Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onClearBasket,
                                modifier = Modifier.weight(1f),
                                enabled = itemsInList.isNotEmpty(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Basket", fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Button(
                                onClick = onFinishAndSave,
                                modifier = Modifier.weight(1f),
                                enabled = itemsInList.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish & Save", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Secondary Actions Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onSetBudget) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Budget", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            TextButton(onClick = onApplyVoucher) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Apply Voucher", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            TextButton(onClick = onRename) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rename", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            TextButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Swipe Gesture Instruction Hint
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "↔ Swipe card: Right to Duplicate • Left to Delete",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
