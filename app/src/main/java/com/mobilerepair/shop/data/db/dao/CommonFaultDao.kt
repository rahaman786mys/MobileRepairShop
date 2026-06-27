package com.mobilerepair.shop.data.db.dao

import androidx.room.*
import com.mobilerepair.shop.data.model.CommonFault
import kotlinx.coroutines.flow.Flow

@Dao
interface CommonFaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fault: CommonFault): Long

    @Update
    suspend fun update(fault: CommonFault)

    @Delete
    suspend fun delete(fault: CommonFault)

    @Query("SELECT * FROM common_faults ORDER BY sortOrder ASC, faultName ASC")
    fun getAllFaults(): Flow<List<CommonFault>>

    @Query("SELECT * FROM common_faults WHERE isActive = 1 ORDER BY sortOrder ASC, faultName ASC")
    fun getActiveFaults(): Flow<List<CommonFault>>

    @Query("SELECT * FROM common_faults WHERE category = :category ORDER BY sortOrder ASC, faultName ASC")
    fun getFaultsByCategory(category: String): Flow<List<CommonFault>>

    @Query("SELECT * FROM common_faults WHERE id = :id")
    suspend fun getFaultById(id: Long): CommonFault?
}
