package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_alerts",
    indices = [
        Index("alertDate"),
        Index("resolved")
    ]
)
data class LedgerAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertDate: Long = System.currentTimeMillis(),
    val type: String,             // "PAYMENT_MISMATCH", "EXPENSE_MISMATCH", "ORPHAN_TRANSACTION"
    val description: String,      // Human-readable summary
    val expectedAmount: Double = 0.0,
    val actualAmount: Double = 0.0,
    val mismatchAmount: Double = 0.0,
    val resolved: Boolean = false,
    val resolvedAt: Long? = null
)
