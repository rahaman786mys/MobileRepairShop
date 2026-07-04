package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring or one-time shop expense (rent, electricity, salaries, misc).
 *
 * - [category] is one of CATEGORY_* constants below.
 * - [isRecurring] true = monthly recurring expense that auto-pops up in work reports.
 * - [paid] true = this expense has been paid.
 */
@Entity(
    tableName = "expenses",
    indices = [Index("date"), Index("category")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = CATEGORY_OTHER,
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val paid: Boolean = true,
    val note: String = "",
    val salaryPaymentId: Long? = null,   // Links to SalaryPayment (null for non-salary expenses)
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_RENT = "Rent"
        const val CATEGORY_ELECTRICITY = "Electricity"
        const val CATEGORY_SALARY = "Salary"
        const val CATEGORY_INTERNET = "Internet"
        const val CATEGORY_SUPPLIES = "Supplies"
        const val CATEGORY_OTHER = "Other"
    }
}
