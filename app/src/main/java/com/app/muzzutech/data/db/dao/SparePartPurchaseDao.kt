package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.SparePartPurchase
import kotlinx.coroutines.flow.Flow

@Dao
interface SparePartPurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: SparePartPurchase): Long

    @Update
    suspend fun update(purchase: SparePartPurchase)

    @Delete
    suspend fun delete(purchase: SparePartPurchase)

    @Query("SELECT * FROM spare_part_purchases WHERE repairEntryId = :repairId ORDER BY purchaseDate DESC")
    fun getPurchasesByRepairId(repairId: Long): Flow<List<SparePartPurchase>>

    @Query("SELECT * FROM spare_part_purchases ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<SparePartPurchase>>

    @Query("SELECT * FROM spare_part_purchases WHERE supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun getPurchasesBySupplier(supplierId: Long): Flow<List<SparePartPurchase>>

    @Query("SELECT * FROM spare_part_purchases WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getPurchasesByDateRange(startDate: Long, endDate: Long): Flow<List<SparePartPurchase>>

    @Query("SELECT SUM(purchasePrice * quantity) FROM spare_part_purchases WHERE purchaseDate BETWEEN :startDate AND :endDate")
    fun getTotalPurchaseInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM spare_part_purchases")
    fun getTotalPurchasesCount(): Flow<Int>
}
