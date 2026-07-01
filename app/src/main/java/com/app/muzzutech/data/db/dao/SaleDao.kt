package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(sale: Sale)

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE supplierId = :supplierId ORDER BY saleDate DESC")
    fun getSalesBySupplier(supplierId: String): Flow<List<Sale>>
    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :start AND :end ORDER BY saleDate DESC")
    fun getSalesByDateRange(start: Long, end: Long): Flow<List<Sale>>
}
