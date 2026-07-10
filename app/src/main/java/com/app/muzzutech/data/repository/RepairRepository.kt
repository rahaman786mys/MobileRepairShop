package com.app.muzzutech.data.repository

import com.app.muzzutech.data.db.dao.RepairEntryDao
import com.app.muzzutech.data.db.dao.DailyReportRow
import com.app.muzzutech.data.model.RepairEntry
import androidx.room.withTransaction
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

    fun getRevenueInRange(startDate: Long, endDate: Long): Flow<Long?> =
        repairEntryDao.getRevenueInRange(startDate, endDate)

    fun getDailyReport(startDate: Long, endDate: Long): Flow<List<DailyReportRow>> =
        repairEntryDao.getDailyReport(startDate, endDate)

    fun searchEntries(query: String): Flow<List<RepairEntry>> =
        repairEntryDao.searchEntries(query)

    suspend fun insert(entry: RepairEntry): Long = repairEntryDao.insert(entry)

    /** Guarded update — silently refuses if handoverDone == 1. Returns true if applied. */
    suspend fun update(entry: RepairEntry): Boolean {
        val current = getEntryById(entry.id) ?: return false
        if (current.handoverDone) return false
        repairEntryDao.update(entry)
        return true
    }

    /** Force-update even completed entries (for adjusting entries only). */
    suspend fun forceUpdate(entry: RepairEntry) = repairEntryDao.update(entry)

    /** 
     * Atomic delete of a repair entry and all its associated financial footprints.
     * This fixes the "Phantom Balance" bug where deleting an entry left dues behind.
     */
    suspend fun delete(entry: RepairEntry, database: com.app.muzzutech.data.db.AppDatabase) {
        database.withTransaction {
            // 1. Delete linked Payments (this should cascade SET NULL to transactions via FK, 
            // but we want to delete transactions too if they belong to this payment).
            val linkedPayment = database.paymentDao().getPaymentByLinkedEntryId(entry.id)
            if (linkedPayment != null) {
                // Find and delete all cash-flow events for this repair
                val txns = database.paymentTransactionDao().getTransactionsByPaymentList(linkedPayment.id)
                for (txn in txns) {
                    database.paymentTransactionDao().delete(txn)
                }
                database.paymentDao().delete(linkedPayment)
            }
            
            // 2. Delete linked Spare Part Purchases and their payments
            val parts = database.sparePartPurchaseDao().getPurchasesByRepairIdList(entry.id)
            for (part in parts) {
                val partPayment = database.paymentDao().getPaymentByLinkedPartId(part.id)
                if (partPayment != null) {
                    val partTxns = database.paymentTransactionDao().getTransactionsByPaymentList(partPayment.id)
                    for (ptxn in partTxns) {
                        database.paymentTransactionDao().delete(ptxn)
                    }
                    database.paymentDao().delete(partPayment)
                }
                database.sparePartPurchaseDao().delete(part)
            }

            // 3. Finally delete the entry
            repairEntryDao.delete(entry)
        }
    }

    suspend fun delete(entry: RepairEntry) = repairEntryDao.delete(entry)
}
