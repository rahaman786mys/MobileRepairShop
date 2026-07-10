package com.app.muzzutech.auth

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Manages WhatsApp Cloud API communication via Firebase Cloud Functions.
 *
 * Usage:
 *   WhatsAppApiManager.sendMessage(phone, "Hello!") { success, msgId -> }
 *   WhatsAppApiManager.sendOtp(phone, "1234") { success, msgId -> }
 *   WhatsAppApiManager.verifyOtp(phone, "1234") { success -> }
 */
object WhatsAppApiManager {

    private const val TAG = "WhatsAppApi"
    private val functions: FirebaseFunctions = Firebase.functions

    fun sendMessage(
        phone: String,
        message: String,
        type: String = "text",
        onResult: (success: Boolean, messageId: String) -> Unit = { _, _ -> }
    ) {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val data = hashMapOf(
            "phone" to cleanPhone,
            "message" to message,
            "type" to type
        )

        functions.getHttpsCallable("sendWhatsApp")
            .call(data)
            .addOnSuccessListener { result ->
                val msgId = (result.data as? Map<*, *>)?.get("messageId") as? String ?: ""
                onResult(true, msgId)
                Log.d(TAG, "WhatsApp sent to $cleanPhone: $msgId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "WhatsApp send failed", e)
                onResult(false, e.message ?: "Unknown error")
            }
    }

    fun sendTemplate(
        phone: String,
        templateName: String,
        languageCode: String = "en",
        components: List<Map<String, Any>> = emptyList(),
        onResult: (success: Boolean, messageId: String) -> Unit = { _, _ -> }
    ) {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val data = hashMapOf(
            "phone" to cleanPhone,
            "templateName" to templateName,
            "languageCode" to languageCode,
            "components" to components
        )

        functions.getHttpsCallable("sendTemplate")
            .call(data)
            .addOnSuccessListener { result ->
                val msgId = (result.data as? Map<*, *>)?.get("messageId") as? String ?: ""
                onResult(true, msgId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Template send failed", e)
                onResult(false, e.message ?: "Error")
            }
    }

    fun sendCredentials(
        phone: String,
        name: String,
        email: String,
        password: String,
        role: String = "Technician",
        onResult: (success: Boolean) -> Unit = {}
    ) {
        val message = buildString {
            appendLine("🎉 Welcome to MuZZu Tech! 🎉")
            appendLine()
            appendLine("Hi $name,")
            appendLine("Your worker account has been created:")
            appendLine()
            appendLine("📧 Email: $email")
            appendLine("🔑 Password: $password")
            appendLine("🆔 Role: $role")
            appendLine()
            appendLine("🔗 Login: https://mobile-shop-7922a.firebaseapp.com")
            appendLine()
            appendLine("Please save these credentials securely.")
            appendLine("— MuZZu Tech Team")
        }

        sendMessage(phone, message, "credentials") { success, _ ->
            onResult(success)
        }
    }

    fun sendOtp(
        phone: String,
        otp: String,
        onResult: (success: Boolean) -> Unit = {}
    ) {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val data = hashMapOf("phone" to cleanPhone, "otp" to otp)

        functions.getHttpsCallable("sendOtp")
            .call(data)
            .addOnSuccessListener {
                onResult(true)
                Log.d(TAG, "OTP sent to $cleanPhone")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OTP send failed", e)
                onResult(false)
            }
    }

    fun verifyOtp(
        phone: String,
        otp: String,
        onResult: (success: Boolean) -> Unit = {}
    ) {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val data = hashMapOf("phone" to cleanPhone, "otp" to otp)

        functions.getHttpsCallable("verifyOtp")
            .call(data)
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OTP verify failed", e)
                onResult(false)
            }
    }
}
