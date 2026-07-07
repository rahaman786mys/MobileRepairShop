package com.app.muzzutech.ui.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.Attendance
import com.app.muzzutech.data.model.SalaryPayment
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

/**
 * Pure-Kotlin payroll logic — kept in an object for unit testing without Android.
 */
object PayrollMath {

    /** Compute worked-days weight: half-day counts 0.5, full-present 1.0, absent 0. */
    fun computeWorkedDays(fullPresent: Int, halfPresent: Int, absent: Int): Double {
        require(fullPresent >= 0 && halfPresent >= 0 && absent >= 0) { "negative counts invalid" }
        return fullPresent + halfPresent * 0.5
    }

    /**
     * Compute payable salary for a tech given worked days.
     *
     * Rule (per product owner):
     *  - If monthlySalary > 0, use monthlySalary + (extra overtime not implemented).
     *    Actually the contract is: per-day-rate × worked-days, with per-day rate
     *    derived from monthlySalary / 30 if explicit perDaySalary is 0.
     *  - If only perDaySalary is set (and monthly is 0), use perDaySalary × workedDays.
     */
    fun computePayable(monthlySalary: Long, perDaySalary: Long, workedDays: Double): Long {
        val effectivePerDay = when {
            perDaySalary > 0L -> perDaySalary.toDouble()
            monthlySalary > 0L -> monthlySalary.toDouble() / 30.0
            else -> 0.0
        }
        return (effectivePerDay * workedDays).roundToLong()
    }

    /** Build a SalaryPayment snapshot row. */
    fun buildSalaryPayment(
        smId: Long,
        smName: String,
        monthStart: Long,
        workedDays: Double,
        monthlySalary: Long,
        perDaySalary: Long,
        paidAmount: Long,
        note: String = ""
    ): SalaryPayment {
        val computed = computePayable(monthlySalary, perDaySalary, workedDays)
        val due = (computed - paidAmount).coerceAtLeast(0L)
        val status = when {
            paidAmount <= 0L -> "UNPAID"
            paidAmount >= computed -> "PAID"
            else -> "PARTIAL"
        }
        return SalaryPayment(
            servicemanId = smId,
            servicemanName = smName,
            monthStart = monthStart,
            daysWorked = workedDays,
            perDaySalary = if (perDaySalary > 0L) perDaySalary else (monthlySalary.toDouble() / 30.0).roundToLong(),
            fixedMonthlySalary = monthlySalary,
            computedAmount = computed,
            paidAmount = paidAmount,
            dueAmount = due,
            status = status,
            note = note
        )
    }
}

/**
 * ViewModel for the Payroll screens (attendance + salary summary).
 *
 * State:
 *  - [viewMode] = month overview vs daily attendance
 *  - [monthStart] = the displayed month's first-millisecond
 *  - [servicemen] = list of active techs
 *  - [attendanceBySm] = per-tech attendance counts for the month (full / half / absent)
 *  - [salaryPayments] = persisted salary slips for the month, keyed by servicemanId
 */
class PayrollViewModel : ViewModel() {

    private val db: AppDatabase = MobileRepairApp.instance.database
    private val smDao = db.serviceManDao()
    private val attendanceDao = db.attendanceDao()
    private val salaryDao = db.salaryDao()

    private val _monthStart = MutableStateFlow(DateUtils.getStartOfMonth())
    val monthStart: StateFlow<Long> = _monthStart

    private val _servicemen = MutableStateFlow<List<ServiceMan>>(emptyList())
    val servicemen: StateFlow<List<ServiceMan>> = _servicemen

    private val _monthStats = MutableStateFlow<Map<Long, MonthStats>>(emptyMap())
    val monthStats: StateFlow<Map<Long, MonthStats>> = _monthStats

    private val _salaryPayments = MutableStateFlow<Map<Long, SalaryPayment>>(emptyMap())
    val salaryPayments: StateFlow<Map<Long, SalaryPayment>> = _salaryPayments

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    data class MonthStats(
        val smId: Long,
        val presentFull: Int = 0,
        val presentHalf: Int = 0,
        val absent: Int = 0,
        val workedDays: Double = 0.0
    )

    init {
        loadServicemen()
        observeMonth()
    }

    fun selectMonth(monthStart: Long) {
        _monthStart.value = monthStart
    }

    fun previousMonth() {
        _monthStart.value = DateUtils.addMonths(_monthStart.value, -1)
    }

    fun nextMonth() {
        _monthStart.value = DateUtils.addMonths(_monthStart.value, 1)
    }

    private fun loadServicemen() {
        viewModelScope.launch {
            smDao.getAllServiceMen().collect { list ->
                _servicemen.value = list.filter { it.isActive || list.all { !it.isActive } }
                // Kick off month refresh now that servicemen data is available
                val ms = _monthStart.value
                refreshMonthStats(ms, DateUtils.getEndOfMonth(ms))
            }
        }
    }

