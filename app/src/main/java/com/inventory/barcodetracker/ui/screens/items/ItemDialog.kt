package com.inventory.barcodetracker.ui.screens.items

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import com.inventory.barcodetracker.ui.components.OfferDetailsEditor
import com.inventory.barcodetracker.ui.components.OfferSelectorDropdown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.scanner.ScanResult
import com.inventory.barcodetracker.service.OfferRecognitionService
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.components.OcrTextEditDialog
import com.inventory.barcodetracker.ui.components.VerticalScrollbarContainer
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemDialog(
    initialPiece: PieceEntity? = null,
    currencySymbol: String = "$",
    savedOffers: List<OfferEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (PieceEntity, Boolean) -> Unit,
) {
    val strings = LocalAppStrings.current
    val isEditing = initialPiece != null
    val context = LocalContext.current
    val barcodeScannerService = remember(context) { BarcodeScannerService(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var barcode by remember { mutableStateOf(initialPiece?.barcode ?: "") }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
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
    var offerFieldTopPx by remember { mutableIntStateOf(0) }

    var nameError by remember { mutableStateOf<String?>(null) }

    var ocrTextCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var editableOcrText by remember { mutableStateOf("") }
    var showOcrChoiceDialog by remember { mutableStateOf(false) }
    var pendingPieceToSave by remember { mutableStateOf<PieceEntity?>(null) }

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
                    detectedOfferMessage = String.format(Locale.US, strings.autoDetectedOfferFmt, recognizedOffer.detectedText)
                } else {
                    detectedOfferMessage = strings.couldNotMatchDeal
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
                Text(
                    text = if (isEditing) strings.editShoppingItem else strings.addNewShoppingItem,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Scrollable Form Fields with Functional Moving Scrollbar
                VerticalScrollbarContainer(
                    scrollState = scrollState,
                    maxHeight = 380.dp
                ) {
                    if (isEditing) {
                        AppOutlinedTextField(
                            value = barcode,
                            onValueChange = {},
                            label = { Text(strings.barcodeItemCodeLabel) },
                            enabled = false,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (barcode.isEmpty()) {
                                            Text(
                                                text = strings.barcodeItemCodeOptional,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1
                                            )
                                        }
                                        BasicTextField(
                                            value = barcode,
                                            onValueChange = {
                                                barcode = it
                                                scanErrorMessage = null
                                            },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (barcode.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                barcode = ""
                                                scanErrorMessage = null
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = strings.clearBarcodeDesc,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            coroutineScope.launch {
                                                when (val result = barcodeScannerService.startScan()) {
                                                    is ScanResult.Success -> {
                                                        barcode = result.barcode
                                                        scanErrorMessage = null
                                                    }
                                                    is ScanResult.Failure -> {
                                                        scanErrorMessage = result.message
                                                    }
                                                    is ScanResult.Cancelled -> Unit
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = if (barcode.isEmpty()) 14.dp else 10.dp,
                                                vertical = 10.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = strings.scanBarcodeDesc,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            AnimatedVisibility(visible = barcode.isEmpty()) {
                                                Row {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = strings.scanButton,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (scanErrorMessage != null) {
                                Text(
                                    text = scanErrorMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            } else {
                                Text(
                                    text = strings.autoGeneratedHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    AppOutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = null
                        },
                        label = { Text(strings.itemNameRequiredLabel) },
                        trailingIcon = {
                            IconButton(onClick = { cameraOcrLauncher.launch(null) }) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = strings.scanProductNameDesc,
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
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = { Text(String.format(Locale.US, strings.priceLabelFmt, currencySymbol)) },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppOutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(strings.categorySectionLabel) },
                        placeholder = { Text(strings.categoryPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppOutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(strings.notesBrandLabel) },
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
                            text = strings.storeSpecialOfferTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { cameraOfferLauncher.launch(null) }) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = strings.scanOfferLabelDesc,
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

                    OfferSelectorDropdown(
                        offerType = offerType,
                        currencySymbol = currencySymbol,
                        savedOffers = savedOffers,
                        onSelectBuiltInType = { typeKey ->
                            offerType = typeKey
                            detectedOfferMessage = null
                            if (typeKey == "NONE") {
                                offerBuyQty = ""
                                offerPayQty = ""
                                offerFixedPrice = ""
                                offerPercentOff = ""
                            }
                        },
                        onSelectSavedOffer = { offer ->
                            offerType = offer.offerType
                            offerBuyQty = if (offer.buyQty > 0) offer.buyQty.toString() else ""
                            offerPayQty = if (offer.payQty > 0) offer.payQty.toString() else ""
                            offerFixedPrice = if (offer.fixedPrice > 0.0) String.format(Locale.US, "%.2f", offer.fixedPrice) else ""
                            offerPercentOff = if (offer.percentOff > 0.0) String.format(Locale.US, "%.0f", offer.percentOff) else ""
                            detectedOfferMessage = String.format(Locale.US, strings.selectedDealFmt, offer.title)
                        },
                        onExpanded = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(120)
                                scrollState.animateScrollTo(offerFieldTopPx)
                            }
                        },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            offerFieldTopPx = coords.positionInParent().y.toInt()
                        }
                    )

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
                        onPercentOffChange = { offerPercentOff = it },
                        standardUnitPrice = unitPrice.toDoubleOrNull() ?: 0.0
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
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AppButton(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = strings.itemNameRequiredError
                                return@AppButton
                            }

                            val finalBarcode = if (barcode.isBlank()) "ITEM_${System.currentTimeMillis()}" else barcode.trim()
                            val parsedUnitPrice = unitPrice.toDoubleOrNull() ?: 0.0

                            val piece = PieceEntity(
                                barcode = finalBarcode,
                                name = name.trim(),
                                description = description.trim(),
                                location = if (category.isBlank()) "Grocery" else category.trim(),
                                stockQuantity = stockQuantity.toIntOrNull() ?: 1,
                                minStockThreshold = 0,
                                unitPrice = parsedUnitPrice,
                                offerType = offerType,
                                offerBuyQty = offerBuyQty.toIntOrNull() ?: 0,
                                offerPayQty = offerPayQty.toIntOrNull() ?: 0,
                                offerFixedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0,
                                offerPercentOff = offerPercentOff.toDoubleOrNull() ?: 0.0,
                                createdAt = initialPiece?.createdAt ?: System.currentTimeMillis()
                            )

                            onSave(piece, true)
                        }
                    ) {
                        Text(strings.saveItem)
                    }
                }
            }
        }
    }

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

    pendingPieceToSave?.let { piece ->
        AlertDialog(
            onDismissRequest = { pendingPieceToSave = null },
            title = { Text(strings.updatePriceInBasketsTitle) },
            text = {
                Text(
                    String.format(Locale.US, strings.updatePriceInBasketsBodyFmt, piece.name, currencySymbol, piece.unitPrice)
                )
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        onSave(piece, true)
                        pendingPieceToSave = null
                    }
                ) {
                    Text(strings.updateBasketsAndSave)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { pendingPieceToSave = null }) {
                        Text(strings.cancel)
                    }
                    TextButton(
                        onClick = {
                            onSave(piece, false)
                            pendingPieceToSave = null
                        }
                    ) {
                        Text(strings.saveItemOnly)
                    }
                }
            }
        )
    }
}
