package com.app.muzzutech.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        viewModelScope.launch {
            combine(
                repository.getEntriesByDateRange(todayStart, todayEnd),
                database.sparePartPurchaseDao().getTotalPurchaseInRange(todayStart, todayEnd)
            ) { entries, investTotal ->
                val revenue = entries.sumOf { it.chargeAmount }
                val invest = investTotal ?: 0.0
                _dailyRevenue.value = revenue
                _dailyInvest.value = invest
                _dailyProfit.value = revenue - invest
            }.collect {}
        }
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
