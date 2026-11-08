package com.inventory.barcodetracker.ui.screens.offers

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.service.OfferRecognitionService
import com.inventory.barcodetracker.service.TextRecognitionService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferDialog(
    initialOffer: OfferEntity? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (OfferEntity) -> Unit,
) {
    val isEditing = initialOffer != null
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialOffer?.title ?: "") }
    var storeName by remember { mutableStateOf(initialOffer?.storeName ?: "") }
    var notes by remember { mutableStateOf(initialOffer?.notes ?: "") }

    var offerType by remember { mutableStateOf(initialOffer?.offerType ?: "THREE_FOR_TWO") }
    var offerBuyQty by remember { mutableStateOf(initialOffer?.buyQty?.let { if (it == 0) "3" else it.toString() } ?: "3") }
    var offerPayQty by remember { mutableStateOf(initialOffer?.payQty?.let { if (it == 0) "2" else it.toString() } ?: "2") }
    var offerFixedPrice by remember { mutableStateOf(initialOffer?.fixedPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }
    var offerPercentOff by remember { mutableStateOf(initialOffer?.percentOff?.let { if (it == 0.0) "" else String.format(Locale.US, "%.0f", it) } ?: "") }
    var detectedOfferMessage by remember { mutableStateOf<String?>(null) }

    var titleError by remember { mutableStateOf<String?>(null) }

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
                    if (title.isBlank()) title = recognizedOffer.detectedText
                } else {
                    detectedOfferMessage = "Could not match deal from label text. Select deal manually below."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isEditing) "Edit Offer / Deal" else "Create New Store Offer")
                IconButton(onClick = { cameraOfferLauncher.launch(null) }) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Scan Offer Label with Camera",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (detectedOfferMessage != null) {
                    Text(
                        text = detectedOfferMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Offer Title / Name *") },
                    placeholder = { Text("e.g. 3 for 2 Grill Deal, 2 for $currencySymbol 5") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store / Supermarket") },
                    placeholder = { Text("e.g. Tesco, Sainsbury's, Asda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Offer Promotion Deal Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = offerType == "THREE_FOR_TWO",
                        onClick = {
                            offerType = "THREE_FOR_TWO"
                            if (offerBuyQty.isBlank()) offerBuyQty = "3"
                            if (offerPayQty.isBlank()) offerPayQty = "2"
                        },
                        label = { Text("Buy X Pay Y (3 for 2)") }
                    )
                    FilterChip(
                        selected = offerType == "MULTI_BUY_FIXED",
                        onClick = {
                            offerType = "MULTI_BUY_FIXED"
                            if (offerBuyQty.isBlank()) offerBuyQty = "2"
                        },
                        label = { Text("X for $currencySymbol Y (2 for ${currencySymbol}5)") }
                    )
                    FilterChip(
                        selected = offerType == "REDUCED_PRICE",
                        onClick = { offerType = "REDUCED_PRICE" },
                        label = { Text("Reduced to $currencySymbol Y") }
                    )
                    FilterChip(
                        selected = offerType == "PERCENT_OFF",
                        onClick = {
                            offerType = "PERCENT_OFF"
                            if (offerPercentOff.isBlank()) offerPercentOff = "20"
                        },
                        label = { Text("% Off / Half Price") }
                    )
                }

                if (offerType == "THREE_FOR_TWO") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { offerBuyQty = "3"; offerPayQty = "2" },
                                label = { Text("3 for 2 (1 free)") }
                            )
                            AssistChip(
                                onClick = { offerBuyQty = "2"; offerPayQty = "1" },
                                label = { Text("2 for 1 (BOGO)") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty (X)") },
                                placeholder = { Text("3") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = offerPayQty,
                                onValueChange = { offerPayQty = it },
                                label = { Text("Pay Qty (Y)") },
                                placeholder = { Text("2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                if (offerType == "MULTI_BUY_FIXED") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { offerBuyQty = "2"; offerFixedPrice = "5.00" },
                                label = { Text("2 for ${currencySymbol}5") }
                            )
                            AssistChip(
                                onClick = { offerBuyQty = "3"; offerFixedPrice = "10.00" },
                                label = { Text("3 for ${currencySymbol}10") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty (X)") },
                                placeholder = { Text("2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = offerFixedPrice,
                                onValueChange = { offerFixedPrice = it },
                                label = { Text("Bundle Price ($currencySymbol Y)") },
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
                        label = { Text("Special Reduced Price ($currencySymbol)") },
                        placeholder = { Text("e.g. 2.50") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (offerType == "PERCENT_OFF") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { offerPercentOff = "50" },
                                label = { Text("50% (Half Price)") }
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
                            label = { Text("Discount Percentage (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Offer Description") },
                    placeholder = { Text("e.g. Valid until end of month, selected items only") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        // Generate fallback title if blank
                        val badge = when (offerType) {
                            "THREE_FOR_TWO" -> "${offerBuyQty.ifBlank { "3" }} for ${offerPayQty.ifBlank { "2" }} Offer"
                            "MULTI_BUY_FIXED" -> "${offerBuyQty.ifBlank { "2" }} for $currencySymbol${offerFixedPrice.ifBlank { "0.00" }}"
                            "REDUCED_PRICE" -> "Reduced to $currencySymbol${offerFixedPrice.ifBlank { "0.00" }}"
                            "PERCENT_OFF" -> "${offerPercentOff.ifBlank { "20" }}% Off"
                            else -> "Special Offer"
                        }
                        title = if (storeName.isNotBlank()) "$storeName $badge" else badge
                    }

                    val offer = OfferEntity(
                        id = initialOffer?.id ?: 0,
                        title = title.trim(),
                        offerType = offerType,
                        buyQty = offerBuyQty.toIntOrNull() ?: 0,
                        payQty = offerPayQty.toIntOrNull() ?: 0,
                        fixedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0,
                        percentOff = offerPercentOff.toDoubleOrNull() ?: 0.0,
                        storeName = storeName.trim(),
                        notes = notes.trim(),
                        createdAt = initialOffer?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(offer)
                }
            ) {
                Text("Save Offer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
