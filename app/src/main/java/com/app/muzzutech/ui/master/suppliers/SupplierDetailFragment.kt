package com.app.muzzutech.ui.master.suppliers

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
import com.app.muzzutech.databinding.FragmentSupplierDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupplierDetailFragment : Fragment(R.layout.fragment_supplier_detail) {

    private var _binding: FragmentSupplierDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSupplierDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mobile = arguments?.getString("supplierMobile") ?: ""

        loadSupplierData(mobile)

        binding.btnEditSupplier.setOnClickListener {
            val bundle = Bundle().apply {
                putString("supplierMobile", mobile)
            }
            findNavController().navigate(R.id.supplierAddFragment, bundle)
        }
    }

    private fun loadSupplierData(mobile: String) {
        val db = MobileRepairApp.instance.database
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.supplierDao().getSupplierByMobileFlow(mobile).collectLatest { supplier ->
                    supplier?.let {
                        binding.tvSupplierName.text = it.name
                        binding.tvSupplierMobile.text = it.mobile
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.sparePartPurchaseDao().getPurchasesBySupplier(mobile).collectLatest { purchases ->
                    val totalBought = purchases.sumOf { it.purchasePrice * it.quantity }
                    binding.tvTotalBought.text = com.app.muzzutech.utils.PriceUtils.formatPrice(totalBought)
                    
                    db.paymentDao().getPaymentsByMobile(mobile).collectLatest { payments ->
                        val totalDue = payments.sumOf { it.dueAmount }
                        val totalPaid = totalBought - totalDue
                        
                        binding.tvTotalPaid.text = com.app.muzzutech.utils.PriceUtils.formatPrice(totalPaid)
                        binding.tvBalanceDue.text = com.app.muzzutech.utils.PriceUtils.formatPrice(totalDue)
                    }
                    
                    db.paymentTransactionDao().getTransactionsByMobile(mobile).collectLatest { transactions ->
                        setupPaymentHistory(transactions)
                    }

                    db.saleDao().getSalesBySupplier(mobile).collectLatest { sales ->
                        setupSalesList(sales)
                    }

                    setupPurchasesList(purchases)
                }
            }
        }
    }

    private fun setupSalesList(sales: List<com.app.muzzutech.data.model.Sale>) {
        binding.rvSupplierSales.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSupplierSales.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val s = sales[position]
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = s.itemName
                holder.itemView.findViewById<TextView>(android.R.id.text2).text = 
                    "Sold for: ₹${s.salePrice} | Buy Price: ₹${s.purchasePrice}"
            }

            override fun getItemCount() = sales.size
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

    private fun setupPurchasesList(purchases: List<com.app.muzzutech.data.model.SparePartPurchase>) {
        binding.rvSupplierPurchases.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSupplierPurchases.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val p = purchases[position]
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = "${p.partName} (Qty: ${p.quantity})"
                holder.itemView.findViewById<TextView>(android.R.id.text2).text = 
                    "Price: ₹${p.purchasePrice} | Total: ₹${p.purchasePrice * p.quantity}"
            }

            override fun getItemCount() = purchases.size
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
