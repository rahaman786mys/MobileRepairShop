package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A salary payment made to a service man for a given month.
 *
 * - [monthStart] is the first-millisecond of the calendar month (e.g. 2026-07-01 00:00).
 * - [daysWorked] is the count of attendance days (full-day=1, half-day=0.5, absent=0).
 * - [perDaySalary] is copied from [ServiceMan.salaryPerDay] at the time of computation so
 *   historical salary slips don't change when the tech's settings later change.
 * - [fixedMonthlySalary] is the contractual monthly salary (also snapshotted).
 * - [computedAmount] is what the system actually computed as payable.
 * - [paidAmount] is what was paid in cash/online.
 * - [status] is UNPAID / PARTIAL / PAID.
 */
@Entity(
    tableName = "salary_payments",
    indices = [Index("servicemanId"), Index("monthStart")],
    foreignKeys = [
        ForeignKey(
            entity = ServiceMan::class,
            parentColumns = ["id"],
            childColumns = ["servicemanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SalaryPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val servicemanId: Long = 0,
    val servicemanName: String = "",
    val monthStart: Long = System.currentTimeMillis(),
    val daysWorked: Double = 0.0,
    val perDaySalary: Double = 0.0,
    val fixedMonthlySalary: Double = 0.0,
    val computedAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val status: String = "UNPAID", // UNPAID, PARTIAL, PAID
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
