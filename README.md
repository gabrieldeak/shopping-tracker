# Shopping Tracker (Android)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Android E2E Tests](https://github.com/gabrieldeak/shopping-tracker/actions/workflows/android-tests.yml/badge.svg)](https://github.com/gabrieldeak/shopping-tracker/actions/workflows/android-tests.yml)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A local-first, offline-ready Android application built with **Kotlin** and **Jetpack Compose**. Developed & Designed by **G.A.D** and ✨ *Vibe coded with AI*. Designed for fast in-store shopping, barcode scanning, per-basket item tracking, budget limits, multi-buy offer management, OCR label recognition, and receipt history.

---

## ✨ Key Features (Version 3.0.4)

- **First-Launch Language & Currency Setup + Quick Tutorial**: Shown automatically on fresh installs or after clearing app data to pick **English (UK)** / **Română** and preferred currency (`£`, `RON`, `€`, `$`, or custom symbol), followed by a skippable 5-step visual **Quick Tutorial** (`Tutorial Rapid`) with localized UI previews, a *"Don't show on next startup"* checkbox, and a **Show on next startup** toggle + **View Quick Tutorial** replay button in **Settings → About & Developer Info**.
- **5-Tab Swipeable Navigation**:
  - **Quick Scan** (`ui/screens/quickscan/`): Top-right **English (UK) / Română** language switcher (with optional currency switch prompt between `£` and `RON`), unified search + inline barcode scan bar, multi-select search results to add multiple items in one go, lookup loading indicator with **Cancel & Enter Manually** and API-unavailable info banner, expandable **Shopping Basket Cards** with live subtotals, **Mix & Match** multi-item offers (e.g. Red + Green Grapes `2 for £4.00`), budget progress, vouchers, duplicate, rename, finish & save, multi-select scan routing, automatic **Create Basket** prompt when adding an item with no baskets, and a **"Scan Another?"** prompt for rapid multi-item scanning.
  - **Items** (`ui/screens/items/`): Clean product catalog without global counts (quantities live inside each basket), optional barcodes, price & offer assignment, multi-select batch add-to-basket / delete, and cascade removal from active baskets on delete.
  - **Offers** (`ui/screens/offers/`): Modern uniform-width promotion pills (`2 for £4.00`, `3 for 2`, `% Off`, etc.) with input validation, optional name, store, and notes in expandable details, plus camera OCR deal scanning.
  - **History** (`ui/screens/history/`): Expandable receipt cards with modern itemized rows, boxed **Mix & Match** deal groups, **Make Active** (`Activează Coșul`) reactivation preserving Mix & Match groupings, and inline **Select All / Delete (N)** controls.
  - **Settings** (`ui/screens/settings/`): **£ (GBP)** default currency, budget limit, theme, smart text capitalization, **Full Backup & Database Restore** (idempotent JSON & SQLite `.db` covering items, baskets, basket items + checked state + `offerGroupId`, offers, receipts, language, currency, and preferences), dynamic saved filenames in notifications, type/date-sorted **Restore** file picker, **CSV Backup & Data Export** (including multi-line History CSV import), **Barcode Lookup APIs** health check (`OpenFoodFacts .org/.net`, `OpenProductsFacts`, `OpenBeautyFacts`), sample data seeding, and GitHub Release update checks.
- **Per-Basket Item Counts & Swipe Actions**:
  - Item quantities are linked to each shopping basket.
  - **Swipe Right** on a basket item to mark it checked/unchecked (stored in Room; **Checked Items** section is collapsed by default).
  - **Swipe Left** on a basket item to delete it with confirmation.
  - **Swipe Right / Left** on a basket card to Duplicate or Delete.
- **Modern Offer & Dialog Inputs**:
  - Compact `AppOutlinedTextField` styling across dialogs with proper floating-label clearance.
  - Empty-by-default quantity/price inputs in `OfferDetailsEditor` with quick preset pills (`3 for 2`, `2 for 1`) and parameter validation.
- **Safe Room Database Migrations (`7 → 8 → 9`)**: Adds `isChecked` and `offerGroupId` to `shopping_list_items` with zero data loss.

---

## 🛠️ Tech Stack & Architecture

Built following **MVVM** architecture and a **Repository** pattern.

| Component | Library / Framework | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern Kotlin with Coroutines & `Flow` |
| **UI Framework** | Jetpack Compose | Declarative Material 3 UI with `HorizontalPager` |
| **Database** | Room Database | SQLite ORM (v9 with `shopping_lists` & `shopping_list_items.isChecked` / `offerGroupId`) |
| **DI** | Manual DI (`AppContainer`) | Lightweight, zero-reflection dependency injection |
| **Barcode Scanner** | Google Code Scanner | Play Services code scanner API |
| **Text Recognition** | Google ML Kit | On-device OCR for text and deal label parsing |
| **API Client** | Open*Facts APIs | Multi-source barcode lookup (`OpenFoodFacts .org/.net`, `OpenProductsFacts`, `OpenBeautyFacts`) |
| **Preferences** | SharedPreferences / Flow | Reactive preferences for language (`en`/`ro`), currency, budget, theme, and capitalization |

---

## 📁 Project Structure

```text
app/src/main/java/com/inventory/barcodetracker/
├── data/
│   ├── local/          # Room AppDatabase (v9), InventoryDao, Entities, Preferences
│   └── repository/     # InventoryRepository
├── di/                 # AppContainer
├── scanner/            # BarcodeScannerService
├── service/            # BackupRestoreService, CsvService, OCR, OpenFoodFacts, UpdateCheckService
└── ui/
    ├── components/     # AppButtons, AppTextField, OfferDetailsEditor, OfferSelectorDropdown, ShoppingListCard, etc.
    ├── i18n/           # AppStrings (English UK & Romanian)
    ├── navigation/     # NavDestination (Screen.QuickScan, Items, Offers, History, Settings), AppNavHost
    └── screens/
        ├── quickscan/  # QuickScanScreen, QuickScanViewModel, ScanResolutionSheet, ApplyVoucherDialog, MixAndMatchOfferDialog
        ├── items/      # ItemsScreen, ItemsViewModel, ItemDialog
        ├── offers/     # OffersScreen, OffersViewModel, OfferDialog, ApplyOfferToItemDialog
        ├── history/    # HistoryScreen, HistoryViewModel
        └── settings/   # SettingsScreen, SettingsViewModel
```

---

## 🚀 Building & Testing

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run Compose UI E2E Tests (requires connected device or emulator)
```bash
./gradlew connectedDebugAndroidTest
```
Covers 8 end-to-end Compose UI flows in [`ShoppingTrackerE2ETest.kt`](app/src/androidTest/java/com/inventory/barcodetracker/ShoppingTrackerE2ETest.kt):
- Empty state, basket creation, catalog item add/search, item check/uncheck, and swipe-to-remove
- Empty-basket prompt when adding a searched item
- Multi-select search batch-add (`Add (2)`), `Mix & Match` deal creation/validation, History `Mix & Match` box, `Make Active` reactivation, and ungrouping
- History itemized receipt expansion and `Make Active` reload
- Offers creation with optional title, empty-field validation, and card expansion
- Settings `Clear All Data` resetting active basket and selection bar
- Runtime `EN` ↔ `RO` language switcher and currency switch prompt (`RON` / `£`)

Test results are published automatically in GitHub Actions via [android-tests.yml](.github/workflows/android-tests.yml).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
