# MobileRepairShop — Complete Feature Inventory

## Summary Counts

| Category | Count |
|---|---|
| Total user-facing features | 19 |
| Total screens (fragment classes) | 25 |
| Total activities | 3 |
| Total database entities (tables) | 14 |
| Total DAO interfaces | 14 |
| Total WorkManager jobs | 2 |
| Bottom-nav tabs | 5 |
| Total source files (main) | 94 |

---

## 1. AUTHENTICATION & SECURITY

**1.1 Biometric App Lock**
- Screen: `SplashActivity`
- On first launch, checks encrypted shared preference for biometric_enabled. If enabled, shows BiometricPrompt (fingerprint / PIN / face). Exits app if user cancels authentication. Falls through to MainActivity if biometric is disabled or hardware unavailable.
- Status: **Fully working (Encrypted)**

**1.2 Login — Google Sign-In + WhatsApp OTP**
- Screen: `LoginFragment`
- Two auth methods: (a) Google Sign-In button — signs in via GoogleSignInClient, stores email/name in encrypted shared prefs. (b) WhatsApp OTP — opens WhatsApp with pre-filled message to shop owner's number, user sends OTP, then verifies on the Verify OTP screen. Sets loggedIn flag, navigates to dashboard.
- Status: **Fully working**

---

## 2. DASHBOARD

**2.1 Business KPI Dashboard**
- Screen: `DashboardFragment`
- Shows live metrics: pending repairs count, completed today, daily revenue, daily expense (parts + supplier payments + salary payments + cash expenses), daily net profit, daily investment (parts purchased today), paid vs due portion of investment, total customer dues (A/R), total supplier dues (A/P).
- Data loads via DashboardViewModel, which queries DAOs and combines the flows. Error-resilient (each metric catches exceptions independently).
- Status: **Fully working**

**2.2 AI Business Health Advisor**
- Screen: Dashboard (inline card)
- Analyzes daily profit margin, displays a health score (0–100), a "smart move" label (e.g. "Expense Alert!", "Premium Performance"), and a plain-text recommendation. Powered by `AIAdvisor.kt` — pure computation, no external API calls.
- Status: **Fully working**

---

## 3. REPAIR WORKFLOW (CORE PIPELINE)

**3.1 Integrated Repair Entry**
- Screen: `EntryFragment` (bottom-nav tab "New Entry")
- Create a repair ticket in one step:
    - Enter customer name/mobile (auto-lookup with customer vs dealer ambiguity resolution).
    - City, brand (spinner: 18 brands), model.
    - Assign Specialist (Service Man spinner).
    - Extra items checklist (multi-select with custom "Other" option).
    - Take two entry photos (camera via FileProvider).
    - Set Charge Amount and Advance Paid.
- Saves to `repair_entries` table. Two save modes: **Save** (triggers navigation to Spare Parts) and **Save Draft** (auto-triggered on pause/rotation, dedup by mobile). Both guarded by `isSaving` flag to prevent double-tap.
- Status: **Fully working (Consolidated Workflow)**

**3.2 Spare Parts Management**
- Screen: `SparePartsFragment`
- For a given repair entry, shows add-part form: select supplier (spinner of active suppliers), enter part name, purchase price, quantity, optional part photo, and "pay later" toggle. Adding a part creates SparePartPurchase + Supplier Payment + PaymentTransaction records atomically. Delete parts from the purchase list. **Guard**: blocks adding parts if repair is already in "Done" or handed-over state.
- Status: **Fully working**

**3.3 Repair Handover & Payment**
- Screen: `HandoverFragment`
- Final step: shows entry summary + parts list + total cost. Enter final amount, select payment mode (Cash / Online / Both / Pay Later). For split payment, Cash + Online must equal the total (validated). On complete: updates RepairEntry (workDone, handoverDone, handoverDate), creates Payment record + PaymentTransaction record(s). Generates a PDF invoice via `InvoiceGenerator` and offers Share intent. Sends WhatsApp handover notification to customer.
- Status: **Fully working**

