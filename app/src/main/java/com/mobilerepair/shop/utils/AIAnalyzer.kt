package com.mobilerepair.shop.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.mobilerepair.shop.data.model.CommonFault
import com.mobilerepair.shop.data.model.RepairEntry
import kotlinx.coroutines.tasks.await

/**
 * AI-powered analysis utilities for the repair shop
 * Uses ML Kit and smart algorithms for analysis and predictions
 */
object AIAnalyzer {

    /**
     * Analyze phone photo to suggest possible faults using ML Kit
     */
    suspend fun suggestFaultsFromPhoto(bitmap: Bitmap?, knownFaults: List<CommonFault>): List<String> {
        val suggestions = mutableListOf<String>()
        if (bitmap == null) return suggestions

        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            // 1. Use Object Detection to find the device and check for structural issues
            val objOptions = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
            val objectDetector = ObjectDetection.getClient(objOptions)
            val detectedObjects = objectDetector.process(image).await()

            if (detectedObjects.isEmpty()) {
                suggestions.add("No device clearly detected. Please ensure the phone is well-lit and centered.")
            } else {
                suggestions.add("Device detected: Analysis in progress...")
            }

            // 2. Use Image Labeling for general context
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val labels: List<ImageLabel> = labeler.process(image).await()

            for (label in labels) {
                val text = label.text
                val confidence = label.confidence
                when {
                    text.contains("Screen", ignoreCase = true) || text.contains("Display", ignoreCase = true) -> {
                        if (confidence > 0.7) suggestions.add("High probability of Display/Screen related context.")
                    }
                    text.contains("Crack", ignoreCase = true) || text.contains("Broken", ignoreCase = true) -> {
                        suggestions.add("Visible structural damage (crack/break) detected!")
                    }
                    text.contains("Liquid", ignoreCase = true) || text.contains("Water", ignoreCase = true) -> {
                        suggestions.add("Possible liquid damage signs detected.")
                    }
                }
            }

            // 3. Keep some of the old pixel-based heuristics as fallbacks or complementary checks
            val pixelSuggestions = runPixelAnalysis(bitmap)
            suggestions.addAll(pixelSuggestions)

        } catch (e: Exception) {
            suggestions.add("AI Analysis failed: ${e.message}")
        }

        return suggestions.distinct()
    }

    private fun runPixelAnalysis(bitmap: Bitmap): List<String> {
        val suggestions = mutableListOf<String>()
        val width = bitmap.width
        val height = bitmap.height
        val samples = mutableListOf<Int>()

        for (x in 0 until width step (width / 10).coerceAtLeast(1)) {
            for (y in 0 until height step (height / 10).coerceAtLeast(1)) {
                samples.add(bitmap.getPixel(x, y))
            }
        }

        val avgBrightness = samples.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }.average()

        if (avgBrightness > 200) suggestions.add("Excessive brightness detected - could indicate backlight issues.")
        if (avgBrightness < 50) suggestions.add("Low brightness detected - check if device powers on.")

        return suggestions
    }

    /**
     * Estimate repair cost based on fault type
     */
    fun estimateRepairCost(
        fault: String,
        commonFaults: List<CommonFault>
    ): Double {
        val matchedFault = commonFaults.find {
            it.faultName.contains(fault, ignoreCase = true) ||
            fault.contains(it.faultName, ignoreCase = true)
        }
        return matchedFault?.defaultCharge ?: 0.0
    }

    /**
     * Generate repair time estimate in days
     */
    fun estimateRepairTime(fault: String): Int {
        return when {
            fault.contains("display", ignoreCase = true) ||
            fault.contains("screen", ignoreCase = true) -> 2
            fault.contains("battery", ignoreCase = true) -> 1
            fault.contains("charging", ignoreCase = true) ||
            fault.contains("pin", ignoreCase = true) -> 1
            fault.contains("motherboard", ignoreCase = true) ||
            fault.contains("board", ignoreCase = true) -> 5
            fault.contains("body", ignoreCase = true) ||
            fault.contains("housing", ignoreCase = true) -> 3
            else -> 2
        }
    }

    /**
     * Analyze repair history to identify trends
     */
    fun analyzeRepairTrends(entries: List<RepairEntry>): TrendsAnalysis {
        if (entries.isEmpty()) return TrendsAnalysis()

        val totalRepairs = entries.size
        val completedRepairs = entries.count { it.handoverDone }
        val totalRevenue = entries.filter { it.handoverDone }.sumOf { it.finalAmount }

        val faultCounts = entries
            .filter { it.faultDetected.isNotBlank() }
            .groupBy { it.faultDetected }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)

        val avgRepairTimeMs = entries
            .filter { it.handoverDone && it.handoverDate > it.createdAt }
            .map { it.handoverDate - it.createdAt }
            .average()
            .toLong()

        val avgRepairTimeDays = avgRepairTimeMs / (1000 * 60 * 60 * 24)

        return TrendsAnalysis(
            totalRepairs = totalRepairs,
            completedRepairs = completedRepairs,
            pendingRepairs = totalRepairs - completedRepairs,
            totalRevenue = totalRevenue,
            averageRevenue = if (completedRepairs > 0) totalRevenue / completedRepairs else 0.0,
            topFaults = faultCounts.map { it.key },
            topFaultCounts = faultCounts.map { it.value },
            averageRepairTimeDays = avgRepairTimeDays.toInt()
        )
    }

    /**
     * Predict when to reorder spare parts based on usage
     */
    fun predictReorder(
        partName: String,
        usageCount: Int,
        currentStock: Int,
        leadTimeDays: Int = 3
    ): ReorderPrediction {
        val weeklyUsage = usageCount.toDouble() / 4.0
        val daysUntilStockout = if (weeklyUsage > 0) {
            (currentStock / weeklyUsage * 7).toInt()
        } else Int.MAX_VALUE

        return ReorderPrediction(
            partName = partName,
            currentStock = currentStock,
            weeklyUsage = weeklyUsage.toInt(),
            daysUntilStockout = daysUntilStockout,
            shouldReorder = daysUntilStockout <= leadTimeDays,
            suggestedOrderQuantity = (weeklyUsage * leadTimeDays * 1.5).toInt()
        )
    }
}

data class TrendsAnalysis(
    val totalRepairs: Int = 0,
    val completedRepairs: Int = 0,
    val pendingRepairs: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRevenue: Double = 0.0,
    val topFaults: List<String> = emptyList(),
    val topFaultCounts: List<Int> = emptyList(),
    val averageRepairTimeDays: Int = 0
)

data class ReorderPrediction(
    val partName: String = "",
    val currentStock: Int = 0,
    val weeklyUsage: Int = 0,
    val daysUntilStockout: Int = Int.MAX_VALUE,
    val shouldReorder: Boolean = false,
    val suggestedOrderQuantity: Int = 0
)
