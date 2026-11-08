package com.inventory.barcodetracker.ui.screens.scan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.service.TextRecognitionService
import com.inventory.barcodetracker.ui.theme.StockLowRed
import com.inventory.barcodetracker.ui.theme.StockLowRedContainer
import com.inventory.barcodetracker.ui.theme.StockOkGreen
import com.inventory.barcodetracker.ui.theme.StockOkGreenContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResolutionSheet(
    state: ScannedItemState,
    currencySymbol: String = "$",
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
                        onAdjustStock = { delta -> onAdjustPieceStock(state.piece.barcode, delta) },
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

@Composable
private fun PieceResolutionContent(
    piece: PieceEntity,
    currencySymbol: String = "$",
    onAdjustStock: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
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
                Text(
                    text = String.format(java.util.Locale.US, "Unit Price: %s%.2f", currencySymbol, piece.unitPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Item Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%s%.2f", currencySymbol, piece.stockQuantity * piece.unitPrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
        OutlinedButton(
            onClick = { onAdjustStock(-1) },
            modifier = Modifier.weight(1f),
            enabled = piece.stockQuantity > 0
        ) {
            Text("- 1", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { onAdjustStock(-5) },
            modifier = Modifier.weight(1f),
            enabled = piece.stockQuantity >= 5
        ) {
            Text("- 5", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onAdjustStock(+1) },
            modifier = Modifier.weight(1f)
        ) {
            Text("+ 1", fontWeight = FontWeight.Bold)
        }

        Button(
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
        OutlinedButton(
            onClick = { onAdjustStock(-1) },
            modifier = Modifier.weight(1f),
            enabled = product.stockQuantity > 0
        ) {
            Text("- 1", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onAdjustStock(+1) },
            modifier = Modifier.weight(1f)
        ) {
            Text("+ 1", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    FilledTonalButton(
        onClick = {
            onDismiss()
            onViewBom()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("View Bill of Materials (BOM) & Build Capacity")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun NotFoundResolutionContent(
    scannedBarcode: String,
    initialName: String = "",
    initialBrand: String = "",
    initialCategory: String = "Grocery",
    currencySymbol: String = "$",
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

    var ocrTextCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var showOcrChoiceDialog by remember { mutableStateOf(false) }

    val cameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            coroutineScope.launch {
                val detected = TextRecognitionService.processImage(bmp)
                if (detected.isNotEmpty()) {
                    if (detected.size == 1) {
                        name = detected.first()
                    } else {
                        ocrTextCandidates = detected
                        showOcrChoiceDialog = true
                    }
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

    OutlinedTextField(
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
        OutlinedTextField(
            value = unitPriceText,
            onValueChange = { unitPriceText = it },
            label = { Text("Price ($currencySymbol)") },
            prefix = { Text(currencySymbol) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        OutlinedTextField(
            value = initialQuantity,
            onValueChange = { initialQuantity = it },
            label = { Text("Quantity in Cart") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category / Store Section") },
        placeholder = { Text("e.g. Dairy, Produce, Bakery") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes / Brand") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            if (name.isBlank()) return@Button
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
                    unitPrice = price
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
        AlertDialog(
            onDismissRequest = { showOcrChoiceDialog = false },
            title = { Text("Select Name Recognized from Package") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Tap the text line that matches your product name:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    ocrTextCandidates.forEach { textLine ->
                        OutlinedButton(
                            onClick = {
                                name = textLine
                                showOcrChoiceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(textLine)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOcrChoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
