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

        // Cash-basis profit: only actual cash movement counts as revenue/cost.
        // Invoice totals are excluded here to avoid double-counting handovers plus collections.
        viewModelScope.launch {
            try {
                val handoverFlow = database.repairEntryDao().getCompletedEntries()
                val salesFlow = database.saleDao().getSalesByDateRange(todayStart, todayEnd)
                val txnFlow = database.paymentTransactionDao().getTransactionsByDateRange(todayStart, todayEnd)
                val partsFlow = database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd)

                combine(handoverFlow, salesFlow, txnFlow, partsFlow) { a, b, c, d ->
                    val saleRevenue = b.sumOf { it.salePrice }

                    // Cash in from customers/dealers: advances (paymentId=null) + due collections (paymentId!=null)
                    val customerCashIn = c.filter {
                        (it.personType == "CUSTOMER" || it.personType == "DEALER") &&
                                it.amount > 0L
                    }.sumOf { it.amount }

                    // Actual cash out to suppliers (not refunds — refunds have personType SUPPLIER_REFUND)
                    val supplierPayments = c.filter {
                        it.personType == "SUPPLIER" && it.amount > 0L
                    }.sumOf { it.amount }

                    val partPurchases = d.sumOf { it.purchasePrice * it.quantity }

                    ProfitAggregate(
                        saleRevenue = saleRevenue,
                        customerCashIn = customerCashIn,
                        partPurchases = partPurchases,
                        supplierPayments = supplierPayments
                    )
                }.combine(database.expenseDao().getByDateRange(todayStart, todayEnd)) { agg, expenses ->
                    agg.copy(shopExpenses = expenses.sumOf { it.amount })
                }.combine(database.salaryDao().getByMonth(monthStart, monthEnd)) { agg, salaries ->
                    agg.copy(
                        salaryPayouts = salaries.sumOf { it.paidAmount },
                        salaryLiability = salaries.sumOf { it.dueAmount }
                    )
                }.collect { agg ->
                    // Cash-basis revenue: all cash in from customers/dealers + direct sales
                    val totalRevenue = agg.customerCashIn + agg.saleRevenue
                    // Cost: parts bought + shop expenses + salaries paid + supplier payments
                    // supplier refunds INCREASE profit (cost recovery), so they increase revenue not decrease cost
                    val totalCost = agg.partPurchases + agg.shopExpenses + agg.salaryPayouts + agg.supplierPayments
                    _dailyRevenue.value = totalRevenue
                    _dailyProfit.value = totalRevenue - totalCost
                    _dailyInvest.value = agg.partPurchases
                    _dailyPaidInvest.value = agg.supplierPayments
                    _dailyDueInvest.value = (agg.partPurchases - agg.supplierPayments).coerceAtLeast(0L)
                    _salaryLiability.value = agg.salaryLiability
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
