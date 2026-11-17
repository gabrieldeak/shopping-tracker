package com.inventory.barcodetracker.ui.screens.quickscan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ShoppingListItemEntity
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.OfferDetailsEditor
import com.inventory.barcodetracker.ui.components.OfferSelectorDropdown
import com.inventory.barcodetracker.ui.components.VerticalScrollbarContainer
import com.inventory.barcodetracker.ui.components.isOfferInputValid
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

@Composable
fun MixAndMatchOfferDialog(
    itemsInList: List<ShoppingListItemEntity>,
    pieceMap: Map<String, PieceEntity>,
    savedOffers: List<OfferEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onApply: (
        selectedBarcodes: List<String>,
        offerType: String,
        buyQty: Int,
        payQty: Int,
        fixedPrice: Double,
        percentOff: Double,
    ) -> Unit,
) {
    val strings = LocalAppStrings.current
    val regularItems = remember(itemsInList) {
        itemsInList.filter { it.offerType != "VOUCHER" && it.unitPrice >= 0 }
    }
    val selectedBarcodes = remember { mutableStateMapOf<String, Boolean>() }

    var offerType by remember { mutableStateOf("MULTI_BUY_FIXED") }
    var offerBuyQty by remember { mutableStateOf("2") }
    var offerPayQty by remember { mutableStateOf("") }
    var offerFixedPrice by remember { mutableStateOf("") }
    var offerPercentOff by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val chosenList = selectedBarcodes.filterValues { it }.keys.toList()

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.mixAndMatchTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.mixAndMatchSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                VerticalScrollbarContainer(
                    scrollState = scrollState,
                    maxHeight = 400.dp
                ) {
                    Text(
                        text = strings.selectItemsToCombine,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        regularItems.forEach { item ->
                            val piece = pieceMap[item.barcode]
                            val displayName = piece?.name ?: item.barcode
                            val price = if (item.unitPrice > 0) item.unitPrice else (piece?.unitPrice ?: 0.0)
                            val isChecked = selectedBarcodes[item.barcode] == true

                            Surface(
                                onClick = { selectedBarcodes[item.barcode] = !isChecked },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedBarcodes[item.barcode] = it },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${strings.qtyLabel} ${item.quantity}" +
                                                if (price > 0) String.format(Locale.US, " • %s%.2f", currencySymbol, price) else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (chosenList.size < 2) {
                        Text(
                            text = strings.needTwoItemsHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    OfferSelectorDropdown(
                        offerType = offerType,
                        currencySymbol = currencySymbol,
                        savedOffers = savedOffers,
                        onSelectBuiltInType = { offerType = it },
                        onSelectSavedOffer = { offer ->
                            offerType = offer.offerType
                            offerBuyQty = if (offer.buyQty > 0) offer.buyQty.toString() else ""
                            offerPayQty = if (offer.payQty > 0) offer.payQty.toString() else ""
                            offerFixedPrice = if (offer.fixedPrice > 0.0) String.format(Locale.US, "%.2f", offer.fixedPrice) else ""
                            offerPercentOff = if (offer.percentOff > 0.0) String.format(Locale.US, "%.0f", offer.percentOff) else ""
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
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val canApply = chosenList.size >= 2 &&
                        offerType != "NONE" &&
                        isOfferInputValid(offerType, offerBuyQty, offerPayQty, offerFixedPrice, offerPercentOff)
                    AppButton(
                        onClick = {
                            onApply(
                                chosenList,
                                offerType,
                                offerBuyQty.toIntOrNull() ?: 0,
                                offerPayQty.toIntOrNull() ?: 0,
                                offerFixedPrice.toDoubleOrNull() ?: 0.0,
                                offerPercentOff.toDoubleOrNull() ?: 0.0,
                            )
                        },
                        enabled = canApply,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(strings.applyMixAndMatch, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
