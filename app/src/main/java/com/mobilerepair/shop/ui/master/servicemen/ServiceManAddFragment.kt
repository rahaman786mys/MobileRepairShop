package com.mobilerepair.shop.ui.master.servicemen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentServiceManAddBinding

class ServiceManAddFragment : Fragment(R.layout.fragment_service_man_add) {

    private var _binding: FragmentServiceManAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServiceManViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiceManAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val empId = binding.etEmployeeId.text.toString().trim()
            val designation = binding.etDesignation.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Name is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.save(name, mobile, email, empId, designation)
            Snackbar.make(binding.root, "Service Man added!", Snackbar.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
