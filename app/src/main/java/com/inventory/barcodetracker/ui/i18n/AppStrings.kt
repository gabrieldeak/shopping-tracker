package com.inventory.barcodetracker.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

interface AppStrings {
    val languageCode: String
    // Bottom nav
    val tabQuickScan: String
    val tabItems: String
    val tabOffers: String
    val tabHistory: String
    val tabSettings: String
    // Common
    val appLogoDesc: String
    val cancel: String
    val save: String
    val delete: String
    val deleteSelected: String
    val edit: String
    val close: String
    val clearAction: String
    val saveAction: String
    val ok: String
    val apply: String
    val remove: String
    val rename: String
    val duplicate: String
    val copy: String
    val share: String
    val restore: String
    val done: String
    val collapse: String
    val expand: String
    val clearSearchDesc: String
    val qtyLabel: String
    val barcodeLabel: String
    val categoryLabel: String
    // Quick Scan
    val quickScanTitle: String
    val manualBarcodeEntryDesc: String
    val searchOrScanPlaceholder: String
    val scanButton: String
    val scanBarcodeDesc: String
    val noBasketSelectedHint: String
    val addingToPrefix: String
    val moreSuffix: String
    val activeBasketsHeader: String
    val newBasket: String
    val noBasketsTitle: String
    val noBasketsSubtitle: String
    val basketsBack: String
    val selectedSuffix: String
    val deleteSelectedBasketsDesc: String
    val noItemsMatchFmt: String
    val addToCatalogFmt: String
    val partsAndPiecesHeaderFmt: String
    val assembledProductsHeaderFmt: String
    val addPlusOne: String
    val eachSuffix: String
    val createBasketTitle: String
    val basketNameLabel: String
    val basketNamePlaceholder: String
    val appendDate: String
    val previewPrefix: String
    val createBasketConfirm: String
    val renameBasketTitle: String
    val newBasketNameLabel: String
    val duplicateBasketTitle: String
    val deleteBasketTitle: String
    val deleteBasketConfirmFmt: String
    val deleteSelectedBasketsTitle: String
    val deleteSelectedBasketsConfirmFmt: String
    val saveBasketsToHistoryTitle: String
    val saveBasketsToHistoryConfirmFmt: String
    val setTargetBudgetTitleFmt: String
    val setTargetBudgetBodyFmt: String
    val budgetLimitLabelFmt: String
    val saveBudget: String
    val updateAvailableTitleFmt: String
    val updateAvailableBodyFmt: String
    val downloadUpdate: String
    val later: String
    val enterBarcodeTitle: String
    val enterBarcodeBody: String
    val enterBarcodePlaceholder: String
    val resolve: String
    val itemAddedTitle: String
    val itemAddedBodyFmt: String
    val scanAnother: String
    val defaultBasketName: String
    val copySuffix: String
    // Scan Resolution Sheet
    val lookingUpBarcode: String
    val checkingOnlineCatalogs: String
    val cancelAndEnterManually: String
    val itemIdentified: String
    val unitPriceFmt: String
    val quantityInShoppingBasket: String
    val inCartFmt: String
    val itemSubtotal: String
    val savedFmt: String
    val addStoreOfferLabel: String
    val activeOfferFmt: String
    val hide: String
    val setOfferBtn: String
    val editOfferBtn: String
    val storeSpecialOfferTitle: String
    val scanOfferLabelDesc: String
    val applyAndSaveOffer: String
    val adjustQuantityInCart: String
    val assembledProduct: String
    val stockInHand: String
    val targetProduction: String
    val unitsFmt: String
    val viewProductComponents: String
    val unrecognizedBarcode: String
    val apisUnavailableInfo: String
    val itemNameRequiredLabel: String
    val itemNamePlaceholder: String
    val scanProductNameDesc: String
    val priceLabelFmt: String
    val quantityInCartLabel: String
    val categorySectionLabel: String
    val categoryPlaceholder: String
    val notesBrandLabel: String
    val storeSpecialOfferOptional: String
    val saveItemAndAddToCart: String
    val autoDetectedOfferFmt: String
    val couldNotMatchDeal: String
    val selectedDealFmt: String
    // Apply Voucher Dialog
    val applyDiscountVoucher: String
    val reduceBasketTotalHint: String
    val quickPresets: String
    val savedVouchersLibrary: String
    val customVoucher: String
    val voucherNameOptional: String
    val voucherNamePlaceholder: String
    val discountAmount: String
    val voucherSuffix: String
    // ShoppingListCard
    val copyListDesc: String
    val deleteListDesc: String
    val overBudget: String
    val oneItem: String
    val itemsCountFmt: String
    val bulletSavedFmt: String
    val leftFmt: String
    val overFmt: String
    val emptyBasketHint: String
    val hideCheckedItems: String
    val showCheckedItems: String
    val uncheckedLabel: String
    val checkedLabel: String
    val totalLabel: String
    val voucherLabelFmt: String
    val removeVoucherDesc: String
    val budgetBtn: String
    val voucherBtn: String
    val swipeHint: String
    val removeItemTitle: String
    val removeItemConfirmFmt: String
    val uncheck: String
    val check: String
    val uncheckItemDesc: String
    val checkItemDesc: String
    val collapseItemDesc: String
    val expandItemDesc: String
    val savedWithDealFmt: String
    val removeItemDesc: String
    val decreaseQtyDesc: String
    val increaseQtyDesc: String
    // Items Screen & Dialogs
    val itemsTitle: String
    val addItem: String
    val searchCatalogPlaceholder: String
    val selectAllFmt: String
    val deselectAllFmt: String
    val addCountFmt: String
    val deleteCountFmt: String
    val savedItemsCatalog: String
    val savedProductsCountFmt: String
    val itemsRoutingHint: String
    val noPartsBelowThreshold: String
    val noItemsInCatalog: String
    val deleteItemTitle: String
    val deleteItemConfirmFmt: String
    val deleteSelectedItemsTitle: String
    val deleteSelectedItemsConfirmFmt: String
    val editShoppingItem: String
    val addNewShoppingItem: String
    val barcodeItemCodeLabel: String
    val barcodeItemCodeOptional: String
    val clearBarcodeDesc: String
    val autoGeneratedHint: String
    val itemNameRequiredError: String
    val saveItem: String
    val updatePriceInBasketsTitle: String
    val updatePriceInBasketsBodyFmt: String
    val updateBasketsAndSave: String
    val saveItemOnly: String
    val addToBasketsTitle: String
    val addToBasketsPromptFmt: String
    val noBasketsExistYet: String
    val addItems: String
    // Offers Screen & Dialogs & Components
    val offersTitle: String
    val addOffer: String
    val offersLogoDesc: String
    val searchOffersPlaceholder: String
    val savedStorePromotions: String
    val activeDealsCountFmt: String
    val offersHint: String
    val noOffersMatchFmt: String
    val noOffersSavedYet: String
    val tapCreateOfferHintFmt: String
    val deleteOfferTitle: String
    val deleteOfferConfirmFmt: String
    val deleteSelectedOffersTitle: String
    val deleteSelectedOffersConfirmFmt: String
    val storeFmt: String
    val applyToItem: String
    val editOfferDeal: String
    val createNewStoreOffer: String
    val promotionTypeRequired: String
    val selectPromotionType: String
    val offerTitleOptional: String
    val offerTitlePlaceholder: String
    val storeOptional: String
    val storePlaceholder: String
    val notesOptional: String
    val notesPlaceholder: String
    val saveOffer: String
    val applyOfferToProduct: String
    val searchProductPlaceholder: String
    val noProductsFound: String
    val codePrefix: String
    val applyDeal: String
    val offerTypeOrSavedDeal: String
    val searchOfferPlaceholder: String
    val clearOfferSearchDesc: String
    val collapseOffersDesc: String
    val expandOffersDesc: String
    val savedOffersLibrary: String
    val offerTypesHeader: String
    val noMatchingOffers: String
    val offerNone: String
    val offerBuyXPayY: String
    val offerMultiBuyFmt: String
    val offerReducedFmt: String
    val offerPercentOff: String
    val offerMultiBuyBundleFmt: String
    val offerReducedLongFmt: String
    val offerPercentLong: String
    val offerVoucherLongFmt: String
    val buyQtyLabel: String
    val payQtyLabel: String
    val threeForTwo: String
    val twoForOne: String
    val bundlePriceLabel: String
    val twoForAmountFmt: String
    val threeForAmountFmt: String
    val specialReducedPriceLabel: String
    val savePerItemFmt: String
    val discountPercentLabel: String
    val fiftyHalf: String
    val percentOffFmt: String
    val basketVoucherDiscountLabel: String
    // OCR Dialog
    val recognizedProductText: String
    val ocrEditInstructions: String
    val productNameLabel: String
    val resetAll: String
    val individualLinesDetected: String
    val useAsName: String
    // History Screen
    val historyTitle: String
    val reloadBasket: String
    val reloadBasketTitle: String
    val reloadBasketConfirmFmt: String
    val searchHistoryPlaceholder: String
    val noSavedBasketsYet: String
    val noSavedBasketsSubtitle: String
    val deleteSavedBasketTitle: String
    val deleteSavedBasketConfirmFmt: String
    val deleteSelectedReceiptsTitle: String
    val deleteSelectedReceiptsConfirmFmt: String
    val defaultHistoryBasketName: String
    val historyDateItemsFmt: String
    val collapseItemsDesc: String
    val expandItemsDesc: String
    // Settings Screen
    val settingsTitle: String
    val currencyAndBudget: String
    val activeCurrencyFmt: String
    val currencyDescription: String
    val customCurrencyChip: String
    val shoppingBudgetLimitFmt: String
    val noShoppingBudgetLimit: String
    val budgetDescription: String
    val changeBudgetLimit: String
    val setBudgetLimit: String
    val clearLimit: String
    val appAppearanceTheme: String
    val themeDescription: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val textCapitalizationMode: String
    val capitalizationDescription: String
    val capTitleCase: String
    val capFirstLetter: String
    val capNone: String
    val fullBackupAndRestore: String
    val completeJsonBackup: String
    val completeJsonBackupSub: String
    val sqliteSnapshot: String
    val sqliteSnapshotSub: String
    val csvBackupAndExport: String
    val itemsCatalogCsvTitle: String
    val itemsCatalogCsvSub: String
    val historyCsvTitle: String
    val historyCsvSub: String
    val demoAndSeedData: String
    val seedSampleDataTitle: String
    val seedSampleDataDesc: String
    val loadSampleData: String
    val dangerZone: String
    val wipeAllLocalData: String
    val wipeAllLocalDataDesc: String
    val clearAllData: String
    val externalServices: String
    val barcodeLookupApis: String
    val barcodeLookupApisDesc: String
    val clearHealthResultsDesc: String
    val testingBarcodeApis: String
    val healthyResultFmt: String
    val failedResultFmt: String
    val sampleResultFmt: String
    val checkingApis: String
    val runBarcodeHealthCheck: String
    val aboutDeveloper: String
    val appDisplayName: String
    val appTagline: String
    val tagJetpackCompose: String
    val tagRoomDb: String
    val tagMlKitOcr: String
    val tagOfflineFirst: String
    val developedByLabel: String
    val vibeCodedWithAi: String
    val shareAppApk: String
    val setBudgetDialogTitle: String
    val setBudgetDialogBody: String
    val targetBudgetLabelFmt: String
    val saveTarget: String
    val setCustomCurrencyTitle: String
    val setCustomCurrencyBody: String
    val currencySymbolLabel: String
    val confirmDataWipeTitle: String
    val confirmDataWipeBody: String
    val clearAllBtn: String
    val exportCsvBtn: String
    val importCsvBtn: String
    // ViewModel Snackbar & Status Messages
    val vmCreatedBasketAddedFmt: String
    val vmCreatedBasketFmt: String
    val vmDuplicatedBasketFmt: String
    val vmRenamedBasketFmt: String
    val vmBudgetSetFmt: String
    val vmBudgetClearedFmt: String
    val vmBudgetSetMultiFmt: String
    val vmBudgetClearedMultiFmt: String
    val vmAppliedVoucherBasketFmt: String
    val vmRemovedItemOrVoucher: String
    val vmDeletedBasketFmt: String
    val vmDeletedBasketsFmt: String
    val vmAppliedVoucherFmt: String
    val vmRemovedVoucherFmt: String
    val vmAddedToBasketsMultiFmt: String
    val vmAddedToBasketSingleFmt: String
    val vmAddedToBasketCartFmt: String
    val vmFoundOnlineFmt: String
    val vmCartQtyUpdatedFmt: String
    val vmProductStockUpdatedFmt: String
    val vmAddedPieceToBasketFmt: String
    val vmAppliedDealFmt: String
    val vmListEmptyFmt: String
    val vmSavedBasketToHistoryFmt: String
    val vmClearedBasketFmt: String
    val vmSelectedBasketsEmpty: String
    val vmClearedSelectedBasketsFmt: String
    val vmCreatedProductFmt: String
    val vmSavedToHistoryKeepItems: String
    val vmBasketClearedKeepItems: String
    val vmPurchasedItemsFallback: String
    val vmSavedPieceUpdatedBasketsFmt: String
    val vmSavedPieceFmt: String
    val vmDeletedPieceFmt: String
    val vmCannotDeleteLinkedItem: String
    val vmDeletedSelectedItemsFmt: String
    val vmCouldNotDeleteLinkedItems: String
    val vmAddedItemsToBasketsFmt: String
    val vmOfferSavedFmt: String
    val vmOfferDeletedFmt: String
    val vmDeletedOffersFmt: String
    val vmAppliedDealToItemFmt: String
    val vmTripReceiptDeleted: String
    val vmDeletedReceiptsFmt: String
    val vmReloadedBasketFmt: String
    val vmFailedReloadBasketFmt: String
    val vmCurrencyUpdatedFmt: String
    val vmBudgetLimitUpdated: String
    val vmThemeUpdatedFmt: String
    val vmCapModeUpdated: String
    val vmAllApisHealthyFmt: String
    val vmSomeApisReachableFmt: String
    val vmExportedItemsCsv: String
    val vmFailedExportItemsFmt: String
    val vmImportedItemsFmt: String
    val vmFailedImportItemsFmt: String
    val vmExportedHistoryCsv: String
    val vmFailedExportHistoryFmt: String
    val vmImportedReceiptsFmt: String
    val vmFailedImportHistoryFmt: String
    val vmFullBackupSaved: String
    val vmFailedExportBackupFmt: String
    val vmRestoredSummaryFmt: String
    val vmCouldNotParseBackup: String
    val vmFailedRestoreBackupFmt: String
    val vmRawDbExported: String
    val vmFailedExportDbFmt: String
    val vmCouldNotParseDb: String
    val vmFailedRestoreDbFmt: String
    val vmShareBackupSubject: String
    val vmShareBackupChooser: String
    val vmFailedShareBackupFmt: String
    val vmShareDbSubject: String
    val vmShareDbChooser: String
    val vmFailedShareDbFmt: String
    val vmShareApkSubject: String
    val vmShareApkText: String
    val vmShareApkChooser: String
    val vmFailedShareApkFmt: String
    val vmSampleDataLoaded: String
    val vmFailedSeedDataFmt: String
    val vmAllDataCleared: String
    val vmFailedClearDataFmt: String
    val switchCurrencyTitle: String
    val switchCurrencyBodyFmt: String
    val switchCurrencyConfirmFmt: String
    val keepCurrencyFmt: String
    val languageNameEnglish: String
    val languageNameRomanian: String
    val vmExportedFileFmt: String
    val vmSavedBackupFileFmt: String
    val vmSavedDbFileFmt: String
    val restorePickerTitleFmt: String
    val availableFilesByDate: String
    val noRecentFilesFoundFmt: String
    val browseDeviceCloud: String
    val wrongFileTypeFmt: String
    val mixAndMatchBtn: String
    val mixAndMatchTitle: String
    val mixAndMatchSubtitle: String
    val selectItemsToCombine: String
    val needTwoItemsHint: String
    val applyMixAndMatch: String
    val mixAndMatchBadge: String
    val ungroupDealDesc: String
    val vmAppliedMixAndMatchFmt: String
    val vmUngroupedDeal: String
    val onboardingTitle: String
    val onboardingSubtitle: String
    val onboardingLanguageLabel: String
    val onboardingCurrencyLabel: String
    val onboardingCustomCurrencyLabel: String
    val onboardingGetStarted: String
    val tutorialHeaderTitle: String
    val tutorialHeaderSubtitle: String
    val tutorialSkip: String
    val tutorialBack: String
    val tutorialNext: String
    val tutorialDone: String
    val tutorialDontShowAgain: String
    val tutorialStep1Title: String
    val tutorialStep1Desc: String
    val tutorialStep2Title: String
    val tutorialStep2Desc: String
    val tutorialStep3Title: String
    val tutorialStep3Desc: String
    val tutorialStep4Title: String
    val tutorialStep4Desc: String
    val tutorialStep5Title: String
    val tutorialStep5Desc: String
    val showTutorialOnStartupLabel: String
    val viewQuickTutorialBtn: String
    fun offerTypeLabel(typeKey: String, currencySymbol: String): String = when (typeKey) {
        "THREE_FOR_TWO" -> offerBuyXPayY
        "MULTI_BUY_FIXED" -> String.format(Locale.US, offerMultiBuyFmt, currencySymbol)
        "REDUCED_PRICE" -> String.format(Locale.US, offerReducedFmt, currencySymbol)
        "PERCENT_OFF" -> offerPercentOff
        else -> offerNone
    }

