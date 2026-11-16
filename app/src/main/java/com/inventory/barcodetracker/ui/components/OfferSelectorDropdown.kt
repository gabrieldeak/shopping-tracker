package com.inventory.barcodetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.barcodetracker.data.local.entity.OfferEntity

fun offerTypeDisplayLabel(type: String, currencySymbol: String): String = when (type) {
    "THREE_FOR_TWO" -> "Buy X Pay Y (e.g. 3 for 2, BOGO)"
    "MULTI_BUY_FIXED" -> "X for $currencySymbol Y (Multi-Buy Bundle)"
    "REDUCED_PRICE" -> "Reduced to $currencySymbol Y (Special Price)"
    "PERCENT_OFF" -> "% Discount"
    else -> "No Offer"
}

/**
 * Reusable searchable offer & saved-deal selector used across item dialogs and scan sheets.
 */
@Composable
fun OfferSelectorDropdown(
    offerType: String,
    currencySymbol: String,
    savedOffers: List<OfferEntity>,
    onSelectBuiltInType: (String) -> Unit,
    onSelectSavedOffer: (OfferEntity) -> Unit,
    modifier: Modifier = Modifier,
    onExpanded: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(offerTypeDisplayLabel(offerType, currencySymbol)) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(offerType) {
        if (!isSearching) {
            searchText = offerTypeDisplayLabel(offerType, currencySymbol)
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            onExpanded?.invoke()
        }
    }

    val builtInOfferOptions = remember(currencySymbol) {
        listOf(
            "NONE" to "No Offer",
            "THREE_FOR_TWO" to "Buy X Pay Y (e.g. 3 for 2, BOGO)",
            "MULTI_BUY_FIXED" to "X for $currencySymbol Y (Multi-Buy Bundle)",
            "REDUCED_PRICE" to "Reduced to $currencySymbol Y (Special Price)",
            "PERCENT_OFF" to "% Discount"
        )
    }
    val libraryOffers = remember(savedOffers) {
        savedOffers.filter { it.offerType != "VOUCHER" }
    }
    val filterQuery = if (isSearching) searchText.trim() else ""
    val filteredBuiltIn = builtInOfferOptions.filter { (_, label) ->
        filterQuery.isEmpty() || label.contains(filterQuery, ignoreCase = true)
    }
    val filteredLibrary = libraryOffers.filter { offer ->
        filterQuery.isEmpty() ||
            offer.title.contains(filterQuery, ignoreCase = true) ||
            offer.getDisplayBadge(currencySymbol).contains(filterQuery, ignoreCase = true) ||
            offer.offerType.contains(filterQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppOutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                isSearching = true
                if (!expanded) expanded = true
            },
            label = { Text("Offer Type or Saved Deal") },
            placeholder = { Text("Search offer or saved deal...") },
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchText = ""
                                isSearching = true
                                expanded = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear offer search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val next = !expanded
                            expanded = next
                            if (!next) {
                                isSearching = false
                                searchText = offerTypeDisplayLabel(offerType, currencySymbol)
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse offers" else "Expand offers"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        expanded = true
                    }
                }
        )

        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                ),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    if (filteredLibrary.isNotEmpty()) {
                        Text(
                            text = "Saved Offers Library",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        filteredLibrary.forEach { offer ->
                            val badge = offer.getDisplayBadge(currencySymbol)
                            Text(
                                text = "🏷️ ${offer.title} ($badge)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSavedOffer(offer)
                                        isSearching = false
                                        searchText = "${offer.title} ($badge)"
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                        if (filteredBuiltIn.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    if (filteredBuiltIn.isNotEmpty()) {
                        Text(
                            text = "Offer Types",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        filteredBuiltIn.forEach { (typeKey, label) ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (offerType == typeKey) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (offerType == typeKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectBuiltInType(typeKey)
                                        isSearching = false
                                        searchText = label
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    if (filteredLibrary.isEmpty() && filteredBuiltIn.isEmpty()) {
                        Text(
                            text = "No matching offers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
