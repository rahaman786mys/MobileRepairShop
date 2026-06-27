package com.mobilerepair.shop.data.db.dao

import androidx.room.*
import com.mobilerepair.shop.data.model.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: Supplier)

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE mobile = :mobile")
    suspend fun getSupplierByMobile(mobile: String): Supplier?

    @Query("SELECT * FROM suppliers WHERE mobile = :mobile")
    fun getSupplierByMobileFlow(mobile: String): Flow<Supplier?>
}
