package com.app.muzzutech.ui.dashboard

import android.app.AlertDialog
import android.widget.Toast
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentDashboardBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupClickListeners()
    observeData()
    updateGreeting()
    
    // Auto-check for updates every time Dashboard is opened to ensure "Suddenly" updates
    UpdateManager.checkForUpdates(requireActivity() as AppCompatActivity)
  }

  private fun updateGreeting() {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
      in 5..11 -> "Good Morning!"
      in 12..16 -> "Good Afternoon!"
      else -> "Good Evening!"
    }
    binding.tvGreeting.text = greeting
  }

  private fun setupClickListeners() {
    binding.cardService.setOnClickListener {
      try {
        findNavController().navigate(R.id.entryFragment)
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(requireContext(), "Navigation Error: ${e.message}", Toast.LENGTH_LONG)
          .show()
      }
    }
    binding.cardSales.setOnClickListener { findNavController().navigate(R.id.saleFragment) }
    binding.cardWork.setOnClickListener { findNavController().navigate(R.id.entriesFragment) }
    binding.cardSuppliersGrid.setOnClickListener { findNavController().navigate(R.id.supplierListFragment) }
    binding.cardDuesGrid?.setOnClickListener { findNavController().navigate(R.id.duesFragment) }
    binding.cardReportsGrid?.setOnClickListener { findNavController().navigate(R.id.reportsFragment) }
    binding.cardMoreGrid?.setOnClickListener { findNavController().navigate(R.id.moreFragment) }
    binding.btnFixMissingInfo.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
    
    binding.btnUpdateAvailable.setOnClickListener {
        val info = com.app.muzzutech.utils.update.UpdateState.availableUpdate.value
        if (info != null) {
            UpdateManager.showUpdateDialog(requireActivity() as AppCompatActivity, info)
        }
    }
  }

  private fun showInvestDialog() {
    val paid = viewModel.dailyPaidInvest.value
    val due = viewModel.dailyDueInvest.value
    val total = viewModel.dailyInvest.value

    AlertDialog.Builder(requireContext())
      .setTitle("Today\"s Investment")
      .setMessage(
        "Total Invest: ${com.app.muzzutech.utils.PriceUtils.formatPrice(total)}\n\n" +
          "Paid: ${com.app.muzzutech.utils.PriceUtils.formatPrice(paid)}\n" +
          "Due: ${com.app.muzzutech.utils.PriceUtils.formatPrice(due)}",
      )
      .setPositiveButton("OK", null)
      .show()
  }

  private fun observeData() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.pendingCount.collectLatest { count ->
          binding.tvPendingCount2.text = count.toString()
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.dailyProfit.collectLatest { profit ->
          val formatted = com.app.muzzutech.utils.PriceUtils.formatPrice(profit)
          binding.tvTodayProfit2.text = formatted
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.dailyInvest.collectLatest { invest ->
          val formatted = com.app.muzzutech.utils.PriceUtils.formatPrice(invest)
          binding.tvTodayInvest2.text = formatted
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.totalCustomerDue.collectLatest { due ->
          binding.tvCustomerDuesTotal.text = com.app.muzzutech.utils.PriceUtils.formatPrice(due)
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        MobileRepairApp.instance.database.userProfileDao().getUserProfileFlow().collectLatest { profile ->
          _binding?.let { b ->
            b.cardMissingInfo.isVisible = profile == null || profile.phone.isEmpty()
          }
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.unresolvedAlertsCount.collectLatest { count ->
          _binding?.let { b ->
            b.cardLedgerAlert.isVisible = count > 0
            b.tvLedgerAlertText.text =
              if (count == 1) "1 ledger mismatch detected!"
              else "$count ledger mismatches detected!"
            b.btnViewLedgerAlerts.setOnClickListener { findNavController().navigate(R.id.reportsFragment) }
          }
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        com.app.muzzutech.utils.update.UpdateState.availableUpdate.collectLatest { info ->
          binding.btnUpdateAvailable.isVisible = info != null
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.businessHealth.collectLatest { health ->
          _binding?.let { b ->
            if (health == null) {
              b.cardAiAdvisor.isVisible = false
            } else {
              b.cardAiAdvisor.isVisible = true
              b.tvAiMoveTitle.text = health.smartMove
              b.tvAiHealthScore.text = health.healthScore.toString()
              b.aiProgressIndicator.progress = health.healthScore
              b.tvAiRecommendation.text = health.recommendation
            }
          }
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
