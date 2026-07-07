package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction): Long

    @Update
    suspend fun update(transaction: PaymentTransaction): Int

    @Delete
    suspend fun delete(transaction: PaymentTransaction): Int

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY transactionDate DESC")
    fun getTransactionsByPayment(paymentId: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE personMobile = :mobile ORDER BY transactionDate DESC")
    fun getTransactionsByMobile(mobile: String): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE personMobile = :mobile")
    fun getTotalPaidByMobile(mobile: String): Flow<Long>

    @Query("SELECT * FROM payment_transactions WHERE transactionDate BETWEEN :start AND :end")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): PaymentTransaction?

    @Query("SELECT * FROM payment_transactions WHERE expenseId = :expenseId LIMIT 1")
    suspend fun getTransactionByExpenseId(expenseId: Long): PaymentTransaction??

    @Query("SELECT * FROM payment_transactions WHERE personMobile = :mobile AND paymentId IS NULL AND amount = :amount LIMIT 1")
    suspend fun findUnlinkedByMobileAndAmount(mobile: String, amount: Long): PaymentTransaction?

    @Query("""
        SELECT strftime('%Y-%m-%d', transactionDate / 1000, 'unixepoch') as dateGroup,
               COUNT(*) as count,
               SUM(amount) as totalRevenue
        FROM payment_transactions
        WHERE personType IN ('CUSTOMER', 'DEALER') AND amount > 0
          AND transactionDate BETWEEN :startDate AND :endDate
        GROUP BY dateGroup
        ORDER BY dateGroup ASC
    """)
    fun getDailyCashReport(startDate: Long, endDate: Long): Flow<List<DailyReportRow>>
}