    companion object {
        fun forLanguage(code: String): AppStrings = when (code) {
            "ro" -> RomanianStrings
            else -> EnglishUkStrings
        }
    }
}

object EnglishUkStrings : AppStrings {
    override val languageCode = "en"
    override val tabQuickScan = "Quick Scan"
    override val tabItems = "Items"
    override val tabOffers = "Offers"
    override val tabHistory = "History"
    override val tabSettings = "Settings"
    override val appLogoDesc = "App Logo"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val deleteSelected = "Delete Selected"
    override val edit = "Edit"
    override val close = "Close"
    override val clearAction = "Clear"
    override val saveAction = "Save"
    override val ok = "OK"
    override val apply = "Apply"
    override val remove = "Remove"
    override val rename = "Rename"
    override val duplicate = "Duplicate"
    override val copy = "Copy"
    override val share = "Share"
    override val restore = "Restore"
    override val done = "Done"
    override val collapse = "Collapse"
    override val expand = "Expand"
    override val clearSearchDesc = "Clear search"
    override val qtyLabel = "Qty:"
    override val barcodeLabel = "Barcode:"
    override val categoryLabel = "Category:"
    override val quickScanTitle = "Barcode Quick Scan"
    override val manualBarcodeEntryDesc = "Manual Barcode Entry"
    override val searchOrScanPlaceholder = "Search or scan item..."
    override val scanButton = "Scan"
    override val scanBarcodeDesc = "Scan Barcode"
    override val noBasketSelectedHint = "No basket selected — check a basket to add items"
    override val addingToPrefix = "Adding to:"
    override val moreSuffix = "more"
    override val activeBasketsHeader = "Shopping Baskets"
    override val newBasket = "New Basket"
    override val noBasketsTitle = "No shopping baskets created yet."
    override val noBasketsSubtitle = "Tap '+ New Basket' above to create custom lists (e.g. Main Basket, Tesco Weekly). Checked baskets will automatically receive scanned or added items!"
    override val basketsBack = "Baskets"
    override val selectedSuffix = "selected"
    override val deleteSelectedBasketsDesc = "Delete Selected Baskets"
    override val noItemsMatchFmt = "No items match \"%s\""
    override val addToCatalogFmt = "Add \"%s\" to Catalog"
    override val partsAndPiecesHeaderFmt = "Items & Groceries (%d) — Tap item to add +1 to selected baskets"
    override val assembledProductsHeaderFmt = "Assembled Products (%d) — Tap product to add +1 to selected baskets"
    override val addPlusOne = "+ Add +1"
    override val eachSuffix = "/ea"
    override val createBasketTitle = "Create New Shopping Basket"
    override val basketNameLabel = "Basket Name / Store"
    override val basketNamePlaceholder = "e.g. Tesco Weekly, BBQ Party"
    override val appendDate = "Append Date"
    override val previewPrefix = "Preview: "
    override val createBasketConfirm = "Create Basket"
    override val renameBasketTitle = "Rename Shopping Basket"
    override val newBasketNameLabel = "New Basket Name"
    override val duplicateBasketTitle = "Duplicate Shopping Basket"
    override val deleteBasketTitle = "Delete Shopping Basket"
    override val deleteBasketConfirmFmt = "Are you sure you want to delete \"%s\"?"
    override val deleteSelectedBasketsTitle = "Delete Selected Baskets"
    override val deleteSelectedBasketsConfirmFmt = "Are you sure you want to delete %d selected shopping basket(s)?"
    override val saveBasketsToHistoryTitle = "Save to History?"
    override val saveBasketsToHistoryConfirmFmt = "Save %d selected basket(s) to History and remove from Active Baskets?"
    override val setTargetBudgetTitleFmt = "Set Target Budget Limit (%s)"
    override val setTargetBudgetBodyFmt = "Enter a target limit for \"%s\". The card will notify you when total items reach or exceed this limit."
    override val budgetLimitLabelFmt = "Budget Limit (%s)"
    override val saveBudget = "Save Budget"
    override val updateAvailableTitleFmt = "🚀 Update Available! (%s)"
    override val updateAvailableBodyFmt = "A new version of Shopping Tracker (%s) is available on GitHub!"
    override val downloadUpdate = "Download"
    override val later = "Later"
    override val enterBarcodeTitle = "Enter Barcode / SKU"
    override val enterBarcodeBody = "Input a barcode or SKU to look up or register new inventory:"
    override val enterBarcodePlaceholder = "e.g. PART-001 or 12345678"
    override val resolve = "Resolve"
    override val itemAddedTitle = "Item Added"
    override val itemAddedBodyFmt = "Added \"%s\" to your basket.\n\nWould you like to scan another product?"
    override val scanAnother = "Scan Another"
    override val defaultBasketName = "Shopping Basket"
    override val copySuffix = "(Copy)"
    override val lookingUpBarcode = "Looking up barcode"
    override val checkingOnlineCatalogs = "Checking online product catalogs"
    override val cancelAndEnterManually = "Cancel & Enter Manually"
    override val itemIdentified = "Item Identified"
    override val unitPriceFmt = "Unit Price: %s%.2f"
    override val quantityInShoppingBasket = "Quantity in Shopping Basket"
    override val inCartFmt = "%d in cart"
    override val itemSubtotal = "Item Subtotal"
    override val savedFmt = "-%s%.2f"
    override val addStoreOfferLabel = "Add Store Special Offer / Deal"
    override val activeOfferFmt = "Active Offer: %s"
    override val hide = "Hide"
    override val setOfferBtn = "+ Set Offer"
    override val editOfferBtn = "Edit Offer"
    override val storeSpecialOfferTitle = "Store Special Offer / Multi-Buy"
    override val scanOfferLabelDesc = "Scan Offer Label with Camera"
    override val applyAndSaveOffer = "Apply & Save Offer"
    override val adjustQuantityInCart = "Adjust Quantity in Cart"
    override val assembledProduct = "Assembled Product"
    override val stockInHand = "Stock In Hand"
    override val targetProduction = "Target Production"
    override val unitsFmt = "%d units"
    override val viewProductComponents = "View Product Components & Build Capacity"
    override val unrecognizedBarcode = "Unrecognized Barcode"
    override val apisUnavailableInfo = "Online barcode lookup APIs are not responding right now. You can still enter the item details manually below."
    override val itemNameRequiredLabel = "Item Name *"
    override val itemNamePlaceholder = "e.g. Steak Burgers, Olive Oil"
    override val scanProductNameDesc = "Scan Product Name with Camera"
    override val priceLabelFmt = "Price (%s)"
    override val quantityInCartLabel = "Quantity in Cart"
    override val categorySectionLabel = "Category / Store Section"
    override val categoryPlaceholder = "e.g. Dairy, Produce, Bakery"
    override val notesBrandLabel = "Notes / Brand"
    override val storeSpecialOfferOptional = "Store Special Offer / Deal (Optional)"
    override val saveItemAndAddToCart = "Save Item & Add to Cart"
    override val autoDetectedOfferFmt = "Auto-detected offer: \"%s\""
    override val couldNotMatchDeal = "Could not match deal from label text. Select deal manually below."
    override val selectedDealFmt = "Selected deal: \"%s\""
    override val applyDiscountVoucher = "Apply Discount Voucher"
    override val reduceBasketTotalHint = "Reduce your basket total with a voucher"
    override val quickPresets = "Quick Presets"
    override val savedVouchersLibrary = "Saved Vouchers Library"
    override val customVoucher = "Custom Voucher"
    override val voucherNameOptional = "Voucher Name (Optional)"
    override val voucherNamePlaceholder = "e.g. Coupon, Clubcard Voucher"
    override val discountAmount = "Discount Amount"
    override val voucherSuffix = "Voucher"
    override val copyListDesc = "Copy List"
    override val deleteListDesc = "Delete List"
    override val overBudget = "Over Budget"
    override val oneItem = "1 item"
    override val itemsCountFmt = "%d items"
    override val bulletSavedFmt = "• Saved %s%.2f"
    override val leftFmt = "%s%.2f left"
    override val overFmt = "+%s%.2f over"
    override val emptyBasketHint = "No items in this basket yet. Check box above and scan barcodes or search items to populate!"
    override val hideCheckedItems = "Hide checked items"
    override val showCheckedItems = "Show checked items"
    override val uncheckedLabel = "Unchecked"
    override val checkedLabel = "Checked"
    override val totalLabel = "Total"
    override val voucherLabelFmt = "Voucher (%s)"
    override val removeVoucherDesc = "Remove Voucher"
    override val budgetBtn = "Budget"
    override val voucherBtn = "Voucher"
    override val swipeHint = "↔ Swipe item: Right to Check • Left to Delete"
    override val removeItemTitle = "Remove Item"
    override val removeItemConfirmFmt = "Remove \"%s\" from \"%s\"?"
    override val uncheck = "Uncheck"
    override val check = "Check"
    override val uncheckItemDesc = "Uncheck item"
    override val checkItemDesc = "Check item"
    override val collapseItemDesc = "Collapse item"
    override val expandItemDesc = "Expand item"
    override val savedWithDealFmt = "-%s%.2f"
    override val removeItemDesc = "Remove item"
    override val decreaseQtyDesc = "Decrease quantity"
    override val increaseQtyDesc = "Increase quantity"
    override val itemsTitle = "My Items Catalog"
    override val addItem = "Add Item"
    override val searchCatalogPlaceholder = "Search catalog by name or barcode..."
    override val selectAllFmt = "Select All (%d)"
    override val deselectAllFmt = "Deselect All (%d)"
    override val addCountFmt = "Add (%d)"
    override val deleteCountFmt = "Delete (%d)"
    override val savedItemsCatalog = "Saved Items Catalog"
    override val savedProductsCountFmt = "%d Saved Products"
    override val itemsRoutingHint = "Check items below, then tap 'Add' above to route them to your shopping baskets."
    override val noPartsBelowThreshold = "No items currently below threshold"
    override val noItemsInCatalog = "No items found in catalog"
    override val deleteItemTitle = "Delete Item"
    override val deleteItemConfirmFmt = "Are you sure you want to delete \"%s\"?\n\nIf you delete it, it will also be removed from any active shopping baskets."
    override val deleteSelectedItemsTitle = "Delete Selected Items"
    override val deleteSelectedItemsConfirmFmt = "Are you sure you want to delete %d selected item(s)?\n\nIf you delete them, they will also be removed from any active shopping baskets."
    override val editShoppingItem = "Edit Shopping Item"
    override val addNewShoppingItem = "Add New Shopping Item"
    override val barcodeItemCodeLabel = "Barcode / Item Code"
    override val barcodeItemCodeOptional = "Barcode / Item Code (Optional)"
    override val clearBarcodeDesc = "Clear barcode"
    override val autoGeneratedHint = "Auto-generated if left blank"
    override val itemNameRequiredError = "Item name is required"
    override val saveItem = "Save Item"
    override val updatePriceInBasketsTitle = "Update Price in Active Baskets?"
    override val updatePriceInBasketsBodyFmt = "You updated the unit price of \"%s\" to %s%.2f.\n\nDo you also want to update its price in all active shopping baskets?"
    override val updateBasketsAndSave = "Update Baskets & Save"
    override val saveItemOnly = "Save Item Only"
    override val addToBasketsTitle = "Add to Shopping Basket(s)"
    override val addToBasketsPromptFmt = "Select which shopping basket(s) to add %d selected item(s) into:"
    override val noBasketsExistYet = "No shopping baskets exist yet on the main screen."
    override val addItems = "Add Items"
    override val offersTitle = "Store Offers & Deals"
    override val addOffer = "Add Offer"
    override val offersLogoDesc = "Offers Logo"
    override val searchOffersPlaceholder = "Search offers by name or store..."
    override val savedStorePromotions = "Saved Store Promotions"
    override val activeDealsCountFmt = "%d Active Deals"
    override val offersHint = "Check offers below to batch delete, or tap 'Apply to Item' to link a deal to a catalog product."
    override val noOffersMatchFmt = "No offers match \"%s\""
    override val noOffersSavedYet = "No store offers saved yet"
    override val tapCreateOfferHintFmt = "Tap '+ Add Offer' below to set up 3 for 2, 2 for %s5, or reduced price deals."
    override val deleteOfferTitle = "Delete Offer"
    override val deleteOfferConfirmFmt = "Are you sure you want to delete offer \"%s\"?"
    override val deleteSelectedOffersTitle = "Delete Selected Offers"
    override val deleteSelectedOffersConfirmFmt = "Are you sure you want to delete %d selected store offer(s)?"
    override val storeFmt = "Store: %s"
    override val applyToItem = "Apply to Item"
    override val editOfferDeal = "Edit Offer / Deal"
    override val createNewStoreOffer = "Create New Store Offer"
    override val promotionTypeRequired = "Promotion Type *"
    override val selectPromotionType = "Select Promotion Type"
    override val offerTitleOptional = "Offer Title / Name (Optional)"
    override val offerTitlePlaceholder = "e.g. Tesco 3 for 2 Grill Deal"
    override val storeOptional = "Store / Supermarket (Optional)"
    override val storePlaceholder = "e.g. Tesco, Sainsbury's, Asda"
    override val notesOptional = "Notes / Description (Optional)"
    override val notesPlaceholder = "e.g. Valid until Sunday"
    override val saveOffer = "Save Offer"
    override val applyOfferToProduct = "Apply Offer to Product"
    override val searchProductPlaceholder = "Search product name, barcode..."
    override val noProductsFound = "No products found"
    override val codePrefix = "Code:"
    override val applyDeal = "Apply Deal"
    override val offerTypeOrSavedDeal = "Offer Type or Saved Deal"
    override val searchOfferPlaceholder = "Search offer or saved deal..."
    override val clearOfferSearchDesc = "Clear offer search"
    override val collapseOffersDesc = "Collapse offers"
    override val expandOffersDesc = "Expand offers"
    override val savedOffersLibrary = "Saved Offers Library"
    override val offerTypesHeader = "Offer Types"
    override val noMatchingOffers = "No matching offers"
    override val offerNone = "No Offer"
    override val offerBuyXPayY = "Buy X Pay Y (e.g. 3 for 2)"
    override val offerMultiBuyFmt = "X for %sY (Multi-Buy Bundle)"
    override val offerReducedFmt = "Reduced to %sY (Special Price)"
    override val offerPercentOff = "% Discount"
    override val offerMultiBuyBundleFmt = "Multi-Buy Bundle (e.g. 2 for %s5)"
    override val offerReducedLongFmt = "Special Reduced Price (e.g. Reduced to %s2.50)"
    override val offerPercentLong = "Percentage Discount (e.g. 20% Off)"
    override val offerVoucherLongFmt = "🎟️ Basket Discount Voucher (%s Off)"
    override val buyQtyLabel = "Buy Qty"
    override val payQtyLabel = "Pay Qty"
    override val threeForTwo = "3 for 2"
    override val twoForOne = "2 for 1"
    override val bundlePriceLabel = "Bundle Price"
    override val twoForAmountFmt = "2 for %s5"
    override val threeForAmountFmt = "3 for %s10"
    override val specialReducedPriceLabel = "Special Reduced Price"
    override val savePerItemFmt = "Save %s%.2f per item (from %s%.2f)"
    override val discountPercentLabel = "Discount %"
    override val fiftyHalf = "50% (Half)"
    override val percentOffFmt = "%s%% Off"
    override val basketVoucherDiscountLabel = "Basket Voucher Discount"
    override val recognizedProductText = "Recognized Product Text"
    override val ocrEditInstructions = "All text found by OCR is added to the editable box below. Edit, add, or delete words to set your product name:"
    override val productNameLabel = "Product Name"
    override val resetAll = "Reset All"
    override val individualLinesDetected = "Individual lines detected (tap to set):"
    override val useAsName = "Use as Name"
    override val historyTitle = "Shopping History"
    override val reloadBasket = "Make Active"
    override val reloadBasketTitle = "Make Basket Active?"
    override val reloadBasketConfirmFmt = "Move \"%s\" back to Active Baskets and remove it from History?"
    override val searchHistoryPlaceholder = "Search history by basket, item or date..."
    override val noSavedBasketsYet = "No saved baskets yet"
    override val noSavedBasketsSubtitle = "When you finish shopping, tap 'Save' on any basket card to save a date-stamped receipt here."
    override val deleteSavedBasketTitle = "Delete Saved Basket"
    override val deleteSavedBasketConfirmFmt = "Are you sure you want to delete the saved basket from %s?"
    override val deleteSelectedReceiptsTitle = "Delete Selected Receipts"
    override val deleteSelectedReceiptsConfirmFmt = "Are you sure you want to delete %d selected receipt(s) from your history?"
    override val defaultHistoryBasketName = "Shopping Basket"
    override val historyDateItemsFmt = "%s • %d items"
    override val collapseItemsDesc = "Collapse items"
    override val expandItemsDesc = "Expand items"
    override val settingsTitle = "App Settings & Data"
    override val currencyAndBudget = "Currency & Budget"
    override val activeCurrencyFmt = "Active Currency Symbol: \"%s\""
    override val currencyDescription = "Choose the currency symbol used across basket totals and item prices."
    override val customCurrencyChip = "Custom..."
    override val shoppingBudgetLimitFmt = "Shopping Budget Limit: %s%.2f"
    override val noShoppingBudgetLimit = "No Shopping Budget Limit Set"
    override val budgetDescription = "Set a spending limit for your shopping basket. You'll get an alert when scanned items reach or exceed this amount."
    override val changeBudgetLimit = "Change Budget Limit"
    override val setBudgetLimit = "Set Budget Limit"
    override val clearLimit = "Clear Limit"
    override val appAppearanceTheme = "App Appearance Theme"
    override val themeDescription = "Switch between System Default, Light Theme, or Dark Theme."
    override val themeSystem = "System"
    override val themeLight = "Light ☀️"
    override val themeDark = "Dark 🌙"
    override val textCapitalizationMode = "Text Capitalization Mode"
    override val capitalizationDescription = "Automatically format basket names, items, and store offer titles when typing or creating records."
    override val capTitleCase = "Title Case (Every Word)"
    override val capFirstLetter = "First Letter Only"
    override val capNone = "As Typed (Manual)"
    override val fullBackupAndRestore = "Full Backup & Database Restore"
    override val completeJsonBackup = "Complete JSON Backup"
    override val completeJsonBackupSub = "Portable backup of items, baskets, deals, receipts & settings."
    override val sqliteSnapshot = "SQLite Database Snapshot"
    override val sqliteSnapshotSub = "Direct 1:1 backup & restore of shopping_tracker.db."
    override val csvBackupAndExport = "CSV Backup & Data Export"
    override val itemsCatalogCsvTitle = "Items Catalog"
    override val itemsCatalogCsvSub = "shopping_items.csv · Barcode, Name, Price, Category, Notes"
    override val historyCsvTitle = "Shopping History & Receipts"
    override val historyCsvSub = "shopping_history.csv · Date, Total Cost, Items, Summary"
    override val demoAndSeedData = "Demo & Seed Data"
    override val seedSampleDataTitle = "Seed Sample Grocery Data"
    override val seedSampleDataDesc = "Pre-populates sample items (Steak Burgers, Cheese Slices, Olive Oil, Milk) so you can test scanning, basket pricing, and deals immediately."
    override val loadSampleData = "Load Sample Data"
    override val dangerZone = "Danger Zone"
    override val wipeAllLocalData = "Wipe All Local Data"
    override val wipeAllLocalDataDesc = "Permanently deletes all shopping baskets, catalog items, offers, and receipt history from this device."
    override val clearAllData = "Clear All Data"
    override val externalServices = "External Services & Diagnostics"
    override val barcodeLookupApis = "Barcode Lookup APIs"
    override val barcodeLookupApisDesc = "Tests OpenFoodFacts (.org & .net), OpenProductsFacts & OpenBeautyFacts."
    override val clearHealthResultsDesc = "Clear health check results"
    override val testingBarcodeApis = "Testing barcode lookup APIs..."
    override val healthyResultFmt = "%s · Healthy (HTTP %d, %d ms)"
    override val failedResultFmt = "%s · %s (%d ms)"
    override val sampleResultFmt = "Sample (%s): %s"
    override val checkingApis = "Checking..."
    override val runBarcodeHealthCheck = "Run Barcode API Health Check"
    override val aboutDeveloper = "About & Developer Info"
    override val appDisplayName = "Shopping Tracker"
    override val appTagline = "Offline-first shopping & inventory tracker"
    override val tagJetpackCompose = "Jetpack Compose"
    override val tagRoomDb = "Room DB"
    override val tagMlKitOcr = "ML Kit OCR"
    override val tagOfflineFirst = "Offline-First"
    override val developedByLabel = "Developed & Designed by"
    override val vibeCodedWithAi = "Vibe coded with AI"
    override val shareAppApk = "Share App APK"
    override val setBudgetDialogTitle = "Set Shopping Budget Limit"
    override val setBudgetDialogBody = "Set a target spending limit for your shopping basket. You will receive a visual alert when your basket total reaches or exceeds this limit."
    override val targetBudgetLabelFmt = "Target Budget (%s)"
    override val saveTarget = "Save Target"
    override val setCustomCurrencyTitle = "Set Custom Currency Symbol"
    override val setCustomCurrencyBody = "Enter a custom currency symbol or code (e.g. CAD, AUD, kr, ₪):"
    override val currencySymbolLabel = "Currency Symbol / Code"
    override val confirmDataWipeTitle = "Confirm Data Wipe"
    override val confirmDataWipeBody = "Are you sure you want to delete all local data? All baskets, catalog items, offers, and shopping history will be permanently removed."
    override val clearAllBtn = "Clear All"
    override val exportCsvBtn = "Export CSV"
    override val importCsvBtn = "Import CSV"
    override val vmCreatedBasketAddedFmt = "Created basket \"%s\" & added %s"
    override val vmCreatedBasketFmt = "Created basket \"%s\""
    override val vmDuplicatedBasketFmt = "Duplicated basket as \"%s\""
    override val vmRenamedBasketFmt = "Renamed basket to \"%s\""
    override val vmBudgetSetFmt = "Budget limit set to %s%.2f for \"%s\""
    override val vmBudgetClearedFmt = "Budget limit cleared for \"%s\""
    override val vmBudgetSetMultiFmt = "Set %s%.2f budget limit for %d selected basket(s)"
    override val vmBudgetClearedMultiFmt = "Cleared budget limit for %d selected basket(s)"
    override val vmAppliedVoucherBasketFmt = "Applied %s (-%s%.2f) to Basket"
    override val vmRemovedItemOrVoucher = "Removed item/voucher from basket"
    override val vmDeletedBasketFmt = "Deleted basket \"%s\""
    override val vmDeletedBasketsFmt = "Deleted %d selected basket(s)"
    override val vmAppliedVoucherFmt = "Applied %s (-%s%.2f)"
    override val vmRemovedVoucherFmt = "Removed voucher %s"
    override val vmAddedToBasketsMultiFmt = "✓ Added %s%s to %d selected baskets"
    override val vmAddedToBasketSingleFmt = "✓ Added %s%s to Basket"
    override val vmAddedToBasketCartFmt = "✓ Added %s%s to Basket (In cart: %d)"
    override val vmFoundOnlineFmt = "Found online: %s. Enter price to save."
    override val vmCartQtyUpdatedFmt = "Cart quantity updated (%s)"
    override val vmProductStockUpdatedFmt = "Product stock updated (%s)"
    override val vmAddedPieceToBasketFmt = "✓ Added %s to Basket"
    override val vmAppliedDealFmt = "Applied deal \"%s\""
    override val vmListEmptyFmt = "List \"%s\" is empty!"
    override val vmSavedBasketToHistoryFmt = "Saved \"%s\" (%s%.2f) to History!"
    override val vmClearedBasketFmt = "Cleared basket \"%s\""
    override val vmSelectedBasketsEmpty = "Selected basket(s) are empty!"
    override val vmClearedSelectedBasketsFmt = "Cleared items from %d selected basket(s)"
    override val vmCreatedProductFmt = "Created product %s"
    override val vmSavedToHistoryKeepItems = "Basket saved to History! Items remain in Items."
    override val vmBasketClearedKeepItems = "Basket cleared. Items remain saved in Items."
    override val vmPurchasedItemsFallback = "Purchased items"
    override val vmSavedPieceUpdatedBasketsFmt = "Saved %s & updated price in active baskets"
    override val vmSavedPieceFmt = "Saved %s"
    override val vmDeletedPieceFmt = "Deleted %s"
    override val vmCannotDeleteLinkedItem = "Cannot delete item: it is currently linked to an assembled product."
    override val vmDeletedSelectedItemsFmt = "Deleted %d selected item(s)"
    override val vmCouldNotDeleteLinkedItems = "Could not delete some items linked to product BOMs"
    override val vmAddedItemsToBasketsFmt = "Added %d item(s) to selected shopping basket(s)!"
    override val vmOfferSavedFmt = "Offer \"%s\" saved successfully."
    override val vmOfferDeletedFmt = "Offer \"%s\" deleted."
    override val vmDeletedOffersFmt = "Deleted %d selected offer(s)."
    override val vmAppliedDealToItemFmt = "Applied deal \"%s\" to %s."
    override val vmTripReceiptDeleted = "Trip receipt deleted"
    override val vmDeletedReceiptsFmt = "Deleted %d selected receipt(s)"
    override val vmReloadedBasketFmt = "Basket \"%s\" is now active!"
    override val vmFailedReloadBasketFmt = "Failed to activate basket: %s"
    override val vmCurrencyUpdatedFmt = "Currency symbol updated to %s"
    override val vmBudgetLimitUpdated = "Shopping budget limit updated"
    override val vmThemeUpdatedFmt = "App theme updated to %s"
    override val vmCapModeUpdated = "Text capitalization mode updated"
    override val vmAllApisHealthyFmt = "All %d barcode APIs healthy"
    override val vmSomeApisReachableFmt = "%d/%d barcode APIs reachable"
    override val vmExportedItemsCsv = "Successfully exported shopping_items.csv"
    override val vmFailedExportItemsFmt = "Failed to export shopping items: %s"
    override val vmImportedItemsFmt = "Imported %d shopping item(s) into Items."
    override val vmFailedImportItemsFmt = "Failed to import items: %s"
    override val vmExportedHistoryCsv = "Successfully exported shopping_history.csv"
    override val vmFailedExportHistoryFmt = "Failed to export history: %s"
    override val vmImportedReceiptsFmt = "Imported %d receipt(s) into History."
    override val vmFailedImportHistoryFmt = "Failed to import history: %s"
    override val vmFullBackupSaved = "Full backup file saved successfully!"
    override val vmFailedExportBackupFmt = "Failed to export backup: %s"
    override val vmRestoredSummaryFmt = "Restored %d items, %d baskets, %d offers & %d receipts!"
    override val vmCouldNotParseBackup = "Could not parse backup file."
    override val vmFailedRestoreBackupFmt = "Failed to restore backup: %s"
    override val vmRawDbExported = "Raw SQLite database exported successfully (shopping_tracker.db)!"
    override val vmFailedExportDbFmt = "Failed to export database file: %s"
    override val vmCouldNotParseDb = "Could not parse database file."
    override val vmFailedRestoreDbFmt = "Failed to restore database file: %s"
    override val vmShareBackupSubject = "Shopping Tracker Full Backup"
    override val vmShareBackupChooser = "Share Backup via Google Drive, Email, etc."
    override val vmFailedShareBackupFmt = "Failed to share backup: %s"
    override val vmShareDbSubject = "Shopping Tracker Database (.db)"
    override val vmShareDbChooser = "Share SQLite Database via Google Drive, Email, etc."
    override val vmFailedShareDbFmt = "Failed to share database file: %s"
    override val vmShareApkSubject = "Shopping Tracker App"
    override val vmShareApkText = "Here is the Shopping Tracker Android App installer (APK)."
    override val vmShareApkChooser = "Share App APK via Bluetooth, Drive, Email..."
    override val vmFailedShareApkFmt = "Failed to share app APK: %s"
    override val vmSampleDataLoaded = "Sample grocery items and offers loaded."
    override val vmFailedSeedDataFmt = "Failed to seed data: %s"
    override val vmAllDataCleared = "All baskets, items, offers, and shopping history have been cleared."
    override val vmFailedClearDataFmt = "Failed to clear data: %s"
    override val switchCurrencyTitle = "Switch Currency?"
    override val switchCurrencyBodyFmt = "You switched the language to %s. Would you also like to switch the currency to %s?"
    override val switchCurrencyConfirmFmt = "Switch to %s"
    override val keepCurrencyFmt = "Keep %s"
    override val languageNameEnglish = "English (UK)"
    override val languageNameRomanian = "Romanian"
    override val vmExportedFileFmt = "Successfully exported %s"
    override val vmSavedBackupFileFmt = "Full backup saved: %s"
    override val vmSavedDbFileFmt = "SQLite database exported: %s"
    override val restorePickerTitleFmt = "Select %s File"
    override val availableFilesByDate = "Available files (newest first):"
    override val noRecentFilesFoundFmt = "No %s files found in app-accessible storage yet. Use 'Browse Device / Cloud' below to pick a file from any folder."
    override val browseDeviceCloud = "Browse Device / Cloud..."
    override val wrongFileTypeFmt = "Selected file (%s) does not match expected %s format."
    override val mixAndMatchBtn = "Mix & Match"
    override val mixAndMatchTitle = "Mix & Match Multi-Item Offer"
    override val mixAndMatchSubtitle = "Combine 2 or more items in this basket under one shared deal (e.g. Red + Green Grapes for £4)."
    override val selectItemsToCombine = "1. Select Items to Combine"
    override val needTwoItemsHint = "Select at least 2 items to create a Mix & Match deal."
    override val applyMixAndMatch = "Apply Mix & Match"
    override val mixAndMatchBadge = "Mix & Match"
    override val ungroupDealDesc = "Ungroup Mix & Match deal"
    override val vmAppliedMixAndMatchFmt = "Applied Mix & Match (%s) to %d items!"
    override val vmUngroupedDeal = "Removed Mix & Match grouping"
    override val onboardingTitle = "Welcome to Shopping Tracker"
    override val onboardingSubtitle = "Choose your preferred language and currency to get started. You can change these anytime in Settings."
    override val onboardingLanguageLabel = "Language"
    override val onboardingCurrencyLabel = "Currency"
    override val onboardingCustomCurrencyLabel = "Custom symbol (optional)"
    override val onboardingGetStarted = "Get Started"
    override val tutorialHeaderTitle = "Quick Tutorial"
    override val tutorialHeaderSubtitle = "A quick tour of Shopping Tracker's key features"
    override val tutorialSkip = "Skip"
    override val tutorialBack = "Back"
    override val tutorialNext = "Next"
    override val tutorialDone = "Start Shopping"
    override val tutorialDontShowAgain = "Don't show on next startup"
    override val tutorialStep1Title = "Quick Scan & Smart Baskets"
    override val tutorialStep1Desc = "Scan barcodes or search your catalog, multi-select items to add at once, and track live totals and budget limits per basket."
    override val tutorialStep2Title = "Swipe to Check or Remove"
    override val tutorialStep2Desc = "Swipe right on any basket item to check it off as you shop, or swipe left to remove it. Checked items collapse neatly out of the way."
    override val tutorialStep3Title = "Mix & Match & Store Offers"
    override val tutorialStep3Desc = "Combine different items under one Mix & Match deal (like 2 for £4), or scan shelf labels to save 3-for-2, % Off, and reduced-price offers."
    override val tutorialStep4Title = "Catalog & Receipt History"
    override val tutorialStep4Desc = "Save completed baskets to History as itemized receipts with Mix & Match groupings preserved, and tap Make Active to reuse any past basket."
    override val tutorialStep5Title = "Backup, Restore & Export"
    override val tutorialStep5Desc = "Keep your catalog, baskets, offers, and history safe with full JSON or SQLite backups and CSV export/import in Settings."
    override val showTutorialOnStartupLabel = "Show Quick Tutorial on next startup"
    override val viewQuickTutorialBtn = "View Quick Tutorial"
}

