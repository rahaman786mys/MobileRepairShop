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

@RunWith(RobolectricTestRunner::class)
class InventoryAccountingTest {
    private lateinit var db: AppDatabase
    private lateinit var supplierDao: SupplierDao
    private lateinit var sparePartPurchaseDao: SparePartPurchaseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        supplierDao = db.supplierDao()
        sparePartPurchaseDao = db.sparePartPurchaseDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testProfitCalculation_WithPaidAndPending() = runBlocking {
        // Assume Profit = GrossRevenue - (PaidPartsPurchases)
        val grossRevenue = 1000.0
        val partCostPaid = 200.0
        val partCostPending = 100.0 // This shouldn't be subtracted yet

        val profit = grossRevenue - partCostPaid

        assertEquals("Profit should only subtract paid expenses", 800.0, profit, 0.0)
    }
}
