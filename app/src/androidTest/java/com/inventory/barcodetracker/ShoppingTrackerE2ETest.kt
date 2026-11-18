package com.inventory.barcodetracker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.inventory.barcodetracker.data.local.AppDatabase
import com.inventory.barcodetracker.data.local.preferences.CurrencyPreferences
import com.inventory.barcodetracker.data.repository.InventoryRepository
import com.inventory.barcodetracker.di.AppContainer
import com.inventory.barcodetracker.scanner.BarcodeScannerService
import com.inventory.barcodetracker.ui.navigation.AppNavHost
import com.inventory.barcodetracker.ui.theme.BarcodeInventoryTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingTrackerE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var testContainer: AppContainer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repository = InventoryRepository(database.inventoryDao())
        val scannerService = BarcodeScannerService(context)
        val preferences = CurrencyPreferences(context)
        preferences.setAppLanguage("en")
        preferences.setCurrencySymbol("£")
        preferences.setBudgetLimit(0.0)

        testContainer = object : AppContainer {
            override val inventoryRepository: InventoryRepository = repository
            override val barcodeScannerService: BarcodeScannerService = scannerService
            override val currencyPreferences: CurrencyPreferences = preferences
        }

        composeRule.setContent {
            BarcodeInventoryTheme {
                AppNavHost(appContainer = testContainer)
            }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testQuickScanEmptyState_andBasketItemCheckAndRemoveFlow() {
        // 1. Fresh DB starts empty (no auto-created Main Shopping Basket)
        composeRule.onNodeWithText("New Basket").assertIsDisplayed()
        composeRule.onNodeWithText("Main Shopping Basket").assertDoesNotExist()

        // 2. Create a basket
        composeRule.onNodeWithText("New Basket").performClick()
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Weekly Shop")
        composeRule.onNodeWithText("Create Basket").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Weekly Shop", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Switch to Items tab, add an item without barcode
        composeRule.onNodeWithText("Items").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Add Item").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Add Item").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Item Name *").performTextInput("Whole Milk")
        composeRule.onNodeWithText("Price (", substring = true).performTextInput("2.50")
        composeRule.onNodeWithText("Save Item").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Whole Milk").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Switch back to Quick Scan, search "Whole Milk" and add to Weekly Shop
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Whole Milk")

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodes(hasText("Whole Milk") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Whole Milk") and !hasSetTextAction()).performClick()

        // Clear search so basket card is visible
        composeRule.onNodeWithContentDescription("Clear search").performClick()

        // 5. Expand Weekly Shop and verify Whole Milk + Qty: 1
        composeRule.onNodeWithContentDescription("Expand").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Whole Milk").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Qty: 1").assertIsDisplayed()

        // 6. Check item -> collapses into "(1) Show checked items"
        composeRule.onNodeWithContentDescription("Check item").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("(1) Show checked items").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Uncheck item").assertCountEquals(0)

        // Expand Checked Items section -> Uncheck item is visible
        composeRule.onNodeWithText("(1) Show checked items").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Uncheck item").fetchSemanticsNodes().isNotEmpty()
        }

        // 7. Swipe left on Whole Milk -> Remove Item confirmation -> confirm
        composeRule.onAllNodesWithText("Whole Milk").onFirst().performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Remove Item").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Remove").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Uncheck item").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun testOffersCreateWithOptionalNameAndExpand() {
        composeRule.onNodeWithText("Offers").performClick()

        composeRule.onNodeWithContentDescription("Add Offer").performClick()
        composeRule.onNodeWithText("Buy Qty").performTextInput("3")
        composeRule.onNodeWithText("Pay Qty").performTextInput("2")
        // Leave Offer Title / Name (Optional) blank
        composeRule.onNodeWithText("Save Offer").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("3 for 2").fetchSemanticsNodes().isNotEmpty()
        }

        // Expand card details
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Apply to Item").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithContentDescription("Edit").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
    }

    @Test
    fun testHistoryFinishBasketItemizedAndReload() {
        // Create basket & item
        composeRule.onNodeWithText("New Basket").performClick()
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Weekend Run")
        composeRule.onNodeWithText("Create Basket").performClick()

        composeRule.onNodeWithText("Items").performClick()
        composeRule.onNodeWithContentDescription("Add Item").performClick()
        composeRule.onNodeWithText("Item Name *").performTextInput("Sourdough Bread")
        composeRule.onNodeWithText("Price (", substring = true).performTextInput("3.50")
        composeRule.onNodeWithText("Save Item").performClick()

        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Sourdough Bread")
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodes(hasText("Sourdough Bread") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Sourdough Bread") and !hasSetTextAction()).performClick()
        composeRule.onNodeWithContentDescription("Clear search").performClick()

        // Save selected basket to History using the "Save" button in the selection bar
        composeRule.onNodeWithText("Save").performClick()

        // Switch to History tab & expand receipt
        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand items").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand items").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Make Active").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify itemized card & top actions
        composeRule.onNodeWithText("Make Active").assertIsDisplayed()
        composeRule.onAllNodesWithText("Sourdough Bread").onLast().assertIsDisplayed()
        composeRule.onNodeWithText("Qty: 1").assertIsDisplayed()

        // Make Active back to Quick Scan
        composeRule.onNodeWithText("Make Active").performClick()
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Weekend Run", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testSettingsClearAllDataClearsSelectionBar() {
        // Create a basket (auto-selected -> shows "1 selected")
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("New Basket").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New Basket")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Basket Name / Store").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Temp Basket")
        composeRule.onNodeWithText("Create Basket").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("1 selected").fetchSemanticsNodes().isNotEmpty()
        }

        // Go to Settings -> scroll to Clear All Data -> confirm
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Clear All Data").performScrollTo().performClick()
        composeRule.onNodeWithText("Clear All").performClick()

        // Return to Quick Scan -> selection bar and basket are gone
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("1 selected").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Temp Basket").assertDoesNotExist()
    }

    @Test
    fun testAddingSearchedItemWithNoBasketPromptsCreateBasketAndAddsItem() {
        // Add item to catalog while no baskets exist
        composeRule.onNodeWithText("Items").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Add Item").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Add Item").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Item Name *").performTextInput("Butter")
        composeRule.onNodeWithText("Price (", substring = true).performTextInput("1.80")
        composeRule.onNodeWithText("Save Item").performClick()

        // Search "Butter" on Quick Scan with no baskets -> tapping it opens New Basket dialog
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Butter")
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodes(hasText("Butter") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Butter") and !hasSetTextAction()).performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Create New Shopping Basket").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Corner Shop")
        composeRule.onNodeWithText("Create Basket").performClick()

        // Basket is created and Butter is automatically added inside it
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Butter").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Qty: 1").assertIsDisplayed()
    }

    @Test
    fun testLanguageSwitcherAndCurrencyPrompt() {
        // Default is English (UK) with £
        composeRule.onNodeWithText("🇬🇧 EN").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("🇷🇴 Română").performClick()

        // Currency switch prompt appears (since currency is £, not RON)
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Schimbați moneda?").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Schimbă în RON").performClick()

        // UI & bottom tabs are translated to Romanian
        composeRule.onNodeWithText("Coș Nou").assertIsDisplayed()
        composeRule.onNodeWithText("Istoric").assertIsDisplayed()
        composeRule.onNodeWithText("Setări").assertIsDisplayed()

        // Switch back to English (UK) -> prompts to switch back to £
        composeRule.onNodeWithText("🇷🇴 RO").performClick()
        composeRule.onNodeWithText("🇬🇧 English (UK)").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Switch Currency?").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Switch to £").performClick()
        composeRule.onNodeWithText("New Basket").assertIsDisplayed()
    }

    @Test
    fun testMultiSelectSearchBatchAddAndMixAndMatchHistoryFlow() {
        // 1. Create basket
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("New Basket").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("New Basket").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Basket Name / Store").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Fruit Run")
        composeRule.onNodeWithText("Create Basket").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Create Basket").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithText("Fruit Run", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Add two grape varieties to catalog
        composeRule.onNodeWithText("Items").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Search catalog by name or barcode...").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("Red Grapes" to "2.50", "Green Grapes" to "2.48").forEach { (name, price) ->
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule.onAllNodesWithContentDescription("Add Item").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Add Item")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Item Name *").performTextInput(name)
            composeRule.onNodeWithText("Price (", substring = true).performTextInput(price)
            composeRule.onNodeWithText("Save Item").performClick()
            composeRule.waitUntil(timeoutMillis = 3000) {
                composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isEmpty() &&
                    composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            }
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            Thread.sleep(300)
        }

        // 3. Search "Grapes" on Quick Scan -> check both items -> tap Add (2)
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Grapes")
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodes(isToggleable() and isOff()).fetchSemanticsNodes().size >= 2
        }
        val checkboxes = composeRule.onAllNodes(isToggleable())
        checkboxes[0].performClick()
        checkboxes[1].performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Add (2)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Add (2)").performClick()
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        androidx.test.espresso.Espresso.closeSoftKeyboard()

        // 4. Expand basket -> open Mix & Match -> select both -> apply 2 for £4.00
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Mix & Match").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mix & Match")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Apply Mix & Match").fetchSemanticsNodes().isNotEmpty()
        }

        // Apply Mix & Match should be disabled before selecting 2 items & entering price
        composeRule.onNodeWithText("Apply Mix & Match").assertIsNotEnabled()
        composeRule.onAllNodesWithText("Red Grapes").onLast().performClick()
        composeRule.onAllNodesWithText("Green Grapes").onLast().performClick()
        composeRule.onNodeWithText("Apply Mix & Match").assertIsNotEnabled() // Bundle Price still empty!

        composeRule.onNodeWithText("Bundle Price").performTextInput("4.00")
        composeRule.onNodeWithText("Apply Mix & Match").assertIsEnabled().performClick()

        // Verify basket savings & total
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("• Saved £0.98").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("£4.00").assertIsDisplayed()

        // 5. Save to History -> verify Mix & Match box in History -> Make Active
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand items").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand items").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("🏷️ Mix & Match").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Make Active").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Make Active")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)

        // 6. Back on Quick Scan -> Mix & Match group is preserved, and ungrouping removes it cleanly
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Fruit Run", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithContentDescription("Expand")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        }
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Ungroup Mix & Match deal").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Ungroup Mix & Match deal").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Ungroup Mix & Match deal").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithText("2 for £4.00").assertCountEquals(0)
    }

    @Test
    fun testOfferValidationDisablesSaveWhenEmpty() {
        composeRule.onNodeWithText("Offers").performClick()
        composeRule.onNodeWithContentDescription("Add Offer").performClick()
        composeRule.onNodeWithText("Save Offer").assertIsNotEnabled()

        composeRule.onNodeWithText("Buy Qty").performTextInput("3")
        composeRule.onNodeWithText("Save Offer").assertIsNotEnabled()

        composeRule.onNodeWithText("Pay Qty").performTextInput("2")
        composeRule.onNodeWithText("Save Offer").assertIsEnabled()
    }

    @Test
    fun testMultiSelectAddWithNoBasketPromptsCreateAndRemovingItemPrunesMixAndMatch() {
        // 1. Add two items to catalog while no baskets exist
        composeRule.onNodeWithText("Items").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Search catalog by name or barcode...").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("Red Apples" to "2.20", "Green Apples" to "2.30").forEach { (name, price) ->
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule.onAllNodesWithContentDescription("Add Item").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Add Item")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Item Name *").performTextInput(name)
            composeRule.onNodeWithText("Price (", substring = true).performTextInput(price)
            composeRule.onNodeWithText("Save Item").performClick()
            composeRule.waitUntil(timeoutMillis = 3000) {
                composeRule.onAllNodesWithText("Item Name *").fetchSemanticsNodes().isEmpty() &&
                    composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            }
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            Thread.sleep(300)
        }

        // 2. Search "Apples" on Quick Scan with NO baskets -> select both -> tap Add (2) -> prompts Create Basket
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Apples")
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodes(isToggleable() and isOff()).fetchSemanticsNodes().size >= 2
        }
        val checkboxes = composeRule.onAllNodes(isToggleable())
        checkboxes[0].performClick()
        checkboxes[1].performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Add (2)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Add (2)").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Basket Name / Store").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Basket Name / Store").performTextInput("Orchard")
        composeRule.onNodeWithText("Create Basket").performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Create Basket").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithText("Orchard", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        androidx.test.espresso.Espresso.closeSoftKeyboard()

        // 3. Expand Orchard -> apply Mix & Match 2 for £4.00
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Expand")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Mix & Match").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mix & Match")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Apply Mix & Match").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Red Apples").onLast().performClick()
        composeRule.onAllNodesWithText("Green Apples").onLast().performClick()
        composeRule.onNodeWithText("Bundle Price").performTextInput("4.00")
        composeRule.onNodeWithText("Apply Mix & Match").assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Ungroup Mix & Match deal").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Swipe left to remove Green Apples -> only 1 item remains -> Mix & Match grouping is auto-removed
        composeRule.onAllNodesWithText("Green Apples").onFirst().performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Remove Item").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Remove").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithContentDescription("Ungroup Mix & Match deal").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithText("2 for £4.00").assertCountEquals(0)
        composeRule.onNodeWithText("Red Apples").assertIsDisplayed()
    }
}
