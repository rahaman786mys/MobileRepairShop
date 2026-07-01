package com.app.muzzutech.ui.master.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.Customer
import com.app.muzzutech.databinding.FragmentCustomerListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.navigation.fragment.findNavController

class CustomerListFragment : Fragment(R.layout.fragment_customer_list) {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        
        viewLifecycleOwner.lifecycleScope.launch {
            val db = MobileRepairApp.instance.database
            db.customerDao().getAllCustomers().collectLatest { customers ->
                db.dealerDao().getAllDealers().collectLatest { dealers ->
                    val combined = mutableListOf<PersonListItem>()
                    combined.addAll(customers.map { PersonListItem(it.name ?: "Unknown", it.mobileNumber, false) })
                    combined.addAll(dealers.map { PersonListItem(it.name ?: "Unknown", it.mobileNumber, true) })
                    binding.rvCustomers.adapter = CustomerAdapter(combined)
                }
            }
        }
    }

    data class PersonListItem(val name: String, val mobile: String, val isDealer: Boolean)

    inner class CustomerAdapter(private val list: List<PersonListItem>) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.text1.text = if (item.isDealer) "${item.name} (Dealer)" else item.name
            holder.text2.text = item.mobile
            
            holder.itemView.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("customerMobile", item.mobile)
                    putBoolean("isDealer", item.isDealer)
                }
                findNavController().navigate(R.id.customerDetailFragment, bundle)
            }
        }
        override fun getItemCount() = list.size
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
