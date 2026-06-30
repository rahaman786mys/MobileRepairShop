package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long = 0,        // Links to Payment
    val personType: String,         // "DEALER", "SUPPLIER", "CUSTOMER"
    val personMobile: String,
    val personName: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH", // "CASH", "ONLINE", "UPI"
    val note: String = "",
    val transactionDate: Long = System.currentTimeMillis()
)
