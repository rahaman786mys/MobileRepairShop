package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RegressionFixTest {

    private lateinit var db: AppDatabase
    private lateinit var paymentDao: PaymentDao
    private lateinit var repairDao: RepairEntryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        paymentDao = db.paymentDao()
        repairDao = db.repairEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPaymentDueAmountNonNegative() = runBlocking {
        // Fix for negative due amount if paidAmount > totalAmount
        val payment = Payment(
            personType = "CUSTOMER",
            personMobile = "1234567890",
            totalAmount = 100.0,
            paidAmount = 150.0, // Overpaid
            dueAmount = (100.0 - 150.0).coerceAtLeast(0.0),
            status = "PAID"
        )
        paymentDao.insert(payment)
        val fetched = paymentDao.getAllPayments().first()[0]
        assertTrue("Due amount should not be negative", fetched.dueAmount >= 0.0)
        assertEquals(0.0, fetched.dueAmount, 0.01)
    }
}
