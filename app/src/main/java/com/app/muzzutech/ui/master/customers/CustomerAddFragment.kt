package com.app.muzzutech.ui.master.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Customer
import com.app.muzzutech.data.model.Dealer
import com.app.muzzutech.databinding.FragmentCustomerAddBinding
import com.app.muzzutech.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CustomerAddFragment : Fragment() {

    private var _binding: FragmentCustomerAddBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editMobile = arguments?.getString("customerMobile")
        if (editMobile != null) {
            loadForEdit(editMobile)
            binding.tvTitle.text = getString(R.string.edit_profile)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val city = binding.etCity.text.toString().trim()

            if (name.isEmpty() || mobile.isEmpty()) {
                Snackbar.make(binding.root, R.string.name_mobile_required, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ValidationUtils.validatePhoneNumber(binding.tilMobile)) return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch {
                val db = MobileRepairApp.instance.database
                // Check if it's a dealer or customer
                val isDealer = arguments?.getBoolean("isDealer", false) ?: false
                if (isDealer) {
                    db.dealerDao().insert(Dealer(mobile, name, city))
                } else {
                    db.customerDao().insert(Customer(mobile, name, city))
                }
                
                Snackbar.make(binding.root, R.string.customer_profile_updated, Snackbar.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadForEdit(mobile: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = MobileRepairApp.instance.database
            val customer = db.customerDao().getCustomerByMobile(mobile)
            if (customer != null) {
                binding.etName.setText(customer.name)
                binding.etMobile.setText(customer.mobileNumber)
                binding.etMobile.isEnabled = false
                binding.etCity.setText(customer.city)
            } else {
                val dealer = db.dealerDao().getDealerByMobile(mobile)
                dealer?.let {
                    binding.etName.setText(it.name)
                    binding.etMobile.setText(it.mobileNumber)
                    binding.etMobile.isEnabled = false
                    binding.etCity.setText(it.city)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
