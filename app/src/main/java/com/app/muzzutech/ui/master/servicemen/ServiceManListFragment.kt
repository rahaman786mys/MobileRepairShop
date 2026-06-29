package com.app.muzzutech.ui.master.servicemen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.R
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.databinding.FragmentServiceManListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ServiceManListFragment : Fragment(R.layout.fragment_service_man_list) {

    private var _binding: FragmentServiceManListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServiceManViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiceManListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddServiceMan.setOnClickListener {
            findNavController().navigate(R.id.serviceManAddFragment)
        }

        binding.rvServiceMen.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.serviceMen.collectLatest { men ->
                    // Simple adapter inline
                    binding.rvServiceMen.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                            object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                            ) {}
                        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                            val sm = men[position]
                            holder.itemView.findViewById<android.widget.TextView>(android.R.id.text1).text = sm.name
                            holder.itemView.findViewById<android.widget.TextView>(android.R.id.text2).text = "${sm.designation} | ${sm.mobile}"
                        }
                        override fun getItemCount() = men.size
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
