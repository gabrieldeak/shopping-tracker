package com.inventory.barcodetracker.service

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object TextRecognitionService {

    suspend fun processImage(bitmap: Bitmap): List<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()

            val lines = visionText.textBlocks.flatMap { block ->
                block.lines.map { it.text.trim() }
            }.filter { it.length >= 2 }

            lines.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
