package com.app.muzzutech.ui.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DuesViewModel : ViewModel() {

    private val paymentDao = MobileRepairApp.instance.database.paymentDao()
    private val transactionDao = MobileRepairApp.instance.database.paymentTransactionDao()

    private val _allDues = MutableStateFlow<List<Payment>>(emptyList())
    val allDues: StateFlow<List<Payment>> = _allDues

    private val _dealerDues = MutableStateFlow<List<Payment>>(emptyList())
    val dealerDues: StateFlow<List<Payment>> = _dealerDues

    private val _supplierDues = MutableStateFlow<List<Payment>>(emptyList())
    val supplierDues: StateFlow<List<Payment>> = _supplierDues

    private val _customerDues = MutableStateFlow<List<Payment>>(emptyList())
    val customerDues: StateFlow<List<Payment>> = _customerDues

    private val _totalDue = MutableStateFlow(0.0)
    val totalDue: StateFlow<Double> = _totalDue

    private val _dealerDue = MutableStateFlow(0.0)
    val dealerDue: StateFlow<Double> = _dealerDue

    private val _supplierDue = MutableStateFlow(0.0)
    val supplierDue: StateFlow<Double> = _supplierDue

    private val _customerDue = MutableStateFlow(0.0)
    val customerDue: StateFlow<Double> = _customerDue

    private val _paymentHistory = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val paymentHistory: StateFlow<List<PaymentTransaction>> = _paymentHistory

    init {
        loadDues()
    }

    private fun loadDues() {
        viewModelScope.launch {
            paymentDao.getPendingDues().collectLatest { list ->
                _allDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getDuesByType("DEALER").collectLatest { list ->
                _dealerDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getDuesByType("SUPPLIER").collectLatest { list ->
                _supplierDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getDuesByType("CUSTOMER").collectLatest { list ->
                _customerDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueAmount().collectLatest { amount ->
                _totalDue.value = amount
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueByType("DEALER").collectLatest { amount ->
                _dealerDue.value = amount
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueByType("SUPPLIER").collectLatest { amount ->
                _supplierDue.value = amount
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueByType("CUSTOMER").collectLatest { amount ->
                _customerDue.value = amount
            }
        }
    }

    fun loadPaymentHistory(mobile: String) {
        viewModelScope.launch {
            transactionDao.getTransactionsByMobile(mobile).collectLatest { list ->
                _paymentHistory.value = list
            }
        }
    }

    fun recordPayment(payment: Payment, amount: Double, mode: String, note: String) {
        viewModelScope.launch {
            val newPaidAmount = payment.paidAmount + amount
            val newDueAmount = payment.totalAmount - newPaidAmount
            val newStatus = when {
                newDueAmount <= 0.0 -> "PAID"
                newPaidAmount > 0.0 -> "PARTIAL"
                else -> "UNPAID"
            }

            val updatedPayment = payment.copy(
                paidAmount = newPaidAmount,
                dueAmount = newDueAmount.coerceAtLeast(0.0),
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            paymentDao.update(updatedPayment)

            val transaction = PaymentTransaction(
                paymentId = payment.id,
                personType = payment.personType,
                personMobile = payment.personMobile,
                personName = payment.personName,
                amount = amount,
                paymentMode = mode,
                note = note
            )
            transactionDao.insert(transaction)
        }
    }
}
