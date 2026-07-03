package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.Index

/**
 * Daily attendance for a service man (technician).
 *
 * Composite PK (servicemanId + date) guarantees one attendance row per tech per day.
 *
 * - [present] true = present, false = absent.
 * - [halfDay] true = counts as 0.5 day of salary.
 * - [note] optional (e.g. "leave", "sick").
 */
@Entity(
    tableName = "attendance",
    primaryKeys = ["servicemanId", "date"],
    indices = [Index("date"), Index("servicemanId")]
)
data class Attendance(
    val servicemanId: Long = 0,
    val date: Long = System.currentTimeMillis(), // start of day
    val present: Boolean = true,
    val halfDay: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
