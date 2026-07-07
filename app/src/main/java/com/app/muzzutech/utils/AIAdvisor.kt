package com.app.muzzutech.utils

import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.data.model.PartReturn

/**
 * Advanced Business Intelligence & AI Advisor
 * Analyzes profitability, trends, and provides "Smart Moves" for the shop owner
 */
object AIAdvisor {

    data class BusinessHealth(
        val dailyProfit: Long,
        val dailyRevenue: Long,
        val dailyExpense: Long,
        val profitMargin: Double,
        val healthScore: Int, // 0-100
        val smartMove: String,
        val recommendation: String
    )

    /**
     * Calculate Daily Profit/Loss with "Smart Move" AI Insight.
     *
     * @param repairs completed repair entries (used for revenue + to identify which parts were consumed today)
     * @param partsPurchased all spare-part purchases in range (will be filtered to today's handovers only)
     * @param expenses all expenses in range (will be filtered to paid+dated-today only)
     * @param directSales direct sales completed today (cash-basis revenue + cost)
     * @param partReturns part returns processed today (reduces cost via refund amount)
     */
    fun analyzeDailyHealth(
        repairs: List<RepairEntry>,
        partsPurchased: List<SparePartPurchase>,
        expenses: List<Expense> = emptyList(),
        directSales: List<Sale> = emptyList(),
        partReturns: List<PartReturn> = emptyList()
    ): BusinessHealth {
        val today = DateUtils.getStartOfDay()

        // Revenue: Final amounts from handovers completed today (accrual basis)
        val repairRevenue = repairs.filter { it.handoverDone && it.handoverDate >= today }
            .sumOf { it.finalAmount }
        // BUG #8: include direct sale revenue
        val directSaleRevenue = directSales.filter { it.saleDate >= today }
            .sumOf { it.salePrice }
        val revenue = repairRevenue + directSaleRevenue

        // BUG #5: only count parts actually consumed in today's handovers (not bulk restock)
        val todayHandoverEntryIds = repairs
            .filter { it.handoverDone && it.handoverDate >= today }
            .map { it.id }
            .toSet()
        val partCost = partsPurchased
            .filter { it.repairEntryId in todayHandoverEntryIds }
            .sumOf { it.purchasePrice * it.quantity }

        // BUG #6: only count PAID expenses dated today (cash-basis)
        val otherCost = expenses.filter { it.date >= today && it.paid }
            .sumOf { it.amount }

        // BUG #8: part returns reduce costs (refund cash received)
        val refundReceived = partReturns.filter { it.returnDate >= today }
            .sumOf { it.refundAmount }

        val totalExpenses = partCost + otherCost - refundReceived

        val profit = revenue - totalExpenses
        val margin = if (revenue > 0) (profit.toDouble() / revenue.toDouble()) * 100.0 else 0.0
        
        // AI Logic for Health Score & Smart Move
        val healthScore = when {
            margin > 40 -> 90
            margin > 20 -> 70
            margin > 0 -> 50
            else -> 30
        }

        val (move, rec) = when {
            totalExpenses > revenue -> Pair(
                "Expense Alert!",
                "You've spent more today than you've collected. Focus on completing high-margin repairs by evening."
            )
            margin > 50 -> Pair(
                "Premium Performance",
                "High profit day! Great job on labor-only repairs or high-margin display swaps."
            )
            repairs.count { !it.workDone } > 5 -> Pair(
                "Efficiency Move",
                "You have ${repairs.count { !it.workDone }} pending jobs. Clearing 3 more today would boost your revenue by approx ₹${(revenue/repairs.size.coerceAtLeast(1) * 3).toInt()}."
            )
            else -> Pair(
                "Steady Growth",
                "Balance is good. Consider upselling tempered glass or back covers to every customer to boost daily profit."
            )
        }

        return BusinessHealth(profit, revenue, totalExpenses, margin, healthScore, move, rec)
    }
}
