# MobileRepairShop — Complete Feature Inventory

## Summary Counts

| Category | Count |
|---|---|
| Total user-facing features | 22 |
| Total screens (fragment classes) | 28 |
| Total activities | 3 |
| Total database entities (tables) | 15 |
| Total DAO interfaces | 15 |
| Total WorkManager jobs | 2 |
| Bottom-nav tabs | 5 |
| Total source files (main) | 97 |

---

## 1. AUTHENTICATION & SECURITY

**1.1 Biometric App Lock**
- Screen: `SplashActivity`
- On first launch, checks shared preference for biometric_enabled. If enabled, shows BiometricPrompt (fingerprint / PIN / face). Exits app if user cancels authentication. Falls through to MainActivity if biometric is disabled or hardware unavailable.
- Status: **Fully working**

**1.2 Login — Google Sign-In + WhatsApp OTP**
- Screen: `LoginFragment`
- Two auth methods: (a) Google Sign-In button — signs in via GoogleSignInClient, stores email/name in shared prefs, handles SHA-1 mismatch error. (b) WhatsApp OTP — opens WhatsApp with pre-filled message to shop owner's number, user sends OTP, then verifies on the Verify OTP screen. Sets loggedIn flag, navigates to dashboard.
- Status: **Fully working**

---

## 2. DASHBOARD

**2.1 Business KPI Dashboard**
- Screen: `DashboardFragment`
- Shows live metrics: pending repairs count, completed today, daily revenue, daily expense (parts + supplier payments + salary payments + cash expenses), daily net profit, daily investment (parts purchased today), paid vs due portion of investment, total customer dues (A/R), total supplier dues (A/P).
- Data loads via DashboardViewModel, which queries all seven DAOs and combines the flows. Error-resilient (each metric catches exceptions independently).
- Status: **Fully working**

**2.2 AI Business Health Advisor**
- Screen: Dashboard (inline card)
- Analyzes daily profit margin, displays a health score (0–100), a "smart move" label (e.g. "Expense Alert!", "Premium Performance"), and a plain-text recommendation. Powered by `AIAdvisor.kt` — pure computation, no external API calls.
- Status: **Fully working**

---

## 3. REPAIR WORKFLOW (CORE PIPELINE)

**3.1 New Repair Entry**
- Screen: `EntryFragment` (bottom-nav tab "New Entry")
- Create a repair ticket: enter customer name/mobile (auto-lookup with customer vs dealer ambiguitiy resolution), city, brand (spinner: 18 brands), model, service man assignment (spinner from DB), extra items checklist (multi-select with custom "Other" option), take two entry photos (camera via FileProvider, permission-managed). Validates mobile, photos, brand, model, specialist. Saves to `repair_entries` table. Two save modes: **Save** (triggers navigation to Inspection) and **Save Draft** (auto-triggered on pause/rotation, dedup by mobile). Both guarded by `isSaving` flag to prevent double-tap. Edge case validation: `.take(100)` on name/city, `.take(50)` on brand/model, `.take(200)` on extraItems.
- Status: **Fully working**

**3.2 Device Inspection**
- Screen: `InspectionFragment`
- Load existing entry (displays previously taken inspection photo). Loads common faults list from DB. Camera button for inspection photo (permission-managed). **AI Analyze** button runs `AIAnalyzer.suggestFaultsFromPhoto()` using ML Kit object detection + image labeling + pixel heuristics to suggest possible faults. Tap a common fault to populate the fault text field, or type custom fault. Saves: updates RepairEntry with fault detected, inspection photo path, inspection timestamp. Sends WhatsApp "repair started" notification to customer. Navigates to Quotation.
- Status: **Fully working**

**3.3 Quotation**
- Screen: `QuotationFragment`
- Shows the selected fault. Enter charge amount (repair labor) and advance amount paid. On save: updates RepairEntry.chargeAmount and marks quotationDone timestamp. Navigates to Spare Parts.
- Status: **Fully working**

