package com.app.muzzutech.ui.master.inventory

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
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.databinding.FragmentInventoryBinding
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadInventoryData()
    }

    private fun loadInventoryData() {
        val db = MobileRepairApp.instance.database
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.sparePartPurchaseDao().getAllPurchases().collectLatest { purchases ->
                    val totalValue = purchases.sumOf { it.purchasePrice * it.quantity }
                    binding.tvTotalInventoryValue.text = "₹ ${String.format("%.0f", totalValue)}"
                    binding.tvTotalItemsCount.text = purchases.size.toString()
                    
                    setupRecyclerView(purchases)
                }
            }
        }
    }

    private fun setupRecyclerView(purchases: List<SparePartPurchase>) {
        binding.rvInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventory.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val p = purchases[position]
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = 
                    "${p.partName} (Ref: #${p.repairEntryId})"
                holder.itemView.findViewById<TextView>(android.R.id.text2).text = 
                    "Price: ₹${p.purchasePrice} | Qty: ${p.quantity} | Date: ${DateUtils.formatDateTime(p.purchaseDate)}"
            }

            override fun getItemCount() = purchases.size
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
