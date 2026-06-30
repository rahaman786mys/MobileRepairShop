package com.app.muzzutech.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppOtpUtil {

    private var generatedOtp: String? = null

    fun generateOtp(): String {
        val otp = (100000..999999).random().toString()
        generatedOtp = otp
        return otp
    }

    fun validateOtp(enteredOtp: String): Boolean {
        return generatedOtp != null && enteredOtp == generatedOtp
    }

    fun sendOtpViaWhatsApp(context: Context, mobileNumber: String): String? {
        val otp = generateOtp()
        val fullNumber = "91$mobileNumber"
        val message = "Your *MuZZu Tech* login OTP is: *$otp*%0A%0ADo not share this code with anyone."

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$fullNumber?text=$message")
            setPackage("com.whatsapp")
        }

        return try {
            context.startActivity(intent)
            otp
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$fullNumber?text=$message")
                }
                context.startActivity(fallbackIntent)
                otp
            } catch (e2: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_LONG).show()
                null
            }
        }
    }
}
