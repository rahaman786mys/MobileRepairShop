package com.app.muzzutech.ui.handover

import android.app.AlertDialog
import android.widget.Toast
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.databinding.FragmentHandoverBinding
import com.app.muzzutech.utils.InvoiceGenerator
import com.app.muzzutech.utils.NotificationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import kotlin.math.roundToLong

class HandoverFragment : Fragment(R.layout.fragment_handover) {

    private var _binding: FragmentHandoverBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HandoverViewModel by viewModels()

    private var entryId: Long = 0
    private var isCompleting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHandoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0
        viewModel.loadEntry(entryId)

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        // Payment mode toggle
        binding.radioGroupPayment.setOnCheckedChangeListener { _, checkedId ->
            val showSplit = checkedId == R.id.radioBoth
            binding.layoutSplitPayment.visibility = if (showSplit) View.VISIBLE else View.GONE
        }

        binding.btnCompleteHandover.setOnClickListener { completeHandover() }

        binding.btnCancelWork.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancel & Delete Work?")
                .setMessage("Are you sure you want to cancel this work? It will be moved to drafts.")
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    cancelWork()
                }
                .setNegativeButton("No", null)
                .show()
        }
        
        binding.btnGenerateInvoice.setOnClickListener {
            generateAndShareInvoice()
        }
    }

    private fun cancelWork() {
        viewModel.cancelWork(entryId) {
            Toast.makeText(requireContext(), "Work Cancelled — advance refunded", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack(R.id.dashboardFragment, false)
        }
    }

    private fun generateAndShareInvoice() {
        val entry = viewModel.entry.value ?: return
        val parts = viewModel.parts.value
        
        val pdfFile = InvoiceGenerator.generateInvoice(requireContext(), entry, parts)
        if (pdfFile != null) {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Invoice"))
        } else {
            Snackbar.make(binding.root, "Failed to generate PDF", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entry.collectLatest { entry ->
                    if (entry != null) {
                        binding.tvSummaryFault.text = "Fault: ${entry.faultDetected}"
                        binding.tvSummaryCharge.text = "Charge: ${com.app.muzzutech.utils.PriceUtils.formatPrice(entry.chargeAmount)}"
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.parts.collectLatest { parts ->
                    val partsText = if (parts.isEmpty()) "None" else parts.joinToString(", ") { it.partName }
                    binding.tvSummaryParts.text = "Parts Used: $partsText"
                }
            }
        }
    }

    private fun completeHandover() {
        if (isCompleting) return
        val finalAmountText = binding.etFinalAmount.text.toString().trim()
        if (finalAmountText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter final amount", Snackbar.LENGTH_LONG).show()
            return
        }

        val finalAmount = ((finalAmountText.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        val selectedPaymentId = binding.radioGroupPayment.checkedRadioButtonId
        val paymentMode = when (selectedPaymentId) {
            R.id.radioCash -> "Cash"
            R.id.radioOnline -> "Online"
            R.id.radioBoth -> "Both"
            R.id.radioPayLater -> "Pay Later"
            else -> {
                Snackbar.make(binding.root, "Select payment mode", Snackbar.LENGTH_LONG).show()
                return
            }
        }

        val isPayLater = paymentMode == "Pay Later"
        val cashAmount = if (!isPayLater && paymentMode == "Both") {
            ((binding.etCashAmount.text.toString().toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        } else if (!isPayLater && paymentMode == "Cash") finalAmount else 0L

        val onlineAmount = if (!isPayLater && paymentMode == "Both") {
            ((binding.etOnlineAmount.text.toString().toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        } else if (!isPayLater && paymentMode == "Online") finalAmount else 0L

        if (paymentMode == "Both") {
            val combined = cashAmount + onlineAmount
            if (combined != finalAmount) {
                Snackbar.make(binding.root, "Cash + Online must equal Total (₹${finalAmount / 100.0})", Snackbar.LENGTH_LONG).show()
                return
            }
        }

        if (finalAmount < 0L) {
            Snackbar.make(binding.root, "Final amount cannot be negative", Snackbar.LENGTH_LONG).show()
            return
        }

        isCompleting = true
        binding.btnCompleteHandover.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completeHandover(entryId, finalAmount, paymentMode, cashAmount, onlineAmount)
            binding.btnGenerateInvoice.visibility = View.VISIBLE
            Snackbar.make(binding.root, "✅ Handover Complete!", Snackbar.LENGTH_LONG).show()
            findNavController().popBackStack(R.id.dashboardFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
