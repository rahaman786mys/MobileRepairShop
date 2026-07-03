package com.app.muzzutech.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.utils.AIAnalyzer
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Daily background worker that scans recent spare-part usage and posts a notification
 * for any part predicted to run out within the configured lead time.
 *
 * Runs once per day via [AppScheduler.enqueueDailyJobs]. Notification body lists the
 * parts needing reorder with suggested quantities.
 *
 * Channel id "reorder_alerts" is registered lazily on first emission.
 */
class ReorderAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = MobileRepairApp.instance.database
        val partDao = db.sparePartPurchaseDao()

        // Pull last 30 days of purchases to estimate usage.
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        val recent: List<SparePartPurchase> = partDao.getPurchasesByDateRange(thirtyDaysAgo, now).first()

        // Group by partName, count usage and pick top candidate.
        val byPart = recent.groupBy { it.partName }
        val predictions = byPart.map { (name, items) ->
            // currentStock = total purchased retained as proxy (no inventory entity yet)
            val stock = items.sumOf { it.quantity }
            AIAnalyzer.predictReorder(name, items.size, stock, leadTimeDays = 3)
        }.filter { it.shouldReorder }

        if (predictions.isEmpty()) return Result.success()

        val message = predictions.joinToString("\n") {
            "• ${it.partName}: ${it.daysUntilStockout}d left, order ~${it.suggestedOrderQuantity}"
        }
        sendNotification(applicationContext, "Reorder reminder: ${predictions.size} parts", message)
        return Result.success()
    }

    private fun sendNotification(context: Context, title: String, body: String) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reorder Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily inventory reorder reminders" }
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
        private const val CHANNEL_ID = "reorder_alerts"
        private const val NOTIF_ID = 4201
    }
}
