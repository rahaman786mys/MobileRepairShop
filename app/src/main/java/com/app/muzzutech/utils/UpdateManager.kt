package com.app.muzzutech.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.app.muzzutech.BuildConfig
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.lifecycle.Observer
import com.app.muzzutech.R
import com.app.muzzutech.ui.update.UpdateBottomSheet
import com.app.muzzutech.utils.update.PlayUpdateHelper
import com.app.muzzutech.utils.update.UpdateRepository
import com.app.muzzutech.utils.update.VersionInfo
import com.app.muzzutech.work.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

  private const val UPDATE_CHANNEL_ID = "app_updates"
  private const val NOTIF_ID_UPDATE = 7701
  private const val TAG = "UpdateManager"
  private var downloadFile: java.io.File? = null

  fun checkForUpdates(activity: AppCompatActivity) {
    PlayUpdateHelper.tryImmediateUpdate(activity) { checkFallbackForUpdates(activity) }
  }

  private fun checkFallbackForUpdates(activity: AppCompatActivity) {
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
    PlayUpdateHelper.tryImmediateUpdate(activity) { showFallbackUpdateFromNotification(activity) }
  }

  private fun showFallbackUpdateFromNotification(activity: AppCompatActivity) {
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
    val appContext = context.applicationContext
    val work =
      OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(Data.Builder().putString(DownloadWorker.KEY_URL, url).build())
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(appContext).enqueue(work)
    val workManager = WorkManager.getInstance(appContext)
    val liveData = workManager.getWorkInfoByIdLiveData(work.id)
    lateinit var observer: Observer<WorkInfo>
    observer = Observer { info ->
      if (info == null) return@Observer
      val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
      val progressText = info.progress.getString(DownloadWorker.KEY_PROGRESS_TEXT).orEmpty()
      if (progress > 0) onProgress(progress, progressText)
      if (info.state.isFinished) {
        liveData.removeObserver(observer)
        if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
          val path = info.outputData.getString(DownloadWorker.KEY_FILE_PATH)
          if (path != null) {
            val file = File(path)
            downloadFile = file
            onComplete(file)
          } else {
            onFailed(context.getString(R.string.download_failed, "missing file"))
          }
        } else {
          onFailed(context.getString(R.string.download_failed, info.state.name.lowercase()))
        }
      }
    }
    liveData.observeForever(observer)
  }

  fun installApk(context: Context, apkFile: File, launcher: androidx.activity.result.ActivityResultLauncher<Intent>? = null) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
      data = uri
      putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
      putExtra(Intent.EXTRA_RETURN_RESULT, true)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      launcher?.launch(intent) ?: context.startActivity(intent)
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
