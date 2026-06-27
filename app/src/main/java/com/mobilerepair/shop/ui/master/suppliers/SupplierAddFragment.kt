package com.mobilerepair.shop.ui.master.suppliers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentSupplierAddBinding

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

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val company = binding.etCompany.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val gst = binding.etGst.text.toString().trim()

            if (name.isEmpty() || mobile.isEmpty()) {
                Snackbar.make(binding.root, "Name and Mobile are required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.save(name, company, mobile, email, address, city, gst)
            Snackbar.make(binding.root, "Supplier added!", Snackbar.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
