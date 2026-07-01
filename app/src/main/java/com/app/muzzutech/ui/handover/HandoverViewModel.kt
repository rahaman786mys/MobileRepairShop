package com.app.muzzutech.ui.handover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase
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
            val db = MobileRepairApp.instance.database
            repository.getEntryById(entryId)?.let { entry ->
                // 1. Update Repair Entry
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

                // 2. Create Payment Record (Accounting)
                val isPayLater = paymentMode == "Pay Later"
                val paidTotal = if (isPayLater) 0.0 else (cashAmount + onlineAmount)
                
                val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
                val personName = entry.customerName.ifEmpty { entry.dealerName }
                val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"

                val payment = com.app.muzzutech.data.model.Payment(
                    personType = personType,
                    personMobile = personMobile,
                    personName = personName,
                    description = "Repair - ${entry.deviceBrand} ${entry.deviceModel}",
                    totalAmount = finalAmount,
                    paidAmount = paidTotal,
                    dueAmount = finalAmount - paidTotal,
                    status = if (isPayLater) "UNPAID" else if (paidTotal >= finalAmount) "PAID" else "PARTIAL",
                    linkedEntryId = entry.id
                )
                val paymentId = db.paymentDao().insert(payment)

                // 3. Create Transaction Records (Cash Flow)
                if (!isPayLater) {
                    if (cashAmount > 0) {
                        db.paymentTransactionDao().insert(
                            com.app.muzzutech.data.model.PaymentTransaction(
                                paymentId = paymentId,
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                amount = cashAmount,
                                paymentMode = "CASH",
                                note = "Received during handover"
                            )
                        )
                    }
                    if (onlineAmount > 0) {
                        db.paymentTransactionDao().insert(
                            com.app.muzzutech.data.model.PaymentTransaction(
                                paymentId = paymentId,
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                amount = onlineAmount,
                                paymentMode = "ONLINE",
                                note = "Received during handover"
                            )
                        )
                    }
                }
            }
        }
    }
}
