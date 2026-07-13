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
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.UpdateManager
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoreFragment : Fragment(R.layout.fragment_more) {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private var currentGoogleAccount: GoogleSignInAccount? = null

    private val restorePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            restoreBackup(uri)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            currentGoogleAccount = account
            if (pendingRestore) startRestore() else startSync()
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                10 -> "Configuration Error (Code 10). SHA-1 not registered."
                7 -> "Network error. Check internet."
                12501 -> "Sign-in cancelled."
                else -> "Sign-in failed (Code: ${e.statusCode})"
            }
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
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
        binding.cardWorkerLogins.setOnClickListener {
            findNavController().navigate(R.id.workerAccessListFragment)
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
            showSyncOptions()
        }

        binding.btnDoSync.setOnClickListener {
            ensureGoogleAccountAndSync()
        }

        binding.cardBackupLocal.setOnClickListener { showBackupOptions() }
        binding.cardRestoreLocal.setOnClickListener {
            restorePicker.launch("*/*")
        }

        // Load profile and sync status
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.userProfileDao().getUserProfileFlow().collectLatest { profile ->
                if (profile != null) {
                    binding.tvProfileName.text = profile.name.ifEmpty { "Your Account" }
                    binding.tvProfileEmail.text = profile.email.ifEmpty { "Manage your shop details" }

                    val statusText = when (profile.lastSyncStatus) {
                        "SUCCESS" -> "Last synced: ${DateUtils.formatDateTime(profile.lastSyncTimestamp)}"
                        "FAILED" -> "Last sync failed"
                        else -> getString(R.string.sync_never)
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

        val isBio = prefs.getBoolean("biometric_enabled", false)
        val isDark = prefs.getBoolean("dark_mode", false)

        binding.switchBiometric.isChecked = isBio
        binding.switchDarkMode.isChecked = isDark

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isAdded) {
                prefs.edit().putBoolean("biometric_enabled", isChecked).apply()
                val status = if (isChecked) "enabled" else "disabled"
                Toast.makeText(requireContext(), "Biometric security $status", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isAdded) {
                val current = prefs.getBoolean("dark_mode", false)
                if (isChecked != current) {
                    prefs.edit().putBoolean("dark_mode", isChecked).apply()
                    AppCompatDelegate.setDefaultNightMode(
                        if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                        else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            }
        }
    }

    // ── Google Drive Sync ─────────────────────────────────────────────────

    private fun showSyncOptions() {
        val options = arrayOf(
            getString(R.string.sync_btn_now),
            getString(R.string.sync_btn_restore)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.ui_google_cloud_sync))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ensureGoogleAccountAndSync()
                    1 -> ensureGoogleAccountAndRestore()
                }
            }
            .show()
    }

    private fun ensureGoogleAccountAndSync() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            currentGoogleAccount = account
            ensureDriveScopeAndExecute { startSync() }
        } else {
            signInWithGoogle()
        }
    }

    private fun ensureGoogleAccountAndRestore() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            currentGoogleAccount = account
            ensureDriveScopeAndExecute { startRestore() }
        } else {
            signInWithGoogle(restoreAfterSignIn = true)
        }
    }

    private var pendingRestore = false

    private fun signInWithGoogle(restoreAfterSignIn: Boolean = false) {
        pendingRestore = restoreAfterSignIn
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun ensureDriveScopeAndExecute(onReady: () -> Unit) {
        val account = currentGoogleAccount ?: return
        val hasDriveScope = account.grantedScopes?.any {
            it.scopeUri == "https://www.googleapis.com/auth/drive.appdata"
        } ?: false

        if (!hasDriveScope) {
            pendingRestore = onReady == ::startRestore
            signInWithGoogle(restoreAfterSignIn = pendingRestore)
        } else {
            onReady()
        }
    }

    private fun startSync() {
        val account = currentGoogleAccount ?: return
        val email = account.email ?: ""

        binding.syncProgress.visibility = View.VISIBLE
        binding.btnDoSync.visibility = View.GONE
        binding.tvSyncStatus.text = getString(R.string.sync_in_progress)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = BackupManager.getAccessToken(requireContext(), account)
                if (token == null) {
                    binding.tvSyncStatus.text = getString(R.string.sync_failed_message, "auth error")
                    Snackbar.make(binding.root, "Failed to get access token", Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                val (success, detail) = BackupManager.syncWithGoogleDrive(requireContext(), email, token)
                binding.tvSyncStatus.text = if (success) {
                    val ts = MobileRepairApp.instance.database.userProfileDao()
                        .getUserProfile()?.lastSyncTimestamp ?: 0
                    getString(R.string.sync_success, DateUtils.formatDateTime(ts))
                } else {
                    getString(R.string.sync_failed_message, detail)
                }

                Snackbar.make(
                    binding.root,
                    if (success) R.string.sync_success_message else R.string.sync_failed_message,
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                binding.tvSyncStatus.text = getString(R.string.sync_failed_message, e.message ?: "error")
                Snackbar.make(binding.root, "Sync failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.syncProgress.visibility = View.GONE
                binding.btnDoSync.visibility = View.VISIBLE
            }
        }
    }

    private fun startRestore() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.sync_confirm_restore)
            .setMessage(R.string.sync_confirm_restore_message)
            .setPositiveButton("Restore") { _, _ ->
                val account = currentGoogleAccount ?: return@setPositiveButton

                binding.syncProgress.visibility = View.VISIBLE
                binding.btnDoSync.visibility = View.GONE
                binding.tvSyncStatus.text = getString(R.string.sync_restore_in_progress)

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val token = BackupManager.getAccessToken(requireContext(), account)
                        if (token == null) {
                            Snackbar.make(binding.root, "Failed to get access token", Snackbar.LENGTH_LONG).show()
                            return@launch
                        }

                        val (success, detail) = BackupManager.restoreFromGoogleDrive(requireContext(), token)
                        if (success) {
                            Toast.makeText(requireContext(), R.string.restore_success_message, Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.dashboardFragment)
                        } else {
                            binding.tvSyncStatus.text = getString(R.string.restore_failed_message, detail)
                            Snackbar.make(binding.root, "Restore failed: $detail", Snackbar.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        binding.tvSyncStatus.text = getString(R.string.restore_failed_message, e.message ?: "error")
                        Snackbar.make(binding.root, "Restore failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                    } finally {
                        binding.syncProgress.visibility = View.GONE
                        binding.btnDoSync.visibility = View.VISIBLE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Local Backup ──────────────────────────────────────────────────────

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
        // End the local session (keep the owner's data; this is a session logout).
        SecurePrefs.authPrefs(requireContext()).edit()
            .putBoolean("is_logged_in", false)
            .remove("logged_in_identifier")
            .remove("logged_in_firebase_uid")
            .apply()

        // End the Firebase + Google sessions so the next sign-in is a fresh choice.
        try { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(requireActivity(), gso).signOut()
        } catch (_: Exception) {}

        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

        // Go to the login screen and clear the entire back stack so Back can't
        // re-enter the app after logout.
        val navController = findNavController()
        navController.navigate(
            R.id.loginFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
