package com.mobilerepair.shop.ui.master

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentMoreBinding
import com.mobilerepair.shop.utils.BackupManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoreFragment : Fragment(R.layout.fragment_more) {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardServiceMen.setOnClickListener {
            findNavController().navigate(R.id.serviceManListFragment)
        }
        binding.cardCustomers.setOnClickListener {
            findNavController().navigate(R.id.customerListFragment)
        }
        binding.cardSuppliers.setOnClickListener {
            findNavController().navigate(R.id.supplierListFragment)
        }
        binding.cardCommonFaults.setOnClickListener {
            findNavController().navigate(R.id.commonFaultsFragment)
        }
        binding.cardLogout.setOnClickListener {
            logout()
        }

        binding.cardCloudSync.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null) {
                BackupManager.syncWithGoogleDrive(requireContext(), account.email ?: "Backup")
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }

        // Load counts
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.serviceManDao().getAllServiceMen().collectLatest { list ->
                binding.tvServiceMenCount.text = "${list.size} technicians"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.customerDao().getAllCustomers().collectLatest { list ->
                binding.tvCustomersCount.text = "${list.size} customers"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.supplierDao().getAllSuppliers().collectLatest { list ->
                binding.tvSuppliersCount.text = "${list.size} suppliers"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.commonFaultDao().getAllFaults().collectLatest { list ->
                binding.tvFaultsCount.text = "${list.size} fault types"
            }
        }
        
        // Version
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvAppVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Version 1.0.1"
        }
    }

    private fun logout() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", false).apply()
        
        findNavController().navigate(R.id.loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
