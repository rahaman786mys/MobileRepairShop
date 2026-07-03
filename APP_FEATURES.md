# MobileRepairShop — Complete Feature Inventory

**Last updated:** 2026-07-04 | **Total features:** 40 | **Total screens:** 28 | **Database entities:** 15 | **DAOs:** 15 | **Background jobs:** 2

---

## COUNT SUMMARY

| Category | Count |
|---|---|
| User-facing screens | 28 |
| User-facing features | 40 |
| Database entities (tables) | 15 |
| DAO classes (with methods) | 15 |
| Background/automated jobs | 2 |
| Activities | 3 |
| Fragments | 28 |
| ViewModels | 15 |
| TestLauncher destinations | 13 |

---

## 1. AUTHENTICATION

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 1 | Splash + Biometric lock | SplashActivity | **FULLY WORKING** | Checks `app_settings` prefs; if biometric is enabled shows BiometricPrompt; on success launches app after 300ms |
| 2 | Biometric toggle | ProfileFragment | **FULLY WORKING** | Reads/writes `biometric_enabled` in `app_settings` shared prefs |
| 3 | WhatsApp OTP login | LoginFragment | **PARTIALLY WORKING** | Enters 10-digit mobile → opens WhatsApp via Intent with prefilled OTP message; OTP validation via `WhatsAppOtpUtil.validateOtp()` |
| 4 | Google Sign-In | LoginFragment, ProfileFragment | **PARTIALLY WORKING** | GoogleSignInClient for login onboarding and profile linking; currently login screen is bypassed in normal flow |
| 5 | Auth state persistence | MoreFragment, MainActivity | **FULLY WORKING** | `auth_prefs` stores `is_logged_in`; logout clears it; auto-set on launch |

---

## 2. DASHBOARD (Home)

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 6 | Stat cards — Pending/Completed/Profit/Investment/Dues | DashboardFragment | **FULLY WORKING** | 6 stat cards driven by parallel Flow queries in DashboardViewModel |
| 7 | Quick-action navigation cards | DashboardFragment | **FULLY WORKING** | 6 cards → Entry, Entries, Quick Sale, Dues, Suppliers, Reports, More |
| 8 | Missing profile prompt | DashboardFragment | **FULLY WORKING** | Shows "Update" button (`btnFixMissingInfo`) when shop phone is empty |
| 9 | AI Business Health Advisor | DashboardFragment | **FULLY WORKING** | `AIAdvisor.analyzeDailyHealth()` — shows score (0–100), smart move, revenue/margin stats, recommendation |
| 10 | Investment breakdown dialog | DashboardFragment | **FULLY WORKING** | `cardInvest` tap → AlertDialog with paid/due/total investment for today |
| 11 | Search entries | DashboardFragment → EntriesListFragment | **FULLY WORKING** | `ivSearch` navigates to all-entries search screen |

---

