package com.app.muzzutech.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.R
import com.app.muzzutech.adapter.RepairEntryAdapter
import com.app.muzzutech.data.db.dao.RepairEntryDao
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.databinding.FragmentReportsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Default to daily
        binding.chipDaily.isChecked = true
        viewModel.loadReport("Daily")

        // Period selector
        binding.chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            val period = when (checkedId) {
                R.id.chipDaily -> "Daily"
                R.id.chipWeekly -> "Weekly"
                R.id.chipMonthly -> "Monthly"
                R.id.chipCustom -> "Custom"
                else -> "Daily"
            }
            viewModel.loadReport(period)
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.revenue.collectLatest { rev ->
                    binding.tvReportRevenue.text = "₹ ${String.format("%.0f", rev)}"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedCount.collectLatest { count ->
                    binding.tvReportCompleted.text = count.toString()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dailyReport.collectLatest { report: List<DailyReportRow> ->
                    updateChart(report)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiInsights.collectLatest { insights ->
                    binding.tvAIInsights.text = insights
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.supplierPurchases.collectLatest { purchases ->
                    val total = purchases.sumOf { it.purchasePrice * it.quantity }
                    binding.tvAIInsights.text = "${binding.tvAIInsights.text}\n\n💰 Supplier Purchases: ₹ ${String.format("%.0f", total)}"
                }
            }
        }
    }

    private fun updateChart(report: List<DailyReportRow>) {
        if (report.isEmpty()) return

        val entries = report.mapIndexed { index: Int, row: DailyReportRow ->
            BarEntry(index.toFloat(), row.totalRevenue?.toFloat() ?: 0f)
        }

        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = resources.getColor(R.color.primary, null)
            valueTextSize = 10f
        }
        binding.barChart.data = BarData(dataSet)
        binding.barChart.description.isEnabled = false
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
