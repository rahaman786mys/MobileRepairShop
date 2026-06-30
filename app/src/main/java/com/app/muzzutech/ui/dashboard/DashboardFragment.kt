package com.app.muzzutech.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.adapter.RepairEntryAdapter
import com.app.muzzutech.databinding.FragmentDashboardBinding
import com.app.muzzutech.utils.UpdateManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: RepairEntryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupSearchView()
        observeData()

        // Check for updates
        UpdateManager.checkForUpdates(requireContext())
    }

    private fun setupRecyclerView() {
        adapter = RepairEntryAdapter { entry ->
            val bundle = Bundle().apply { putLong("entryId", entry.id) }
            findNavController().navigate(R.id.entryDetailFragment, bundle)
        }
        binding.rvRecentEntries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentEntries.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.cardService.setOnClickListener {
            findNavController().navigate(R.id.entryFragment)
        }
        binding.cardSales.setOnClickListener {
            findNavController().navigate(R.id.saleFragment)
        }
        binding.cardWork.setOnClickListener {
            findNavController().navigate(R.id.entriesFragment)
        }
        binding.cardSuppliersGrid.setOnClickListener {
            findNavController().navigate(R.id.supplierListFragment)
        }
        binding.cardDuesGrid?.setOnClickListener {
            findNavController().navigate(R.id.duesFragment)
        }
        binding.cardReportsGrid?.setOnClickListener {
            findNavController().navigate(R.id.reportsFragment)
        }
        binding.cardMoreGrid?.setOnClickListener {
            findNavController().navigate(R.id.moreFragment)
        }
        binding.btnFixMissingInfo.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingCount.collectLatest { count ->
                    binding.tvPendingCount.text = count.toString()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedToday.collectLatest { count ->
                    binding.tvCompletedCount.text = count.toString()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentEntries.collectLatest { entries ->
                    adapter.submitList(entries)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.businessHealth.collectLatest { health ->
                    health?.let {
                        binding.tvTodayProfit.text = "₹ ${String.format("%.0f", it.dailyProfit)}"
                        binding.tvTodayExpense.text = "₹ ${String.format("%.0f", it.dailyExpense)}"
                    }
                }
            }
        }
        
        // Check for missing phone number in profile
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairApp.instance.database.userProfileDao().getUserProfileFlow().collectLatest { profile ->
                    binding.cardMissingInfo.isVisible = profile == null || profile.phone.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
