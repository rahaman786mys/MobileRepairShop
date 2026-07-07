package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Common faults/issues that can be pre-configured
 * Service man can select from these or add custom ones
 */
@Entity(tableName = "common_faults")
data class CommonFault(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faultName: String = "",
    val category: String = "",           // e.g., "Display", "Battery", "Charging", "Motherboard", "Body"
    val defaultCharge: Long = 0L,     // Suggested charge amount (paise)
    val description: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
