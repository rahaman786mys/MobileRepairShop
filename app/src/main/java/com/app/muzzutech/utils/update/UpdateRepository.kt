package com.app.muzzutech.utils.update

import android.content.Context
import android.content.SharedPreferences
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

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(null)
        .hostnameVerifier { host, _ -> host == "raw.githubusercontent.com" || host == "github.com" }
        .build()

    companion object {
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
        private const val GITHUB_LATEST_URL =
            "https://api.github.com/repos/rahaman786mys/MobileRepairShop/releases/latest"
        const val PREFS_NAME = "update_prefs"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_SNOOZED_VERSION = "snoozed_update_version"
        const val KEY_LAST_CHECK_TS = "last_update_check_ts"
    }

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
                if (!response.isSuccessful) return@withContext Result.failure(IOException("version.json HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("version.json empty body"))
                val info = gson.fromJson(body, VersionInfo::class.java)
                Result.success(info)
            }
        } catch (e: Exception) {
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
                if (!response.isSuccessful) return@withContext Result.failure(IOException("GitHub API HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("GitHub API empty body"))
                val json = gson.fromJson(body, Map::class.java)
                val tagName = (json["tag_name"] as? String) ?: return@withContext Result.success(null)
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
                    ?: "https://github.com/rahaman786mys/MobileRepairShop/releases/latest/download/app-release.apk"
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
            Result.success(null)
        } catch (e: Exception) {
            Result.success(null)
        }
    }
}
