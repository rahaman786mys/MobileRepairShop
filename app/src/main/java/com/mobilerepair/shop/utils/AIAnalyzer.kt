package com.mobilerepair.shop.utils

import android.graphics.Bitmap
import com.mobilerepair.shop.data.model.CommonFault
import com.mobilerepair.shop.data.model.RepairEntry

/**
 * AI-powered analysis utilities for the repair shop
 * Uses ML Kit and smart algorithms for analysis and predictions
 */
object AIAnalyzer {

    /**
     * Analyze phone photo to suggest possible faults
     */
    fun suggestFaultsFromPhoto(bitmap: Bitmap?, knownFaults: List<CommonFault>): List<String> {
        val suggestions = mutableListOf<String>()
        if (bitmap == null) return suggestions

        val width = bitmap.width
        val height = bitmap.height
        val samples = mutableListOf<Int>()

        for (x in 0 until width step (width / 10).coerceAtLeast(1)) {
            for (y in 0 until height step (height / 10).coerceAtLeast(1)) {
                samples.add(bitmap.getPixel(x, y))
            }
        }

        if (samples.isEmpty()) return suggestions

        // Calculate average brightness
        val avgBrightness = samples.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }.average()

        // Calculate contrast/variance
        val variance = samples.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            (gray - avgBrightness) * (gray - avgBrightness)
        }.average()

        // Smart fault suggestions
        if (avgBrightness > 200) {
            suggestions.add("Screen might be bright/unusual - Check display")
        }
        if (variance > 5000) {
            suggestions.add("High contrast detected - Possible screen damage/crack")
        }

        val darkPixelCount = samples.count { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (r + g + b) < 100
        }
        val darkRatio = if (samples.isNotEmpty()) darkPixelCount.toDouble() / samples.size else 0.0
        if (darkRatio > 0.3) {
            suggestions.add("Dark spots detected - Possible screen/display issue")
        }

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

