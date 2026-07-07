package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        androidx.room.Index("supplierId"),
        androidx.room.Index("saleDate")
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val supplierId: String, // Supplier mobile number
    val supplierName: String = "",
    val purchasePrice: Long,
    val salePrice: Long,
    val paidToSupplier: Long = 0L,     // Amount paid to supplier
    val supplierDue: Long = 0L,         // Remaining due to supplier
    val customerPaid: Long = 0L,       // Amount customer paid us
    val customerDue: Long = 0L,        // Remaining customer owes us
    val saleDate: Long = System.currentTimeMillis()
)
