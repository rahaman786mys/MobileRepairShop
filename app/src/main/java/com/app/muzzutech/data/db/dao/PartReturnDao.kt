package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.PartReturn
import kotlinx.coroutines.flow.Flow

@Dao
interface PartReturnDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partReturn: PartReturn): Long

    @Update
    suspend fun update(partReturn: PartReturn)

    @Delete
    suspend fun delete(partReturn: PartReturn)

    @Query("SELECT * FROM part_returns ORDER BY returnDate DESC")
    fun getAllReturns(): Flow<List<PartReturn>>

    @Query("SELECT * FROM part_returns WHERE supplierId = :supplierId ORDER BY returnDate DESC")
    fun getReturnsBySupplier(supplierId: String): Flow<List<PartReturn>>

    @Query("SELECT * FROM part_returns WHERE id = :id")
    suspend fun getReturnById(id: Long): PartReturn?
}
