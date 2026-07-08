package com.app.muzzutech.ui.master

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentMoreBinding
import com.app.muzzutech.utils.BackupManager
import com.app.muzzutech.utils.UpdateManager
import com.app.muzzutech.utils.crpto.SecurePrefs
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
        binding.cardInventory.setOnClickListener {
            findNavController().navigate(R.id.inventoryFragment)
        }
        binding.cardPayroll.setOnClickListener {
            findNavController().navigate(R.id.payrollFragment)
        }
        binding.cardExpenses.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }
        binding.cardAccountProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        binding.cardLogout.setOnClickListener {
            logout()
        }

        binding.btnCheckUpdate.setOnClickListener {
            Toast.makeText(requireContext(), "Checking for updates...", Toast.LENGTH_SHORT).show()
            UpdateManager.checkForUpdates(requireActivity() as androidx.appcompat.app.AppCompatActivity)
        }

        binding.cardCloudSync.setOnClickListener {
            Toast.makeText(requireContext(), "Google Drive sync coming soon", Toast.LENGTH_SHORT).show()
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
                    
                    // Update sync status
                    val statusText = when (profile.lastSyncStatus) {
                        "SUCCESS" -> "Last synced: ${com.app.muzzutech.utils.DateUtils.formatDateTime(profile.lastSyncTimestamp)}"
                        "FAILED" -> "Last sync failed"
                        else -> "Never synced"
                    }
                    binding.tvSyncStatus.text = statusText
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

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvAppVersion.text = "MuZZu Tech Professional v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "MuZZu Tech Professional"
        }

        setupSettings()
    }

    private fun setupSettings() {
        val prefs = SecurePrefs.appSettings(requireContext())
        binding.switchBiometric.isChecked = prefs.getBoolean("biometric_enabled", false)
        binding.switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isAdded) {
                prefs.edit().putBoolean("biometric_enabled", isChecked).apply()
                val status = if (isChecked) "enabled" else "disabled"
                Toast.makeText(requireContext(), "Biometric security $status", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isAdded) {
                prefs.edit().putBoolean("dark_mode", isChecked).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    private fun showBackupOptions() {
        val options = arrayOf("Save to Downloads (Locally)", "Share Backup to Other Apps")
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data Backup")
            .setItems(options) { _, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (which == 0) {
                        BackupManager.exportLocally(requireContext())
                    } else {
                        BackupManager.shareBackup(requireContext())
                    }
                }
            }
            .show()
    }

    private fun restoreBackup(uri: android.net.Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restore Data?")
            .setMessage("This will replace ALL current data with the backup. Current data will be lost. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val success = BackupManager.importDatabase(requireContext(), uri)
                        if (success) {
                            Toast.makeText(requireContext(), "Restore successful! Restarting app...", Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.dashboardFragment)
                        } else {
                            Toast.makeText(requireContext(), "Restore failed. Invalid backup file.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        val prefs = SecurePrefs.authPrefs(requireContext())
        prefs.edit().putBoolean("is_logged_in", false).apply()
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