**3.4 Spare Parts Management**
- Screen: `SparePartsFragment`
- For a given repair entry, shows add-part form: select supplier (spinner of active suppliers), enter part name, purchase price, quantity, optional part photo, and "pay later" toggle. Adding a part creates SparePartPurchase + Supplier Payment + PaymentTransaction records atomically. Delete parts from the purchase list. **Guard**: blocks adding parts if repair is already in "Done" or handed-over state (shows Snackbar error). Edge case validation: `.take(100)` on partName, `.take(20)` on supplierId.
- Status: **Fully working**

**3.5 Repair Handover & Payment**
- Screen: `HandoverFragment`
- Final step: shows entry summary + parts list + total cost. Enter final amount, select payment mode (Cash / Online / Both / Pay Later). For split payment, Cash + Online must equal the total (validated). On complete: updates RepairEntry (workDone, handoverDone, handoverDate), creates Payment record + PaymentTransaction record(s). Generates a PDF invoice via `InvoiceGenerator` and offers Share intent. Sends WhatsApp handover notification to customer. Also supports Cancel (moves entry to drafts).
- Status: **Fully working**

**3.6 Entries List**
- Screen: `EntriesListFragment` (bottom-nav tab "Entries")
- Lists all repairs sorted by date (newest first). Each row shows customer name, device, brand, status (Pending/In Progress/Done/Handed Over). Tapping opens EntryDetailFragment. Supports swipe-to-delete with confirmation dialog.
- Status: **Fully working**

**3.7 Entry Detail**
- Screen: `EntryDetailFragment`
- Shows full details of a single repair: all fields, photos (via Glide), fault, assigned service man, parts used, payment info. Read-only view.
- Status: **Fully working**

---

## 4. SALES

**4.1 Quick Sale (Direct Supplier Sale)**
- Screen: `SaleFragment`
- Record a non-repair direct sale: select supplier from list (or quick-add new supplier inline), enter item name, purchase price, sale price. On save: inserts Sale record, creates Supplier Payment + two PaymentTransaction records (cash-out to supplier + cash-in from sale) in a single Room transaction. Edge case validation: `.take(100)` on itemName. Button guarded by `isSaving` to prevent double-tap.
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
- Record a part returned to a supplier: select purchased part from spinner (live-loaded from SparePartPurchaseDao), enter part name, refund amount, select reason (Defective / Wrong Item / Not Needed / Damaged / Other). Saves PartReturn record.
- Status: **Fully working**

---

## 6. FINANCIAL REPORTS

**6.1 Period-Based Financial Reports**
- Screen: `ReportsFragment`
- Generate reports filtered by Daily / Weekly / Monthly / Custom date range. Shows: total revenue in range, completed jobs count, daily breakdown rows, supplier purchases, direct sales. All data is read-only. Caches the job handle to cancel stale async loads.
- Status: **Fully working** (missing PDF/CSV export — not a gap, just not implemented)

---

## 7. MASTER DATA MANAGEMENT

**7.1 Customer & Dealer Management**
- Screens: `CustomerListFragment`, `CustomerAddFragment`, `CustomerDetailFragment`
- Combined list of customers + dealers. Add new (name, mobile, type picker, email, city, address). Tap to view detail (name, mobile, total jobs, balance due, work history, payment history). Edit by re-using the add screen (pre-filled). **Missing: delete** — no delete button on list or detail, and no delete() method wired in UI.
- Status: **Partially working** (C-R-U but no delete)

**7.2 Supplier Management**
- Screens: `SupplierListFragment`, `SupplierAddFragment`, `SupplierDetailFragment`
- Full CRUD for suppliers (name, company, mobile, email, city, address, GST). List shows name + company. Detail shows purchases, sales, payment history with balance. Edit re-uses add screen. **Missing: delete** — no delete button on list or detail.
- Status: **Partially working** (C-R-U but no delete)

**7.3 Service Men Management**
- Screens: `ServiceManListFragment`, `ServiceManAddFragment`
- Add service men (name, mobile, email, employeeId, designation, monthlySalary, perDaySalary). List shows all active service men. **Missing: detail/edit screen** — clicking a list item does nothing. **Missing: delete UI** — `delete()` method exists in ViewModel but no button in UI.
- Status: **Partially working** (C-R, no update, no delete UI)

