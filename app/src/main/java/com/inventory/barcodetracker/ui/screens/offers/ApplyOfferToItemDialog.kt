package com.inventory.barcodetracker.ui.screens.offers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyOfferToItemDialog(
    offer: OfferEntity,
    pieces: List<PieceEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onApplyToItem: (barcode: String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredPieces = remember(searchQuery, pieces) {
        if (searchQuery.isBlank()) {
            pieces
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            pieces.filter {
                it.name.lowercase(Locale.ROOT).contains(q) ||
                    it.barcode.lowercase(Locale.ROOT).contains(q) ||
                    it.location.lowercase(Locale.ROOT).contains(q) ||
                    it.description.lowercase(Locale.ROOT).contains(q) ||
                    it.offerDescription.lowercase(Locale.ROOT).contains(q) ||
                    it.offerType.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(strings.applyOfferToProduct)
                Text(
                    text = if (offer.title.isNotBlank()) "${offer.title} (${offer.getDisplayBadge(currencySymbol)})" else offer.getDisplayBadge(currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .imePadding()
            ) {
                AppOutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchProductPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = strings.clearAction)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredPieces.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.noProductsFound,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPieces, key = { it.barcode }) { piece ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onApplyToItem(piece.barcode)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(piece.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            val priceText = if (piece.unitPrice > 0) " • $currencySymbol${String.format(Locale.US, "%.2f", piece.unitPrice)}" else ""
                                            Text("${strings.codePrefix} ${piece.barcode}$priceText", style = MaterialTheme.typography.labelSmall)
                                            if (piece.offerType != "NONE" && piece.offerDescription.isNotBlank()) {
                                                Text(String.format(Locale.US, strings.activeOfferFmt, piece.offerDescription), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    AppButton(
                                        onClick = { onApplyToItem(piece.barcode) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(strings.applyDeal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}