object RomanianStrings : AppStrings {
    override val languageCode = "ro"
    override val tabQuickScan = "Scanare"
    override val tabItems = "Produse"
    override val tabOffers = "Oferte"
    override val tabHistory = "Istoric"
    override val tabSettings = "Setări"
    override val appLogoDesc = "Logo aplicație"
    override val cancel = "Anulează"
    override val save = "Salvează"
    override val delete = "Șterge"
    override val deleteSelected = "Șterge selecția"
    override val edit = "Editează"
    override val close = "Închide"
    override val clearAction = "Golește"
    override val saveAction = "Salvează"
    override val ok = "OK"
    override val apply = "Aplică"
    override val remove = "Elimină"
    override val rename = "Redenumește"
    override val duplicate = "Duplică"
    override val copy = "Copiază"
    override val share = "Partajează"
    override val restore = "Restaurează"
    override val done = "Gata"
    override val collapse = "Restrânge"
    override val expand = "Extinde"
    override val clearSearchDesc = "Șterge căutarea"
    override val qtyLabel = "Cant:"
    override val barcodeLabel = "Cod de bare:"
    override val categoryLabel = "Categorie:"
    override val quickScanTitle = "Scanare Rapidă"
    override val manualBarcodeEntryDesc = "Introducere manuală cod de bare"
    override val searchOrScanPlaceholder = "Caută sau scanează produs..."
    override val scanButton = "Scanează"
    override val scanBarcodeDesc = "Scanează cod de bare"
    override val noBasketSelectedHint = "Niciun coș selectat — bifează un coș pentru a adăuga produse"
    override val addingToPrefix = "Se adaugă în:"
    override val moreSuffix = "alte"
    override val activeBasketsHeader = "Coșuri de Cumpărături"
    override val newBasket = "Coș Nou"
    override val noBasketsTitle = "Niciun coș creat încă."
    override val noBasketsSubtitle = "Apasă '+ Coș Nou' mai sus pentru a crea liste personalizate (ex. Coș Principal, Lidl Săptămânal). Coșurile bifate primesc automat produsele scanate sau adăugate!"
    override val basketsBack = "Coșuri"
    override val selectedSuffix = "selectate"
    override val deleteSelectedBasketsDesc = "Șterge coșurile selectate"
    override val noItemsMatchFmt = "Niciun produs nu corespunde cu \"%s\""
    override val addToCatalogFmt = "Adaugă \"%s\" în catalog"
    override val partsAndPiecesHeaderFmt = "Produse (%d) — Atinge pentru a adăuga +1 în coșurile selectate"
    override val assembledProductsHeaderFmt = "Produse asamblate (%d) — Atinge pentru a adăuga +1 în coșurile selectate"
    override val addPlusOne = "+ Adaugă +1"
    override val eachSuffix = "/buc"
    override val createBasketTitle = "Creează Coș de Cumpărături"
    override val basketNameLabel = "Nume Coș / Magazin"
    override val basketNamePlaceholder = "ex. Lidl Săptămânal, Grătar"
    override val appendDate = "Adaugă data"
    override val previewPrefix = "Previzualizare: "
    override val createBasketConfirm = "Creează Coș"
    override val renameBasketTitle = "Redenumește Coșul"
    override val newBasketNameLabel = "Nume nou coș"
    override val duplicateBasketTitle = "Duplică Coșul"
    override val deleteBasketTitle = "Șterge Coșul"
    override val deleteBasketConfirmFmt = "Sigur doriți să ștergeți coșul \"%s\"?"
    override val deleteSelectedBasketsTitle = "Șterge coșurile selectate"
    override val deleteSelectedBasketsConfirmFmt = "Sigur doriți să ștergeți %d coș(uri) selectate?"
    override val saveBasketsToHistoryTitle = "Salvați în Istoric?"
    override val saveBasketsToHistoryConfirmFmt = "Salvați %d coș(uri) selectate în Istoric și eliminați-le din Coșurile Active?"
    override val setTargetBudgetTitleFmt = "Setează Bugetul (%s)"
    override val setTargetBudgetBodyFmt = "Introduceți o limită de buget pentru \"%s\". Cardul vă va avertiza când totalul atinge sau depășește această limită."
    override val budgetLimitLabelFmt = "Limită buget (%s)"
    override val saveBudget = "Salvează buget"
    override val updateAvailableTitleFmt = "🚀 Actualizare disponibilă! (%s)"
    override val updateAvailableBodyFmt = "O nouă versiune Shopping Tracker (%s) este disponibilă pe GitHub!"
    override val downloadUpdate = "Descarcă"
    override val later = "Mai târziu"
    override val enterBarcodeTitle = "Introdu Cod de Bare / SKU"
    override val enterBarcodeBody = "Introduceți un cod de bare sau SKU pentru căutare sau înregistrare:"
    override val enterBarcodePlaceholder = "ex. PART-001 sau 12345678"
    override val resolve = "Caută"
    override val itemAddedTitle = "Produs Adăugat"
    override val itemAddedBodyFmt = "Produsul \"%s\" a fost adăugat în coș.\n\nDoriți să scanați alt produs?"
    override val scanAnother = "Scanează Altul"
    override val defaultBasketName = "Coș de Cumpărături"
    override val copySuffix = "(Copie)"
    override val lookingUpBarcode = "Se caută codul de bare"
    override val checkingOnlineCatalogs = "Se verifică cataloagele online"
    override val cancelAndEnterManually = "Anulează și Introdu Manual"
    override val itemIdentified = "Produs Identificat"
    override val unitPriceFmt = "Preț unitar: %s%.2f"
    override val quantityInShoppingBasket = "Cantitate în coș"
    override val inCartFmt = "%d în coș"
    override val itemSubtotal = "Subtotal produs"
    override val savedFmt = "-%s%.2f"
    override val addStoreOfferLabel = "Adaugă Ofertă / Promoție"
    override val activeOfferFmt = "Ofertă activă: %s"
    override val hide = "Ascunde"
    override val setOfferBtn = "+ Setează Ofertă"
    override val editOfferBtn = "Editează Ofertă"
    override val storeSpecialOfferTitle = "Ofertă Specială / Multi-Buy"
    override val scanOfferLabelDesc = "Scanează eticheta ofertei cu camera"
    override val applyAndSaveOffer = "Aplică și Salvează Oferta"
    override val adjustQuantityInCart = "Ajustează cantitatea din coș"
    override val assembledProduct = "Produs Asamblat"
    override val stockInHand = "Stoc disponibil"
    override val targetProduction = "Țintă producție"
    override val unitsFmt = "%d buc"
    override val viewProductComponents = "Vezi componentele produsului"
    override val unrecognizedBarcode = "Cod de bare nerecunoscut"
    override val apisUnavailableInfo = "API-urile online de căutare nu răspund momentan. Puteți introduce manual detaliile produsului mai jos."
    override val itemNameRequiredLabel = "Nume produs *"
    override val itemNamePlaceholder = "ex. Burgeri de vită, Ulei de măsline"
    override val scanProductNameDesc = "Scanează numele produsului cu camera"
    override val priceLabelFmt = "Preț (%s)"
    override val quantityInCartLabel = "Cantitate în coș"
    override val categorySectionLabel = "Categorie / Raion"
    override val categoryPlaceholder = "ex. Lactate, Legume, Brutărie"
    override val notesBrandLabel = "Note / Marcă"
    override val storeSpecialOfferOptional = "Ofertă Specială Magazin (Opțional)"
    override val saveItemAndAddToCart = "Salvează și Adaugă în Coș"
    override val autoDetectedOfferFmt = "Ofertă detectată automat: \"%s\""
    override val couldNotMatchDeal = "Nu s-a putut detecta oferta din etichetă. Selectați manual mai jos."
    override val selectedDealFmt = "Ofertă selectată: \"%s\""
    override val applyDiscountVoucher = "Aplică Voucher de Reducere"
    override val reduceBasketTotalHint = "Reduceți totalul coșului cu un voucher"
    override val quickPresets = "Valori Rapide"
    override val savedVouchersLibrary = "Vouchere Salvate"
    override val customVoucher = "Voucher Personalizat"
    override val voucherNameOptional = "Nume Voucher (Opțional)"
    override val voucherNamePlaceholder = "ex. Cupon, Voucher Fidelitate"
    override val discountAmount = "Valoare Reducere"
    override val voucherSuffix = "Voucher"
    override val copyListDesc = "Copiază lista"
    override val deleteListDesc = "Șterge lista"
    override val overBudget = "Buget depășit"
    override val oneItem = "1 produs"
    override val itemsCountFmt = "%d produse"
    override val bulletSavedFmt = "• Economisit %s%.2f"
    override val leftFmt = "%s%.2f rămas"
    override val overFmt = "+%s%.2f peste"
    override val emptyBasketHint = "Niciun produs în acest coș. Bifează caseta de mai sus și scanează coduri de bare sau caută produse!"
    override val hideCheckedItems = "Ascunde produsele bifate"
    override val showCheckedItems = "Arată produsele bifate"
    override val uncheckedLabel = "Nebifate"
    override val checkedLabel = "Bifate"
    override val totalLabel = "Total"
    override val voucherLabelFmt = "Voucher (%s)"
    override val removeVoucherDesc = "Elimină voucher"
    override val budgetBtn = "Buget"
    override val voucherBtn = "Voucher"
    override val swipeHint = "↔ Glisează produsul: Dreapta pt. bifare • Stânga pt. ștergere"
    override val removeItemTitle = "Elimină produs"
    override val removeItemConfirmFmt = "Eliminați \"%s\" din \"%s\"?"
    override val uncheck = "Debifează"
    override val check = "Bifează"
    override val uncheckItemDesc = "Debifează produs"
    override val checkItemDesc = "Bifează produs"
    override val collapseItemDesc = "Restrânge produs"
    override val expandItemDesc = "Extinde produs"
    override val savedWithDealFmt = "-%s%.2f"
    override val removeItemDesc = "Elimină produs"
    override val decreaseQtyDesc = "Scade cantitatea"
    override val increaseQtyDesc = "Crește cantitatea"
    override val itemsTitle = "Catalog Produse"
    override val addItem = "Adaugă Produs"
    override val searchCatalogPlaceholder = "Caută în catalog după nume sau cod..."
    override val selectAllFmt = "Selectează tot (%d)"
    override val deselectAllFmt = "Deselectează tot (%d)"
    override val addCountFmt = "Adaugă (%d)"
    override val deleteCountFmt = "Șterge (%d)"
    override val savedItemsCatalog = "Catalog Produse Salvate"
    override val savedProductsCountFmt = "%d Produse Salvate"
    override val itemsRoutingHint = "Bifați produsele mai jos, apoi apăsați 'Adaugă' mai sus pentru a le trimite în coșuri."
    override val noPartsBelowThreshold = "Niciun produs sub pragul minim"
    override val noItemsInCatalog = "Niciun produs găsit în catalog"
    override val deleteItemTitle = "Șterge Produs"
    override val deleteItemConfirmFmt = "Sigur doriți să ștergeți \"%s\"?\n\nVa fi eliminat și din coșurile active."
    override val deleteSelectedItemsTitle = "Șterge Produsele Selectate"
    override val deleteSelectedItemsConfirmFmt = "Sigur doriți să ștergeți %d produs(e) selectate?\n\nVor fi eliminate și din coșurile active."
    override val editShoppingItem = "Editează Produs"
    override val addNewShoppingItem = "Adaugă Produs Nou"
    override val barcodeItemCodeLabel = "Cod de bare / Cod produs"
    override val barcodeItemCodeOptional = "Cod de bare / Cod produs (Opțional)"
    override val clearBarcodeDesc = "Șterge codul de bare"
    override val autoGeneratedHint = "Generat automat dacă rămâne gol"
    override val itemNameRequiredError = "Numele produsului este obligatoriu"
    override val saveItem = "Salvează Produs"
    override val updatePriceInBasketsTitle = "Actualizați prețul în coșurile active?"
    override val updatePriceInBasketsBodyFmt = "Ați actualizat prețul unitar pentru \"%s\" la %s%.2f.\n\nDoriți să actualizați prețul și în toate coșurile active?"
    override val updateBasketsAndSave = "Actualizează Coșurile și Salvează"
    override val saveItemOnly = "Salvează Doar Produsul"
    override val addToBasketsTitle = "Adaugă în Coș(uri)"
    override val addToBasketsPromptFmt = "Selectați în ce coș(uri) să adăugați cele %d produs(e) selectate:"
    override val noBasketsExistYet = "Nu există încă coșuri create pe ecranul principal."
    override val addItems = "Adaugă Produse"
    override val offersTitle = "Oferte și Promoții"
    override val addOffer = "Adaugă Ofertă"
    override val offersLogoDesc = "Logo Oferte"
    override val searchOffersPlaceholder = "Caută oferte după nume sau magazin..."
    override val savedStorePromotions = "Promoții Salvate"
    override val activeDealsCountFmt = "%d Oferte Active"
    override val offersHint = "Bifați ofertele pentru ștergere în bloc sau apăsați 'Aplică pe Produs' pentru a asocia o ofertă."
    override val noOffersMatchFmt = "Nicio ofertă nu corespunde cu \"%s\""
    override val noOffersSavedYet = "Nicio ofertă salvată încă"
    override val tapCreateOfferHintFmt = "Apăsați '+ Adaugă Ofertă' mai jos pentru oferte 3 la 2, 2 la %s5 sau preț redus."
    override val deleteOfferTitle = "Șterge Oferta"
    override val deleteOfferConfirmFmt = "Sigur doriți să ștergeți oferta \"%s\"?"
    override val deleteSelectedOffersTitle = "Șterge Ofertele Selectate"
    override val deleteSelectedOffersConfirmFmt = "Sigur doriți să ștergeți %d ofertă(e) selectate?"
    override val storeFmt = "Magazin: %s"
    override val applyToItem = "Aplică pe Produs"
    override val editOfferDeal = "Editează Ofertă"
    override val createNewStoreOffer = "Creează Ofertă Nouă"
    override val promotionTypeRequired = "Tip Promoție *"
    override val selectPromotionType = "Selectează Tip Promoție"
    override val offerTitleOptional = "Titlu / Nume Ofertă (Opțional)"
    override val offerTitlePlaceholder = "ex. Lidl 3 la 2 Grătar"
    override val storeOptional = "Magazin / Supermarket (Opțional)"
    override val storePlaceholder = "ex. Lidl, Kaufland, Mega Image"
    override val notesOptional = "Note / Descriere (Opțional)"
    override val notesPlaceholder = "ex. Valabil până duminică"
    override val saveOffer = "Salvează Ofertă"
    override val applyOfferToProduct = "Aplică Ofertă pe Produs"
    override val searchProductPlaceholder = "Caută nume produs, cod de bare..."
    override val noProductsFound = "Niciun produs găsit"
    override val codePrefix = "Cod:"
    override val applyDeal = "Aplică Oferta"
    override val offerTypeOrSavedDeal = "Tip Ofertă sau Promoție Salvată"
    override val searchOfferPlaceholder = "Caută ofertă sau promoție salvată..."
    override val clearOfferSearchDesc = "Șterge căutarea ofertei"
    override val collapseOffersDesc = "Restrânge ofertele"
    override val expandOffersDesc = "Extinde ofertele"
    override val savedOffersLibrary = "Oferte Salvate"
    override val offerTypesHeader = "Tipuri de Oferte"
    override val noMatchingOffers = "Nicio ofertă găsită"
    override val offerNone = "Fără Ofertă"
    override val offerBuyXPayY = "Cumperi X Plătești Y (ex. 3 la 2)"
    override val offerMultiBuyFmt = "X la %sY (Pachet Multi-Buy)"
    override val offerReducedFmt = "Redus la %sY (Preț Special)"
    override val offerPercentOff = "% Reducere"
    override val offerMultiBuyBundleFmt = "Pachet Multi-Buy (ex. 2 la %s5)"
    override val offerReducedLongFmt = "Preț Special Redus (ex. Redus la %s2.50)"
    override val offerPercentLong = "Reducere Procentuală (ex. 20% Reducere)"
    override val offerVoucherLongFmt = "🎟️ Voucher Reducere Coș (%s Reducere)"
    override val buyQtyLabel = "Cant. Cumpărată"
    override val payQtyLabel = "Cant. Plătită"
    override val threeForTwo = "3 la 2"
    override val twoForOne = "2 la 1"
    override val bundlePriceLabel = "Preț Pachet"
    override val twoForAmountFmt = "2 la %s5"
    override val threeForAmountFmt = "3 la %s10"
    override val specialReducedPriceLabel = "Preț Special Redus"
    override val savePerItemFmt = "Economisești %s%.2f / produs (de la %s%.2f)"
    override val discountPercentLabel = "Reducere %"
    override val fiftyHalf = "50% (Jumătate)"
    override val percentOffFmt = "%s%% Reducere"
    override val basketVoucherDiscountLabel = "Reducere Voucher Coș"
    override val recognizedProductText = "Text Produs Recunoscut"
    override val ocrEditInstructions = "Tot textul găsit prin OCR este adăugat mai jos. Editați, adăugați sau ștergeți cuvinte pentru numele produsului:"
    override val productNameLabel = "Nume Produs"
    override val resetAll = "Resetează"
    override val individualLinesDetected = "Linii detectate individual (atinge pentru a seta):"
    override val useAsName = "Folosește ca Nume"
    override val historyTitle = "Istoric Cumpărături"
    override val reloadBasket = "Activează Coșul"
    override val reloadBasketTitle = "Activați Coșul?"
    override val reloadBasketConfirmFmt = "Mutați \"%s\" înapoi în Coșurile Active și eliminați-l din Istoric?"
    override val searchHistoryPlaceholder = "Caută în istoric după coș, produs sau dată..."
    override val noSavedBasketsYet = "Niciun coș salvat încă"
    override val noSavedBasketsSubtitle = "Când terminați cumpărăturile, apăsați 'Salvează' pe orice coș pentru a salva bonul aici."
    override val deleteSavedBasketTitle = "Șterge Coș Salvat"
    override val deleteSavedBasketConfirmFmt = "Sigur doriți să ștergeți coșul salvat din %s?"
    override val deleteSelectedReceiptsTitle = "Șterge Bonurile Selectate"
    override val deleteSelectedReceiptsConfirmFmt = "Sigur doriți să ștergeți %d bon(uri) selectate din istoric?"
    override val defaultHistoryBasketName = "Coș de Cumpărături"
    override val historyDateItemsFmt = "%s • %d produse"
    override val collapseItemsDesc = "Restrânge produsele"
    override val expandItemsDesc = "Extinde produsele"
    override val settingsTitle = "Setări și Date"
    override val currencyAndBudget = "Monedă și Buget"
    override val activeCurrencyFmt = "Simbol Monedă Activ: \"%s\""
    override val currencyDescription = "Alegeți simbolul monedei folosit pentru totaluri și prețurile produselor."
    override val customCurrencyChip = "Personalizat..."
    override val shoppingBudgetLimitFmt = "Limită Buget: %s%.2f"
    override val noShoppingBudgetLimit = "Nicio Limită de Buget Setată"
    override val budgetDescription = "Setați o limită de cheltuieli pentru coș. Veți primi o alertă când produsele ating sau depășesc suma."
    override val changeBudgetLimit = "Schimbă Limita de Buget"
    override val setBudgetLimit = "Setează Limită de Buget"
    override val clearLimit = "Șterge Limita"
    override val appAppearanceTheme = "Temă Aspect Aplicație"
    override val themeDescription = "Comutați între tema sistemului, tema luminoasă sau tema întunecată."
    override val themeSystem = "Sistem"
    override val themeLight = "Luminos ☀️"
    override val themeDark = "Întunecat 🌙"
    override val textCapitalizationMode = "Formatare Majuscule Text"
    override val capitalizationDescription = "Formatează automat numele coșurilor, produselor și titlurile ofertelor la tastare sau creare."
    override val capTitleCase = "Majusculă la Fiecare Cuvânt"
    override val capFirstLetter = "Doar Prima Literă"
    override val capNone = "Ca atare (Manual)"
    override val fullBackupAndRestore = "Backup Complet și Restaurare"
    override val completeJsonBackup = "Backup JSON Complet"
    override val completeJsonBackupSub = "Backup portabil cu produse, coșuri, oferte, istoric și setări."
    override val sqliteSnapshot = "Copie Bază de Date SQLite"
    override val sqliteSnapshotSub = "Backup și restaurare directă 1:1 pentru shopping_tracker.db."
    override val csvBackupAndExport = "Backup CSV și Export Date"
    override val itemsCatalogCsvTitle = "Catalog Produse"
    override val itemsCatalogCsvSub = "shopping_items.csv · Cod de bare, Nume, Preț, Categorie, Note"
    override val historyCsvTitle = "Istoric Cumpărături și Bonuri"
    override val historyCsvSub = "shopping_history.csv · Dată, Cost Total, Produse, Sumar"
    override val demoAndSeedData = "Date Demo"
    override val seedSampleDataTitle = "Încarcă Date Demo"
    override val seedSampleDataDesc = "Adaugă produse de test (Burgeri, Brânză, Ulei, Lapte) pentru a testa scanarea, prețurile și ofertele."
    override val loadSampleData = "Încarcă Date Demo"
    override val dangerZone = "Zonă Periculoasă"
    override val wipeAllLocalData = "Șterge Toate Datele Locale"
    override val wipeAllLocalDataDesc = "Șterge definitiv toate coșurile, produsele, ofertele și istoricul de pe acest dispozitiv."
    override val clearAllData = "Șterge Tot"
    override val externalServices = "Servicii Externe și Diagnostic"
    override val barcodeLookupApis = "API-uri Căutare Coduri de Bare"
    override val barcodeLookupApisDesc = "Testează OpenFoodFacts (.org & .net), OpenProductsFacts și OpenBeautyFacts."
    override val clearHealthResultsDesc = "Ascunde rezultatele testului"
    override val testingBarcodeApis = "Se testează API-urile de coduri de bare..."
    override val healthyResultFmt = "%s · Funcțional (HTTP %d, %d ms)"
    override val failedResultFmt = "%s · %s (%d ms)"
    override val sampleResultFmt = "Exemplu (%s): %s"
    override val checkingApis = "Se verifică..."
    override val runBarcodeHealthCheck = "Rulează Test API Coduri de Bare"
    override val aboutDeveloper = "Despre Aplicație"
    override val appDisplayName = "Shopping Tracker"
    override val appTagline = "Monitorizare cumpărături și stocuri offline-first"
    override val tagJetpackCompose = "Jetpack Compose"
    override val tagRoomDb = "Room DB"
    override val tagMlKitOcr = "ML Kit OCR"
    override val tagOfflineFirst = "Offline-First"
    override val developedByLabel = "Dezvoltat și proiectat de"
    override val vibeCodedWithAi = "Vibe coded with AI"
    override val shareAppApk = "Partajează APK Aplicație"
    override val setBudgetDialogTitle = "Setează Limita de Buget"
    override val setBudgetDialogBody = "Setați o limită țintă pentru coșul de cumpărături. Veți primi o alertă vizuală când totalul atinge sau depășește limita."
    override val targetBudgetLabelFmt = "Buget Țintă (%s)"
    override val saveTarget = "Salvează Ținta"
    override val setCustomCurrencyTitle = "Setează Simbol Monedă Personalizat"
    override val setCustomCurrencyBody = "Introduceți un simbol sau cod de monedă (ex. RON, EUR, £, $):"
    override val currencySymbolLabel = "Simbol / Cod Monedă"
    override val confirmDataWipeTitle = "Confirmă Ștergerea Datelor"
    override val confirmDataWipeBody = "Sigur doriți să ștergeți toate datele locale? Toate coșurile, produsele, ofertele și istoricul vor fi șterse definitiv."
    override val clearAllBtn = "Șterge Tot"
    override val exportCsvBtn = "Exportă CSV"
    override val importCsvBtn = "Importă CSV"
    override val vmCreatedBasketAddedFmt = "Coșul \"%s\" a fost creat și s-a adăugat %s"
    override val vmCreatedBasketFmt = "Coșul \"%s\" a fost creat"
    override val vmDuplicatedBasketFmt = "Coș duplicat ca \"%s\""
    override val vmRenamedBasketFmt = "Coș redenumit în \"%s\""
    override val vmBudgetSetFmt = "Buget setat la %s%.2f pentru \"%s\""
    override val vmBudgetClearedFmt = "Limită de buget ștearsă pentru \"%s\""
    override val vmBudgetSetMultiFmt = "Buget de %s%.2f setat pentru %d coș(uri)"
    override val vmBudgetClearedMultiFmt = "Limită de buget ștearsă pentru %d coș(uri)"
    override val vmAppliedVoucherBasketFmt = "S-a aplicat %s (-%s%.2f) în coș"
    override val vmRemovedItemOrVoucher = "Produs/voucher eliminat din coș"
    override val vmDeletedBasketFmt = "Coșul \"%s\" a fost șters"
    override val vmDeletedBasketsFmt = "%d coș(uri) selectate au fost șterse"
    override val vmAppliedVoucherFmt = "S-a aplicat %s (-%s%.2f)"
    override val vmRemovedVoucherFmt = "Voucherul %s a fost eliminat"
    override val vmAddedToBasketsMultiFmt = "✓ Adăugat %s%s în %d coșuri selectate"
    override val vmAddedToBasketSingleFmt = "✓ Adăugat %s%s în coș"
    override val vmAddedToBasketCartFmt = "✓ Adăugat %s%s în coș (În coș: %d)"
    override val vmFoundOnlineFmt = "Găsit online: %s. Introduceți prețul pentru a salva."
    override val vmCartQtyUpdatedFmt = "Cantitate actualizată (%s)"
    override val vmProductStockUpdatedFmt = "Stoc produs actualizat (%s)"
    override val vmAddedPieceToBasketFmt = "✓ Adăugat %s în coș"
    override val vmAppliedDealFmt = "Ofertă aplicată: \"%s\""
    override val vmListEmptyFmt = "Coșul \"%s\" este gol!"
    override val vmSavedBasketToHistoryFmt = "Coșul \"%s\" (%s%.2f) a fost salvat în Istoric!"
    override val vmClearedBasketFmt = "Coșul \"%s\" a fost golit"
    override val vmSelectedBasketsEmpty = "Coșurile selectate sunt goale!"
    override val vmClearedSelectedBasketsFmt = "Produse șterse din %d coș(uri) selectate"
    override val vmCreatedProductFmt = "Produs creat: %s"
    override val vmSavedToHistoryKeepItems = "Coș salvat în Istoric! Produsele rămân în Catalog."
    override val vmBasketClearedKeepItems = "Coș golit. Produsele rămân salvate în Catalog."
    override val vmPurchasedItemsFallback = "Produse cumpărate"
    override val vmSavedPieceUpdatedBasketsFmt = "Salvat %s și preț actualizat în coșurile active"
    override val vmSavedPieceFmt = "Salvat %s"
    override val vmDeletedPieceFmt = "Șters %s"
    override val vmCannotDeleteLinkedItem = "Nu se poate șterge: produsul este asociat unui produs asamblat."
    override val vmDeletedSelectedItemsFmt = "%d produs(e) selectate au fost șterse"
    override val vmCouldNotDeleteLinkedItems = "Unele produse asociate cu produse asamblate nu au putut fi șterse"
    override val vmAddedItemsToBasketsFmt = "%d produs(e) adăugate în coșurile selectate!"
    override val vmOfferSavedFmt = "Oferta \"%s\" a fost salvată."
    override val vmOfferDeletedFmt = "Oferta \"%s\" a fost ștearsă."
    override val vmDeletedOffersFmt = "%d ofertă(e) selectate au fost șterse."
    override val vmAppliedDealToItemFmt = "Oferta \"%s\" a fost aplicată pe %s."
    override val vmTripReceiptDeleted = "Bonul a fost șters"
    override val vmDeletedReceiptsFmt = "%d bon(uri) selectate au fost șterse"
    override val vmReloadedBasketFmt = "Coșul \"%s\" este acum activ!"
    override val vmFailedReloadBasketFmt = "Eroare la activarea coșului: %s"
    override val vmCurrencyUpdatedFmt = "Moneda a fost schimbată în %s"
    override val vmBudgetLimitUpdated = "Limita de buget a fost actualizată"
    override val vmThemeUpdatedFmt = "Tema aplicației a fost schimbată în %s"
    override val vmCapModeUpdated = "Modul de majuscule a fost actualizat"
    override val vmAllApisHealthyFmt = "Toate cele %d API-uri sunt funcționale"
    override val vmSomeApisReachableFmt = "%d/%d API-uri disponibile"
    override val vmExportedItemsCsv = "Fișierul shopping_items.csv a fost exportat cu succes"
    override val vmFailedExportItemsFmt = "Eroare la exportul produselor: %s"
    override val vmImportedItemsFmt = "%d produs(e) importate în Catalog."
    override val vmFailedImportItemsFmt = "Eroare la importul produselor: %s"
    override val vmExportedHistoryCsv = "Fișierul shopping_history.csv a fost exportat cu succes"
    override val vmFailedExportHistoryFmt = "Eroare la exportul istoricului: %s"
    override val vmImportedReceiptsFmt = "%d bon(uri) importate în Istoric."
    override val vmFailedImportHistoryFmt = "Eroare la importul istoricului: %s"
    override val vmFullBackupSaved = "Backup-ul complet a fost salvat cu succes!"
    override val vmFailedExportBackupFmt = "Eroare la exportul backup-ului: %s"
    override val vmRestoredSummaryFmt = "Restaurate %d produse, %d coșuri, %d oferte și %d bonuri!"
    override val vmCouldNotParseBackup = "Fișierul de backup nu a putut fi citit."
    override val vmFailedRestoreBackupFmt = "Eroare la restaurarea backup-ului: %s"
    override val vmRawDbExported = "Baza de date SQLite a fost exportată (shopping_tracker.db)!"
    override val vmFailedExportDbFmt = "Eroare la exportul bazei de date: %s"
    override val vmCouldNotParseDb = "Fișierul bazei de date nu a putut fi citit."
    override val vmFailedRestoreDbFmt = "Eroare la restaurarea bazei de date: %s"
    override val vmShareBackupSubject = "Backup Complet Shopping Tracker"
    override val vmShareBackupChooser = "Partajează Backup prin Google Drive, Email etc."
    override val vmFailedShareBackupFmt = "Eroare la partajarea backup-ului: %s"
    override val vmShareDbSubject = "Bază de Date Shopping Tracker (.db)"
    override val vmShareDbChooser = "Partajează Baza de Date prin Google Drive, Email etc."
    override val vmFailedShareDbFmt = "Eroare la partajarea bazei de date: %s"
    override val vmShareApkSubject = "Aplicația Shopping Tracker"
    override val vmShareApkText = "Iată fișierul de instalare Android (APK) pentru Shopping Tracker."
    override val vmShareApkChooser = "Partajează APK prin Bluetooth, Drive, Email..."
    override val vmFailedShareApkFmt = "Eroare la partajarea APK: %s"
    override val vmSampleDataLoaded = "Produsele și ofertele demo au fost încărcate."
    override val vmFailedSeedDataFmt = "Eroare la încărcarea datelor demo: %s"
    override val vmAllDataCleared = "Toate coșurile, produsele, ofertele și istoricul au fost șterse."
    override val vmFailedClearDataFmt = "Eroare la ștergerea datelor: %s"
    override val switchCurrencyTitle = "Schimbați moneda?"
    override val switchCurrencyBodyFmt = "Ați schimbat limba în %s. Doriți să schimbați și moneda în %s?"
    override val switchCurrencyConfirmFmt = "Schimbă în %s"
    override val keepCurrencyFmt = "Păstrează %s"
    override val languageNameEnglish = "Engleză (UK)"
    override val languageNameRomanian = "Română"
    override val vmExportedFileFmt = "Exportat cu succes: %s"
    override val vmSavedBackupFileFmt = "Backup complet salvat: %s"
    override val vmSavedDbFileFmt = "Baza de date SQLite exportată: %s"
    override val restorePickerTitleFmt = "Selectați fișier %s"
    override val availableFilesByDate = "Fișiere disponibile (cele mai recente):"
    override val noRecentFilesFoundFmt = "Nu s-au găsit fișiere %s accesibile direct. Folosiți 'Răsfoiește dispozitiv / Cloud' mai jos pentru a alege din orice locație."
    override val browseDeviceCloud = "Răsfoiește dispozitiv / Cloud..."
    override val wrongFileTypeFmt = "Fișierul selectat (%s) nu corespunde formatului %s așteptat."
    override val mixAndMatchBtn = "Ofertă Combinată"
    override val mixAndMatchTitle = "Ofertă Combinată"
    override val mixAndMatchSubtitle = "Combină 2 sau mai multe produse din coș sub o singură ofertă (ex. Struguri Roșii + Verzi 2 la 20 RON)."
    override val selectItemsToCombine = "1. Selectează Produsele"
    override val needTwoItemsHint = "Selectați cel puțin 2 produse pentru o ofertă combinată."
    override val applyMixAndMatch = "Aplică Oferta"
    override val mixAndMatchBadge = "Ofertă Combinată"
    override val ungroupDealDesc = "Anulează gruparea ofertei"
    override val vmAppliedMixAndMatchFmt = "Ofertă Combinată (%s) aplicată pe %d produse!"
    override val vmUngroupedDeal = "Gruparea ofertei a fost eliminată"
    override val onboardingTitle = "Bine ai venit la Shopping Tracker"
    override val onboardingSubtitle = "Alege limba și moneda preferată pentru a începe. Le poți schimba oricând din Setări."
    override val onboardingLanguageLabel = "Limbă"
    override val onboardingCurrencyLabel = "Monedă"
    override val onboardingCustomCurrencyLabel = "Simbol personalizat (opțional)"
    override val onboardingGetStarted = "Începe"
    override val tutorialHeaderTitle = "Tutorial Rapid"
    override val tutorialHeaderSubtitle = "Un scurt tur al funcțiilor principale ale aplicației"
    override val tutorialSkip = "Sari peste"
    override val tutorialBack = "Înapoi"
    override val tutorialNext = "Următorul"
    override val tutorialDone = "Începe"
    override val tutorialDontShowAgain = "Nu mai arăta la următoarea pornire"
    override val tutorialStep1Title = "Scanare Rapidă & Coșuri"
    override val tutorialStep1Desc = "Scanează coduri de bare sau caută în catalog, selectează mai multe produse odată și urmărește totalul și bugetul fiecărui coș."
    override val tutorialStep2Title = "Glisează pentru Bifare sau Ștergere"
    override val tutorialStep2Desc = "Glisează spre dreapta pe un produs pentru a-l bifa, sau spre stânga pentru a-l elimina. Produsele bifate se restrâng automat."
    override val tutorialStep3Title = "Oferte Combinate & Promoții"
    override val tutorialStep3Desc = "Combină produse diferite într-o Ofertă Combinată (ex. 2 la 20 RON), sau salvează oferte 3 la preț de 2, reduceri procentuale și preț redus."
    override val tutorialStep4Title = "Catalog & Istoric Bonuri"
    override val tutorialStep4Desc = "Salvează coșurile în Istoric ca bonuri detaliate cu ofertele combinate păstrate și apasă Activează Coșul pentru a reutiliza o listă."
    override val tutorialStep5Title = "Backup, Restaurare & Export"
    override val tutorialStep5Desc = "Păstrează în siguranță produsele, coșurile, ofertele și istoricul cu backup complet JSON/SQLite și export/import CSV din Setări."
    override val showTutorialOnStartupLabel = "Arată Tutorialul Rapid la următoarea pornire"
    override val viewQuickTutorialBtn = "Vezi Tutorialul Rapid"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishUkStrings }
