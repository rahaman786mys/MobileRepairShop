package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.ServiceMan
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceManDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(serviceMan: ServiceMan): Long

    @Update
    suspend fun update(serviceMan: ServiceMan): Int

    @Delete
    suspend fun delete(serviceMan: ServiceMan): Int

    @Query("SELECT * FROM service_men ORDER BY name ASC")
    fun getAllServiceMen(): Flow<List<ServiceMan>>

    @Query("SELECT * FROM service_men WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveServiceMen(): Flow<List<ServiceMan>>

    @Query("SELECT * FROM service_men WHERE id = :id")
    suspend fun getServiceManById(id: Long): ServiceMan?

    @Query("SELECT * FROM service_men WHERE id = :id")
    fun getServiceManByIdFlow(id: Long): Flow<ServiceMan?>

    @Query("SELECT COUNT(*) FROM service_men")
    suspend fun getCount(): Int

    @Query("SELECT * FROM service_men WHERE ownerId = :ownerId AND mobile = :phone")
    suspend fun getWorkerByPhone(ownerId: String, phone: String): ServiceMan?

    @Query("SELECT * FROM service_men WHERE ownerId = :ownerId AND canLogin = 1 ORDER BY name ASC")
    fun getActiveWorkersByOwner(ownerId: String): Flow<List<ServiceMan>>

    // All login workers for an owner, including ones whose login was revoked
    // (canLogin = 0), so the owner can regenerate a password for them.
    @Query("SELECT * FROM service_men WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getWorkersByOwner(ownerId: String): Flow<List<ServiceMan>>

    @Query("SELECT COUNT(*) FROM service_men WHERE ownerId = :ownerId")
    suspend fun getWorkerCountByOwner(ownerId: String): Int

    @Query("SELECT * FROM service_men WHERE mobile = :phone")
    suspend fun findByPhone(phone: String): ServiceMan?
}
