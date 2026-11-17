package com.inventory.barcodetracker.ui.screens.offers

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import com.inventory.barcodetracker.ui.components.OfferDetailsEditor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.service.OfferRecognitionService
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.components.VerticalScrollbarContainer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDialog(
    initialOffer: OfferEntity? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (OfferEntity) -> Unit,
) {
    val isEditing = initialOffer != null
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(initialOffer?.title ?: "") }
    var storeName by remember { mutableStateOf(initialOffer?.storeName ?: "") }
    var notes by remember { mutableStateOf(initialOffer?.notes ?: "") }

    var offerType by remember { mutableStateOf(initialOffer?.offerType ?: "THREE_FOR_TWO") }
    var offerBuyQty by remember { mutableStateOf(initialOffer?.buyQty?.let { if (it == 0) "" else it.toString() } ?: "") }
    var offerPayQty by remember { mutableStateOf(initialOffer?.payQty?.let { if (it == 0) "" else it.toString() } ?: "") }
    var offerFixedPrice by remember { mutableStateOf(initialOffer?.fixedPrice?.let { if (it == 0.0) "" else String.format(Locale.US, "%.2f", it) } ?: "") }
    var offerPercentOff by remember { mutableStateOf(initialOffer?.percentOff?.let { if (it == 0.0) "" else String.format(Locale.US, "%.0f", it) } ?: "") }
    var detectedOfferMessage by remember { mutableStateOf<String?>(null) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val offerTypesMap = listOf(
        "THREE_FOR_TWO" to "Buy X Pay Y (e.g. 3 for 2)",
        "MULTI_BUY_FIXED" to "Multi-Buy Bundle (e.g. 2 for $currencySymbol 5)",
        "REDUCED_PRICE" to "Special Reduced Price (e.g. Reduced to $currencySymbol 2.50)",
        "PERCENT_OFF" to "Percentage Discount (e.g. 20% Off)",
        "VOUCHER" to "🎟️ Basket Discount Voucher ($currencySymbol Off)"
    )

    val currentTypeLabel = offerTypesMap.firstOrNull { it.first == offerType }?.second ?: "Select Promotion Type"

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Offer / Deal" else "Create New Store Offer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { cameraOfferLauncher.launch(null) }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Scan Offer Label with Camera",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Scrollable Form Fields with Functional Moving Scrollbar
                VerticalScrollbarContainer(
                    scrollState = scrollState,
                    maxHeight = 380.dp
                ) {
                    if (detectedOfferMessage != null) {
                        Text(
                            text = detectedOfferMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Promotion Type Dropdown (Required)
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppOutlinedTextField(
                            value = currentTypeLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Promotion Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            offerTypesMap.forEach { (typeKey, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        offerType = typeKey
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Dynamic Deal Parameters
                    OfferDetailsEditor(
                        offerType = offerType,
                        currencySymbol = currencySymbol,
                        offerBuyQty = offerBuyQty,
                        onBuyQtyChange = { offerBuyQty = it },
                        offerPayQty = offerPayQty,
                        onPayQtyChange = { offerPayQty = it },
                        offerFixedPrice = offerFixedPrice,
                        onFixedPriceChange = { offerFixedPrice = it },
                        offerPercentOff = offerPercentOff,
                        onPercentOffChange = { offerPercentOff = it }
                    )

                    AppOutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Offer Title / Name (Optional)") },
                        placeholder = { Text("e.g. Tesco 3 for 2 Grill Deal") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppOutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store / Supermarket (Optional)") },
                        placeholder = { Text("e.g. Tesco, Sainsbury's, Asda") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppOutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description (Optional)") },
                        placeholder = { Text("e.g. Valid until Sunday") },
                        keyboardOptions = KeyboardCapitalization.Sentences.let { KeyboardOptions(capitalization = it) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Fixed Action Buttons at Bottom of Card (Zero empty bottom space!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AppButton(
                        onClick = {
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
                }
            }
        }
    }
}
