package com.app.muzzutech.ui.dues

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
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.R
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.databinding.FragmentDuesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DuesFragment : Fragment(R.layout.fragment_dues) {

    private var _binding: FragmentDuesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DuesViewModel by viewModels()

    private var currentTab = "ALL"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        observeData()
    }

    private fun setupTabs() {
        binding.tabAll.setOnClickListener {
            currentTab = "ALL"
            updateTabColors()
            updateList(viewModel.allDues.value)
        }
        binding.tabDealer.setOnClickListener {
            currentTab = "DEALER"
            updateTabColors()
            updateList(viewModel.dealerDues.value)
        }
        binding.tabSupplier.setOnClickListener {
            currentTab = "SUPPLIER"
            updateTabColors()
            updateList(viewModel.supplierDues.value)
        }
        binding.tabCustomer.setOnClickListener {
            currentTab = "CUSTOMER"
            updateTabColors()
            updateList(viewModel.customerDues.value)
        }
    }

    private fun updateTabColors() {
        binding.tabAll.setBackgroundColor(resources.getColor(R.color.background))
        binding.tabDealer.setBackgroundColor(resources.getColor(R.color.background))
        binding.tabSupplier.setBackgroundColor(resources.getColor(R.color.background))
        binding.tabCustomer.setBackgroundColor(resources.getColor(R.color.background))

        val activeColor = resources.getColor(R.color.primary)
        when (currentTab) {
            "ALL" -> binding.tabAll.setBackgroundColor(activeColor)
            "DEALER" -> binding.tabDealer.setBackgroundColor(activeColor)
            "SUPPLIER" -> binding.tabSupplier.setBackgroundColor(activeColor)
            "CUSTOMER" -> binding.tabCustomer.setBackgroundColor(activeColor)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalDue.collectLatest { amount ->
                    binding.tvTotalDue.text = "₹ ${String.format("%.0f", amount)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dealerDue.collectLatest { amount ->
                    binding.tvDealerDue.text = "₹ ${String.format("%.0f", amount)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.supplierDue.collectLatest { amount ->
                    binding.tvSupplierDue.text = "₹ ${String.format("%.0f", amount)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.customerDue.collectLatest { amount ->
                    binding.tvCustomerDue.text = "₹ ${String.format("%.0f", amount)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allDues.collectLatest { list ->
                    if (currentTab == "ALL") updateList(list)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dealerDues.collectLatest { list ->
                    if (currentTab == "DEALER") updateList(list)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.supplierDues.collectLatest { list ->
                    if (currentTab == "SUPPLIER") updateList(list)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.customerDues.collectLatest { list ->
                    if (currentTab == "CUSTOMER") updateList(list)
                }
            }
        }
    }

    private fun updateList(payments: List<Payment>) {
        binding.rvDues.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDues.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_due, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val p = payments[position]
                holder.itemView.findViewById<TextView>(R.id.tvPersonName).text = p.personName.ifEmpty { p.personMobile }
                holder.itemView.findViewById<TextView>(R.id.tvPersonMobile).text = p.personMobile
                holder.itemView.findViewById<TextView>(R.id.tvDueAmount).text = "₹ ${String.format("%.0f", p.dueAmount)}"
                holder.itemView.findViewById<TextView>(R.id.tvDescription).text = p.description
                val statusColor = when (p.status) {
                    "PAID" -> R.color.success
                    "PARTIAL" -> R.color.warning
                    else -> R.color.error
                }
                holder.itemView.findViewById<TextView>(R.id.tvStatus).apply {
                    text = p.status
                    setTextColor(resources.getColor(statusColor))
                }
                holder.itemView.setOnClickListener {
                    val bundle = Bundle().apply {
                        putLong("paymentId", p.id)
                    }
                    findNavController().navigate(R.id.payDuesFragment, bundle)
                }
            }

            override fun getItemCount() = payments.size
        }
        binding.tvEmpty.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