    private fun observeMonth() {
        viewModelScope.launch {
            _monthStart.collect { monthStart ->
                val monthEnd = DateUtils.getEndOfMonth(monthStart)
                // Only refresh if servicemen list is already loaded (guard against race)
                if (_servicemen.value.isNotEmpty()) {
                    refreshMonthStats(monthStart, monthEnd)
                }
                refreshSalaryPayments(monthStart)
            }
        }
    }

    private fun refreshMonthStats(monthStart: Long, monthEnd: Long) {
        viewModelScope.launch {
            val list = _servicemen.value
            val stats = mutableMapOf<Long, MonthStats>()
            for (sm in list) {
                val full = withContext(Dispatchers.IO) { attendanceDao.getFullPresentDays(sm.id, monthStart, monthEnd) }
                val half = withContext(Dispatchers.IO) { attendanceDao.getHalfPresentDays(sm.id, monthStart, monthEnd) }
                val all = withContext(Dispatchers.IO) { attendanceDao.getByServiceManInRangeList(sm.id, monthStart, monthEnd) }
                val totalDays = all.size
                val absent = totalDays - full - half
                val worked = PayrollMath.computeWorkedDays(full, half, absent.coerceAtLeast(0))
                stats[sm.id] = MonthStats(sm.id, full, half, absent.coerceAtLeast(0), worked)
            }
            _monthStats.value = stats
        }
    }

    private fun refreshSalaryPayments(monthStart: Long) {
        viewModelScope.launch {
            val end = DateUtils.getEndOfMonth(monthStart)
            salaryDao.getByMonth(monthStart, end).collect { payments ->
                val map = payments.associateBy { it.servicemanId }
                _salaryPayments.value = map
            }
        }
    }

    /** Toggle attendance for a service man on a specific day (or today if date=0). */
    fun setAttendance(smId: Long, day: Long, present: Boolean, halfDay: Boolean = false, note: String = "") {
        val date = if (day <= 0) DateUtils.getStartOfDay() else DateUtils.getStartOfDay(day)
        viewModelScope.launch {
            _busy.value = true
            try {
                attendanceDao.upsert(
                    Attendance(
                        servicemanId = smId,
                        date = date,
                        present = present,
                        halfDay = halfDay,
                        note = note
                    )
                )
                // refresh stats
                val ms = _monthStart.value
                refreshMonthStats(ms, DateUtils.getEndOfMonth(ms))
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Generate or refresh the salary slip for a serviceman for the current month.
     * Computes worked days from attendance and snapshots the per-day/monthly salary
     * from the current ServiceMan settings.
     */
    fun generateOrUpdateSalary(smId: Long, paidAmount: Long, note: String = "") {
        viewModelScope.launch {
            _busy.value = true
            try {
                db.withTransaction {
                    val sm = smDao.getServiceManById(smId) ?: return@withTransaction
                    val stats = _monthStats.value[smId] ?: return@withTransaction
                    val monthStart = _monthStart.value
                    val slip = PayrollMath.buildSalaryPayment(
                        smId = sm.id,
                        smName = sm.name,
                        monthStart = monthStart,
                        workedDays = stats.workedDays,
                        monthlySalary = sm.monthlySalary,
                        perDaySalary = sm.perDaySalary,
                        paidAmount = paidAmount,
                        note = note
                    )
                    val existing = salaryDao.getByServiceManAndMonth(smId, monthStart)
                    val slipToSave = if (existing != null) slip.copy(id = existing.id) else slip
                    val salaryId = salaryDao.insert(slipToSave)

                    // Only remove old accounting rows for this specific serviceman when creating a new payment.
                    // If paidAmount == 0 we are only refreshing the slip — never destroy audit history.
                    if (paidAmount > 0L) {
                        val monthEnd = DateUtils.getEndOfMonth(monthStart)
                        val oldExpenses = db.expenseDao().getByDateRange(monthStart, monthEnd).first().filter {
                            it.category == com.app.muzzutech.data.model.Expense.CATEGORY_SALARY &&
                            it.title == "Salary: ${sm.name}"
                        }
                        for (oldExp in oldExpenses) {
                            db.paymentTransactionDao().getTransactionByExpenseId(oldExp.id)?.let { txn ->
                                db.paymentTransactionDao().delete(txn)
                            }
                            db.expenseDao().deleteById(oldExp.id)
                        }

                        val expenseId = db.expenseDao().insert(
                            com.app.muzzutech.data.model.Expense(
                                title = "Salary: ${sm.name}",
                                amount = paidAmount,
                                category = com.app.muzzutech.data.model.Expense.CATEGORY_SALARY,
                                date = System.currentTimeMillis(),
                                paid = true,
                                note = "Salary for ${DateUtils.formatDateTime(monthStart)}",
                                salaryPaymentId = salaryId
                            )
                        )
                        db.paymentTransactionDao().insert(
                            com.app.muzzutech.data.model.PaymentTransaction(
                                paymentId = null,
                                expenseId = expenseId,
                                salaryPaymentId = salaryId,
                                personType = "SALARY",
                                personMobile = sm.mobile,
                                personName = sm.name,
                                amount = paidAmount,
                                paymentMode = "CASH",
                                note = "Salary: ${DateUtils.formatDateTime(monthStart)}"
                            )
                        )
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }
}
