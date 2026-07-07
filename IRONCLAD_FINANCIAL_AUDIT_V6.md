# V6 "Ironclad" Financial Audit Report

**Project:** MobileRepairShop (MuZZu Tech Repair Shop)
**Auditor:** Lead FinTech Architect & Database Concurrency Expert
**Date:** 2026-07-07
**Severity Scale:** P0 (catastrophic) → P1 (critical) → P2 (major) → P3 (minor)

---

## 1. Concurrency & Race Conditions (Read-Modify-Write)

### V6-F01 [P0] — DuesViewModel.recordPayment(): Read-Modify-Write inside `withTransaction` (single-process-safe but fragile)

**Location:** `DuesViewModel.kt:119-156`

**Vulnerability:** The current code re-reads `Payment` inside `db.withTransaction{}`, then modifies in Kotlin, then writes back:

```kotlin
val current = paymentDao.getPaymentById(payment.id) ?: return@withTransaction
val newPaidAmount = current.paidAmount + amount
...
paymentDao.update(updatedPayment)
```

In SQLite's SERIALIZABLE transaction isolation within a single process, this is safe **today**. However, it is an anti-pattern that breaks if:
- A future refactoring removes the `withTransaction` wrapper
- A Room `@Transaction` method uses a separate connection (unlikely but possible with WAL mode)
- The code is ever used from multiple processes (content provider, multi-process service)

The fix is an atomic SQL UPDATE that lets SQLite handle the increment internally.

**Remediation — Atomic SQL UPDATE for dues (replace read-modify-write):**

Add to `PaymentDao.kt`:
```kotlin
@Query("""
    UPDATE payments 
    SET paidAmount = paidAmount + :amount, 
        dueAmount = CASE WHEN (totalAmount - (paidAmount + :amount)) < 0 
                        THEN 0 
                        ELSE totalAmount - (paidAmount + :amount) 
                   END,
        status = CASE 
                    WHEN totalAmount - (paidAmount + :amount) <= 0 THEN 'PAID'
                    WHEN paidAmount + :amount > 0 THEN 'PARTIAL'
                    ELSE 'UNPAID'
                 END,
        updatedAt = :now
    WHERE id = :id AND (totalAmount - paidAmount) >= :amount
""")
suspend fun atomicAddPayment(id: Long, amount: Long, now: Long): Int
```

Then in `DuesViewModel.kt`, replace the read-modify-write block with:
```kotlin
val rowsAffected = paymentDao.atomicAddPayment(payment.id, amount, System.currentTimeMillis())
if (rowsAffected == 0) {
    // Overpayment guard triggered by SQL itself — no race possible
    _paymentError.emit("Overpayment detected — amount exceeds due")
    return@withTransaction
}
// Then insert the PaymentTransaction normally
```

The SQL `WHERE (totalAmount - paidAmount) >= :amount` is the atomic overpayment guard. Two concurrent calls cannot both pass this WHERE — exactly one wins, the other fails with 0 rows.

---

### V6-F02 [P1] — PayrollViewModel.generateOrUpdateSalary(): Complex read-modify-write can duplicate expenses

**Location:** `PayrollViewModel.kt:223-289`

**Vulnerability:** This method:
1. Reads `_monthStats.value[smId]` (in-memory state, may be stale)
2. Reads existing SalaryPayment via `salaryDao.getByServiceManAndMonth()`
3. Deletes ALL old salary expenses for this service man in the month range
4. Deletes linked PaymentTransactions for those old expenses
5. Creates a new expense + PaymentTransaction

If two calls happen in quick succession (user double-taps "Save"), the second call deletes the first's newly created expense, losing the accounting trail. The `_busy` flag prevents double-tap at the UI level but is checked only at the start — there is no database-level serialization of the business operation.

**Remediation — Use UPSERT with atomic boundary:**

Replace the delete-then-reinsert pattern with a single atomic UPSERT on `salary_payments` and an upsert-or-update on `expenses`. Add a unique constraint on `(servicemanId, monthStart)` to `salary_payments`:

