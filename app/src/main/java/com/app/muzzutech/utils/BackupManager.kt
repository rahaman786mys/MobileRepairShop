package com.app.muzzutech.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.room.Room
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.UserProfile
import com.app.muzzutech.utils.crpto.DatabasePassphraseProvider
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SupportFactory
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manages database backup and restore locally, sharing, and Google Drive sync.
 * All file IO is suspended and must be called from a coroutine scope (e.g. lifecycleScope).
 */
object BackupManager {

    private const val DB_NAME = "mobile_repair_shop_db"
    private const val TAG = "BackupManager"

    // Google Drive API
    private const val DRIVE_FILES_API = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"
    private const val BACKUP_PREFIX = "muzzutech_backup_"
    private const val MAX_BACKUPS = 10

    private val driveClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ── Local Backup ──────────────────────────────────────────────────────

    suspend fun exportLocally(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloadsDir, "MuZZu_Tech_Backup_${System.currentTimeMillis()}.db")

            if (dbFile.exists()) {
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.channel.transferTo(0, input.channel.size(), output.channel)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup saved to Downloads!", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Database not found", Toast.LENGTH_SHORT).show()
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local export failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    suspend fun shareBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val tempFile = File(context.cacheDir, "MuZZu_Backup.db")

            if (dbFile.exists()) {
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.channel.transferTo(0, input.channel.size(), output.channel)
                    }
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Share Backup via"))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Share failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    suspend fun importDatabase(context: Context, backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "restore_temp.db")
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            val integrityOk = try {
                val passphrase = DatabasePassphraseProvider.getOrCreatePassphrase(context)
                val testDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    tempFile.absolutePath
                )
                    .openHelperFactory(SupportFactory(passphrase))
                    .addMigrations(
                        AppDatabase.MIGRATION_8_9,
                        AppDatabase.MIGRATION_9_10,
                        AppDatabase.MIGRATION_10_11,
                        AppDatabase.MIGRATION_11_12,
                        AppDatabase.MIGRATION_12_13,
                        AppDatabase.MIGRATION_13_14,
                        AppDatabase.MIGRATION_14_15
                    )
                    .build()
                testDb.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sqlite_master").close()
                testDb.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Integrity check failed", e)
                tempFile.delete()
                false
            }
            if (!integrityOk) return@withContext false

            MobileRepairApp.instance.database.close()
            val dbFile = context.getDatabasePath(DB_NAME)
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()
            MobileRepairApp.resetDatabaseInstance()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            MobileRepairApp.resetDatabaseInstance()
            false
        }
    }

    // ── Google Drive Auth ────────────────────────────────────────────────

    /**
     * Gets a fresh OAuth2 Bearer token for the given account with drive.appdata scope.
     * Must be called from a background thread.
     * drive.appdata restricts access to the hidden appDataFolder — invisible to user's Drive.
     */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.getToken(
                    context,
                    account.account ?: return@withContext null,
                    "oauth2:https://www.googleapis.com/auth/drive.appdata"
                )
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "UserRecoverableAuth - need to re-authorize", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Token acquisition failed", e)
                null
            }
        }
    }

    // ── Google Drive Sync (Upload) ───────────────────────────────────────

    /**
     * Uploads the encrypted database to Google Drive appDataFolder.
     * Keeps at most [MAX_BACKUPS] recent files.
     *
     * @return Pair(success, detailMessage)
     */
    suspend fun syncWithGoogleDrive(
        context: Context,
        email: String,
        accessToken: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return@withContext Pair(false, "Database file not found")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${BACKUP_PREFIX}${timestamp}.db"
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val tempBackupFile = File(backupDir, fileName)
            dbFile.copyTo(tempBackupFile, overwrite = true)

            val uploadResult = uploadToDrive(accessToken, fileName, tempBackupFile)
            tempBackupFile.delete()
            if (!uploadResult.first) {
                return@withContext Pair(false, uploadResult.second)
            }

            cleanupOldBackups(accessToken)
            updateSyncStatus(context, "SUCCESS", email)
            Pair(true, "Sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive sync failed", e)
            updateSyncStatus(context, "FAILED", email)
            Pair(false, e.message ?: "Unknown error")
        }
    }

    /**
     * Uploads a file to Drive appDataFolder via multipart upload.
     * POST /upload/drive/v3/files?uploadType=multipart&spaces=appDataFolder
     */
    private suspend fun uploadToDrive(
        accessToken: String,
        fileName: String,
        file: File
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("parents", JSONObject.wrap(arrayOf("appDataFolder")))
            }

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(
                    Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                    RequestBody.create("application/json; charset=UTF-8".toMediaType(), metadata.toString())
                )
                .addPart(
                    Headers.headersOf("Content-Type", "application/octet-stream"),
                    RequestBody.create("application/octet-stream".toMediaType(), file)
                )
                .build()

            val request = Request.Builder()
                .url("$DRIVE_UPLOAD_API?uploadType=multipart&spaces=appDataFolder")
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            val response = driveClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Pair(true, "Upload successful")
            } else {
                Pair(false, "Upload failed: HTTP ${response.code} - $body")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload to Drive failed", e)
            Pair(false, e.message ?: "Upload error")
        }
    }

    // ── Google Drive List ────────────────────────────────────────────────

    /**
     * Lists backup files in the Drive appDataFolder, createdTime descending.
     */
    suspend fun listDriveBackups(accessToken: String): List<DriveBackupInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$DRIVE_FILES_API?" +
                        "spaces=appDataFolder" +
                        "&q=name%20contains%20%27muzzutech_backup%27" +
                        "&orderBy=createdTime%20desc" +
                        "&pageSize=20" +
                        "&fields=files(id%2Cname%2CcreatedTime%2Csize)"

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val response = driveClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext emptyList()

                val json = JSONObject(body)
                val filesArray = json.optJSONArray("files") ?: return@withContext emptyList()
                val files = mutableListOf<DriveBackupInfo>()
                for (i in 0 until filesArray.length()) {
                    val file = filesArray.getJSONObject(i)
                    files.add(
                        DriveBackupInfo(
                            id = file.getString("id"),
                            name = file.optString("name", ""),
                            createdTime = file.optString("createdTime", ""),
                            size = file.optLong("size", 0)
                        )
                    )
                }
                files
            } catch (e: Exception) {
                Log.e(TAG, "List Drive backups failed", e)
                emptyList()
            }
        }
    }

    // ── Google Drive Cleanup ─────────────────────────────────────────────

    /**
     * Deletes oldest backups beyond the [MAX_BACKUPS] retention limit.
     */
    private suspend fun cleanupOldBackups(accessToken: String) {
        withContext(Dispatchers.IO) {
            try {
                val files = listDriveBackups(accessToken)
                if (files.size > MAX_BACKUPS) {
                    for (file in files.drop(MAX_BACKUPS)) {
                        deleteDriveFile(accessToken, file.id)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup failed (non-fatal)", e)
            }
        }
    }

    // ── Google Drive Delete ──────────────────────────────────────────────

    /**
     * Deletes a file from Drive by ID.
     * DELETE /drive/v3/files/{fileId}
     */
    private suspend fun deleteDriveFile(accessToken: String, fileId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$DRIVE_FILES_API/$fileId")
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()
                driveClient.newCall(request).execute().isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed for $fileId", e)
                false
            }
        }
    }

    // ── Google Drive Restore ─────────────────────────────────────────────

    /**
     * Downloads the most recent backup from Drive, verifies integrity,
     * and atomically replaces the live database.
     *
     * @return Pair(success, detailMessage)
     */
    suspend fun restoreFromGoogleDrive(
        context: Context,
        accessToken: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val backups = listDriveBackups(accessToken)
            if (backups.isEmpty()) {
                return@withContext Pair(false, "No cloud backup found")
            }

            val latest = backups.first()

            val request = Request.Builder()
                .url("$DRIVE_FILES_API/${latest.id}?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = driveClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Pair(false, "Download failed: HTTP ${response.code}")
            }

            val downloadedBytes = response.body?.bytes()
                ?: return@withContext Pair(false, "Empty response")
            val tempFile = File(context.cacheDir, "download_restore_temp.db")
            tempFile.writeBytes(downloadedBytes)

            val passphrase = DatabasePassphraseProvider.getOrCreatePassphrase(context)
            val integrityOk = try {
                val testDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    tempFile.absolutePath
                )
                    .openHelperFactory(SupportFactory(passphrase))
                    .addMigrations(
                        AppDatabase.MIGRATION_8_9,
                        AppDatabase.MIGRATION_9_10,
                        AppDatabase.MIGRATION_10_11,
                        AppDatabase.MIGRATION_11_12,
                        AppDatabase.MIGRATION_12_13,
                        AppDatabase.MIGRATION_13_14,
                        AppDatabase.MIGRATION_14_15,
                        AppDatabase.MIGRATION_16_17,
                        AppDatabase.MIGRATION_17_18
                    )
                    .build()
                testDb.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sqlite_master").close()
                testDb.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Restore integrity check failed", e)
                tempFile.delete()
                false
            }

            if (!integrityOk) {
                return@withContext Pair(false, "Backup file integrity check failed")
            }

            MobileRepairApp.instance.database.close()
            val dbFile = context.getDatabasePath(DB_NAME)
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()
            MobileRepairApp.resetDatabaseInstance()
            updateSyncStatus(context, "SUCCESS", "")

            Pair(true, "Restored from: ${latest.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Restore from Drive failed", e)
            MobileRepairApp.resetDatabaseInstance()
            Pair(false, e.message ?: "Restore failed")
        }
    }

    // ── Sync Status Persistence ──────────────────────────────────────────

    /**
     * Persists sync result to UserProfile in Room.
     * This drives the sync status display in the More screen.
     */
    private suspend fun updateSyncStatus(context: Context, status: String, email: String) {
        try {
            val dao = MobileRepairApp.instance.database.userProfileDao()
            val existing = dao.getUserProfile() ?: UserProfile(id = 1)
            dao.insertOrUpdate(
                existing.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = status,
                    lastSyncEmail = if (email.isNotEmpty()) email else existing.lastSyncEmail
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist sync status", e)
        }
    }
}

/**
 * Lightweight model for Drive file metadata returned by the list API.
 */
data class DriveBackupInfo(
    val id: String,
    val name: String,
    val createdTime: String,
    val size: Long
)
