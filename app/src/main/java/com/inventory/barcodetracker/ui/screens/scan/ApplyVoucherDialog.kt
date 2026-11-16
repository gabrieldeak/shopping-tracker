package com.inventory.barcodetracker.ui.screens.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppFilledTonalButton
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import com.inventory.barcodetracker.ui.components.VerticalScrollbarContainer
import java.util.Locale

@Composable
fun ApplyVoucherDialog(
    savedVouchers: List<OfferEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onApplyVoucher: (OfferEntity) -> Unit,
) {
    var customNameText by remember { mutableStateOf("") }
    var customAmountText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val customAmount = customAmountText.toDoubleOrNull() ?: 0.0

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
                // Header
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
                            text = "Apply Discount Voucher",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Reduce your basket total with a voucher",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                VerticalScrollbarContainer(
                    scrollState = scrollState,
                    maxHeight = 380.dp
                ) {
                    // Quick Presets
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { preset ->
                            AppFilledTonalButton(
                                onClick = {
                                    onApplyVoucher(
                                        OfferEntity(
                                            title = "$currencySymbol$preset Voucher",
                                            offerType = "VOUCHER",
                                            fixedPrice = preset.toDouble()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "-$currencySymbol$preset",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Saved Vouchers Library
                    if (savedVouchers.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Text(
                            text = "Saved Vouchers Library",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedVouchers.forEach { voucher ->
                                Surface(
                                    onClick = { onApplyVoucher(voucher) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = voucher.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (voucher.storeName.isNotBlank()) {
                                                Text(
                                                    text = voucher.storeName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "-$currencySymbol${String.format(Locale.US, "%.2f", voucher.fixedPrice)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // Custom Voucher Input
                    Text(
                        text = "Custom Voucher",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AppOutlinedTextField(
                        value = customNameText,
                        onValueChange = { customNameText = it },
                        label = { Text("Voucher Name (Optional)") },
                        placeholder = { Text("e.g. Coupon, Clubcard Voucher") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppOutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        label = { Text("Discount Amount") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    AppButton(
                        onClick = {
                            if (customAmount > 0.0) {
                                val name = customNameText.trim().ifEmpty {
                                    "$currencySymbol${String.format(Locale.US, "%.2f", customAmount)} Voucher"
                                }
                                onApplyVoucher(
                                    OfferEntity(
                                        title = name,
                                        offerType = "VOUCHER",
                                        fixedPrice = customAmount
                                    )
                                )
                            }
                        },
                        enabled = customAmount > 0.0,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
