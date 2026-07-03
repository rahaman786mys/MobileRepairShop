package com.app.muzzutech.ui.expenses

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.ui.compose.MuzzuTheme
import com.app.muzzutech.ui.compose.composeView
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils

class ExpensesFragment : Fragment() {

    private val viewModel: ExpensesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView(this) {
            MuzzuTheme { ExpensesScreen(viewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpensesScreen(vm: ExpensesViewModel) {
    val monthStart by vm.monthStart.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val totalThisMonth by vm.totalThisMonth.collectAsStateWithLifecycle()
    val categoryTotals by vm.categoryTotals.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val monthEnd = DateUtils.getEndOfMonth(monthStart)
    val monthExpenses = expenses.filter { it.date in monthStart..monthEnd }
        .sortedByDescending { it.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.previousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(DateUtils.formatMonth(monthStart), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { vm.nextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ExpenseSummaryCard(
                total = totalThisMonth,
                categoryTotals = categoryTotals
            )
            Spacer(Modifier.height(8.dp))
            if (monthExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses recorded this month.\nTap + to add one.", modifier = Modifier.padding(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthExpenses, key = { it.id }) { e ->
                        ExpenseRow(
                            expense = e,
                            onDelete = { vm.deleteExpense(e.id) },
                            onTogglePaid = { vm.togglePaid(e) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category, recurring, paid, note ->
                vm.addExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    date = System.currentTimeMillis(),
                    recurring = recurring,
                    paid = paid,
                    note = note
                ) { showAddDialog = false }
            }
        )
    }
}

@Composable
private fun ExpenseSummaryCard(total: Double, categoryTotals: Map<String, Double>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("This Month", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                PriceUtils.formatPrice(total),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            categoryTotals.forEach { (cat, amt) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(PriceUtils.formatPrice(amt), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onDelete: () -> Unit,
    onTogglePaid: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(categoryColor(expense.category))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    expense.category.first().toString(),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.SemiBold)
                Text(
                    "${DateUtils.formatDayMonth(expense.date)} · ${if (expense.isRecurring) "Recurring · " else ""}${if (expense.paid) "Paid" else "Unpaid"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    PriceUtils.formatPrice(expense.amount),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = expense.paid, onCheckedChange = { onTogglePaid() })
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, recurring: Boolean, paid: Boolean, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Expense.CATEGORY_OTHER) }
    var recurring by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(
                        Expense.CATEGORY_RENT,
                        Expense.CATEGORY_ELECTRICITY,
                        Expense.CATEGORY_INTERNET,
                        Expense.CATEGORY_SUPPLIES,
                        Expense.CATEGORY_OTHER
                    ).forEach { c ->
                        TextButton(
                            onClick = { category = c },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Text(
                                c,
                                fontWeight = if (category == c) FontWeight.Bold else FontWeight.Normal,
                                color = if (category == c) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                    Text("Recurring monthly", modifier = Modifier.weight(1f))
                    Checkbox(checked = paid, onCheckedChange = { paid = it })
                    Text("Paid")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, amt, category, recurring, paid, note)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun categoryColor(category: String): androidx.compose.ui.graphics.Color = when (category) {
    Expense.CATEGORY_RENT -> androidx.compose.ui.graphics.Color(0xFF6366F1)
    Expense.CATEGORY_ELECTRICITY -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    Expense.CATEGORY_SALARY -> androidx.compose.ui.graphics.Color(0xFF10B981)
    Expense.CATEGORY_INTERNET -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
    Expense.CATEGORY_SUPPLIES -> androidx.compose.ui.graphics.Color(0xFF8B5CF6)
    else -> androidx.compose.ui.graphics.Color(0xFF94A3B8)
}
