package com.inventory.barcodetracker.ui.screens.pieces

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.service.OfferRecognitionService
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.components.OcrTextEditDialog
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieceDialog(
    initialPiece: PieceEntity? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (PieceEntity) -> Unit,
) {
    val isEditing = initialPiece != null
    val coroutineScope = rememberCoroutineScope()

    var barcode by remember { mutableStateOf(initialPiece?.barcode ?: "") }
    var name by remember { mutableStateOf(initialPiece?.name ?: "") }
    var description by remember { mutableStateOf(initialPiece?.description ?: "") }
    var category by remember { mutableStateOf(initialPiece?.location ?: "Grocery") }
    var stockQuantity by remember { mutableStateOf(initialPiece?.stockQuantity?.toString() ?: "1") }
    var unitPrice by remember { mutableStateOf(initialPiece?.unitPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }

    var offerType by remember { mutableStateOf(initialPiece?.offerType ?: "NONE") }
    var offerBuyQty by remember { mutableStateOf(initialPiece?.offerBuyQty?.let { if (it == 0) "" else it.toString() } ?: "") }
    var offerPayQty by remember { mutableStateOf(initialPiece?.offerPayQty?.let { if (it == 0) "" else it.toString() } ?: "") }
    var offerFixedPrice by remember { mutableStateOf(initialPiece?.offerFixedPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }
    var offerPercentOff by remember { mutableStateOf(initialPiece?.offerPercentOff?.let { if (it == 0.0) "" else String.format(Locale.US, "%.0f", it) } ?: "") }
    var detectedOfferMessage by remember { mutableStateOf<String?>(null) }

    var barcodeError by remember { mutableStateOf<String?>(null) }
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

    val cameraOfferLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            coroutineScope.launch {
                val lines = TextRecognitionService.processImage(bmp)
                val recognizedOffer = OfferRecognitionService.parseOfferFromText(lines)
                if (recognizedOffer != null) {
                    offerType = recognizedOffer.offerType
                    if (recognizedOffer.buyQty > 0) offerBuyQty = recognizedOffer.buyQty.toString()
                    if (recognizedOffer.payQty > 0) offerPayQty = recognizedOffer.payQty.toString()
                    if (recognizedOffer.fixedPrice > 0.0) offerFixedPrice = String.format(Locale.US, "%.2f", recognizedOffer.fixedPrice)
                    if (recognizedOffer.percentOff > 0.0) offerPercentOff = String.format(Locale.US, "%.0f", recognizedOffer.percentOff)
                    detectedOfferMessage = "Auto-detected offer: \"${recognizedOffer.detectedText}\""
                } else {
                    detectedOfferMessage = "Could not match deal from label text. Select deal manually below."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Shopping Item" else "Add New Shopping Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = {
                        barcode = it
                        if (it.isNotBlank()) barcodeError = null
                    },
                    label = { Text("Barcode / Item Code *") },
                    enabled = !isEditing,
                    isError = barcodeError != null,
                    supportingText = barcodeError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Item Name *") },
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

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Price ($currencySymbol)") },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("Quantity in Cart") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category / Store Section") },
                    placeholder = { Text("e.g. Dairy, Produce, Bakery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Brand") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Special Store Offer Section ---
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Store Special Offer / Multi-Buy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { cameraOfferLauncher.launch(null) }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Scan Offer Label with Camera",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (detectedOfferMessage != null) {
                    Text(
                        text = detectedOfferMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = offerType == "NONE",
                        onClick = { offerType = "NONE" },
                        label = { Text("No Offer") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = offerType == "THREE_FOR_TWO",
                        onClick = {
                            offerType = "THREE_FOR_TWO"
                            if (offerBuyQty.isBlank()) offerBuyQty = "3"
                            if (offerPayQty.isBlank()) offerPayQty = "2"
                        },
                        label = { Text("Buy X Pay Y") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = offerType == "MULTI_BUY_FIXED",
                        onClick = {
                            offerType = "MULTI_BUY_FIXED"
                            if (offerBuyQty.isBlank()) offerBuyQty = "2"
                        },
                        label = { Text("X for $currencySymbol Y") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = offerType == "REDUCED_PRICE",
                        onClick = { offerType = "REDUCED_PRICE" },
                        label = { Text("Reduced to $currencySymbol Y") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = offerType == "PERCENT_OFF",
                        onClick = {
                            offerType = "PERCENT_OFF"
                            if (offerPercentOff.isBlank()) offerPercentOff = "20"
                        },
                        label = { Text("% Discount") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (offerType == "THREE_FOR_TWO") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { offerBuyQty = "3"; offerPayQty = "2" },
                                label = { Text("3 for 2 (1 free)") }
                            )
                            AssistChip(
                                onClick = { offerBuyQty = "2"; offerPayQty = "1" },
                                label = { Text("2 for 1 (BOGO)") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty") },
                                placeholder = { Text("3") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = offerPayQty,
                                onValueChange = { offerPayQty = it },
                                label = { Text("Pay Qty") },
                                placeholder = { Text("2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                if (offerType == "MULTI_BUY_FIXED") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { offerBuyQty = "2"; offerFixedPrice = "5.00" },
                                label = { Text("2 for ${currencySymbol}5") }
                            )
                            AssistChip(
                                onClick = { offerBuyQty = "3"; offerFixedPrice = "10.00" },
                                label = { Text("3 for ${currencySymbol}10") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty") },
                                placeholder = { Text("2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = offerFixedPrice,
                                onValueChange = { offerFixedPrice = it },
                                label = { Text("Bundle Price") },
                                prefix = { Text(currencySymbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                if (offerType == "REDUCED_PRICE") {
                    OutlinedTextField(
                        value = offerFixedPrice,
                        onValueChange = { offerFixedPrice = it },
                        label = { Text("Special Reduced Price") },
                        placeholder = { Text("e.g. 2.50") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            val standardPrice = unitPrice.toDoubleOrNull() ?: 0.0
                            val reducedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0
                            if (standardPrice > 0.0 && reducedPrice > 0.0) {
                                val savingPerUnit = standardPrice - reducedPrice
                                if (savingPerUnit > 0) {
                                    Text("Standard: $currencySymbol${String.format(Locale.US, "%.2f", standardPrice)} -> Save $currencySymbol${String.format(Locale.US, "%.2f", savingPerUnit)} per item!", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }

                if (offerType == "PERCENT_OFF") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { offerPercentOff = "50" },
                                label = { Text("50% Off") }
                            )
                            AssistChip(
                                onClick = { offerPercentOff = "20" },
                                label = { Text("20% Off") }
                            )
                            AssistChip(
                                onClick = { offerPercentOff = "25" },
                                label = { Text("25% Off") }
                            )
                        }
                        OutlinedTextField(
                            value = offerPercentOff,
                            onValueChange = { offerPercentOff = it },
                            label = { Text("Discount Percentage") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (barcode.isBlank()) {
                        barcodeError = "Barcode is required"
                        hasError = true
                    }
                    if (name.isBlank()) {
                        nameError = "Item name is required"
                        hasError = true
                    }
                    if (hasError) return@Button

                    val piece = PieceEntity(
                        barcode = barcode.trim(),
                        name = name.trim(),
                        description = description.trim(),
                        location = if (category.isBlank()) "Grocery" else category.trim(),
                        stockQuantity = stockQuantity.toIntOrNull() ?: 1,
                        minStockThreshold = 0,
                        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                        offerType = offerType,
                        offerBuyQty = offerBuyQty.toIntOrNull() ?: 0,
                        offerPayQty = offerPayQty.toIntOrNull() ?: 0,
                        offerFixedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0,
                        offerPercentOff = offerPercentOff.toDoubleOrNull() ?: 0.0,
                        createdAt = initialPiece?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(piece)
                }
            ) {
                Text("Save Item")
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
