package com.mobilerepair.shop.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

/**
 * Manages in-app updates by checking GitHub for latest version
 */
object UpdateManager {

    private const val VERSION_URL = "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/main/version.json"
    private val client = OkHttpClient()

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
                        val currentVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
            .setTitle("Update Available (v${info.versionName})")
            .setMessage("New version is available with latest features:\n\n${info.releaseNotes}\n\nDo you want to update now?")
            .setPositiveButton("Update Now") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                context.startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }
}

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)
