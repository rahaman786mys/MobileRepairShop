package com.app.muzzutech.utils

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.app.muzzutech.R
import com.app.muzzutech.ui.update.UpdateBottomSheet
import com.app.muzzutech.utils.update.UpdateRepository
import com.app.muzzutech.utils.update.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UpdateManager {

  private const val UPDATE_CHANNEL_ID = "app_updates"
  private const val NOTIF_ID_UPDATE = 7701

  fun checkForUpdates(activity: AppCompatActivity) {
    CoroutineScope(Dispatchers.IO).launch {
      val prefs = UpdateRepository(activity)
      val result = prefs.fetchLatestVersion()
      var info = result.getOrNull()
      if (info == null) {
        info = prefs.fetchReleaseFromGitHubApi().getOrNull()
      }
      prefs.setLastCheckTimestamp(System.currentTimeMillis())

      if (info == null) return@launch
      val currentCode = prefs.getCurrentVersionCode()
      if (info.versionCode <= currentCode) return@launch
      val snoozed = prefs.getSnoozedVersion()
      if (info.versionCode == snoozed) return@launch

      activity.runOnUiThread {
        UpdateBottomSheet.newInstance(
            versionName = info.versionName,
            currentVersionName = prefs.getCurrentVersionName(),
            releaseNotes = info.releaseNotes,
            sizeBytes = info.sizeBytes,
            downloadUrl = info.downloadUrl,
            versionCode = info.versionCode,
          )
          .also { sheet ->
            sheet.show(activity.supportFragmentManager, "update_sheet")
          }
      }
    }
  }

  fun handleNotificationIntent(activity: AppCompatActivity, intent: Intent) {
    CoroutineScope(Dispatchers.IO).launch {
      val prefs = UpdateRepository(activity)
      val result = prefs.fetchLatestVersion()
      var info = result.getOrNull()
      if (info == null) {
        info = prefs.fetchReleaseFromGitHubApi().getOrNull()
      }
      if (info != null && info.versionCode > prefs.getCurrentVersionCode()) {
        activity.runOnUiThread {
          UpdateBottomSheet.newInstance(
              versionName = info.versionName,
              currentVersionName = prefs.getCurrentVersionName(),
              releaseNotes = info.releaseNotes,
              sizeBytes = info.sizeBytes,
              downloadUrl = info.downloadUrl,
              versionCode = info.versionCode,
            )
            .also { sheet ->
              sheet.show(activity.supportFragmentManager, "update_sheet")
            }
        }
      }
    }
  }

  fun downloadAndInstall(
    context: Context,
    url: String,
    onProgress: (Int, String) -> Unit,
    onComplete: () -> Unit,
    onFailed: (String) -> Unit,
  ) {
    val request = Request.Builder().url(url).build()
    val client =
      OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null)
        .hostnameVerifier { host, _ ->
          host == "raw.githubusercontent.com" || host == "github.com"
        }
        .build()

    val apkFile = File(context.cacheDir, "repair_shop_update_${System.currentTimeMillis()}.apk")
    if (apkFile.exists()) apkFile.delete()

    client.newCall(request).enqueue(
      object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
          CoroutineScope(Dispatchers.Main).launch {
            onFailed(e.message ?: context.getString(R.string.download_failed, "network error"))
          }
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
          val body = response.body ?: run {
            CoroutineScope(Dispatchers.Main).launch {
              onFailed(context.getString(R.string.empty_response))
            }
            response.close()
            return
          }
          val totalBytes = body.contentLength()
          var downloadedBytes: Long = 0
          try {
            body.byteStream().use { input ->
              FileOutputStream(apkFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                  fos.write(buffer, 0, bytesRead)
                  downloadedBytes += bytesRead
                  if (totalBytes > 0) {
                    val pct = ((downloadedBytes * 100) / totalBytes).toInt()
                    val mbStr =
                      String.format(
                        java.util.Locale.getDefault(),
                        "%.1f / %.1f MB",
                        downloadedBytes / (1024.0 * 1024.0),
                        totalBytes / (1024.0 * 1024.0),
                      )
                    CoroutineScope(Dispatchers.Main).launch { onProgress(pct, mbStr) }
                  }
                }
                fos.flush()
              }
            }
            CoroutineScope(Dispatchers.Main).launch { onComplete() }
          } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
              onFailed(e.message ?: context.getString(R.string.download_failed, "io error"))
            }
          } finally {
            response.close()
          }
        }
      },
    )
  }

  fun installApk(context: Context, apkFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (!context.packageManager.canRequestPackageInstalls()) {
        val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
          data = Uri.parse("package:${context.packageName}")
        }
        if (context is Activity) {
          context.startActivityForResult(settingsIntent, 9001)
        } else {
          context.startActivity(settingsIntent)
        }
        Toast.makeText(context, R.string.enable_install_settings, Toast.LENGTH_LONG).show()
        return
      }
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(
          context,
          context.getString(R.string.installation_failed, e.message),
          Toast.LENGTH_LONG,
        )
        .show()
    }
  }

  fun postUpdateNotification(context: Context, versionName: String) {
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          UPDATE_CHANNEL_ID,
          "App Updates",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "New app version available" }
      mgr.createNotificationChannel(channel)
    }
    val intent = Intent(context, com.app.muzzutech.MainActivity::class.java).apply {
      putExtra("show_update_dialog", true)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val pi =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notif =
      NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Update Available — v$versionName")
        .setContentText("MuZZu Tech has a new update ready.")
        .setStyle(NotificationCompat.BigTextStyle().bigText("Tap to update now."))
        .setContentIntent(pi)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    mgr.notify(NOTIF_ID_UPDATE, notif)
  }
}
