package com.app.muzzutech.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()

        viewModelScope.launch {
            repository.getPendingCount().collect { count ->
                _pendingCount.value = count
            }
        }
        viewModelScope.launch {
            repository.getCompletedCountInRange(todayStart, todayEnd).collect { count ->
                _completedToday.value = count
            }
        }
        // Revenue = sum of finalAmount for entries handed over today
        viewModelScope.launch {
            repository.getRevenueInRange(todayStart, todayEnd).collect { revenue ->
                _dailyRevenue.value = revenue ?: 0.0
                _dailyProfit.value = (revenue ?: 0.0) - _dailyInvest.value
            }
        }
        // Invest = sum of parts purchased today
        viewModelScope.launch {
            database.sparePartPurchaseDao()
                .getTotalPurchaseInRange(todayStart, todayEnd).collect { total ->
                    _dailyInvest.value = total ?: 0.0
                    _dailyProfit.value = _dailyRevenue.value - (total ?: 0.0)
                }
        }
        // Paid vs Due from supplier payments today
        viewModelScope.launch {
            database.paymentDao()
                .getPaymentsByTypeAndDate("SUPPLIER", todayStart, todayEnd).collect { payments ->
                    val paid = payments.filter { it.status == "PAID" }.sumOf { it.totalAmount }
                    val due = payments.filter { it.status != "PAID" }.sumOf { it.dueAmount }
                    _dailyPaidInvest.value = paid
                    _dailyDueInvest.value = due
                }
        }
    }
}
