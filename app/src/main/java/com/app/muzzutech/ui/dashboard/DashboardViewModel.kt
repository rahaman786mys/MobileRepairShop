package com.app.muzzutech.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.utils.AIAdvisor
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val _dailyInvest = MutableStateFlow(0L)
    val dailyInvest: StateFlow<Long> = _dailyInvest

    private val _dailyProfit = MutableStateFlow(0L)
    val dailyProfit: StateFlow<Long> = _dailyProfit

    private val _dailyPaidInvest = MutableStateFlow(0L)
    val dailyPaidInvest: StateFlow<Long> = _dailyPaidInvest

    private val _dailyDueInvest = MutableStateFlow(0L)
    val dailyDueInvest: StateFlow<Long> = _dailyDueInvest

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

        // True COGS-based daily profit: Revenue - COGS - Expenses - Salaries
        viewModelScope.launch {
            try {
                combine(
                    database.repairEntryDao().getCompletedEntries(),
                    database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd),
                    database.expenseDao().getByDateRange(todayStart, todayEnd),
                    database.salaryDao().getByMonth(monthStart, DateUtils.getEndOfMonth(monthStart))
                ) { completed, parts, expenses, salaries ->
                    // Revenue: finalAmount of entries handed over today
                    val todayHandovers = completed.filter { it.handoverDate in todayStart..todayEnd }
                    val revenue = todayHandovers.sumOf { it.finalAmount }

                    // COGS: parts consumed in today's handovers only
                    val handoverIds = todayHandovers.map { it.id }.toSet()
                    val cogs = parts.filter { it.repairEntryId in handoverIds }
                        .sumOf { it.purchasePrice * it.quantity }

                    // Shop expenses (paid today)
                    val shopExpenses = expenses.sumOf { it.amount }

                    // Salaries paid this month (apportioned daily view)
                    val salariesPaid = salaries.sumOf { it.paidAmount }

                    val totalCost = cogs + shopExpenses + salariesPaid
                    Pair(revenue, totalCost)
                }.collect { (revenue, totalCost) ->
                    _dailyRevenue.value = revenue
                    _dailyProfit.value = revenue - totalCost
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        viewModelScope.launch {
            try {
                database.sparePartPurchaseDao()
                    .getTotalPurchaseInRange(todayStart, todayEnd).collect { total ->
                        _dailyInvest.value = total ?: 0L
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
                database.paymentDao()
                    .getPaymentsByTypeAndDate("SUPPLIER", todayStart, todayEnd).collect { payments ->
                        val paid = payments.sumOf { it.paidAmount }
                        val due = payments.sumOf { it.dueAmount }
                        _dailyPaidInvest.value = paid
                        _dailyDueInvest.value = due
                    }
            } catch (e: Exception) { e.printStackTrace() }
        }

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
}
