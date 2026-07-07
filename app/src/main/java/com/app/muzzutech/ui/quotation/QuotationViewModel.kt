package com.app.muzzutech.ui.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.RepairEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuotationViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val db = MobileRepairApp.instance.database

    private val _entry = MutableStateFlow<RepairEntry?>(null)
    val entry: StateFlow<RepairEntry?> = _entry

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _entry.value = repository.getEntryById(id)
        }
    }

    fun saveQuotation(entryId: Long, chargeAmount: Long, advanceAmount: Long) {
        viewModelScope.launch {
            db.withTransaction {
                repository.getEntryById(entryId)?.let { entry ->
                    val updated = entry.copy(
                        chargeAmount = chargeAmount,
                        advanceAmount = advanceAmount,
                        quotationDate = System.currentTimeMillis(),
                        quotationDone = true
                    )
                    repository.update(updated)

                    if (advanceAmount > 0L) {
                        val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
                        val personName = entry.customerName.ifEmpty { entry.dealerName }
                        val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"
                        val advanceTxnId = db.paymentTransactionDao().insert(
                            com.app.muzzutech.data.model.PaymentTransaction(
                                paymentId = null,
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                amount = advanceAmount,
                                paymentMode = "CASH",
                                note = "Advance for ${entry.deviceBrand} ${entry.deviceModel}"
                            )
                        )
                        // Store the explicit transaction ID on the RepairEntry for handover linking
                        repository.update(entry.copy(
                            chargeAmount = chargeAmount,
                            advanceAmount = advanceAmount,
                            advancePaymentTransactionId = advanceTxnId,
                            quotationDate = System.currentTimeMillis(),
                            quotationDone = true
                        ))
                    } else {
                        repository.update(updated)
                    }
                }
            }
        }
    }
}
