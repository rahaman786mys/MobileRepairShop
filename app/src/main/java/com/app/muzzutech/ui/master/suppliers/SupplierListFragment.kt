package com.app.muzzutech.ui.master.suppliers

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentSupplierListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupplierListFragment : Fragment(R.layout.fragment_supplier_list) {

    private var _binding: FragmentSupplierListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SupplierViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSupplierListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddSupplier.setOnClickListener {
            findNavController().navigate(R.id.supplierAddFragment)
        }

        binding.rvSuppliers.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.suppliers.collectLatest { suppliers ->
                    binding.rvSuppliers.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                            object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                            ) {}
                        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                            val s = suppliers[position]
                            holder.itemView.findViewById<TextView>(android.R.id.text1).text = s.name
                            holder.itemView.findViewById<TextView>(android.R.id.text2).text = "${s.companyName} | ${s.mobile}"
                            
                            holder.itemView.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putString("supplierMobile", s.mobile)
                                }
                                findNavController().navigate(R.id.supplierDetailFragment, bundle)
                            }
                        }
                        override fun getItemCount() = suppliers.size
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
