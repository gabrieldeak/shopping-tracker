package com.inventory.barcodetracker.service

import com.inventory.barcodetracker.data.local.entity.PieceEntity
import com.inventory.barcodetracker.data.local.entity.ProductEntity
import com.inventory.barcodetracker.data.local.entity.ProductPieceCrossRef
import com.inventory.barcodetracker.data.local.entity.ShoppingTripEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvService {

    // --- Helper CSV escaping and parsing ---
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // skip escaped quote
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    // --- Export Pieces ---
    fun exportPieces(outputStream: OutputStream, pieces: List<PieceEntity>) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write("barcode,name,description,location,stockQuantity,minStockThreshold,unitPrice,offerType,offerBuyQty,offerPayQty,offerFixedPrice,offerPercentOff,createdAt\n")
            for (p in pieces) {
                val row = listOf(
                    escapeCsv(p.barcode),
                    escapeCsv(p.name),
                    escapeCsv(p.description),
                    escapeCsv(p.location),
                    p.stockQuantity.toString(),
                    p.minStockThreshold.toString(),
                    p.unitPrice.toString(),
                    escapeCsv(p.offerType),
                    p.offerBuyQty.toString(),
                    p.offerPayQty.toString(),
                    p.offerFixedPrice.toString(),
                    p.offerPercentOff.toString(),
                    p.createdAt.toString()
                ).joinToString(",")
                writer.write("$row\n")
            }
            writer.flush()
        }
    }

    // --- Import Pieces ---
    fun importPieces(inputStream: InputStream): List<PieceEntity> {
        val pieces = mutableListOf<PieceEntity>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val header = parseCsvLine(headerLine).map { it.lowercase() }
            val barcodeIdx = header.indexOf("barcode")
            val nameIdx = header.indexOf("name")
            val descIdx = header.indexOf("description")
            val locIdx = header.indexOf("location")
            val stockIdx = header.indexOf("stockquantity")
            val minStockIdx = header.indexOf("minstockthreshold")
            val priceIdx = header.indexOf("unitprice")
            val offerTypeIdx = header.indexOf("offertype")
            val offerBuyQtyIdx = header.indexOf("offerbuyqty")
            val offerPayQtyIdx = header.indexOf("offerpayqty")
            val offerFixedPriceIdx = header.indexOf("offerfixedprice")
            val offerPercentOffIdx = header.indexOf("offerpercentoff")
            val createdIdx = header.indexOf("createdat")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                val cols = parseCsvLine(l)
                if (cols.size <= barcodeIdx || barcodeIdx < 0) continue

                val barcode = cols[barcodeIdx]
                if (barcode.isBlank()) continue
                val name = if (nameIdx in cols.indices) cols[nameIdx] else barcode
                val desc = if (descIdx in cols.indices) cols[descIdx] else ""
                val loc = if (locIdx in cols.indices && cols[locIdx].isNotBlank()) cols[locIdx] else "Grocery"
                val stock = if (stockIdx in cols.indices) cols[stockIdx].toIntOrNull() ?: 0 else 0
                val minStock = if (minStockIdx in cols.indices) cols[minStockIdx].toIntOrNull() ?: 0 else 0
                val unitPrice = if (priceIdx in cols.indices) cols[priceIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val offerType = if (offerTypeIdx in cols.indices && cols[offerTypeIdx].isNotBlank()) cols[offerTypeIdx] else "NONE"
                val offerBuyQty = if (offerBuyQtyIdx in cols.indices) cols[offerBuyQtyIdx].toIntOrNull() ?: 0 else 0
                val offerPayQty = if (offerPayQtyIdx in cols.indices) cols[offerPayQtyIdx].toIntOrNull() ?: 0 else 0
                val offerFixedPrice = if (offerFixedPriceIdx in cols.indices) cols[offerFixedPriceIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val offerPercentOff = if (offerPercentOffIdx in cols.indices) cols[offerPercentOffIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val created = if (createdIdx in cols.indices) cols[createdIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()

                pieces.add(
                    PieceEntity(
                        barcode = barcode,
                        name = name,
                        description = desc,
                        location = loc,
                        stockQuantity = stock,
                        minStockThreshold = minStock,
                        unitPrice = unitPrice,
                        offerType = offerType,
                        offerBuyQty = offerBuyQty,
                        offerPayQty = offerPayQty,
                        offerFixedPrice = offerFixedPrice,
                        offerPercentOff = offerPercentOff,
                        createdAt = created
                    )
                )
            }
        }
        return pieces
    }

    // --- Export Products ---
    fun exportProducts(outputStream: OutputStream, products: List<ProductEntity>) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write("productSku,name,description,stockQuantity,targetProductionCount,unitPrice,createdAt\n")
            for (p in products) {
                val row = listOf(
                    escapeCsv(p.productSku),
                    escapeCsv(p.name),
                    escapeCsv(p.description),
                    p.stockQuantity.toString(),
                    p.targetProductionCount.toString(),
                    p.unitPrice.toString(),
                    p.createdAt.toString()
                ).joinToString(",")
                writer.write("$row\n")
            }
            writer.flush()
        }
    }

    // --- Import Products ---
    fun importProducts(inputStream: InputStream): List<ProductEntity> {
        val products = mutableListOf<ProductEntity>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val header = parseCsvLine(headerLine).map { it.lowercase() }
            val skuIdx = header.indexOf("productsku")
            val nameIdx = header.indexOf("name")
            val descIdx = header.indexOf("description")
            val stockIdx = header.indexOf("stockquantity")
            val targetIdx = header.indexOf("targetproductioncount")
            val priceIdx = header.indexOf("unitprice")
            val createdIdx = header.indexOf("createdat")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                val cols = parseCsvLine(l)
                if (cols.size <= skuIdx || skuIdx < 0) continue

                val sku = cols[skuIdx]
                if (sku.isBlank()) continue
                val name = if (nameIdx in cols.indices) cols[nameIdx] else sku
                val desc = if (descIdx in cols.indices) cols[descIdx] else ""
                val stock = if (stockIdx in cols.indices) cols[stockIdx].toIntOrNull() ?: 0 else 0
                val target = if (targetIdx in cols.indices) cols[targetIdx].toIntOrNull() ?: 0 else 0
                val unitPrice = if (priceIdx in cols.indices) cols[priceIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val created = if (createdIdx in cols.indices) cols[createdIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()

                products.add(
                    ProductEntity(
                        productSku = sku,
                        name = name,
                        description = desc,
                        stockQuantity = stock,
                        targetProductionCount = target,
                        unitPrice = unitPrice,
                        createdAt = created
                    )
                )
            }
        }
        return products
    }

    // --- Export BOM ---
    fun exportBom(outputStream: OutputStream, crossRefs: List<ProductPieceCrossRef>) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write("productSku,barcode,quantityRequiredPerProduct\n")
            for (ref in crossRefs) {
                val row = listOf(
                    escapeCsv(ref.productSku),
                    escapeCsv(ref.barcode),
                    ref.quantityRequiredPerProduct.toString()
                ).joinToString(",")
                writer.write("$row\n")
            }
            writer.flush()
        }
    }

    // --- Import BOM ---
    fun importBom(inputStream: InputStream): List<ProductPieceCrossRef> {
        val refs = mutableListOf<ProductPieceCrossRef>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val header = parseCsvLine(headerLine).map { it.lowercase() }
            val skuIdx = header.indexOf("productsku")
            val barcodeIdx = header.indexOf("barcode")
            val qtyIdx = header.indexOf("quantityrequiredperproduct")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                val cols = parseCsvLine(l)
                if (skuIdx < 0 || barcodeIdx < 0 || cols.size <= maxOf(skuIdx, barcodeIdx)) continue

                val sku = cols[skuIdx]
                val barcode = cols[barcodeIdx]
                if (sku.isBlank() || barcode.isBlank()) continue
                val qty = if (qtyIdx in cols.indices) cols[qtyIdx].toIntOrNull() ?: 1 else 1

                refs.add(
                    ProductPieceCrossRef(
                        productSku = sku,
                        barcode = barcode,
                        quantityRequiredPerProduct = maxOf(1, qty)
                    )
                )
            }
        }
        return refs
    }

    // --- Export Shopping History ---
    fun exportTrips(outputStream: OutputStream, trips: List<ShoppingTripEntity>) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write("id,tripDate,totalCost,itemCount,itemsSummary,currencySymbol\n")
            for (t in trips) {
                val row = listOf(
                    t.id.toString(),
                    t.tripDate.toString(),
                    t.totalCost.toString(),
                    t.itemCount.toString(),
                    escapeCsv(t.itemsSummary),
                    escapeCsv(t.currencySymbol)
                ).joinToString(",")
                writer.write("$row\n")
            }
            writer.flush()
        }
    }

    // --- Import Shopping History ---
    fun importTrips(inputStream: InputStream): List<ShoppingTripEntity> {
        val trips = mutableListOf<ShoppingTripEntity>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val header = parseCsvLine(headerLine).map { it.lowercase() }
            val dateIdx = header.indexOf("tripdate")
            val costIdx = header.indexOf("totalcost")
            val countIdx = header.indexOf("itemcount")
            val summaryIdx = header.indexOf("itemssummary")
            val symbolIdx = header.indexOf("currencysymbol")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                val cols = parseCsvLine(l)

                val date = if (dateIdx in cols.indices) cols[dateIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                val cost = if (costIdx in cols.indices) cols[costIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val count = if (countIdx in cols.indices) cols[countIdx].toIntOrNull() ?: 0 else 0
                val summary = if (summaryIdx in cols.indices) cols[summaryIdx] else ""
                val symbol = if (symbolIdx in cols.indices && cols[symbolIdx].isNotBlank()) cols[symbolIdx] else "$"

                trips.add(
                    ShoppingTripEntity(
                        tripDate = date,
                        totalCost = cost,
                        itemCount = count,
                        itemsSummary = summary,
                        currencySymbol = symbol
                    )
                )
            }
        }
        return trips
    }
}
