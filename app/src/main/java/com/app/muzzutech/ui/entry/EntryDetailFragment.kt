package com.app.muzzutech.ui.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentEntryDetailBinding
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EntryDetailFragment : Fragment(R.layout.fragment_entry_detail) {

    private var _binding: FragmentEntryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entryId = arguments?.getLong("entryId", 0) ?: 0

        // Load entry data
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairApp.instance.repairRepository.getEntryByIdFlow(entryId).collectLatest { entry ->
                    if (entry != null) {
                        binding.tvDetailCustomer.text = "Customer: ${entry.customerName}"
                        binding.tvDetailMobile.text = "Mobile: ${entry.customerMobile}"
                        binding.tvDetailCity.text = "City: ${entry.customerCity}"
                        binding.tvDetailDealer.text = "Dealer: ${entry.dealerName.ifEmpty { "N/A" }}"
                        binding.tvDetailServiceMan.text = "Service Man ID: ${entry.serviceManId}"
                        binding.tvDetailDate.text = "Entry: ${DateUtils.formatDateTime(entry.createdAt)}"
                        binding.tvDetailFault.text = "Fault: ${entry.faultDetected.ifEmpty { "Not inspected" }}"
                        binding.tvDetailCharge.text = "Charge: ₹ ${String.format("%.0f", entry.chargeAmount)}"
                        binding.tvDetailStatus.text = "Status: ${entry.workStatus}"
                    }
                }
            }
        }

        // Workflow buttons
        binding.btnInspect.setOnClickListener {
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.inspectionFragment, bundle)
        }
        binding.btnQuotation.setOnClickListener {
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.quotationFragment, bundle)
        }
        binding.btnSpareParts.setOnClickListener {
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.sparePartsFragment, bundle)
        }
        binding.btnHandover.setOnClickListener {
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.handoverFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
