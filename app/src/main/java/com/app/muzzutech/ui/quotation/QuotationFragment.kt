package com.app.muzzutech.ui.quotation

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.adapter.CommonFaultAdapter
import com.app.muzzutech.databinding.FragmentQuotationBinding
import com.app.muzzutech.utils.NotificationUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuotationFragment : Fragment(R.layout.fragment_quotation) {

    private var _binding: FragmentQuotationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuotationViewModel by viewModels()
    private lateinit var faultAdapter: CommonFaultAdapter

    private var entryId: Long = 0
    private var selectedFault: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuotationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0
        viewModel.loadEntry(entryId)

        setupFaultsList()
        binding.btnSaveQuotation.setOnClickListener { saveQuotation() }
        observeData()
    }

    private fun setupFaultsList() {
        faultAdapter = CommonFaultAdapter { fault ->
            selectedFault = fault.faultName
            binding.tvFaultDetected.text = "Selected: $selectedFault"
        }
        binding.rvCommonFaults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faultAdapter
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.database.commonFaultDao().getAllFaults().collectLatest {
                faultAdapter.submitList(it)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entry.collectLatest { entry ->
                    if (entry != null && selectedFault.isEmpty()) {
                        selectedFault = entry.faultDetected
                        binding.tvFaultDetected.text = "Fault: $selectedFault"
                    }
                }
            }
        }
    }

    private fun saveQuotation() {
        val chargeText = binding.etChargeAmount.text.toString().trim()
        val advanceText = binding.etAdvanceAmount.text.toString().trim()

        if (chargeText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter charge amount", Snackbar.LENGTH_SHORT).show()
            return
        }

        val charge = chargeText.toDoubleOrNull() ?: 0.0
        val advance = advanceText.toDoubleOrNull() ?: 0.0

        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.repairRepository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    faultDetected = selectedFault,
                    chargeAmount = charge,
                    advanceAmount = advance,
                    quotationDate = System.currentTimeMillis(),
                    quotationDone = true
                )
                MobileRepairApp.instance.repairRepository.update(updated)
                
                // Notify Customer
                NotificationUtils.sendRepairStartedWhatsApp(requireContext(), updated)
                
                Snackbar.make(binding.root, "Work Started & Notified!", Snackbar.LENGTH_SHORT).show()
                val bundle = Bundle().apply { putLong("entryId", entryId) }
                findNavController().navigate(R.id.sparePartsFragment, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
