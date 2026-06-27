package com.mobilerepair.shop.ui.handover

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
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentHandoverBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
            binding.layoutSplitPayment.visibility = if (checkedId == R.id.radioBoth) View.VISIBLE else View.GONE
        }

        binding.btnCompleteHandover.setOnClickListener { completeHandover() }
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
            else -> {
                Snackbar.make(binding.root, "Select payment mode", Snackbar.LENGTH_LONG).show()
                return
            }
        }

        val cashAmount = if (paymentMode == "Both") {
            binding.etCashAmount.text.toString().toDoubleOrNull() ?: 0.0
        } else if (paymentMode == "Cash") finalAmount else 0.0

        val onlineAmount = if (paymentMode == "Both") {
            binding.etOnlineAmount.text.toString().toDoubleOrNull() ?: 0.0
        } else if (paymentMode == "Online") finalAmount else 0.0

        viewModel.completeHandover(entryId, finalAmount, paymentMode, cashAmount, onlineAmount)
        Snackbar.make(binding.root, "✅ Handover Complete!", Snackbar.LENGTH_LONG).show()

        // Navigate back to dashboard
        findNavController().popBackStack(R.id.dashboardFragment, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
