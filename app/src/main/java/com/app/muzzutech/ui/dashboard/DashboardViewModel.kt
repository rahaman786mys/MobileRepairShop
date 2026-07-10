package com.app.muzzutech.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.utils.AIAdvisor
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val database = MobileRepairApp.instance.database

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday

    private val _dailyRevenue = MutableStateFlow(0L)
    val dailyRevenue: StateFlow<Long> = _dailyRevenue
    private val _dailyInvest = MutableStateFlow(0L)
    val dailyInvest: StateFlow<Long> = _dailyInvest

    private val _dailyProfit = MutableStateFlow(0L)
    val dailyProfit: StateFlow<Long> = _dailyProfit

    private val _dailyPaidInvest = MutableStateFlow(0L)
    val dailyPaidInvest: StateFlow<Long> = _dailyPaidInvest

    private val _dailyDueInvest = MutableStateFlow(0L)
    val dailyDueInvest: StateFlow<Long> = _dailyDueInvest

    private val _salaryLiability = MutableStateFlow(0L)
    val salaryLiability: StateFlow<Long> = _salaryLiability

    private val _totalCustomerDue = MutableStateFlow(0L)
    val totalCustomerDue: StateFlow<Long> = _totalCustomerDue

    private val _totalSupplierDue = MutableStateFlow(0L)
    val totalSupplierDue: StateFlow<Long> = _totalSupplierDue

    private val _businessHealth = MutableStateFlow<AIAdvisor.BusinessHealth?>(null)
    val businessHealth: StateFlow<AIAdvisor.BusinessHealth?> = _businessHealth

    private val _unresolvedAlertsCount = MutableStateFlow(0)
    val unresolvedAlertsCount: StateFlow<Int> = _unresolvedAlertsCount

    private var _secondaryLoaded = false

    init {
        loadPrimaryData()
    }

    private fun loadPrimaryData() {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        val monthStart = DateUtils.getStartOfMonth()
        val monthEnd = DateUtils.getEndOfMonth(monthStart)

        viewModelScope.launch {
            try {
                repository.getPendingCount().collect { count ->
                    _pendingCount.value = count
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        viewModelScope.launch {
            try {
                repository.getCompletedCountInRange(todayStart, todayEnd).collect { count ->
                    _completedToday.value = count
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Step 2 Fix: Calculate Dashboard metrics directly from the Master Ledger (PaymentTransaction)
        viewModelScope.launch {
            try {
                database.paymentTransactionDao().getTransactionsByDateRange(todayStart, todayEnd).collectLatest { txns ->
                    // Revenue: All "IN" transactions of type REVENUE (Cash/Online from customers)
                    val dailyRev = txns.filter { it.direction == "IN" && it.transactionType == "REVENUE" }.sumOf { it.amount }
                    
                    // Expense: All "OUT" transactions (Supplier payments, Shop expenses, Salary)
                    val dailyExp = txns.filter { it.direction == "OUT" }.sumOf { it.amount }
                    
                    // Profit: Total IN - Total OUT
                    val dailyProf = dailyRev - dailyExp

                    _dailyRevenue.value = dailyRev
                    _dailyProfit.value = dailyProf
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Secondary Metrics (Investment portion)
        viewModelScope.launch {
            try {
                database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd).collectLatest { parts ->
                    val totalInvest = parts.sumOf { it.purchasePrice * it.quantity }
                    _dailyInvest.value = totalInvest
                    
                    // Paid portion of investment
                    database.paymentTransactionDao().getTransactionsByDateRange(todayStart, todayEnd).collectLatest { txns ->
                         val paidInvest = txns.filter { it.personType == "SUPPLIER" && it.direction == "OUT" }.sumOf { it.amount }
                         _dailyPaidInvest.value = paidInvest
                         _dailyDueInvest.value = (totalInvest - paidInvest).coerceAtLeast(0L)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadSecondaryData() {
        if (_secondaryLoaded) return
        _secondaryLoaded = true
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()

        viewModelScope.launch {
            try {
                combine(
                    database.paymentDao().getTotalDueByType("CUSTOMER"),
                    database.paymentDao().getTotalDueByType("DEALER")
                ) { customerDue, dealerDue ->
                    customerDue + dealerDue
                }.collect { total ->
                    _totalCustomerDue.value = total
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        viewModelScope.launch {
            try {
                database.paymentDao().getTotalDueByType("SUPPLIER").collectLatest { total ->
                    _totalSupplierDue.value = total
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        viewModelScope.launch {
            try {
                database.ledgerAlertDao().getUnresolved().collectLatest { alerts ->
                    _unresolvedAlertsCount.value = alerts.size
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        loadBusinessHealth()
    }

    private fun loadBusinessHealth() {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        viewModelScope.launch {
            try {
                combine(
                    database.repairEntryDao().getEntriesByDateRange(todayStart, todayEnd),
                    database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd),
                    database.expenseDao().getByDateRange(todayStart, todayEnd),
                    database.saleDao().getSalesByDateRange(todayStart, todayEnd),
                    database.partReturnDao().getReturnsByDateRangeQuery(todayStart, todayEnd)
                ) { repairs, parts, expenses, sales, returns ->
                    AIAdvisor.analyzeDailyHealth(repairs, parts, expenses, sales, returns)
                }.collect { health ->
                    _businessHealth.value = health
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private data class ProfitAggregate(
        val saleRevenue: Long = 0L,
        val customerCashIn: Long = 0L,
        val partPurchases: Long = 0L,
        val shopExpenses: Long = 0L,
        val salaryPayouts: Long = 0L,
        val salaryLiability: Long = 0L,
        val supplierPayments: Long = 0L
    )
}
