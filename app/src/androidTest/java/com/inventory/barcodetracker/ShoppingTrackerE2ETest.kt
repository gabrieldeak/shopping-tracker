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
        composeRule.onNodeWithContentDescription("Expand details").performClick()
        composeRule.onNodeWithText("Apply to Item").assertIsDisplayed()
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
            composeRule.onAllNodesWithText("Reload Basket").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify itemized card & top actions
        composeRule.onNodeWithText("Reload Basket").assertIsDisplayed()
        composeRule.onAllNodesWithText("Sourdough Bread").onLast().assertIsDisplayed()
        composeRule.onNodeWithText("Qty: 1").assertIsDisplayed()

        // Reload Basket back to Quick Scan
        composeRule.onNodeWithText("Reload Basket").performClick()
        composeRule.onNodeWithText("Quick Scan").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.onAllNodesWithText("Weekend Run", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testSettingsClearAllDataClearsSelectionBar() {
        // Create a basket (auto-selected -> shows "1 selected")
        composeRule.onNodeWithText("New Basket").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
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
}
