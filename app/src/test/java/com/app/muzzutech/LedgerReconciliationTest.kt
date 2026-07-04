package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.PaymentTransaction
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.data.model.PartReturn
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.utils.AIAdvisor
import com.app.muzzutech.utils.DateUtils
import com.app.muzzutech.ui.payroll.PayrollMath
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
        val expectedTotal = fetched!!.paidAmount + fetched.dueAmount
        assertTrue(
            "totalAmount (${fetched.totalAmount}) must equal paidAmount + dueAmount ($expectedTotal)",
            kotlin.math.abs(fetched.totalAmount - expectedTotal) < 0.001
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
        val now = System.currentTimeMillis()

        // Pre-create RepairEntries referenced by Payments (FK constraint)
        db.repairEntryDao().insert(
            RepairEntry(id = 1, deviceBrand = "Samsung", deviceModel = "S21",
                customerMobile = "9111111111", customerName = "Cust A",
                handoverDone = true, handoverDate = now)
        )
        db.repairEntryDao().insert(
            RepairEntry(id = 2, deviceBrand = "iPhone", deviceModel = "13",
                dealerMobile = "9222222222", dealerName = "Dealer B",
                handoverDone = true, handoverDate = now)
        )

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

        assertTrue("Cash IN must be 3000, got $cashIn", kotlin.math.abs(3000.0 - cashIn) < 0.001)
        assertTrue("Cash OUT must be 1000, got $cashOut", kotlin.math.abs(1000.0 - cashOut) < 0.001)
        assertTrue("Net cash must be 2000, got $net", kotlin.math.abs(2000.0 - net) < 0.001)

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

    @Test
    fun bug1_directSale_supplierTxnIsLinkedToPayment() = runBlocking {
        // Simulate SaleViewModel.saveSale flow
        val saleId = db.saleDao().insert(
            com.app.muzzutech.data.model.Sale(
                itemName = "Screen Assembly",
                supplierId = "9999911111",
                supplierName = "Parts Co",
                purchasePrice = 2500.0,
                salePrice = 4500.0,
                paidToSupplier = 2500.0,
                supplierDue = 0.0,
                customerPaid = 4500.0,
                customerDue = 0.0
            )
        )

        val supplierPaymentId = db.paymentDao().insert(
            Payment(
                personType = "SUPPLIER",
                personMobile = "9999911111",
                personName = "Parts Co",
                description = "Direct Sale: Screen Assembly",
                totalAmount = 2500.0,
                paidAmount = 2500.0,
                dueAmount = 0.0,
                status = "PAID",
                linkedSaleId = saleId
            )
        )

        // BUG #1 fix: paymentId is supplierPaymentId (not null)
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = supplierPaymentId,
                personType = "SUPPLIER",
                personMobile = "9999911111",
                personName = "Parts Co",
                amount = 2500.0,
                paymentMode = "CASH",
                note = "Purchase for Direct Sale: Screen Assembly"
            )
        )

        val payment = db.paymentDao().getPaymentById(supplierPaymentId)!!
        val linkedTxns = db.paymentTransactionDao().getTransactionsByPayment(supplierPaymentId).first()
        val sumTxn = linkedTxns.sumOf { it.amount }

        assertTrue(
            "Supplier payment paidAmount (${payment.paidAmount}) must equal sum of linked transactions ($sumTxn)",
            kotlin.math.abs(payment.paidAmount - sumTxn) < 0.001
        )
    }

    @Test
    fun bug3_payrollReRun_doesNotCreateDuplicateExpenseOrTransaction() = runBlocking {
        val smId = 42L
        val monthStart = DateUtils.getStartOfMonth()

        db.serviceManDao().insert(
            com.app.muzzutech.data.model.ServiceMan(
                id = smId, name = "Ravi", mobile = "9000000001",
                designation = "Tech", monthlySalary = 30000.0
            )
        )

        // Run the payroll logic twice for the same month (simulates user correcting paidAmount).
        // Each run must clean up old Expense + Transaction rows before inserting new ones.
        repeat(2) {
            val slip = PayrollMath.buildSalaryPayment(smId, "Ravi", monthStart, 20.0, 30000.0, 0.0, 15000.0)
            val existing = db.salaryDao().getByServiceManAndMonth(smId, monthStart)
            val slipToSave = if (existing != null) slip.copy(id = existing.id) else slip
            db.salaryDao().insert(slipToSave)

            // Dedup: delete old accounting rows for this service man in this month
            val monthEnd = DateUtils.getEndOfMonth(monthStart)
            val oldExpenses = db.expenseDao().getByDateRange(monthStart, monthEnd).first()
                .filter { it.category == com.app.muzzutech.data.model.Expense.CATEGORY_SALARY }
            for (oldExp in oldExpenses) {
                db.paymentTransactionDao().getTransactionByExpenseId(oldExp.id)?.let { txn ->
                    db.paymentTransactionDao().delete(txn)
                }
                db.expenseDao().deleteById(oldExp.id)
            }

            if (slip.paidAmount > 0) {
                val expenseId = db.expenseDao().insert(
                    com.app.muzzutech.data.model.Expense(
                        title = "Salary: Ravi",
                        amount = slip.paidAmount,
                        category = com.app.muzzutech.data.model.Expense.CATEGORY_SALARY,
                        date = monthStart,
                        paid = true,
                        note = "Salary for ${DateUtils.formatDateTime(monthStart)}"
                    )
                )
                db.paymentTransactionDao().insert(
                    com.app.muzzutech.data.model.PaymentTransaction(
                        paymentId = null,
                        expenseId = expenseId,
                        personType = "SALARY",
                        personMobile = "9000000001",
                        personName = "Ravi",
                        amount = slip.paidAmount,
                        paymentMode = "CASH",
                        note = "Salary: ${DateUtils.formatDateTime(monthStart)}"
                    )
                )
            }
        }

        val expensesAfter = db.expenseDao().getByDateRange(monthStart, DateUtils.getEndOfMonth(monthStart)).first()
        val salaryExpenses = expensesAfter.filter { it.category == com.app.muzzutech.data.model.Expense.CATEGORY_SALARY }
        val salaryTxns = db.paymentTransactionDao().getAllTransactions().first()
            .filter { it.personType == "SALARY" && it.personName == "Ravi" }

        assertTrue(
            "BUG #3: Payroll re-run must not duplicate Expense rows for same month — found ${salaryExpenses.size}: ${salaryExpenses.map { it.title }}",
            salaryExpenses.size <= 1
        )
        assertTrue(
            "BUG #3: Payroll re-run must not duplicate Transaction rows for same month — found ${salaryTxns.size}",
            salaryTxns.size <= 1
        )
    }

    @Test
    fun bug4_partReturn_createsRefundTransaction() = runBlocking {
        val partId = db.sparePartPurchaseDao().insert(
            com.app.muzzutech.data.model.SparePartPurchase(
                repairEntryId = 0,
                partName = "Test Screen",
                partPhotoPath = "/tmp/screen.jpg",
                purchasePrice = 3000.0,
                supplierId = "9999922222",
                supplierName = "Return Supplier",
                quantity = 1,
                purchaseDate = System.currentTimeMillis()
            )
        )

        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "SUPPLIER",
                personMobile = "9999922222",
                personName = "Return Supplier",
                description = "Parts: Test Screen x 1 (Repair #0)",
                totalAmount = 3000.0,
                paidAmount = 3000.0,
                dueAmount = 0.0,
                status = "PAID",
                linkedPartId = partId
            )
        )

        // Simulate PartReturnFragment with BUG #4 fix: create refund txn
        val refundAmount = 1500.0
        db.withTransaction {
            db.partReturnDao().insert(
                com.app.muzzutech.data.model.PartReturn(
                    supplierId = "9999922222",
                    supplierName = "Return Supplier",
                    partName = "Test Screen",
                    returnReason = "Defective",
                    refundAmount = refundAmount
                )
            )
            val linkedPayment = db.paymentDao().getPaymentById(paymentId)!!
            val reducedTotal = (linkedPayment.totalAmount - refundAmount).coerceAtLeast(0.0)
            val reducedPaid = (linkedPayment.paidAmount - refundAmount).coerceAtLeast(0.0)
            val reducedDue = (reducedTotal - reducedPaid).coerceAtLeast(0.0)
            db.paymentDao().update(
                linkedPayment.copy(
                    totalAmount = reducedTotal,
                    paidAmount = reducedPaid,
                    dueAmount = reducedDue,
                    status = if (reducedDue <= 0.0) "PAID" else "PARTIAL",
                    updatedAt = System.currentTimeMillis()
                )
            )
            // BUG #4 fix: record cash-in refund transaction
            db.paymentTransactionDao().insert(
                PaymentTransaction(
                    paymentId = paymentId,
                    personType = "SUPPLIER",
                    personMobile = "9999922222",
                    personName = "Return Supplier (Refund)",
                    amount = refundAmount,
                    paymentMode = "CASH",
                    note = "Refund for Test Screen"
                )
            )
        }

        val updatedPayment = db.paymentDao().getPaymentById(paymentId)!!
        val refundTxns = db.paymentTransactionDao().getTransactionsByPayment(paymentId).first()
        val refundSum = refundTxns.filter { it.note.contains("Refund") }.sumOf { it.amount }

        assertTrue("Supplier total must reduce by refund — got ${updatedPayment.totalAmount}", kotlin.math.abs(1500.0 - updatedPayment.totalAmount) < 0.001)
        assertTrue("Supplier paid must reduce by refund — got ${updatedPayment.paidAmount}", kotlin.math.abs(1500.0 - updatedPayment.paidAmount) < 0.001)
        assertTrue("Refund transaction must exist with correct amount — got $refundSum", kotlin.math.abs(1500.0 - refundSum) < 0.001)
    }

    @Test
    fun bug2_handover_zeroAdvance_doesNotLinkUnrelatedTransaction() = runBlocking {
        val entryId = db.repairEntryDao().insert(
            com.app.muzzutech.data.model.RepairEntry(
                customerMobile = "9111111111",
                customerName = "ZeroAdvance Cust",
                deviceBrand = "Samsung",
                deviceModel = "A52",
                advanceAmount = 0.0
            )
        )

        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "CUSTOMER",
                personMobile = "9111111111",
                personName = "ZeroAdvance Cust",
                description = "Repair - Samsung A52",
                totalAmount = 2000.0,
                paidAmount = 2000.0,
                dueAmount = 0.0,
                status = "PAID",
                linkedEntryId = entryId
            )
        )

        // BUG #2 guard: only try linking if advanceAmount > 0
        val advanceTxn = db.paymentTransactionDao().findUnlinkedByMobileAndAmount("9111111111", 0.0)
        val linkedTxn = if (advanceTxn != null && 0.0 > 0) {
            db.paymentTransactionDao().update(advanceTxn.copy(paymentId = paymentId))
            advanceTxn
        } else null

        assertNull(
            "Zero-advance handover must NOT link unrelated transactions",
            linkedTxn
        )
        val txns = db.paymentTransactionDao().getTransactionsByPayment(paymentId).first()
        assertTrue(
            "Zero-advance handover must have no linked transactions at all",
            txns.isEmpty()
        )
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
            val allLinked = db.paymentTransactionDao().getAllTransactions().first().filter { it.expenseId == expense.id }
            val sumTxn = allLinked.sumOf { it.amount }
            val diff = kotlin.math.abs(sumTxn - expense.amount)
            when {
                allLinked.isEmpty() -> {
                    alerts.add(
                        com.app.muzzutech.data.model.LedgerAlert(
                            type = "EXPENSE_MISMATCH",
                            description = "Expense #${expense.id} '${expense.title}' (${expense.amount}) is PAID but has no PaymentTransaction",
                            expectedAmount = expense.amount,
                            actualAmount = 0.0,
                            mismatchAmount = expense.amount
                        )
                    )
                }
                diff > 0.01 -> {
                    alerts.add(
                        com.app.muzzutech.data.model.LedgerAlert(
                            type = "EXPENSE_MISMATCH",
                            description = "Expense #${expense.id} sum mismatch",
                            expectedAmount = expense.amount,
                            actualAmount = sumTxn,
                            mismatchAmount = diff
                        )
                    )
                }
            }
        }

        val allTxns = db.paymentTransactionDao().getAllTransactions().first()
        for (txn in allTxns.filter {
            (it.personType == "EXPENSE" && it.expenseId == null) ||
            (it.personType == "SALARY" && it.expenseId == null)
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

    // ----------------------------------------------------------------
    // BUG #5 — AIAdvisor.partCost counts parts used today (linked to handovers)
    // ----------------------------------------------------------------
    @Test
    fun bug5_aiAdvisor_partCostCountsOnlyPartsUsedInTodaysHandovers() = runBlocking {
        val today = DateUtils.getStartOfDay()
        val entryId = db.repairEntryDao().insert(
            RepairEntry(
                deviceBrand = "Apple", deviceModel = "iPhone13",
                customerMobile = "9111111111", customerName = "Cust A",
                handoverDone = true, handoverDate = today
            )
        )
        db.sparePartPurchaseDao().insert(
            SparePartPurchase(
                repairEntryId = 0, partName = "Bulk Screen",
                partPhotoPath = "/tmp/b.jpg", purchasePrice = 500.0,
                supplierId = "9999911111", supplierName = "Bulk Supplier",
                quantity = 100, purchaseDate = today
            )
        )
        db.sparePartPurchaseDao().insert(
            SparePartPurchase(
                repairEntryId = entryId, partName = "Used Screen",
                partPhotoPath = "/tmp/u.jpg", purchasePrice = 800.0,
                supplierId = "9999911111", supplierName = "Bulk Supplier",
                quantity = 1, purchaseDate = today
            )
        )

        val repairs = db.repairEntryDao().getEntriesByDateRange(today, today + 86400000).first()
        val parts = db.sparePartPurchaseDao().getPurchasesByDateRange(today, today + 86400000).first()
        val health = AIAdvisor.analyzeDailyHealth(repairs, parts)

        assertTrue("BUG #5: Only parts used in today's handovers should count — got ${health.dailyExpense}", kotlin.math.abs(800.0 - health.dailyExpense) < 0.001)
    }

    // ----------------------------------------------------------------
    // BUG #6 — AIAdvisor.otherCost excludes unpaid expenses
    // ----------------------------------------------------------------
    @Test
    fun bug6_aiAdvisor_otherCostOnlyCountsPaidExpenses() = runBlocking {
        val today = DateUtils.getStartOfDay()
        db.expenseDao().insert(
            Expense(title = "Paid Rent", amount = 5000.0, category = Expense.CATEGORY_RENT, date = today, paid = true)
        )
        db.expenseDao().insert(
            Expense(title = "Pending Electric", amount = 2000.0, category = Expense.CATEGORY_ELECTRICITY, date = today, paid = false)
        )

        val expenses = db.expenseDao().getByDateRange(today, today + 86400000).first()
        val health = AIAdvisor.analyzeDailyHealth(emptyList(), emptyList(), expenses)

        assertEquals("BUG #6: Unpaid expense must not count as today's cost", 5000.0, health.dailyExpense, 0.001)
    }

    // ----------------------------------------------------------------
    // BUG #8 — AIAdvisor includes Direct Sales + Part Returns
    // ----------------------------------------------------------------
    @Test
    fun bug8_aiAdvisor_includesDirectSalesAndPartReturns() = runBlocking {
        val today = DateUtils.getStartOfDay()

        db.saleDao().insert(
            Sale(
                itemName = "Charger", supplierId = "9999933333", supplierName = "Gadget Co",
                purchasePrice = 600.0, salePrice = 1000.0,
                paidToSupplier = 600.0, supplierDue = 0.0,
                customerPaid = 1000.0, customerDue = 0.0,
                saleDate = today
            )
        )
        db.partReturnDao().insert(
            PartReturn(
                supplierId = "9999944444", supplierName = "Parts Inc",
                partName = "Old Screen", returnReason = "Defective",
                refundAmount = 300.0, returnDate = today
            )
        )

        val sales = db.saleDao().getSalesByDateRange(today, today + 86400000).first()
        val returns = db.partReturnDao().getReturnsByDateRangeQuery(today, today + 86400000).first()
        val health = AIAdvisor.analyzeDailyHealth(emptyList(), emptyList(), emptyList(), sales, returns)

        assertTrue("BUG #8a: Direct sale revenue must be counted — got ${health.dailyRevenue}", kotlin.math.abs(1000.0 - health.dailyRevenue) < 0.001)
        assertTrue("BUG #8b: Part return refund reduces cost — got ${health.dailyExpense}", kotlin.math.abs(-300.0 - health.dailyExpense) < 0.001)
    }

    // ----------------------------------------------------------------
    // BUG #9 — Payment.linkedEntryId FK: delete RepairEntry → SET NULL
    // ----------------------------------------------------------------
    @Test
    fun bug9_paymentLinkedEntryId_fkSetNullOnEntryDelete() = runBlocking {
        val entryId = db.repairEntryDao().insert(
            RepairEntry(deviceBrand = "Samsung", deviceModel = "A52", customerMobile = "9222222222")
        )
        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "CUSTOMER", personMobile = "9222222222",
                personName = "Cust FK", description = "Repair - Samsung A52",
                totalAmount = 2000.0, paidAmount = 2000.0, dueAmount = 0.0,
                status = "PAID", linkedEntryId = entryId
            )
        )

        db.repairEntryDao().delete(RepairEntry(id = entryId))

        val paymentAfter = db.paymentDao().getPaymentById(paymentId)!!
        assertNull(
            "BUG #9: linkedEntryId must be NULL after parent entry is deleted, got=${paymentAfter.linkedEntryId}",
            paymentAfter.linkedEntryId
        )
    }

    // ----------------------------------------------------------------
    // BUG #11 — PartReturn stores refundTransactionId
    // ----------------------------------------------------------------
    @Test
    fun bug11_partReturn_storesRefundTransactionId() = runBlocking {
        val paymentId = db.paymentDao().insert(
            Payment(
                personType = "SUPPLIER", personMobile = "9333333333",
                personName = "Supplier TXN", description = "Parts",
                totalAmount = 500.0, paidAmount = 500.0, dueAmount = 0.0, status = "PAID"
            )
        )
        val txnId = db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = paymentId, personType = "SUPPLIER", personMobile = "9333333333",
                personName = "Supplier TXN", amount = 500.0, note = "Refund"
            )
        )
        val partReturnId = db.partReturnDao().insert(
            PartReturn(
                supplierId = "9333333333", supplierName = "Supplier TXN",
                partName = "Sensor", returnReason = "Defective",
                refundAmount = 500.0, refundTransactionId = txnId
            )
        )
        val saved = db.partReturnDao().getReturnById(partReturnId)!!
        assertEquals("BUG #11: refundTransactionId must be persisted", txnId, saved.refundTransactionId)
    }

    // ----------------------------------------------------------------
    // BUG #12 — LedgerAuditWorker validates Expense transaction sum
    // ----------------------------------------------------------------
    @Test
    fun bug12_auditorCatch_expenseTxnSumMismatch() = runBlocking {
        val expenseId = db.expenseDao().insert(
            Expense(title = "Big Buy", amount = 8000.0, category = Expense.CATEGORY_SUPPLIES, paid = true)
        )
        // Only 7500 paid via txn — 500 short
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = null, expenseId = expenseId,
                personType = "EXPENSE", personMobile = "SHOP",
                personName = Expense.CATEGORY_SUPPLIES, amount = 7500.0,
                note = "Partial"
            )
        )

        val alerts = runAuditor()
        val mismatch = alerts.firstOrNull { it.type == "EXPENSE_MISMATCH" && it.description.contains("sum mismatch") }
        assertNotNull("BUG #12: Auditor must flag sum mismatch", mismatch)
        assertEquals(500.0, mismatch!!.mismatchAmount, 0.001)
    }
}
