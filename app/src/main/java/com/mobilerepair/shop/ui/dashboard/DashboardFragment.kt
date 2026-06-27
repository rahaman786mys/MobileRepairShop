package com.mobilerepair.shop.ui.dashboard

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
import com.mobilerepair.shop.R
import com.mobilerepair.shop.adapter.RepairEntryAdapter
import com.mobilerepair.shop.databinding.FragmentDashboardBinding
import com.mobilerepair.shop.utils.UpdateChecker
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
        observeData()

        // Check for updates
        UpdateChecker.checkForUpdates(requireContext())
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
        binding.cardNewEntry.setOnClickListener {
            findNavController().navigate(R.id.entryFragment)
        }
        binding.cardEntries.setOnClickListener {
            findNavController().navigate(R.id.entriesFragment)
        }
        binding.cardReports.setOnClickListener {
            findNavController().navigate(R.id.reportsFragment)
        }
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
                viewModel.todayRevenue.collectLatest { revenue ->
                    binding.tvTodayRevenue.text = "₹ ${String.format("%.0f", revenue)}"
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
