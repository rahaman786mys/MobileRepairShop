package com.app.muzzutech.ui.handover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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

    fun cancelWork(entryId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            if (entry.handoverDone) return@launch
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                // 1. If advance exists, create a cash-out refund transaction
                val advanceTxnId = entry.advancePaymentTransactionId
                if (advanceTxnId != null) {
                    val advanceTxn = db.paymentTransactionDao().getTransactionById(advanceTxnId)
                    if (advanceTxn != null) {
                        db.paymentTransactionDao().insert(
                            com.app.muzzutech.data.model.PaymentTransaction(
                                paymentId = null,
                                personType = advanceTxn.personType,
                                personMobile = advanceTxn.personMobile,
                                personName = advanceTxn.personName,
                                amount = -advanceTxn.amount,
                                paymentMode = "REFUND",
                                note = "Refund of advance for cancelled repair #${entry.id}: ${entry.deviceBrand} ${entry.deviceModel}"
                            )
                        )
                    }
                }

                // 2. Delete linked SparePartPurchases and their Payments
                val parts = purchaseDao.getPurchasesByRepairIdList(entryId)
                for (part in parts) {
                    val linkedPayment = db.paymentDao().getPaymentByLinkedPartId(part.id)
                    if (linkedPayment != null) {
                        db.paymentDao().delete(linkedPayment)
                    }
                    purchaseDao.delete(part)
                }

                // 3. Mark entry Cancelled
                repository.forceUpdate(entry.copy(
                    workStatus = "Cancelled",
                    isDraft = true,
                    advanceAmount = 0L,
                    advancePaymentTransactionId = null
                ))
            }
            onDone()
        }
    }

    suspend fun completeHandover(
        entryId: Long,
        finalAmount: Long,
        paymentMode: String,
        cashAmount: Long,
        onlineAmount: Long
    ) {
        val db = MobileRepairApp.instance.database
        db.withTransaction {
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
                _entry.value = updated

                val isPayLater = paymentMode == "Pay Later"
                val handoverPaid = if (isPayLater) 0L else (cashAmount + onlineAmount)
                val paidTotal = (entry.advanceAmount + handoverPaid).coerceAtMost(finalAmount)

                val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
                val personName = entry.customerName.ifEmpty { entry.dealerName }
                val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"

                val existingPayment = db.paymentDao().getPaymentByLinkedEntryId(entry.id)
                val payment = if (existingPayment != null) {
                    existingPayment.copy(
                        totalAmount = finalAmount,
                        paidAmount = paidTotal,
                        dueAmount = (finalAmount - paidTotal).coerceAtLeast(0L),
                        status = if (isPayLater) "UNPAID" else if (paidTotal >= finalAmount) "PAID" else "PARTIAL",
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    com.app.muzzutech.data.model.Payment(
                        personType = personType,
                        personMobile = personMobile,
                        personName = personName,
                        description = "Repair - ${entry.deviceBrand} ${entry.deviceModel}",
                        totalAmount = finalAmount,
                        paidAmount = paidTotal,
                        dueAmount = (finalAmount - paidTotal).coerceAtLeast(0L),
                        status = if (isPayLater) "UNPAID" else if (paidTotal >= finalAmount) "PAID" else "PARTIAL",
                        linkedEntryId = entry.id
                    )
                }
                val paymentId = if (existingPayment != null) {
                    db.paymentDao().update(payment)
                    existingPayment.id
                } else {
                    db.paymentDao().insert(payment)
                }

                // Link the advance PaymentTransaction via the explicit advancePaymentTransactionId
                // stored on RepairEntry during quotation. This replaces the fragile mobile+amount match.
                val advanceTxnId = entry.advancePaymentTransactionId
                if (advanceTxnId != null) {
                    val advanceTxn = db.paymentTransactionDao().getTransactionById(advanceTxnId)
                    if (advanceTxn != null) {
                        db.paymentTransactionDao().update(advanceTxn.copy(paymentId = paymentId))
                    }
                }

                if (!isPayLater) {
                    if (cashAmount > 0L) {
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
                    if (onlineAmount > 0L) {
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
