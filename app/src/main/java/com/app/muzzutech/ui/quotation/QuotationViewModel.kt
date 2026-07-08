package com.app.muzzutech.ui.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class QuotationViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val db = MobileRepairApp.instance.database

    private val _entry = MutableStateFlow<RepairEntry?>(null)
    val entry: StateFlow<RepairEntry?> = _entry

    private val _saveComplete = MutableSharedFlow<Long>()
    val saveComplete: SharedFlow<Long> = _saveComplete.asSharedFlow()

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _entry.value = repository.getEntryById(id)
        }
    }

    fun saveQuotation(entryId: Long, faultDetected: String) {
        viewModelScope.launch {
            db.withTransaction {
                repository.getEntryById(entryId)?.let { entry ->
                    repository.update(entry.copy(
                        faultDetected = faultDetected,
                        quotationDate = System.currentTimeMillis(),
                        quotationDone = true
                    ))
                }
            }
            _saveComplete.emit(entryId)
        }
    }
}