package com.inventory.barcodetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

@Composable
fun AddItemsToBasketDialog(
    shoppingLists: List<ShoppingListEntity>,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmAdd: (List<Long>, Int) -> Unit,
    initialCheckedListIds: Set<Long>? = null,
) {
    val strings = LocalAppStrings.current
    val checkedListIds = remember { mutableStateMapOf<Long, Boolean>() }
    var quantityToAdd by remember { mutableIntStateOf(1) }

    LaunchedEffect(shoppingLists, initialCheckedListIds) {
        if (checkedListIds.isEmpty()) {
            val preselected = initialCheckedListIds?.takeIf { it.isNotEmpty() }
            shoppingLists.forEach { list ->
                checkedListIds[list.id] = preselected?.contains(list.id) ?: true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addToBasketsTitle)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    String.format(Locale.US, strings.addToBasketsPromptFmt, selectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (shoppingLists.isEmpty()) {
                    Text(
                        strings.noBasketsExistYet,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.qtyLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { if (quantityToAdd > 1) quantityToAdd-- },
                                    enabled = quantityToAdd > 1,
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = strings.decreaseQtyDesc, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "$quantityToAdd",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                FilledIconButton(
                                    onClick = { quantityToAdd++ },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = strings.increaseQtyDesc, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(shoppingLists, key = { it.id }) { list ->
                            val isChecked = checkedListIds[list.id] == true
                            Card(
                                onClick = { checkedListIds[list.id] = !isChecked },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checkedListIds[list.id] = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        list.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val targetIds = checkedListIds.filterValues { it }.keys.toList()
                    onConfirmAdd(targetIds, quantityToAdd)
                },
                enabled = checkedListIds.any { it.value }
            ) {
                Text(strings.addItems)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
