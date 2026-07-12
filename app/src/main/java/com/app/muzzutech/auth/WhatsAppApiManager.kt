package com.app.muzzutech.auth

import android.util.Log
import com.app.muzzutech.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * WhatsApp (AiSensy) + SMS (Fast2SMS) Messaging Manager
 *
 * WhatsApp via AiSensy (template-based, needs Meta approval)
 * SMS via Fast2SMS (works instantly for Indian numbers)
 *
 * Add to local.properties:
 *   aisensy.api.key=your_aisensy_key
 *   aisensy.campaign.name=muzzutech_notifications
 *   fast2sms.api.key=your_fast2sms_key
 */
object WhatsAppApiManager {

    private const val TAG = "MessagingApi"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    // --- AiSensy (WhatsApp) config ---
    private val aisensyKey: String get() = BuildConfig.AISENSY_API_KEY
    private val aisensyCampaign: String get() = BuildConfig.AISENSY_CAMPAIGN_NAME
    private val aisensyEnabled: Boolean get() = aisensyKey.isNotBlank() && aisensyKey != "YOUR_AISENSY_API_KEY"

    // --- Fast2SMS (SMS) config ---
    private val fast2smsKey: String get() = BuildConfig.FAST2SMS_API_KEY
    private val fast2smsEnabled: Boolean get() = fast2smsKey.isNotBlank() && fast2smsKey != "YOUR_FAST2SMS_API_KEY"

    // ===================== PUBLIC API =====================

    fun sendOtp(
        phone: String,
        otp: String,
        onResult: (success: Boolean, provider: String) -> Unit = { _, _ -> }
    ) {
        val message = "Your MuZZu Tech OTP is: $otp. Valid for 5 minutes."
        sendMessage(phone, message, "otp") { success, provider ->
            onResult(success, provider)
        }
    }

    fun sendCredentials(
        phone: String,
        name: String,
        email: String,
        password: String,
        role: String = "Technician",
        onResult: (success: Boolean, provider: String) -> Unit = { _, _ -> }
    ) {
        val message = buildString {
            appendLine("Welcome to MuZZu Tech!")
            appendLine("Hi $name,")
            appendLine("Your worker account has been created:")
            appendLine("Email: $email")
            appendLine("Password: $password")
            appendLine("Role: $role")
            appendLine("Login: https://mobile-shop-7922a.firebaseapp.com")
            appendLine("- MuZZu Tech Team")
        }
        sendMessage(phone, message, "credentials", onResult)
    }

    fun sendPaymentNotification(
        phone: String,
        shopName: String,
        shopAddress: String,
        amount: String,
        deviceInfo: String = "",
        onResult: (success: Boolean, provider: String) -> Unit = { _, _ -> }
    ) {
        val message = buildString {
            appendLine("MuZZu Tech - Payment Received")
            appendLine("Shop: $shopName")
            appendLine("Address: $shopAddress")
            if (deviceInfo.isNotBlank()) appendLine("Device: $deviceInfo")
            appendLine("Amount: Rs.$amount")
            appendLine("Status: Completed")
            appendLine("Thank you!")
        }
        sendMessage(phone, message, "payment", onResult)
    }

    fun sendRepairUpdate(
        phone: String,
        customerName: String,
        shopName: String,
        device: String,
        status: String,
        extraMessage: String = "",
        onResult: (success: Boolean, provider: String) -> Unit = { _, _ -> }
    ) {
        val message = buildString {
            appendLine("MuZZu Tech - Repair Update")
            appendLine("Hi $customerName,")
            appendLine("Your $device repair is now: $status")
            if (extraMessage.isNotBlank()) appendLine("Note: $extraMessage")
            appendLine("Shop: $shopName")
        }
        sendMessage(phone, message, "repair_update", onResult)
    }

    fun sendDualChannel(
        phone: String,
        message: String,
        onResult: (whatsappOk: Boolean, smsOk: Boolean) -> Unit = { _, _ -> }
    ) {
        var whatsResult = false
        var smsResult = false
        var whatsDone = false
        var smsDone = false

        fun checkDone() {
            if (whatsDone && smsDone) onResult(whatsResult, smsResult)
        }

        sendViaWhatsApp(phone, message) { ok ->
            whatsResult = ok; whatsDone = true; checkDone()
        }

        sendViaSms(phone, message) { ok ->
            smsResult = ok; smsDone = true; checkDone()
        }
    }

    // ===================== SEND LOGIC =====================

    /**
     * Send via WhatsApp (AiSensy) if available, else fallback to SMS (Fast2SMS)
     */
    private fun sendMessage(
        phone: String,
        message: String,
        type: String = "text",
        onResult: (success: Boolean, provider: String) -> Unit
    ) {
        if (aisensyEnabled) {
            sendViaWhatsApp(phone, message) { success ->
                if (success) onResult(true, "whatsapp")
                else fallbackToSms(phone, message, type, onResult)
            }
        } else {
            fallbackToSms(phone, message, type, onResult)
        }
    }

    private fun fallbackToSms(
        phone: String,
        message: String,
        type: String,
        onResult: (success: Boolean, provider: String) -> Unit
    ) {
        if (fast2smsEnabled) {
            sendViaSms(phone, message) { success ->
                onResult(success, if (success) "sms" else "failed")
            }
        } else {
            Log.w(TAG, "No messaging provider configured (AiSensy or Fast2SMS)")
            onResult(false, "unconfigured")
        }
    }

    // ===================== AiSensy (WhatsApp) =====================

    private fun sendViaWhatsApp(
        phone: String,
        message: String,
        onResult: (success: Boolean) -> Unit
    ) {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val json = JSONObject().apply {
            put("apiKey", aisensyKey)
            put("campaignName", aisensyCampaign)
            put("destination", "+$cleanPhone")
            put("userName", "Customer")
            put("source", "MuZZuTechApp")
            put("templateParams", arrayOf(message))
        }

        val request = Request.Builder()
            .url("https://backend.aisensy.com/campaign/t1/api/v2")
            .post(json.toString().toRequestBody(JSON_MEDIA))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "AiSensy failed", e)
                onResult(false)
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { Log.d(TAG, "AiSensy: $it") }
                onResult(response.isSuccessful)
            }
        })
    }

    // ===================== Fast2SMS (SMS) =====================

    private fun sendViaSms(
        phone: String,
        message: String,
        onResult: (success: Boolean) -> Unit
    ) {
        // Fast2SMS expects 10-digit Indian numbers (no country code)
        val cleanPhone = phone.replace("+", "").replace(" ", "").takeLast(10)

        val json = JSONObject().apply {
            put("message", message)
            put("route", "q")
            put("numbers", cleanPhone)
        }

        val request = Request.Builder()
            .url("https://www.fast2sms.com/dev/bulkV2")
            .post(json.toString().toRequestBody(JSON_MEDIA))
            .addHeader("Authorization", fast2smsKey)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Fast2SMS failed", e)
                onResult(false)
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val success = try {
                    val obj = JSONObject(body ?: "{}")
                    obj.optBoolean("return", false)
                } catch (e: Exception) {
                    response.isSuccessful
                }
                onResult(success)
            }
        })
    }
}
