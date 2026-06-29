package com.app.muzzutech.ui.auth

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
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.UserProfile
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.BackupManager
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
                binding.progressBar.isVisible = true
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
                    
                    Toast.makeText(requireContext(), "Welcome, $email", Toast.LENGTH_SHORT).show()
                    BackupManager.syncWithGoogleDrive(requireContext(), email)
                    loginSuccess()
                }
            } else {
                Toast.makeText(requireContext(), "Could not retrieve email. Check console settings.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val errorMsg = when(e.statusCode) {
                10 -> "DEVELOPER_ERROR (10): Register your SHA-1 in Google/Firebase Console."
                12500 -> "Sign-in failed: Google Play Services mismatch or missing Client ID."
                7 -> "Network error. Please check your internet."
                else -> "Sign-in failed (Code: ${e.statusCode})"
            }
            Log.e("LoginFragment", "Google sign in failed: ${e.statusCode}", e)
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            binding.progressBar.isVisible = false
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
    }

    private fun signInWithGoogle() {
        binding.progressBar.isVisible = true
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
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
