package com.app.muzzutech.ui.reports

import android.app.DatePickerDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.R
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.databinding.FragmentReportsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

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

        binding.chipDaily.isChecked = true
        viewModel.loadReport("Daily")

        binding.chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipDaily -> viewModel.loadReport("Daily")
                R.id.chipWeekly -> viewModel.loadReport("Weekly")
                R.id.chipMonthly -> viewModel.loadReport("Monthly")
                R.id.chipCustom -> showDateRangePicker()
            }
        }

        observeData()
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, sYear, sMonth, sDay ->
            val start = Calendar.getInstance().apply { set(sYear, sMonth, sDay, 0, 0) }.timeInMillis
            DatePickerDialog(requireContext(), { _, eYear, eMonth, eDay ->
                val end = Calendar.getInstance().apply { set(eYear, eMonth, eDay, 23, 59) }.timeInMillis
                viewModel.loadCustomReport(start, end)
                binding.chipCustom.text = "Range: $sDay/${sMonth+1} - $eDay/${eMonth+1}"
            }, year, month, day).show()
        }, year, month, day).show()
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
                viewModel.directSales.collectLatest { sales ->
                    setupSalesList(sales)
                }
            }
        }
    }

    private fun setupSalesList(sales: List<Sale>) {
        binding.rvDirectSales.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDirectSales.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_sale_ledger, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val s = sales[position]
                holder.itemView.findViewById<TextView>(R.id.tvItemName).text = s.itemName
                holder.itemView.findViewById<TextView>(R.id.tvSupplierInfo).text = "Ref: ${s.supplierId}"
                holder.itemView.findViewById<TextView>(R.id.tvSalePrice).text = "₹${s.salePrice}"
                val profit = s.salePrice - s.purchasePrice
                holder.itemView.findViewById<TextView>(R.id.tvProfit).text = "Profit: ₹$profit"
            }
            override fun getItemCount() = sales.size
        }
    }

    private fun updateChart(report: List<DailyReportRow>) {
        if (report.isEmpty()) {
            binding.barChart.clear()
            return
        }

        val entries = report.mapIndexed { index: Int, row: DailyReportRow ->
            BarEntry(index.toFloat(), row.totalRevenue?.toFloat() ?: 0f)
        }

        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = resources.getColor(R.color.muzzu_accent, null)
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