## 3. REPAIR WORKFLOW — 5-Step Pipeline

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 12 | New repair entry (Step 1) | EntryFragment | **FULLY WORKING** | Customer/Dealer toggle, 10-digit mobile (auto-lookup existing), 2 inspection photos (camera + FileProvider), brand spinner (17 brands), model name, extra-item multi-select, service-man spinner |
| 13 | Auto-save draft | EntryFragment | **FULLY WORKING** | On `onPause()`: if mobile ≥ 4 chars, saves draft; deduplication via `draftEntryIds` map |
| 14 | Mobile auto-lookup | EntryFragment | **FULLY WORKING** | On 10-digit entry → queries CustomerDao/DealerDao → pre-fills name + city |
| 15 | Inspection + fault detection (Step 2) | InspectionFragment | **FULLY WORKING** | Inspection photo (camera), CommonFaults RecyclerView (tap to pre-fill), save → updates entry + sends WhatsApp notification |
| 16 | AI photo fault analysis | InspectionFragment | **FULLY WORKING** | `AIAnalyzer.suggestFaultsFromPhoto()` — ML Kit Object Detection + Image Labeling + pixel-brightness heuristic |
| 17 | Quotation (Step 3) | QuotationFragment | **FULLY WORKING** | Charge amount + advance amount fields; save → updates entry + sends "Repair Started" WhatsApp |
| 18 | Spare parts selection (Step 4) | SparePartsFragment | **FULLY WORKING** | Part photo (camera), name, purchase price, quantity, supplier spinner; add-to-transaction engine; quick-add supplier |
| 19 | Part purchase accounting | SparePartsViewModel | **FULLY WORKING** | `addPart()` within `db.withTransaction{}`: inserts SparePartPurchase + Payment (SUPPLIER) + PaymentTransaction (cash-out) when not pay-later |
| 20 | Handover / completion (Step 5) | HandoverFragment | **FULLY WORKING** | Payment mode: Cash / Online / Both (split must equal total) / Pay Later; save → `completeHandover()` updates entry + creates Payment + 1–2 PaymentTransactions |
| 21 | PDF invoice generation | HandoverFragment | **FULLY WORKING** | `InvoiceGenerator.generateInvoice()` → shares PDF via ACTION_SEND |
| 22 | Cancel work | HandoverFragment | **FULLY WORKING** | Confirmation dialog → sets `isDraft=true`, `workStatus="Cancelled"` |
| 23 | WhatsApp handover notification | HandoverFragment | **FULLY WORKING** | `NotificationUtils.sendHandoverSummaryWhatsApp()` — parts list + amount to customer |
| 24 | Entry detail view + workflow nav | EntryDetailFragment | **FULLY WORKING** | Full entry details (3 photos, fault, charge, status); buttons → Inspection / Quotation / Spare Parts / Handover |

---

## 4. QUICK SALE

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 25 | Record direct sale | SaleFragment | **FULLY WORKING** | Item name, purchase price, sale price, supplier; double-tap guard |
| 26 | Sale accounting | SaleViewModel | **FULLY WORKING** | `saveSale()` within transaction: Sale row + Supplier Payment + CUSTOMER cash-in + SUPPLIER cash-out |

---

## 5. DUES & PAYMENTS

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 27 | Dues dashboard (4 tabs) | DuesFragment | **FULLY WORKING** | Tabs: ALL / DEALER / SUPPLIER / CUSTOMER; total due amounts in header; color-coded status badges |
| 28 | Record payment | PayDuesFragment | **FULLY WORKING** | Payment mode spinner (Cash, Online UPI, Bank); `recordPayment()` within transaction → updates Payment + inserts PaymentTransaction |
| 29 | Payment history view | PayDuesFragment | **FULLY WORKING** | Shows all PaymentTransactions for the personMobile |
| 30 | Return parts to supplier | PartReturnFragment | **FULLY WORKING** | Select part from purchases, reason, refund amount → inserts PartReturn record |

---

## 6. REPORTS

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 31 | Revenue reports | ReportsFragment | **FULLY WORKING** | Period selector: Daily / Weekly / Monthly / Custom (date-range picker); revenue = SUM(finalAmount) for completed handovers |
| 32 | Bar chart — daily revenue | ReportsFragment | **FULLY WORKING** | MPAndroidChart bar chart: daily breakdown in date range |
| 33 | Direct sales list | ReportsFragment | **FULLY WORKING** | itemName, supplier ref, salePrice, profit |

---

## 7. MASTER DATA

### Service Men

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 34 | Service men list | ServiceManListFragment | **FULLY WORKING** | List all technicians |
| 35 | Add service man | ServiceManAddFragment | **FULLY WORKING** | Name, mobile, email, employeeId, designation, monthlySalary, perDaySalary; validates name required, mobile 10 digits |

### Suppliers

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 36 | Supplier list | SupplierListFragment | **FULLY WORKING** | Name, company, mobile; tap → detail; "Add" → SupplierAddFragment |
| 37 | Supplier ledger (detail) | SupplierDetailFragment | **FULLY WORKING** | Total bought, total paid, balance due, payment history, sales history, purchases list; Edit button |
| 38 | Add / edit supplier | SupplierAddFragment | **FULLY WORKING** | Name, company, mobile, email, address, city, GST; mobile locked in edit mode |

