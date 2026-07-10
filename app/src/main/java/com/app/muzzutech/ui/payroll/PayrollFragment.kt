package com.app.muzzutech.ui.payroll

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.ui.compose.DarkBorder
import com.app.muzzutech.ui.compose.ErrorRed
import com.app.muzzutech.ui.compose.MuzzuTheme
import com.app.muzzutech.ui.compose.SuccessGreen
import com.app.muzzutech.ui.compose.WarningAmber
import com.app.muzzutech.ui.compose.composeView
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils
import kotlin.math.roundToLong

import kotlinx.coroutines.launch

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
    var payingSm by remember { mutableStateOf<ServiceMan?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Month Picker
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { vm.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    DateUtils.formatMonth(monthStart).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { vm.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (busy && servicemen.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                }
            } else if (servicemen.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "No technicians registered.\nAdd staff in Master Records to manage payroll.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PayrollSummaryCard(
                            totalSalary = salaries.values.sumOf { it.computedAmount },
                            totalPaid = salaries.values.sumOf { it.paidAmount },
                            totalDue = salaries.values.sumOf { it.dueAmount }
                        )
                        Spacer(Modifier.height(12.dp))
    Text(
        "STAFF DISBURSEMENT",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
                    }
                    items(servicemen, key = { it.id }) { sm ->
                        val salary = salaries[sm.id]
                        val smStats = stats[sm.id]
                        val isUnpaid = salary == null || salary.status == "UNPAID" || salary.status == "NO_WORK"
                        PayrollTechCard(
                            serviceman = sm,
                            stats = smStats,
                            salary = salary,
                            canPay = isUnpaid,
                            onPay = { payingSm = sm }
                        )
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }

        payingSm?.let { sm ->
            val smStats = stats[sm.id]
            val salary = salaries[sm.id]
            PaySalaryDialog(
                serviceman = sm,
                workedDays = smStats?.workedDays ?: 0.0,
                computedAmount = salary?.computedAmount ?: PayrollMath.computePayable(sm.monthlySalary, sm.perDaySalary, smStats?.workedDays ?: 0.0),
                currentPaid = salary?.paidAmount ?: 0L,
                onDismiss = { payingSm = null },
                onConfirm = { amount, mode ->
                    scope.launch {
                        vm.generateOrUpdateSalary(sm.id, amount, "Salary payout", mode)
                        payingSm = null
                    }
                }
            )
        }
    }
}

@Composable
private fun PayrollSummaryCard(totalSalary: Long, totalPaid: Long, totalDue: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Payroll Summary", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("TOTAL PAYABLE", totalSalary, MaterialTheme.colorScheme.onSurface)
                SummaryItem("TOTAL PAID", totalPaid, SuccessGreen)
                SummaryItem("TOTAL DUE", totalDue, ErrorRed)
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            PriceUtils.formatPrice(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PayrollTechCard(
    serviceman: ServiceMan,
    stats: PayrollViewModel.MonthStats?,
    salary: com.app.muzzutech.data.model.SalaryPayment?,
    canPay: Boolean = false,
    onPay: (() -> Unit)? = null
) {
    val computed = salary?.computedAmount ?: PayrollMath.computePayable(serviceman.monthlySalary, serviceman.perDaySalary, stats?.workedDays ?: 0.0)
    val perDay = if (serviceman.perDaySalary > 0L) serviceman.perDaySalary else if (serviceman.monthlySalary > 0L) (serviceman.monthlySalary / 30L) else 0L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        serviceman.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(serviceman.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${serviceman.designation.ifBlank { "Technician" }} \u2022 ${serviceman.employeeId.ifBlank { "ID-N/A" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                attendanceBadge(stats?.workedDays ?: 0.0)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItemSmall("WORK DAYS", "%.1f".format(stats?.workedDays ?: 0.0))
                SummaryItemSmall("RATE/DAY", PriceUtils.formatPrice(perDay))
                SummaryItemSmall("COMPUTED", PriceUtils.formatPrice(computed), isBold = true)
            }

            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val status = salary?.status ?: if ((stats?.workedDays ?: 0.0) > 0) "PENDING" else "NO WORK"
                Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = statusColor(status))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PAID ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(PriceUtils.formatPrice(salary?.paidAmount ?: 0L), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text("DUE ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(PriceUtils.formatPrice(salary?.dueAmount ?: (salary?.computedAmount ?: 0L)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                    if (canPay && onPay != null) {
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.material3.Button(
                            onClick = onPay,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("PAY", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItemSmall(label: String, value: String, isBold: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun attendanceBadge(workedDays: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (workedDays > 0) SuccessGreen.copy(alpha = 0.1f) else DarkBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "%.1f D".format(workedDays),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (workedDays > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "PAID" -> SuccessGreen
    "UNPAID", "PENDING" -> ErrorRed
    "PARTIAL" -> WarningAmber
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaySalaryDialog(
    serviceman: ServiceMan,
    workedDays: Double,
    computedAmount: Long,
    currentPaid: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPaid: Long, paymentMode: String) -> Unit
) {
    var amountPaid by remember { mutableStateOf(computedAmount) }
    var selectedMode by remember { mutableStateOf("CASH") }
    val modes = listOf("CASH", "UPI", "BANK")
    val remaining = (computedAmount - currentPaid).coerceAtLeast(0L)
    val finalDue = (computedAmount - amountPaid).coerceAtLeast(0L)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Salary — ${serviceman.name}", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryItemSmall("MONTH", DateUtils.formatMonth(DateUtils.getStartOfMonth()))
                SummaryItemSmall("WORKED DAYS", "%.1f days".format(workedDays))
                SummaryItemSmall("RATE/DAY", PriceUtils.formatPrice(
                    if (serviceman.perDaySalary > 0L) serviceman.perDaySalary else (serviceman.monthlySalary / 30L)
                ))
                SummaryItemSmall("COMPUTED", PriceUtils.formatPrice(computedAmount), isBold = true)
                if (currentPaid > 0L) {
                    SummaryItemSmall("ALREADY PAID", PriceUtils.formatPrice(currentPaid))
                }
                OutlinedTextField(
                    value = PriceUtils.formatAmount(amountPaid),
                    onValueChange = {
                        val paise = ((it.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                        amountPaid = paise.coerceIn(0L, computedAmount)
                    },
                    label = { Text("Amount to Pay (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Payment Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        val selected = selectedMode == mode
                        androidx.compose.material3.FilterChip(
                            selected = selected,
                            onClick = { selectedMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }
                if (finalDue > 0L && amountPaid < computedAmount) {
                    SummaryItemSmall("REMAINING DUE", PriceUtils.formatPrice(finalDue), isBold = true)
                }
                if (amountPaid >= computedAmount && computedAmount > 0L) {
                    Text("✓ Full salary will be marked PAID", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amountPaid, selectedMode) },
                enabled = amountPaid > 0L
            ) {
                Text("CONFIRM & PAY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
