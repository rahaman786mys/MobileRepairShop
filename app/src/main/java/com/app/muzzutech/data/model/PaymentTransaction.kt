package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_transactions",
    indices = [
        Index("paymentId"),
        Index("personMobile"),
        Index("expenseId"),
        Index("salaryPaymentId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Payment::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long? = null,     // Links to Payment (nullable: salary/expense/sale txs have no parent Payment)
    val expenseId: Long? = null,     // Links to Expense (nullable: most txs have no parent Expense)
    val salaryPaymentId: Long? = null, // Links to SalaryPayment (nullable: non-salary txs)
    val personType: String,          // "DEALER", "SUPPLIER", "CUSTOMER", "EXPENSE", "SALARY"
    val personMobile: String,
    val personName: String = "",
    val amount: Long = 0L,
    val paymentMode: String = "CASH", // "CASH", "ONLINE", "UPI"
    val note: String = "",
    val transactionDate: Long = System.currentTimeMillis()
)
