package com.app.muzzutech.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.ValidationUtils

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
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) {
                sendOtpSimulation(phone)
            } else {
                binding.tilMobileNumber.error = "Enter valid 10-digit number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp == "123456" || otp == "000000") { // Demo OTPs
                loginSuccess()
            } else {
                Toast.makeText(requireContext(), "Invalid OTP. Use 123456", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendOtp.setOnClickListener {
            Toast.makeText(requireContext(), "OTP Resent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendOtpSimulation(mobile: String) {
        binding.progressBar.isVisible = true
        binding.btnSendOtp.isEnabled = false
        
        view?.postDelayed({
            binding.progressBar.isVisible = false
            binding.layoutMobileInput.isVisible = false
            binding.layoutOtpInput.isVisible = true
            binding.tvOtpSentTo.text = "Demo OTP sent to +91 $mobile"
            Toast.makeText(requireContext(), "Use Demo OTP: 123456", Toast.LENGTH_LONG).show()
        }, 1000)
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
