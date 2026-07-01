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
import androidx.navigation.fragment.findNavController
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

        binding.btnEditCustomer.setOnClickListener {
            val bundle = Bundle().apply {
                putString("customerMobile", mobile)
                putBoolean("isDealer", arguments?.getBoolean("isDealer") ?: false)
            }
            findNavController().navigate(R.id.customerAddFragment, bundle)
        }
    }

    private fun loadCustomerData(mobile: String) {
        val db = MobileRepairApp.instance.database
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combine Customer and Dealer checks
                db.customerDao().getCustomerByMobileFlow(mobile).collectLatest { customer ->
                    if (customer != null) {
                        binding.tvCustomerName.text = customer.name
                        binding.tvCustomerMobile.text = customer.mobileNumber
                    } else {
                        db.dealerDao().getDealerByMobileFlow(mobile).collectLatest { dealer ->
                            dealer?.let {
                                binding.tvCustomerName.text = "${it.name} (Dealer)"
                                binding.tvCustomerMobile.text = it.mobileNumber
                            }
                        }
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
                        binding.tvBalanceDue.text = com.app.muzzutech.utils.PriceUtils.formatPrice(totalDue)
                    }

                    db.paymentTransactionDao().getTransactionsByMobile(mobile).collectLatest { transactions ->
                        setupPaymentHistory(transactions)
                    }

                    setupWorkHistory(entries)
                }
            }
        }
    }

    private fun setupPaymentHistory(transactions: List<com.app.muzzutech.data.model.PaymentTransaction>) {
        binding.rvPaymentHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaymentHistory.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val t = transactions[position]
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = 
                    "Paid: ₹${t.amount} (${t.paymentMode})"
                holder.itemView.findViewById<TextView>(android.R.id.text2).text = 
                    com.app.muzzutech.utils.DateUtils.formatDateTime(t.transactionDate)
            }

            override fun getItemCount() = transactions.size
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
