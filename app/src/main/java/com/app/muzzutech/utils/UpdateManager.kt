package com.app.muzzutech.utils

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UpdateManager {

    private const val VERSION_URL = "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null) // Disable cache to ensure we get fresh version info
        .build()
    private var skippedVersionCode: Int = -1

    fun checkForUpdates(context: Context, onNoUpdate: (() -> Unit)? = null) {
        // Add timestamp to bypass GitHub/System caching
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
                            packageInfo.versionCode.toLong()
                        }
                        val currentVersionName = packageInfo.versionName ?: "?"

                        if (versionInfo.versionCode > currentVersion && versionInfo.versionCode != skippedVersionCode) {
                            (context as? android.app.Activity)?.runOnUiThread {
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
        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("v$currentVersionName → v${info.versionName}\n\n${info.releaseNotes}")
            .setPositiveButton("Download") { _, _ ->
                downloadApk(context, info.downloadUrl)
            }
            .setNegativeButton("Later") { _, _ ->
                skippedVersionCode = info.versionCode
            }
            .setCancelable(false)
            .show()
    }

    private fun downloadApk(context: Context, url: String) {
        val progressDialog = ProgressDialog(context).apply {
            setTitle("Downloading Update")
            setMessage("Please wait...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            show()
        }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Storage not available", Toast.LENGTH_SHORT).show()
            return
        }
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val apkFile = File(downloadsDir, "repair_shop_update.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? android.app.Activity)?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body ?: return
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
                            (context as? android.app.Activity)?.runOnUiThread {
                                progressDialog.progress = progress
                                progressDialog.setMessage("${progress}% - ${downloadedBytes / (1024 * 1024)}MB / ${totalBytes / (1024 * 1024)}MB")
                            }
                        }
                    }
                    fos.close()
                    inputStream.close()

                    (context as? android.app.Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        showInstallPrompt(context, apkFile)
                    }
                } catch (e: Exception) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun showInstallPrompt(context: Context, apkFile: File) {
        AlertDialog.Builder(context)
            .setTitle("Download Complete")
            .setMessage("Update downloaded successfully. Install now?")
            .setPositiveButton("Install") { _, _ ->
                installApk(context, apkFile)
            }
            .setNegativeButton("Later", null)
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
                Toast.makeText(context, "Enable install from this source in settings, then reopen the app", Toast.LENGTH_LONG).show()
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

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)
