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
}
