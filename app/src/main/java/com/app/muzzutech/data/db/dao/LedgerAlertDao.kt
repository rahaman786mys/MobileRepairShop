package com.app.muzzutech.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.app.muzzutech.data.model.LedgerAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerAlertDao {

    @Insert
    suspend fun insert(alert: LedgerAlert): Long

    @Update
    suspend fun update(alert: LedgerAlert): Int

    @Query("SELECT * FROM ledger_alerts ORDER BY alertDate DESC")
    fun getAll(): Flow<List<LedgerAlert>>

    @Query("SELECT * FROM ledger_alerts WHERE resolved = 0 ORDER BY alertDate DESC")
    fun getUnresolved(): Flow<List<LedgerAlert>>

    @Query("SELECT * FROM ledger_alerts WHERE alertDate BETWEEN :start AND :end ORDER BY alertDate DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<LedgerAlert>>

    @Query("SELECT COUNT(*) FROM ledger_alerts WHERE resolved = 0")
    suspend fun countUnresolved(): Int

    @Query("UPDATE ledger_alerts SET resolved = 1, resolvedAt = :now WHERE id = :id")
    suspend fun resolve(id: Long, now: Long = System.currentTimeMillis()): Int
}
