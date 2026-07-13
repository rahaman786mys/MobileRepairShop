package com.app.muzzutech.ui.master.workeraccess

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.R
import com.app.muzzutech.auth.FirestoreSyncManager
import com.app.muzzutech.auth.WorkerCredentialManager
import com.app.muzzutech.auth.WorkerCredentialShare
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.databinding.FragmentWorkerAccessBinding
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owner-side "Manage Workers": create a login (phone = Worker ID), share the
 * generated password via the native share sheet, terminate a worker's access
 * (kills their session + invalidates the password), or generate a new password.
 *
 * Does not touch payroll or the worker dashboard — it only manages login access
 * on the ServiceMan records via [WorkerCredentialManager].
 */
class WorkerAccessListFragment : Fragment(R.layout.fragment_worker_access) {

    private var _binding: FragmentWorkerAccessBinding? = null
    private val binding get() = _binding!!

    private val manager by lazy { WorkerCredentialManager() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerAccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvWorkers.layoutManager = LinearLayoutManager(requireContext())
        binding.btnAddWorker.setOnClickListener { showAddWorkerDialog() }

        val ownerId = currentOwnerId()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairAppDb().serviceManDao().getWorkersByOwner(ownerId).collectLatest { workers ->
                    binding.tvEmptyWorkers.visibility = if (workers.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvWorkers.adapter = WorkerAdapter(workers)
                }
            }
        }
    }

    private fun currentOwnerId(): String {
        val authPrefs = SecurePrefs.authPrefs(requireContext()).getString("logged_in_firebase_uid", "") ?: ""
        if (authPrefs.isNotEmpty()) return authPrefs
        return SecurePrefs.appSettings(requireContext()).getString("auth_firebase_uid", "") ?: ""
    }

    // ── Add worker ───────────────────────────────────────────────────────────

    private fun showAddWorkerDialog() {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()

        val nameInput = EditText(ctx).apply {
            hint = "Worker name"
            inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val phoneInput = EditText(ctx).apply {
            hint = "Phone number (this is the Worker ID)"
            inputType = InputType.TYPE_CLASS_PHONE
        }
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameInput)
            addView(phoneInput)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Add Worker")
            .setView(container)
            .setPositiveButton("Create Login") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isEmpty() || phone.isEmpty()) {
                    Snackbar.make(binding.root, "Name and phone number are required", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                createWorker(name, phone)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createWorker(name: String, phone: String) {
        val ownerId = currentOwnerId()
        viewLifecycleOwner.lifecycleScope.launch {
            val credential = manager.addWorker(ownerId, name, phone)
            if (!isAdded || _binding == null) return@launch
            if (credential == null) {
                Snackbar.make(
                    binding.root,
                    "Couldn't create login. This phone may already be registered, or you've reached the 10-worker limit.",
                    Snackbar.LENGTH_LONG
                ).show()
                return@launch
            }
            // Reflect the new login in the cloud for the founder console.
            FirestoreSyncManager.syncWorker(credential.worker, ownerId)
            showCredentialDialog(credential.worker.mobile, credential.plainPassword, "Worker Login Created")
        }
    }

    // ── Credential display + native share ─────────────────────────────────────

    private fun showCredentialDialog(phone: String, password: String, title: String) {
        if (!isAdded || _binding == null) return
        val message = "Phone (Worker ID): $phone\nPassword: $password\n\n" +
            "Share this with the worker now — for security the password is not stored and won't be shown again."
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Done", null)
            .setNeutralButton("Share") { _, _ ->
                try {
                    startActivity(WorkerCredentialShare.buildShareIntent(phone, password))
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "No app available to share", Snackbar.LENGTH_LONG).show()
                }
            }
            .show()
    }

    // ── Manage existing worker (terminate / regenerate) ────────────────────────

    private fun showManageDialog(worker: ServiceMan) {
        val ctx = requireContext()
        val statusLine = if (worker.canLogin) "Login is ACTIVE" else "Login is REVOKED"
        AlertDialog.Builder(ctx)
            .setTitle(worker.name.ifEmpty { worker.mobile })
            .setMessage("Phone (Worker ID): ${worker.mobile}\n$statusLine")
            .setPositiveButton("Generate New Password") { _, _ -> regeneratePassword(worker) }
            .setNeutralButton("Terminate Session") { _, _ -> confirmTerminate(worker) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun confirmTerminate(worker: ServiceMan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Terminate ${worker.name.ifEmpty { "worker" }}?")
            .setMessage("This worker will be logged out and their password will stop working until you generate a new one. Continue?")
            .setPositiveButton("Terminate") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
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

    private fun regeneratePassword(worker: ServiceMan) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newPassword = manager.regeneratePassword(worker.id)
            if (!isAdded || _binding == null) return@launch
            if (newPassword == null) {
                Snackbar.make(binding.root, "Could not generate a new password", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            FirestoreSyncManager.updateWorkerStatus(worker.id, true, worker)
            showCredentialDialog(worker.mobile, newPassword, "New Password Generated")
        }
    }

    // ── List ───────────────────────────────────────────────────────────────────

    private inner class WorkerAdapter(private val items: List<ServiceMan>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
            ) {}

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val worker = items[position]
            holder.itemView.findViewById<TextView>(R.id.text1).text = worker.name.ifEmpty { worker.mobile }
            val status = if (worker.canLogin) "Active login" else "Login revoked"
            holder.itemView.findViewById<TextView>(R.id.text2).text = "${worker.mobile}  •  $status"
            holder.itemView.setOnClickListener { showManageDialog(worker) }
        }
    }

    private fun MobileRepairAppDb() = com.app.muzzutech.MobileRepairApp.instance.database

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
