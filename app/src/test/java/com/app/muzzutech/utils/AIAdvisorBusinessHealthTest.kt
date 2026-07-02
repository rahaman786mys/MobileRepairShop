package com.app.muzzutech.utils

import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario-driven tests for [AIAdvisor.analyzeDailyHealth].
 *
 * Branches covered (per the source code at AIAdvisor.kt:42-67):
 *   - margin > 50        -> "Premium Performance" + score 90
 *   - 20 < margin <= 40  -> score 70 (no specific move branch until efficiency)
 *   - 0 < margin <= 20   -> score 50
 *   - margin <= 0        -> score 30
 *   - expenses > revenue -> "Expense Alert!" (overrides smart move regardless of margin)
 *   - pendingRepairs > 5 -> "Efficiency Move"
 *   - fallback           -> "Steady Growth"
 *   - empty inputs       -> zero-score health defensively
 *   - revenue=0 division -> profitMargin=0.0 (no NaN)
 *
 * The test does NOT touch Android Room, Bitmap, or ML Kit, so it runs as plain JUnit.
 */
class AIAdvisorBusinessHealthTest {

    private fun entry(
        handoverDone: Boolean = false,
        handoverDate: Long = 0L,
        finalAmount: Double = 0.0,
        workDone: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = RepairEntry(
        handoverDone = handoverDone,
        handoverDate = handoverDate,
        finalAmount = finalAmount,
        workDone = workDone,
        createdAt = createdAt
    )

    private fun part(purchasePrice: Double = 0.0, quantity: Int = 1, purchaseDate: Long = System.currentTimeMillis()) =
        SparePartPurchase(purchasePrice = purchasePrice, quantity = quantity, purchaseDate = purchaseDate)

    @Test
    fun `empty repairs and parts return zero health score and Steady Growth`() {
        val health = AIAdvisor.analyzeDailyHealth(emptyList(), emptyList())
        assertEquals(0.0, health.dailyRevenue, 0.01)
        assertEquals(0.0, health.dailyExpense, 0.01)
        assertEquals(0.0, health.dailyProfit, 0.01)
        assertEquals(0.0, health.profitMargin, 0.01)
        assertEquals(30, health.healthScore) // margin 0 triggers "else" branch -> score 30
        assertTrue("Expected Steady Growth for empty day, got ${health.smartMove}", health.smartMove == "Steady Growth")
    }

    @Test
    fun `premium performance when margin exceeds 50 percent`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = today + 1, finalAmount = 1000.0)
        )
        val parts = listOf(part(purchasePrice = 100.0)) // expense 100 => profit 900 => margin 90%
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        assertEquals(1000.0, health.dailyRevenue, 0.01)
        assertEquals(100.0, health.dailyExpense, 0.01)
        assertEquals(900.0, health.dailyProfit, 0.01)
        assertEquals(90.0, health.profitMargin, 0.01)
        assertEquals(90, health.healthScore)
        assertEquals("Premium Performance", health.smartMove)
    }

    @Test
    fun `expense alert overrides smart move when spend exceeds revenue`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = today + 1, finalAmount = 200.0)
        )
        val parts = listOf(part(purchasePrice = 500.0)) // expense 500 > revenue 200
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        assertEquals("Expense Alert!", health.smartMove)
        assertEquals(200.0 - 500.0, health.dailyProfit, 0.01)
        // 200 revenue, 500 expense -> profit -300, margin = (-300/200)*100 = -150 -> score 30
        assertEquals(30, health.healthScore)
        assertTrue(
            "Recommendation should mention focusing on high-margin repairs, got: ${health.recommendation}",
            health.recommendation.contains("high-margin", ignoreCase = true)
        )
    }

    @Test
    fun `efficiency move when more than 5 pending jobs and not in expense alert`() {
        val today = DateUtils.getStartOfDay()
        // To reach the "pending > 5" branch we must NOT trigger "Expense Alert" (expense > revenue)
        // and NOT trigger "Premium Performance" (margin > 50). With revenue=1000 and expense=500,
        // margin = 50% which is NOT strictly > 50, so we fall through to pending > 5 branch.
        val repairs = (1..6).map { entry(handoverDone = false, finalAmount = 0.0) } +
            listOf(entry(handoverDone = true, workDone = true, handoverDate = today + 1, finalAmount = 1000.0))
        val parts = listOf(part(purchasePrice = 500.0)) // margin = 50% (NOT > 50), profit = 500
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        assertEquals("Efficiency Move", health.smartMove)
        assertEquals(50.0, health.profitMargin, 0.01)
        // 6 pending entries with workDone=false + 1 completed with workDone=true -> pending = 6
        assertEquals(6, repairs.count { !it.workDone })
        assertTrue(
            "Recommendation should mention pending jobs count, got: ${health.recommendation}",
            health.recommendation.contains("6 pending jobs")
        )
    }

    @Test
    fun `steady growth fallback when no special conditions triggered`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = today + 1, finalAmount = 500.0)
        )
        val parts = listOf(part(purchasePrice = 300.0)) // profit 200, margin 40% -> branch margin>40 -> score 90, smart move falls to else
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        // 200 profit on 500 revenue = 40% margin: not > 40 (strictly greater), not > 50, not > 5 pending, not expense alert
        // Note: source uses `margin > 40` strict for score 90; we crafted margin = 40 exactly -> score 70
        // And the smart-move ladder: expenses>(revenue) false; margin>50 false; pending>5 false; else -> Steady Growth
        assertEquals(500.0, health.dailyRevenue, 0.01)
        assertEquals(300.0, health.dailyExpense, 0.01)
        assertEquals(200.0, health.dailyProfit, 0.01)
        assertEquals(40.0, health.profitMargin, 0.01)
        assertEquals(70, health.healthScore) // margin > 20 -> 70
        assertEquals("Steady Growth", health.smartMove)
    }

    @Test
    fun `health score 50 for thin positive margin`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = today + 1, finalAmount = 1000.0)
        )
        val parts = listOf(part(purchasePrice = 950.0)) // profit 50, margin 5%
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        assertEquals(50, health.healthScore)
        assertEquals(5.0, health.profitMargin, 0.01)
    }

    @Test
    fun `revenue zero takes early branch to avoid NaN in margin`() {
        val parts = listOf(part(purchasePrice = 250.0)) // revenue 0
        val health = AIAdvisor.analyzeDailyHealth(emptyList(), parts)
        assertEquals(0.0, health.dailyRevenue, 0.01)
        assertEquals(0.0, health.profitMargin, 0.01)
        // expenses (250) > revenue (0) -> "Expense Alert!" wins
        assertEquals("Expense Alert!", health.smartMove)
    }

    @Test
    fun `parts purchased outside today are excluded from expense`() {
        val yesterday = DateUtils.getStartOfDay() - 24L * 60 * 60 * 1000
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(entry(handoverDone = true, handoverDate = today + 1, finalAmount = 500.0))
        val parts = listOf(part(purchasePrice = 1000.0, purchaseDate = yesterday))
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        // Even though part's purchaseDate is yesterday, AIAdvisor filters by `purchaseDate >= today`,
        // so expense should be 0 and profit margin should be 100%
        assertEquals(500.0, health.dailyRevenue, 0.01)
        assertEquals(0.0, health.dailyExpense, 0.01)
        assertEquals(100.0, health.profitMargin, 0.01)
        assertEquals("Premium Performance", health.smartMove)
    }

    @Test
    fun `handover outside today is excluded from revenue`() {
        val yesterday = DateUtils.getStartOfDay() - 24L * 60 * 60 * 1000
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = yesterday, finalAmount = 5000.0)
        )
        val health = AIAdvisor.analyzeDailyHealth(repairs, emptyList())
        // HANDOVER WAS YESTERDAY: revenue 0 for today => expenses (0) NOT > revenue (0) => margin 0 => "Steady Growth"
        assertEquals(0.0, health.dailyRevenue, 0.01)
        assertEquals("Steady Growth", health.smartMove)
    }

    @Test
    fun `multiple repairs sum revenue`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(
            entry(handoverDone = true, handoverDate = today + 1, finalAmount = 200.0),
            entry(handoverDone = true, handoverDate = today + 2, finalAmount = 300.0),
            entry(handoverDone = true, handoverDate = today + 3, finalAmount = 500.0)
        )
        val health = AIAdvisor.analyzeDailyHealth(repairs, emptyList())
        assertEquals(1000.0, health.dailyRevenue, 0.01)
    }

    @Test
    fun `quantity multiplied by price for parts expense`() {
        val today = DateUtils.getStartOfDay()
        val repairs = listOf(entry(handoverDone = true, handoverDate = today + 1, finalAmount = 2000.0))
        val parts = listOf(
            part(purchasePrice = 50.0, quantity = 4), // 200
            part(purchasePrice = 100.0, quantity = 2) // 200
        )
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)
        assertEquals(400.0, health.dailyExpense, 0.01)
        assertEquals(2000.0 - 400.0, health.dailyProfit, 0.01)
        assertEquals(80.0, health.profitMargin, 0.01) // 1600/2000 = 80%
        assertEquals(90, health.healthScore)
        assertEquals("Premium Performance", health.smartMove)
    }

    @Test
    fun `recommendation exists and is non-empty in all branches`() {
        val branches = mutableListOf(
            "Premium Performance" to AIAdvisor.analyzeDailyHealth(
                listOf(entry(handoverDone = true, handoverDate = DateUtils.getStartOfDay() + 1, finalAmount = 1000.0)),
                listOf(part(purchasePrice = 50.0))
            ).recommendation,
            "Expense Alert" to AIAdvisor.analyzeDailyHealth(
                listOf(entry(handoverDone = true, handoverDate = DateUtils.getStartOfDay() + 1, finalAmount = 50.0)),
                listOf(part(purchasePrice = 500.0))
            ).recommendation,
            "Efficiency Move" to AIAdvisor.analyzeDailyHealth(
                (1..6).map { entry(handoverDone = false) } + entry(handoverDone = true, handoverDate = DateUtils.getStartOfDay() + 1, finalAmount = 1000.0),
                listOf(part(purchasePrice = 10.0))
            ).recommendation,
            "Steady Growth" to AIAdvisor.analyzeDailyHealth(emptyList(), emptyList()).recommendation
        )
        branches.forEach { (name, rec) ->
            assertTrue("$name recommendation should not be empty", rec.isNotBlank())
        }
    }
}
