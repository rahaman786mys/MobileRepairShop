package com.app.muzzutech.utils

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * OTP via Firebase Phone Auth — sends SMS automatically, no third-party API needed.
 * Firebase handles the entire OTP lifecycle: send, verify, validity.
 */
object OtpManager {

    private const val TAG = "OtpManager"
    private const val OTP_TIMEOUT_SECONDS = 60L

    private var currentPhone: String? = null
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(activity: Activity, phone: String, callback: (Boolean, String?) -> Unit) {
        val formattedPhone = if (!phone.startsWith("+")) "+91$phone" else phone
        currentPhone = formattedPhone

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(formattedPhone)
            .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Auto-verify succeeded for $formattedPhone")
                    // SMS auto-retrieved (instant verify on some devices)
                    storedVerificationId = credential.smsCode ?: ""
                    callback(true, null)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "OTP send failed", e)
                    callback(false, e.message ?: "Failed to send OTP")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "OTP sent to $formattedPhone: $verificationId")
                    storedVerificationId = verificationId
                    resendToken = token
                    callback(true, null)
                }

                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    Log.d(TAG, "Auto-retrieval timed out")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendOtp(activity: Activity, callback: (Boolean, String?) -> Unit) {
        val phone = currentPhone ?: run {
            callback(false, "No phone number from previous attempt")
            return
        }
        val token = resendToken ?: run {
            callback(false, "No resend token available. Request OTP again.")
            return
        }

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    callback(true, null)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    callback(false, e.message ?: "Resend failed")
                }
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificationId
                    resendToken = token
                    callback(true, null)
                }
            })
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String, callback: (Boolean, String?) -> Unit) {
        val vid = storedVerificationId
        if (vid == null) {
            callback(false, "No OTP was sent. Request a new one.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(vid, code)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storedVerificationId = null
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message ?: "Invalid OTP")
                }
            }
    }

    fun getCurrentPhone(): String? = currentPhone
}
