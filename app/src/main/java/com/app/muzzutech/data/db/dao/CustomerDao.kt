package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile")
    suspend fun getCustomerByMobile(mobile: String): Customer?

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile")
    fun getCustomerByMobileFlow(mobile: String): Flow<Customer?>

    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>
}
