package com.inventory.barcodetracker.ui.screens.scan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppFilledTonalButton
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import com.inventory.barcodetracker.ui.components.OfferSelectorDropdown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.service.OfferRecognitionService
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.components.OcrTextEditDialog
import com.inventory.barcodetracker.ui.theme.StockLowRed
import com.inventory.barcodetracker.ui.theme.StockLowRedContainer
import com.inventory.barcodetracker.ui.theme.StockOkGreen
import com.inventory.barcodetracker.ui.theme.StockOkGreenContainer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResolutionSheet(
    state: ScannedItemState,
    currencySymbol: String = "$",
    savedOffers: List<OfferEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAdjustPieceStock: (barcode: String, delta: Int) -> Unit,
    onAdjustProductStock: (sku: String, delta: Int) -> Unit,
    onCreatePiece: (PieceEntity) -> Unit,
    onCreateProduct: (ProductEntity) -> Unit,
    onNavigateToProductBom: (sku: String) -> Unit,
) {
    if (state is ScannedItemState.Idle) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (state) {
                is ScannedItemState.FoundPiece -> {
                    PieceResolutionContent(
                        piece = state.piece,
                        currencySymbol = currencySymbol,
                        savedOffers = savedOffers,
                        onAdjustStock = { delta -> onAdjustPieceStock(state.piece.barcode, delta) },
                        onUpdatePiece = onCreatePiece,
                        onDismiss = onDismiss
                    )
                }
                is ScannedItemState.FoundProduct -> {
                    ProductResolutionContent(
                        product = state.product,
                        onAdjustStock = { delta -> onAdjustProductStock(state.product.productSku, delta) },
                        onViewBom = { onNavigateToProductBom(state.product.productSku) },
                        onDismiss = onDismiss
                    )
                }
                is ScannedItemState.NotFound -> {
                    NotFoundResolutionContent(
                        scannedBarcode = state.barcode,
                        initialName = state.initialName,
                        initialBrand = state.initialBrand,
                        initialCategory = state.initialCategory,
                        currencySymbol = currencySymbol,
                        savedOffers = savedOffers,
                        onCreatePiece = { piece ->
                            onCreatePiece(piece)
                        },
                        onCreateProduct = { product ->
                            onCreateProduct(product)
                        },
                        onDismiss = onDismiss
                    )
                }
                ScannedItemState.Idle -> {}
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieceResolutionContent(
    piece: PieceEntity,
    currencySymbol: String = "$",
    savedOffers: List<OfferEntity> = emptyList(),
    onAdjustStock: (Int) -> Unit,
    onUpdatePiece: (PieceEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showOfferEditor by remember { mutableStateOf(false) }

    var offerType by remember(piece) { mutableStateOf(piece.offerType) }
    var offerBuyQty by remember(piece) { mutableStateOf(if (piece.offerBuyQty == 0) "" else piece.offerBuyQty.toString()) }
    var offerPayQty by remember(piece) { mutableStateOf(if (piece.offerPayQty == 0) "" else piece.offerPayQty.toString()) }
    var offerFixedPrice by remember(piece) { mutableStateOf(if (piece.offerFixedPrice == 0.0) "" else String.format(Locale.US, "%.2f", piece.offerFixedPrice)) }
    var offerPercentOff by remember(piece) { mutableStateOf(if (piece.offerPercentOff == 0.0) "" else String.format(Locale.US, "%.0f", piece.offerPercentOff)) }
    var detectedOfferMessage by remember { mutableStateOf<String?>(null) }

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Item Identified",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = piece.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Barcode: ${piece.barcode}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (piece.description.isNotBlank()) {
                Text(piece.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (piece.unitPrice > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "Unit Price: %s%.2f", currencySymbol, piece.unitPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (piece.offerType != "NONE" && piece.offerDescription.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏷️ ${piece.offerDescription}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Cart Quantity & Total Cost Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Quantity in Shopping Basket",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${piece.stockQuantity} in cart",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (piece.unitPrice > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Item Subtotal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currencySymbol, piece.calculatedTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (piece.savings > 0) {
                        Text(
                            text = String.format(Locale.US, "Saved %s%.2f", currencySymbol, piece.savings),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Quick Offer Toggle / Manage Offer Button
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (showOfferEditor) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (piece.offerType == "NONE") "Add Store Special Offer / Deal" else "Active Offer: ${piece.offerDescription}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = { showOfferEditor = !showOfferEditor }) {
                    Text(if (showOfferEditor) "Hide" else if (piece.offerType == "NONE") "+ Set Offer" else "Edit Offer")
                }
            }

            if (showOfferEditor) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Store Special Offer / Multi-Buy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
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
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OfferSelectorDropdown(
                    offerType = offerType,
                    currencySymbol = currencySymbol,
                    savedOffers = savedOffers,
                    onSelectBuiltInType = { typeKey ->
                        offerType = typeKey
                        when (typeKey) {
                            "THREE_FOR_TWO" -> {
                                if (offerBuyQty.isBlank()) offerBuyQty = "3"
                                if (offerPayQty.isBlank()) offerPayQty = "2"
                            }
                            "MULTI_BUY_FIXED" -> {
                                if (offerBuyQty.isBlank()) offerBuyQty = "2"
                            }
                            "PERCENT_OFF" -> {
                                if (offerPercentOff.isBlank()) offerPercentOff = "20"
                            }
                        }
                        detectedOfferMessage = null
                    },
                    onSelectSavedOffer = { offer ->
                        offerType = offer.offerType
                        offerBuyQty = if (offer.buyQty > 0) offer.buyQty.toString() else ""
                        offerPayQty = if (offer.payQty > 0) offer.payQty.toString() else ""
                        offerFixedPrice = if (offer.fixedPrice > 0.0) String.format(Locale.US, "%.2f", offer.fixedPrice) else ""
                        offerPercentOff = if (offer.percentOff > 0.0) String.format(Locale.US, "%.0f", offer.percentOff) else ""
                        detectedOfferMessage = "Selected deal: \"${offer.title}\""
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            AppOutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty") },
                                placeholder = { Text("3") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            AppOutlinedTextField(
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
                            AppOutlinedTextField(
                                value = offerBuyQty,
                                onValueChange = { offerBuyQty = it },
                                label = { Text("Buy Qty") },
                                placeholder = { Text("2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            AppOutlinedTextField(
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
                    AppOutlinedTextField(
                        value = offerFixedPrice,
                        onValueChange = { offerFixedPrice = it },
                        label = { Text("Special Reduced Price") },
                        placeholder = { Text("e.g. 2.50") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                        AppOutlinedTextField(
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

                Spacer(modifier = Modifier.height(10.dp))

                AppButton(
                    onClick = {
                        val updatedPiece = piece.copy(
                            offerType = offerType,
                            offerBuyQty = offerBuyQty.toIntOrNull() ?: 0,
                            offerPayQty = offerPayQty.toIntOrNull() ?: 0,
                            offerFixedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0,
                            offerPercentOff = offerPercentOff.toDoubleOrNull() ?: 0.0
                        )
                        onUpdatePiece(updatedPiece)
                        showOfferEditor = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Save Offer")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Adjust Quantity in Cart",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppOutlinedButton(
            onClick = { onAdjustStock(-1) },
            modifier = Modifier.weight(1f),
            enabled = piece.stockQuantity > 0
        ) {
            Text("- 1", fontWeight = FontWeight.Bold)
        }

        AppOutlinedButton(
            onClick = { onAdjustStock(-5) },
            modifier = Modifier.weight(1f),
            enabled = piece.stockQuantity >= 5
        ) {
            Text("- 5", fontWeight = FontWeight.Bold)
        }

        AppButton(
            onClick = { onAdjustStock(+1) },
            modifier = Modifier.weight(1f)
        ) {
            Text("+ 1", fontWeight = FontWeight.Bold)
        }

        AppButton(
            onClick = { onAdjustStock(+5) },
            modifier = Modifier.weight(1f)
        ) {
            Text("+ 5", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProductResolutionContent(
    product: ProductEntity,
    onAdjustStock: (Int) -> Unit,
    onViewBom: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Assembled Product",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("SKU: ${product.productSku}", style = MaterialTheme.typography.bodyMedium)
    if (product.description.isNotBlank()) {
        Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Stock In Hand", style = MaterialTheme.typography.labelMedium)
                Text("${product.stockQuantity} units", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Target Production", style = MaterialTheme.typography.labelMedium)
                Text("${product.targetProductionCount} units", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppOutlinedButton(
            onClick = { onAdjustStock(-1) },
            modifier = Modifier.weight(1f),
            enabled = product.stockQuantity > 0
        ) {
            Text("- 1", fontWeight = FontWeight.Bold)
        }

        AppButton(
            onClick = { onAdjustStock(+1) },
            modifier = Modifier.weight(1f)
        ) {
            Text("+ 1", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AppFilledTonalButton(
        onClick = {
            onDismiss()
            onViewBom()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("View Product Components & Build Capacity")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotFoundResolutionContent(
    scannedBarcode: String,
    initialName: String = "",
    initialBrand: String = "",
    initialCategory: String = "Grocery",
    currencySymbol: String = "$",
    savedOffers: List<OfferEntity> = emptyList(),
    onCreatePiece: (PieceEntity) -> Unit,
    onCreateProduct: (ProductEntity) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember(scannedBarcode, initialName) { mutableStateOf(initialName) }
    var unitPriceText by remember { mutableStateOf("") }
    var initialQuantity by remember { mutableStateOf("1") }
    var category by remember(scannedBarcode, initialCategory) { mutableStateOf(initialCategory.ifEmpty { "Grocery" }) }
    var notes by remember(scannedBarcode, initialBrand) { mutableStateOf(initialBrand) }

    var offerType by remember { mutableStateOf("NONE") }
    var offerBuyQty by remember { mutableStateOf("") }
    var offerPayQty by remember { mutableStateOf("") }
    var offerFixedPrice by remember { mutableStateOf("") }
    var offerPercentOff by remember { mutableStateOf("") }
    var detectedOfferMessage by remember { mutableStateOf<String?>(null) }

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Unrecognized Barcode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = scannedBarcode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AppOutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Item Name *") },
        placeholder = { Text("e.g. Steak Burgers, Olive Oil") },
        trailingIcon = {
            IconButton(onClick = { cameraOcrLauncher.launch(null) }) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Scan Product Name with Camera",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppOutlinedTextField(
            value = unitPriceText,
            onValueChange = { unitPriceText = it },
            label = { Text("Price ($currencySymbol)") },
            prefix = { Text(currencySymbol) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        AppOutlinedTextField(
            value = initialQuantity,
            onValueChange = { initialQuantity = it },
            label = { Text("Quantity in Cart") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    AppOutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category / Store Section") },
        placeholder = { Text("e.g. Dairy, Produce, Bakery") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    AppOutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes / Brand") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Store Special Offer / Multi-Buy Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Store Special Offer / Deal (Optional)",
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
                Spacer(modifier = Modifier.height(4.dp))
            }

            OfferSelectorDropdown(
                offerType = offerType,
                currencySymbol = currencySymbol,
                savedOffers = savedOffers,
                onSelectBuiltInType = { typeKey ->
                    offerType = typeKey
                    when (typeKey) {
                        "THREE_FOR_TWO" -> {
                            if (offerBuyQty.isBlank()) offerBuyQty = "3"
                            if (offerPayQty.isBlank()) offerPayQty = "2"
                        }
                        "MULTI_BUY_FIXED" -> {
                            if (offerBuyQty.isBlank()) offerBuyQty = "2"
                        }
                        "PERCENT_OFF" -> {
                            if (offerPercentOff.isBlank()) offerPercentOff = "20"
                        }
                    }
                    detectedOfferMessage = null
                },
                onSelectSavedOffer = { offer ->
                    offerType = offer.offerType
                    offerBuyQty = if (offer.buyQty > 0) offer.buyQty.toString() else ""
                    offerPayQty = if (offer.payQty > 0) offer.payQty.toString() else ""
                    offerFixedPrice = if (offer.fixedPrice > 0.0) String.format(Locale.US, "%.2f", offer.fixedPrice) else ""
                    offerPercentOff = if (offer.percentOff > 0.0) String.format(Locale.US, "%.0f", offer.percentOff) else ""
                    detectedOfferMessage = "Selected deal: \"${offer.title}\""
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                        AppOutlinedTextField(
                            value = offerBuyQty,
                            onValueChange = { offerBuyQty = it },
                            label = { Text("Buy Qty (X)") },
                            placeholder = { Text("3") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        AppOutlinedTextField(
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
                        AppOutlinedTextField(
                            value = offerBuyQty,
                            onValueChange = { offerBuyQty = it },
                            label = { Text("Buy Qty (X)") },
                            placeholder = { Text("2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        AppOutlinedTextField(
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
                AppOutlinedTextField(
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
                    AppOutlinedTextField(
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
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    AppButton(
        onClick = {
            if (name.isBlank()) return@AppButton
            val qty = initialQuantity.toIntOrNull() ?: 1
            val price = unitPriceText.toDoubleOrNull() ?: 0.0
            onCreatePiece(
                PieceEntity(
                    barcode = scannedBarcode,
                    name = name.trim(),
                    description = notes.trim(),
                    location = if (category.isBlank()) "Grocery" else category.trim(),
                    stockQuantity = maxOf(1, qty),
                    minStockThreshold = 0,
                    unitPrice = price,
                    offerType = offerType,
                    offerBuyQty = offerBuyQty.toIntOrNull() ?: 0,
                    offerPayQty = offerPayQty.toIntOrNull() ?: 0,
                    offerFixedPrice = offerFixedPrice.toDoubleOrNull() ?: 0.0,
                    offerPercentOff = offerPercentOff.toDoubleOrNull() ?: 0.0
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = name.isNotBlank()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Item & Add to Cart")
    }

    if (showOcrChoiceDialog) {
        OcrTextEditDialog(
            initialText = editableOcrText,
            linesDetected = ocrTextCandidates,
            onDismiss = { showOcrChoiceDialog = false },
            onConfirm = { editedName ->
                name = editedName
                showOcrChoiceDialog = false
            }
        )
    }
}