### Customers + Dealers

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 39 | Unified customers + dealers list | CustomerListFragment | **FULLY WORKING** | Aggregates both tables; dealers tagged "(Dealer)"; tap → CustomerDetailFragment |
| 40 | Customer/Dealer ledger | CustomerDetailFragment | **FULLY WORKING** | Total jobs, balance due, payment history, work history |
| 41 | Add / edit customer or dealer | CustomerAddFragment | **FULLY WORKING** | Name, mobile, city; `isDealer` arg determines mode; validates mobile 10 digits |

### Common Faults

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 42 | Add common fault | CommonFaultsFragment | **PARTIALLY WORKING** | Name, default charge, category; RecyclerView lists all; `deleteFault()` exists in ViewModel but **no UI button wired** |

### Inventory

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 43 | Inventory transaction ledger | InventoryFragment | **FULLY WORKING** | 3 types: PURCHASE (SparePartPurchase qty × price), SALE (Sale salePrice), RETURN (−refundAmount); sorted most recent first; summary totals |

---

## 8. PAYROLL

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 44 | Payroll overview (Compose) | PayrollFragment | **FULLY WORKING** | Month prev/next nav; summary card; per-technician: worked days (full + half), per-day rate, computed salary, PAID/UNPAID/PARTIAL status |
| 45 | Mark attendance | PayrollViewModel | **FULLY WORKING** | `setAttendance(smId, day, present, halfDay, note)` — upserts Attendance record; refreshes month stats |
| 46 | Generate / update salary slip | PayrollFragment | **FULLY WORKING** | `generateOrUpdateSalary(smId, paidAmount, note)` → creates/updates SalaryPayment; if paid, inserts PaymentTransaction (SALARY) |
| 47 | Salary math engine | PayrollViewModel / PayrollMath | **FULLY WORKING** | `computeWorkedDays()`, `computePayable()` (perDaySalary priority, falls back to monthlySalary/30), `buildSalaryPayment()` |

---

## 9. EXPENSES

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 48 | Record expense | ExpensesFragment (Compose) | **FULLY WORKING** | Month nav; FAB adds dialog; categories: Rent, Electricity, Internet, Supplies, Other; recurring flag; paid checkbox; delete |
| 49 | Expense accounting | ExpensesViewModel | **FULLY WORKING** | `addExpense()` within transaction: inserts Expense; if paid=true, inserts PaymentTransaction (EXPENSE, SHOP) |

---

## 10. ENTRIES LIST

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 50 | Search all entries | EntriesListFragment | **FULLY WORKING** | TextWatcher on search ET → `loadEntries(query)` → full-text search on customerName/mobile/dealerName |
| 51 | View all entries | EntriesListFragment | **FULLY WORKING** | `getAllEntries()` Flow; navigate to EntryDetailFragment on tap |

---

## 11. PROFILE / SETTINGS

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 52 | Shop profile CRUD | ProfileFragment | **FULLY WORKING** | Name, email, phone, shop name, address, GST, profile photo |
| 53 | Profile photo — camera or gallery | ProfileFragment | **FULLY WORKING** | `btnTakeProfilePhoto` opens chooser (camera or gallery) |
| 54 | Link Google Account | ProfileFragment | **PARTIALLY WORKING** | Google Sign-In fills email/name into profile fields |
| 55 | Last sync timestamp | ProfileFragment | **FULLY WORKING** | `lastSyncTimestamp` displayed from UserProfile |

---

