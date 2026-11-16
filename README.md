# Shopping & Inventory Tracker (Android)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A local-first, offline-ready Android application built with **Kotlin** and **Jetpack Compose**. Developed & Designed by **G.A.D** and ✨ *Vibe coded with AI*. Designed for fast in-store shopping, barcode scanning, budget tracking, dedicated multi-buy offer management, comprehensive field searching, OCR label recognition, shopping history receipts, and product bundle assembly.

---

## ✨ Key Features (Version 2.0.0)

- ⚙️ **About & Developer Info**:
  - **Developed & Designed by G.A.D**
  - **✨ Vibe Coded with AI**
  - **Version 2.0.0** (Dynamic versioning in Settings and User-Agent headers)
- 📋 **Multiple Shopping Basket Feed**: Main screen presents a unified, scrollable feed of expandable **Shopping Basket Cards** (each card contains list name, live subtotal, item count, budget limit progress, vouchers, clear, finish & save, duplicate, rename, and delete).
- ☑️ **Multi-Select Scanning Targets**: Check one or more basket cards to automatically route scanned barcodes or searched items into all checked lists simultaneously!
- 🗑️ **Multi-Select Batch Delete across All Screens**:
  - **Quick Scan**: Checkboxes on basket cards + **Delete Selected Baskets** button.
  - **My Items Catalog**: Checkboxes on item cards + **Delete Selected Items** button + **Select All**.
  - **Offers Manager**: Checkboxes on offer cards + **Delete Selected Offers** button + **Select All**.
  - **Shopping History**: Checkboxes on receipt cards + **Delete Selected Receipts** button + **Select All**.
- 🔠 **Smart Text Capitalization Settings**: Configure auto-capitalization mode in Settings:
  - **Capitalize Every Word (Title Case)** e.g. *"Fresh Apples"*, *"Tesco Weekly"*.
  - **Capitalize First Letter Only** e.g. *"Fresh apples"*, *"Tesco weekly"*.
  - **As Typed (Manual)**.
  - Applied automatically across basket names, catalog items, and store offer titles!
- 🏷️ **Optional Barcodes**: Register items without barcodes (auto-generates clean internal IDs for loose produce/bakery items) or with optional prices.
- 🔄 **1-Tap Receipt Reload**: Reload any historical receipt from **Shopping History** directly back into an active shopping basket with 1 tap.
- 🚀 **GitHub Release Auto-Update Check**: Asynchronously checks GitHub Releases API on app startup and prompts when a newer version is available.
- 🖐️ **Horizontal Swipe Navigation & Card Gestures**:
  - **Horizontal Page Swiping**: Swipe left/right anywhere on the screen to switch smoothly between the 5 main app tabs (*Quick Scan*, *My Items*, *Offers*, *History*, *Settings*).
  - **Card Swipe Gestures**: Swipe any shopping basket card Right to **Duplicate** or Left to **Delete**.
- 📐 **Keyboard-Friendly IME Inset Scrolling & Scrollable Dialogs**: Action buttons embedded inside scrollable dialog columns with dynamic bottom scroll padding so all inputs and buttons remain 100% visible above the soft keyboard.
- 💾 **Safe Database Migration & Backups**: Room Database Migration `6 -> 7` with zero data loss, supported by complete JSON and standalone `.db` file backup/restore.

---

## 🛠️ Tech Stack & Architecture

Built following **MVVM** architecture and a clean **Repository** pattern.

| Component | Library / Framework | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern Kotlin with Coroutines & `Flow` |
| **UI Framework** | Jetpack Compose | Declarative Material 3 UI with `HorizontalPager` |
| **Database** | Room Database | SQLite ORM (v7 with `shopping_lists` budgetLimit) |
| **DI** | Manual DI (`AppContainer`) | Lightweight, zero-reflection dependency injection |
| **Barcode Scanner** | Google Code Scanner | Play Services code scanner API |
| **Text Recognition** | Google ML Kit | On-device OCR for text and deal label parsing |
| **API Client** | OpenFoodFacts API | Automatic barcode web lookup for grocery items |
| **Preferences** | DataStore / SharedPreferences | Reactive preferences for currency, budget, theme, and capitalization |

---

## 🚀 Deployment & Running

### Build via Command Line
```bash
./gradlew assembleDebug
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
