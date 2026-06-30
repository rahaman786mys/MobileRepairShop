package com.app.muzzutech.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.data.db.dao.RepairEntryDao
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.utils.DateUtils
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

    private val _dailyReport = MutableStateFlow<List<DailyReportRow>>(emptyList())
    val dailyReport: StateFlow<List<DailyReportRow>> = _dailyReport

    private val _supplierPurchases = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val supplierPurchases: StateFlow<List<SparePartPurchase>> = _supplierPurchases

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
    }
}
