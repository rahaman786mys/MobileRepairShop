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

    fun saveQuotation(entryId: Long, chargeAmount: Long, advanceAmount: Long, faultDetected: String) {
        viewModelScope.launch {
            db.withTransaction {
                repository.getEntryById(entryId)?.let { entry ->
                    val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
                    val personName = entry.customerName.ifEmpty { entry.dealerName }
                    val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"

                    if (advanceAmount > 0L) {
                        val advanceTxnId = db.paymentTransactionDao().insert(
                            PaymentTransaction(
                                paymentId = null,
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                amount = advanceAmount,
                                paymentMode = "CASH",
                                note = "Advance for ${entry.deviceBrand} ${entry.deviceModel}"
                            )
                        )
                        // Create a Payment record so the advance appears in the Dues screen
                        val paymentId = db.paymentDao().insert(
                            com.app.muzzutech.data.model.Payment(
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                description = "Advance for ${entry.deviceBrand} ${entry.deviceModel}",
                                totalAmount = chargeAmount,
                                paidAmount = advanceAmount,
                                dueAmount = (chargeAmount - advanceAmount).coerceAtLeast(0L),
                                status = if (advanceAmount >= chargeAmount) "PAID" else "PARTIAL",
                                linkedEntryId = entryId
                            )
                        )
                        // Link the advance txn to the payment
                        db.paymentTransactionDao().getTransactionById(advanceTxnId)?.let { txn ->
                            db.paymentTransactionDao().update(txn.copy(paymentId = paymentId))
                        }
                        repository.update(entry.copy(
                            faultDetected = faultDetected,
                            chargeAmount = chargeAmount,
                            advanceAmount = advanceAmount,
                            advancePaymentTransactionId = advanceTxnId,
                            quotationDate = System.currentTimeMillis(),
                            quotationDone = true
                        ))
                    } else {
                        repository.update(entry.copy(
                            faultDetected = faultDetected,
                            chargeAmount = chargeAmount,
                            advanceAmount = 0L,
                            quotationDate = System.currentTimeMillis(),
                            quotationDone = true
                        ))
                    }
                }
            }
            _saveComplete.emit(entryId)
        }
    }
}