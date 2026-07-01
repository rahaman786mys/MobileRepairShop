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

    // Removed onResume() override that was triggering loadSuppliers() repeatedly
    // The coroutine inside loadSuppliers() (using repeatOnLifecycle) handles lifecycles correctly.

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
            val db = MobileRepairApp.instance.database
            
            // 1. Record the Sale
            val sale = Sale(
                itemName = itemName,
                supplierId = supplier.mobile,
                supplierName = supplier.name,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                customerPaid = salePrice, // Assuming full payment for Direct Sale for now
                customerDue = 0.0
            )
            db.saleDao().insert(sale)
            
            // 2. Record the Cash Inflow (Revenue)
            val transactionIn = com.app.muzzutech.data.model.PaymentTransaction(
                personType = "CUSTOMER",
                personMobile = "DIRECT_SALE", // Special tag for direct sales
                personName = "Cash Customer",
                amount = salePrice,
                paymentMode = "CASH",
                note = "Direct Sale: $itemName"
            )
            db.paymentTransactionDao().insert(transactionIn)
            
            // 3. Record the Cash Outflow (Supplier Payment)
            if (purchasePrice > 0) {
                val transactionOut = com.app.muzzutech.data.model.PaymentTransaction(
                    personType = "SUPPLIER",
                    personMobile = supplier.mobile,
                    personName = supplier.name,
                    amount = purchasePrice,
                    paymentMode = "CASH",
                    note = "Purchase for Direct Sale: $itemName"
                )
                db.paymentTransactionDao().insert(transactionOut)
            }

            Toast.makeText(requireContext(), R.string.sale_recorded, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
