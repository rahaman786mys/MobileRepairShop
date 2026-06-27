package com.mobilerepair.shop.ui.handover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.RepairEntry
import com.mobilerepair.shop.data.model.SparePartPurchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HandoverViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val purchaseDao = MobileRepairApp.instance.database.sparePartPurchaseDao()

    private val _entry = MutableStateFlow<RepairEntry?>(null)
    val entry: StateFlow<RepairEntry?> = _entry

    private val _parts = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val parts: StateFlow<List<SparePartPurchase>> = _parts

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _entry.value = repository.getEntryById(id)
        }
        viewModelScope.launch {
            purchaseDao.getPurchasesByRepairId(id).collect { list ->
                _parts.value = list
            }
        }
    }

    fun completeHandover(
        entryId: Long,
        finalAmount: Double,
        paymentMode: String,
        cashAmount: Double,
        onlineAmount: Double
    ) {
        viewModelScope.launch {
            repository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    finalAmount = finalAmount,
                    paymentMode = paymentMode,
                    cashAmount = cashAmount,
                    onlineAmount = onlineAmount,
                    handoverDate = System.currentTimeMillis(),
                    handoverDone = true,
                    workStatus = "Done",
                    workDone = true,
                    completionDate = System.currentTimeMillis()
                )
                repository.update(updated)
            }
        }
    }
}