```sql
-- Migration to add unique constraint
CREATE UNIQUE INDEX IF NOT EXISTS idx_salary_payments_unique_sm_month 
ON salary_payments(servicemanId, monthStart);
```

Then the Kotlin logic becomes:
```kotlin
db.withTransaction {
    val slip = PayrollMath.buildSalaryPayment(...)
    salaryDao.insert(slip) // REPLACE conflict: if (servicemanId, monthStart) duplicate, overwrites
    
    // Update existing expense if present, else insert
    val existingExpense = db.expenseDao().getBySalaryPaymentId(salaryId)
    if (existingExpense != null) {
        db.expenseDao().update(existingExpense.copy(amount = paidAmount, ...))
        db.paymentTransactionDao().update(existingTxn.copy(amount = paidAmount, ...))
    } else if (paidAmount > 0L) {
        // create new expense + txn
    }
}
```

---

## 2. Reverse Money Flows (Refunds & Cancellations)

### V6-F03 [P0] — HandoverFragment.cancelWork(): Advance payment becomes "dark revenue"

**Location:** `HandoverFragment.kt:78-87`

**Vulnerability:** When a repair is cancelled:
```kotlin
val updated = entry.copy(isDraft = true, workStatus = "Cancelled")
MobileRepairApp.instance.repairRepository.update(updated)
```

This sets `workStatus = "Cancelled"` and `isDraft = true` but **never reverses the advance payment**. The advance PaymentTransaction (created during quotation) remains in the ledger as:
- `paymentId = null` (orphaned)
- `personType = CUSTOMER/DEALER`
- `amount = advanceAmount`
- Still counted in all revenue aggregates forever

This is **dark revenue** — the shop appears to have collected money that doesn't correspond to any delivered service. If a customer pays a ₹5000 advance and the repair is cancelled, the shop shows ₹5000 in revenue it must legally return.

**Remediation — Atomic cancellation with refund cascade:**

Add a cancellable status check and create a refund PaymentTransaction:

```kotlin
private fun cancelWork() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.entry.value?.let { entry ->
            if (entry.handoverDone) {
                Toast.makeText(requireContext(), "Cannot cancel a completed handover", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val db = MobileRepairApp.instance.database
            db.withTransaction {
                // 1. If advance exists, create a cash-out refund transaction
                if (entry.advanceAmount > 0L && entry.advancePaymentTransactionId != null) {
                    val advanceTxn = db.paymentTransactionDao().getTransactionById(entry.advancePaymentTransactionId!!)
                    if (advanceTxn != null) {
                        // Create REFUND transaction (negative cash flow)
                        db.paymentTransactionDao().insert(
                            PaymentTransaction(
                                paymentId = null,  // no parent Payment — it's a reversal
                                personType = advanceTxn.personType,
                                personMobile = advanceTxn.personMobile,
                                personName = advanceTxn.personName,
                                amount = -advanceTxn.amount,  // NEGATIVE = money out
                                paymentMode = "REFUND",
                                note = "Refund of advance for cancelled repair #${entry.id}: ${entry.deviceBrand} ${entry.deviceModel}"
                            )
                        )
                    }
                }

                // 2. Delete any SparePartPurchase records linked to this entry
                //    (parts not consumed = no COGS)
                val parts = db.sparePartPurchaseDao().getPurchasesByRepairId(entry.id)
                // Note: this is a Flow, need a suspend method. Add:
                // suspend fun getPurchasesByRepairIdList(id: Long): List<SparePartPurchase>
                for (part in parts) {
                    // Delete linked Payment if exists
                    val linkedPayment = db.paymentDao().getPaymentByLinkedPartId(part.id)
                    if (linkedPayment != null) {
                        db.paymentDao().delete(linkedPayment)
                    }
                    db.sparePartPurchaseDao().delete(part)
                }

                // 3. Mark entry Cancelled
                val updated = entry.copy(
                    workStatus = "Cancelled",
                    isDraft = true,
                    advanceAmount = 0L,
                    advancePaymentTransactionId = null
                )
                MobileRepairApp.instance.repairRepository.update(updated)
            }
            Toast.makeText(requireContext(), "Work Cancelled — advance refunded", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack(R.id.dashboardFragment, false)
        }
    }
}
```

