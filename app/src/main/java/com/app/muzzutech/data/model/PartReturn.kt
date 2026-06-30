package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_returns")
data class PartReturn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: String,         // Supplier mobile number
    val supplierName: String = "",
    val partName: String,
    val returnReason: String = "",  // "Defective", "Wrong Item", "Not Needed", "Other"
    val returnDate: Long = System.currentTimeMillis(),
    val refundAmount: Double = 0.0,
    val refundReceived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
