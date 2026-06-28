package com.mobilerepair.shop.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private val client = OkHttpClient()
    private const val UPDATE_URL = "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"

    fun checkForUpdates(context: Context) {
        val request = Request.Builder().url(UPDATE_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // To debug: (context as? android.app.Activity)?.runOnUiThread { Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    if (!response.isSuccessful) return
                    
                    val json = response.body?.string()
                    if (json != null) {
                        try {
                            val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                            val currentVersionCode = getCurrentVersionCode(context)
                            
                            if (updateInfo.versionCode > currentVersionCode) {
                                showUpdateDialog(context, updateInfo)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun showUpdateDialog(context: Context, updateInfo: UpdateInfo) {
        (context as? android.app.Activity)?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("A new version (${updateInfo.versionName}) is available.\n\nRelease Notes:\n${updateInfo.releaseNotes}")
                .setPositiveButton("Download") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                    context.startActivity(intent)
                }
                .setNegativeButton("Later", null)
                .setCancelable(true)
                .show()
        }
    }
}
