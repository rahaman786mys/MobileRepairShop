package com.app.muzzutech.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.muzzutech.data.model.SalaryPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: SalaryPayment): Long

    @Update
    suspend fun update(payment: SalaryPayment): Int

    @Query("SELECT * FROM salary_payments WHERE servicemanId = :smId ORDER BY monthStart DESC")
    fun getByServiceMan(smId: Long): Flow<List<SalaryPayment>>

    @Query("SELECT * FROM salary_payments WHERE monthStart BETWEEN :start AND :end ORDER BY monthStart DESC")
    fun getByMonth(start: Long, end: Long): Flow<List<SalaryPayment>>

    @Query("SELECT * FROM salary_payments WHERE servicemanId = :smId AND monthStart = :monthStart LIMIT 1")
    suspend fun getByServiceManAndMonth(smId: Long, monthStart: Long): SalaryPayment??

    @Query("SELECT * FROM salary_payments WHERE id = :id")
    suspend fun getById(id: Long): SalaryPayment?

    @Query("DELETE FROM salary_payments WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM salary_payments WHERE status != 'PAID' ORDER BY monthStart DESC")
    fun getPendingPayments(): Flow<List<SalaryPayment>>

    @Query("SELECT COALESCE(SUM(paidAmount), 0.0) FROM salary_payments WHERE monthStart BETWEEN :start AND :end")
    suspend fun getTotalPaidInRange(start: Long, end: Long): Long

    @Query("SELECT COALESCE(SUM(dueAmount), 0) FROM salary_payments WHERE monthStart BETWEEN :start AND :end")
    suspend fun getTotalDueInRange(start: Long, end: Long): Long
}
