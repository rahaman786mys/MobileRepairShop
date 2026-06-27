package com.mobilerepair.shop.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.RepairEntry
import com.mobilerepair.shop.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday

    private val _todayRevenue = MutableStateFlow(0.0)
    val todayRevenue: StateFlow<Double> = _todayRevenue

    private val _recentEntries = MutableStateFlow<List<RepairEntry>>(emptyList())
    val recentEntries: StateFlow<List<RepairEntry>> = _recentEntries

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            // Pending count
            repository.getPendingCount().collect { count ->
                _pendingCount.value = count
            }
        }
        viewModelScope.launch {
            // Today's completed
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            repository.getCompletedCountInRange(todayStart, todayEnd).collect { count ->
                _completedToday.value = count
            }
        }
        viewModelScope.launch {
            // Today's revenue
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            repository.getRevenueInRange(todayStart, todayEnd).collect { revenue ->
                _todayRevenue.value = revenue ?: 0.0
            }
        }
        viewModelScope.launch {
            // Recent entries (last 10)
            repository.getAllEntries().collect { entries ->
                _recentEntries.value = entries.take(10)
            }
        }
    }
}
