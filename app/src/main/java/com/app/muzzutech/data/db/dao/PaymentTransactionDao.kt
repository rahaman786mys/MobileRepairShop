package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction): Long

    @Update
    suspend fun update(transaction: PaymentTransaction)

    @Delete
    suspend fun delete(transaction: PaymentTransaction)

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY transactionDate DESC")
    fun getTransactionsByPayment(paymentId: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE personMobile = :mobile ORDER BY transactionDate DESC")
    fun getTransactionsByMobile(mobile: String): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE personMobile = :mobile")
    fun getTotalPaidByMobile(mobile: String): Flow<Double>

    @Query("SELECT * FROM payment_transactions WHERE transactionDate BETWEEN :start AND :end")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<PaymentTransaction>>
}
