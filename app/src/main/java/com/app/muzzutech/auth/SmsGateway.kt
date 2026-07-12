package com.app.muzzutech.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

object SmsGateway {

    private const val TAG = "SmsGateway"

    fun isAvailable(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun sendSms(context: Context, phone: String, message: String, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable(context)) {
            onResult(false, "SMS permission not granted")
            return
        }
        try {
            var num = phone.replace("+", "").replace(" ", "")
            if (num.length > 10) num = num.takeLast(10)
            if (num.length != 10) {
                onResult(false, "Invalid phone number")
                return
            }
            val sm = SmsManager.getDefault()
            val parts = sm.divideMessage(message)
            if (parts.size > 1) {
                sm.sendMultipartTextMessage(num, null, parts, null, null)
            } else {
                sm.sendTextMessage(num, null, message, null, null)
            }
            Log.d(TAG, "SMS sent to $num")
            onResult(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "SMS send failed", e)
            onResult(false, e.message ?: "SMS failed")
        }
    }
}