**7.4 Common Faults**
- Screen: `CommonFaultsFragment`
- Manage the master list of common device faults used during Inspection. Shows all faults with name, category, default charge. Add new fault (name, category, default charge). Delete fault with confirmation dialog. **Missing: edit/update** — no way to modify an existing fault.
- Status: **Partially working** (C-R-D, no update)

**7.5 Inventory Ledger**
- Screen: `InventoryFragment`
- Read-only aggregate view combining all purchases, sales, and part returns in a single chronological list. Shows total inventory value and item count. Mutations happen through SpareParts (purchases) and Sales (sales) screens.
- Status: **Fully working** (read-only aggregate)

---

## 8. PAYROLL

**8.1 Attendance Tracking + Salary Generation**
- Screen: `PayrollFragment` (Jetpack Compose)
- Month navigation (prev/next). Loads active service men. For each, a calendar grid with tap-to-toggle: Present / Half-Day / Absent. Shows per-serviceman stats (full days, half days, absent, worked days) for the month. Generate salary slip: computes payable based on attendance + perDaySalary or monthlySalary, creates SalaryPayment record with status UNPAID/PAID/PARTIAL. Paying a salary creates PaymentTransaction audit trail. Pure math extracted to `PayrollMath.kt` (unit tested: 11/11).
- Status: **Fully working**

---

## 9. EXPENSES

**9.1 Shop Expense Tracking**
- Screen: `ExpensesFragment` (Jetpack Compose)
- Month navigation (prev/next, jump to month). Lists all expenses for the selected month. Shows monthly total and category-grouped totals. Add expense: title, amount, category (dropdown), date, recurring toggle, paid toggle, note. Creates PaymentTransaction record if marked paid. Delete expense. Toggle paid/unpaid.
- Status: **Fully working**

---

## 10. SHOP PROFILE

**10.1 Shop Profile Management**
- Screen: `ProfileFragment`
- View and edit shop info: name, phone, shop name, address, email, GST number, profile photo. Save persists to user_profile table. Displays current profile data on load.
- Status: **Fully working**

---

## 11. BACKUP & RESTORE

**11.1 Local Database Backup**
- Screen: MoreFragment (Backup button)
- Export: copies the live SQLite database to `/sdcard/Download/MuZZu_Tech_Backup_<timestamp>.db` with Toast confirmation. Share: copies to cache dir, shares via FileProvider intent chooser.
- Status: **Fully working**

**11.2 Local Database Restore**
- Screen: MoreFragment (Restore button)
- Opens file picker (ACTION_OPEN_DOCUMENT). On file selected: closes the database, overwrites with the selected file, resets the database singleton. Shows confirmation dialog before restoring.
- Status: **Fully working**

**11.3 Cloud Sync (Google Drive)**
- Screen: MoreFragment -> Profile (Cloud Sync)
- Deprecated stub: throws `NotImplementedError("Google Drive API not integrated. Tracked at TODO-123")`. Method annotated `@Deprecated`. Google Sign-In still works for login but the sync function itself is not implemented.
- Status: **Stub** — throws NotImplementedError at runtime

---

## 12. MORE MENU (NAVIGATION HUB)

**12.1 More Hub**
- Screen: `MoreFragment` (bottom-nav tab "More")
- Central menu with card buttons: Service Men, Customers, Suppliers, Common Faults, Inventory, Payroll, Expenses, Profile. Also Cloud Sync, Backup Local, Share Backup, Restore, Logout. Shows live counts for service men, customers, suppliers, and faults on their cards. Shows current profile name/email and sync timestamp. Displays app version from BuildConfig.
- Status: **Fully working**

---

## 13. BACKGROUND AUTOMATION

