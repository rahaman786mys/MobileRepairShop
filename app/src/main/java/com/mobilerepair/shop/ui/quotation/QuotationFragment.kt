package com.mobilerepair.shop.ui.quotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentQuotationBinding
import com.mobilerepair.shop.utils.AIAnalyzer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuotationFragment : Fragment(R.layout.fragment_quotation) {

    private var _binding: FragmentQuotationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuotationViewModel by viewModels()

    private var entryId: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuotationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0
        viewModel.loadEntry(entryId)

        binding.btnSaveQuotation.setOnClickListener { saveQuotation() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entry.collectLatest { entry ->
                    if (entry != null) {
                        binding.tvFaultDetected.text = "Fault: ${entry.faultDetected}"
                        val aiEstimate = AIAnalyzer.estimateRepairCost(entry.faultDetected, listOf())
                        binding.tvAICostEstimate.text = "AI Estimate: ₹ ${String.format("%.0f", aiEstimate)}"
                    }
                }
            }
        }
    }

    private fun saveQuotation() {
        val chargeText = binding.etChargeAmount.text.toString().trim()
        val advanceText = binding.etAdvanceAmount.text.toString().trim()

        if (chargeText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter charge amount", Snackbar.LENGTH_LONG).show()
            return
        }

        val charge = chargeText.toDoubleOrNull() ?: 0.0
        val advance = advanceText.toDoubleOrNull() ?: 0.0

        viewModel.saveQuotation(entryId, charge, advance)
        Snackbar.make(binding.root, "Quotation saved!", Snackbar.LENGTH_SHORT).show()

        val bundle = Bundle().apply { putLong("entryId", entryId) }
        findNavController().navigate(R.id.sparePartsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