## 12. BACKUP & RESTORE

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 56 | Export local backup | MoreFragment | **FULLY WORKING** | `BackupManager.exportLocally()` → copies DB to Downloads as `MuZZu_Tech_Backup_{timestamp}.db` |
| 57 | Share backup file | MoreFragment | **FULLY WORKING** | `BackupManager.shareBackup()` → copies to cache, shares via `ACTION_SEND` with `application/octet-stream` |
| 58 | Restore local backup | MoreFragment | **FULLY WORKING** | File picker → `BackupManager.importDatabase()`: closes DB, streams backup over, resets singleton; confirmation dialog with data-loss warning |
| 59 | Cloud sync (Google Drive) | ProfileFragment, MoreFragment | **STUB** | `BackupManager.syncWithGoogleDrive()` — shows Toast "Google Drive Sync initiated", updates `lastSyncTimestamp/Status` to SUCCESS; does NOT actually upload |

---

## 13. AUTO-UPDATE

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 60 | GitHub version check + APK download | UpdateManager | **FULLY WORKING** | On startup (MainActivity + DashboardFragment), fetches `version.json` from GitHub raw URL; if newer → shows dialog; Download → OkHttp progress → opens installer intent |

---

## 14. NOTIFICATIONS

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 61 | WhatsApp — repair started | NotificationUtils | **FULLY WORKING** | After InspectionFragment save: sends "Repair Started" via `wa.me` URL |
| 62 | WhatsApp — repair completed | NotificationUtils | **PARTIALLY WORKING** | Function exists (`sendRepairCompletedWhatsApp`) but **not called from any UI code path** |
| 63 | WhatsApp — device collected | NotificationUtils | **FULLY WORKING** | After HandoverFragment complete: sends "Device Collected" with parts list + amount |
| 64 | Reorder alert notification | ReorderAlertWorker | **FULLY WORKING** | Reads last 30 days of SparePartPurchases; `AIAnalyzer.predictReorder()` per part; posts notification if any should reorder |
| 65 | Salary reminder notification | SalaryReminderWorker | **FULLY WORKING** | Queries last month's SalaryPayments; filters unpaid; posts notification listing technicians + dues |

---

## 15. AI FEATURES

| # | Feature | Screen | Status | Description |
|---|---|---|---|---|
| 66 | AI photo fault detection | InspectionFragment | **FULLY WORKING** | `AIAnalyzer.suggestFaultsFromPhoto()` — ML Kit Object Detection + Image Labeling + brightness heuristic |
| 67 | AI repair cost estimator | AIAnalyzer (object) | **FULLY WORKING** | `estimateRepairCost()` — matches fault name against CommonFault list → returns defaultCharge |
| 68 | AI repair time estimator | AIAnalyzer | **FULLY WORKING** | `estimateRepairTime()` — Display=2, Battery=1, Charging=1, Motherboard=5, Body=3, default=2 days |
| 69 | AI business health advisor | DashboardFragment | **FULLY WORKING** | `AIAdvisor.analyzeDailyHealth()` — score (0–100), smart move rule engine, revenue/margin computed from today's jobs |
| 70 | AI repair trends analysis | AIAnalyzer | **NOT WIRED** | `analyzeRepairTrends()` — defined; returns totalRepairs, topFaults, averageDays; **not called from any UI** |

---

## 16. BACKGROUND / AUTOMATED JOBS

| # | Job | Worker Class | Trigger / Schedule | What It Does | Status |
|---|---|---|---|---|---|
| 71 | Reorder alert | ReorderAlertWorker | Every 24 h (PeriodicWorkRequest, 1 day), initial delay 15 min, constraint battery-not-low, KEEP policy | Reads last 30 days of SparePartPurchases; groups by partName; `predictReorder()` for each; posts notification on channel `reorder_alerts` if any parts should reorder | **FULLY WORKING** |
| 72 | Salary reminder | SalaryReminderWorker | Every ~30 days; initial delay = minutes until next 1st of month 9:00 AM; KEEP policy | Queries last month's SalaryPayments; filters `status != "PAID"`; posts notification on channel `salary_reminders` with technician names + due amounts | **FULLY WORKING** |

---

## 17. DATABASE ENTITIES (15 Tables)

