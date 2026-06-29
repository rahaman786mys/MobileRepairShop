package com.mobilerepair.shop.ui.auth

import android.app.Activity
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.UserProfile
import com.mobilerepair.shop.databinding.FragmentLoginBinding
import com.mobilerepair.shop.utils.BackupManager
import com.mobilerepair.shop.utils.OtpManager
import com.mobilerepair.shop.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            
            if (email.isNotEmpty()) {
                Toast.makeText(requireContext(), "Welcome, $email", Toast.LENGTH_SHORT).show()
                
                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = MobileRepairApp.instance.database.userProfileDao()
                    val existing = dao.getUserProfile()
                    if (existing == null) {
                        dao.insertOrUpdate(UserProfile(
                            id = 1,
                            email = email,
                            name = account?.displayName ?: ""
                        ))
                    } else {
                        dao.insertOrUpdate(existing.copy(email = email))
                    }
                    loginSuccess()
                }
            } else {
                Toast.makeText(requireContext(), "Could not retrieve email", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val errorMsg = when(e.statusCode) {
                10 -> "Developer Error: Check SHA-1 Fingerprint in Console"
                12500 -> "Sign-in failed: Google Play Services issue"
                7 -> "Network error. Please check your internet."
                else -> "Sign-in failed (Code: ${e.statusCode})"
            }
            Log.e("LoginFragment", "Google sign in failed: ${e.statusCode}", e)
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendOtp.setOnClickListener {
            if (ValidationUtils.validatePhoneNumber(binding.tilMobileNumber)) {
                sendRealOtp(binding.etMobileNumber.text.toString())
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val code = binding.etOtp.text.toString().trim()
            if (code.length == 4) {
                verifyRealOtp(code)
            } else {
                Toast.makeText(requireContext(), "Please enter 4-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendOtp.setOnClickListener {
            sendRealOtp(binding.etMobileNumber.text.toString())
        }

        binding.btnGoogleSync.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun sendRealOtp(phone: String) {
        binding.progressBar.isVisible = true
        binding.btnSendOtp.isEnabled = false
        
        OtpManager.sendOtp(phone) { success, error ->
            activity?.runOnUiThread {
                binding.progressBar.isVisible = false
                binding.btnSendOtp.isEnabled = true
                if (success) {
                    binding.layoutMobileInput.isVisible = false
                    binding.layoutOtpInput.isVisible = true
                    binding.tvOtpSentTo.text = "OTP sent to +91 $phone"
                    Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), error ?: "Failed to send OTP", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun verifyRealOtp(code: String) {
        binding.progressBar.isVisible = true
        binding.btnVerifyOtp.isEnabled = false
        
        OtpManager.verifyOtp(code) { success, error ->
            activity?.runOnUiThread {
                binding.progressBar.isVisible = false
                binding.btnVerifyOtp.isEnabled = true
                if (success) {
                    loginSuccess()
                } else {
                    Toast.makeText(requireContext(), error ?: "Verification Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun loginSuccess() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()
        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
