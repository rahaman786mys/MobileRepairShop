package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.ServiceMan
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceManDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(serviceMan: ServiceMan): Long

    @Update
    suspend fun update(serviceMan: ServiceMan)

    @Delete
    suspend fun delete(serviceMan: ServiceMan)

    @Query("SELECT * FROM service_men ORDER BY name ASC")
    fun getAllServiceMen(): Flow<List<ServiceMan>>

    @Query("SELECT * FROM service_men WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveServiceMen(): Flow<List<ServiceMan>>

    @Query("SELECT * FROM service_men WHERE id = :id")
    suspend fun getServiceManById(id: Long): ServiceMan?

    @Query("SELECT * FROM service_men WHERE id = :id")
    fun getServiceManByIdFlow(id: Long): Flow<ServiceMan?>
}
