package com.app.muzzutech.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentProfileBinding
import com.app.muzzutech.utils.BackupManager
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                binding.etProfileEmail.setText(email)
                if (binding.etProfileName.text.isNullOrEmpty()) {
                    binding.etProfileName.setText(account?.displayName)
                }
                Toast.makeText(requireContext(), "Google Account Linked: $email", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val msg = when(e.statusCode) {
                10 -> "Configuration Error (Code 10). Please register SHA-1 in Google Console."
                7 -> "Network error. Check internet."
                else -> "Link failed (Code: ${e.statusCode})"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            Log.e("ProfileFragment", "Google Link Error: ${e.statusCode}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeProfile()
        setupListeners()
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileFlow.collectLatest { profile ->
                    profile?.let {
                        binding.etProfileName.setText(it.name)
                        binding.etProfileEmail.setText(it.email)
                        binding.etProfilePhone.setText(it.phone)
                        binding.etShopName.setText(it.shopName)
                        binding.etShopAddress.setText(it.shopAddress)
                        
                        if (it.lastSyncTimestamp > 0) {
                            binding.tvLastSync.text = "Last Sync: ${DateUtils.formatDateTime(it.lastSyncTimestamp)}"
                        } else {
                            binding.tvLastSync.text = "Last Sync: Never"
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etProfileName.text.toString().trim()
            val email = binding.etProfileEmail.text.toString().trim()
            val phone = binding.etProfilePhone.text.toString().trim()
            val shop = binding.etShopName.text.toString().trim()
            val address = binding.etShopAddress.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Full Name is required", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveProfile(name, phone, shop, address, email)
            Snackbar.make(binding.root, "Profile saved successfully!", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnLinkGoogle.setOnClickListener {
            linkGoogleAccount()
        }

        binding.btnSyncNow.setOnClickListener {
            val email = binding.etProfileEmail.text.toString().trim()
            if (email.isEmpty()) {
                Snackbar.make(binding.root, "Please enter an email for backup", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            BackupManager.syncWithGoogleDrive(requireContext(), email)
            viewModel.updateSyncTimestamp()
            Snackbar.make(binding.root, "Cloud backup initiated!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun linkGoogleAccount() {
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestServerAuthCode(webClientId)
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
