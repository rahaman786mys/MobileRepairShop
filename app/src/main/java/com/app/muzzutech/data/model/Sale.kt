package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val supplierId: String, // Supplier mobile number
    val supplierName: String = "",
    val purchasePrice: Double,
    val salePrice: Double,
    val paidToSupplier: Double = 0.0,     // Amount paid to supplier
    val supplierDue: Double = 0.0,         // Remaining due to supplier
    val customerPaid: Double = 0.0,       // Amount customer paid us
    val customerDue: Double = 0.0,        // Remaining customer owes us
    val saleDate: Long = System.currentTimeMillis()
)
