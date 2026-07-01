package com.app.muzzutech.ui.master.suppliers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentSupplierAddBinding
import com.app.muzzutech.utils.ValidationUtils
import kotlinx.coroutines.launch

class SupplierAddFragment : Fragment(R.layout.fragment_supplier_add) {

    private var _binding: FragmentSupplierAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SupplierViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSupplierAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editMobile = arguments?.getString("supplierMobile")
        if (editMobile != null) {
            loadSupplierForEdit(editMobile)
            binding.btnSave.text = getString(R.string.update_supplier)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val company = binding.etCompany.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val gst = binding.etGst.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Name is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (mobile.isEmpty()) {
                Snackbar.make(binding.root, "Mobile is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!ValidationUtils.validatePhoneNumber(binding.tilSupplierMobile)) {
                return@setOnClickListener
            }

            viewModel.save(name, company, mobile, email, address, city, gst)
            val msg = if (editMobile != null) R.string.supplier_updated else R.string.supplier_added
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadSupplierForEdit(mobile: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getSupplierByMobile(mobile)?.let { s ->
                binding.etName.setText(s.name)
                binding.etCompany.setText(s.companyName)
                binding.etMobile.setText(s.mobile)
                binding.etMobile.isEnabled = false // Cannot change mobile as it is the key
                binding.etEmail.setText(s.email)
                binding.etAddress.setText(s.address)
                binding.etCity.setText(s.city)
                binding.etGst.setText(s.gstNo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
