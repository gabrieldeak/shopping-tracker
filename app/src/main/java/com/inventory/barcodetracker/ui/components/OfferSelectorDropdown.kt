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
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings

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
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(strings.offerTypeLabel(offerType, currencySymbol)) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(offerType, strings.languageCode) {
        if (!isSearching) {
            searchText = strings.offerTypeLabel(offerType, currencySymbol)
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            onExpanded?.invoke()
        }
    }

    val builtInOfferOptions = remember(currencySymbol, strings.languageCode) {
        listOf(
            "NONE" to strings.offerTypeLabel("NONE", currencySymbol),
            "THREE_FOR_TWO" to strings.offerTypeLabel("THREE_FOR_TWO", currencySymbol),
            "MULTI_BUY_FIXED" to strings.offerTypeLabel("MULTI_BUY_FIXED", currencySymbol),
            "REDUCED_PRICE" to strings.offerTypeLabel("REDUCED_PRICE", currencySymbol),
            "PERCENT_OFF" to strings.offerTypeLabel("PERCENT_OFF", currencySymbol)
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
                if (it.isBlank() && offerType != "NONE") {
                    onSelectBuiltInType("NONE")
                }
            },
            label = { Text(strings.offerTypeOrSavedDeal) },
            placeholder = { Text(strings.searchOfferPlaceholder) },
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (offerType != "NONE") {
                                    onSelectBuiltInType("NONE")
                                    isSearching = false
                                    searchText = strings.offerTypeLabel("NONE", currencySymbol)
                                    expanded = false
                                    focusManager.clearFocus()
                                } else {
                                    searchText = ""
                                    isSearching = true
                                    expanded = true
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = strings.clearOfferSearchDesc,
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
                                searchText = strings.offerTypeLabel(offerType, currencySymbol)
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) strings.collapseOffersDesc else strings.expandOffersDesc
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        expanded = true
                    } else if (isSearching && searchText.isBlank()) {
                        isSearching = false
                        searchText = strings.offerTypeLabel(offerType, currencySymbol)
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
                            text = strings.savedOffersLibrary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        filteredLibrary.forEach { offer ->
                            val badge = offer.getDisplayBadge(currencySymbol)
                            val offerDisplay = if (offer.title.isNotBlank() && !offer.title.equals(badge, ignoreCase = true)) {
                                "${offer.title} ($badge)"
                            } else {
                                badge
                            }
                            Text(
                                text = "🏷️ $offerDisplay",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSavedOffer(offer)
                                        isSearching = false
                                        searchText = offerDisplay
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
                            text = strings.offerTypesHeader,
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
                            text = strings.noMatchingOffers,
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
