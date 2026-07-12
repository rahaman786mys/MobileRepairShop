package com.app.muzzutech.utils

import android.util.Log
import com.app.muzzutech.auth.WhatsAppApiManager
import com.app.muzzutech.utils.crpto.SecurePrefs
import kotlin.random.Random

/**
 * OTP generation, sending (via Fast2SMS SMS), and local verification.
 * No external OTP service needed.
 */
object OtpManager {

    private const val TAG = "OtpManager"
    private const val OTP_LENGTH = 4
    private const val OTP_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes

    private var currentOtp: String? = null
    private var otpGeneratedAt: Long = 0
    private var currentPhone: String? = null

    fun sendOtp(phone: String, callback: (Boolean, String?) -> Unit) {
        val formattedPhone = if (phone.startsWith("+91")) phone.removePrefix("+")
            else if (phone.length == 10) "91$phone"
            else phone

        // Generate 4-digit OTP
        val otp = String.format("%04d", Random.nextInt(10000))
        currentOtp = otp
        currentPhone = formattedPhone
        otpGeneratedAt = System.currentTimeMillis()

        Log.d(TAG, "Generated OTP for $formattedPhone: $otp")

        // Send via WhatsApp/SMS
        WhatsAppApiManager.sendOtp("+$formattedPhone", otp) { success, provider ->
            if (success) {
                Log.d(TAG, "OTP sent via $provider")
                callback(true, null)
            } else {
                val errMsg = if (provider == "unconfigured") {
                    "SMS not configured. Add Fast2SMS API key in settings."
                } else {
                    "Failed to send. Check Fast2SMS balance/api key."
                }
                Log.e(TAG, errMsg)
                callback(false, errMsg)
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
            currentOtp = null // invalidate after successful verification
            callback(true, null)
        } else {
            callback(false, "Invalid OTP. Try again.")
        }
    }

    fun getCurrentPhone(): String? = currentPhone
}
