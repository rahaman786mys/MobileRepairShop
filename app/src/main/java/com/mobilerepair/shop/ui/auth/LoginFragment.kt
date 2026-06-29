package com.mobilerepair.shop.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.google.android.gms.common.api.Scope
import com.mobilerepair.shop.R
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.UserProfile
import com.mobilerepair.shop.databinding.FragmentLoginBinding
import com.mobilerepair.shop.utils.BackupManager
import com.mobilerepair.shop.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                val email = account?.email ?: "user"
                Toast.makeText(requireContext(), "Signed in as $email", Toast.LENGTH_SHORT).show()
                
                // Save Initial Profile
                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = MobileRepairApp.instance.database.userProfileDao()
                    if (dao.getUserProfile() == null) {
                        dao.insertOrUpdate(UserProfile(
                            email = email,
                            name = account?.displayName ?: ""
                        ))
                    }
                }

                // Trigger Sync
                BackupManager.syncWithGoogleDrive(requireContext(), email)
                
                // Proceed to login if not already done, or just show success
                loginSuccess()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                sendOtpSimulation(binding.etMobileNumber.text.toString())
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString()
            if (otp == "123456") { // Simulation OTP
                loginSuccess()
            } else {
                Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendOtp.setOnClickListener {
            Toast.makeText(requireContext(), "OTP Resent", Toast.LENGTH_SHORT).show()
        }

        binding.btnGoogleSync.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun sendOtpSimulation(mobile: String) {
        binding.progressBar.isVisible = true
        binding.btnSendOtp.isEnabled = false
        
        // Simulate network delay
        view?.postDelayed({
            binding.progressBar.isVisible = false
            binding.layoutMobileInput.isVisible = false
            binding.layoutOtpInput.isVisible = true
            binding.tvOtpSentTo.text = "OTP sent to +91 $mobile"
            Toast.makeText(requireContext(), "OTP sent: 123456", Toast.LENGTH_LONG).show()
        }, 1500)
    }

    private fun loginSuccess() {
        // Save login state
        val prefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()

        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
