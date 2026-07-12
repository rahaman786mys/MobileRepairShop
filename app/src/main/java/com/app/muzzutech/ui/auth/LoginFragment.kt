package com.app.muzzutech.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.R
import com.app.muzzutech.auth.AuthManager
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.OtpManager
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var isRegisterMode = true

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                loginSuccess(email)
            } else {
                Snackbar.make(binding.root, "No email returned", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                10 -> "Configuration Error (Code 10). SHA-1 not registered in Google Console."
                7 -> "Network error. Check internet."
                12501 -> "Sign-in cancelled."
                else -> "Sign-in failed (Code: ${e.statusCode})"
            }
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            Log.e("LoginFragment", "Google Sign-In Error: ${e.statusCode} - ${e.message}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUiForMode()

        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isRegisterMode = checkedId == R.id.btnToggleRegister
                updateUiForMode()
            }
        }

        binding.btnGoogleSync.setOnClickListener { signInWithGoogle() }

        binding.btnWhatsApp.setOnClickListener {
            Toast.makeText(requireContext(), "WhatsApp login coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPhoneOtp.setOnClickListener { showPhoneInput() }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) {
                sendOtp(phone)
            } else {
                binding.tilMobileNumber.error = "Enter valid 10-digit number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            verifyOtp(otp)
        }

        binding.tvResendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) sendOtp(phone)
        }

        binding.btnWorkerLogin.setOnClickListener { workerLogin() }
    }

    private fun updateUiForMode() {
        binding.tvGoogleLabel.text = if (isRegisterMode) "Register with Google" else "Login with Google"
        binding.tvWhatsAppLabel.text = if (isRegisterMode) "Register with WhatsApp" else "Login with WhatsApp"
        binding.tvPhoneOtpLabel.text = if (isRegisterMode) "Register with Phone OTP" else "Login with Phone OTP"
        binding.layoutWorker.isVisible = !isRegisterMode
        hideAllInputs()
    }

    private fun hideAllInputs() {
        binding.layoutPhoneInput.isVisible = false
        binding.layoutOtpInput.isVisible = false
    }

    private fun showPhoneInput() {
        hideAllInputs()
        binding.layoutPhoneInput.isVisible = true
    }

    private fun signInWithGoogle() {
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun sendOtp(mobile: String) {
        binding.progressBar.isVisible = true
        binding.btnSendOtp.isEnabled = false
        binding.tilMobileNumber.error = null
        OtpManager.sendOtp(mobile) { success, error ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                binding.btnSendOtp.isEnabled = true
                if (success) {
                    binding.layoutPhoneInput.isVisible = false
                    binding.layoutOtpInput.isVisible = true
                    binding.tvOtpSentTo.text = if (isRegisterMode) "OTP sent for registration" else "OTP sent for login"
                    binding.tvOtpPhone.text = "+91 $mobile"
                    Toast.makeText(requireContext(), "OTP sent via SMS", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), error ?: "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        binding.progressBar.isVisible = true
        OtpManager.verifyOtp(otp) { success, error ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                if (success) {
                    val phone = OtpManager.getCurrentPhone() ?: ""
                    loginSuccess(phone)
                } else {
                    Toast.makeText(requireContext(), error ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun workerLogin() {
        val phone = binding.etWorkerPhone.text.toString().trim()
        val password = binding.etWorkerPassword.text.toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Enter email/phone and password", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.progressBar.isVisible = true
        binding.btnWorkerLogin.isEnabled = false

        lifecycleScope.launch {
            val authManager = AuthManager(requireContext())
            val result = authManager.workerLogin(phone, password)
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                binding.btnWorkerLogin.isEnabled = true
                if (result.success) {
                    loginSuccess(phone, result.firebaseUid)
                } else {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loginSuccess(identifier: String = "", firebaseUid: String = "") {
        val ctx = context ?: return
        val prefs = SecurePrefs.authPrefs(ctx)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            if (identifier.isNotEmpty()) putString("logged_in_identifier", identifier)
            if (firebaseUid.isNotEmpty()) putString("logged_in_firebase_uid", firebaseUid)
            apply()
        }

        if (isAdded) {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
