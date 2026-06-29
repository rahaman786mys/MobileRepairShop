package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Supplier / Parts Vendor
 */
@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey
    val mobile: String, // Unique 10-digit number as ID
    val name: String = "",
    val companyName: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val gstNo: String = "",              // GST number if applicable
    val suppliesTypes: String = "",       // What they supply (e.g., "Displays, Batteries")
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