**13.1 Low-Stock Reorder Alert (Daily)**
- Worker: `ReorderAlertWorker` — extends `CoroutineWorker`
- Scheduled by `AppScheduler.enqueueDailyJobs()` on app startup with `ExistingPeriodicWorkPolicy.KEEP`. Runs daily with `setRequiresBatteryNotLow(true)`. Scans spare part usage to detect low stock, posts a notification (channel `reorder_alerts`, ID 4201).
- Status: **Fully working** (but stock levels are inferred from usage — no explicit min-stock field per part)

**13.2 Monthly Salary Reminder**
- Worker: `SalaryReminderWorker` — extends `CoroutineWorker`
- Scheduled by `AppScheduler` with an initial delay to the 1st of next month at 9 AM, then runs every ~30 days. Queries salary_payments for pending slips and posts a notification with per-technician breakdown and total due (channel `salary_reminders`, ID 4202).
- Status: **Fully working**

---

## 14. AI & SMART FEATURES

**14.1 Photo-Based Fault Suggestion (ML Kit)**
- Screen: InspectionFragment (AI Analyze button)
- Uses Google ML Kit: (a) Object Detection — detects if a phone is present in the photo. (b) Image Labeling — scans for screen/display/crack/water/liquid labels with confidence threshold. (c) Pixel heuristics — samples pixels across the image for average brightness (backlight / power-on indicators). Returns a list of text suggestions shown to the technician.
- Status: **Fully working**

**14.2 Business Health Analysis**
- Screen: Dashboard (inline card)
- See 2.2 above. Pure algorithmic analysis of daily margin, pending jobs, and expense vs revenue ratio.
- Status: **Fully working**

**14.3 Repair Trend Analysis**
- Utility: `AIAnalyzer.analyzeRepairTrends()` — computes total/completed/pending repairs, total/average revenue, top 5 faults by frequency, average repair time in days. Not currently rendered on any screen; available for future use.
- Status: **Implemented but not wired to UI** (available in AIAnalyzer object, no screen consumes it)

**14.4 Reorder Prediction**
- Utility: `AIAnalyzer.predictReorder()` — given a part name, usage count, current stock, and lead time, computes days until stockout and suggested reorder quantity. Not currently wired to any screen.
- Status: **Implemented but not wired to UI** (available in AIAnalyzer object, not consumed by any screen)

---

## 15. WHATSAPP & NOTIFICATIONS

**15.1 WhatsApp Repair Notification**
- Triggered from: InspectionFragment (on inspection save), HandoverFragment (on handover)
- Opens WhatsApp with pre-filled message to the customer's mobile number: "Your device repair has started / been completed at MuZZu Tech..."
- Status: **Fully working**

**15.2 WhatsApp OTP Login**
- See 1.2 above.
- Status: **Fully working**

---

## Database Schema (15 Tables)

| # | Table | Real-World Thing | Key Fields |
|---|-------|------------------|-----------|
| 1 | `repair_entries` | A repair ticket/job for a customer's device | id, customerName, mobile, brand, model, fault, chargeAmount, workDone, handoverDone, photos, assignedServiceMan |
| 2 | `customers` | A customer who brings devices for repair | mobile (PK), name, email, city, address, type (CUSTOMER/DEALER) |
| 3 | `dealers` | A dealer/business customer | mobile (PK), name, email, city, address, type |
| 4 | `suppliers` | A parts supplier/vendor | mobile (PK), name, companyName, email, city, address, gstNo |
| 5 | `service_men` | A technician/employee | id, name, mobile, email, employeeId, designation, monthlySalary, perDaySalary |
| 6 | `common_faults` | Pre-defined device faults for quick selection | id, faultName, defaultCharge, category, sortOrder |
| 7 | `spare_part_purchases` | A spare part bought for a repair | id, repairEntryId (FK), partName, purchasePrice, quantity, supplierId, supplierName |
| 8 | `sales` | A direct (non-repair) sale | id, itemName, purchasePrice, salePrice, supplierMobile, date |
| 9 | `payments` | A payment record (A/R or A/P) | id, personType, personMobile, personName, totalAmount, paidAmount, dueAmount, type (CREDIT/DEBIT) |
| 10 | `payment_transactions` | An individual payment transaction | id, paymentId (FK nullable), amount, paymentMode, personType, personMobile, note, date |
| 11 | `part_returns` | A spare part returned to supplier | id, partName, refundAmount, reason, sparePartPurchaseId, supplierId |
| 12 | `attendance` | Daily attendance for a service man | servicemanId (PK), date (PK), present, halfDay, note |
| 13 | `salary_payments` | A generated monthly salary slip | id, servicemanId (FK), monthStart, daysWorked, computedAmount, paidAmount, dueAmount, status (UNPAID/PAID/PARTIAL) |
| 14 | `expenses` | A shop expense entry | id, title, amount, category, date, isRecurring, paid, note |
| 15 | `user_profile` | Shop owner's profile | id, name, phone, shopName, shopAddress, email, gstNo, profilePhotoPath |

