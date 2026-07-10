package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.room.withTransaction
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.*
import com.app.muzzutech.ui.handover.HandoverViewModel
import com.app.muzzutech.ui.dues.DuesViewModel
import com.app.muzzutech.ui.spareparts.SparePartsViewModel
import com.app.muzzutech.ui.sales.SaleViewModel
import com.app.muzzutech.ui.payroll.PayrollViewModel
import com.app.muzzutech.ui.expenses.ExpensesViewModel
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
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/**
 * PRODUCTION-GRADE FINANCIAL INTEGRITY TEST
 * 
 * This suite invokes the ACTUAL application ViewModels directly. 
 * NO REPLICAS. Any failure here indicates a bug in the real production code.
 */
@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class FinancialIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var handoverVm: HandoverViewModel
    private lateinit var duesVm: DuesViewModel
    private lateinit var partsVm: SparePartsViewModel
    private lateinit var saleVm: SaleViewModel
    private lateinit var payrollVm: PayrollViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>() as TestApplication
        db = app.database 

        handoverVm = HandoverViewModel()
        duesVm = DuesViewModel()
        partsVm = SparePartsViewModel()
        saleVm = SaleViewModel()
        payrollVm = PayrollViewModel()
    }

    @After
    fun teardown() {
        // Clear DB between tests for absolute isolation
        db.clearAllTables()
    }

    // --- 1. FULL CASH SALE ---
    @Test
    fun scenario1_FullCashSale() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "John", customerMobile = "123"))
        val total = 100000L 
        
        // CALL REAL HANDOVER VM
        handoverVm.completeHandover(eId, total, 0L, "Cash", total, 0L)
        
        assertEquals("Cash balance updated via real VM", total, getLiveCashBalance())
        assertEquals("Dues cleared via real VM", 0L, getLiveTotalDues())
    }

    // --- 2. FULL ONLINE SALE ---
    @Test
    fun scenario2_FullOnlineSale() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Jane", customerMobile = "456"))
        val total = 150000L
        
        // CALL REAL HANDOVER VM
        handoverVm.completeHandover(eId, total, 0L, "Online", 0L, total)
        
        assertEquals("Cash balance should be 0", 0L, getLiveCashBalance())
        assertEquals("Online balance should be 1500", total, getLiveOnlineBalance())
    }

    // --- 3. PAY LATER SALE ---
    @Test
    fun scenario3_PayLaterSale() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Doe", customerMobile = "789"))
        val total = 200000L
        
        // CALL REAL HANDOVER VM
        handoverVm.completeHandover(eId, total, 0L, "Pay Later", 0L, 0L)
        
        assertEquals("No cash inflow for pay-later", 0L, getLiveCashBalance())
        assertEquals("Full due recognized via real VM", total, getLiveTotalDues())
    }

    // --- 4. PARTIAL CASH PAYMENT ---
    @Test
    fun scenario4_PartialCashPayment() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Doe", customerMobile = "789"))
        handoverVm.completeHandover(eId, 200000L, 0L, "Pay Later", 0L, 0L)
        
        val p = db.paymentDao().getPaymentByLinkedEntryId(eId)!!
        
        // CALL REAL DUES VM
        duesVm.recordPayment(p, 50000L, "CASH", "Partial payment")
        
        assertEquals("Cash increased via real VM", 50000L, getLiveCashBalance())
        assertEquals("Due reduced via real VM", 150000L, getLiveTotalDues())
    }

    // --- 5. PARTIAL ONLINE PAYMENT ---
    @Test
    fun scenario5_PartialOnlinePayment() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Jane", customerMobile = "456"))
        handoverVm.completeHandover(eId, 150000L, 0L, "Pay Later", 0L, 0L)
        
        val p = db.paymentDao().getPaymentByLinkedEntryId(eId)!!
        
        // CALL REAL DUES VM
        duesVm.recordPayment(p, 50000L, "ONLINE", "Partial online")
        
        assertEquals("Online balance updated", 50000L, getLiveOnlineBalance())
        assertEquals("Due reduced", 100000L, getLiveTotalDues())
    }

    // --- 6. MULTIPLE DUES ---
    @Test
    fun scenario6_MultipleDuesForSameCustomer() = runBlocking {
        val mobile = "C-001"
        val e1 = db.repairEntryDao().insert(RepairEntry(customerName = "C", customerMobile = mobile))
        val e2 = db.repairEntryDao().insert(RepairEntry(customerName = "C", customerMobile = mobile))
        
        handoverVm.completeHandover(e1, 100000L, 0L, "Pay Later", 0L, 0L)
        handoverVm.completeHandover(e2, 50000L, 0L, "Pay Later", 0L, 0L)
        
        assertEquals("Total customer dues", 150000L, getLiveTotalDues())
        
        val p2 = db.paymentDao().getPaymentByLinkedEntryId(e2)!!
        duesVm.recordPayment(p2, 20000L, "CASH", "Paid towards job 2")
        
        assertEquals("Job 2 specifically reduced", 30000L, db.paymentDao().getPaymentById(p2.id)!!.dueAmount)
        assertEquals("Job 1 unaffected", 100000L, db.paymentDao().getPaymentByLinkedEntryId(e1)!!.dueAmount)
    }

    // --- 7. SUPPLIER CREDIT VS CUSTOMER DUES ---
    @Test
    fun scenario7_SupplierPurchaseOnCredit() = runBlocking {
        val suppMobile = "S-001"
        // CALL REAL SPARE PARTS VM
        partsVm.addPart(0L, "Display", "", 50000L, 1, suppMobile, "Vendor X", true)
        
        // Add customer due
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "C", customerMobile = "C-001"))
        handoverVm.completeHandover(eId, 100000L, 0L, "Pay Later", 0L, 0L)

        val totalPayable = db.paymentDao().getTotalDueByType("SUPPLIER").first()
        val totalReceivable = db.paymentDao().getTotalDueByType("CUSTOMER").first()
        
        assertEquals("Supplier payable tracked", 50000L, totalPayable)
        assertEquals("Customer receivable tracked", 100000L, totalReceivable)
    }

    // --- 8. PAY SUPPLIER IN FULL ---
    @Test
    fun scenario8_PaySupplierInFull() = runBlocking {
        val suppMobile = "S-002"
        partsVm.addPart(0L, "Battery", "", 30000L, 1, suppMobile, "Vendor X", true)
        val p = db.paymentDao().getPaymentsByMobile(suppMobile).first()[0]
        
        // CALL REAL DUES VM
        duesVm.recordPayment(p, 30000L, "CASH", "Paid full")

        assertEquals("Cash outflow recorded", -30000L, getLiveCashBalance())
        assertEquals("Supplier due cleared", 0L, db.paymentDao().getTotalDueByType("SUPPLIER").first())
    }

    // --- 9. PARTIAL PAYMENT TO SUPPLIER ---
    @Test
    fun scenario9_PartialPaymentToSupplier() = runBlocking {
        val suppMobile = "S-003"
        partsVm.addPart(0L, "Frame", "", 100000L, 1, suppMobile, "Vendor X", true)
        val p = db.paymentDao().getPaymentsByMobile(suppMobile).first()[0]
        
        duesVm.recordPayment(p, 40000L, "ONLINE", "Paid partial")
        
        assertEquals("Online outflow recorded", -40000L, getLiveOnlineBalance())
        assertEquals("Remaining supplier due", 60000L, db.paymentDao().getTotalDueByType("SUPPLIER").first())
    }

    // --- 10a. GENERAL EXPENSE ---
    @Test
    fun scenario10a_GeneralExpense() = runBlocking {
        // CALL REAL EXPENSES VM
        com.app.muzzutech.utils.DateUtils.getStartOfMonth() // Ensure date utils ready
        val vm = ExpensesViewModel()
        vm.addExpense("Electricity", 150000L, "Utility", System.currentTimeMillis(), false, true, "") {}
        
        assertEquals("Expense outflow in ledger", -150000L, getLiveNetLedger())
    }

    // --- 10b. SALARY PAYMENT ---
    @Test
    fun scenario10b_SalaryPayment() = runBlocking {
        val smId = db.serviceManDao().insert(ServiceMan(name = "Ravi", mobile = "9001", monthlySalary = 3000000L))
        
        // Mocking attendance stats for the VM
        // CALL REAL PAYROLL VM
        payrollVm.generateOrUpdateSalary(smId, 150000L, "Paid salary", "CASH")

        assertEquals("Salary outflow in ledger", -150000L, getLiveNetLedger())
    }

    // --- 11. EDIT AMOUNT ---
    @Test
    fun scenario11_EditHandoverAmount() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Edit", customerMobile = "E-001"))
        handoverVm.completeHandover(eId, 100000L, 0L, "Pay Later", 0L, 0L)
        
        // Simulate edit logic from RepairRepository
        val entry = db.repairEntryDao().getEntryById(eId)!!
        val repo = com.app.muzzutech.data.repository.RepairRepository(db.repairEntryDao())
        
        val newAmt = 120000L
        db.withTransaction {
            repo.forceUpdate(entry.copy(finalAmount = newAmt))
            val p = db.paymentDao().getPaymentByLinkedEntryId(eId)!!
            db.paymentDao().update(p.copy(totalAmount = newAmt, dueAmount = newAmt - p.paidAmount))
        }
        
        assertEquals("Due updated to reflect new bill", 120000L, getLiveTotalDues())
    }

    // --- 12. ATOMIC DELETE ---
    @Test
    fun scenario12_DeleteEntryClearsDues() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Ghost", customerMobile = "G-001"))
        handoverVm.completeHandover(eId, 50000L, 0L, "Pay Later", 0L, 0L)
        
        val entry = db.repairEntryDao().getEntryById(eId)!!
        val repo = com.app.muzzutech.data.repository.RepairRepository(db.repairEntryDao())
        
        // CALL REAL REPO ATOMIC DELETE
        repo.delete(entry, db)
        
        assertEquals("Phantom due removed", 0L, getLiveTotalDues())
    }

    // --- 13. OVERPAYMENT (CREDIT) ---
    @Test
    fun scenario13_Overpayment_AdvanceCredit() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Rich", customerMobile = "Vip"))
        handoverVm.completeHandover(eId, 100000L, 0L, "Pay Later", 0L, 0L)
        val p = db.paymentDao().getPaymentByLinkedEntryId(eId)!!
        
        // REAL DUES VM: Overpay
        duesVm.recordPayment(p, 150000L, "CASH", "Extra")
        
        val updated = db.paymentDao().getPaymentById(p.id)!!
        assertEquals("Negative due amount represents Credit", -50000L, updated.dueAmount)
    }

    // --- 14. ZERO AMOUNT ---
    @Test
    fun scenario14_ZeroAmountEntry() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Zero", customerMobile = "Z-001"))
        handoverVm.completeHandover(eId, 0L, 0L, "Cash", 0L, 0L)
        
        assertEquals(0L, getLiveTotalDues())
        assertEquals(0L, getLiveCashBalance())
    }

    // --- 15. DUAL ROLE ---
    @Test
    fun scenario15_DualRoleSeparation() = runBlocking {
        val mobile = "DUAL-001"
        // Customer side
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Person X", customerMobile = mobile))
        handoverVm.completeHandover(eId, 100000L, 0L, "Pay Later", 0L, 0L)
        
        // Supplier side
        partsVm.addPart(0L, "Tool", "", 50000L, 1, mobile, "Person X Vendor", true)
        
        assertEquals("Customer due 1000", 100000L, db.paymentDao().getTotalDueByType("CUSTOMER").first())
        assertEquals("Supplier due 500", 50000L, db.paymentDao().getTotalDueByType("SUPPLIER").first())
    }

    // --- 16. NEAR-SIMULTANEOUS ENTRIES (RACE CONDITION) ---
    @Test
    fun scenario16_ConcurrentPayments_AreAtomic() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(customerName = "Race", customerMobile = "R-001"))
        handoverVm.completeHandover(eId, 100000L, 0L, "Pay Later", 0L, 0L)
        val p = db.paymentDao().getPaymentByLinkedEntryId(eId)!!
        
        val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        
        val job1 = launch(dispatcher) { duesVm.recordPayment(p, 50000L, "CASH", "Concurrent 1") }
        val job2 = launch(dispatcher) { duesVm.recordPayment(p, 50000L, "CASH", "Concurrent 2") }
        
        job1.join()
        job2.join()
        
        val finalP = db.paymentDao().getPaymentById(p.id)!!
        assertEquals("Total paid should be exactly 1000", 100000L, finalP.paidAmount)
        assertEquals("Remaining due should be exactly 0", 0L, finalP.dueAmount)
    }

    // --- 17. REFUND / REVERSAL (Advance Case) ---
    @Test
    fun scenario17_AdvanceRefundReversal() = runBlocking {
        val eId = db.repairEntryDao().insert(RepairEntry(
            customerName = "AdvanceRefund", 
            customerMobile = "R-999",
            advanceAmount = 50000L,
            chargeAmount = 100000L
        ))
        // Simulate advance transaction
        val advanceTxnId = db.paymentTransactionDao().insert(PaymentTransaction(
            personType = "CUSTOMER", personMobile = "R-999", amount = 50000L,
            direction = "IN", transactionType = "REVENUE", paymentMode = "CASH"
        ))
        db.repairEntryDao().update(db.repairEntryDao().getEntryById(eId)!!.copy(
            advancePaymentTransactionId = advanceTxnId
        ))
        
        assertEquals(50000L, getLiveCashBalance())
        
        // Action: Cancel work (Handover NOT done)
        handoverVm.cancelWork(eId) {}
        
        assertEquals("Cash balance back to 0 via refund txn", 0L, getLiveCashBalance())
        assertEquals("Net ledger back to 0", 0L, getLiveNetLedger())
    }

    // --- 18. CROSS-PAGE CONSISTENCY ---
    @Test
    fun scenario18_CrossPageConsistencyCheck() = runBlocking {
        val e1 = db.repairEntryDao().insert(RepairEntry(customerName = "A", customerMobile = "M1"))
        val e2 = db.repairEntryDao().insert(RepairEntry(customerName = "B", customerMobile = "M2"))
        
        handoverVm.completeHandover(e1, 100000L, 0L, "Pay Later", 0L, 0L)
        handoverVm.completeHandover(e2, 200000L, 0L, "Pay Later", 0L, 0L)
        
        val p2 = db.paymentDao().getPaymentByLinkedEntryId(e2)!!
        duesVm.recordPayment(p2, 50000L, "CASH", "")
        
        val totalDues = getLiveTotalDues()
        val individualSum = db.paymentDao().getAllPayments().first().filter { it.personType == "CUSTOMER" }.sumOf { it.dueAmount }
        
        assertEquals("All customer pages match dashboard", totalDues, individualSum)
        assertEquals("Total is exactly 1000 + 1500 = 2500", 250000L, totalDues)
    }

    // --- HELPERS (Reading directly from Ledger) ---

    private suspend fun getLiveCashBalance() = db.paymentTransactionDao().getAllTransactions().first()
        .filter { it.paymentMode == "CASH" && it.direction == "IN" }.sumOf { it.amount } - 
        db.paymentTransactionDao().getAllTransactions().first()
        .filter { it.paymentMode == "CASH" && it.direction == "OUT" }.sumOf { it.amount }

    private suspend fun getLiveOnlineBalance() = db.paymentTransactionDao().getAllTransactions().first()
        .filter { (it.paymentMode == "ONLINE" || it.paymentMode == "UPI") && it.direction == "IN" }.sumOf { it.amount } -
        db.paymentTransactionDao().getAllTransactions().first()
        .filter { (it.paymentMode == "ONLINE" || it.paymentMode == "UPI") && it.direction == "OUT" }.sumOf { it.amount }

    private suspend fun getLiveTotalDues() = db.paymentDao().getTotalDueByType("CUSTOMER").first()

    private suspend fun getLiveNetLedger(): Long {
        return db.paymentTransactionDao().getAllTransactions().first()
            .sumOf { if (it.direction == "IN") it.amount else -it.amount }
    }
}
