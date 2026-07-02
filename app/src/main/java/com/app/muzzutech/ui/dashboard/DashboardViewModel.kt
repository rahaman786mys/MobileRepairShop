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

    private val _dailyRevenue = MutableStateFlow(0.0)
    private val _dailyInvest = MutableStateFlow(0.0)
    val dailyInvest: StateFlow<Double> = _dailyInvest

    private val _dailyProfit = MutableStateFlow(0.0)
    val dailyProfit: StateFlow<Double> = _dailyProfit

    private val _dailyPaidInvest = MutableStateFlow(0.0)
    val dailyPaidInvest: StateFlow<Double> = _dailyPaidInvest

    private val _dailyDueInvest = MutableStateFlow(0.0)
    val dailyDueInvest: StateFlow<Double> = _dailyDueInvest

    private val _totalCustomerDue = MutableStateFlow(0.0)
    val totalCustomerDue: StateFlow<Double> = _totalCustomerDue

    private val _totalSupplierDue = MutableStateFlow(0.0)
    val totalSupplierDue: StateFlow<Double> = _totalSupplierDue

    private val _businessHealth = MutableStateFlow<AIAdvisor.BusinessHealth?>(null)
    val businessHealth: StateFlow<AIAdvisor.BusinessHealth?> = _businessHealth

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()

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
        
        // Revenue (Cash In) = Sum of actual PAID amounts from CUSTOMER/DEALER today
        viewModelScope.launch {
            try {
                database.paymentTransactionDao().getTransactionsByDateRange(todayStart, todayEnd).collect { transactions ->
                    val revenue = transactions.filter { it.personType != "SUPPLIER" }.sumOf { it.amount }
                    val expense = transactions.filter { it.personType == "SUPPLIER" }.sumOf { it.amount }
                    
                    _dailyRevenue.value = revenue
                    // Net Cash Flow = Revenue - Actual Cash paid out to suppliers today
                    _dailyProfit.value = revenue - expense
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Investment = sum of parts purchased today (Regardless of paid/due)
        viewModelScope.launch {
            try {
                database.sparePartPurchaseDao()
                    .getTotalPurchaseInRange(todayStart, todayEnd).collect { total ->
                        _dailyInvest.value = total ?: 0.0
                    }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Paid vs Due from supplier obligations today
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

        // Customer Dues Total (Accounts Receivable) — combine instead of nested collectLatest
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

        // Supplier Dues Total (Accounts Payable)
        viewModelScope.launch {
            try {
                database.paymentDao().getTotalDueByType("SUPPLIER").collectLatest { total ->
                    _totalSupplierDue.value = total
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // AI Advisor - Daily Business Health
        loadBusinessHealth()
    }

    private fun loadBusinessHealth() {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        viewModelScope.launch {
            try {
                combine(
                    database.repairEntryDao().getEntriesByDateRange(todayStart, todayEnd),
                    database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd)
                ) { repairs, parts ->
                    AIAdvisor.analyzeDailyHealth(repairs, parts)
                }.collect { health ->
                    _businessHealth.value = health
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
