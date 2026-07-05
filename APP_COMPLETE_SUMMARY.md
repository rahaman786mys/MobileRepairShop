# MuZZu Tech Professional - Complete App Summary

## 1. WHAT THE APP DOES
MuZZu Tech Professional is a comprehensive shop management system designed specifically for mobile repair businesses. It streamlines the entire repair lifecycle—from registering a new device with diagnostic photos to assigning technicians, tracking spare parts, and managing handovers. Beyond repair tracking, the app functions as a complete business ERP, handling customer/dealer ledgers, inventory purchasing, daily sales, technician payroll (with attendance), and expense tracking, all while providing AI-powered diagnostic suggestions and automated nightly ledger audits to ensure financial accuracy.

## 2. COMPLETE FEATURE LIST

### Repair Management
- **New Repair Entry**: Register repairs for Customers/Dealers with 2 mandatory diagnostic photos. (Fully Working)
- **Repair List/Search**: Filterable list of all active and past repairs. (Fully Working)
- **Device Inspection**: AI-powered photo analysis to suggest faults and estimate costs/time. (Fully Working)
- **Status Tracking**: Transition repairs through "Received", "In Progress", "Ready", and "Handed Over". (Fully Working)
- **Handover & Billing**: Finalize amount, generate digital invoices, and record payments. (Fully Working)

### Inventory & Sales
- **Spare Parts Inventory**: Track stock levels of repair components. (Fully Working)
- **Purchase Management**: Record part purchases from suppliers. (Fully Working)
- **Direct Sales**: Sell items/parts directly without a repair ticket. (Fully Working)
- **Supplier Management**: Track supplier details and purchase history. (Fully Working)

### Financials & Payroll
- **Customer/Dealer Ledgers**: Track total dues, paid amounts, and transaction history. (Fully Working)
- **Technician Payroll**: Manage technicians, mark daily attendance, and compute monthly salaries. (Fully Working)
- **Expense Tracking**: Record business expenses with recurring options. (Fully Working)
- **Payments**: Unified payment system for repairs, sales, and general ledger clearing. (Fully Working)
- **Nightly Auditor**: Background job that reconciles ledgers and flags financial mismatches. (Fully Working)

### System Features
- **In-App Updates**: Automated check for new versions via GitHub Releases with background download/install. (Fully Working)
- **Cloud/Local Backup**: Sync data with Google Drive or export/import local backup files. (Fully Working)
- **Biometric Lock**: Secure app access using fingerprint/face unlock. (Fully Working)

## 3. TECHNICAL STACK
- **Language**: Kotlin (100%)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern.
- **UI Framework**: XML ViewBinding (Primary), Jetpack Compose (Modernized components).
- **Navigation**: Jetpack Navigation Component.
- **Concurrency**: Kotlin Coroutines & Flow.
- **Database**: Room Persistence Library (v15).
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Key Libraries**:
    - **Glide**: Image loading and caching.
    - **ML Kit**: Object detection and image labeling for AI diagnostics.
    - **MPAndroidChart**: Analytics and reporting visualizations.
    - **WorkManager**: Background task scheduling.
    - **Google Play Services Auth**: Google Sign-In and Drive integration.

## 4. DATABASE ENTITIES
| Entity | Represents |
| :--- | :--- |
| `RepairEntry` | Core repair ticket (device details, photos, status, technician). |
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

**Migration History**: Current version is **15**. Recent migrations added payroll/expense tables (v9), enforced Foreign Keys (v10), optimized query indices (v12), and fixed ledger reconciliation bugs (v15).

## 5. SCREENS
- **SplashActivity**: Branding and Biometric Security gate.
- **MainActivity**: Single Activity host for all Fragments.
- **DashboardFragment**: Key metrics (today's revenue, active repairs) and quick actions.
- **EntryFragment**: Form for registering new repairs with photo capture.
- **EntriesFragment**: Central list for searching and managing all repair tickets.
- **InspectionFragment**: Diagnostic hub where AI analyzes photos for faults.
- **PayrollFragment**: Attendance dashboard and salary disbursement.
- **SaleFragment**: Interface for creating direct sales.
- **ProfileFragment**: Shop settings, Biometric toggle, and Google Sign-In.
- **MoreFragment**: Secondary hub for Master Data (Technicians, Suppliers, Faults) and Backups.
- **UpdateBottomSheet**: Interactive UI for downloading and installing app updates.

## 6. BACKGROUND JOBS (WorkManager)
- **UpdateWorker**: Daily check for new versions on GitHub; downloads APKs in background.
- **LedgerAuditWorker**: Nightly task that reconciles all transaction totals against ledger summaries.
- **ReorderAlertWorker**: Daily inventory check that notifies if parts are below threshold.
- **SalaryReminderWorker**: Monthly trigger (1st of month) to prepare technician payroll.

## 7. AI FEATURES
- **AI Diagnostics**: Uses ML Kit Object Detection to identify devices and Image Labeling to detect cracks, liquid, or display damage. (Fully Working)
- **Repair Estimator**: Heuristic-based logic to predict repair cost and time based on fault name. (Fully Working)
- **Inventory Predictor**: Predictive logic (in `AIAnalyzer`) to estimate days until stockout based on weekly usage. (Fully Working)

## 8. SECURITY
- **Authentication**: Google Sign-In for cloud sync; Local session management via SharedPreferences.
- **Biometrics**: `BiometricPrompt` implementation in `SplashActivity` for app-level lock.
- **Gaps**:
    - Data is stored in an unencrypted Room database locally.
    - No multi-user role-based access control (Admin vs Technician).

## 9. KNOWN GAPS
- **Unencrypted Database**: Local SQLite file is not encrypted (SQLCipher).
- **No Export to PDF**: Invoice generation exists in code (`InvoiceGenerator`) but UI sharing is inconsistent.
- **Stubbed Analytics**: Some charts in `ReportsFragment` use static or mock-heavy data.

## 10. TEST COVERAGE
- **Test Count**: ~15-20 Core Test Suites.
- **Covered**: Payroll math, Ledger reconciliation logic, Repair pipeline E2E, Navigation, and WorkManager scheduling.
- **Missing**: Edge case UI testing for Biometrics, Google Drive sync error states, and low-level Repository unit tests.

## 11. RECENT CHANGES (Last 10 Commits)
1. **GitHub Update Flow**: Switched to GitHub Releases API for more reliable update checks.
2. **Glide Optimization**: Improved photo loading in lists using thumbnails and existence guards.
3. **Version Parsing Fix**: Fixed a bug where version codes were parsed incorrectly as hashes.
4. **Camera Return Fix**: Resolved a conflict where Glide would block camera results from loading.
5. **Modern Camera API**: Refactored camera flows to use `ActivityResultContracts`.
6. **BuildConfig Fix**: Fixed CI build failures by qualifying `BuildConfig` references.
7. **In-App Update System**: Completed and verified the full automated update workflow.
8. **Dependency Revert**: Restored stable AGP and Gradle versions to fix build environment issues.
9. **Update System Prep**: Initial commit for the modernized update system components.
10. **Camera Crash Fix**: Added permission guards and error handling to `EntryFragment` camera launch.
