package com.app.muzzutech.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.data.model.Supplier
import com.app.muzzutech.databinding.FragmentSaleBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SaleFragment : Fragment(R.layout.fragment_sale) {

    private var _binding: FragmentSaleBinding? = null
    private val binding get() = _binding!!
    private var suppliersList = listOf<Supplier>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSaleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSuppliers()

        binding.btnSaveSale.setOnClickListener { saveSale() }
        binding.btnAddSupplierQuick.setOnClickListener {
            findNavController().navigate(R.id.supplierAddFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSuppliers()
    }

    private fun loadSuppliers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairApp.instance.database.supplierDao().getAllSuppliers().collectLatest { suppliers ->
                    suppliersList = suppliers
                    val names = suppliers.map { "${it.name} (${it.mobile})" }.toMutableList()
                    names.add(0, "Select Supplier *")
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        names
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerSupplier.adapter = adapter
                }
            }
        }
    }

    private fun saveSale() {
        val itemName = binding.etItemName.text.toString().trim()
        val purchasePrice = binding.etPurchasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val salePrice = binding.etSalePrice.text.toString().toDoubleOrNull() ?: 0.0
        val selectedPos = binding.spinnerSupplier.selectedItemPosition

        if (itemName.isEmpty()) {
            binding.etItemName.error = "Enter item name"
            return
        }

        if (selectedPos <= 0 || suppliersList.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a supplier (or add new)", Toast.LENGTH_SHORT).show()
            return
        }

        val supplier = suppliersList[selectedPos - 1]

        viewLifecycleOwner.lifecycleScope.launch {
            val sale = Sale(
                itemName = itemName,
                supplierId = supplier.mobile,
                purchasePrice = purchasePrice,
                salePrice = salePrice
            )
            MobileRepairApp.instance.database.saleDao().insert(sale)
            Toast.makeText(requireContext(), "Sale Recorded!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
