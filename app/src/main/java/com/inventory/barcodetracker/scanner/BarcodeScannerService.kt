package com.inventory.barcodetracker.scanner

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.tasks.await

sealed class ScanResult {
    data class Success(val barcode: String) : ScanResult()
    object Cancelled : ScanResult()
    data class Failure(val message: String, val canFallbackToManual: Boolean = true) : ScanResult()
}

class BarcodeScannerService(private val context: Context) {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
        )
        .enableAutoZoom()
        .build()

    private val scanner by lazy {
        GmsBarcodeScanning.getClient(context, options)
    }

    fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    suspend fun startScan(): ScanResult {
        return try {
            val barcode = scanner.startScan().await()
            val rawValue = barcode.rawValue
            if (!rawValue.isNullOrBlank()) {
                ScanResult.Success(rawValue)
            } else {
                ScanResult.Failure("No readable barcode detected.")
            }
        } catch (e: Exception) {
            val message = e.message ?: "Scanner operation failed or canceled"
            if (message.contains("cancel", ignoreCase = true)) {
                ScanResult.Cancelled
            } else {
                ScanResult.Failure(message, canFallbackToManual = true)
            }
        }
    }

    fun preloadScannerModule() {
        try {
            val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                .addApi(scanner)
                .build()
            ModuleInstall.getClient(context).installModules(moduleInstallRequest)
        } catch (_: Exception) {
            // Module install preload is best-effort
        }
    }
}
