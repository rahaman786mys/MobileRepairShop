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
    Log.d(TAG, "checkForUpdates: starting update check")
    CoroutineScope(Dispatchers.IO).launch {
      val prefs = UpdateRepository(activity)
      
      // FORCED PRIORITY: Always check version.json on master branch first for "Suddenly" updates
      val result = prefs.fetchLatestVersion() 
      var info = result.getOrNull()
      
      if (info == null) {
        Log.w(TAG, "checkForUpdates: version.json failed, trying GitHub Releases")
        info = prefs.fetchReleaseFromGitHubApi().getOrNull()
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
        com.app.muzzutech.utils.update.UpdateState.setAvailableUpdate(null)
        return@launch
      }
      
      com.app.muzzutech.utils.update.UpdateState.setAvailableUpdate(info)
      
      val snoozed = prefs.getSnoozedVersion()
      Log.d(TAG, "checkForUpdates: snoozedVersion=$snoozed")
      
      val sessionSnoozed = com.app.muzzutech.utils.update.UpdateState.isSnoozed.value
      
      if (info.versionCode == snoozed || sessionSnoozed) {
        Log.i(TAG, "checkForUpdates: version ${info.versionCode} was snoozed, skipping dialog")
        return@launch
      }

      activity.runOnUiThread {
        showUpdateDialog(activity, info)
      }
    }
  }

  fun showUpdateDialog(activity: AppCompatActivity, info: VersionInfo) {
    val prefs = UpdateRepository(activity)
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
    if (!apkFile.exists()) {
        Toast.makeText(context, "Update file not found", Toast.LENGTH_SHORT).show()
        return
    }

    // Check for "Install Unknown Apps" permission on Android 8.0+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, "Please allow 'Install Unknown Apps' for MuZZu Tech", Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    
    // Modern installation intent (works better on API 30+)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    
    // Explicitly target the system package installer to avoid any "ambiguous intent" errors
    // Note: On some devices, there might be multiple installers, but let's try this first
    // Actually, setting the type should be enough for the system to pick the right one.
    
    try {
      launcher?.launch(intent) ?: context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Installation failed", e)
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
