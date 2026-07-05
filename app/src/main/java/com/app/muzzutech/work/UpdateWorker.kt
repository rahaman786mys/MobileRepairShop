package com.app.muzzutech.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.muzzutech.MainActivity
import com.app.muzzutech.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    return@withContext try {
      val repo = com.app.muzzutech.utils.update.UpdateRepository(applicationContext)
      val result = repo.fetchLatestVersion()
      var info = result.getOrNull()
      if (info == null) {
        val fromGithub = repo.fetchReleaseFromGitHubApi().getOrNull()
        if (fromGithub != null) info = fromGithub
      }
            if (info != null && info.versionCode > repo.getCurrentVersionCode()) {
                postNotification(info.versionName)
                repo.setLastCheckTimestamp(System.currentTimeMillis())
                Result.success()
            } else {
                repo.setLastCheckTimestamp(System.currentTimeMillis())
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun postNotification(versionName: String) {
        val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            mgr.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("show_update_dialog", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val primary = applicationContext.getColor(R.color.muzzu_primary)

        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Update Available — v$versionName")
            .setContentText("MuZZu Tech has a new update ready.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Tap to update now."))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(primary)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        mgr.notify(7701, notif)
    }
}