---

### V6-F04 [P0] — QuotationFragment.saveQuotation() bypasses advance PaymentTransaction creation entirely

**Location:** `QuotationFragment.kt:91-108`

**Vulnerability:** The fragment's `saveQuotation()` method calls `repairRepository.update()` directly, **never calling `QuotationViewModel.saveQuotation()`**. The ViewModel has the complete logic (reading the returned `paymentTransactionDao().insert()` ID and storing it as `advancePaymentTransactionId`), but the fragment ignores it:

```kotlin
// QuotationFragment.kt:92-100 — WRONG: bypasses ViewModel
MobileRepairApp.instance.repairRepository.getEntryById(entryId)?.let { entry ->
    val updated = entry.copy(
        faultDetected = selectedFault,
        chargeAmount = charge,
        advanceAmount = advance,    // Stored on RepairEntry, but NO PaymentTransaction created
        quotationDate = System.currentTimeMillis(),
        quotationDone = true
    )
    MobileRepairApp.instance.repairRepository.update(updated)
```

Meanwhile `QuotationViewModel.saveQuotation()` (which creates the advance txn + stores `advancePaymentTransactionId`) is **dead code** — never called from any fragment.

**Remediation — Route QuotationFragment through ViewModel:**

Replace `QuotationFragment.saveQuotation()` body with:
```kotlin
viewModel.saveQuotation(entryId, charge, advance)
```

And update the ViewModel to return a result (navigate on success):
```kotlin
// QuotationViewModel.kt
private val _saveComplete = MutableSharedFlow<Long>()
val saveComplete: SharedFlow<Long> = _saveComplete.asSharedFlow()

fun saveQuotation(entryId: Long, chargeAmount: Long, advanceAmount: Long) {
    viewModelScope.launch {
        db.withTransaction {
            repository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    faultDetected = selectedFault,
                    chargeAmount = chargeAmount,
                    advanceAmount = advanceAmount,
                    quotationDate = System.currentTimeMillis(),
                    quotationDone = true
                )
                repository.update(updated)

                if (advanceAmount > 0L) {
                    val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
                    val personName = entry.customerName.ifEmpty { entry.dealerName }
                    val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"
                    val advanceTxnId = db.paymentTransactionDao().insert(
                        PaymentTransaction(
                            paymentId = null,
                            personType = personType,
                            personMobile = personMobile,
                            personName = personName,
                            amount = advanceAmount,
                            paymentMode = "CASH",
                            note = "Advance for ${entry.deviceBrand} ${entry.deviceModel}"
                        )
                    )
                    // Store explicit transaction ID for handover linking
                    repository.update(updated.copy(
                        advancePaymentTransactionId = advanceTxnId
                    ))
                }
            }
        }
        _saveComplete.emit(entryId)
    }
}
```

