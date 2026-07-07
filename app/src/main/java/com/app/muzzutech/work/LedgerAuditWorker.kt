package com.app.muzzutech.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.LedgerAlert
import com.app.muzzutech.utils.PriceUtils
import kotlinx.coroutines.flow.first

class LedgerAuditWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = MobileRepairApp.instance.database
        val paymentDao = db.paymentDao()
        val txnDao = db.paymentTransactionDao()
        val expenseDao = db.expenseDao()
        val alertDao = db.ledgerAlertDao()

        val todayStart = getTodayStart()
        val todayEnd = todayStart + 24L * 60 * 60 * 1000
        val alerts = mutableListOf<LedgerAlert>()

        // 1. Check every Payment: sum of linked transactions should equal paidAmount
        val allPayments = paymentDao.getAllPayments().first()
        for (payment in allPayments) {
            val linkedTxns = txnDao.getTransactionsByPayment(payment.id).first()
            val sumTxn = linkedTxns.sumOf { it.amount }
            val diff = kotlin.math.abs(sumTxn - payment.paidAmount)
            if (diff != 0L && payment.paidAmount > 0L) {
                alerts.add(
                    LedgerAlert(
                        type = "PAYMENT_MISMATCH",
                        description = "Payment #${payment.id} (${payment.personName}, ${payment.personType}): " +
                                "paidAmount=${payment.paidAmount} but txn sum=$sumTxn (diff=$diff)",
                        expectedAmount = payment.paidAmount,
                        actualAmount = sumTxn,
                        mismatchAmount = diff
                    )
                )
            }
        }

        // 2. Check paid Expenses in the last 30 days: linked txn must exist AND sum(amount) must match expense.amount
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recentExpenses = expenseDao.getByDateRange(thirtyDaysAgo, System.currentTimeMillis()).first()
        for (expense in recentExpenses.filter { it.paid }) {
            val allLinked = txnDao.getAllTransactions().first().filter { it.expenseId == expense.id }
            val sumTxn = allLinked.sumOf { it.amount }
            val diff = kotlin.math.abs(sumTxn - expense.amount)
            when {
                allLinked.isEmpty() -> {
                    alerts.add(
                        LedgerAlert(
                            type = "EXPENSE_MISMATCH",
                            description = "Expense #${expense.id} '${expense.title}' (${PriceUtils.formatPrice(expense.amount)}) is PAID but has no PaymentTransaction",
                            expectedAmount = expense.amount,
                            actualAmount = 0L,
                            mismatchAmount = expense.amount
                        )
                    )
                }
                diff != 0L -> {
                    alerts.add(
                        LedgerAlert(
                            type = "EXPENSE_MISMATCH",
                            description = "Expense #${expense.id} '${expense.title}': amount=${expense.amount} but sum(linked txns)=$sumTxn (diff=$diff)",
                            expectedAmount = expense.amount,
                            actualAmount = sumTxn,
                            mismatchAmount = diff
                        )
                    )
                }
            }
        }

        // 3. Check for recent orphan transactions — only those whose personType
        //    implies a parent record exists. Advances (CUSTOMER/DEALER with no paymentId)
        //    are legitimate; EXPENSE without expenseId or SALARY without paymentId are bugs.
        val allTxns = txnDao.getTransactionsByDateRange(todayStart, todayEnd).first()
        val orphans = allTxns.filter {
            (it.personType == "EXPENSE" && it.expenseId == null) ||
            (it.personType == "SALARY" && it.expenseId == null)
        }
        for (txn in orphans) {
            alerts.add(
                LedgerAlert(
                    type = "ORPHAN_TRANSACTION",
                    description = "Txn #${txn.id} ${txn.personType}/${txn.personName}: ${PriceUtils.formatPrice(txn.amount)} " +
                            "(${txn.note}) is missing its parent ${if (txn.personType == "EXPENSE") "Expense" else "SalaryPayment"}",
                    expectedAmount = 0L,
                    actualAmount = txn.amount,
                    mismatchAmount = txn.amount
                )
            )
        }

        // Persist alerts
        for (alert in alerts) {
            alertDao.insert(alert)
        }

        // Notify user if any mismatch found
        val unresolved = alertDao.countUnresolved()
        if (unresolved > 0) {
            sendNotification(
                applicationContext,
                "Ledger Alert: $unresolved mismatch${if (unresolved > 1) "es" else ""}",
                "Nightly audit found $unresolved issue${if (unresolved > 1) "s" else ""}. Open Reports to review."
            )
        }

        return Result.success()
    }

    private fun getTodayStart(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun sendNotification(context: Context, title: String, body: String) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ledger Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Nightly ledger audit mismatch alerts" }
            mgr.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "ledger_alerts"
        private const val NOTIF_ID = 4203
    }
}
