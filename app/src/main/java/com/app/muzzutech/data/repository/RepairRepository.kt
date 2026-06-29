package com.app.muzzutech.data.repository

import com.app.muzzutech.data.db.dao.RepairEntryDao
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.data.model.RepairEntry
import kotlinx.coroutines.flow.Flow

class RepairRepository(private val repairEntryDao: RepairEntryDao) {

    fun getAllEntries(): Flow<List<RepairEntry>> = repairEntryDao.getAllEntries()

    fun getEntryByIdFlow(id: Long): Flow<RepairEntry?> = repairEntryDao.getEntryByIdFlow(id)

    suspend fun getEntryById(id: Long): RepairEntry? = repairEntryDao.getEntryById(id)

    fun getPendingEntries(): Flow<List<RepairEntry>> = repairEntryDao.getPendingEntries()

    fun getCompletedEntries(): Flow<List<RepairEntry>> = repairEntryDao.getCompletedEntries()

    fun getEntriesByServiceMan(serviceManId: Long): Flow<List<RepairEntry>> =
        repairEntryDao.getEntriesByServiceMan(serviceManId)

    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<RepairEntry>> =
        repairEntryDao.getEntriesByDateRange(startDate, endDate)

    fun getPendingCount(): Flow<Int> = repairEntryDao.getPendingCount()

    fun getCompletedCountInRange(startDate: Long, endDate: Long): Flow<Int> =
        repairEntryDao.getCompletedCountInRange(startDate, endDate)

    fun getRevenueInRange(startDate: Long, endDate: Long): Flow<Double?> =
        repairEntryDao.getRevenueInRange(startDate, endDate)

    fun getDailyReport(startDate: Long, endDate: Long): Flow<List<DailyReportRow>> =
        repairEntryDao.getDailyReport(startDate, endDate)

    fun searchEntries(query: String): Flow<List<RepairEntry>> =
        repairEntryDao.searchEntries(query)

    suspend fun insert(entry: RepairEntry): Long = repairEntryDao.insert(entry)

    suspend fun update(entry: RepairEntry) = repairEntryDao.update(entry)

    suspend fun delete(entry: RepairEntry) = repairEntryDao.delete(entry)
}
