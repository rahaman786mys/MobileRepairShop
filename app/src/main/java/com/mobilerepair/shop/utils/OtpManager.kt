package com.mobilerepair.shop.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Real OTP Integration using otp.dev
 */
object OtpManager {

    private const val API_KEY = "5ecc5d768b7399b9f83f06b0d9af8482"
    private const val SENDER_ID = "846c8485-0170-475e-b296-4ceff50eb9fb"
    private const val TEMPLATE_ID = "4663fd51-9287-4179-a46c-495cb395f5aa"
    private const val BASE_URL = "https://api.otp.dev/v1"

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    // Store current verification ID to verify later
    private var currentVerificationId: String? = null

    fun sendOtp(phone: String, callback: (Boolean, String?) -> Unit) {
        // Format phone: ensuring it starts with country code without +
        val formattedPhone = if (phone.startsWith("+")) phone.substring(1) else if (phone.length == 10) "91$phone" else phone

        val dataObj = JsonObject().apply {
            addProperty("channel", "sms")
            addProperty("sender", SENDER_ID)
            addProperty("phone", formattedPhone)
            addProperty("template", TEMPLATE_ID)
            addProperty("code_length", 4)
        }

        val rootObj = JsonObject().apply {
            add("data", dataObj)
        }

        val request = Request.Builder()
            .url("$BASE_URL/verifications")
            .post(rootObj.toString().toRequestBody(JSON))
            .addHeader("X-OTP-Key", API_KEY)
            .addHeader("accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OtpManager", "Failed to send OTP", e)
                callback(false, "Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val jsonResponse = gson.fromJson(body, JsonObject::class.java)
                        val verification = jsonResponse.getAsJsonObject("data")
                        currentVerificationId = verification.get("id").asString
                        callback(true, null)
                    } catch (e: Exception) {
                        callback(false, "Parsing Error")
                    }
                } else {
                    callback(false, "API Error: ${response.code}")
                }
            }
        })
    }

    fun verifyOtp(code: String, callback: (Boolean, String?) -> Unit) {
        val verificationId = currentVerificationId
        if (verificationId == null) {
            callback(false, "No active verification found")
            return
        }

        val dataObj = JsonObject().apply {
            addProperty("code", code)
        }

        val rootObj = JsonObject().apply {
            add("data", dataObj)
        }

        val request = Request.Builder()
            .url("$BASE_URL/verifications/$verificationId/verify")
            .post(rootObj.toString().toRequestBody(JSON))
            .addHeader("X-OTP-Key", API_KEY)
            .addHeader("accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Network Error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, "Invalid OTP Code")
                }
            }
        })
    }
}
