package com.app.muzzutech.ui.expenses

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.ui.compose.DarkBorder
import com.app.muzzutech.ui.compose.ErrorRed
import com.app.muzzutech.ui.compose.MuzzuTheme
import com.app.muzzutech.ui.compose.SuccessGreen
import com.app.muzzutech.ui.compose.composeView
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.utils.PriceUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

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
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val monthEnd = DateUtils.getEndOfMonth(monthStart)
    val monthExpenses = expenses.filter { it.date in monthStart..monthEnd }
        .sortedByDescending { it.date }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Premium Month Picker
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

            // Category Totals Horizontal Scroll
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(label = "Total", amount = totalThisMonth, color = ErrorRed)
                }
                categoryTotals.forEach { (cat, amt) ->
                    item {
                        CategoryChip(label = cat, amount = amt, color = categoryColor(cat))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Text(
                "TRANSACTION HISTORY",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (monthExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No expenses recorded.\nTap + to add one.", 
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(monthExpenses, key = { it.id }) { e ->
                        ExpenseRow(
                            expense = e,
                            onDelete = { scope.launch { vm.deleteExpense(e.id) } },
                            onTogglePaid = { scope.launch { vm.togglePaid(e) } }
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
                scope.launch {
                    vm.addExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = System.currentTimeMillis(),
                        recurring = recurring,
                        paid = paid,
                        note = note
                    )
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun CategoryChip(label: String, amount: Long, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                PriceUtils.formatPrice(amount),
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onDelete: () -> Unit,
    onTogglePaid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor(expense.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    expense.category.take(1).uppercase(),
                    color = categoryColor(expense.category),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${DateUtils.formatDayMonth(expense.date)} \u2022 ${if (expense.isRecurring) "RECURRING" else "ONE-TIME"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    PriceUtils.formatPrice(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = expense.paid, 
                        onCheckedChange = { onTogglePaid() },
                        colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
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
    onConfirm: (title: String, amount: Long, category: String, recurring: Boolean, paid: Boolean, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Expense.CATEGORY_OTHER) }
    var recurring by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Record Expense", style = MaterialTheme.typography.headlineLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Category", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(
                        Expense.CATEGORY_RENT,
                        Expense.CATEGORY_ELECTRICITY,
                        Expense.CATEGORY_INTERNET,
                        Expense.CATEGORY_SUPPLIES,
                        Expense.CATEGORY_OTHER
                    )) { c ->
                        val selected = category == c
                        TextButton(
                            onClick = { category = c },
                            modifier = Modifier
                                .border(
                                    1.dp, 
                                    if (selected) MaterialTheme.colorScheme.primary else DarkBorder, 
                                    RoundedCornerShape(8.dp)
                                )
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        ) {
                            Text(c, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                    Text("Monthly Recurring", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Checkbox(checked = paid, onCheckedChange = { paid = it }, colors = CheckboxDefaults.colors(checkedColor = SuccessGreen))
                    Text("Mark Paid", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = ((amountText.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                    if (title.isNotBlank() && amt > 0L) {
                        onConfirm(title, amt, category, recurring, paid, note)
                    }
                }
            ) { Text("SAVE TRANSACTION", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun categoryColor(category: String): Color = when (category) {
    Expense.CATEGORY_RENT -> Color(0xFF6366F1)
    Expense.CATEGORY_ELECTRICITY -> Color(0xFFF59E0B)
    Expense.CATEGORY_SALARY -> Color(0xFF10B981)
    Expense.CATEGORY_INTERNET -> Color(0xFF3B82F6)
    Expense.CATEGORY_SUPPLIES -> Color(0xFF8B5CF6)
    else -> Color(0xFF94A3B8)
}