| # | Table | Entity Class | Real-World Thing |
|---|---|---|---|
| 1 | `repair_entries` | RepairEntry | A phone repair job / work order, from intake to handover |
| 2 | `service_men` | ServiceMan | A technician/employee at the shop |
| 3 | `suppliers` | Supplier | A parts vendor (phone is primary key) |
| 4 | `common_faults` | CommonFault | A predefined fault type with suggested charge (e.g., "Display Replacement") |
| 5 | `spare_part_purchases` | SparePartPurchase | A part bought for a repair job, optionally linked to a supplier |
| 6 | `customers` | Customer | A customer contact record (mobile is primary key) |
| 7 | `dealers` | Dealer | A dealer contact record (mobile is primary key) |
| 8 | `sales` | Sale | A direct sale (accessory/item sold to walk-in) |
| 9 | `user_profile` | UserProfile | Shop owner's profile/branding data (singleton, id=1) |
| 10 | `payments` | Payment | A payment obligation/record (customer, dealer, supplier, salary, expense) |
| 11 | `part_returns` | PartReturn | A return of a faulty/wrong spare part to a supplier |
| 12 | `payment_transactions` | PaymentTransaction | An individual cash-flow event (cash in or out) |
| 13 | `attendance` | Attendance | Daily attendance record per technician (composite PK: servicemanId + date) |
| 14 | `salary_payments` | SalaryPayment | A monthly salary slip for one technician |
| 15 | `expenses` | Expense | A shop expense (rent, electricity, internet, supplies, other) |

**Database version:** 11 — migrations: v8→v9 (attendance/salary/expenses), v9→v10 (FK constraints), v10→v11 (nullable `paymentId` + SET NULL)

---

## 18. PERMISSIONS

| Permission | Used For | Scope |
|---|---|---|
| `android.permission.CAMERA` | Entry, Inspection, SpareParts, Profile — all camera-photo buttons | Runtime requested per screen |
| `WRITE_EXTERNAL_STORAGE` (maxSdk 28) | Backup export to Downloads, UpdateManager APK save | Declared |
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | Backup restore file picker | Declared |
| `READ_MEDIA_IMAGES` | Android 13+ scoped-storage images access | Declared |
| `android.permission.INTERNET` | UpdateManager, otp.dev API, Google Sign-In, WhatsApp URLs | Declared |
| `REQUEST_INSTALL_PACKAGES` | UpdateManager APK installer intent | Declared |
| `POST_NOTIFICATIONS` | ReorderAlertWorker, SalaryReminderWorker | Android 13+ runtime |

---

## 19. STATUS LEGEND

| Legend | Meaning |
|---|---|
| **FULLY WORKING** | Feature is implemented and verified end-to-end |
| **PARTIALLY WORKING** | Core function works but has known gaps (e.g., missing UI trigger, unused code path) |
| **STUB** | Function exists but does nothing real (mock/stub implementation) |
| **NOT WIRED** | Logic is defined but not connected to any UI screen |
| **BYPASSED** | Screen/code exists but is never reached in normal app flow |

---

## 20. KNOWN GAPS / NEEDS DECISION

| # | Item | Details |
|---|---|---|
| 1 | Attendance entry UI | `PayrollViewModel.setAttendance()` exists but there is no screen to batch-mark attendance; currently set programmatically only |
| 2 | Delete Common Fault UI button | `CommonFaultsViewModel.deleteFault()` is defined but no UI button triggers it |
| 3 | AI Repair Trends screen | `AIAnalyzer.analyzeRepairTrends()` is fully implemented but not exposed in any UI |
| 4 | WhatsApp "Repair Completed" notification | Function exists (`sendRepairCompletedWhatsApp`) but not called; only handover-summary notification is used |
| 5 | Real SMS OTP | `OtpManager` exists with `otp.dev` API wiring but LoginFragment only uses WhatsApp OTP |
| 6 | Google Drive sync | `BackupManager.syncWithGoogleDrive()` is a stub (mock timestamp only) |
| 7 | Stock-level inventory | InventoryFragment shows a transaction ledger (purchases/sales/returns), not actual quantity-on-hand stock tracking |
| 8 | Login screen | Complete and functional but bypassed in normal flow (app goes straight to Dashboard) |
