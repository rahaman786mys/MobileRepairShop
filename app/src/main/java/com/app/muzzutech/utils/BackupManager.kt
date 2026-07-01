package com.app.muzzutech.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.muzzutech.MobileRepairApp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * Manages database backup and restore locally and providing Share options.
 */
object BackupManager {

    private const val DB_NAME = "mobile_repair_shop_db"

    /**
     * Exports the current Room database to the Downloads folder
     */
    fun exportLocally(context: Context) {
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
                Toast.makeText(context, "Backup saved to Downloads!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Database not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Local export failed", e)
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports the database and opens the Share sheet (for other apps)
     */
    fun shareBackup(context: Context) {
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
                context.startActivity(Intent.createChooser(intent, "Share Backup via"))
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Share failed", e)
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Restores the database from a backup file
     */
    fun importDatabase(context: Context, backupUri: Uri): Boolean {
        return try {
            MobileRepairApp.instance.database.close()
            val dbFile = context.getDatabasePath(DB_NAME)
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Import failed", e)
            false
        }
    }

    fun syncWithGoogleDrive(context: Context, email: String) {
        Toast.makeText(context, "Google Drive Sync initiated for $email", Toast.LENGTH_SHORT).show()
    }
}
