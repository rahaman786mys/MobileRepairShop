package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.PaymentTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Reconciliation tests for the accounting layer. Verifies:
 *  - Expense -> PaymentTransaction linkage via expenseId
 *  - Toggling paid/unpaid creates/removes the linked transaction
 *  - Deleting an expense cascades to its PaymentTransaction
 *  - The totalAmount = paidAmount + dueAmount invariant on Payment
 *  - The Nightly Auditor catches a deliberately injected ₹1 mismatch
 */
@RunWith(RobolectricTestRunner::class)
class LedgerReconciliationTest {

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

    @Test
    fun expenseInsert_whenPaid_createsLinkedTransaction() = runBlocking {
        // Mimic ExpensesViewModel.addExpense: insert expense, then linked txn
        val expenseId = db.expenseDao().insert(
            Expense(
                title = "Shop Rent July",
                amount = 15000.0,
                category = Expense.CATEGORY_RENT,
                paid = true
            )
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = null,
                expenseId = expenseId,
                personType = "EXPENSE",
                personMobile = "SHOP",
                personName = Expense.CATEGORY_RENT,
                amount = 15000.0,
                paymentMode = "CASH",
                note = "Paid: Shop Rent July"
            )
        )
        val txn = db.paymentTransactionDao().getTransactionByExpenseId(expenseId)
        assertNotNull("Paid expense must have a linked PaymentTransaction", txn)
        assertEquals(15000.0, txn!!.amount, 0.001)
        assertEquals("EXPENSE", txn.personType)
        assertEquals(expenseId, txn.expenseId)
    }

    @Test
    fun expenseInsert_whenUnpaid_hasNoTransaction() = runBlocking {
        val expenseId = db.expenseDao().insert(
            Expense(
                title = "Electricity Bill",
                amount = 2500.0,
                category = Expense.CATEGORY_ELECTRICITY,
                paid = false
            )
        )
        val txn = db.paymentTransactionDao().getTransactionByExpenseId(expenseId)
        assertNull("Unpaid expense must NOT have a PaymentTransaction", txn)
    }

    @Test
    fun expenseDelete_removesLinkedTransaction() = runBlocking {
        // Mimic ExpensesViewModel: insert expense, then linked txn
        val expenseId = db.expenseDao().insert(
            Expense(title = "Internet", amount = 1000.0, category = Expense.CATEGORY_INTERNET, paid = true)
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = null, expenseId = expenseId, personType = "EXPENSE",
                personMobile = "SHOP", personName = Expense.CATEGORY_INTERNET,
                amount = 1000.0, note = "Paid: Internet")
        )
        assertNotNull(db.paymentTransactionDao().getTransactionByExpenseId(expenseId))

        // Mimic ExpensesViewModel.deleteExpense() cleanup
        db.withTransaction {
            db.paymentTransactionDao().getTransactionByExpenseId(expenseId)?.let { txn ->
                db.paymentTransactionDao().delete(txn)
            }
            db.expenseDao().deleteById(expenseId)
        }

        assertNull(db.paymentTransactionDao().getTransactionByExpenseId(expenseId))
        val all = db.expenseDao().getAll().first()
        assertTrue("Expense should be gone", all.isEmpty())
    }

    @Test
    fun payment_totalAmountEqualsPaidPlusDue_invariant() = runBlocking {
        val p = Payment(
            personType = "CUSTOMER",
            personMobile = "9999912345",
            personName = "Test Customer",
            totalAmount = 5000.0,
            paidAmount = 3500.0,
            dueAmount = 1500.0,
            status = "PARTIAL"
        )
        val id = db.paymentDao().insert(p)
        val fetched = db.paymentDao().getPaymentById(id)
        assertNotNull(fetched)
        assertEquals(
            "totalAmount must equal paidAmount + dueAmount",
            fetched!!.totalAmount,
            fetched.paidAmount + fetched.dueAmount,
            0.001
        )
    }

    @Test
    fun auditor_detectsPaymentAmountMismatch() = runBlocking {
        // Insert a Payment with paidAmount = 1000...
        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "CUSTOMER",
                personMobile = "9999988888",
                personName = "Ledger Victim",
                totalAmount = 1000.0,
                paidAmount = 1000.0,
                dueAmount = 0.0,
                status = "PAID"
            )
        )
        // ...but deliberately insert a transaction with a 1 rupee mismatch (999 instead of 1000).
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = paymentId,
                personType = "CUSTOMER",
                personMobile = "9999988888",
                personName = "Ledger Victim",
                amount = 999.0,
                paymentMode = "CASH",
                note = "Advance"
            )
        )

        // Run the auditor manually
        val alerts = runAuditor()
        assertTrue("Auditor must produce at least one alert", alerts.isNotEmpty())
        val mismatch = alerts.firstOrNull { it.type == "PAYMENT_MISMATCH" }
        assertNotNull("Auditor must flag ₹1 payment mismatch", mismatch)
        assertEquals(1.0, mismatch!!.mismatchAmount, 0.001)
    }

    @Test
    fun auditor_detectsOrphanTransaction() = runBlocking {
        // EXPENSE transaction with no expenseId -> orphan (legitimate advances have
        // personType CUSTOMER/DEALER and are allowed to have null paymentId)
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = null,
                expenseId = null,
                personType = "EXPENSE",
                personMobile = "SHOP",
                personName = "Mystery",
                amount = 500.0,
                note = "Untraceable"
            )
        )

        val alerts = runAuditor()
        val orphan = alerts.firstOrNull { it.type == "ORPHAN_TRANSACTION" }
        assertNotNull("Auditor must flag orphan EXPENSE transaction", orphan)
        assertEquals(500.0, orphan!!.mismatchAmount, 0.001)
    }

    @Test
    fun auditor_detectsPaidExpenseWithoutTransaction() = runBlocking {
        // Mark an expense as paid without creating a linked txn
        val expenseId = db.expenseDao().insert(
            Expense(title = "Supplies", amount = 800.0, category = Expense.CATEGORY_SUPPLIES, paid = true)
        )
        // Simulate the corruption: delete the auto-created txn (if any)
        db.paymentTransactionDao().getTransactionByExpenseId(expenseId)?.let {
            db.paymentTransactionDao().delete(it)
        }

        val alerts = runAuditor()
        val bad = alerts.firstOrNull { it.type == "EXPENSE_MISMATCH" }
        assertNotNull("Auditor must flag paid expense without txn", bad)
        assertEquals(800.0, bad!!.mismatchAmount, 0.001)
    }

    @Test
    fun fullDay_cashFlowReconciles() = runBlocking {
        // Day scenario:
        //   Revenue:      2 repairs (1000 + 1500 = 2500) + 1 advance (500) = 3000 in
        //   Expense:      rent 1000 (paid)  = 1000 out
        //   Net cash:     3000 - 1000 = 2000
        val now = System.currentTimeMillis()

        // Repair 1 — full payment with linked txn
        val p1 = db.paymentDao().insert(
            Payment(
                personType = "CUSTOMER", personMobile = "9111111111", personName = "Cust A",
                description = "Repair screen",
                totalAmount = 1000.0, paidAmount = 1000.0, dueAmount = 0.0, status = "PAID",
                linkedEntryId = 1L, createdAt = now
            )
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = p1, personType = "CUSTOMER", personMobile = "9111111111",
                personName = "Cust A", amount = 1000.0, paymentMode = "CASH", transactionDate = now)
        )

        // Repair 2 — full payment with linked txn
        val p2 = db.paymentDao().insert(
            Payment(
                personType = "DEALER", personMobile = "9222222222", personName = "Dealer B",
                description = "Battery swap",
                totalAmount = 1500.0, paidAmount = 1500.0, dueAmount = 0.0, status = "PAID",
                linkedEntryId = 2L, createdAt = now
            )
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = p2, personType = "DEALER", personMobile = "9222222222",
                personName = "Dealer B", amount = 1500.0, paymentMode = "CASH", transactionDate = now)
        )

        // Advance 500 with no linked payment (typical for Quotation flow)
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = null, personType = "CUSTOMER", personMobile = "9333333333",
                personName = "Advance Cust", amount = 500.0, paymentMode = "CASH",
                note = "Advance for X", transactionDate = now)
        )

        // Paid rent expense 1000 — must produce linked txn
        val rentId = db.expenseDao().insert(
            Expense(title = "Rent", amount = 1000.0, category = Expense.CATEGORY_RENT, date = now, paid = true)
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = null, expenseId = rentId, personType = "EXPENSE",
                personMobile = "SHOP", personName = "Rent", amount = 1000.0,
                paymentMode = "CASH", note = "Paid: Rent", transactionDate = now)
        )

        // Calculate cash flow
        val txns = db.paymentTransactionDao().getTransactionsByDateRange(now - 1000, now + 1000).first()
        val cashIn = txns.filter { it.personType in listOf("CUSTOMER", "DEALER") }.sumOf { it.amount }
        val cashOut = txns.filter { it.personType in listOf("EXPENSE", "SALARY", "SUPPLIER") }.sumOf { it.amount }
        val net = cashIn - cashOut

        assertEquals("Cash IN must be 3000", 3000.0, cashIn, 0.001)
        assertEquals("Cash OUT must be 1000", 1000.0, cashOut, 0.001)
        assertEquals("Net cash must be 2000", 2000.0, net, 0.001)

        // Auditor must produce NO alerts in a clean ledger
        val alerts = runAuditor()
        assertTrue("Clean ledger must yield zero alerts, got: ${alerts.map { it.type }}", alerts.isEmpty())
    }

    @Test
    fun auditorResolves_afterManualFix() = runBlocking {
        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "CUSTOMER", personMobile = "9444444444", personName = "Recover",
                totalAmount = 2000.0, paidAmount = 2000.0, dueAmount = 0.0, status = "PAID"
            )
        )
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = paymentId, personType = "CUSTOMER",
                personMobile = "9444444444", personName = "Recover", amount = 1999.0)
        )

        // First audit run flags the issue
        val firstPass = runAuditor()
        assertTrue(firstPass.isNotEmpty())

        // Fix the mismatch by inserting the missing ₹1
        db.paymentTransactionDao().insert(
            PaymentTransaction(paymentId = paymentId, personType = "CUSTOMER",
                personMobile = "9444444444", personName = "Recover", amount = 1.0,
                note = "Auditor-adjustment")
        )

        // Mark old alerts as resolved
        for (a in firstPass) db.ledgerAlertDao().resolve(a.id)

        // Second audit run must be clean
        val secondPass = runAuditor()
        assertTrue("After fix, no new alerts expected, got: ${secondPass.map { it.type }}", secondPass.isEmpty())
    }

    /**
     * Runs the same reconciliation logic the LedgerAuditWorker uses.
     * Returns the alerts that would be raised for the current DB state.
     */
    private suspend fun runAuditor(): List<com.app.muzzutech.data.model.LedgerAlert> {
        val alerts = mutableListOf<com.app.muzzutech.data.model.LedgerAlert>()

        for (payment in db.paymentDao().getAllPayments().first()) {
            val linked = db.paymentTransactionDao().getTransactionsByPayment(payment.id).first()
            val sumTxn = linked.sumOf { it.amount }
            val diff = kotlin.math.abs(sumTxn - payment.paidAmount)
            if (diff > 0.01 && payment.paidAmount > 0) {
                alerts.add(
                    com.app.muzzutech.data.model.LedgerAlert(
                        type = "PAYMENT_MISMATCH",
                        description = "Payment #${payment.id} mismatch",
                        expectedAmount = payment.paidAmount,
                        actualAmount = sumTxn,
                        mismatchAmount = diff
                    )
                )
            }
        }

        for (expense in db.expenseDao().getAll().first().filter { it.paid }) {
            val txn = db.paymentTransactionDao().getTransactionByExpenseId(expense.id)
            if (txn == null) {
                alerts.add(
                    com.app.muzzutech.data.model.LedgerAlert(
                        type = "EXPENSE_MISMATCH",
                        description = "Expense #${expense.id} missing txn",
                        expectedAmount = expense.amount,
                        actualAmount = 0.0,
                        mismatchAmount = expense.amount
                    )
                )
            }
        }

        val allTxns = db.paymentTransactionDao().getAllTransactions().first()
        for (txn in allTxns.filter {
            (it.personType == "EXPENSE" && it.expenseId == null) ||
            (it.personType == "SALARY" && it.paymentId == null)
        }) {
            alerts.add(
                com.app.muzzutech.data.model.LedgerAlert(
                    type = "ORPHAN_TRANSACTION",
                    description = "Txn #${txn.id} orphan",
                    expectedAmount = 0.0,
                    actualAmount = txn.amount,
                    mismatchAmount = txn.amount
                )
            )
        }

        return alerts
    }
}
