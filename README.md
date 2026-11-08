# Shopping & Inventory Tracker (Android)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A local-first, offline-ready Android application built with **Kotlin** and **Jetpack Compose**. Designed for fast inventory management, barcode scanning, price tracking, receipt/offer OCR recognition, shopping trip history, and Bill of Materials (BOM) assembly tracking.

---

## ✨ Features

- 📷 **Instant Barcode Scanning**: Uses **Google Code Scanner API** to read standard 1D/2D barcodes and QR codes quickly without requiring runtime camera permissions.
- 🧾 **OCR Receipt & Offer Recognition**: Powered by **ML Kit Text Recognition** to extract items and prices directly from receipts, store tags, and promotional offers.
- 📦 **Inventory Catalog**: Manage individual items (*Pieces*) with custom barcodes, quantities, minimum stock thresholds, storage bin locations, and pricing.
- 🧩 **Bill of Materials (BOM)**: Define composite products built from multiple sub-pieces, with auto-calculated assembly costs and stock requirements.
- 🛒 **Shopping Trips & Price History**: Track store visits, log purchased items, view price changes over time, and analyze total trip expenditures.
- 💱 **Multi-Currency & Custom Themes**: Configure preferred currency symbols (`$`, `€`, `£`, `¥`, etc.) and seamlessly toggle between Light, Dark, or System themes.
- 💾 **Data Backup, Restore & CSV Export**: Export inventory to CSV files via Android Storage Access Framework and backup/restore local databases anytime.

---

## 🛠️ Architecture & Tech Stack

The application strictly follows modern Android development best practices with **MVVM** architecture and a clean **Repository** layer.

| Component | Library / Framework | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern functional and object-oriented Kotlin |
| **UI Framework** | Jetpack Compose | Declarative Material 3 user interface |
| **State & Async** | Coroutines & Flow | `StateFlow` / `SharedFlow` with `collectAsStateWithLifecycle` |
| **Database** | Room Database | SQLite ORM with KSP code generation |
| **Dependency Injection** | Manual DI (`AppContainer`) | Clean, lightweight application dependency container |
| **Barcode Scanning** | Google Code Scanner | Play Services code scanner with minimal footprint |
| **Text Recognition** | Google ML Kit | On-device OCR text recognition |
| **Storage & DataStore** | Preferences DataStore | Reactive key-value storage for settings |

---

## 📁 Project Structure

```
shopping-tracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/inventory/barcodetracker/
│   │   │   ├── data/             # Room Entities, DAOs, Database, and Repositories
│   │   │   ├── di/               # Manual Dependency Injection Container
│   │   │   ├── scanner/          # Google Code Scanner Service wrapper
│   │   │   ├── service/          # CSV Import/Export, OCR Text Recognition, Backup/Restore
│   │   │   └── ui/               # Jetpack Compose Screens, ViewModels, Theme, Navigation
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/libs.versions.toml     # Centralized Version Catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2) or newer recommended.
- **JDK**: Java 17.
- **Min SDK**: API 26 (Android 8.0 Oreo).
- **Target SDK**: API 35/37.

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/<YOUR-USERNAME>/shopping-tracker.git
   cd shopping-tracker
   ```

2. **Open in Android Studio**:
   Open the project folder directly in Android Studio. Allow Gradle to sync dependencies.

3. **Build via Command Line**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device / Emulator**:
   Select your connected Android device or emulator in Android Studio and hit **Run (Shift + F10)**.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