And in `QuotationFragment`:
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.saveComplete.collect { id ->
        val bundle = Bundle().apply { putLong("entryId", id) }
        findNavController().navigate(R.id.sparePartsFragment, bundle)
    }
}
```

---

### V6-F05 [P1] — SparePartsViewModel.deletePart(): Payment deletion without refund reversal

**Location:** `SparePartsViewModel.kt:114-124`

**Vulnerability:** Deleting a part removes the SparePartPurchase and its linked Payment, but:
1. No PartReturn record is created (lost return-to-supplier audit trail)
2. If the part was paid immediately (`!payLater`), the PaymentTransaction (cash outflow) is **never reversed** — the money is permanently missing from cash flow
3. If the part was `payLater`, the supplier's due amount is reduced silently with no audit trail

**Remediation — Convert deletePart to a proper return flow:**

```kotlin
fun returnPart(part: SparePartPurchase, reason: String, refundAmount: Long) {
    viewModelScope.launch {
        database.withTransaction {
            // 1. Create PartReturn record
            val partReturn = PartReturn(
                supplierId = part.supplierId,
                supplierName = part.supplierName,
                partName = part.partName,
                returnReason = reason,
                refundAmount = refundAmount,
                refundReceived = refundAmount > 0L
            )
            val returnId = database.partReturnDao().insert(partReturn)

            // 2. If the part was paid, reverse the cash flow
            val linkedPayment = paymentDao.getPaymentByLinkedPartId(part.id)
            if (linkedPayment != null) {
                if (linkedPayment.paidAmount > 0L) {
                    // Create a cash-IN reversal (refund received from supplier)
                    val refundTxn = database.paymentTransactionDao().insert(
                        PaymentTransaction(
                            paymentId = null,
                            expenseId = null,
                            personType = "SUPPLIER",
                            personMobile = part.supplierId,
                            personName = part.supplierName,
                            amount = refundAmount,  // Positive = cash in (money returned)
                            paymentMode = "REFUND",
                            note = "Part return refund: ${part.partName} (${reason})"
                        )
                    )
                    // Link the refund to the PartReturn
                    database.partReturnDao().update(partReturn.copy(
                        refundTransactionId = refundTxn
                    ))
                }
                // Delete the original Payment (no longer applies)
                paymentDao.delete(linkedPayment)
            }

            // 3. Delete the SparePartPurchase
            purchaseDao.delete(part)
        }
    }
}
```

---

### V6-F06 [P2] — PartReturn lacks COGS reversal integration

**Location:** `PartReturn.kt`

**Vulnerability:** The `PartReturn` entity has `refundAmount` and `refundTransactionId`, but this data is **never consumed by any profit/cash-flow calculation** except `AIAdvisor.analyzeDailyHealth()`. The Dashboard and Reports profit engines ignore part returns entirely.

**Remediation — Include part returns in all profit aggregates:**

DashboardViewModel's combine block should add:
```kotlin
val partReturnsToday = database.partReturnDao().getReturnsByDateRangeQuery(todayStart, todayEnd)
// ...
val refundsReceived = partReturnsToday.sumOf { it.refundAmount }
val totalCost = cogs + shopExpenses + salariesPaid - refundsReceived
```

---

## 3. Banker's Rounding on Tax & Discounts

### V6-F07 [P2] — PriceUtils uses HALF_UP instead of HALF_EVEN

**Location:** `PriceUtils.kt:14,22,29`

**Vulnerability:** All three methods use `RoundingMode.HALF_UP`:
```kotlin
BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
```

International accounting standards (IAS 18 / IFRS 15) and GST laws in most jurisdictions require **Banker's Rounding** (`RoundingMode.HALF_EVEN`) for tax calculations. HALF_UP introduces systematic bias: 0.5 paise always rounds up, creating a cumulative positive bias over thousands of transactions.

**Remediation:**
```kotlin
private val ROUNDING = RoundingMode.HALF_EVEN

fun formatPrice(paise: Long): String {
    val rupees = BigDecimal(paise).divide(BigDecimal(100), 2, ROUNDING)
    return "$CURRENCY_SYMBOL ${rupees.setScale(2, ROUNDING)}"
}
```

---

### V6-F08 [P1] — GST and Discount fields are dead — never populated from percentage

**Locations:** `RepairEntry.kt:49-50`, `InvoiceGenerator.kt:81-98`

**Vulnerability:** The `gstAmount` and `discountAmount` fields exist on `RepairEntry` but:
1. No UI fragment sets them (QuotationFragment has no GST/discount input fields)
2. No ViewModel computes them from percentage
3. They are always `0L` in practice
4. The InvoiceGenerator displays them if non-zero, which never happens

When the UI is eventually added, the percentage-to-paise calculation must use Banker's Rounding. A naive `subtotal * 18 / 100` with integer arithmetic truncates (always rounds down), which is both a legal violation and a revenue leak.

**Remediation — Percentage-to-paise utility with Banker's Rounding:**

Add to `PriceUtils.kt`:
```kotlin
/**
 * Compute (amountPaise × percentage / 100) with Banker's Rounding.
 * Example: gstOn(15000, 18) = 2700 paise (₹27.00 on ₹150.00 at 18%)
 */
