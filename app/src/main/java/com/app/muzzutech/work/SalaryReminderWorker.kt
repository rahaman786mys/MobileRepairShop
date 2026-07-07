package com.app.muzzutech.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils
import kotlinx.coroutines.flow.first

/**
 * Monthly salary reminder worker. Runs at the start of each calendar month and notifies
 * the shop owner about pending salary slips. Body lists per-technician worked-days +
 * computed payable amount for last month, plus total due.
 *
 * Channel id "salary_reminders" is registered lazily on first emission.
 */
class SalaryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = MobileRepairApp.instance.database
        val salaryDao = db.salaryDao()

        val lastMonthRef = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val monthStart = DateUtils.getStartOfMonth(lastMonthRef)
        val monthEnd = DateUtils.getEndOfMonth(lastMonthRef)

        val slips = salaryDao.getByMonth(monthStart, monthEnd).first()
        val pending = slips.filter { it.status != "PAID" }
        if (pending.isEmpty()) return Result.success()

        val totalDue = pending.sumOf { it.dueAmount }
        val body = pending.joinToString("\n") {
            "• ${it.servicemanName}: ${PriceUtils.formatPrice(it.dueAmount)} (${it.status})"
        } + "\n\nTotal due: ${PriceUtils.formatPrice(totalDue)}"

        sendNotification(
            applicationContext,
            "Salary reminder: ${pending.size} pending slip(s)",
            body
        )
        return Result.success()
    }

    private fun sendNotification(context: Context, title: String, body: String) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Salary Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Monthly salary payout reminders" }
            mgr.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "salary_reminders"
        private const val NOTIF_ID = 4202
    }
}
