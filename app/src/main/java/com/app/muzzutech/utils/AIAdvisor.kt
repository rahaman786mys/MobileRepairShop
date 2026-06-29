package com.app.muzzutech.utils

import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase

/**
 * Advanced Business Intelligence & AI Advisor
 * Analyzes profitability, trends, and provides "Smart Moves" for the shop owner
 */
object AIAdvisor {

    data class BusinessHealth(
        val dailyProfit: Double,
        val dailyRevenue: Double,
        val dailyExpense: Double,
        val profitMargin: Double,
        val healthScore: Int, // 0-100
        val smartMove: String,
        val recommendation: String
    )

    /**
     * Calculate Daily Profit/Loss with "Smart Move" AI Insight
     */
    fun analyzeDailyHealth(
        repairs: List<RepairEntry>,
        partsPurchased: List<SparePartPurchase>
    ): BusinessHealth {
        val today = DateUtils.getStartOfDay()
        
        // Revenue: Final amounts from handovers completed today
        val revenue = repairs.filter { it.handoverDone && it.handoverDate >= today }
            .sumOf { it.finalAmount }

        // Expenses: Cost of spare parts purchased today
        val expenses = partsPurchased.filter { it.purchaseDate >= today }
            .sumOf { it.purchasePrice * it.quantity }

        val profit = revenue - expenses
        val margin = if (revenue > 0) (profit / revenue) * 100 else 0.0
        
        // AI Logic for Health Score & Smart Move
        val healthScore = when {
            margin > 40 -> 90
            margin > 20 -> 70
            margin > 0 -> 50
            else -> 30
        }

        val (move, rec) = when {
            expenses > revenue -> Pair(
                "Expense Alert!",
                "You've spent more on parts today than you've collected. Focus on completing high-margin repairs by evening."
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

        return BusinessHealth(profit, revenue, expenses, margin, healthScore, move, rec)
    }
}