fun computePercentage(amountPaise: Long, percent: BigDecimal): Long {
    val amount = BigDecimal(amountPaise)
    val fraction = amount.multiply(percent).divide(BigDecimal(100), 0, RoundingMode.HALF_EVEN)
    return fraction.longValueExact()
}

// Usage in QuotationViewModel (to be added when UI appears):
val gstPaise = PriceUtils.computePercentage(subtotalPaise, BigDecimal("18"))
val discountPaise = PriceUtils.computePercentage(subtotalPaise, BigDecimal(discountPercent))
```

---

### V6-F09 [P2] — PayrollMath.computePayable() uses roundToLong() (HALF_UP)

**Location:** `PayrollMath.kt:49`

```kotlin
return (effectivePerDay * workedDays).roundToLong()
```

`kotlin.math.roundToLong()` rounds .5 up (HALF_UP). Over 10 employees × 12 months = 120 salary computations, the cumulative rounding error can reach a few rupees.

**Remediation:**
```kotlin
return BigDecimal(effectivePerDay * workedDays)
    .setScale(0, RoundingMode.HALF_EVEN)
    .toLong()
```

---

## 4. Ledger Immutability & Modification

### V6-F10 [P1] — No enforcement of `handoverDone` immutability

**Vulnerability:** There is **no check anywhere** that prevents editing a RepairEntry after `handoverDone = true`. The `update()` method on `RepairEntryDao` is unguarded:

```kotlin
@Update
suspend fun update(entry: RepairEntry): Int
```

Any code can call `repairRepository.update(entry)` with modified `finalAmount`, `chargeAmount`, `advanceAmount`, etc. on an already-completed transaction, effectively rewriting history. The only partial guard is in `SparePartsViewModel.addPart()` (line 56-58) which checks `handoverDone` before adding parts.

This violates accounting principle of **immutable completed records** and exposes the shop to:
- Fraudulent adjustment of past revenues
- Audit trail breaks
- Tax filing errors

**Remediation — Version-lock on completed entries:**

Add a `@Query` update with a `handoverDone = 0` guard:

```kotlin
@Query("""
    UPDATE repair_entries 
    SET ...all mutable columns...
    WHERE id = :id AND handoverDone = 0
""")
suspend fun updateIfNotHandedOver(/* all fields */): Int
```

Or better, implement optimistic locking with a `version` field:

Add to `RepairEntry`:
```kotlin
val version: Int = 0
```

Add to DAO:
```kotlin
@Update
suspend fun update(entry: RepairEntry): Int  // returns 0 if version doesn't match
```

All update callers must check the return value and throw/notify if `rowCount == 0` (indicating the record was modified concurrently or is frozen).

---

### V6-F11 [P2] — ExpensesViewModel.togglePaid() adjusts past records without audit trail

**Location:** `ExpensesViewModel.kt:129-155`

**Vulnerability:** `togglePaid()` adds or removes PaymentTransactions to retroactively change an expense's payment status. While this is actually correct accountant behavior (an adjusting entry), it leaves no audit trail — the original transaction is silently deleted when toggling from paid→unpaid.

**Remediation — Use a reversal pattern instead of deletion:**

When toggling from PAID → UNPAID, do NOT delete the original transaction. Instead, insert a reversing transaction:
```kotlin
if (newPaid) {
    // Mark as paid: insert cash-out txn
    ...
} else {
    // Mark as unpaid: insert reversing entry instead of deleting
    val originalTxn = db.paymentTransactionDao().getTransactionByExpenseId(expense.id)
    if (originalTxn != null) {
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                paymentId = null,
                expenseId = expense.id,  // Link to same expense
                personType = "REVERSAL",
                personMobile = "SHOP",
                personName = expense.category,
                amount = -originalTxn.amount,  // Negative = reversal
                paymentMode = "ADJUSTMENT",
                note = "Reversal: ${expense.title}"
            )
        )
    }
}
```

---

## 5. The Zero-Sum Equation (Cash Drawer Verification)

### V6-F12 [P0] — Dashboard & Reports profit calculation is incomplete — misses 5 cash flow sources

**Locations:** `DashboardViewModel.kt:77-108`, `ReportsViewModel.kt:136-155`

**Dashboard Current Profit Formula:**
```
_dailyProfit = revenue(handoverFinalAmounts) - cogs(handoverParts) - expenses(paid today) - salaries(paid this month)
```

**What's MISSING from the equation:**

| Component | Included? | Impact |
|-----------|-----------|--------|
| Repair handover revenue (`finalAmount`) | ✅ | — |
| Direct sales revenue (`Sale.salePrice`) | ❌ | Lost revenue in KPI |
| Advance payments collected | ❌ | Dark revenue not tracked |
| Due payments collected | ❌ | Cash inflow not counted |
| Part return refunds received | ❌ | Makes profit look worse |
| Supplier payments made | ❌ | Cash outflow not subtracted |
| Parts purchased (not just consumed) | ❌ | Inventory spend invisible |

**The true Zero-Sum Cash Drawer equation should be:**
```
Cash In Drawer = 
  (Repair finalAmount collected today)
