package com.app.muzzutech

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.app.muzzutech.utils.update.UpdateRepository

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val smsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        proceedToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        animateSplashLogo()

        checkBiometrics()
    }

    private fun requestSmsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        } else {
            proceedToMain()
        }
    }

    private fun animateSplashLogo() {
        val splash = findViewById<android.widget.ImageView>(R.id.ivSplash) ?: return
        val fadeIn = ObjectAnimator.ofFloat(splash, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val scaleX = ObjectAnimator.ofFloat(splash, "scaleX", 0.85f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(splash, "scaleY", 0.85f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            start()
        }
    }

    private fun checkBiometrics() {
        val prefs = SecurePrefs.appSettings(this)
        val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)

        if (!isBiometricEnabled) {
            requestSmsPermissionIfNeeded()
            return
        }

        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showBiometricPrompt()
            else -> requestSmsPermissionIfNeeded()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(applicationContext, "Authentication required", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(applicationContext, "Biometric authentication failed: $errString", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    requestSmsPermissionIfNeeded()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Repair Shop Security")
            .setSubtitle("Authenticate to access your shop data")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun proceedToMain() {
        handler.postDelayed({
            val prefs = SecurePrefs.authPrefs(this)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java) // nav graph will show login as start
            }
            startActivity(intent)
            finish()
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
