package com.inventory.barcodetracker.ui.screens.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.PieceEntity

@Composable
fun AddBomItemDialog(
    availablePieces: List<PieceEntity>,
    alreadyLinkedBarcodes: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (barcode: String, quantity: Int) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPiece by remember { mutableStateOf<PieceEntity?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    val filteredPieces = remember(availablePieces, searchQuery, alreadyLinkedBarcodes) {
        val trimmed = searchQuery.trim()
        availablePieces.filter { piece ->
            !alreadyLinkedBarcodes.contains(piece.barcode) && (
                    trimmed.isEmpty() ||
                            piece.name.contains(trimmed, ignoreCase = true) ||
                            piece.barcode.contains(trimmed, ignoreCase = true)
                    )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Part to BOM") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (selectedPiece == null) {
                    Text(
                        "Select a part to link to this product:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search parts...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredPieces.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No unlinked parts found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredPieces, key = { it.barcode }) { piece ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPiece = piece },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(piece.name, fontWeight = FontWeight.SemiBold)
                                            Text("Code: ${piece.barcode}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("${piece.stockQuantity} in stock", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Selected Part:", style = MaterialTheme.typography.labelSmall)
                            Text(selectedPiece!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Barcode: ${selectedPiece!!.barcode} • In stock: ${selectedPiece!!.stockQuantity}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity Required Per Product") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedPiece = null }) {
                        Text("Choose different part")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val piece = selectedPiece ?: return@Button
                    val qty = quantityText.toIntOrNull() ?: 1
                    onConfirm(piece.barcode, maxOf(1, qty))
                },
                enabled = selectedPiece != null && (quantityText.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add to BOM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
