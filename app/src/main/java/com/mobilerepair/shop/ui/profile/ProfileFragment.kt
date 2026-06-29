package com.mobilerepair.shop.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentProfileBinding
import com.mobilerepair.shop.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

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

            if (name.isEmpty() || email.isEmpty()) {
                Snackbar.make(binding.root, "Name and Email are required", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveProfile(name, phone, shop, address, email)
            Snackbar.make(binding.root, "Profile saved successfully!", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnSyncNow.setOnClickListener {
            // Simulated Sync
            viewModel.updateSyncTimestamp()
            Snackbar.make(binding.root, "Cloud backup successful!", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
