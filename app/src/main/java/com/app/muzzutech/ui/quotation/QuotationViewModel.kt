package com.app.muzzutech.ui.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.RepairEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuotationViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository

    private val _entry = MutableStateFlow<RepairEntry?>(null)
    val entry: StateFlow<RepairEntry?> = _entry

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _entry.value = repository.getEntryById(id)
        }
    }

    fun saveQuotation(entryId: Long, chargeAmount: Double, advanceAmount: Double) {
        viewModelScope.launch {
            repository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    chargeAmount = chargeAmount,
                    advanceAmount = advanceAmount,
                    quotationDate = System.currentTimeMillis(),
                    quotationDone = true
                )
                repository.update(updated)
            }
        }
    }
}
