package com.app.muzzutech.ui.master.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentCustomerDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CustomerDetailFragment : Fragment(R.layout.fragment_customer_detail) {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mobile = arguments?.getString("customerMobile") ?: ""

        loadCustomerData(mobile)
    }

    private fun loadCustomerData(mobile: String) {
        val db = MobileRepairApp.instance.database
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.customerDao().getCustomerByMobileFlow(mobile).collectLatest { customer ->
                    customer?.let {
                        binding.tvCustomerName.text = it.name
                        binding.tvCustomerMobile.text = it.mobileNumber
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.repairEntryDao().getEntriesByMobile(mobile).collectLatest { entries ->
                    binding.tvTotalJobs.text = entries.size.toString()
                    
                    db.paymentDao().getPaymentsByMobile(mobile).collectLatest { payments ->
                        val totalDue = payments.sumOf { it.dueAmount }
                        binding.tvBalanceDue.text = "₹ ${String.format("%.0f", totalDue)}"
                    }

                    setupWorkHistory(entries)
                }
            }
        }
    }

    private fun setupWorkHistory(entries: List<com.app.muzzutech.data.model.RepairEntry>) {
        binding.rvWorkHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkHistory.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val e = entries[position]
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = 
                    "${e.deviceBrand} ${e.deviceModel} (${e.faultDetected})"
                holder.itemView.findViewById<TextView>(android.R.id.text2).text = 
                    "Status: ${e.workStatus} | Date: ${com.app.muzzutech.utils.DateUtils.formatDateTime(e.entryDate)}"
            }

            override fun getItemCount() = entries.size
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
