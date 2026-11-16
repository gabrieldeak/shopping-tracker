package com.inventory.barcodetracker.ui.screens.products

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.components.OcrTextEditDialog
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProductDialog(
    initialProduct: ProductEntity? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
) {
    val isEditing = initialProduct != null
    val coroutineScope = rememberCoroutineScope()

    var sku by remember { mutableStateOf(initialProduct?.productSku ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var stockQuantity by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "0") }
    var targetCount by remember { mutableStateOf(initialProduct?.targetProductionCount?.toString() ?: "10") }
    var unitPrice by remember { mutableStateOf(initialProduct?.unitPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }

    var skuError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    var ocrTextCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var editableOcrText by remember { mutableStateOf("") }
    var showOcrChoiceDialog by remember { mutableStateOf(false) }

    val cameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            coroutineScope.launch {
                val detected = TextRecognitionService.processImage(bmp)
                if (detected.isNotEmpty()) {
                    ocrTextCandidates = detected
                    editableOcrText = detected.joinToString(" ")
                    showOcrChoiceDialog = true
                }
            }
        }
    }

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
                AppOutlinedTextField(
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

                AppOutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Product Name *") },
                    trailingIcon = {
                        IconButton(onClick = { cameraOcrLauncher.launch(null) }) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Scan Product Name with Camera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AppOutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                AppOutlinedTextField(
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
                    AppOutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Built Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    AppOutlinedTextField(
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
            AppButton(
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
                    if (hasError) return@AppButton

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

    if (showOcrChoiceDialog) {
        OcrTextEditDialog(
            initialText = editableOcrText,
            linesDetected = ocrTextCandidates,
            onDismiss = { showOcrChoiceDialog = false },
            onConfirm = { editedName ->
                name = editedName
                if (editedName.isNotBlank()) nameError = null
                showOcrChoiceDialog = false
            }
        )
    }
}
