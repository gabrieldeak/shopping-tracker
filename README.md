# Shopping & Inventory Tracker (Android)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A local-first, offline-ready Android application built with **Kotlin** and **Jetpack Compose**. Developed & Designed by **G.A.D** and ✨ *Vibe coded with AI*. Designed for fast in-store shopping, barcode scanning, budget tracking, dedicated multi-buy offer management, comprehensive field searching, OCR label recognition, shopping history receipts, and Bill of Materials (BOM) product assembly.

---

## ✨ Key Features

- ⚙️ **About & Developer Info**:
  - **Developed & Designed by G.A.D**
  - **✨ Vibe Coded with AI**
- 🔍 **Deep Multi-Field Search**: Search filters across the app (Quick Scan, My Items, Offers, BOM assembly, and Offer Dialogs) perform exhaustive multi-field matching across:
  - **Barcode / SKU**
  - **Item Name**
  - **Location / Category / Store Section**
  - **Notes / Brand**
  - **Active Offer Badge & Offer Type** (e.g. searching *"3 for 2"*, *"2 for £5"*, *"Dairy"*, *"Tesco"*, or *"Angus"* instantly matches all relevant items and deals).
- 🏷️ **Dedicated Offers Manager Tab**: Create, edit, search, and delete store deals in a central **Offers** library.
  - **Full Offer CRUD**: Create custom deals (e.g., *"Tesco 3 for 2 Mix & Match"*, *"Sainsbury's 2 for £5 Grill Deal"*, *"50% Off Half Price"*).
  - **Apply to Product**: Search your items catalog directly from any offer card to assign the deal to a product in 1 tap.
  - **Product Offer History**: View past offers previously applied to a specific product and re-apply historical deals instantly.
- 📷 **Instant Barcode Scanning**: Powered by **Google Code Scanner API** to scan standard 1D/2D barcodes and QR codes instantly with zero runtime camera permissions needed. Includes manual barcode entry for emulators and damaged labels.
- 🏷️ **Store Special Offers & Multi-Buy Deal Engine**:
  - **Buy X Pay Y**: e.g. *3 for 2* (1 free) or *2 for 1* (BOGO).
  - **Multi-Buy Fixed Price**: e.g. *2 for £5.00* or *3 for £10.00*.
  - **Special Reduced Price**: e.g. *Was £4.00 $\rightarrow$ Reduced to £2.50*, with live savings calculation.
  - **Percentage Discount**: e.g. *20% Off* or *50% Off / Half Price*.
  - **1-Tap Saved Offers Picker**: Pick saved deals directly from your library during camera scan or manual item entry if shop OCR fails to detect the label.
- 🧾 **ML Kit OCR Camera Text & Offer Recognition**: On-device optical character recognition to extract product names and deal text directly from packaging and store labels.
- 🌐 **OpenFoodFacts Barcode Lookup**: Automatically fetches product names, brands, and categories from OpenFoodFacts for unrecognized grocery barcodes.
- 💰 **Cart Total & Target Budget Tracker**: Live shopping cart total with customizable budget limit alerts, visual progress bar, remaining budget calculations, and over-budget warnings.
- 📦 **My Items Catalog**: Manage inventory items (*Pieces*) with custom barcodes, quantities in cart, unit prices, storage categories/locations, and active offer badges.
- 🧩 **Bill of Materials (BOM) & Build Capacity**: Define assembled products built from multiple sub-components and compute maximum assembly build capacity from current stock.
- 🛒 **Shopping History & Receipts**: Finish and save completed shopping carts to a historical trip log with total cost, date, item count, and item summaries.
- 💱 **Multi-Currency & Custom Themes**: Configure preferred currency symbols (`RON`, `€`, `£`, `$`, or Custom) and toggle between Light, Dark, or System themes.
- 💾 **Data Backup, Restore & CSV Export/Import**: Full JSON backup and restore for all app data and settings, plus Storage Access Framework CSV export/import for items, products, BOM links, and trip history.

---

## 🛠️ Tech Stack & Architecture

Built following **MVVM** architecture and a clean **Repository** pattern.

| Component | Library / Framework | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern Kotlin with Coroutines & `Flow` |
| **UI Framework** | Jetpack Compose | Declarative Material 3 UI |
| **Database** | Room Database | SQLite ORM (v5 with `offers` and `product_offer_history`) |
| **DI** | Manual DI (`AppContainer`) | Lightweight, zero-reflection dependency injection |
| **Barcode Scanner** | Google Code Scanner | Play Services code scanner API |
| **Text Recognition** | Google ML Kit | On-device OCR for text and deal label parsing |
| **API Client** | OpenFoodFacts API | Automatic barcode web lookup for grocery items |
| **Preferences** | DataStore | Key-value reactive preferences for settings |

---

## 📁 Project Structure

```
shopping-tracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/inventory/barcodetracker/
│   │   │   ├── data/             # Room Entities (Piece, Product, Offer, ProductOfferHistory, BOM, Trip), DAOs, AppDatabase, Repository
│   │   │   ├── di/               # AppContainer manual DI
│   │   │   ├── scanner/          # Google Code Scanner Service
│   │   │   ├── service/          # CsvService, TextRecognitionService, OfferRecognitionService, OpenFoodFactsService, BackupRestoreService
│   │   │   └── ui/               # Jetpack Compose Screens (QuickScan, Items, Offers, Trips, Settings), ViewModels, Theme, Navigation
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/libs.versions.toml     # Version Catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Deployment & Running

### Build via Command Line
```bash
./gradlew assembleDebug
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