**3.4 Entries List**
- Screen: `EntriesListFragment` (bottom-nav tab "Entries")
- Lists all repairs sorted by date (newest first). Each row shows customer name, device, brand, status (Pending/In Progress/Done/Handed Over). Tapping opens EntryDetailFragment. Supports swipe-to-delete with confirmation dialog.
- Status: **Fully working**

**3.5 Entry Detail**
- Screen: `EntryDetailFragment`
- Shows full details of a single repair: all fields, photos (via Glide), assigned service man, parts used, payment info. Read-only view.
- Status: **Fully working**

---

## 4. SALES

**4.1 Quick Sale (Direct Supplier Sale)**
- Screen: `SaleFragment`
- Record a non-repair direct sale: select supplier from list (or quick-add new supplier inline), enter item name, purchase price, sale price. On save: inserts Sale record, creates Supplier Payment + two PaymentTransaction records (cash-out to supplier + cash-in from sale) in a single Room transaction.
- Status: **Fully working**

---

## 5. DUES & PAYMENTS

**5.1 Dues Overview**
- Screen: `DuesFragment`
- Shows all pending payments grouped by person type (tab filter: All / Dealer / Supplier / Customer). Displays total due amount for the selected filter. Each due row shows person name, type, amount due. Tapping navigates to PayDuesFragment.
- Status: **Fully working**

**5.2 Pay Dues**
- Screen: `PayDuesFragment`
- For a selected due: shows person details, total/paid/due amounts, payment history list. Enter payment amount, select payment mode (Cash / Online-UPI / Online-Bank), optional note. Recording a payment atomically re-reads the current Payment record inside the transaction to avoid lost-update race, then inserts PaymentTransaction.
- Status: **Fully working**

**5.3 Part Return**
- Screen: `PartReturnFragment`
- Record a part returned to a supplier: select purchased part from spinner, enter part name, refund amount, select reason. Saves PartReturn record.
- Status: **Fully working**

---

## 6. FINANCIAL REPORTS

**6.1 Period-Based Financial Reports**
- Screen: `ReportsFragment`
- Generate reports filtered by Daily / Weekly / Monthly / Custom date range. Shows: total revenue in range, completed jobs count, daily breakdown rows, supplier purchases, direct sales.
- Status: **Fully working**

---

## 7. MASTER DATA MANAGEMENT

**7.1 Customer & Dealer Management**
- Screens: `CustomerListFragment`, `CustomerAddFragment`, `CustomerDetailFragment`
- Combined list of customers + dealers. Add new (name, mobile, type picker, email, city, address). Tap to view detail (name, mobile, total jobs, balance due, work history, payment history). Edit by re-using the add screen (pre-filled). Delete available in Detail screen with confirmation.
- Status: **Fully working**

**7.2 Supplier Management**
- Screens: `SupplierListFragment`, `SupplierAddFragment`, `SupplierDetailFragment`
- Full CRUD for suppliers (name, company, mobile, email, city, address, GST). List shows name + company. Detail shows purchases, sales, payment history with balance. Edit re-uses add screen. Delete available in Detail screen.
- Status: **Fully working**

**7.3 Service Men Management**
- Screens: `ServiceManListFragment`, `ServiceManAddFragment`
- Full CRUD for technicians (name, mobile, email, employeeId, designation, monthlySalary, perDaySalary). List shows all active technicians. Click to edit, long-press to delete.
- Status: **Fully working**

**7.4 Inventory Ledger**
- Screen: `InventoryFragment`
- Read-only aggregate view combining all purchases, sales, and part returns in a single chronological list. Shows total inventory value and item count.
- Status: **Fully working**

---

## 8. PAYROLL

