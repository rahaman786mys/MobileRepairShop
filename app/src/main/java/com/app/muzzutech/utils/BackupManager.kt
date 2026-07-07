package com.app.muzzutech.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.muzzutech.MobileRepairApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages database backup and restore locally and providing Share options.
 * All file IO is suspended and must be called from a coroutine scope (e.g. lifecycleScope).
 */
object BackupManager {

    private const val DB_NAME = "mobile_repair_shop_db"

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
            Log.e("BackupManager", "Local export failed", e)
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
            Log.e("BackupManager", "Share failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    suspend fun importDatabase(context: Context, backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Copy backup to temp file first
            val tempFile = File(context.cacheDir, "restore_temp.db")
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            // 2. Verify integrity by attempting to open with raw SQLite
            val integrityOk = try {
                val testDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    tempFile.absolutePath, null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                testDb.close()
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Integrity check failed", e)
                tempFile.delete()
                false
            }
            if (!integrityOk) return@withContext false

            // 3. Close live database
            MobileRepairApp.instance.database.close()

            // 4. Atomic replace: copy verified temp to live DB location
            val dbFile = context.getDatabasePath(DB_NAME)
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            // 5. Reset singleton
            MobileRepairApp.resetDatabaseInstance()
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Import failed", e)
            MobileRepairApp.resetDatabaseInstance()
            false
        }
    }

    suspend fun syncWithGoogleDrive(context: Context, email: String): Boolean = withContext(Dispatchers.IO) {
        android.util.Log.w("BackupManager", "Google Drive sync not yet implemented")
        false
    }
}
