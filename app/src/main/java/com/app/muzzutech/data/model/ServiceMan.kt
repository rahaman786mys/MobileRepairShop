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
    val monthlySalary: Long = 0L,      // Contractual fixed monthly salary (paise)
    val perDaySalary: Long = 0L,       // Per-day salary (paise, auto-computed fallback if monthlySalary=0)
    val createdAt: Long = System.currentTimeMillis(),
    // Auth fields for multi-user support
    val ownerId: String = "",          // Firebase Auth UID of the owner who created this worker
    val passwordHash: String = "",     // bcrypt-hashed password for worker login
    val workerAuthUid: String? = null, // Firebase Auth UID for this worker (if using Firebase Auth)
    val canLogin: Boolean = true       // Owner can toggle this to enable/disable worker login
)
