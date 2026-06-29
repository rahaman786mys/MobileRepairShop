package com.app.muzzutech.ui.master.faults

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.R
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

        binding.btnAddFault.setOnClickListener {
            val name = binding.etFaultName.text.toString().trim()
            val charge = binding.etDefaultCharge.text.toString().toDoubleOrNull() ?: 0.0
            val category = binding.etCategory.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Fault name is required", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.addFault(name, charge, category)
            binding.etFaultName.text?.clear()
            binding.etDefaultCharge.text?.clear()
            binding.etCategory.text?.clear()
            Snackbar.make(binding.root, "Fault added!", Snackbar.LENGTH_SHORT).show()
        }

        binding.rvFaults.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.faults.collectLatest { faults ->
                    binding.rvFaults.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                            object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                            ) {}
                        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                            val f = faults[position]
                            holder.itemView.findViewById<TextView>(android.R.id.text1).text = f.faultName
                            holder.itemView.findViewById<TextView>(android.R.id.text2).text = "${f.category} | ₹${String.format("%.0f", f.defaultCharge)}"
                        }
                        override fun getItemCount() = faults.size
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
