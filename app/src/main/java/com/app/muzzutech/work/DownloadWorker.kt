package com.app.muzzutech.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads APK from a URL. Reports progress via WorkManager's setProgress().
 * Retries up to 3 times with exponential backoff on failure.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_URL = "download_url"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_PROGRESS = "progress"
        const val KEY_PROGRESS_TEXT = "progress_text"

        private val client by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val destDir = File(applicationContext.filesDir, "updates")
        if (!destDir.exists()) destDir.mkdirs()
        val apkFile = File(destDir, "update.apk")

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: HTTP ${response.code}")
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
            val body = response.body ?: return Result.failure()
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastUpdateMillis = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (totalBytes > 0 && now - lastUpdateMillis > 500L) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            setProgress(
                                Data.Builder()
                                    .putInt(KEY_PROGRESS, progress)
                                    .putString(KEY_PROGRESS_TEXT, formatProgress(downloadedBytes, totalBytes))
                                    .build()
                            )
                            lastUpdateMillis = now
                        }
                    }
                }
            }
            val outputData = Data.Builder().putString(KEY_FILE_PATH, apkFile.absolutePath).build()
            Log.i(TAG, "Download complete: ${apkFile.absolutePath} (${downloadedBytes} bytes)")
            Result.success(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Download error (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun formatProgress(downloadedBytes: Long, totalBytes: Long): String {
        return String.format(
            java.util.Locale.getDefault(),
            "%.1f / %.1f MB",
            downloadedBytes / (1024.0 * 1024.0),
            totalBytes / (1024.0 * 1024.0)
        )
    }
}
