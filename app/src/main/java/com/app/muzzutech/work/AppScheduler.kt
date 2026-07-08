package com.app.muzzutech.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.muzzutech.work.UpdateWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Centralized scheduler for background jobs.
 *
 *  - Daily reorder alert: runs once per day (battery-not-low constraint).
 *  - Monthly salary reminder: runs once on the 1st of every month.
 *
 * Workers are idempotent — re-enqueueing on app startup uses KEEP policy.
 */
object AppScheduler {

    private const val WORK_REORDER_DAILY = "reorder_daily_alert"
    private const val WORK_SALARY_MONTHLY = "salary_monthly_reminder"
    private const val WORK_LEDGER_AUDIT = "ledger_daily_audit"
    private const val WORK_APP_UPDATE = "app_update_check"

    fun enqueueDailyJobs(context: Context) {
        val wm = WorkManager.getInstance(context)

        // Reorder reminder — runs every ~24h (WorkManager enforces min interval 15min).
        val reorderRequest = PeriodicWorkRequestBuilder<ReorderAlertWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(15, TimeUnit.MINUTES) // give app a moment to settle on first launch
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_REORDER_DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            reorderRequest
        )

        // Monthly salary reminder — runs once ~every 30 days. WorkManager doesn't have a
        // "1st of month" constraint, so we approximate by computing time-to-next-1st-of-month.
        val initialDelayMinutes = computeMinutesUntilNextMonthFirst()
        val salaryRequest = PeriodicWorkRequestBuilder<SalaryReminderWorker>(30, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_SALARY_MONTHLY,
            ExistingPeriodicWorkPolicy.KEEP,
            salaryRequest
        )

        // App update check — runs once per day
        val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(60, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_APP_UPDATE,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )

        // Nightly ledger audit — runs once per day, reconciles books
        val auditRequest = PeriodicWorkRequestBuilder<LedgerAuditWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(30, TimeUnit.MINUTES) // ample settling time after app install
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_LEDGER_AUDIT,
            ExistingPeriodicWorkPolicy.KEEP,
            auditRequest
        )
    }

    private fun computeMinutesUntilNextMonthFirst(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = next.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMs.coerceAtLeast(0))
    }
}
