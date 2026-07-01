package com.app.muzzutech.ui.master

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentMoreBinding
import com.app.muzzutech.utils.BackupManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoreFragment : Fragment(R.layout.fragment_more) {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private val restorePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            restoreBackup(uri)
        }
    }

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
        binding.cardInventory.setOnClickListener {
            findNavController().navigate(R.id.inventoryFragment)
        }
        binding.cardAccountProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        binding.cardLogout.setOnClickListener {
            logout()
        }

        binding.cardCloudSync.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null) {
                BackupManager.syncWithGoogleDrive(requireContext(), account.email ?: "Backup")
            } else {
                Toast.makeText(requireContext(), "Please sign in with Google in Profile first", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.profileFragment)
            }
        }

        binding.cardBackupLocal.setOnClickListener { showBackupOptions() }
        binding.cardRestoreLocal.setOnClickListener {
            restorePicker.launch("*/*")
        }

        // Load profile and counts
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.userProfileDao().getUserProfileFlow().collectLatest { profile ->
                if (profile != null) {
                    binding.tvProfileName.text = profile.name.ifEmpty { "Your Account" }
                    binding.tvProfileEmail.text = profile.email.ifEmpty { "Manage your shop details" }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.serviceManDao().getAllServiceMen().collectLatest { list ->
                binding.tvServiceMenCount.text = "${list.size} technicians"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.customerDao().getAllCustomers().collectLatest { customers ->
                MobileRepairApp.instance.database.dealerDao().getAllDealers().collectLatest { dealers ->
                    val total = customers.size + dealers.size
                    binding.tvCustomersCount.text = "$total people"
                }
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

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvAppVersion.text = "MuZZu Tech Professional v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "MuZZu Tech Professional"
        }
    }

    private fun showBackupOptions() {
        val options = arrayOf("Save to Downloads (Locally)", "Share Backup to Other Apps")
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data Backup")
            .setItems(options) { _, which ->
                if (which == 0) {
                    BackupManager.exportLocally(requireContext())
                } else {
                    BackupManager.shareBackup(requireContext())
                }
            }
            .show()
    }

    private fun restoreBackup(uri: android.net.Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restore Data?")
            .setMessage("This will replace ALL current data with the backup. Current data will be lost. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                try {
                    val success = BackupManager.importDatabase(requireContext(), uri)
                    if (success) {
                        Toast.makeText(requireContext(), "Restore successful! Restarting app...", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(requireContext(), "Restore failed. Invalid backup file.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
