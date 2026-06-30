package com.app.muzzutech.utils

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.IOException

/**
 * Advanced In-App Update Manager
 * Checks GitHub for updates, downloads the APK, and triggers installation
 */
object UpdateManager {

    private const val VERSION_URL = "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
    private val client = OkHttpClient()
    private var downloadId: Long = -1

    fun checkForUpdates(context: Context, onNoUpdate: (() -> Unit)? = null) {
        val request = Request.Builder().url(VERSION_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onNoUpdate?.invoke()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        val versionInfo = Gson().fromJson(body, VersionInfo::class.java)
                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            packageInfo.versionCode.toLong()
                        }
                        
                        if (versionInfo.versionCode > currentVersion) {
                            (context as? android.app.Activity)?.runOnUiThread {
                                showUpdateDialog(context, versionInfo)
                            }
                        } else {
                            onNoUpdate?.invoke()
                        }
                    } catch (e: Exception) {
                        onNoUpdate?.invoke()
                    }
                } else {
                    onNoUpdate?.invoke()
                }
            }
        })
    }

    private fun showUpdateDialog(context: Context, info: VersionInfo) {
        AlertDialog.Builder(context)
            .setTitle("New Update v${info.versionName}")
            .setMessage("What's New:\n${info.releaseNotes}\n\nDownload and install now?")
            .setPositiveButton("Update Now") { _, _ ->
                startDownload(context, info.downloadUrl)
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    private fun startDownload(context: Context, url: String) {
        // Cleanup old downloads
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null) {
            val oldFile = File(downloadsDir, "repair_shop_update.apk")
            if (oldFile.exists()) oldFile.delete()
        }

        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Repair Shop Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "repair_shop_update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId)

        if (uri != null) {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            try {
                context.startActivity(installIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)
