package com.app.muzzutech.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _totalThisMonth = MutableStateFlow(0.0)
    val totalThisMonth: StateFlow<Double> = _totalThisMonth

    private val _categoryTotals = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryTotals: StateFlow<Map<String, Double>> = _categoryTotals

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
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _monthStart.value
            add(java.util.Calendar.MONTH, -1)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        _monthStart.value = cal.timeInMillis
    }
    fun nextMonth() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _monthStart.value
            add(java.util.Calendar.MONTH, 1)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        _monthStart.value = cal.timeInMillis
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        date: Long,
        recurring: Boolean,
        paid: Boolean,
        note: String,
        onDone: () -> Unit
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dao.insert(
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
            onDone()
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun togglePaid(expense: Expense) {
        viewModelScope.launch {
            dao.update(expense.copy(paid = !expense.paid))
        }
    }
}
