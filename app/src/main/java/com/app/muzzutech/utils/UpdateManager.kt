package com.app.muzzutech.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.app.muzzutech.R
import com.app.muzzutech.ui.update.UpdateBottomSheet
import com.app.muzzutech.utils.update.PlayUpdateHelper
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
import java.util.Locale

object UpdateManager {

  private const val UPDATE_CHANNEL_ID = "app_updates"
  private const val NOTIF_ID_UPDATE = 7701
  private const val TAG = "UpdateManager"
  private var downloadFile: java.io.File? = null

  fun checkForUpdates(activity: AppCompatActivity) {
    // Try Play In-App Updates first (for Play Store distribution)
    if (PlayUpdateHelper.tryImmediateUpdate(activity)) {
      Log.i(TAG, "checkForUpdates: Play In-App Updates triggered")
      return
    }
    Log.d(TAG, "checkForUpdates: starting update check, currentVersionCode=${UpdateRepository(activity).getCurrentVersionCode()}")
    CoroutineScope(Dispatchers.IO).launch {
      val prefs = UpdateRepository(activity)
      // Prioritize GitHub Releases API for the real source of truth
      val result = prefs.fetchReleaseFromGitHubApi()
      var info = result.getOrNull()
      Log.d(TAG, "checkForUpdates: GitHub API result=${result.isSuccess} info=$info")
      
      if (info == null) {
        Log.w(TAG, "checkForUpdates: GitHub API null, trying version.json fallback")
        info = prefs.fetchLatestVersion().getOrNull()
        Log.d(TAG, "checkForUpdates: version.json fallback=$info")
      }
      prefs.setLastCheckTimestamp(System.currentTimeMillis())

      if (info == null) {
        Log.w(TAG, "checkForUpdates: no version info from any source, aborting")
        return@launch
      }
      val currentCode = prefs.getCurrentVersionCode()
      Log.d(TAG, "checkForUpdates: remote=$info remoteVersionCode=${info.versionCode} localVersionCode=$currentCode")
      if (info.versionCode <= currentCode) {
        Log.i(TAG, "checkForUpdates: already up to date ($currentCode >= ${info.versionCode}), skipping")
        return@launch
      }
      val snoozed = prefs.getSnoozedVersion()
      Log.d(TAG, "checkForUpdates: snoozedVersion=$snoozed")
      if (info.versionCode == snoozed) {
        Log.i(TAG, "checkForUpdates: version ${info.versionCode} was snoozed, skipping")
        return@launch
      }

      activity.runOnUiThread {
        Log.i(TAG, "checkForUpdates: showing update dialog ${info.versionName}")
        val sheet = UpdateBottomSheet.newInstance(
          versionName = info.versionName,
          currentVersionName = prefs.getCurrentVersionName(),
          releaseNotes = info.releaseNotes,
          sizeBytes = info.sizeBytes,
          downloadUrl = info.downloadUrl,
          versionCode = info.versionCode,
          forceUpdate = info.forceUpdate
        )
        sheet.show(activity.supportFragmentManager, "update_sheet")
      }
    }
  }

  fun handleNotificationIntent(activity: AppCompatActivity, intent: Intent) {
    Log.d(TAG, "handleNotificationIntent: processing notification tap")
    if (PlayUpdateHelper.tryImmediateUpdate(activity)) return
    CoroutineScope(Dispatchers.IO).launch {
      val prefs = UpdateRepository(activity)
      var result = prefs.fetchReleaseFromGitHubApi()
      var info = result.getOrNull()
      if (info == null) {
        info = prefs.fetchLatestVersion().getOrNull()
      }
      if (info != null && info.versionCode > prefs.getCurrentVersionCode()) {
        activity.runOnUiThread {
          val sheet = UpdateBottomSheet.newInstance(
            versionName = info.versionName,
            currentVersionName = prefs.getCurrentVersionName(),
            releaseNotes = info.releaseNotes,
            sizeBytes = info.sizeBytes,
            downloadUrl = info.downloadUrl,
            versionCode = info.versionCode,
            forceUpdate = info.forceUpdate
          )
          sheet.show(activity.supportFragmentManager, "update_sheet")
        }
      } else {
        Log.i(TAG, "handleNotificationIntent: no update available (info=$info current=${prefs.getCurrentVersionCode()})")
      }
    }
  }

  fun setSnoozedVersion(versionCode: Int) {
    UpdateRepository(appInstance()).setSnoozedVersion(versionCode)
  }

  fun getSnoozedVersion(): Int {
    return UpdateRepository(appInstance()).getSnoozedVersion()
  }

  private fun appInstance(): Context {
    return try {
      Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as Context
    } catch (e: Exception) {
      throw RuntimeException("Cannot get Application instance", e)
    }
  }

  fun downloadAndInstall(
    context: Context,
    url: String,
    onProgress: (Int, String) -> Unit,
    onComplete: (File) -> Unit,
    onFailed: (String) -> Unit,
  ) {
    val updateDir = File(context.filesDir, "updates")
    if (!updateDir.exists()) updateDir.mkdirs()
    val apkFile = File(updateDir, "update.apk")
    if (apkFile.exists()) apkFile.delete()

    val request = Request.Builder().url(url).build()
    val client =
      OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null)
        .build()

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
                        Locale.getDefault(),
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
            downloadFile = apkFile
            CoroutineScope(Dispatchers.Main).launch { onComplete(apkFile) }
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

  fun installApk(context: Context, apkFile: File, launcher: androidx.activity.result.ActivityResultLauncher<Intent>? = null) {
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
        if (launcher != null) {
          launcher.launch(settingsIntent)
        } else if (context is android.app.Activity) {
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
        .setContentTitle("Update Available \u2014 v$versionName")
        .setContentText("MuZZu Tech has a new update ready.")
        .setStyle(NotificationCompat.BigTextStyle().bigText("Tap to update now."))
        .setContentIntent(pi)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    mgr.notify(NOTIF_ID_UPDATE, notif)
  }
}
