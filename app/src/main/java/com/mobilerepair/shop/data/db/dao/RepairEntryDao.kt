package com.mobilerepair.shop.data.db.dao

import androidx.room.*
import com.mobilerepair.shop.data.model.RepairEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RepairEntry): Long

    @Update
    suspend fun update(entry: RepairEntry)

    @Delete
    suspend fun delete(entry: RepairEntry)

    @Query("SELECT * FROM repair_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<RepairEntry>>

    @Query("SELECT * FROM repair_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): RepairEntry?

    @Query("SELECT * FROM repair_entries WHERE id = :id")
    fun getEntryByIdFlow(id: Long): Flow<RepairEntry?>

    // Pending entries (not handed over)
    @Query("SELECT * FROM repair_entries WHERE handoverDone = 0 ORDER BY createdAt DESC")
    fun getPendingEntries(): Flow<List<RepairEntry>>

    // Completed entries
    @Query("SELECT * FROM repair_entries WHERE handoverDone = 1 ORDER BY handoverDate DESC")
    fun getCompletedEntries(): Flow<List<RepairEntry>>

    // By service man
    @Query("SELECT * FROM repair_entries WHERE serviceManId = :serviceManId ORDER BY createdAt DESC")
    fun getEntriesByServiceMan(serviceManId: Long): Flow<List<RepairEntry>>

    // By date range
    @Query("SELECT * FROM repair_entries WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<RepairEntry>>

    // Counts
    @Query("SELECT COUNT(*) FROM repair_entries WHERE handoverDone = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repair_entries WHERE handoverDone = 1 AND handoverDate BETWEEN :startDate AND :endDate")
    fun getCompletedCountInRange(startDate: Long, endDate: Long): Flow<Int>

    // Total revenue in date range
    @Query("SELECT SUM(finalAmount) FROM repair_entries WHERE handoverDone = 1 AND handoverDate BETWEEN :startDate AND :endDate")
    fun getRevenueInRange(startDate: Long, endDate: Long): Flow<Double?>

    // Reports - Daily/Weekly/Monthly
    @Query("""
        SELECT strftime('%Y-%m-%d', handoverDate / 1000, 'unixepoch') as dateGroup,
               COUNT(*) as count,
               SUM(finalAmount) as totalRevenue
        FROM repair_entries 
        WHERE handoverDone = 1 
          AND handoverDate BETWEEN :startDate AND :endDate
        GROUP BY dateGroup
        ORDER BY dateGroup ASC
    """)
    fun getDailyReport(startDate: Long, endDate: Long): Flow<List<DailyReportRow>>

    @Query("SELECT * FROM repair_entries WHERE customerName LIKE '%' || :query || '%' OR customerMobile LIKE '%' || :query || '%' OR dealerName LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<RepairEntry>>
}

data class DailyReportRow(
    val dateGroup: String,
    val count: Int,
    val totalRevenue: Double?
)
