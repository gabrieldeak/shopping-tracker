package com.inventory.barcodetracker.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import java.util.Locale

@Composable
fun ProductDialog(
    initialProduct: ProductEntity? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
) {
    val isEditing = initialProduct != null

    var sku by remember { mutableStateOf(initialProduct?.productSku ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var stockQuantity by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "0") }
    var targetCount by remember { mutableStateOf(initialProduct?.targetProductionCount?.toString() ?: "10") }
    var unitPrice by remember { mutableStateOf(initialProduct?.unitPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }

    var skuError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Product" else "New Assembled Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = {
                        sku = it
                        if (it.isNotBlank()) skuError = null
                    },
                    label = { Text("Product SKU / Barcode *") },
                    enabled = !isEditing,
                    isError = skuError != null,
                    supportingText = skuError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Product Name *") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Selling Price ($currencySymbol)") },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Built Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetCount,
                        onValueChange = { targetCount = it },
                        label = { Text("Target Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (sku.isBlank()) {
                        skuError = "SKU is required"
                        hasError = true
                    }
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        hasError = true
                    }
                    if (hasError) return@Button

                    val product = ProductEntity(
                        productSku = sku.trim(),
                        name = name.trim(),
                        description = description.trim(),
                        stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                        targetProductionCount = targetCount.toIntOrNull() ?: 0,
                        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                        createdAt = initialProduct?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(product)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
