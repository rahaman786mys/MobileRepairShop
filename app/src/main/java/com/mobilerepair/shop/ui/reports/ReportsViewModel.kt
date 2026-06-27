package com.mobilerepair.shop.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.SparePartPurchase
import com.mobilerepair.shop.data.db.dao.RepairEntryDao
import com.mobilerepair.shop.utils.AIAnalyzer
import com.mobilerepair.shop.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val purchaseDao = MobileRepairApp.instance.database.sparePartPurchaseDao()

    private val _revenue = MutableStateFlow(0.0)
    val revenue: StateFlow<Double> = _revenue

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount

    private val _dailyReport = MutableStateFlow<List<RepairEntryDao.DailyReportRow>>(emptyList())
    val dailyReport: StateFlow<List<RepairEntryDao.DailyReportRow>> = _dailyReport

    private val _supplierPurchases = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val supplierPurchases: StateFlow<List<SparePartPurchase>> = _supplierPurchases

    private val _aiInsights = MutableStateFlow("")
    val aiInsights: StateFlow<String> = _aiInsights

    fun loadReport(period: String) {
        val (start, end) = when (period) {
            "Daily" -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
            "Weekly" -> Pair(DateUtils.getStartOfWeek(), DateUtils.getEndOfDay())
            "Monthly" -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay())
            else -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
        }

        viewModelScope.launch {
            repository.getRevenueInRange(start, end).collect { rev ->
                _revenue.value = rev ?: 0.0
            }
        }
        viewModelScope.launch {
            repository.getCompletedCountInRange(start, end).collect { count ->
                _completedCount.value = count
            }
        }
        viewModelScope.launch {
            repository.getDailyReport(start, end).collect { report ->
                _dailyReport.value = report
            }
        }
        viewModelScope.launch {
            purchaseDao.getPurchasesByDateRange(start, end).collect { purchases ->
                _supplierPurchases.value = purchases
            }
        }
        viewModelScope.launch {
            // AI Insights
            repository.getEntriesByDateRange(start, end).collect { entries ->
                val trends = AIAnalyzer.analyzeRepairTrends(entries)
                val insights = buildString {
                    append("📊 AI Analysis:\n")
                    append("• Total Repairs: ${trends.totalRepairs}\n")
                    append("• Completed: ${trends.completedRepairs}\n")
                    append("• Avg Revenue/Job: ₹ ${String.format("%.0f", trends.averageRevenue)}\n")
                    append("• Avg Repair Time: ${trends.averageRepairTimeDays} days\n")
                    if (trends.topFaults.isNotEmpty()) {
                        append("• Most Common: ${trends.topFaults[0]}\n")
                    }
                }
                _aiInsights.value = insights
            }
        }
    }
}
