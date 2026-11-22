package com.inventory.barcodetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.data.local.entity.calculateBasketSummary
import com.inventory.barcodetracker.data.local.entity.calculateItemSavings
import com.inventory.barcodetracker.data.local.entity.calculateItemTotal
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

enum class BasketCardSection { FULL, HEADER_ONLY, BODY_ONLY }

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingListCard(
    list: ShoppingListEntity,
    isSelected: Boolean,
    itemsInList: List<ShoppingListItemEntity>,
    pieceMap: Map<String, PieceEntity> = emptyMap(),
    currencySymbol: String = "$",
    defaultBudgetLimit: Double = 0.0,
    isExpanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    renderSection: BasketCardSection = BasketCardSection.FULL,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onApplyVoucher: () -> Unit = {},
    onSetBudget: () -> Unit = {},
    onOpenMixAndMatch: () -> Unit = {},
    onUngroupMixAndMatch: (String) -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
    onToggleItemChecked: (String, Boolean) -> Unit = { _, _ -> },
    onAdjustItemQuantity: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onEditItem: (PieceEntity) -> Unit = {},
    onRemoveItemOffer: (String) -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    var internalExpanded by remember { mutableStateOf(false) }
    val effectiveExpanded = isExpanded ?: internalExpanded
    var showCheckedItems by remember { mutableStateOf(false) }
    var itemPendingDelete by remember { mutableStateOf<Pair<ShoppingListItemEntity, String>?>(null) }
    var showQuickActionsInline by remember { mutableStateOf(false) }
    val toggleExpand = {
        val next = !effectiveExpanded
        if (onToggleExpand != null) {
            onToggleExpand()
        } else {
            internalExpanded = next
        }
        if (next) showQuickActionsInline = false
    }

    val totalCount = itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }.sumOf { it.quantity }
    val basketSummary = remember(itemsInList, pieceMap, currencySymbol) {
        calculateBasketSummary(itemsInList, pieceMap, currencySymbol)
    }
    val itemsSubtotal = basketSummary.regularItemsSubtotal
    val preVoucherSubtotal = basketSummary.subtotalBeforeVouchers
    val hasVoucherDiscount = basketSummary.voucherDiscountAmount > 0.0
    val totalSavings = basketSummary.totalSavings

    val effectiveBudgetLimit = when {
        list.budgetLimit < 0.0 -> 0.0
        list.budgetLimit > 0.0 -> list.budgetLimit
        else -> defaultBudgetLimit
    }
    val isOverBudget = effectiveBudgetLimit > 0 && preVoucherSubtotal >= effectiveBudgetLimit
    val budgetProgress = if (effectiveBudgetLimit > 0) (preVoucherSubtotal / effectiveBudgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f

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

    val cardContent: @Composable () -> Unit = {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (renderSection != BasketCardSection.BODY_ONLY) {
                        Modifier.combinedClickable(
                            onClick = toggleExpand,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!effectiveExpanded) {
                                    showQuickActionsInline = !showQuickActionsInline
                                }
                            }
                        )
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = if (renderSection == BasketCardSection.HEADER_ONLY) {
                CardDefaults.cardElevation(defaultElevation = 4.dp)
            } else {
                CardDefaults.cardElevation()
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (renderSection != BasketCardSection.BODY_ONLY) {
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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalCount == 1) strings.oneItem else String.format(Locale.US, strings.itemsCountFmt, totalCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (totalSavings > 0) {
                                Text(
                                    text = String.format(Locale.US, strings.bulletSavedFmt, currencySymbol, totalSavings),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1
                                )
                            }
                        }
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
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, itemsSubtotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
                                    else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.primary
                                )
                                if (hasVoucherDiscount && preVoucherSubtotal > 0.0) {
                                    Text(
                                        text = String.format(Locale.US, "%s%.2f", currencySymbol, preVoucherSubtotal),
                                        style = MaterialTheme.typography.labelSmall,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = toggleExpand,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (effectiveExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (effectiveExpanded) strings.collapse else strings.expand,
                                    tint = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Compact Budget Meter (always visible when a budget is set; tracks pre-voucher spend)
                if (effectiveBudgetLimit > 0) {
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
                                text = if (remaining >= 0) String.format(Locale.US, strings.leftFmt, currencySymbol, remaining)
                                else String.format(Locale.US, strings.overFmt, currencySymbol, -remaining),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Inline Quick Actions (shown directly below collapsed basket on long press)
                AnimatedVisibility(visible = !effectiveExpanded && showQuickActionsInline) {
                    val regularItemCount = itemsInList.count { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        BasketActionButtonsRow(
                            showMixAndMatch = regularItemCount >= 2,
                            onSetBudget = {
                                showQuickActionsInline = false
                                onSetBudget()
                            },
                            onApplyVoucher = {
                                showQuickActionsInline = false
                                onApplyVoucher()
                            },
                            onRename = {
                                showQuickActionsInline = false
                                onRename()
                            },
                            onCopy = {
                                showQuickActionsInline = false
                                onCopy()
                            },
                            onOpenMixAndMatch = {
                                showQuickActionsInline = false
                                onOpenMixAndMatch()
                            }
                        )
                    }
                }
                } // end if (renderSection != BODY_ONLY)

                // Expanded Details Section
                if (renderSection != BasketCardSection.HEADER_ONLY) {
                AnimatedVisibility(visible = effectiveExpanded || renderSection == BasketCardSection.BODY_ONLY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (renderSection == BasketCardSection.BODY_ONLY) 0.dp else 8.dp)
                    ) {
                        if (renderSection != BasketCardSection.BODY_ONLY) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val regularItems = remember(itemsInList) {
                            itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
                        }
                        val ungroupedRegular = remember(regularItems) {
                            regularItems.filter { it.offerGroupId.isBlank() }
                        }
                        val uncheckedItems = remember(ungroupedRegular) { ungroupedRegular.filter { it.uncheckedQty > 0 } }
                        val checkedItems = remember(ungroupedRegular) { ungroupedRegular.filter { it.effectiveCheckedQty > 0 } }
                        val voucherItems = remember(itemsInList) {
                            itemsInList.filter { it.offerType == "VOUCHER" || it.unitPrice < 0 }
                        }

                        val uncheckedTotal = basketSummary.uncheckedTotal
                        val checkedTotal = basketSummary.checkedTotal

                        if (regularItems.isEmpty() && voucherItems.isEmpty()) {
                            Text(
                                strings.emptyBasketHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Mix & Match Deal Groups
                            if (basketSummary.groupPricings.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    basketSummary.groupPricings.values.forEach { group ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                                                        modifier = Modifier.weight(1f),
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
                                                            text = group.offerDescription,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (group.savings > 0) {
                                                            Text(
                                                                text = String.format(Locale.US, strings.savedFmt, currencySymbol, group.savings),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.tertiary
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { onUngroupMixAndMatch(group.groupId) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Clear,
                                                                contentDescription = strings.ungroupDealDesc,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                                group.items.forEach { item ->
                                                    key(item.barcode) {
                                                        val piece = pieceMap[item.barcode]
                                                        val displayName = piece?.name ?: item.barcode
                                                        BasketItemRow(
                                                            item = item,
                                                            piece = piece,
                                                            currencySymbol = currencySymbol,
                                                            showOfferBadge = false,
                                                            onToggleChecked = { onToggleItemChecked(item.barcode, !item.isChecked) },
                                                            onRequestDelete = { itemPendingDelete = item to displayName },
                                                            onAdjustQuantity = { delta -> onAdjustItemQuantity(item.barcode, delta, item.isChecked) },
                                                            onEditItem = onEditItem,
                                                            onRemoveOffer = { onRemoveItemOffer(item.barcode) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (uncheckedItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // Unchecked Items
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uncheckedItems.forEach { item ->
                                    key("unchecked_${item.barcode}") {
                                        val piece = pieceMap[item.barcode]
                                        val displayName = piece?.name ?: item.barcode
                                        BasketItemRow(
                                            item = item,
                                            piece = piece,
                                            currencySymbol = currencySymbol,
                                            displayQty = item.uncheckedQty,
                                            displayChecked = false,
                                            onToggleChecked = { onToggleItemChecked(item.barcode, true) },
                                            onRequestDelete = { itemPendingDelete = item to displayName },
                                            onAdjustQuantity = { delta -> onAdjustItemQuantity(item.barcode, delta, false) },
                                            onEditItem = onEditItem,
                                            onRemoveOffer = { onRemoveItemOffer(item.barcode) }
                                        )
                                    }
                                }
                            }

                            // Collapsible Checked Items Section
                            if (checkedItems.isNotEmpty()) {
                                val checkedUnitsCount = checkedItems.sumOf { it.effectiveCheckedQty }
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
                                            text = "($checkedUnitsCount) ${if (showCheckedItems) strings.hideCheckedItems else strings.showCheckedItems}",
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
                                            key("checked_${item.barcode}") {
                                                val piece = pieceMap[item.barcode]
                                                val displayName = piece?.name ?: item.barcode
                                                BasketItemRow(
                                                    item = item,
                                                    piece = piece,
                                                    currencySymbol = currencySymbol,
                                                    displayQty = item.effectiveCheckedQty,
                                                    displayChecked = true,
                                                    onToggleChecked = { onToggleItemChecked(item.barcode, false) },
                                                    onRequestDelete = { itemPendingDelete = item to displayName },
                                                    onAdjustQuantity = { delta -> onAdjustItemQuantity(item.barcode, delta, true) },
                                                    onEditItem = onEditItem,
                                                    onRemoveOffer = { onRemoveItemOffer(item.barcode) }
                                                )
                                            }
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
                                            Text(strings.uncheckedLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                String.format(Locale.US, "%s%.2f", currencySymbol, uncheckedTotal),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(strings.checkedLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                String.format(Locale.US, "%s%.2f", currencySymbol, checkedTotal),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(strings.totalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                                    val voucherName = if (rawName.contains("Voucher", ignoreCase = true) || rawName.contains("Coupon", ignoreCase = true) || rawName.contains("Cupon", ignoreCase = true)) {
                                        rawName
                                    } else {
                                        String.format(Locale.US, strings.voucherLabelFmt, rawName)
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
                                                Icon(Icons.Default.Clear, contentDescription = strings.removeVoucherDesc, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Streamlined Full-Width Actions Row
                        BasketActionButtonsRow(
                            showMixAndMatch = regularItems.size >= 2,
                            onSetBudget = onSetBudget,
                            onApplyVoucher = onApplyVoucher,
                            onRename = onRename,
                            onCopy = onCopy,
                            onOpenMixAndMatch = onOpenMixAndMatch
                        )

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
                                    text = strings.swipeHint,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                } // end if (renderSection != HEADER_ONLY)
            }
        }
    }

    when (renderSection) {
        BasketCardSection.FULL -> {
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = !effectiveExpanded,
                enableDismissFromEndToStart = !effectiveExpanded,
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
                                Icon(Icons.Default.ContentCopy, contentDescription = strings.copyListDesc, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.duplicate, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(strings.delete, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Delete, contentDescription = strings.deleteListDesc, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            ) {
                cardContent()
            }
        }
        BasketCardSection.HEADER_ONLY -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 2.dp)
            ) {
                cardContent()
            }
        }
        BasketCardSection.BODY_ONLY -> {
            cardContent()
        }
    }

    // Confirmation Dialog for Item Deletion
    itemPendingDelete?.let { (item, displayName) ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text(strings.removeItemTitle) },
            text = { Text(String.format(Locale.US, strings.removeItemConfirmFmt, displayName, list.name)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        onRemoveItem(item.barcode)
                        itemPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.remove)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BasketActionButtonsRow(
    showMixAndMatch: Boolean,
    onSetBudget: () -> Unit,
    onApplyVoucher: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onOpenMixAndMatch: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val actionColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.primary
    )
    val actionBorder = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
    Column {
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
                Text(strings.budgetBtn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
                Text(strings.voucherBtn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
                Text(strings.rename, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
                Text(strings.copy, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        if (showMixAndMatch) {
            Spacer(modifier = Modifier.height(6.dp))
            AppOutlinedButton(
                onClick = onOpenMixAndMatch,
                colors = actionColors,
                border = actionBorder,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.mixAndMatchBtn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasketItemRow(
    item: ShoppingListItemEntity,
    piece: PieceEntity?,
    currencySymbol: String,
    showOfferBadge: Boolean = true,
    displayQty: Int? = null,
    displayChecked: Boolean? = null,
    onToggleChecked: () -> Unit,
    onRequestDelete: () -> Unit,
    onAdjustQuantity: (Int) -> Unit,
    onEditItem: (PieceEntity) -> Unit = {},
    onRemoveOffer: () -> Unit = {},
) {
    var itemExpanded by remember { mutableStateOf(false) }

    val effOfferType = when (item.offerType) {
        "NO_OFFER" -> "NONE"
        "NONE" -> piece?.offerType ?: "NONE"
        else -> item.offerType
    }
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
    val shownQty = displayQty ?: item.quantity
    val shownChecked = displayChecked ?: item.isChecked
    val fullLineTotal = mergedPiece?.calculatedTotal ?: item.calculateItemTotal(null)
    val lineTotal = if (item.quantity > 0) fullLineTotal * shownQty.toDouble() / item.quantity.toDouble() else fullLineTotal
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
                            text = if (shownChecked) LocalAppStrings.current.uncheck else LocalAppStrings.current.check,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = LocalAppStrings.current.delete,
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
            color = if (shownChecked) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
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
                            imageVector = if (shownChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (shownChecked) LocalAppStrings.current.uncheckItemDesc else LocalAppStrings.current.checkItemDesc,
                            tint = if (shownChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (shownChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (shownChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${LocalAppStrings.current.qtyLabel} $shownQty",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (showOfferBadge && offerDesc.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 5.dp, end = 2.dp, top = 1.dp, bottom = 1.dp)
                                    ) {
                                        Text(
                                            text = "🏷️ $offerDesc",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = onRemoveOffer,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = LocalAppStrings.current.remove,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (lineTotal > 0) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f", currencySymbol, lineTotal),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (shownChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { itemExpanded = !itemExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (itemExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (itemExpanded) LocalAppStrings.current.collapseItemDesc else LocalAppStrings.current.expandItemDesc,
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
                            text = "${LocalAppStrings.current.barcodeLabel} ${item.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (piece != null && piece.location.isNotBlank() && piece.location != "Default Bin") {
                            Text(
                                text = "${LocalAppStrings.current.categoryLabel} ${piece.location}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lineSavings > 0) {
                            Text(
                                text = String.format(Locale.US, LocalAppStrings.current.savedWithDealFmt, currencySymbol, lineSavings),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = onRequestDelete,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = LocalAppStrings.current.removeItemDesc,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val editable = mergedPiece ?: PieceEntity(
                                            barcode = item.barcode,
                                            name = displayName,
                                            unitPrice = if (item.unitPrice > 0) item.unitPrice else 0.0,
                                            offerType = if (effOfferType == "NO_OFFER") "NONE" else effOfferType,
                                            offerBuyQty = effBuyQty,
                                            offerPayQty = effPayQty,
                                            offerFixedPrice = effFixedPrice,
                                            offerPercentOff = effPercentOff
                                        )
                                        onEditItem(editable)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = LocalAppStrings.current.edit,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = LocalAppStrings.current.qtyLabel,
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
                                    Icon(Icons.Default.Remove, contentDescription = LocalAppStrings.current.decreaseQtyDesc, modifier = Modifier.size(15.dp))
                                }
                                Text(
                                    text = "$shownQty",
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
                                    Icon(Icons.Default.Add, contentDescription = LocalAppStrings.current.increaseQtyDesc, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