**8.1 Attendance Tracking + Salary Generation**
- Screen: `PayrollFragment` (Jetpack Compose)
- Month navigation (prev/next). Loads active service men. For each, a calendar grid with tap-to-toggle: Present / Half-Day / Absent. Shows per-serviceman stats for the month. Generate salary slip: computes payable based on attendance + perDaySalary or monthlySalary. Paying a salary creates PaymentTransaction audit trail.
- Status: **Fully working**

---

## 9. EXPENSES

**9.1 Shop Expense Tracking**
- Screen: `ExpensesFragment` (Jetpack Compose)
- Month navigation. Lists all expenses for the selected month. Shows monthly total and category-grouped totals. Add/Delete/Toggle paid status. Creates PaymentTransaction record if marked paid.
- Status: **Fully working**

---

## 10. SHOP PROFILE

**10.1 Shop Profile Management**
- Screen: `ProfileFragment`
- View and edit shop info: name, phone, shop name, address, email, GST number, profile photo. Save persists to user_profile table.
- Status: **Fully working**

---

## 11. BACKUP & RESTORE

**11.1 Local Database Backup**
- Screen: MoreFragment (Backup button)
- Export: copies the encrypted SQLite database to Downloads. Share: shares via FileProvider intent chooser.
- Status: **Fully working**

**11.2 Local Database Restore**
- Screen: MoreFragment (Restore button)
- Opens file picker. On file selected: closes the database, overwrites with the selected file (must be valid encrypted backup), resets the database singleton.
- Status: **Fully working**

**11.3 Cloud Sync (Google Drive)**
- Screen: MoreFragment -> Profile (Cloud Sync)
- Deprecated stub: throws `NotImplementedError`. Method annotated `@Deprecated`.
- Status: **Stub**

---

## 12. MORE MENU (NAVIGATION HUB)

**12.1 More Hub**
- Screen: `MoreFragment` (bottom-nav tab "More")
- Central menu with card buttons for all modules. Shows live counts for service men, customers, and suppliers. Displays app version from BuildConfig. Controls for Dark Mode and Biometric Security.
- Status: **Fully working**

---

## 13. BACKGROUND AUTOMATION

**13.1 Low-Stock Reorder Alert (Daily)**
- Worker: `ReorderAlertWorker`
- Scans spare part usage to detect low stock, posts a notification.
- Status: **Fully working**

**13.2 Monthly Salary Reminder**
- Worker: `SalaryReminderWorker`
- Runs every ~30 days. Queries pending salary payments and posts a notification.
- Status: **Fully working**

---

## 14. AI & SMART FEATURES

**14.1 Business Health Analysis**
- Screen: Dashboard (inline card)
- Pure algorithmic analysis of daily margin, pending jobs, and expense vs revenue ratio.
- Status: **Fully working**

**14.2 Repair Trend Analysis**
- Utility: `AIAnalyzer.analyzeRepairTrends()` — computes trends. Available for future dashboard extensions.
- Status: **Implemented but not wired to UI**

**14.3 Reorder Prediction**
- Utility: `AIAnalyzer.predictReorder()` — computes days until stockout and suggested reorder quantity.
- Status: **Implemented but not wired to UI**

---

## 15. WHATSAPP & NOTIFICATIONS

**15.1 WhatsApp Repair Notification**
- Triggered from: HandoverFragment (on handover)
- Opens WhatsApp with pre-filled message: "Your device repair has been completed at MuZZu Tech..."
- Status: **Fully working**

**15.2 WhatsApp OTP Login**
- See 1.2 above.
- Status: **Fully working**

---

## Database Schema (14 Tables)

