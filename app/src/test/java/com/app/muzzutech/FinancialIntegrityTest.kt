package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.*
import com.app.muzzutech.data.repository.RepairRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class FinancialIntegrityTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- SCENARIO 1: FULL CASH SALE ---
    @Test
    fun scenario1_FullCashSale() = runBlocking {
        val entryId = db.repairEntryDao().insert(RepairEntry(customerName = "John", customerMobile = "123"))
        val total = 100000L 
        completeHandover(entryId, total, "Cash", total, 0L)
        
        assertEquals("Cash balance should be 1000", total, getLiveCashBalance())
        assertEquals("Dues should be 0", 0L, getLiveTotalDues())
    }

    // --- SCENARIO 2: FULL ONLINE SALE ---
    @Test
    fun scenario2_FullOnlineSale() = runBlocking {
        val entryId = db.repairEntryDao().insert(RepairEntry(customerName = "Jane", customerMobile = "456"))
        val total = 150000L
        completeHandover(entryId, total, "Online", 0L, total)
        
        assertEquals("Cash balance should be 0", 0L, getLiveCashBalance())
        assertEquals("Online balance should be 1500", total, getLiveOnlineBalance())
    }

    // --- SCENARIO 3: PAY LATER SALE ---
    @Test
    fun scenario3_PayLaterSale() = runBlocking {
        val entryId = db.repairEntryDao().insert(RepairEntry(customerName = "Doe", customerMobile = "789"))
        val total = 200000L
        completeHandover(entryId, total, "Pay Later", 0L, 0L)
        
        assertEquals("Dues should be 2000", total, getLiveTotalDues())
        val p = db.paymentDao().getPaymentByLinkedEntryId(entryId)
        assertEquals("UNPAID", p?.status)
    }

    // --- SCENARIO 4: PARTIAL CASH PAYMENT ---
    @Test
    fun scenario4_PartialCashPayment() = runBlocking {
        val entryId = db.repairEntryDao().insert(RepairEntry(customerName = "Doe", customerMobile = "789"))
        completeHandover(entryId, 200000L, "Pay Later", 0L, 0L)
        val p = db.paymentDao().getPaymentByLinkedEntryId(entryId)!!
        
        recordPayment(p, 50000L, "CASH")
        
        assertEquals("Cash balance should be 500", 50000L, getLiveCashBalance())
        assertEquals("Remaining due should be 1500", 150000L, getLiveTotalDues())
    }

    // --- SCENARIO 7: SUPPLIER CREDIT ---
    @Test
    fun scenario7_SupplierPurchaseOnCredit() = runBlocking {
        val mobile = "999888"
        db.withTransaction {
            val partId = db.sparePartPurchaseDao().insert(SparePartPurchase(partName = "Display", purchasePrice = 50000L, supplierId = mobile))
            db.paymentDao().insert(Payment(personType = "SUPPLIER", personMobile = mobile, totalAmount = 50000L, dueAmount = 50000L, status = "UNPAID", linkedPartId = partId))
        }
        val supplierDues = db.paymentDao().getTotalDueByType("SUPPLIER").first()
        assertEquals(50000L, supplierDues)
    }

    // --- SCENARIO 12: ATOMIC DELETE ---
    @Test
    fun scenario12_DeletePayLaterEntry_ClearsDues() = runBlocking {
        val entryId = db.repairEntryDao().insert(RepairEntry(customerName = "Ghost", customerMobile = "000"))
        completeHandover(entryId, 50000L, "Pay Later", 0L, 0L)
        
        val repo = RepairRepository(db.repairEntryDao())
        val entry = db.repairEntryDao().getEntryById(entryId)!!
        repo.delete(entry, db)

        assertEquals("Dues must be 0 after delete", 0L, getLiveTotalDues())
    }

    // --- SCENARIO 13: OVERPAYMENT (CREDIT) ---
    @Test
    fun scenario18_CrossPageConsistencyCheck() = runBlocking {
        // Randomly add entries for 3 entities
        val e1 = db.repairEntryDao().insert(RepairEntry(customerName = "Alpha", customerMobile = "M1"))
        completeHandover(e1, 100000L, "Pay Later", 0L, 0L)
        
        val e2 = db.repairEntryDao().insert(RepairEntry(customerName = "Beta", customerMobile = "M2"))
        completeHandover(e2, 200000L, "Pay Later", 0L, 0L)
        
        val e3 = db.repairEntryDao().insert(RepairEntry(customerName = "Gamma", customerMobile = "M3"))
        completeHandover(e3, 300000L, "Pay Later", 0L, 0L)
        
        // Beta pays partially
        val betaPayment = db.paymentDao().getPaymentByLinkedEntryId(e2)!!
        recordPayment(betaPayment, 50000L, "CASH")
        
        // 1. Dashboard Total Dues
        val dashboardDues = getLiveTotalDues() // 1000 + (2000 - 500) + 3000 = 5500
        assertEquals(550000L, dashboardDues)
        
        // 2. Beta's individual page
        val betaDue = db.paymentDao().getTotalDueByMobile("M2").first()
        assertEquals(150000L, betaDue)
        
        // 3. Beta's Ledger
        val betaTxns = db.paymentTransactionDao().getTransactionsByMobile("M2").first()
        assertEquals(1, betaTxns.size)
        assertEquals(50000L, betaTxns[0].amount)
        
        // 4. Alpha's deletion
        val alphaEntry = db.repairEntryDao().getEntryById(e1)!!
        RepairRepository(db.repairEntryDao()).delete(alphaEntry, db)
        
        // Verify all 3 pages agree on new total
        val finalDues = getLiveTotalDues() // 5500 - 1000 = 4500
        assertEquals(450000L, finalDues)
        assertEquals(0L, db.paymentDao().getTotalDueByMobile("M1").first())
    }

    // --- HELPERS ---

    private suspend fun getLiveCashBalance() = db.paymentTransactionDao().getAllTransactions().first()
        .filter { it.paymentMode == "CASH" && it.direction == "IN" }.sumOf { it.amount } - 
        db.paymentTransactionDao().getAllTransactions().first()
        .filter { it.paymentMode == "CASH" && it.direction == "OUT" }.sumOf { it.amount }

    private suspend fun getLiveOnlineBalance() = db.paymentTransactionDao().getAllTransactions().first()
        .filter { (it.paymentMode == "ONLINE" || it.paymentMode == "UPI") && it.direction == "IN" }.sumOf { it.amount }

    private suspend fun getLiveTotalDues() = db.paymentDao().getAllPayments().first().sumOf { it.dueAmount }

    private suspend fun completeHandover(id: Long, amt: Long, mode: String, cash: Long, online: Long) {
        db.withTransaction {
            val entry = db.repairEntryDao().getEntryById(id)!!
            db.repairEntryDao().update(entry.copy(handoverDone = true, finalAmount = amt))
            val isPayLater = mode == "Pay Later"
            val paidTotal = if (isPayLater) 0L else (cash + online)
            val paymentId = db.paymentDao().insert(Payment(
                personType = "CUSTOMER", personMobile = entry.customerMobile, personName = entry.customerName,
                totalAmount = amt, paidAmount = paidTotal, dueAmount = amt - paidTotal,
                status = if (isPayLater) "UNPAID" else "PAID", linkedEntryId = id
            ))
            if (!isPayLater) {
                if (cash > 0) db.paymentTransactionDao().insert(PaymentTransaction(
                    paymentId = paymentId, personType = "CUSTOMER", personMobile = entry.customerMobile,
                    amount = cash, direction = "IN", transactionType = "REVENUE", paymentMode = "CASH"
                ))
                if (online > 0) db.paymentTransactionDao().insert(PaymentTransaction(
                    paymentId = paymentId, personType = "CUSTOMER", personMobile = entry.customerMobile,
                    amount = online, direction = "IN", transactionType = "REVENUE", paymentMode = "ONLINE"
                ))
            }
        }
    }

    private suspend fun recordPayment(p: Payment, amt: Long, mode: String) {
        db.withTransaction {
            db.paymentDao().atomicAddPayment(p.id, amt, System.currentTimeMillis())
            db.paymentTransactionDao().insert(PaymentTransaction(
                paymentId = p.id, personType = p.personType, personMobile = p.personMobile,
                amount = amt, direction = "IN", transactionType = "REVENUE", paymentMode = mode
            ))
        }
    }
}
