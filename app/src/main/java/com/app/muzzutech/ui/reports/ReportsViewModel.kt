package com.app.muzzutech.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val db = MobileRepairApp.instance.database
    private val purchaseDao = db.sparePartPurchaseDao()
    private val saleDao = db.saleDao()
    private val expenseDao = db.expenseDao()
    private val transactionDao = db.paymentTransactionDao()

    private val _revenue = MutableStateFlow(0.0)
    val revenue: StateFlow<Double> = _revenue

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount

    private val _dailyReport = MutableStateFlow<List<DailyReportRow>>(emptyList())
    val dailyReport: StateFlow<List<DailyReportRow>> = _dailyReport

    private val _supplierPurchases = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val supplierPurchases: StateFlow<List<SparePartPurchase>> = _supplierPurchases

    private val _directSales = MutableStateFlow<List<Sale>>(emptyList())
    val directSales: StateFlow<List<Sale>> = _directSales

    private val _expenses = MutableStateFlow(0.0)
    val expenses: StateFlow<Double> = _expenses

    private val _profit = MutableStateFlow(0.0)
    val profit: StateFlow<Double> = _profit

    private var reportJobs: List<Job> = emptyList()

    fun loadReport(period: String) {
        val (start, end) = when (period) {
            "Daily" -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
            "Weekly" -> Pair(DateUtils.getStartOfWeek(), DateUtils.getEndOfDay())
            "Monthly" -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay())
            else -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
        }
        loadData(start, end)
    }

    fun loadCustomReport(start: Long, end: Long) {
        loadData(start, end)
    }

    private fun updateProfit() {
        _profit.value = _revenue.value - _expenses.value
    }

    private fun loadData(start: Long, end: Long) {
        reportJobs.forEach { it.cancel() }
        reportJobs = listOf(
            viewModelScope.launch {
                repository.getRevenueInRange(start, end).collect { rev ->
                    _revenue.value = rev ?: 0.0
                    updateProfit()
                }
            },
            viewModelScope.launch {
                repository.getCompletedCountInRange(start, end).collect { count ->
                    _completedCount.value = count
                }
            },
            viewModelScope.launch {
                repository.getDailyReport(start, end).collect { report ->
                    _dailyReport.value = report
                }
            },
            viewModelScope.launch {
                purchaseDao.getPurchasesByDateRange(start, end).collect { purchases ->
                    _supplierPurchases.value = purchases
                }
            },
            viewModelScope.launch {
                saleDao.getSalesByDateRange(start, end).collect { sales ->
                    _directSales.value = sales
                }
            },
            viewModelScope.launch {
                expenseDao.getByDateRange(start, end).collect { list ->
                    _expenses.value = list.sumOf { it.amount }
                    updateProfit()
                }
            }
        )
    }
}
