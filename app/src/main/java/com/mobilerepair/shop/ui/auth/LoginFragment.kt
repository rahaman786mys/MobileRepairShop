package com.mobilerepair.shop.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentLoginBinding
import com.mobilerepair.shop.utils.ValidationUtils

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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