| # | Table | Real-World Thing | Key Fields |
|---|-------|------------------|-----------|
| 1 | `repair_entries` | A repair ticket/job | id, customerName, mobile, brand, model, chargeAmount, advanceAmount, workStatus, assignedServiceMan |
| 2 | `customers` | A customer | mobile (PK), name, email, city, address, type (CUSTOMER/DEALER) |
| 3 | `dealers` | A dealer/business customer | mobile (PK), name, email, city, address, type |
| 4 | `suppliers` | A parts supplier/vendor | mobile (PK), name, companyName, email, city, address, gstNo |
| 5 | `service_men` | A technician/employee | id, name, mobile, email, employeeId, designation, monthlySalary, perDaySalary |
| 6 | `spare_part_purchases` | A spare part bought | id, repairEntryId (FK), partName, purchasePrice, quantity, supplierId |
| 7 | `sales` | A direct (non-repair) sale | id, itemName, purchasePrice, salePrice, supplierMobile, date |
| 8 | `payments` | A payment record (A/R or A/P) | id, personType, personMobile, personName, totalAmount, paidAmount, dueAmount |
| 9 | `payment_transactions` | An individual txn | id, paymentId (FK), amount, paymentMode, transactionDate |
| 10 | `part_returns` | A spare part returned | id, partName, refundAmount, reason, supplierId |
| 11 | `attendance` | Daily attendance | servicemanId (PK), date (PK), present, halfDay |
| 12 | `salary_payments` | A monthly salary slip | id, servicemanId (FK), monthStart, computedAmount, status |
| 13 | `expenses` | A shop expense entry | id, title, amount, category, date, paid |
| 14 | `user_profile` | Shop owner's profile | id, name, shopName, gstNo |

---

## Complete Screen List (25 Fragments + 3 Activities)

### Activities
| # | Class | Purpose |
|---|-------|---------|
| 1 | `SplashActivity` | Biometric gate → MainActivity |
| 2 | `MainActivity` | Single-activity host with bottom nav |
| 3 | `TestLauncherActivity` | Debug deep-link launcher |

### Navigation Fragments (25)
| # | Fragment | Module | Access |
|---|----------|--------|--------|
| 1 | LoginFragment | Auth | First launch |
| 2 | DashboardFragment | Dashboard | Bottom-nav tab |
| 3 | EntryFragment | Repair Workflow | Bottom-nav tab |
| 4 | SparePartsFragment | Repair Workflow | From Entry save |
| 5 | HandoverFragment | Repair Workflow | From SpareParts save |
| 6 | SaleFragment | Sales | Quick action |
| 7 | EntriesListFragment | Repair Workflow | Bottom-nav tab |
| 8 | EntryDetailFragment | Repair Workflow | Tap entry in list |
| 9 | DuesFragment | Dues | Quick action |
| 10 | PayDuesFragment | Dues | Tap due item |
| 11 | PartReturnFragment | Dues | From PayDues |
| 12 | ReportsFragment | Reports | Bottom-nav tab |
| 13 | MoreFragment | More Hub | Bottom-nav tab |
| 14 | ServiceManListFragment | Master Data | More menu |
| 15 | ServiceManAddFragment | Master Data | More menu / Edit |
| 16 | SupplierListFragment | Master Data | More menu |
| 17 | SupplierDetailFragment | Master Data | Tap supplier |
| 18 | SupplierAddFragment | Master Data | More menu / Edit |
| 19 | CustomerListFragment | Master Data | More menu |
| 20 | CustomerDetailFragment | Master Data | Tap customer |
| 21 | CustomerAddFragment | Master Data | More menu / Edit |
| 22 | ProfileFragment | Profile | More menu |
| 23 | InventoryFragment | Master Data | More menu |
| 24 | PayrollFragment | Payroll | More menu |
| 25 | ExpensesFragment | Expenses | More menu |

---

## Known Gaps

1. **Google Drive Cloud Sync** — `@Deprecated` stub, throws NotImplementedError at runtime
2. **Trend Analysis + Reorder Prediction** — pure functions exist in `AIAnalyzer` but no screen consumes them yet
3. **Reports: no PDF/CSV export** — reports are view-only on screen
4. **Invoice PDF test** — blocked by Robolectric not shadowing `PdfDocument`
