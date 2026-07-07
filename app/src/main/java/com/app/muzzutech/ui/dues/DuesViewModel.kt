package com.app.muzzutech.ui.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    private val _totalDue = MutableStateFlow(0L)
    val totalDue: StateFlow<Long> = _totalDue

    private val _dealerDue = MutableStateFlow(0L)
    val dealerDue: StateFlow<Long> = _dealerDue

    private val _supplierDue = MutableStateFlow(0L)
    val supplierDue: StateFlow<Long> = _supplierDue

    private val _customerDue = MutableStateFlow(0L)
    val customerDue: StateFlow<Long> = _customerDue

    private val _paymentHistory = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val paymentHistory: StateFlow<List<PaymentTransaction>> = _paymentHistory

    private val _paymentError = MutableSharedFlow<String>()
    val paymentError: SharedFlow<String> = _paymentError.asSharedFlow()

    init {
        loadPrimaryDues()
    }

    private fun loadPrimaryDues() {
        viewModelScope.launch {
            paymentDao.getPendingDues().collectLatest { list ->
                _allDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getDuesByType("CUSTOMER").collectLatest { list ->
                _customerDues.value = list
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueByType("CUSTOMER").collectLatest { amount ->
                _customerDue.value = amount
            }
        }
        viewModelScope.launch {
            combine(
                paymentDao.getTotalDueByType("DEALER"),
                paymentDao.getTotalDueByType("SUPPLIER")
            ) { dealer, supplier ->
                dealer + supplier
            }.collect { total ->
                _totalDue.value = total
            }
        }
    }

    fun loadSecondaryDues() {
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
            paymentDao.getTotalDueByType("DEALER").collectLatest { amount ->
                _dealerDue.value = amount
            }
        }
        viewModelScope.launch {
            paymentDao.getTotalDueByType("SUPPLIER").collectLatest { amount ->
                _supplierDue.value = amount
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

    fun recordPayment(payment: Payment, amount: Long, mode: String, note: String) {
        viewModelScope.launch {
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                // Re-read the payment inside the transaction to avoid lost-update race
                val current = paymentDao.getPaymentById(payment.id) ?: return@withTransaction

                // Overpayment guard: amount must not exceed current due amount
                val currentDueAmount = current.totalAmount - current.paidAmount
                if (amount > currentDueAmount) {
                    _paymentError.emit("Payment of ${com.app.muzzutech.utils.PriceUtils.formatPrice(amount)} exceeds due amount of ${com.app.muzzutech.utils.PriceUtils.formatPrice(currentDueAmount)}")
                    return@withTransaction
                }

                val newPaidAmount = current.paidAmount + amount
                val newDueAmount = current.totalAmount - newPaidAmount
                val newStatus = when {
                    newDueAmount <= 0L -> "PAID"
                    newPaidAmount > 0L -> "PARTIAL"
                    else -> "UNPAID"
                }

                val updatedPayment = current.copy(
                    paidAmount = newPaidAmount,
                    dueAmount = newDueAmount.coerceAtLeast(0L),
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
                paymentDao.update(updatedPayment)

                val transaction = PaymentTransaction(
                    paymentId = current.id,
                    personType = current.personType,
                    personMobile = current.personMobile,
                    personName = current.personName,
                    amount = amount,
                    paymentMode = mode,
                    note = note
                )
                transactionDao.insert(transaction)
            }
        }
    }
}
