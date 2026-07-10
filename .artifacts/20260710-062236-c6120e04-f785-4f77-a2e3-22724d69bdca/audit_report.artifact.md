# Financial System Audit Report

I have completed the audit of the app's current financial architecture. Here is the breakdown of the existing entities and calculation logic.

## 1. Entity & Table Audit

The app currently uses **15 tables**. Below are the key fields related to financial tracking:

| Table | Key Financial Fields | Purpose |
| :--- | :--- | :--- |
| `repair_entries` | `chargeAmount`, `advanceAmount`, `finalAmount`, `cashAmount`, `onlineAmount` | Tracks lifecycle of a repair and captured revenue. |
| `customers` | `mobile` (PK) | Links customer to their ledger via mobile number. |
| `dealers` | `mobile` (PK) | Links dealer to their ledger via mobile number. |
| `suppliers` | `mobile` (PK) | Links supplier to their ledger via mobile number. |
| `spare_part_purchases` | `purchasePrice`, `quantity`, `supplierId` | Tracks inventory costs and supplier obligations. |
| `sales` | `purchasePrice`, `salePrice`, `paidToSupplier`, `supplierDue`, `customerPaid`, `customerDue` | Tracks direct sales and associated dues. |
| `payments` | `totalAmount`, `paidAmount`, `dueAmount`, `linkedEntryId`, `linkedSaleId`, `linkedPartId` | **The Summary Ledger**: Records the state of a specific obligation. |
| `payment_transactions` | `amount`, `paymentMode`, `paymentId`, `expenseId`, `salaryPaymentId` | **The Transaction Log**: Records individual cash/online events. |
| `expenses` | `amount`, `paid` (boolean) | Tracks operational costs (Rent, Internet, etc.). |
| `salary_payments` | `computedAmount`, `paidAmount`, `dueAmount` | Tracks technician payroll obligations and payouts. |
| `part_returns` | `refundAmount`, `refundTransactionId` | Tracks money recovered from faulty inventory. |

---

## 2. Independent Calculation Points (Risk Areas)

Currently, financial "truth" is calculated in multiple independent places, which is the root cause of the "out of sync" behavior you've noticed:

1.  **Dashboard (`DashboardViewModel.kt`)**:
    *   Re-calculates daily profit and revenue by filtering the `payment_transactions` table.
    *   Calculates Total Dues by summing the `dueAmount` column in the `payments` table.
    *   *Risk:* If a transaction is added but the `payments` summary isn't updated, the Dashboard Dues and individual ledgers will drift.

2.  **Handover Flow (`HandoverViewModel.kt`)**:
    *   Updates the `RepairEntry` record.
    *   Separately updates/inserts a `Payment` summary record.
    *   Separately inserts one or more `PaymentTransaction` records.
    *   *Risk:* Logic assumes these three operations succeed perfectly. If one is edited manually later, the others don't "know."

3.  **Supplier Detail (`SupplierDetailFragment.kt`)**:
    *   Sums all `Payment` records linked to that supplier's mobile.
    *   *Risk:* If a `PartReturn` happens, it currently tries to manually subtract from a `Payment` record instead of the ledger recalculating the balance.

4.  **Dues Overview (`DuesViewModel.kt`)**:
    *   Reads directly from the `payments` summary table.
    *   *Risk:* Doesn't verify if the `paidAmount` in the summary actually matches the sum of individual transactions for that person.

---

## 3. Ambiguity & Assumptions (Step 5)

Before I move to the design phase, I have identified these ambiguities:

1.  **Revenue Recognition**: Currently, the Dashboard recognizes revenue on a **Cash Basis** (only when money physically changes hands).
    *   *My Assumption for Fix:* I will maintain Cash Basis for the "Profit" and "Revenue" cards on the Dashboard to reflect your actual cash flow, but the "Dues" section will accurately reflect **Accrual** obligations (money you are owed but haven't received).
2.  **Dealer vs. Supplier Netting**: Can a person be both a Supplier and a Dealer?
    *   *My Assumption:* Currently, the code treats them as separate mobile-linked entities. I will ensure these ledgers stay completely separate unless you explicitly request a "netting" transaction.
3.  **"Pay Later" Sale**: When a "Pay Later" sale is made, should it show up in today's Revenue?
    *   *Standard Accounting:* It shows in "Sales" but not in "Cash Flow." I will treat it as an active Receivable (Due) that only enters Revenue when the payment is actually made.

---

## Next Steps

**I am ready to move to Step 2: Designing the Single Source of Truth.** This will involve:
*   Refactoring the `payment_transactions` table to act as the master Ledger.
*   Implementing a **Live-Calculation View** for all balances.

**Do I have your approval on this audit before I proceed with the code changes?**
