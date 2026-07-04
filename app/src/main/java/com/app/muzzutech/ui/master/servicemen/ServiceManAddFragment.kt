package com.app.muzzutech.ui.master.servicemen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentServiceManAddBinding
import com.app.muzzutech.utils.ValidationUtils

class ServiceManAddFragment : Fragment(R.layout.fragment_service_man_add) {

    private var _binding: FragmentServiceManAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServiceManViewModel by viewModels()
    private var editingId: Long = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiceManAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingId = arguments?.getLong("serviceManId", 0L) ?: 0L
        if (editingId > 0L) {
            binding.btnSave.text = "Update"
            arguments?.let { args ->
                binding.etName.setText(args.getString("serviceManName", ""))
                binding.etMobile.setText(args.getString("serviceManMobile", ""))
                binding.etEmail.setText(args.getString("serviceManEmail", ""))
                binding.etEmployeeId.setText(args.getString("serviceManEmpId", ""))
                binding.etDesignation.setText(args.getString("serviceManDesignation", ""))
                binding.etMonthlySalary.setText(args.getDouble("monthlySalary", 0.0).toString())
                binding.etPerDaySalary.setText(args.getDouble("perDaySalary", 0.0).toString())
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val empId = binding.etEmployeeId.text.toString().trim()
            val designation = binding.etDesignation.text.toString().trim()
            val monthly = binding.etMonthlySalary.text.toString().toDoubleOrNull() ?: 0.0
            val perDay = binding.etPerDaySalary.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Name is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (mobile.isNotEmpty() && !ValidationUtils.validatePhoneNumber(binding.tilServiceManMobile)) {
                return@setOnClickListener
            }

            if (editingId > 0L) {
                viewModel.update(editingId, name, mobile, email, empId, designation, monthly, perDay)
                Snackbar.make(binding.root, "Service Man updated!", Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.save(name, mobile, email, empId, designation, monthly, perDay)
                Snackbar.make(binding.root, "Service Man added!", Snackbar.LENGTH_SHORT).show()
            }
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
