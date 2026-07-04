package com.app.muzzutech.ui.master.faults

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.R
import com.app.muzzutech.adapter.CommonFaultAdapter
import com.app.muzzutech.databinding.FragmentCommonFaultsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommonFaultsFragment : Fragment(R.layout.fragment_common_faults) {

    private var _binding: FragmentCommonFaultsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommonFaultsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommonFaultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var editingFaultId: Long? = null

        binding.btnAddFault.setOnClickListener {
            val name = binding.etFaultName.text.toString().trim()
            val charge = binding.etDefaultCharge.text.toString().toDoubleOrNull() ?: 0.0
            val category = binding.etCategory.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Fault name is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (editingFaultId != null) {
                viewModel.updateFault(com.app.muzzutech.data.model.CommonFault(
                    id = editingFaultId!!,
                    faultName = name,
                    defaultCharge = charge,
                    category = category
                ))
                Snackbar.make(binding.root, "Fault updated!", Snackbar.LENGTH_SHORT).show()
                editingFaultId = null
                binding.btnAddFault.text = "Add"
            } else {
                viewModel.addFault(name, charge, category)
                Snackbar.make(binding.root, "Fault added!", Snackbar.LENGTH_SHORT).show()
            }
            binding.etFaultName.text?.clear()
            binding.etDefaultCharge.text?.clear()
            binding.etCategory.text?.clear()
        }

        binding.rvFaults.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.faults.collectLatest { faults ->
                    val adapter = CommonFaultAdapter(
                        onFaultClick = { fault ->
                            editingFaultId = fault.id
                            binding.etFaultName.setText(fault.faultName)
                            binding.etCategory.setText(fault.category)
                            binding.etDefaultCharge.setText(fault.defaultCharge.toBigDecimal().toPlainString())
                            binding.btnAddFault.text = "Update"
                        },
                        onFaultDelete = { fault ->
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Delete Fault")
                                .setMessage("Delete \"${fault.faultName}\"?")
                                .setPositiveButton("Delete") { _, _ ->
                                    viewModel.deleteFault(fault)
                                    Snackbar.make(binding.root, "Fault deleted", Snackbar.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    )
                    binding.rvFaults.adapter = adapter
                    adapter.submitList(faults)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
