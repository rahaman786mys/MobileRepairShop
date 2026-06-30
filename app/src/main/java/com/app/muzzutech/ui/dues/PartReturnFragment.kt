package com.app.muzzutech.ui.dues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.PartReturn
import com.app.muzzutech.databinding.FragmentPartReturnBinding
import kotlinx.coroutines.launch

class PartReturnFragment : Fragment(R.layout.fragment_part_return) {

    private var _binding: FragmentPartReturnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPartReturnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupReasonSpinner()
        loadSupplierParts()
        setupListeners()
    }

    private fun setupReasonSpinner() {
        val reasons = listOf("Defective", "Wrong Item", "Not Needed", "Damaged", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reasons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerReturnReason.adapter = adapter
    }

    private fun loadSupplierParts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = MobileRepairApp.instance.database
            val parts = db.sparePartPurchaseDao().getAllPurchases()
            parts.collect { list ->
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    list.map { "${it.partName} (₹${it.purchasePrice}) - ${it.supplierName}" }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSelectPart.adapter = adapter
                if (list.isNotEmpty()) {
                    binding.tvSupplierInfo.text = "Supplier: ${list[0].supplierName} (${list[0].supplierId})"
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnReturnPart.setOnClickListener {
            val partName = binding.etPartName.text.toString().trim()
            val refundStr = binding.etRefundAmount.text.toString().trim()
            val refund = refundStr.toDoubleOrNull() ?: 0.0

            if (partName.isEmpty()) {
                Snackbar.make(binding.root, "Enter part name", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val db = MobileRepairApp.instance.database
                val parts = db.sparePartPurchaseDao().getAllPurchases()
                val partsList = kotlinx.coroutines.flow.first(parts)

                val selectedPos = binding.spinnerSelectPart.selectedItemPosition
                if (selectedPos >= 0 && selectedPos < partsList.size) {
                    val selectedPart = partsList[selectedPos]
                    val reason = binding.spinnerReturnReason.selectedItem.toString()

                    val partReturn = PartReturn(
                        supplierId = selectedPart.supplierId,
                        supplierName = selectedPart.supplierName,
                        partName = partName,
                        returnReason = reason,
                        refundAmount = refund
                    )
                    db.partReturnDao().insert(partReturn)
                    Snackbar.make(binding.root, "Part return recorded!", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
