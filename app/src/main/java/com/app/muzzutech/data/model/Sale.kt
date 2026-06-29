package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val supplierId: String, // Supplier mobile number
    val purchasePrice: Double,
    val salePrice: Double,
    val saleDate: Long = System.currentTimeMillis()
)