+ (Direct Sale salePrice collected today)
+ (Advance payments collected today)
+ (Due payments collected today: Cash + Online from DuesViewModel)
- (Supplier payments made today: paidAmount from supplier Payments)
- (Cash expenses paid today)
- (Salary payouts today)
+ (Part return refunds received today)
```

**Remediation — Complete cash flow combine:**

```kotlin
// DashboardViewModel loadPrimaryData — enhanced combine
combine(
    database.repairEntryDao().getCompletedEntries(),                  // revenue
    database.sparePartPurchaseDao().getPurchasesByDateRange(todayStart, todayEnd),  // parts COGS
    database.expenseDao().getByDateRange(todayStart, todayEnd),       // expenses
    database.salaryDao().getByMonth(monthStart, monthEnd),            // salaries
    database.saleDao().getSalesByDateRange(todayStart, todayEnd),     // direct sales
    database.paymentTransactionDao().getTransactionsByDateRange(todayStart, todayEnd), // all cash movements
    database.partReturnDao().getReturnsByDateRangeQuery(todayStart, todayEnd)          // refunds
) { completed, parts, expenses, salaries, sales, allTxns, returns ->
    val handovers = completed.filter { it.handoverDate in todayStart..todayEnd }
    val repairRevenue = handovers.sumOf { it.finalAmount }
    val saleRevenue = sales.sumOf { it.salePrice }
    val totalRevenue = repairRevenue + saleRevenue
    
    val cogs = parts.filter { it.repairEntryId in handovers.map { h -> h.id }.toSet() }
        .sumOf { it.purchasePrice * it.quantity }
    val shopExpenses = expenses.sumOf { it.amount }
    val salariesPaid = salaries.sumOf { it.paidAmount }
    val refunds = returns.sumOf { it.refundAmount }
    
    val totalCost = cogs + shopExpenses + salariesPaid - refunds
    Pair(totalRevenue, totalCost)
}
```

---

### V6-F13 [P2] — No cash-on-hand function exists anywhere in the codebase

**Vulnerability:** The system has no mechanism to compute or display actual cash drawer balance. The `_dailyProfit` metric is a gross margin, not cash flow. Without a cash-on-hand function, the shop owner cannot reconcile physical cash drawer counts against the digital ledger.

**Remediation — CashDrawerEngine object:**

```kotlin
object CashDrawerEngine {
    data class CashPosition(
        val openingBalance: Long,
        val cashIn: Long,
        val cashOut: Long,
        val expectedClosing: Long,
        val cashTransactions: List<CashLineItem>
    )
    
