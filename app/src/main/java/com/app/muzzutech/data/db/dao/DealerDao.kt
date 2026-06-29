package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.Dealer
import kotlinx.coroutines.flow.Flow

@Dao
interface DealerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dealer: Dealer)

    @Update
    suspend fun update(dealer: Dealer)

    @Query("SELECT * FROM dealers WHERE mobileNumber = :mobile")
    suspend fun getDealerByMobile(mobile: String): Dealer?

    @Query("SELECT * FROM dealers ORDER BY createdAt DESC")
    fun getAllDealers(): Flow<List<Dealer>>
}
