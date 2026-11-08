package com.inventory.barcodetracker.service

import java.util.Locale
import java.util.regex.Pattern

data class DetectedOffer(
    val offerType: String,
    val buyQty: Int = 0,
    val payQty: Int = 0,
    val fixedPrice: Double = 0.0,
    val percentOff: Double = 0.0,
    val detectedText: String = "",
)

object OfferRecognitionService {

    fun parseOfferFromText(lines: List<String>): DetectedOffer? {
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT).trim()

            // 1. Check "3 for 2" / "Buy 3 for 2" / "Buy 3 pay for 2"
            val threeForTwoMatcher = Pattern.compile("(?:BUY\\s*)?(\\d+)\\s*FOR\\s*(\\d+)").matcher(upper)
            if (threeForTwoMatcher.find()) {
                val buy = threeForTwoMatcher.group(1)?.toIntOrNull() ?: 3
                val pay = threeForTwoMatcher.group(2)?.toIntOrNull() ?: 2
                if (buy > pay) {
                    return DetectedOffer(
                        offerType = "THREE_FOR_TWO",
                        buyQty = buy,
                        payQty = pay,
                        detectedText = line,
                    )
                }
            }

            // 2. Check "Buy 1 Get 1 Free" / "1+1 Free" / "BOGO"
            if (upper.contains("BUY 1 GET 1 FREE") || upper.contains("1 GET 1 FREE") || upper.contains("BOGO")) {
                return DetectedOffer(
                    offerType = "THREE_FOR_TWO",
                    buyQty = 2,
                    payQty = 1,
                    detectedText = line,
                )
            }

            // 3. Check "2 for £5.00" / "2 for $5" / "3 for 10"
            val multiBuyFixedMatcher = Pattern.compile("(\\d+)\\s*FOR\\s*[£$€]?\\s*(\\d+(?:\\.\\d{1,2})?)").matcher(upper)
            if (multiBuyFixedMatcher.find()) {
                val buy = multiBuyFixedMatcher.group(1)?.toIntOrNull() ?: 2
                val price = multiBuyFixedMatcher.group(2)?.toDoubleOrNull() ?: 0.0
                if (buy > 0 && price > 0.0) {
                    return DetectedOffer(
                        offerType = "MULTI_BUY_FIXED",
                        buyQty = buy,
                        fixedPrice = price,
                        detectedText = line,
                    )
                }
            }

            // 4. Check "Half Price"
            if (upper.contains("HALF PRICE") || upper.contains("50% OFF") || upper.contains("50% DISCOUNT")) {
                return DetectedOffer(
                    offerType = "PERCENT_OFF",
                    percentOff = 50.0,
                    detectedText = line,
                )
            }

            // 5. Check "% Off" (e.g. "20% OFF", "25% DISCOUNT")
            val percentMatcher = Pattern.compile("(\\d{1,2})%\\s*(?:OFF|DISCOUNT)").matcher(upper)
            if (percentMatcher.find()) {
                val percent = percentMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                if (percent > 0.0) {
                    return DetectedOffer(
                        offerType = "PERCENT_OFF",
                        percentOff = percent,
                        detectedText = line,
                    )
                }
            }
        }
        return null
    }
}
