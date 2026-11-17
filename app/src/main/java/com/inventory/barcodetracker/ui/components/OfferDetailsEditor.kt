package com.inventory.barcodetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings
import java.util.Locale

/**
 * Shared parameter editor for store offers and multi-buy rules matching app text-field styling.
 * All inputs start empty (no prepopulated defaults) with optional preset pills.
 */
@Composable
fun OfferDetailsEditor(
    offerType: String,
    currencySymbol: String,
    offerBuyQty: String,
    onBuyQtyChange: (String) -> Unit,
    offerPayQty: String,
    onPayQtyChange: (String) -> Unit,
    offerFixedPrice: String,
    onFixedPriceChange: (String) -> Unit,
    offerPercentOff: String,
    onPercentOffChange: (String) -> Unit,
    standardUnitPrice: Double = 0.0,
    modifier: Modifier = Modifier,
) {
    if (offerType == "NONE") return
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (offerType) {
            "THREE_FOR_TWO" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOutlinedTextField(
                        value = offerBuyQty,
                        onValueChange = onBuyQtyChange,
                        label = { Text(strings.buyQtyLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    AppOutlinedTextField(
                        value = offerPayQty,
                        onValueChange = onPayQtyChange,
                        label = { Text(strings.payQtyLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPill(label = strings.threeForTwo) {
                        onBuyQtyChange("3")
                        onPayQtyChange("2")
                    }
                    PresetPill(label = strings.twoForOne) {
                        onBuyQtyChange("2")
                        onPayQtyChange("1")
                    }
                }
            }

            "MULTI_BUY_FIXED" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOutlinedTextField(
                        value = offerBuyQty,
                        onValueChange = onBuyQtyChange,
                        label = { Text(strings.buyQtyLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    AppOutlinedTextField(
                        value = offerFixedPrice,
                        onValueChange = onFixedPriceChange,
                        label = { Text(strings.bundlePriceLabel) },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPill(label = String.format(Locale.US, strings.twoForAmountFmt, currencySymbol)) {
                        onBuyQtyChange("2")
                        onFixedPriceChange("5.00")
                    }
                    PresetPill(label = String.format(Locale.US, strings.threeForAmountFmt, currencySymbol)) {
                        onBuyQtyChange("3")
                        onFixedPriceChange("10.00")
                    }
                }
            }

            "REDUCED_PRICE" -> {
                AppOutlinedTextField(
                    value = offerFixedPrice,
                    onValueChange = onFixedPriceChange,
                    label = { Text(strings.specialReducedPriceLabel) },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        val reduced = offerFixedPrice.toDoubleOrNull() ?: 0.0
                        if (standardUnitPrice > 0.0 && reduced > 0.0) {
                            val saving = standardUnitPrice - reduced
                            if (saving > 0) {
                                Text(
                                    text = String.format(Locale.US, strings.savePerItemFmt, currencySymbol, saving, currencySymbol, standardUnitPrice),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }

            "PERCENT_OFF" -> {
                AppOutlinedTextField(
                    value = offerPercentOff,
                    onValueChange = onPercentOffChange,
                    label = { Text(strings.discountPercentLabel) },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("10", "20", "25", "50").forEach { pct ->
                        PresetPill(label = if (pct == "50") strings.fiftyHalf else String.format(Locale.US, strings.percentOffFmt, pct)) {
                            onPercentOffChange(pct)
                        }
                    }
                }
            }

            "VOUCHER" -> {
                AppOutlinedTextField(
                    value = offerFixedPrice,
                    onValueChange = onFixedPriceChange,
                    label = { Text(strings.basketVoucherDiscountLabel) },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PresetPill(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
