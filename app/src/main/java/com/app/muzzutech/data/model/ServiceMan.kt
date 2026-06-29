package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Service Man / Technician working at the shop
 */
@Entity(tableName = "service_men")
data class ServiceMan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val employeeId: String = "",          // Shop's employee ID
    val designation: String = "Technician",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
