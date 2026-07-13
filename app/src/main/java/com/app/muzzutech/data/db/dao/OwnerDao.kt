package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.Owner
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(owner: Owner)

    @Query("SELECT * FROM owners ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstOwner(): Owner?

    @Query("SELECT * FROM owners WHERE id = :ownerId")
    suspend fun getOwnerById(ownerId: String): Owner?

    @Query("SELECT * FROM owners WHERE id = :ownerId")
    fun getOwnerByIdFlow(ownerId: String): Flow<Owner?>

    @Query("SELECT * FROM owners WHERE email = :email")
    suspend fun getOwnerByEmail(email: String): Owner?

    @Query("SELECT * FROM owners WHERE phoneNumber = :phone")
    suspend fun getOwnerByPhone(phone: String): Owner?

    @Query("DELETE FROM owners WHERE id = :ownerId")
    suspend fun deleteById(ownerId: String)
}