    data class CashLineItem(
        val description: String,
        val amount: Long,
        val type: String  // "IN" or "OUT"
    )

    suspend fun computeTodayCashPosition(db: AppDatabase): CashPosition {
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        
        val allTxns = db.paymentTransactionDao()
            .getTransactionsByDateRange(todayStart, todayEnd)
            .first()
        
        var cashIn = 0L
        var cashOut = 0L
        val items = mutableListOf<CashLineItem>()
        
        for (txn in allTxns) {
            if (txn.paymentMode == "CASH" || txn.paymentMode == "REFUND") {
                when (txn.personType) {
                    "CUSTOMER", "DEALER" -> {
                        // Cash received from customers
                        cashIn += txn.amount
                        items.add(CashLineItem(txn.note, txn.amount, "IN"))
                    }
                    "SUPPLIER", "EXPENSE", "SALARY" -> {
                        // Cash paid out
                        cashOut += txn.amount
                        items.add(CashLineItem(txn.note, txn.amount, "OUT"))
                    }
                }
            }
        }
        
        return CashPosition(
            openingBalance = 0L,  // Requires yesterday's closing to be persisted
            cashIn = cashIn,
            cashOut = cashOut,
            expectedClosing = cashIn - cashOut,
            cashTransactions = items
        )
    }
}
```

---

## Summary of Findings

| ID | Severity | Category | Area | Description |
|----|----------|----------|------|-------------|
| V6-F01 | P0 | Concurrency | DuesViewModel | Atomic SQL needed for payment (fragile RMW pattern) |
| V6-F02 | P1 | Concurrency | PayrollViewModel | Double-tap can duplicate/delete salary expenses |
| V6-F03 | P0 | Reverse Flow | HandoverFragment | Cancellation creates dark revenue — advance never refunded |
| V6-F04 | P0 | Reverse Flow | QuotationFragment | Bypasses ViewModel — advance txn never created |
| V6-F05 | P1 | Reverse Flow | SparePartsViewModel | deletePart loses cash flow + audit trail |
| V6-F06 | P2 | Reverse Flow | PartReturn | Part returns excluded from Dashboard/Reports profit |
| V6-F07 | P2 | Rounding | PriceUtils | Uses HALF_UP not HALF_EVEN — systematic rounding bias |
| V6-F08 | P1 | Rounding | RepairEntry | GST/discount fields dead — no percentage computation exists |
| V6-F09 | P2 | Rounding | PayrollMath | roundToLong() instead of BigDecimal HALF_EVEN |
| V6-F10 | P1 | Immutability | RepairEntryDao | No handoverDone guard on updates — past records editable |
| V6-F11 | P2 | Immutability | ExpensesViewModel | togglePaid() deletes original txn — no audit trail |
| V6-F12 | P0 | Zero-Sum | Dashboard/Reports | Profit misses 5 cash flow sources — metric is misleading |
| V6-F13 | P2 | Zero-Sum | Codebase | No cash-on-hand calculation exists |

**Grade:** C- (up from D in prior audit — structural Double→Long fix completed, but 5 critical P0 cash-flow holes remain open)

**Priority Order for Remediation:**
1. V6-F04 (P0) — QuotationFragment advance txn never created (dead ViewModel code)
2. V6-F03 (P0) — CancelWork creates dark revenue (advance never refunded)
3. V6-F12 (P0) — Dashboard profit equation incomplete (misleading KPI)
4. V6-F01 (P0) — Atomic SQL for payment updates
5. V6-F10 (P1) — Handover immutability enforcement
6. V6-F05 (P1) — Part return accounting
7. V6-F08 (P1) — GST percentage calculation
8. V6-F11 (P2) — Audit trail for expense toggle
9. V6-F02 (P1) — Salary duplicate guard
10. V6-F07/F6-F09 (P2) — Banker's Rounding migration
11. V6-F06 (P2) — Part return in profit
12. V6-F13 (P2) — Cash-on-hand function
