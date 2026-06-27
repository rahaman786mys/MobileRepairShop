package com.mobilerepair.shop.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.Sale
import com.mobilerepair.shop.data.model.Supplier
import com.mobilerepair.shop.databinding.FragmentSaleBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SaleFragment : Fragment(R.layout.fragment_sale) {

    private var _binding: FragmentSaleBinding? = null
    private val binding get() = _binding!!
    private var suppliersList: List<Supplier> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSaleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSuppliers()

        binding.btnSaveSale.setOnClickListener {
            saveSale()
        }
    }

    private fun loadSuppliers() {
        viewLifecycleOwner.lifecycleScope.launch {
            suppliersList = MobileRepairApp.instance.database.supplierDao().getAllSuppliers().first()
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                suppliersList.map { it.name + " (" + it.mobile + ")" }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSupplier.adapter = adapter
        }
    }

    private fun saveSale() {
        val itemName = binding.etItemName.text.toString().trim()
        val purchasePrice = binding.etPurchasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val salePrice = binding.etSalePrice.text.toString().toDoubleOrNull() ?: 0.0
        
        if (itemName.isEmpty()) {
            binding.etItemName.error = "Enter item name"
            return
        }
        
        if (suppliersList.isEmpty()) {
            Toast.makeText(requireContext(), "Please add a supplier first", Toast.LENGTH_SHORT).show()
            return
        }

        val supplier = suppliersList[binding.spinnerSupplier.selectedItemPosition]

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
