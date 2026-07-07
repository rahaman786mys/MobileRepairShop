package com.app.muzzutech.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpensesViewModel : ViewModel() {

    private val dao = MobileRepairApp.instance.database.expenseDao()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _monthStart = MutableStateFlow(DateUtils.getStartOfMonth())
    val monthStart: StateFlow<Long> = _monthStart

    private val _totalThisMonth = MutableStateFlow(0L)
    val totalThisMonth: StateFlow<Long> = _totalThisMonth

    private val _categoryTotals = MutableStateFlow<Map<String, Long>>(emptyMap())
    val categoryTotals: StateFlow<Map<String, Long>> = _categoryTotals

    init {
        observeExpenses()
        observeMonth()
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            dao.getAll().collectLatest {
                _expenses.value = it
                recomputeTotals(it)
            }
        }
    }

    private fun observeMonth() {
        viewModelScope.launch {
            _monthStart.collect { ms ->
                recomputeTotals(_expenses.value)
            }
        }
    }

    private fun recomputeTotals(all: List<Expense>) {
        val ms = _monthStart.value
        val monthEnd = DateUtils.getEndOfMonth(ms)
        val inMonth = all.filter { it.date in ms..monthEnd }
        _totalThisMonth.value = inMonth.sumOf { it.amount }
        val byCat = inMonth.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
        _categoryTotals.value = byCat
    }

    fun selectMonth(monthStart: Long) { _monthStart.value = monthStart }

    fun previousMonth() {
        _monthStart.value = DateUtils.addMonths(_monthStart.value, -1)
    }

    fun nextMonth() {
        _monthStart.value = DateUtils.addMonths(_monthStart.value, 1)
    }

    fun addExpense(
        title: String,
        amount: Long,
        category: String,
        date: Long,
        recurring: Boolean,
        paid: Boolean,
        note: String,
        onDone: () -> Unit
    ) {
        if (title.isBlank() || amount <= 0L) return
        viewModelScope.launch {
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                val expenseId = dao.insert(
                    Expense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        isRecurring = recurring,
                        paid = paid,
                        note = note
                    )
                )

                // If paid, record a cash transaction for accounting/tally matching
                if (paid) {
                    db.paymentTransactionDao().insert(
                        com.app.muzzutech.data.model.PaymentTransaction(
                            paymentId = null,
                            expenseId = expenseId,
                            personType = "EXPENSE",
                            personMobile = "SHOP",
                            personName = category,
                            amount = amount,
                            paymentMode = "CASH",
                            note = "Paid: $title"
                        )
                    )
                }
            }
            onDone()
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                db.paymentTransactionDao().getTransactionByExpenseId(id)?.let { txn ->
                    db.paymentTransactionDao().delete(txn)
                }
                dao.deleteById(id)
            }
        }
    }

    fun togglePaid(expense: Expense) {
        viewModelScope.launch {
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                val newPaid = !expense.paid
                dao.update(expense.copy(paid = newPaid))
                if (newPaid) {
                    db.paymentTransactionDao().insert(
                        com.app.muzzutech.data.model.PaymentTransaction(
                            paymentId = null,
                            expenseId = expense.id,
                            personType = "EXPENSE",
                            personMobile = "SHOP",
                            personName = expense.category,
                            amount = expense.amount,
                            paymentMode = "CASH",
                            note = "Paid: ${expense.title}"
                        )
                    )
                } else {
                    db.paymentTransactionDao().getTransactionByExpenseId(expense.id)?.let { txn ->
                        db.paymentTransactionDao().delete(txn)
                    }
                }
            }
        }
    }
}
