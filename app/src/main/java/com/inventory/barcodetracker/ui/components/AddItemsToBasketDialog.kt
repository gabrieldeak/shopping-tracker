package com.inventory.barcodetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.ShoppingListEntity

@Composable
fun AddItemsToBasketDialog(
    shoppingLists: List<ShoppingListEntity>,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmAdd: (List<Long>) -> Unit,
) {
    val checkedListIds = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(shoppingLists) {
        if (checkedListIds.isEmpty()) {
            shoppingLists.forEach { checkedListIds[it.id] = true }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Shopping Basket(s)")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Select which shopping basket(s) to add $selectedCount selected item(s) into:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (shoppingLists.isEmpty()) {
                    Text(
                        "No shopping baskets exist yet on the main screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
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
            Button(
                onClick = {
                    val targetIds = checkedListIds.filterValues { it }.keys.toList()
                    onConfirmAdd(targetIds)
                },
                enabled = checkedListIds.any { it.value }
            ) {
                Text("Add Items")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
