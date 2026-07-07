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
import com.app.muzzutech.utils.crpto.DatabasePassphraseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SupportFactory
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

            // 2. Verify integrity using the same encrypted Room configuration as production.
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
