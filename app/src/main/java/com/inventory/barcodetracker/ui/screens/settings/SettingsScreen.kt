package com.inventory.barcodetracker.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import com.inventory.barcodetracker.ui.components.AppButton
import com.inventory.barcodetracker.ui.components.AppFilledTonalButton
import com.inventory.barcodetracker.ui.components.AppOutlinedButton
import com.inventory.barcodetracker.ui.components.AppOutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.inventory.barcodetracker.service.UpdateCheckService
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inventory.barcodetracker.R
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.ui.i18n.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val currentCurrency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimit.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val capitalizationMode by viewModel.capitalizationMode.collectAsStateWithLifecycle()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showCustomCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var customCurrencyText by remember { mutableStateOf("") }
    var budgetInputText by remember { mutableStateOf("") }
    var restorePickerTarget by remember { mutableStateOf<String?>(null) } // "JSON", "DB", "CSV_ITEMS", "CSV_HISTORY"
    val availableBackups by viewModel.availableBackups.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // --- Export Launchers ---
    val exportItemsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportItems(context, contentResolver, it) }
    }

    val exportHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportHistory(context, contentResolver, it) }
    }

    // --- Import Launchers ---
    val importItemsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (viewModel.validateAndNotifyWrongType(context, it, SettingsViewModel.BackupFileType.CSV)) {
                viewModel.importItems(contentResolver, it)
            }
        }
    }

    val importHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (viewModel.validateAndNotifyWrongType(context, it, SettingsViewModel.BackupFileType.CSV)) {
                viewModel.importHistory(contentResolver, it)
            }
        }
    }

    val exportFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportFullBackup(context, contentResolver, it) }
    }

    val restoreFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (viewModel.validateAndNotifyWrongType(context, it, SettingsViewModel.BackupFileType.JSON)) {
                viewModel.restoreFullBackup(context, contentResolver, it)
            }
        }
    }

    val exportDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportRawDatabase(context, contentResolver, it) }
    }

    val restoreDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (viewModel.validateAndNotifyWrongType(context, it, SettingsViewModel.BackupFileType.DB)) {
                viewModel.importRawDatabase(context, contentResolver, it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = strings.appLogoDesc,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.settingsTitle)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Currency Preference & Shopping Budget
            Text(
                text = strings.currencyAndBudget,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Currency Symbol Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = String.format(java.util.Locale.US, strings.activeCurrencyFmt, currentCurrency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.currencyDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyPreferences.POPULAR_CURRENCIES.forEach { (symbol, label) ->
                            FilterChip(
                                selected = currentCurrency == symbol,
                                onClick = { viewModel.setCurrencySymbol(symbol) },
                                label = { Text(label) }
                            )
                        }
                        FilterChip(
                            selected = CurrencyPreferences.POPULAR_CURRENCIES.none { it.first == currentCurrency },
                            onClick = {
                                customCurrencyText = currentCurrency
                                showCustomCurrencyDialog = true
                            },
                            label = { Text(strings.customCurrencyChip) }
                        )
                    }
                }
            }

            // Target Shopping Budget Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (budgetLimit > 0) String.format(java.util.Locale.US, strings.shoppingBudgetLimitFmt, currentCurrency, budgetLimit) else strings.noShoppingBudgetLimit,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.budgetDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            onClick = {
                                budgetInputText = if (budgetLimit > 0) String.format(java.util.Locale.US, "%.2f", budgetLimit) else ""
                                showBudgetDialog = true
                            }
                        ) {
                            Text(if (budgetLimit > 0) strings.changeBudgetLimit else strings.setBudgetLimit)
                        }
                        if (budgetLimit > 0) {
                            AppOutlinedButton(
                                onClick = { viewModel.setBudgetLimit(0.0) }
                            ) {
                                Text(strings.clearLimit)
                            }
                        }
                    }
                }
            }

            // Theme Mode Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.appAppearanceTheme,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.themeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text(strings.themeSystem)
                        }
                        SegmentedButton(
                            selected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text(strings.themeLight)
                        }
                        SegmentedButton(
                            selected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text(strings.themeDark)
                        }
                    }
                }
            }

            // Text Capitalization Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.textCapitalizationMode,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.capitalizationDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val capitalizationOptions = remember(strings.languageCode) {
                        listOf(
                            "TITLE_CASE" to strings.capTitleCase,
                            "FIRST_LETTER" to strings.capFirstLetter,
                            "NONE" to strings.capNone
                        )
                    }
                    var capitalizationExpanded by remember { mutableStateOf(false) }
                    val selectedCapLabel = capitalizationOptions.firstOrNull { it.first == capitalizationMode }?.second
                        ?: strings.capTitleCase

                    ExposedDropdownMenuBox(
                        expanded = capitalizationExpanded,
                        onExpandedChange = { capitalizationExpanded = it }
                    ) {
                        AppOutlinedTextField(
                            value = selectedCapLabel,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = capitalizationExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        )

                        ExposedDropdownMenu(
                            expanded = capitalizationExpanded,
                            onDismissRequest = { capitalizationExpanded = false }
                        ) {
                            capitalizationOptions.forEach { (modeKey, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (capitalizationMode == modeKey) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (capitalizationMode == modeKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setCapitalizationMode(modeKey)
                                        capitalizationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Section: Full Backup & Restore
            Text(
                text = strings.fullBackupAndRestore,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // JSON Full Backup Card
            FullBackupActionCard(
                title = strings.completeJsonBackup,
                subtitle = strings.completeJsonBackupSub,
                badgeText = "JSON",
                icon = Icons.Default.CloudUpload,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                onSave = { exportFullBackupLauncher.launch("barcode_tracker_backup.json") },
                onShare = { viewModel.shareFullBackup(context) },
                onRestore = {
                    viewModel.loadAvailableBackups(context, SettingsViewModel.BackupFileType.JSON)
                    restorePickerTarget = "JSON"
                }
            )

            // Raw SQLite DB File Card
            FullBackupActionCard(
                title = strings.sqliteSnapshot,
                subtitle = strings.sqliteSnapshotSub,
                badgeText = ".DB",
                icon = Icons.Default.Save,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                onSave = { exportDatabaseLauncher.launch("shopping_tracker.db") },
                onShare = { viewModel.shareRawDatabase(context) },
                onRestore = {
                    viewModel.loadAvailableBackups(context, SettingsViewModel.BackupFileType.DB)
                    restorePickerTarget = "DB"
                }
            )

            HorizontalDivider()

            // Section: CSV Export & Import
            Text(
                text = strings.csvBackupAndExport,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // My Items Catalog Card
            DataTransferCard(
                title = strings.itemsCatalogCsvTitle,
                subtitle = strings.itemsCatalogCsvSub,
                icon = Icons.Default.Inventory2,
                onExport = { exportItemsLauncher.launch("shopping_items.csv") },
                onImport = {
                    viewModel.loadAvailableBackups(context, SettingsViewModel.BackupFileType.CSV)
                    restorePickerTarget = "CSV_ITEMS"
                }
            )

            // Shopping History Card
            DataTransferCard(
                title = strings.historyCsvTitle,
                subtitle = strings.historyCsvSub,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                onExport = { exportHistoryLauncher.launch("shopping_history.csv") },
                onImport = {
                    viewModel.loadAvailableBackups(context, SettingsViewModel.BackupFileType.CSV)
                    restorePickerTarget = "CSV_HISTORY"
                }
            )

            HorizontalDivider()

            // Section 2: Demo / Sample Data Seeding
            Text(
                text = strings.demoAndSeedData,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.seedSampleDataTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        strings.seedSampleDataDesc,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppButton(
                        onClick = viewModel::seedSampleData,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(strings.loadSampleData)
                    }
                }
            }

            HorizontalDivider()

            // Section 3: Reset / Wipe Database
            Text(
                text = strings.dangerZone,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.wipeAllLocalData, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        strings.wipeAllLocalDataDesc,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.clearAllData)
                    }
                }
            }

            HorizontalDivider()

            // Section: External Services & Diagnostics
            Text(
                text = strings.externalServices,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val healthState by viewModel.barcodeHealthState.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.barcodeLookupApis,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.barcodeLookupApisDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (healthState is SettingsViewModel.BarcodeHealthState.Result) {
                            IconButton(
                                onClick = { viewModel.clearBarcodeApiHealthCheck() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = strings.clearHealthResultsDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    when (val state = healthState) {
                        is SettingsViewModel.BarcodeHealthState.Idle -> Unit
                        is SettingsViewModel.BarcodeHealthState.Checking -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = strings.testingBarcodeApis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is SettingsViewModel.BarcodeHealthState.Result -> {
                            val res = state.result
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                res.endpoints.forEach { endpoint ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (endpoint.isHealthy) {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (endpoint.isHealthy) {
                                                    String.format(java.util.Locale.US, strings.healthyResultFmt, endpoint.name, endpoint.statusCode ?: 0, endpoint.latencyMs)
                                                } else {
                                                    String.format(java.util.Locale.US, strings.failedResultFmt, endpoint.name, endpoint.errorMessage ?: "HTTP ${endpoint.statusCode}", endpoint.latencyMs)
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (endpoint.isHealthy) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                }
                                            )
                                            if (!endpoint.sampleProductName.isNullOrBlank()) {
                                                Text(
                                                    text = String.format(java.util.Locale.US, strings.sampleResultFmt, endpoint.sampleBarcode ?: "", endpoint.sampleProductName),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    AppButton(
                        onClick = { viewModel.runBarcodeApiHealthCheck() },
                        enabled = healthState !is SettingsViewModel.BarcodeHealthState.Checking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (healthState is SettingsViewModel.BarcodeHealthState.Checking) {
                                strings.checkingApis
                            } else {
                                strings.runBarcodeHealthCheck
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Section 4: About & Developer Info
            Text(
                text = strings.aboutDeveloper,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // App Header + Version Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = strings.appDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "v${UpdateCheckService.CURRENT_VERSION}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tech Stack Pills
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(strings.tagJetpackCompose, strings.tagRoomDb, strings.tagMlKitOcr, strings.tagOfflineFirst).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Developer & AI Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.developedByLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "G.A.D",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.vibeCodedWithAi,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Share App APK Button
                    AppButton(
                        onClick = { viewModel.shareAppApk(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.shareAppApk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(strings.setBudgetDialogTitle) },
            text = {
                Column {
                    Text(
                        strings.setBudgetDialogBody,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text(String.format(java.util.Locale.US, strings.targetBudgetLabelFmt, currentCurrency)) },
                        prefix = { Text(currentCurrency) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val limit = budgetInputText.toDoubleOrNull() ?: 0.0
                        viewModel.setBudgetLimit(limit)
                        showBudgetDialog = false
                    }
                ) {
                    Text(strings.saveTarget)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showCustomCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCurrencyDialog = false },
            title = { Text(strings.setCustomCurrencyTitle) },
            text = {
                Column {
                    Text(strings.setCustomCurrencyBody, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    AppOutlinedTextField(
                        value = customCurrencyText,
                        onValueChange = { customCurrencyText = it },
                        label = { Text(strings.currencySymbolLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val sym = customCurrencyText.trim()
                        if (sym.isNotEmpty()) {
                            viewModel.setCurrencySymbol(sym)
                        }
                        showCustomCurrencyDialog = false
                    },
                    enabled = customCurrencyText.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCurrencyDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(strings.confirmDataWipeTitle) },
            text = { Text(strings.confirmDataWipeBody) },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.clearAllBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    restorePickerTarget?.let { target ->
        val expectedType = when (target) {
            "JSON" -> SettingsViewModel.BackupFileType.JSON
            "DB" -> SettingsViewModel.BackupFileType.DB
            else -> SettingsViewModel.BackupFileType.CSV
        }
        val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { restorePickerTarget = null },
            title = { Text(String.format(java.util.Locale.US, strings.restorePickerTitleFmt, expectedType.label)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableBackups.isEmpty()) {
                        Text(
                            text = String.format(java.util.Locale.US, strings.noRecentFilesFoundFmt, expectedType.label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = strings.availableFilesByDate,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        availableBackups.forEach { file ->
                            Surface(
                                onClick = {
                                    restorePickerTarget = null
                                    when (target) {
                                        "JSON" -> viewModel.restoreFullBackup(context, contentResolver, file.uri)
                                        "DB" -> viewModel.importRawDatabase(context, contentResolver, file.uri)
                                        "CSV_ITEMS" -> viewModel.importItems(contentResolver, file.uri)
                                        "CSV_HISTORY" -> viewModel.importHistory(contentResolver, file.uri)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        val sizeKb = maxOf(1L, file.sizeBytes / 1024L)
                                        Text(
                                            text = "${dateFormat.format(java.util.Date(file.modifiedAtMillis))} • ${sizeKb} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = file.type.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    AppOutlinedButton(
                        onClick = {
                            restorePickerTarget = null
                            when (target) {
                                "JSON" -> restoreFullBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                "DB" -> restoreDatabaseLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3", "*/*"))
                                "CSV_ITEMS" -> importItemsLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/*"))
                                "CSV_HISTORY" -> importHistoryLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/*"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.browseDeviceCloud)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { restorePickerTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun FullBackupActionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContainerColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconContainerColor
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        LocalAppStrings.current.save,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                AppOutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        LocalAppStrings.current.share,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                AppFilledTonalButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        LocalAppStrings.current.restore,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DataTransferCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LocalAppStrings.current.exportCsvBtn, maxLines = 1)
                }
                AppFilledTonalButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LocalAppStrings.current.importCsvBtn, maxLines = 1)
                }
            }
        }
    }
}
