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
import com.app.muzzutech.databinding.FragmentInventoryBinding
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private var allItems = listOf<InventoryItem>()
    private var purchases = listOf<InventoryItem>()
    private var sales = listOf<InventoryItem>()
    private var returns = listOf<InventoryItem>()

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
                db.sparePartPurchaseDao().getAllPurchases().collectLatest { list ->
                    purchases = list.map { p ->
                        InventoryItem(
                            type = "PURCHASE",
                            label = p.partName,
                            detail = "Qty: ${p.quantity} @ ${PriceUtils.formatPrice(p.purchasePrice)} | Supplier: ${p.supplierName}",
                            value = p.purchasePrice * p.quantity,
                            date = p.purchaseDate
                        )
                    }
                    updateSummary()
                    combineAndDisplay()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.saleDao().getAllSales().collectLatest { list ->
                    sales = list.map { s ->
                        InventoryItem(
                            type = "SALE",
                            label = s.itemName,
                            detail = "Sold: ${PriceUtils.formatPrice(s.salePrice)} | Cost: ${PriceUtils.formatPrice(s.purchasePrice)} | Supplier: ${s.supplierName}",
                            value = s.salePrice,
                            date = s.saleDate
                        )
                    }
                    combineAndDisplay()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.partReturnDao().getAllReturns().collectLatest { list ->
                    returns = list.map { r ->
                        InventoryItem(
                            type = "RETURN",
                            label = r.partName,
                            detail = "Refund: ${PriceUtils.formatPrice(r.refundAmount)} | Reason: ${r.returnReason} | Supplier: ${r.supplierName}",
                            value = -r.refundAmount,
                            date = r.returnDate
                        )
                    }
                    combineAndDisplay()
                }
            }
        }
    }

    private fun updateSummary() {
        val totalValue = allItems.sumOf { it.value }
        binding.tvTotalInventoryValue.text = PriceUtils.formatPrice(totalValue)
        binding.tvTotalItemsCount.text = "${purchases.size} purchases, ${returns.size} returns"
    }

    private fun combineAndDisplay() {
        allItems = (purchases + sales + returns).sortedByDescending { it.date }
        updateSummary()
        setupRecyclerView(allItems)
    }

    private fun setupRecyclerView(items: List<InventoryItem>) {
        binding.rvInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventory.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = items[position]
                val typeTag = when (item.type) {
                    "PURCHASE" -> "[PURCHASE]"
                    "SALE" -> "[SALE]"
                    "RETURN" -> "[RETURN]"
                    else -> item.type
                }
                holder.itemView.findViewById<TextView>(android.R.id.text1).text =
                    "$typeTag ${item.label}"
                holder.itemView.findViewById<TextView>(android.R.id.text2).text =
                    "${item.detail} | ${DateUtils.formatDateTime(item.date)}"
            }

            override fun getItemCount() = items.size
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class InventoryItem(
        val type: String,
        val label: String,
        val detail: String,
        val value: Long,
        val date: Long
    )
}