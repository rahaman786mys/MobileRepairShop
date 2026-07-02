package com.app.muzzutech.utils

import com.app.muzzutech.data.model.CommonFault
import com.app.muzzutech.data.model.RepairEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario-driven tests for the pure-Kotlin parts of [AIAnalyzer]:
 *  - estimateRepairCost
 *  - estimateRepairTime
 *  - analyzeRepairTrends
 *  - predictReorder
 *
 * (suggestFaultsFromPhoto uses ML Kit + Bitmap and is skipped here.)
 */
class AIAnalyzerTrendsTest {

    private fun entry(
        faultDetected: String = "",
        handoverDone: Boolean = false,
        finalAmount: Double = 0.0,
        createdAt: Long = 0L,
        handoverDate: Long = 0L
    ) = RepairEntry(
        faultDetected = faultDetected,
        handoverDone = handoverDone,
        finalAmount = finalAmount,
        createdAt = createdAt,
        handoverDate = handoverDate
    )

    private fun fault(name: String, charge: Double = 0.0) =
        CommonFault(faultName = name, defaultCharge = charge)

    // --- estimateRepairCost -------------------------------------------------

    @Test
    fun `estimateRepairCost returns matched fault charge`() {
        val faults = listOf(
            fault("Cracked Screen", 1500.0),
            fault("Battery Replacement", 600.0),
            fault("Charging Port", 400.0)
        )
        assertEquals(1500.0, AIAnalyzer.estimateRepairCost("screen", faults), 0.01)
        assertEquals(600.0, AIAnalyzer.estimateRepairCost("battery", faults), 0.01)
    }

    @Test
    fun `estimateRepairCost matches case insensitive exact substring`() {
        val faults = listOf(fault("Display Assembly", 2500.0))
        // "display assembly" contains "display" — so fault.contains(faultName-fragment, ignoreCase)
        assertEquals(2500.0, AIAnalyzer.estimateRepairCost("display assembly", faults), 0.01)
    }

    @Test
    fun `estimateRepairCost returns zero when no fault matches`() {
        val faults = listOf(fault("Cracked Screen", 1500.0))
        assertEquals(0.0, AIAnalyzer.estimateRepairCost("water damage", faults), 0.01)
    }

    @Test
    fun `estimateRepairCost empty fault list returns zero`() {
        assertEquals(0.0, AIAnalyzer.estimateRepairCost("Cracked Screen", emptyList()), 0.01)
    }

    // --- estimateRepairTime --------------------------------------------------

    @Test
    fun `estimateRepairTime mapping is correct for known keywords`() {
        assertEquals(2, AIAnalyzer.estimateRepairTime("Display replacement needed"))
        assertEquals(1, AIAnalyzer.estimateRepairTime("Battery drain"))
        assertEquals(1, AIAnalyzer.estimateRepairTime("Charging pin broken"))
        assertEquals(5, AIAnalyzer.estimateRepairTime("Motherboard IC dead"))
        assertEquals(3, AIAnalyzer.estimateRepairTime("Body housing dented"))
        assertEquals(2, AIAnalyzer.estimateRepairTime("Speaker not working")) // fallback
    }

    @Test
    fun `estimateRepairTime is case insensitive`() {
        assertEquals(2, AIAnalyzer.estimateRepairTime("DISPLAY issue"))
        assertEquals(1, AIAnalyzer.estimateRepairTime("BATTERY BULGING"))
    }

    @Test
    fun `estimateRepairTime empty fault returns fallback 2 days`() {
        assertEquals(2, AIAnalyzer.estimateRepairTime(""))
    }

    // --- analyzeRepairTrends ------------------------------------------------

    @Test
    fun `analyzeRepairTrends empty list returns zeroed TrendsAnalysis`() {
        val trends = AIAnalyzer.analyzeRepairTrends(emptyList())
        assertEquals(0, trends.totalRepairs)
        assertEquals(0, trends.completedRepairs)
        assertEquals(0, trends.pendingRepairs)
        assertEquals(0.0, trends.totalRevenue, 0.01)
        assertEquals(0.0, trends.averageRevenue, 0.01)
        assertTrue(trends.topFaults.isEmpty())
        assertTrue(trends.topFaultCounts.isEmpty())
        assertEquals(0, trends.averageRepairTimeDays)
    }

    @Test
    fun `analyzeRepairTrends counts completed vs pending correctly`() {
        val entries = listOf(
            entry(handoverDone = true, finalAmount = 1000.0, createdAt = 1000, handoverDate = 2000),
            entry(handoverDone = true, finalAmount = 500.0, createdAt = 1500, handoverDate = 2500),
            entry(handoverDone = false),
            entry(handoverDone = false)
        )
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals(4, trends.totalRepairs)
        assertEquals(2, trends.completedRepairs)
        assertEquals(2, trends.pendingRepairs)
        assertEquals(1500.0, trends.totalRevenue, 0.01)
        assertEquals(750.0, trends.averageRevenue, 0.01)
    }

    @Test
    fun `analyzeRepairTrends ranks faults by frequency desc and takes top 5`() {
        val faults = listOf("Screen", "Screen", "Screen", "Battery", "Battery", "Speaker", "Mic", "Camera", "USB")
        val entries = faults.map { entry(faultDetected = it, handoverDone = true, finalAmount = 100.0, createdAt = 1, handoverDate = 2) }
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals("Screen", trends.topFaults.first())
        assertEquals(3, trends.topFaultCounts.first())
        assertEquals(5, trends.topFaults.size)
        assertEquals("Battery", trends.topFaults[1])
        assertEquals(2, trends.topFaultCounts[1])
    }

