package com.app.muzzutech.utils.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val sizeBytes: Long? = null,
    val forceUpdate: Boolean = false
) {
    val hasSize: Boolean get() = sizeBytes != null && sizeBytes > 0
}

class UpdateRepository(private val context: Context) {

    companion object {
        private const val TAG = "UpdateRepository"
        private val VERSION_URL get() = com.app.muzzutech.BuildConfig.UPDATE_CHECK_URL
        private const val GITHUB_LATEST_URL =
            "https://api.github.com/repos/rahaman786mys/MobileRepairShop/releases/latest"
        const val PREFS_NAME = "update_prefs"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_SNOOZED_VERSION = "snoozed_update_version"
        const val KEY_LAST_CHECK_TS = "last_update_check_ts"
    }

    private val gson = Gson()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null)
        .build()

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentVersionCode(): Int = com.app.muzzutech.BuildConfig.VERSION_CODE
    fun getCurrentVersionName(): String = com.app.muzzutech.BuildConfig.VERSION_NAME

    fun getLastSeenVersion(): Int =
        prefs.getInt(KEY_LAST_SEEN_VERSION, getCurrentVersionCode())

    fun setLastSeenVersion(code: Int) {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION, code).apply()
    }

    fun getSnoozedVersion(): Int =
        prefs.getInt(KEY_SNOOZED_VERSION, -1)

    fun setSnoozedVersion(code: Int) {
        prefs.edit().putInt(KEY_SNOOZED_VERSION, code).apply()
    }

    fun getLastCheckTimestamp(): Long =
        prefs.getLong(KEY_LAST_CHECK_TS, 0L)

    fun setLastCheckTimestamp(ts: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TS, ts).apply()
    }

    fun isSameSessionSnoozed(targetCode: Int): Boolean {
        return getSnoozedVersion() == targetCode
    }

    fun shouldShowUpdate(latestCode: Int): Boolean {
        val current = getCurrentVersionCode()
        val snoozed = getSnoozedVersion()
        return latestCode > current && latestCode != snoozed
    }

    fun shouldShowWhatsNew(): Boolean {
        val lastSeen = getLastSeenVersion()
        val current = getCurrentVersionCode()
        return current > lastSeen
    }

    fun markWhatsNewShown() {
        setLastSeenVersion(getCurrentVersionCode())
    }

    suspend fun fetchLatestVersion(): Result<VersionInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(VERSION_URL)
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchLatestVersion: version.json HTTP ${response.code}")
                    return@withContext Result.failure(IOException("version.json HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: run {
                    Log.w(TAG, "fetchLatestVersion: version.json empty body")
                    return@withContext Result.failure(IOException("version.json empty body"))
                }
                val info = gson.fromJson(body, VersionInfo::class.java)
                Log.i(TAG, "fetchLatestVersion: success=$info")
                Result.success(info)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatestVersion: exception ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchReleaseFromGitHubApi(): Result<VersionInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchReleaseFromGitHubApi: GitHub API HTTP ${response.code} body=${response.body?.string()}")
                    return@withContext Result.failure(IOException("GitHub API HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: run {
                    Log.w(TAG, "fetchReleaseFromGitHubApi: empty body")
                    return@withContext Result.failure(IOException("GitHub API empty body"))
                }
                val json = gson.fromJson(body, Map::class.java)
                val tagName = (json["tag_name"] as? String) ?: run {
                    Log.w(TAG, "fetchReleaseFromGitHubApi: missing tag_name, body=$body")
                    return@withContext Result.success(null)
                }
                val sanitized = tagName
                    .removePrefix("v")
                    .removePrefix("V")
                    .split("-")
                    .first()
                val parts = sanitized.split(".")
                val versionCode =
                    (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 1000000 +
                            (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 1000 +
                            (parts.getOrNull(2)?.toIntOrNull() ?: 0)
                val vName = tagName.removePrefix("v")
                val releaseNotes = (json["body"] as? String) ?: ""
                val assetList = json["assets"] as? List<Map<String, Any>>
                val size = assetList?.firstOrNull()?.get("size") as? Double
                val downloadUrl = assetList?.firstOrNull()?.get("browser_download_url") as? String
                    ?: "https://github.com/rahaman786mys/MobileRepairShop/releases/download/v1.5.9/app-debug-v1.5.9.apk"
                Log.i(TAG, "fetchReleaseFromGitHubApi: tag=$tagName versionCode=$versionCode name=$vName url=$downloadUrl")
                Result.success(
                    VersionInfo(
                        versionCode = versionCode,
                        versionName = vName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                        sizeBytes = size?.toLong()
                    )
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "fetchReleaseFromGitHubApi: IOException ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchReleaseFromGitHubApi: Exception ${e.message}", e)
            Result.failure(e)
        }
    }
}