**DAO count: 15** (one per table, all in `data/db/dao/`)

---

## Complete Screen List (28 Fragments + 3 Activities)

### Activities
| # | Class | Purpose |
|---|-------|---------|
| 1 | `SplashActivity` | Biometric gate → MainActivity |
| 2 | `MainActivity` | Single-activity host with bottom nav |
| 3 | `TestLauncherActivity` | Debug deep-link launcher |

### Navigation Fragments (28)
| # | Fragment | Module | Access |
|---|----------|--------|--------|
| 1 | LoginFragment | Auth | First launch |
| 2 | DashboardFragment | Dashboard | Bottom-nav tab |
| 3 | EntryFragment | Repair Workflow | Bottom-nav tab |
| 4 | InspectionFragment | Repair Workflow | From Entry save |
| 5 | QuotationFragment | Repair Workflow | From Inspection save |
| 6 | SparePartsFragment | Repair Workflow | From Quotation save |
| 7 | HandoverFragment | Repair Workflow | From SpareParts save |
| 8 | SaleFragment | Sales | Quick action |
| 9 | EntriesListFragment | Repair Workflow | Bottom-nav tab |
| 10 | EntryDetailFragment | Repair Workflow | Tap entry in list |
| 11 | DuesFragment | Dues | Quick action |
| 12 | PayDuesFragment | Dues | Tap due item |
| 13 | PartReturnFragment | Dues | From PayDues |
| 14 | ReportsFragment | Reports | Bottom-nav tab |
| 15 | MoreFragment | More Hub | Bottom-nav tab |
| 16 | ServiceManListFragment | Master Data | More menu |
| 17 | ServiceManAddFragment | Master Data | More menu |
| 18 | SupplierListFragment | Master Data | More menu |
| 19 | SupplierDetailFragment | Master Data | Tap supplier |
| 20 | SupplierAddFragment | Master Data | More menu |
| 21 | CommonFaultsFragment | Master Data | More menu |
| 22 | CustomerListFragment | Master Data | More menu |
| 23 | CustomerDetailFragment | Master Data | Tap customer |
| 24 | CustomerAddFragment | Master Data | More menu |
| 25 | ProfileFragment | Profile | More menu |
| 26 | InventoryFragment | Master Data | More menu |
| 27 | PayrollFragment | Payroll | More menu |
| 28 | ExpensesFragment | Expenses | More menu |

---

## Known Gaps

1. **Customer/Dealer: no delete** — no way to remove a customer or dealer from the system
2. **Supplier: no delete** — no way to remove a supplier
3. **Service Men: no detail/edit screen** — list items are not clickable; no way to edit existing service man
4. **Service Men: no delete UI** — `delete()` method exists in ViewModel but has no UI button
5. **Common Faults: no update** — can add and delete, but cannot edit an existing fault
6. **Google Drive Cloud Sync** — `@Deprecated` stub, throws NotImplementedError at runtime
7. **Trend Analysis + Reorder Prediction** — pure functions exist in `AIAnalyzer` but no screen consumes them yet
8. **Reports: no PDF/CSV export** — reports are view-only on screen
9. **Invoice PDF test** — blocked by Robolectric not shadowing `PdfDocument` (unit test uses file-naming validation instead)
