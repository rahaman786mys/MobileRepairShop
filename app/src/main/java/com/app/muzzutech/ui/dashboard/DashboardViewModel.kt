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

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday

    private val _dailyProfit = MutableStateFlow(0.0)
    val dailyProfit: StateFlow<Double> = _dailyProfit

    private val _dailyExpense = MutableStateFlow(0.0)
    val dailyExpense: StateFlow<Double> = _dailyExpense

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            repository.getPendingCount().collect { count ->
                _pendingCount.value = count
            }
        }
        viewModelScope.launch {
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            repository.getCompletedCountInRange(todayStart, todayEnd).collect { count ->
                _completedToday.value = count
            }
        }
        viewModelScope.launch {
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            repository.getEntriesByDateRange(todayStart, todayEnd).collect { entries ->
                val revenue = entries.sumOf { it.chargeAmount }
                _dailyProfit.value = revenue
            }
        }
        viewModelScope.launch {
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            MobileRepairApp.instance.database.sparePartPurchaseDao()
                .getPurchasesByDateRange(todayStart, todayEnd).collect { purchases ->
                    val expense = purchases.sumOf { it.purchasePrice * it.quantity }
                    _dailyExpense.value = expense
                }
        }
    }
}
