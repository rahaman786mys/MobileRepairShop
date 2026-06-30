package com.app.muzzutech.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentDashboardBinding
import com.app.muzzutech.utils.UpdateManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeData()

        UpdateManager.checkForUpdates(requireContext())
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
        binding.cardInvest.setOnClickListener {
            showInvestDialog()
        }
        binding.ivSearch.setOnClickListener {
            findNavController().navigate(R.id.entriesFragment)
        }
    }

    private fun showInvestDialog() {
        val paid = viewModel.dailyPaidInvest.value
        val due = viewModel.dailyDueInvest.value
        val total = viewModel.dailyInvest.value

        AlertDialog.Builder(requireContext())
            .setTitle("Today's Investment")
            .setMessage(
                "Total Invest: ₹ ${String.format("%.0f", total)}\n\n" +
                "✅ Paid: ₹ ${String.format("%.0f", paid)}\n" +
                "⏳ Due: ₹ ${String.format("%.0f", due)}"
            )
            .setPositiveButton("OK", null)
            .show()
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
                viewModel.dailyProfit.collectLatest { profit ->
                    binding.tvTodayProfit.text = "₹ ${String.format("%.0f", profit)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dailyInvest.collectLatest { invest ->
                    binding.tvTodayInvest.text = "₹ ${String.format("%.0f", invest)}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairApp.instance.database.userProfileDao().getUserProfileFlow().collectLatest { profile ->
                    binding.cardMissingInfo.isVisible = profile == null || profile.phone.isEmpty()
                    binding.tvWorkshopTitle.text = profile?.shopName?.ifEmpty { "MuZZu Tech" } ?: "MuZZu Tech"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
