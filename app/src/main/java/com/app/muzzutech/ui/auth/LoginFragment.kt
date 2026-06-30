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
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.WhatsAppOtpUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                Log.d("LoginFragment", "Google Sign-In success: $email")
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

        binding.btnGoogleSync.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) {
                sendOtpViaWhatsApp(phone)
            } else {
                binding.tilMobileNumber.error = "Enter valid 10-digit WhatsApp number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (WhatsAppOtpUtil.validateOtp(otp)) {
                loginSuccess()
            } else {
                Toast.makeText(requireContext(), "Invalid OTP. Check your WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) {
                sendOtpViaWhatsApp(phone)
            }
        }
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

    private fun sendOtpViaWhatsApp(mobile: String) {
        val otp = WhatsAppOtpUtil.sendOtpViaWhatsApp(requireContext(), mobile)
        if (otp != null) {
            binding.progressBar.isVisible = true
            binding.btnSendOtp.isEnabled = false

            view?.postDelayed({
                binding.progressBar.isVisible = false
                binding.layoutMobileInput.isVisible = false
                binding.layoutOtpInput.isVisible = true
                binding.tvOtpSentTo.text = "OTP sent via WhatsApp to +91 $mobile\n\nCheck your WhatsApp, copy the OTP and enter it below."
                Toast.makeText(requireContext(), "WhatsApp opened! Send the message to yourself", Toast.LENGTH_LONG).show()
            }, 500)
        } else {
            Toast.makeText(requireContext(), "Failed to open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginSuccess(email: String = "") {
        val prefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()
        if (email.isNotEmpty()) {
            prefs.edit().putString("logged_in_email", email).apply()
        }
        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
