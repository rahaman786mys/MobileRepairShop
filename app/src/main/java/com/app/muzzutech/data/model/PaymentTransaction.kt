package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_transactions",
    indices = [
        Index("paymentId"),
        Index("personMobile")
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
    val personType: String,          // "DEALER", "SUPPLIER", "CUSTOMER", "EXPENSE", "SALARY"
    val personType: String,         // "DEALER", "SUPPLIER", "CUSTOMER"
    val personMobile: String,
    val personName: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH", // "CASH", "ONLINE", "UPI"
    val note: String = "",
    val transactionDate: Long = System.currentTimeMillis()
)
