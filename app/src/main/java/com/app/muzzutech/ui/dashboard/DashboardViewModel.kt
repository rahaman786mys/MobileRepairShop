package com.app.muzzutech.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.utils.AIAdvisor
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val purchaseDao = MobileRepairApp.instance.database.sparePartPurchaseDao()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday

    private val _recentEntries = MutableStateFlow<List<RepairEntry>>(emptyList())
    val recentEntries: StateFlow<List<RepairEntry>> = _recentEntries

    private val _businessHealth = MutableStateFlow<AIAdvisor.BusinessHealth?>(null)
    val businessHealth: StateFlow<AIAdvisor.BusinessHealth?> = _businessHealth

    init {
        loadDashboardData()
        observeBusinessHealth()
    }

    private fun observeBusinessHealth() {
        viewModelScope.launch {
            val todayStart = DateUtils.getStartOfDay()
            val todayEnd = DateUtils.getEndOfDay()
            
            combine(
                repository.getEntriesByDateRange(todayStart, todayEnd),
                purchaseDao.getPurchasesByDateRange(todayStart, todayEnd)
            ) { entries, purchases ->
                AIAdvisor.analyzeDailyHealth(entries, purchases)
            }.collect { health ->
                _businessHealth.value = health
            }
        }
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
            repository.getAllEntries().collect { entries ->
                _recentEntries.value = entries.take(10)
            }
        }
    }
}
