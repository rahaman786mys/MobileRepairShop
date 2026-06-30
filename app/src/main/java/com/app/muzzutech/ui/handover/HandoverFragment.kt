package com.app.muzzutech.ui.handover

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

class HandoverFragment : Fragment(R.layout.fragment_handover) {

    private var _binding: FragmentHandoverBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HandoverViewModel by viewModels()

    private var entryId: Long = 0

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
        
        binding.btnGenerateInvoice.setOnClickListener {
            generateAndShareInvoice()
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
                        binding.tvSummaryCharge.text = "Charge: ₹ ${String.format("%.0f", entry.chargeAmount)}"
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
        val finalAmountText = binding.etFinalAmount.text.toString().trim()
        if (finalAmountText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter final amount", Snackbar.LENGTH_LONG).show()
            return
        }

        val finalAmount = finalAmountText.toDoubleOrNull() ?: 0.0
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
            binding.etCashAmount.text.toString().toDoubleOrNull() ?: 0.0
        } else if (!isPayLater && paymentMode == "Cash") finalAmount else 0.0

        val onlineAmount = if (!isPayLater && paymentMode == "Both") {
            binding.etOnlineAmount.text.toString().toDoubleOrNull() ?: 0.0
        } else if (!isPayLater && paymentMode == "Online") finalAmount else 0.0

        viewModel.completeHandover(entryId, finalAmount, paymentMode, cashAmount, onlineAmount)

        // Create payment record for customer if Pay Later
        if (isPayLater) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.entry.value?.let { entry ->
                    val payment = Payment(
                        personType = "CUSTOMER",
                        personMobile = entry.customerMobile,
                        personName = entry.customerName,
                        description = "Repair - ${entry.deviceBrand} ${entry.deviceModel} (${entry.faultDetected})",
                        totalAmount = finalAmount,
                        paidAmount = 0.0,
                        dueAmount = finalAmount,
                        status = "UNPAID",
                        linkedEntryId = entry.id
                    )
                    MobileRepairApp.instance.database.paymentDao().insert(payment)
                }
            }
        }

        // Notify Customer via WhatsApp & Show Print button
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entry.value?.let { entry ->
                val updatedEntry = entry.copy(
                    finalAmount = finalAmount,
                    paymentMode = paymentMode,
                    handoverDone = true
                )
                val partsText = viewModel.parts.value.joinToString(", ") { it.partName }
                NotificationUtils.sendHandoverSummaryWhatsApp(requireContext(), updatedEntry, partsText)
            }
        }

        binding.btnGenerateInvoice.visibility = View.VISIBLE
        binding.btnCompleteHandover.isEnabled = false
        
        Snackbar.make(binding.root, "✅ Handover Complete! Notification Sent.", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
