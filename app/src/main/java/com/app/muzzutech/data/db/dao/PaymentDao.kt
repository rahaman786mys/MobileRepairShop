package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE status != 'PAID' ORDER BY dueAmount DESC")
    fun getPendingDues(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE personType = :type AND status != 'PAID' ORDER BY dueAmount DESC")
    fun getDuesByType(type: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE personMobile = :mobile ORDER BY createdAt DESC")
    fun getPaymentsByMobile(mobile: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): Payment?

    @Query("SELECT COALESCE(SUM(dueAmount), 0) FROM payments WHERE status != 'PAID'")
    fun getTotalDueAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(dueAmount), 0) FROM payments WHERE personType = :type AND status != 'PAID'")
    fun getTotalDueByType(type: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(dueAmount), 0) FROM payments WHERE personMobile = :mobile AND status != 'PAID'")
    fun getTotalDueByMobile(mobile: String): Flow<Double>
}
