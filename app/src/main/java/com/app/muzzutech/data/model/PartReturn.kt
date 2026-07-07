package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "part_returns",
    indices = [
        androidx.room.Index("supplierId"),
        androidx.room.Index("returnDate")
    ]
)
data class PartReturn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: String,         // Supplier mobile number
    val supplierName: String = "",
    val partName: String,
    val returnReason: String = "",  // "Defective", "Wrong Item", "Not Needed", "Other"
    val returnDate: Long = System.currentTimeMillis(),
    val refundAmount: Long = 0L,
    val refundReceived: Boolean = false,
    val refundTransactionId: Long? = null,   // Links to cash-in PaymentTransaction for the refund
    val createdAt: Long = System.currentTimeMillis()
)
