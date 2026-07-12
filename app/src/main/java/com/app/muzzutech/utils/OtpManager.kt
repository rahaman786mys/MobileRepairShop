package com.app.muzzutech.utils

import android.util.Log
import com.app.muzzutech.auth.WhatsAppApiManager
import kotlin.random.Random

/**
 * OTP generation, sending (via Fast2SMS Quick SMS), and local verification.
 */
object OtpManager {

    private const val TAG = "OtpManager"
    private const val OTP_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes

    private var currentOtp: String? = null
    private var otpGeneratedAt: Long = 0
    private var currentPhone: String? = null

    fun sendOtp(phone: String, callback: (Boolean, String?) -> Unit) {
        val formattedPhone = if (phone.startsWith("+91")) phone.removePrefix("+")
            else if (phone.length == 10) "91$phone"
            else phone

        val otp = String.format("%04d", Random.nextInt(10000))
        currentOtp = otp
        currentPhone = formattedPhone
        otpGeneratedAt = System.currentTimeMillis()

        Log.d(TAG, "Generated OTP for $formattedPhone: $otp")

        WhatsAppApiManager.sendOtp("+$formattedPhone", otp) { success, provider ->
            if (success) {
                Log.d(TAG, "OTP sent via $provider")
                callback(true, null)
            } else {
                Log.e(TAG, "Failed to send OTP")
                callback(false, "Failed to send. Check Fast2SMS balance.")
            }
        }
    }

    fun verifyOtp(code: String, callback: (Boolean, String?) -> Unit) {
        val storedOtp = currentOtp
        val storedPhone = currentPhone
        val generatedAt = otpGeneratedAt

        if (storedOtp == null || storedPhone == null) {
            callback(false, "No OTP was sent. Request a new one.")
            return
        }

        if (System.currentTimeMillis() - generatedAt > OTP_VALIDITY_MS) {
            currentOtp = null
            callback(false, "OTP expired. Request a new one.")
            return
        }

        if (code == storedOtp) {
            currentOtp = null
            callback(true, null)
        } else {
            callback(false, "Invalid OTP. Try again.")
        }
    }

    fun getCurrentPhone(): String? = currentPhone
}
