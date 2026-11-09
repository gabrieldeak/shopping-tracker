package com.inventory.barcodetracker.ui.screens.scan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.OfferEntity
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplyVoucherDialog(
    savedVouchers: List<OfferEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onApplyVoucher: (OfferEntity) -> Unit,
) {
    var customNameText by remember { mutableStateOf("") }
    var customAmountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Discount Voucher")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Select a quick voucher or create a custom discount to reduce your basket total:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Presets
                Text(
                    "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            onApplyVoucher(
                                OfferEntity(
                                    title = "$currencySymbol 5 Voucher",
                                    offerType = "VOUCHER",
                                    fixedPrice = 5.0
                                )
                            )
                        },
                        label = { Text("$currencySymbol 5 Off") }
                    )
                    AssistChip(
                        onClick = {
                            onApplyVoucher(
                                OfferEntity(
                                    title = "$currencySymbol 10 Voucher",
                                    offerType = "VOUCHER",
                                    fixedPrice = 10.0
                                )
                            )
                        },
                        label = { Text("$currencySymbol 10 Off") }
                    )
                    AssistChip(
                        onClick = {
                            onApplyVoucher(
                                OfferEntity(
                                    title = "$currencySymbol 15 Voucher",
                                    offerType = "VOUCHER",
                                    fixedPrice = 15.0
                                )
                            )
                        },
                        label = { Text("$currencySymbol 15 Off") }
                    )
                    AssistChip(
                        onClick = {
                            onApplyVoucher(
                                OfferEntity(
                                    title = "$currencySymbol 20 Voucher",
                                    offerType = "VOUCHER",
                                    fixedPrice = 20.0
                                )
                            )
                        },
                        label = { Text("$currencySymbol 20 Off") }
                    )
                }

                HorizontalDivider()

                // Saved Vouchers Library
                if (savedVouchers.isNotEmpty()) {
                    Text(
                        "Pick from Saved Vouchers Library:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(savedVouchers, key = { it.id }) { voucher ->
                            Card(
                                onClick = { onApplyVoucher(voucher) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(voucher.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (voucher.storeName.isNotBlank()) {
                                            Text("Store: ${voucher.storeName}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(
                                        "-$currencySymbol${String.format(Locale.US, "%.2f", voucher.fixedPrice)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Custom Voucher Input
                Text(
                    "Custom Voucher Amount:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = customNameText,
                    onValueChange = { customNameText = it },
                    label = { Text("Voucher Name (Optional)") },
                    placeholder = { Text("e.g. Coupon, Clubcard Voucher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        label = { Text("Discount Amount") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amount = customAmountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0) {
                                val name = customNameText.trim().ifEmpty { "$currencySymbol${String.format(Locale.US, "%.2f", amount)} Voucher" }
                                onApplyVoucher(
                                    OfferEntity(
                                        title = name,
                                        offerType = "VOUCHER",
                                        fixedPrice = amount
                                    )
                                )
                            }
                        },
                        enabled = (customAmountText.toDoubleOrNull() ?: 0.0) > 0.0
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
