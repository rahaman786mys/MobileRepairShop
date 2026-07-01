package com.app.muzzutech.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.app.muzzutech.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val VERSION_URL = "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null)
        .build()
        
    private var skippedVersionCode: Int = -1

    fun checkForUpdates(context: Context, onNoUpdate: (() -> Unit)? = null) {
        val urlWithCacheBuster = "$VERSION_URL?t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(urlWithCacheBuster)
            .header("Cache-Control", "no-cache")
            .build()

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
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        }
                        val currentVersionName = packageInfo.versionName ?: "?"

                        if (versionInfo.versionCode > currentVersion && versionInfo.versionCode != skippedVersionCode) {
                            (context as? Activity)?.runOnUiThread {
                                showUpdateDialog(context, versionInfo, currentVersionName)
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

    private fun showUpdateDialog(context: Context, info: VersionInfo, currentVersionName: String) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) return

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.update_available)
            .setMessage("v$currentVersionName → v${info.versionName}\n\n${info.releaseNotes}")
            .setPositiveButton(R.string.download) { _, _ ->
                downloadApk(context, info.downloadUrl)
            }
            .setNegativeButton(R.string.later) { _, _ ->
                skippedVersionCode = info.versionCode
            }
            .setCancelable(false)
            .show()
    }

    private fun downloadApk(context: Context, url: String) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null)
        val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progressBar)
        val tvPercent = dialogView.findViewById<TextView>(R.id.tvProgressPercent)
        val tvBytes = dialogView.findViewById<TextView>(R.id.tvProgressBytes)

        val progressDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.show()

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir == null) {
            progressDialog.dismiss()
            Toast.makeText(context, R.string.storage_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val apkFile = File(downloadsDir, "repair_shop_update.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                context.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, context.getString(R.string.download_failed, e.message), Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body ?: run {
                    context.runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(context, R.string.empty_response, Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                try {
                    val fos = FileOutputStream(apkFile)
                    val inputStream = body.byteStream()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            context.runOnUiThread {
                                progressBar.progress = progress
                                tvPercent.text = String.format(Locale.getDefault(), "%d%%", progress)
                                tvBytes.text = String.format(
                                    Locale.getDefault(),
                                    "%.1fMB / %.1fMB",
                                    downloadedBytes / (1024.0 * 1024.0),
                                    totalBytes / (1024.0 * 1024.0)
                                )
                            }
                        }
                    }
                    fos.flush()
                    fos.close()
                    inputStream.close()

                    context.runOnUiThread {
                        progressDialog.dismiss()
                        showInstallPrompt(context, apkFile)
                    }
                } catch (e: Exception) {
                    context.runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(context, context.getString(R.string.download_failed, e.message), Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun showInstallPrompt(context: Context, apkFile: File) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) return

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.download_complete)
            .setMessage(R.string.install_now)
            .setPositiveButton(R.string.install) { _, _ ->
                installApk(context, apkFile)
            }
            .setNegativeButton(R.string.later, null)
            .setCancelable(false)
            .show()
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

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
                Toast.makeText(context, R.string.enable_install_settings, Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.installation_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }
}

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)
