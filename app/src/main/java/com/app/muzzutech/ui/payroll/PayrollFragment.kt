package com.app.muzzutech.ui.payroll

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.ui.compose.MuzzuTheme
import com.app.muzzutech.ui.compose.composeView
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils

class PayrollFragment : Fragment() {

    private val viewModel: PayrollViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView(this) {
            MuzzuTheme {
                PayrollScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayrollScreen(vm: PayrollViewModel) {
    val monthStart by vm.monthStart.collectAsStateWithLifecycle()
    val servicemen by vm.servicemen.collectAsStateWithLifecycle()
    val stats by vm.monthStats.collectAsStateWithLifecycle()
    val salaries by vm.salaryPayments.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.previousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            DateUtils.formatMonth(monthStart),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { vm.nextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (busy && servicemen.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (servicemen.isEmpty()) {
                Text(
                    "No technicians yet. Add servicemen to use payroll.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PayrollSummaryCard(
                            totalSalary = salaries.values.sumOf { it.computedAmount },
                            totalPaid = salaries.values.sumOf { it.paidAmount },
                            totalDue = salaries.values.sumOf { it.dueAmount }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(servicemen, key = { it.id }) { sm ->
                        PayrollTechCard(
                            serviceman = sm,
                            stats = stats[sm.id],
                            salary = salaries[sm.id]
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PayrollSummaryCard(totalSalary: Double, totalPaid: Double, totalDue: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Payroll Summary", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Salary", style = MaterialTheme.typography.labelMedium)
                    Text(
                        PriceUtils.formatPrice(totalSalary),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        PriceUtils.formatPrice(totalPaid),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Text(
                        PriceUtils.formatPrice(totalDue),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PayrollTechCard(
    serviceman: ServiceMan,
    stats: PayrollViewModel.MonthStats?,
    salary: com.app.muzzutech.data.model.SalaryPayment?
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        serviceman.name.firstOrNull()?.toString()?.take(1) ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(serviceman.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${serviceman.designation.ifBlank { "Technician" }} · ${serviceman.employeeId.ifBlank { "No ID" }}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                attendanceBadge(stats?.workedDays ?: 0.0)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Worked Days", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "%.1f".format(stats?.workedDays ?: 0.0),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Per Day", style = MaterialTheme.typography.labelMedium)
                    val perDay = serviceman.perDaySalary.let {
                        if (it > 0) it else serviceman.monthlySalary / 30.0
                    }
                    Text(PriceUtils.formatPrice(perDay), fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Computed", style = MaterialTheme.typography.labelMedium)
                    Text(
                        PriceUtils.formatPrice(salary?.computedAmount ?: PayrollMath.computePayable(
                            serviceman.monthlySalary,
                            serviceman.perDaySalary,
                            stats?.workedDays ?: 0.0
                        )),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Status", style = MaterialTheme.typography.labelMedium)
                    val status = salary?.status ?: if ((stats?.workedDays ?: 0.0) > 0) "PENDING" else "—"
                    Text(status, fontWeight = FontWeight.SemiBold, color = statusColor(status))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(PriceUtils.formatPrice(salary?.paidAmount ?: 0.0), color = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Text(PriceUtils.formatPrice(salary?.dueAmount ?: (salary?.computedAmount ?: 0.0)), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun attendanceBadge(workedDays: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "%.1f".format(workedDays) + " days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "PAID" -> MaterialTheme.colorScheme.primary
    "UNPAID", "PENDING" -> MaterialTheme.colorScheme.error
    "PARTIAL" -> Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
