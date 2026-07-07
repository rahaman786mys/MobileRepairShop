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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val db = MobileRepairApp.instance.database
    private val purchaseDao = db.sparePartPurchaseDao()
    private val saleDao = db.saleDao()
    private val expenseDao = db.expenseDao()
    private val salaryDao = db.salaryDao()
    private val transactionDao = db.paymentTransactionDao()

    private val _revenue = MutableStateFlow(0L)
    val revenue: StateFlow<Long> = _revenue

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount

    private val _dailyReport = MutableStateFlow<List<DailyReportRow>>(emptyList())
    val dailyReport: StateFlow<List<DailyReportRow>> = _dailyReport

    private val _supplierPurchases = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val supplierPurchases: StateFlow<List<SparePartPurchase>> = _supplierPurchases

    private val _directSales = MutableStateFlow<List<Sale>>(emptyList())
    val directSales: StateFlow<List<Sale>> = _directSales

    private val _expenses = MutableStateFlow(0L)
    val expenses: StateFlow<Long> = _expenses

    private val _profit = MutableStateFlow(0L)
    val profit: StateFlow<Long> = _profit

    private val _cogs = MutableStateFlow(0L)
    val cogs: StateFlow<Long> = _cogs

    private val _salariesPaid = MutableStateFlow(0L)
    val salariesPaid: StateFlow<Long> = _salariesPaid

    private val _unresolvedAlertsCount = MutableStateFlow(0)
    val unresolvedAlertsCount: StateFlow<Int> = _unresolvedAlertsCount

    private var reportJobs: List<Job> = emptyList()

    init {
        observeLedgerAlerts()
    }

    private fun observeLedgerAlerts() {
        viewModelScope.launch {
            db.ledgerAlertDao().getUnresolved().collect { alerts ->
                _unresolvedAlertsCount.value = alerts.size
            }
        }
    }

    fun runAuditNow(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ctx = com.app.muzzutech.MobileRepairApp.instance.applicationContext
                val wm = androidx.work.WorkManager.getInstance(ctx)
                val request = androidx.work.OneTimeWorkRequest.Builder(com.app.muzzutech.work.LedgerAuditWorker::class.java)
                    .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 1, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                wm.enqueue(request)
                onComplete("Audit started — check alerts below when complete")
            } catch (e: Exception) {
                onComplete("Failed: ${e.message}")
            }
        }
    }

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

    private fun loadData(start: Long, end: Long) {
        reportJobs.forEach { it.cancel() }
        val monthStart = DateUtils.getStartOfMonth(start)
        val monthEnd = DateUtils.getEndOfMonth(start)
        reportJobs = listOf(
            viewModelScope.launch {
                repository.getRevenueInRange(start, end).collect { rev ->
                    _revenue.value = rev ?: 0L
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
                }
            },
            // True COGS-based profit: Revenue - COGS - Expenses - Salaries
            viewModelScope.launch {
                combine(
                    repository.getRevenueInRange(start, end),
                    purchaseDao.getPurchasesByDateRange(start, end),
                    expenseDao.getByDateRange(start, end),
                    salaryDao.getByMonth(monthStart, monthEnd)
                ) { rev, purchases, expenses, salaries ->
                    val revenue = rev ?: 0L
                    // COGS: parts purchased in range (approximation; exact COGS would need RepairEntry linkage)
                    val cogs = purchases.sumOf { it.purchasePrice * it.quantity }
                    val shopExpenses = expenses.sumOf { it.amount }
                    val salariesPaid = salaries.sumOf { it.paidAmount }
                    val totalCost = cogs + shopExpenses + salariesPaid
                    Triple(revenue, cogs, totalCost)
                }.collect { (revenue, cogs, totalCost) ->
                    _cogs.value = cogs
                    _profit.value = revenue - totalCost
                }
            }
        )
    }
}