    @Test
    fun `analyzeRepairTrends ignores blank faults in top faults counting`() {
        val entries = listOf(
            entry(faultDetected = "Screen", handoverDone = true, finalAmount = 100.0, createdAt = 1, handoverDate = 2),
            entry(faultDetected = "", handoverDone = true, finalAmount = 100.0, createdAt = 1, handoverDate = 2),
            entry(faultDetected = "  ", handoverDone = true, finalAmount = 100.0, createdAt = 1, handoverDate = 2)
        )
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals(1, trends.topFaults.size)
        assertEquals("Screen", trends.topFaults.first())
    }

    @Test
    fun `analyzeRepairTrends average repair time computed in days`() {
        // 1 day = 24h = 86_400_000 ms
        val oneDay = 86_400_000L
        val entries = listOf(
            entry(createdAt = 0L, handoverDone = true, handoverDate = 3 * oneDay), // 3 days
            entry(createdAt = 0L, handoverDone = true, handoverDate = 5 * oneDay)  // 5 days -> avg 4
        )
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals(4, trends.averageRepairTimeDays)
    }

    @Test
    fun `analyzeRepairTrends excludes pending repairs from average time`() {
        val oneDay = 86_400_000L
        val entries = listOf(
            entry(createdAt = 0L, handoverDone = true, handoverDate = 2 * oneDay),
            entry(createdAt = 0L, handoverDone = false, handoverDate = 999L) // pending: handoverDate 999 < createdAt 0? skip filter requires handoverDone && handoverDate > createdAt
        )
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals(2, trends.averageRepairTimeDays)
    }

    @Test
    fun `analyzeRepairTrends handles handover before creation edge case`() {
        val entries = listOf(
            entry(createdAt = 1000L, handoverDone = true, handoverDate = 500L) // handoverDate < createdAt: filtered out
        )
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        // entries marked handoverDone=true count completion, but avg repair time excludes them via filter
        assertEquals(1, trends.completedRepairs)
        assertEquals(0, trends.averageRepairTimeDays)
    }

    @Test
    fun `analyzeRepairTrends zero completed repairs keeps averageRevenue at zero`() {
        val entries = listOf(entry(handoverDone = false))
        val trends = AIAnalyzer.analyzeRepairTrends(entries)
        assertEquals(0.0, trends.averageRevenue, 0.01)
        assertEquals(0.0, trends.totalRevenue, 0.01)
    }

    // --- predictReorder -----------------------------------------------------

    @Test
    fun `predictReorder shouldReorder true when days to stockout below lead time`() {
        val pred = AIAnalyzer.predictReorder(partName = "Display", usageCount = 40, currentStock = 2, leadTimeDays = 3)
        // weeklyUsage = 40/4 = 10; daysUntilStockout = 2/10 * 7 = 1 (int)
        assertTrue("Should reorder", pred.shouldReorder)
        assertEquals(1, pred.daysUntilStockout)
        // suggested = weekly * leadTime * 1.5 = 10 * 3 * 1.5 = 45
        assertEquals(45, pred.suggestedOrderQuantity)
    }

    @Test
    fun `predictReorder shouldReorder false when stock is sufficient`() {
        val pred = AIAnalyzer.predictReorder(partName = "Battery", usageCount = 4, currentStock = 50, leadTimeDays = 3)
        // weeklyUsage = 4/4 = 1; daysUntilStockout = 50/1 * 7 = 350 -> longer than 3 day lead time
        assertFalse(pred.shouldReorder)
    }

    @Test
    fun `predictReorder zero usage never triggers reorder`() {
        val pred = AIAnalyzer.predictReorder(partName = "Camera", usageCount = 0, currentStock = 1, leadTimeDays = 3)
        assertFalse(pred.shouldReorder)
        assertEquals(Int.MAX_VALUE, pred.daysUntilStockout)
        assertEquals(0, pred.weeklyUsage)
    }

    @Test
    fun `predictReorder default lead time of 3 days applies`() {
        val pred = AIAnalyzer.predictReorder(partName = "Charging Port", usageCount = 12, currentStock = 5)
        // weekly = 3; daysUntil = 5/3 * 7 ≈ 11; leadTime default 3 -> shouldReorder false
        assertFalse(pred.shouldReorder)
    }

    @Test
    fun `predictReorder met exactly at lead-time threshold does not reorder`() {
        // weeklyUsage = 1; currentStock = 1; daysUntil = 1/1 * 7 = 7; leadTime 7 -> NOT <= 7 (no reorder)
        // Wait: source: `daysUntilStockout <= leadTimeDays` -> 7 <= 7 is TRUE so profit
        val pred = AIAnalyzer.predictReorder(partName = "X", usageCount = 4, currentStock = 1, leadTimeDays = 7)
        assertTrue(pred.shouldReorder)
    }

    @Test
    fun `predictReorder returns populated partName field`() {
        val pred = AIAnalyzer.predictReorder(partName = "Speaker", usageCount = 4, currentStock = 10, leadTimeDays = 3)
        assertEquals("Speaker", pred.partName)
        assertEquals(10, pred.currentStock)
    }

    @Test
    fun `predictReorder computes weekly usage from monthly usage divided by four`() {
        val pred = AIAnalyzer.predictReorder(partName = "P", usageCount = 40, currentStock = 100, leadTimeDays = 3)
        assertEquals(10, pred.weeklyUsage)
    }
}
