# MuZZu Tech Professional - Complete App Summary

## 1. WHAT THE APP DOES
MuZZu Tech Professional is a comprehensive shop management system designed specifically for mobile repair businesses. It streamlines the entire repair lifecycle—from registering a new device with diagnostic photos to assigning technicians, tracking spare parts, and managing handovers. Beyond repair tracking, the app functions as a complete business ERP, handling customer/dealer ledgers, inventory purchasing, daily sales, technician payroll (with attendance), and expense tracking. It features an integrated, streamlined workflow and robust data security with full database encryption.

## 2. COMPLETE FEATURE LIST

### Repair Management
- **Integrated Repair Entry**: Register repairs for Customers/Dealers with diagnostic photos, specialist assignment, and initial charges in one step. (Fully Working)
- **Repair List/Search**: Filterable list of all active and past repairs. (Fully Working)
- **Spare Parts & Inventory**: Track parts used in repairs and linked to supplier purchases. (Fully Working)
- **Status Tracking**: Transition repairs through "Received", "In Progress", "Ready", and "Handed Over". (Fully Working)
- **Handover & Billing**: Finalize amount, generate digital invoices, and record payments. (Fully Working)

### Inventory & Sales
- **Spare Parts Inventory**: Track stock levels and usage of repair components. (Fully Working)
- **Purchase Management**: Record part purchases from suppliers with automatic ledger updates. (Fully Working)
- **Direct Sales**: sell items/parts directly without a repair ticket. (Fully Working)
- **Supplier Management**: Full CRUD for suppliers with purchase and payment history. (Fully Working)

### Financials & Payroll
- **Customer/Dealer Ledgers**: Track total dues, paid amounts, and transaction history with full CRUD support. (Fully Working)
- **Technician Payroll**: Manage technicians, mark daily attendance, and compute monthly salaries. (Fully Working)
- **Expense Tracking**: Record business expenses with categories and recurring options. (Fully Working)
- **Payments**: Unified payment system for repairs, sales, and general ledger clearing. (Fully Working)

### System Features
- **In-App Updates**: Automated check for new versions via GitHub Releases. (Fully Working)
- **Local Backup & Restore**: Export and import encrypted database backups. (Fully Working)
- **Biometric Lock**: Secure app access using fingerprint/face unlock. (Fully Working)

## 3. TECHNICAL STACK
- **Language**: Kotlin (100%)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern.
- **UI Framework**: XML ViewBinding (Primary), Jetpack Compose (Payroll & Expenses).
- **Navigation**: Jetpack Navigation Component.
- **Concurrency**: Kotlin Coroutines & Flow.
- **Database**: Room Persistence Library (v17) with **SQLCipher Encryption**.
- **Security**: **EncryptedSharedPreferences** for sensitive settings and biometric flags.
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Key Libraries**:
    - **SQLCipher**: Full-disk database encryption.
    - **Glide**: Image loading and caching.
    - **ML Kit**: Object detection and image labeling capabilities (Utility layer).
    - **MPAndroidChart**: Analytics and reporting visualizations.
    - **WorkManager**: Background task scheduling.

## 4. DATABASE ENTITIES
| Entity | Represents |
| :--- | :--- |
| `RepairEntry` | Core repair ticket (device details, photos, status, technician, charges). |
| `ServiceMan` | Technician profiles including salary details. |
| `Customer` / `Dealer` | Contact information and ledger summaries. |
| `Sale` | Record of direct item sales. |
| `SparePartPurchase` | Inventory stock-in records linked to suppliers. |
| `Supplier` | Business entities providing spare parts. |
| `Payment` | Summary of financial transactions for a specific entity. |
| `PaymentTransaction` | Individual credit/debit events linked to Payments/Expenses. |
| `Expense` | General business costs (rent, electricity, etc.). |
| `Attendance` | Daily present/absent logs for technicians. |
| `SalaryPayment` | Monthly payroll disbursement records. |
| `PartReturn` | Logistics for returning faulty parts to suppliers. |
| `LedgerAlert` | Discrepancies found by the Nightly Auditor. |
| `UserProfile` | Shop details (name, email, sync status). |

**Migration History**: Current version is **17**. Recent migrations added payroll/expense tables (v9), enforced Foreign Keys (v10), optimized query indices (v12), fixed ledger reconciliation bugs (v15), and **consolidated repair workflow by dropping common_faults and unused flows (v17)**.

## 5. SCREENS
- **SplashActivity**: Branding and Biometric Security gate.
- **MainActivity**: Single Activity host for all Fragments.
- **DashboardFragment**: Key metrics (today's revenue, active repairs) and business health advisor.
- **EntryFragment**: Streamlined form for registering repairs with photo capture and initial billing.
- **EntriesFragment**: Central list for searching and managing all repair tickets.
- **SparePartsFragment**: Management of components used in a specific repair.
- **HandoverFragment**: Final billing, invoice generation, and completion.
- **PayrollFragment**: Attendance dashboard and salary disbursement (Compose).
- **ExpensesFragment**: Shop expense management (Compose).
- **MoreFragment**: Secondary hub for Master Data (Technicians, Customers, Suppliers) and Backups.

## 6. BACKGROUND JOBS (WorkManager)
- **ReorderAlertWorker**: Daily inventory check that notifies if parts are below threshold.
- **SalaryReminderWorker**: Monthly trigger (1st of month) to prepare technician payroll.

## 7. AI FEATURES
- **AI Business Advisor**: Algorithmic analysis of daily margin and business health. (Fully Working)
- **AI Analysis (Utility)**: `AIAnalyzer` provides device detection and fault labeling logic via ML Kit. (Implemented)
- **Inventory Predictor**: Predictive logic to estimate days until stockout based on usage. (Implemented)

## 8. SECURITY
- **Database Encryption**: Full AES-256 encryption using SQLCipher.
- **Encrypted Preferences**: `EncryptedSharedPreferences` for auth tokens and security flags.
- **Biometrics**: `BiometricPrompt` implementation in `SplashActivity`.

## 9. KNOWN GAPS
- **Cloud Sync**: Google Drive sync is currently a stub.
- **PDF Export**: Invoices are generated as PDFs but consistent sharing across all devices is still being refined.
- **Reports Export**: Financial reports are view-only on screen (no CSV/PDF export yet).

## 10. TEST COVERAGE
- **Test Count**: ~25 Core Test Suites.
- **Covered**: Payroll math, Ledger reconciliation logic, Repair pipeline E2E, Navigation, and WorkManager scheduling.

## 11. RECENT CHANGES (Last 5 Commits)
1. **Workflow Consolidation**: Merged Entry, Inspection, and Quotation into a single-step `EntryFragment`.
2. **Database V17**: Dropped `common_faults` table and purged unused legacy flows.
3. **Security Hardening**: Implemented SQLCipher and EncryptedSharedPreferences globally.
4. **Master Data Refactor**: Added full Edit/Delete support for Technicians, Customers, and Suppliers.
5. **UI Refresh**: Modernized all master data screens with Material 3 components and premium themes.
