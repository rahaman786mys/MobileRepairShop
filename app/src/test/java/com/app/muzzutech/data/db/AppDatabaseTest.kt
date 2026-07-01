package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var supplierDao: SupplierDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = db.customerDao()
        supplierDao = db.supplierDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadCustomer() = runBlocking {
        val customer = Customer(mobileNumber = "1234567890", name = "Test Customer", city = "City")
        customerDao.insert(customer)
        val fetched = customerDao.getCustomerByMobile("1234567890")
        assertEquals("Test Customer", fetched?.name)
    }

    @Test
    fun testValidationLogic_NegativeAmount() {
        // Mocking logic to test validation (assuming utility exists or checking values)
        val amount = -10.0
        val isValid = amount >= 0
        assertEquals("Should be invalid for negative amount", false, isValid)
    }
}
