package com.app.muzzutech.ui.master.servicemen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.R
import com.app.muzzutech.auth.FirestoreSyncManager
import com.app.muzzutech.auth.WorkerCredentialManager
import com.app.muzzutech.auth.WorkerCredentialShare
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.databinding.FragmentServiceManListBinding
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ServiceManListFragment : Fragment(R.layout.fragment_service_man_list) {

    private var _binding: FragmentServiceManListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServiceManViewModel by viewModels()
    private val manager by lazy { WorkerCredentialManager() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiceManListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddServiceMan.setOnClickListener {
            findNavController().navigate(R.id.serviceManAddFragment)
        }

        binding.rvServiceMen.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.serviceMen.collectLatest { men ->
                    binding.tvEmptyState.visibility = if (men.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvServiceMen.adapter = TechnicianAdapter(men)
                }
            }
        }
    }

    private fun currentOwnerId(): String {
        val authPrefs = SecurePrefs.authPrefs(requireContext()).getString("logged_in_firebase_uid", "") ?: ""
        if (authPrefs.isNotEmpty()) return authPrefs
        return SecurePrefs.appSettings(requireContext()).getString("auth_firebase_uid", "") ?: ""
    }

    // ── Technician list item ──

    private inner class TechnicianAdapter(private val items: List<ServiceMan>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
            ) {}

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val sm = items[position]
            holder.itemView.findViewById<TextView>(R.id.text1).text = sm.name
            val loginStatus = when {
                !sm.canLogin -> "Login Revoked"
                sm.passwordHash.isNotBlank() -> "Login Active"
                else -> "No Login Yet"
            }
            holder.itemView.findViewById<TextView>(R.id.text2).text = "${sm.designation} | ${sm.mobile} • $loginStatus"
            holder.itemView.setOnClickListener { showTechActions(sm) }
        }
    }

    // ── Actions dialog ──

    private fun showTechActions(worker: ServiceMan) {
        val items = mutableListOf<String>()
        val hasLogin = worker.passwordHash.isNotBlank()

        if (!hasLogin) {
            items.add("Generate Worker Login")
        } else {
            items.add("Share Credentials")
            items.add("Regenerate Password")
        }
        items.add("Edit Details")
        if (hasLogin && worker.canLogin) items.add("Terminate Session")
        items.add("Delete Technician")

        AlertDialog.Builder(requireContext())
            .setTitle(worker.name)
            .setItems(items.toTypedArray()) { _, which ->
                when (items[which]) {
                    "Generate Worker Login" -> generateLogin(worker)
                    "Share Credentials" -> shareCredentials(worker)
                    "Regenerate Password" -> regeneratePassword(worker)
                    "Edit Details" -> editTechnician(worker)
                    "Terminate Session" -> confirmTerminate(worker)
                    "Delete Technician" -> confirmDelete(worker)
                }
            }
            .show()
    }

    // ── Generate Login ──

    private fun generateLogin(worker: ServiceMan) {
        lifecycleScope.launch {
            val password = manager.regeneratePassword(worker.id)
            if (!isAdded || _binding == null) return@launch
            if (password == null) {
                Snackbar.make(binding.root, "Could not generate login", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val updatedWorker = MobileRepairAppDb().serviceManDao().getServiceManById(worker.id)
            if (updatedWorker != null) {
                FirestoreSyncManager.syncWorker(updatedWorker, currentOwnerId())
            }
            showCredentialDialog(worker, password)
        }
    }

    private fun MobileRepairAppDb() = com.app.muzzutech.MobileRepairApp.instance.database

    // ── Share ──

    private fun shareCredentials(worker: ServiceMan) {
        lifecycleScope.launch {
            val regen = manager.regeneratePassword(worker.id)
            if (!isAdded || _binding == null) return@launch
            if (regen == null) {
                Snackbar.make(binding.root, "Could not retrieve credentials", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            FirestoreSyncManager.updateWorkerStatus(worker.id, true, worker)
            showCredentialDialog(worker, regen)
        }
    }

    // ── Regenerate ──

    private fun regeneratePassword(worker: ServiceMan) {
        lifecycleScope.launch {
            val newPassword = manager.regeneratePassword(worker.id)
            if (!isAdded || _binding == null) return@launch
            if (newPassword == null) {
                Snackbar.make(binding.root, "Could not generate a new password", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            FirestoreSyncManager.updateWorkerStatus(worker.id, true, worker)
            showCredentialDialog(worker, newPassword)
        }
    }

    // ── Terminate ──

    private fun confirmTerminate(worker: ServiceMan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Terminate ${worker.name}?")
            .setMessage("This worker will be logged out and their password will stop working until you generate a new login.")
            .setPositiveButton("Terminate") { _, _ ->
                lifecycleScope.launch {
                    val ok = manager.terminateAccess(worker.id)
                    if (!isAdded || _binding == null) return@launch
                    if (ok) {
                        FirestoreSyncManager.updateWorkerStatus(worker.id, false, worker)
                        Snackbar.make(binding.root, "${worker.name}'s access terminated", Snackbar.LENGTH_LONG).show()
                    } else {
                        Snackbar.make(binding.root, "Could not terminate access", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Credential dialog + share ──

    private fun showCredentialDialog(worker: ServiceMan, password: String) {
        if (!isAdded || _binding == null) return
        val message = "Worker: ${worker.name}\nPhone (Worker ID): ${worker.mobile}\nPassword: $password\n\nShare this with the worker now."
        AlertDialog.Builder(requireContext())
            .setTitle("Worker Login Created")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Done") { _, _ -> }
            .setNeutralButton("Share") { _, _ ->
                try {
                    startActivity(WorkerCredentialShare.buildShareIntent(worker.mobile, password))
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "No app available to share", Snackbar.LENGTH_LONG).show()
                }
            }
            .show()
    }

    // ── Edit / Delete ──

    private fun editTechnician(worker: ServiceMan) {
        val bundle = Bundle().apply {
            putLong("serviceManId", worker.id)
            putString("serviceManName", worker.name)
            putString("serviceManMobile", worker.mobile)
            putString("serviceManEmail", worker.email)
            putString("serviceManEmpId", worker.employeeId)
            putString("serviceManDesignation", worker.designation)
            putLong("monthlySalary", worker.monthlySalary)
            putLong("perDaySalary", worker.perDaySalary)
        }
        findNavController().navigate(R.id.serviceManAddFragment, bundle)
    }

    private fun confirmDelete(worker: ServiceMan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Technician")
            .setMessage("Delete ${worker.name}? This will also revoke their login if active.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    manager.terminateAccess(worker.id)
                    viewModel.delete(worker)
                }
                Snackbar.make(binding.root, "${worker.name} deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
