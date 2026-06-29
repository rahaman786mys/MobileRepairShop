package com.app.muzzutech.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for handling photo operations
 */
object PhotoUtils {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Create a photo file in the app's private storage
     */
    fun createPhotoFile(context: Context, prefix: String = "IMG_"): File {
        val timeStamp = dateFormat.format(Date())
        val imageDir = File(context.filesDir, "photos")
        if (!imageDir.exists()) imageDir.mkdirs()
        return File(imageDir, "${prefix}${timeStamp}.jpg")
    }

    /**
     * Copy URI content to a local file
     */
    fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get a scaled bitmap from a file path
     */
    fun getScaledBitmap(filePath: String, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            val scale = calculateInSampleSize(options, maxWidth, maxHeight)
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            BitmapFactory.decodeFile(filePath, finalOptions)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Delete photo file
     */
    fun deletePhotoFile(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }
}
