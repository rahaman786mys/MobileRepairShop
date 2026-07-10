# Unified Financial Ledger Implementation Plan

This plan outlines the steps to fix the broken math and data synchronization issues by establishing a single source of truth for all financial transactions.

## Proposed Changes

### 1. Database Layer (The Source of Truth)

#### [PaymentTransaction.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/data/model/PaymentTransaction.kt)
- Add `transactionType` (REVENUE, EXPENSE, REFUND, ADJUSTMENT) to clearly categorize the flow.
- Add `linkedId` (nullable) to point back to the source entity (RepairEntry, Sale, SparePartPurchase, etc.) for traceability.
- Ensure all entries use positive for inflow and negative for outflow (or separate columns, but signed amount is standard).

#### [PaymentDao.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/data/db/dao/PaymentDao.kt)
- Add a trigger or a managed function to recalculate `paidAmount` and `dueAmount` by summing the Ledger entries for a given `paymentId`. This ensures summaries never drift.

---

### 2. Business Logic Layer (The Linking Logic)

#### [HandoverViewModel.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/handover/HandoverViewModel.kt)
- Wrap the entire completion logic in a `@Transaction`.
- Instead of manual field updates, use a unified `AccountingService` (new) to record the transaction.

#### [ExpensesViewModel.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/expenses/ExpensesViewModel.kt)
- Ensure every expense record automatically inserts a corresponding `PaymentTransaction` of type `EXPENSE`.

#### [SaleViewModel.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/sales/SaleViewModel.kt)
- "Pay Later" logic: Insert into `Payment` (Obligation) but do NOT insert into `PaymentTransaction` (Cash Flow) until money is received.

---

### 3. UI/Reporting Layer (Live Calculation)

#### [DashboardViewModel.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/dashboard/DashboardViewModel.kt)
- Re-write the metrics fetch logic to query the `PaymentTransaction` table directly for Profit/Revenue using SQL `SUM` and `GROUP BY`.

---

## Verification Plan (The Test Matrix)

I will create a comprehensive integration test suite `FinancialIntegrityTest.kt` that will perform the following actions and assert the state of the Ledger and Summaries.

| Scenario | Action | Expected Outcome |
| :--- | :--- | :--- |
| **Cash Sale** | Register repair, full cash handover | Ledger: +Amt. Summary: Paid. Dashboard: Profit up. |
| **Pay Later** | Register repair, handover "Pay Later" | Ledger: 0. Summary: Due=Total. Dashboard: Dues up. |
| **Partial Pay** | Record payment against Pay Later | Ledger: +Partial. Summary: Due=Remaining. Dashboard: Profit up. |
| **Expense** | Add "Rent" expense | Ledger: -Amt. Dashboard: Profit down. |
| **Edit Amount**| Change repair charge after handover | Summary: Due/Paid recalculated. Ledger: Unchanged. |
| **Delete** | Delete a repair with payments | Ledger entries reversed/deleted. Dues cleared. |

### Execution Command
`./gradlew testDebugUnitTest --tests "com.app.muzzutech.FinancialIntegrityTest"`

## User Review Required
- **Accounting Basis**: Confirming we are using **Cash Basis** for Profit/Revenue cards and **Accrual Basis** for the Dues list.
- **Negative Balances**: If a customer overpays, do you want it to show as a "Negative Due" (Credit) or a separate "Advance" pool?
