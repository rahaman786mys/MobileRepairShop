# TEST PLAN â€” Phase 1: Scenario Map
**Project:** MobileRepairShop
**Phase:** Phase 1 â€” Scenario Map
**Source of Truth:** `APP_FEATURES.md`
**Generated:** 2026-07-04
**Total Features Covered:** 71
**Total Scenarios Target:** 200+

---

## HOW TO READ THIS DOCUMENT

Each scenario has:
- **ID** â€” `TP-001` format, sequential
- **Feature#** â€” Links to the feature number in `APP_FEATURES.md`
- **Priority** â€” P1 (blocking / data-critical), P2 (core UX), P3 (nice-to-have)
- **Type** â€” `HAPPY`, `BRANCH`, `BOUNDARY`, `INTERRUPT`
- **Status Target** â€” Items marked STUB, NOT WIRED, or BYPASSED in APP_FEATURES.md have a `[VERIFY DEGRADATION]` tag

---

## SECTION 1 â€” AUTHENTICATION (Features 1-5)

### Feature 1 â€” Splash + Biometric lock

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-001 | Feature 1 | P1 | HAPPY | Splash shows 300ms then transitions to Dashboard when biometric disabled | 1. Disable biometric in app settings. 2. Force-close and relaunch app. | Splash screen appears; after 300ms app navigates to Dashboard without biometric prompt. |
| TP-002 | Feature 1 | P1 | HAPPY | Splash shows BiometricPrompt when biometric enabled and user authenticates | 1. Enable biometric. 2. Ensure device has enrolled biometrics. 3. Force-close and relaunch. | BiometricPrompt dialog appears within 300ms; auth succeeds; Dashboard opens. |
| TP-003 | Feature 1 | P2 | BRANCH | Splash enforces 300ms delay even after biometric success | 1. Enable biometric. 2. Authenticate. 3. Observe timing. | Dashboard appears no sooner than 300ms after successful auth. |
| TP-004 | Feature 1 | P2 | BOUNDARY | First launch (no app_settings prefs yet) â€” biometric defaults to OFF | 1. Uninstall and reinstall app. 2. Cold-launch. | No BiometricPrompt on first launch; app navigates straight to Dashboard. |
| TP-005 | Feature 1 | P2 | BOUNDARY | BiometricPrompt cancelled by user â€” app does not crash | 1. Enable biometric. 2. Launch app. 3. Tap Cancel on prompt. | App shows fallback handling or re-prompts; no crash, no infinite loop. |
| TP-006 | Feature 1 | P3 | INTERRUPT | Biometric prompt survives device rotation | 1. Enable biometric. 2. Launch app. 3. Rotate while prompt is showing. | BiometricPrompt survives rotation; no crash; auth succeeds/fails as tapped. |

### Feature 2 â€” Biometric toggle

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-007 | Feature 2 | P1 | HAPPY | Toggle biometric ON writes biometric_enabled=true to app_settings | 1. Open Profile. 2. Toggle biometric ON. 3. Close app. 4. Relaunch. | Biometric prompt appears on relaunch (confirmed by TP-002 behavior). |
| TP-008 | Feature 2 | P1 | HAPPY | Toggle biometric OFF writes biometric_enabled=false | 1. Open Profile with biometric currently enabled. 2. Toggle OFF. 3. Relaunch. | No BiometricPrompt on relaunch; Dashboard opens directly. |
| TP-009 | Feature 2 | P2 | BOUNDARY | Toggle persists across data-clear vs. non-data-clear scenarios | 1. Enable biometric. 2. Clear app data. 3. Relaunch. | Settings cleared (first-launch behavior). Toggle state NOT retained without data. |

### Feature 3 â€” WhatsApp OTP login (BYPASSED in normal flow)

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-010 | Feature 3 | P2 | [VERIFY DEGRADATION] | Login screen bypassed â€” verify app goes straight to Dashboard | 1. Install fresh. 2. Launch app. 3. Observe entry point. | App goes straight to Dashboard (via SplashActivity). No blank screen, no crash. |
| TP-011 | Feature 3 | P3 | [VERIFY DEGRADATION] | Verify LoginFragment is not reachable via normal bottom-nav flow | 1. Use app normally. 2. Try to reach LoginFragment from any screen. | LoginFragment not reachable without deep-link / special intent. App stays on functional screens. |

### Feature 4 â€” Google Sign-In (PARTIALLY WORKING)

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-012 | Feature 4 | P2 | HAPPY | Google Sign-In client available and configured | 1. Navigate to Login. 2. Tap Google Sign-In button. | Google account chooser opens. No crash. |
| TP-013 | Feature 4 | P1 | BOUNDARY | Google Sign-In fails/cancels â€” no crash, no NPE | 1. Navigate to Login. 2. Tap Google Sign-In. 3. Cancel/dismiss dialog. | User returned to Login; no crash; no NullPointerException. |
| TP-014 | Feature 4 | P3 | [VERIFY DEGRADATION] | Login screen bypassed â€” verify Google Sign-In still works if manually invoked | 1. Force-close app. 2. Invoke LoginFragment through test/hidden path. 3. Tap Google Sign-In. | Google Sign-In flow launches; on success/error, app remains stable. |

### Feature 5 â€” Auth state persistence

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-015 | Feature 5 | P1 | HAPPY | Auth state (is_logged_in=true) auto-set on cold launch | 1. Launch app for first time. 2. Check auth_prefs (via adb shell). | is_logged_in=true set automatically; app reaches Dashboard. |
| TP-016 | Feature 5 | P1 | HAPPY | Logout clears is_logged_in in auth_prefs | 1. Log in. 2. Navigate to More -> Logout. 3. Verify prefs. | is_logged_in=false in auth_prefs; next launch shows Dashboard (login bypassed) but prefs cleared. |
| TP-017 | Feature 5 | P2 | BOUNDARY | Auth state survives app background/kill cycle | 1. Log in. 2. OS kills app in background. 3. Relaunch. | Auth state consistent; no infinite auth loop. |

---

## SECTION 2 â€” DASHBOARD (Features 6-11)

### Feature 6 â€” Stat cards (Pending/Completed/Profit/Investment/Dues)

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-018 | Feature 6 | P1 | HAPPY | All 6 stat cards display correct initial values on Dashboard load | 1. Seed DB with known entries. 2. Open Dashboard. | Pending count, Completed count, Profit, Investment, Dues all shown correctly. |
| TP-019 | Feature 6 | P1 | HAPPY | Stat cards update after creating new entry and returning to Dashboard | 1. Note Dashboard stats. 2. Create new repair entry. 3. Return to Dashboard. | Stats updated. Pending count +1 (if entry active). |
| TP-020 | Feature 6 | P2 | BOUNDARY | Stat cards display 0 when no data in DB | 1. Fresh install. 2. Open Dashboard. | All stat cards show 0 or appropriate empty-state value. No crash. |
| TP-021 | Feature 6 | P2 | BRANCH | Card labels correct in all locales | 1. Open Dashboard. 2. Verify text for each stat card. | "Pending", "Completed", "Profit", "Investment", "Dues" all correct. |
| TP-022 | Feature 6 | P3 | INTERRUPT | Stat cards reload correctly after orientation change | 1. Open Dashboard with data. 2. Rotate to landscape. | Cards reload with same values. |

### Feature 7 â€” Quick-action navigation cards

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-023 | Feature 7 | P1 | HAPPY | Each card navigates to its correct fragment | 1. Tap Entry -> EntryFragment. 2. Back. 3. Tap Entries -> EntriesListFragment. 4. Back. 5. Tap Quick Sale -> SaleFragment. 6. Quick Sale navigates to SaleFragment. 7. Back. 8. Tap Dues -> DuesFragment. 9. Back. 10. Tap Suppliers -> SupplierListFragment. 11. Back. 12. Tap Reports -> ReportsFragment. 13. Back. 14. Tap More -> MoreFragment. | Each opens its correct destination. No cross-navigation. |
| TP-024 | Feature 7 | P2 | BOUNDARY | Double-tap same card doesn't double-launch fragment | 1. Rapidly double-tap Entry card. | Only one Entry screen instance. No duplicate fragments in back stack. |
| TP-025 | Feature 7 | P3 | BRANCH | Card tap when Dashboard data is empty | 1. Fresh install. 2. Tap each card. | All cards navigate correctly even with no data in DB. |

### Feature 8 â€” Missing profile prompt

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-026 | Feature 8 | P1 | HAPPY | "Update" button shown when shop phone is null | 1. Fresh install (no profile). 2. Open Dashboard. | btnFixMissingInfo visible and clickable. |
| TP-027 | Feature 8 | P1 | HAPPY | "Update" button hidden when shop phone is set | 1. Fill profile including phone. 2. Open Dashboard. | Button hidden. |
| TP-028 | Feature 8 | P2 | HAPPY | Tapping "Update" navigates to ProfileFragment | 1. Show Update button (no phone set). 2. Tap it. | ProfileFragment opens for editing. |

### Feature 9 â€” AI Business Health Advisor

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-029 | Feature 9 | P1 | HAPPY | Advisor score (0-100) displayed when data exists | 1. Create multiple completed jobs. 2. Open Dashboard. 3. Observe advisor section. | Score card with numeric value between 0-100. |
| TP-030 | Feature 9 | P2 | BRANCH | Advisor shows empty/fallback state when no data | 1. Fresh install. 2. Open Dashboard. | Advisor card shows "No data yet" or similar; no crash. |
| TP-031 | Feature 9 | P2 | BOUNDARY | Score always within 0-100 range | 1. Seed extreme data (100+ completed, zero pending). 2. Inspect score. | Score clamped 0-100. |
| TP-032 | Feature 9 | P3 | INTERRUPT | Advisor re-evaluates after creating new job | 1. Note initial score. 2. Complete a job. 3. Return to Dashboard. | Score or smart-move may update. |

### Feature 10 â€” Investment breakdown dialog

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-033 | Feature 10 | P1 | HAPPY | Tapping investment card shows breakdown alert dialog | 1. Open Dashboard. 2. Tap investment stat card. | AlertDialog appears with paid, due, total for today. |
| TP-034 | Feature 10 | P1 | HAPPY | Dialog shows correct paid/due/total values | 1. Record investment with partial payment. 2. Tap card. | Paid = amount paid; Due = owed; Total = sum. |
| TP-035 | Feature 10 | P2 | BOUNDARY | Dialog shows zero values when no investments | 1. Fresh install. 2. Tap investment card. | All three values = 0 or Rs. 0; no crash. |

### Feature 11 â€” Search entries shortcut

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-036 | Feature 11 | P1 | HAPPY | Tapping search icon navigates to EntriesListFragment | 1. Open Dashboard. 2. Tap search icon. | Entries list screen opens with search bar focused. |
| TP-037 | Feature 11 | P2 | BOUNDARY | Tapping search with zero entries shows empty state (not crash) | 1. Fresh install. 2. Tap search icon. | Entries list opens showing empty state, no crash. |

---

## SECTION 8 — PAYROLL (Features 44-47)

### Feature 44 — Payroll overview (Compose)

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-302 | Feature 44 | P1 | HAPPY | Month nav (prev/next) loads correct attendance data | 1. Open Payroll. 2. Note current month. 3. Tap prev/next. | Loads Attendance/SalarySlip data for selected month. |
| TP-303 | Feature 44 | P1 | HAPPY | Summary card shows Total/Paid/Due for current month | 1. Seed attendance + salary slips for month. 2. Open Payroll. | Summary = sum of computed salary amounts. |
| TP-304 | Feature 44 | P1 | HAPPY | Per-technician row: worked days, per-day rate, computed salary, status | 1. Add 2 service men with attendance. 2. Open Payroll. | Each row: name, empId, fullDays, halfDays, perDayRate, computedSalary, status. |
| TP-305 | Feature 44 | P2 | BRANCH | Status badge: PAID/UNPAID/PARTIAL rendered correctly | 1. Create salary with paidAmount = total -> PAID. 2. paidAmount < total -> PARTIAL. 3. paidAmount = 0 -> UNPAID. | Badge color/text matches status. |
| TP-306 | Feature 44 | P2 | BOUNDARY | Zero service men — empty state | 1. Fresh install. 2. Open Payroll. | Empty state message; no crash. |
| TP-307 | Feature 44 | P3 | INTERRUPT | Month retained across rotation | 1. Navigate to specific month. 2. Rotate. | Same month displayed. |

### Feature 45 — Mark attendance

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-308 | Feature 45 | P1 | HAPPY | setAttendance upserts row (composite PK: smId + date) | 1. Call setAttendance(smId, date, present=true, halfDay=false). 2. Verify DB. | Attendance row exists; present=1; halfDay=0. |
| TP-309 | Feature 45 | P2 | BRANCH | Half-day present correctly tracked | 1. setAttendance(smId, date, present=true, halfDay=true). | row.halfDay=1. |
| TP-310 | Feature 45 | P2 | HAPPY | Month stats refresh after attendance change | 1. Mark attendance for all month days. 2. Open Payroll month view. | fullPresentDays = count where present=1 AND halfDay=0; halfPresentDays = count where present=1 AND halfDay=1. |

### Feature 46 — Generate / update salary slip

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-311 | Feature 46 | P1 | HAPPY | Salary slip created with computed salary | 1. setAttendance for a tech. 2. generateOrUpdateSalary(paidAmount=0). | SalaryPayment row: computedAmount = PayrollMath result; dueAmount = computedAmount; status = UNPAID. |
| TP-312 | Feature 46 | P1 | HAPPY | Paid salary creates PaymentTransaction (SALARY) | 1. generateOrUpdateSalary(smId, paidAmount=5000). | SalaryPayment.status = PAID; PaymentTransaction row inserted with personType=SALARY. |
| TP-313 | Feature 46 | P2 | BRANCH | Re-running updates existing slip (not duplicate) | 1. generateOrUpdateSalary(...). 2. Call again with different paidAmount. | Single row per smId+month; values updated. |

### Feature 47 — Salary math engine

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-314 | Feature 47 | P1 | HAPPY | computePayable uses perDaySalary priority | 1. monthlySalary=30000, perDaySalary=1200, days=25. | effectivePerDay = 1200; payable = 30000. |
| TP-315 | Feature 47 | P2 | BRANCH | Falls back to monthlySalary/30 when perDaySalary = 0/null | 1. monthlySalary=30000, perDaySalary=0, days=25. | effectivePerDay = 1000; payable = 25000. |
| TP-316 | Feature 47 | P2 | BOUNDARY | computeWorkedDays = full + half * 0.5 | 1. 20 full + 4 half. | = 22. |
| TP-317 | Feature 47 | P2 | BOUNDARY | No negative salary ever | 1. Set 0 days, 0 paid. | computedAmount >= 0 always. |

---

## SECTION 9 — EXPENSES (Features 48-49)

### Feature 48 — Record expense

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-318 | Feature 48 | P1 | HAPPY | Expense recorded with category, title, amount, recurring flag | 1. Open Expenses. 2. FAB -> Add. 3. Category=Rent, title=Shop Rent, amount=15000, recurring=true, paid=true. | Expense row inserted. |
| TP-319 | Feature 48 | P1 | HAPPY | Paid expense creates PaymentTransaction (EXPENSE, SHOP) | 1. Add expense with paid=true. | Expense row + PaymentTransaction(personType=EXPENSE, personMobile=SHOP) in single DB transaction. |
| TP-320 | Feature 48 | P2 | BRANCH | Unpaid expense — no PaymentTransaction | 1. Add expense with paid=false. | Only Expense row; no PaymentTransaction. |
| TP-321 | Feature 48 | P2 | BOUNDARY | All 5 categories accepted (Rent, Electricity, Internet, Supplies, Other) | 1. Create one expense per category. | All 5 rows inserted; category stored correctly. |
| TP-322 | Feature 48 | P2 | HAPPY | Delete removes expense row | 1. Add expense. 2. Tap delete. | Row removed from DB. |

### Feature 49 — Expense accounting

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-323 | Feature 49 | P1 | HAPPY | Monthly total = SUM(amount) for expenses in date range | 1. Add 3 expenses totaling Rs. 5000 in month. 2. Check month total. | Total = 5000. |
| TP-324 | Feature 49 | P2 | HAPPY | Category breakdown accurate | 1. Add 2 Rent (10000) + 1 Electricity (2000). 2. Check breakdown. | Rent = 10000, Electricity = 2000. |
| TP-325 | Feature 49 | P3 | INTERRUPT | Month nav survives rotation | 1. Navigate to specific month. 2. Rotate. | Same month retained. |

---

## SECTION 10 — ENTRIES LIST (Features 50-51)

### Feature 50 — Search all entries

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-326 | Feature 50 | P1 | HAPPY | TextWatcher search filters entries by customerName/mobile/dealerName | 1. Create 3 entries. 2. Search by customer name. | Only matching entries shown. |
| TP-327 | Feature 50 | P2 | BOUNDARY | Empty search shows all entries | 1. Clear search field. | All entries listed. |
| TP-328 | Feature 50 | P2 | BRANCH | Partial mobile match works | 1. Search by first 5 digits of mobile. | Matching entry shown. |

### Feature 51 — View all entries

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-329 | Feature 51 | P1 | HAPPY | getAllEntries shows all repair entries | 1. Create 5 entries. 2. Open Entries list. | All 5 listed. |
| TP-330 | Feature 51 | P1 | HAPPY | Entry tap -> EntryDetailFragment with all data | 1. Tap any entry. | EntryDetail shows photos, fault, charge, status. |

---

## SECTION 11 — PROFILE / SETTINGS (Features 52-55)

### Feature 52 — Shop profile CRUD

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-331 | Feature 52 | P1 | HAPPY | Save profile updates UserProfile (id=1) | 1. Fill all fields. 2. Save. | UserProfile row updated in DB; fields persist. |
| TP-332 | Feature 52 | P2 | BOUNDARY | Phone empty -> missing-info card shown on Dashboard | 1. Save profile with no phone. 2. Open Dashboard. | btnFixMissingInfo visible. |

### Feature 53 — Profile photo

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-333 | Feature 53 | P2 | HAPPY | Camera path stores photo to UserProfile | 1. Tap profile photo -> Camera. 2. Capture. | Photo path stored; displayed in profile view. |
| TP-334 | Feature 53 | P2 | HAPPY | Gallery path picks existing image | 1. Tap profile photo -> Gallery. | Image stored; displayed. |

### Feature 54 — Link Google Account

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-335 | Feature 54 | P2 | HAPPY | Google Sign-In opens account chooser | 1. Tap Link Google. | Google account picker opens. |
| TP-336 | Feature 54 | P2 | BOUNDARY | Google Sign-In cancel -> ProfileFragment unchanged | 1. Tap Link Google. 2. Cancel. | No fields modified; no crash. |
| TP-337 | Feature 54 | P3 | [PARTIALLY WORKING] | On success, email/name filled | 1. Complete Google Sign-In. | Profile email/name auto-populated. |

### Feature 55 — Last sync timestamp

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-338 | Feature 55 | P2 | HAPPY | lastSyncTimestamp displayed | 1. Empty DB profile. 2. Open Profile. | "Last sync: never" or timestamp shown. |
| TP-339 | Feature 55 | P3 | HAPPY | Timestamp updates after sync action | 1. Tap Sync Now. 2. Observe timestamp. | Timestamp updated to now. |

---

## SECTION 12 — BACKUP & RESTORE (Features 56-59)

### Feature 56 — Export local backup

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-340 | Feature 56 | P1 | HAPPY | exportLocally() copies DB to Downloads folder with timestamp | 1. Tap More -> Export. 2. Choose "Save to Downloads". | DB file MuZZu_Tech_Backup_{timestamp}.db appears in Downloads. |
| TP-341 | Feature 56 | P1 | HAPPY | Backup file is non-zero size and valid SQLite | 1. Export. 2. Verify file size > 0. 3. Open with SQLite tool. | File opens; tables present. |

### Feature 57 — Share backup file

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-342 | Feature 57 | P2 | HAPPY | shareBackup() copies DB to cache and fires ACTION_SEND | 1. Tap Share. 2. Observe Android share sheet. | Share sheet opens with pplication/octet-stream MIME and backup file attached. |

### Feature 58 — Restore local backup

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-343 | Feature 58 | P1 | HAPPY | importDatabase() replaces current DB from backup | 1. Export current DB. 2. Delete some data. 3. Restore from exported file. | Data restored; database file replaced successfully. |
| TP-344 | Feature 58 | P1 | BOUNDARY | Confirmation dialog shown before restore (data-loss warning) | 1. Tap Restore. | Dialog: "This will replace ALL current data" with Cancel/Confirm. |
| TP-345 | Feature 58 | P2 | HAPPY | Post-restore: singleton reset, app restarts cleanly | 1. Restore backup. 2. Open app. | App launches; DB consistent; no crash. |

### Feature 59 — Cloud sync (Google Drive) [STUB]

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-346 | Feature 59 | P1 | [VERIFY DEGRADATION] | syncWithGoogleDrive() is a STUB — verify no crash, updates timestamp/toast only | 1. Tap Sync Now in Profile. 2. Check DB. | Toast "Google Drive Sync initiated" shown; lastSyncTimestamp updated; NO actual Drive upload. |
| TP-347 | Feature 59 | P2 | [VERIFY DEGRADATION] | No Drive API client initialized | 1. Call syncWithGoogleDrive with network OFF. | Fails or shows error locally; no Drive API exception. |

---

## SECTION 13 — AUTO-UPDATE (Feature 60)

### Feature 60 — GitHub version check + APK download

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-348 | Feature 60 | P1 | HAPPY | Startup checkForUpdates() fetches version.json from GitHub | 1. Launch app with internet. 2. Observe check. | No crash; version.json parsed. |
| TP-349 | Feature 60 | P2 | BRANCH | Newer version available -> dialog shown | 1. Mock GitHub version.json returning higher versionCode. 2. Launch. | Dialog: "Update available" with version info and release notes. |
| TP-350 | Feature 60 | P2 | BRANCH | No update -> silent skip | 1. Set GitHub versionCode <= current. 2. Launch. | No dialog; app proceeds normally. |
| TP-351 | Feature 60 | P2 | BOUNDARY | Download -> progress dialog -> installer intent | 1. Tap Download on update dialog. | Progress dialog shown -> installer intent opened. |
| TP-352 | Feature 60 | P3 | INTERRUPT | Update check interrupted mid-download | 1. Start download. 2. Kill app. 3. Relaunch. | App does not corrupt state; re-checks for updates on relaunch. |

---

## SECTION 14 — NOTIFICATIONS (Features 61-65)

### Feature 61 — WhatsApp repair started

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-353 | Feature 61 | P2 | HAPPY | After Inspection save -> WhatsApp intent fired with "Repair Started" | 1. Complete Step 1, save Inspection. | wa.me / ACTION_VIEW intent launched. |

### Feature 62 — WhatsApp repair completed [NOT WIRED]

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-354 | Feature 62 | P1 | [NOT WIRED] | Verify sendRepairCompletedWhatsApp() is NOT called in any UI path | 1. Search all call sites for sendRepairCompletedWhatsApp. | Zero call sites in production code (only handover-summary notification used). |
| TP-355 | Feature 62 | P2 | [VERIFY DEGRADATION] | Calling function directly does not crash | 1. Call via adb/test. | Toasts/returns gracefully; no crash. |

### Feature 63 — WhatsApp device collected

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-356 | Feature 63 | P2 | HAPPY | After completeHandover() -> WhatsApp intent with parts list | 1. Complete full repair pipeline. 2. Check wa.me URL. | Message contains parts list + total amount. |

### Feature 64 — Reorder alert notification

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-357 | Feature 64 | P2 | HAPPY | ReorderAlertWorker posts notification when parts below reorder threshold | 1. Seed spare parts with usage > stock. 2. Trigger worker manually. | Notification on channel reorder_alerts listing parts to reorder. |
| TP-358 | Feature 64 | P2 | BOUNDARY | No parts to reorder -> no notification | 1. Clear all purchases. 2. Trigger worker. | No notification posted. |

### Feature 65 — Salary reminder notification

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-359 | Feature 65 | P2 | HAPPY | SalaryReminderWorker fires on 1st of month triggering unpaid slips list | 1. Seed unpaid salary slips. 2. Trigger worker. | Notification on salary_reminders channel with technician names + dues. |
| TP-360 | Feature 65 | P2 | BOUNDARY | No unpaid slips -> no notification | 1. Mark all salaries PAID. 2. Trigger worker. | No notification. |

---

## SECTION 15 — AI FEATURES (Features 66-70)

### Feature 66 — AI photo fault detection

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-361 | Feature 66 | P1 | HAPPY | suggestFaultsFromPhoto() detects fault from ML Kit + brightness heuristic | 1. Provide known-fault phone image. 2. Run analysis. | Fault suggestions returned (e.g., "Broken Screen","Liquid Damage"). |
| TP-362 | Feature 66 | P2 | BRANCH | Brightness heuristic exercised independently | 1. Provide dark/overbright image. 2. Run. | Brightness warning suggestions included. |

### Feature 67 — AI repair cost estimator

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-363 | Feature 67 | P2 | HAPPY | estimateRepairCost returns matching CommonFault defaultCharge | 1. Call with "Display Replacement". 2. Check return. | Returns defaultCharge from matching CommonFault row. |

### Feature 68 — AI repair time estimator

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-364 | Feature 68 | P2 | HAPPY | estimateRepairTime returns correct day estimates | 1. "Display" -> 2. 2. "Battery" -> 1. 3. "Motherboard" -> 5. | Matches documented mapping. |

### Feature 69 — AI business health advisor

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-365 | Feature 69 | P1 | HAPPY | analyzeDailyHealth returns score 0-100 and smartMove text | 1. Seed jobs with varying margins. 2. Call. | score in 0..100; smartMove non-empty string. |
| TP-366 | Feature 69 | P2 | BOUNDARY | 0% margin -> score = 30 | 1. Breakeven jobs (expenses = revenue). 2. Call. | score = 30. |

### Feature 70 — AI repair trends analysis [NOT WIRED]

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-367 | Feature 70 | P1 | [NOT WIRED] | Verify analyzeRepairTrends() is NOT called from any production UI | 1. Grep codebase for analyzeRepairTrends callsites. | Zero UI call sites. |
| TP-368 | Feature 70 | P2 | [VERIFY DEGRADATION] | Function call returns valid data structure | 1. Call directly with empty list. | Returns TrendsAnalysis object with zero values; no crash. |

---

## SECTION 16 — BACKGROUND / AUTOMATED JOBS (Features 71-72)

### Feature 71 — Reorder alert

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-369 | Feature 71 | P1 | HAPPY | ReorderAlertWorker runs every 24h, posts notification when parts need reorder | 1. Enqueue worker manually. 2. Observe notification. | Notification on reorder_alerts channel. |
| TP-370 | Feature 71 | P2 | BOUNDARY | No purchases -> no notification | 1. Fresh DB. 2. Enqueue worker. | No notification posted. |

### Feature 72 — Salary reminder

| ID | Feature# | Priority | Type | Description | Test Steps | Expected Result |
|---|---|---|---|---|---|---|
| TP-371 | Feature 72 | P1 | HAPPY | SalaryReminderWorker posts notification for unpaid salaries | 1. Seed unpaid salary slips. 2. Enqueue worker. | Notification on salary_reminders channel. |
| TP-372 | Feature 72 | P2 | BOUNDARY | All salaries PAID -> no notification | 1. Seed all paid. 2. Enqueue worker. | No notification. |

---

## SECTION 17 — PHASE 2: FINANCIAL INTERLINKING (9 Chains)

### Chain 1 — Supplier -> Inventory -> Repair -> Handover -> Revenue/COGS/Due

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-400 | P1 | Complete chain: Add supplier, buy parts (pay-later+paid), create repair entry using those parts, complete handover. Verify: Supplier due = total unpaid parts. COGS = sum of purchasePrice*qty. Revenue = sum of finalAmount. | SQL fresh sums match cached values within Rs. 0.01 |
| TP-401 | P1 | [Same chain] Partially paid supplier: pay Rs. 500 of Rs. 2000 due. Verify partial payment reflected in Supplier ledger balance. | Balance recalculates = 1500. |
| TP-402 | P2 | Handover with split payment (Cash+Online) -> Supplier still unpaid. Verify Cash-in (CUSTOMER) and Online-in both recorded. | 2 PaymentTransactions added; total = finalAmount. |

### Chain 2 — Customer -> Repair -> Advance -> Cancel -> Part Return -> Refund

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-403 | P1 | Full chain: Customer repair (advance Rs. 200), cancel before handover, return part, refund. Verify Wallet/Due = advance - refund. | Due recalculates to 0. |
| TP-404 | P1 | Cancel -> No handover -> revenue record correctly excluded. | Reports revenue = 0 for cancelled entry. |

### Chain 3 — Direct Sale -> Supplier Payment -> Cash In/Out -> Dues + Reports

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-405 | P1 | Direct Sale: purchase Rs.300, sale Rs.800. Verify: Cash-in = rs.800, Cash-out = rs.300, Supplier Payment created, Reports profit = rs.500. | All three records exist; profit = 500. |

### Chain 4 — Service Man -> Attendance -> Salary -> Payroll totals

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-406 | P1 | Multi-month attendance (5 men), varying full/half/present/absent days. Verify salary slips match PayrollMath exactly and no NaN/negative output. | All computedAmounts >= 0; no NaN. |
| TP-407 | P1 | [Same chain] SalaryReminderWorker fires only for genuinely unpaid slips. | Notification lists exactly slips with status != PAID. |

### Chain 5 — Expenses + Rent/recurring + Salary expense in Dashboard profit

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-408 | P1 | Add recurring Rent (Rs.10000), Electricity (Rs.3000), SalaryPayment (Rs.15000). Dashboard "Today's Profit" must subtract ALL three. | Profit = Revenue - PartsInvest - Rent - Elec - Salary. |
| TP-409 | P1 | Unpaid salary NOT subtracted from Dashboard profit (only paid transactions affect cash). | Dashboard profit unchanged for unpaid salary. |

### Chain 6 — Supplier partial payment + Part return credit -> Due recalculates

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-410 | P1 | Supplier total due Rs.4000. Pay Rs.1500. Then return parts worth Rs.800. Verify Due = 4000 - 1500 - 800 = rs.1700. | Supplier ledger balance = 1700. |

### Chain 7 — Dealer flow parity with Customer/Supplier

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-411 | P2 | Dealer: repair entry + payment + handover. Verify same payment pathway as Customer (CUSTOMER-type Payment + PaymentTransaction). | Dealer payment behaves identically to Customer. |
| TP-412 | P2 | Dealer-ledger view (via CustomerDetailFragment with isDealer=true) shows correct totals. | Ledger consistent. |

### Chain 8 — Multi-technician month (5 men, mixed types)

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-413 | P1 | 5 service men, random attendance mix. Generate all salary slips. Verify: no negative computedAmount, no NaN, total salaries = SUM. | Total >= 0. |
| TP-414 | P1 | Mix of monthlySalary vs perDaySalary techs. Both math paths exercised. | Both produce same format SalaryPayment. No crashes. |

### Chain 9 — "Worst day" stress test

| ID | Priority | Description | Reconciliation |
|---|---|---|---|
| TP-415 | P1 | 20 repairs + 10 sales + 3 deliveries + 2 payroll entries + 4 expenses + 2 refunds + 1 return, all interleaved in same day. End-of-day reconciliation: Inventory sum = 0, Revenue = sum, COGS = sum, SupplierDue = sum, ExpenseSum = sum, NetProfit = Revenue - COGS - ExpenseSum. | All sums agree to Rs. 0.01. |

---

## SECTION 18 — PHASE 3: ADVERSARIAL & CONCURRENCY

### Rapid double-tap on every submit button

| ID | Priority | Screen | Description | Expected |
|---|---|---|---|---|
| TP-420 | P1 | All 28 screens | Rapid double-tap on Save/Submit button | No duplicate DB rows; guard flag prevents second insert. |
| TP-421 | P2 | EntryFragment, HandoverFragment, PayDuesFragment, ExpenseDialog, ServiceManAddFragment | Triple-tap (3 rapid taps) | Same: only 1 DB row per transaction. |
| TP-422 | P2 | SparePartsFragment | Double-tap "Add Part" | Only one Part row created. |

### Force-kill mid-transaction (10+ code points)

| ID | Priority | Transaction Point | Method | Expected DB State |
|---|---|---|---|---|
| TP-423 | P1 | EntryViewModel.saveEntry() mid-insert | adb shell am kill | No orphan RepairEntry or Customer/Dealer rows; DB consistent. |
| TP-424 | P1 | HandoverViewModel.completeHandover() mid-transaction | adb shell am kill | No orphan Payment or PaymentTransaction; entry not marked complete. |
| TP-425 | P1 | SparePartsViewModel.addPart() mid-transaction | adb shell am kill | No orphan SparePartPurchase/Payment/Transaction rows. |
| TP-426 | P1 | SaleViewModel.saveSale() mid-transaction | adb shell am kill | No partial Sale/Payment/Transaction rows. |
| TP-427 | P1 | DuesViewModel.recordPayment() mid-transaction | adb shell am kill | Payment not updated; no orphan transaction row. |
| TP-428 | P2 | PayrollViewModel.generateOrUpdateSalary() mid-transaction | adb shell am kill | SalaryPayment not inserted; no orphan PaymentTransaction. |
| TP-429 | P2 | ExpensesViewModel.addExpense() mid-transaction | adb shell am kill | Expense not inserted; no orphan transaction. |
| TP-430 | P2 | BackupManager.importDatabase() mid-stream | adb shell am kill | DB file either old or complete; no zero-byte corrupted file. |
| TP-431 | P2 | UpdateManager.downloadApk() mid-download | adb shell am kill | No partial APK in external files; next launch re-checks version. |

### Coroutine concurrency — shared-row race

| ID | Priority | Description | Expected |
|---|---|---|---|
| TP-432 | P1 | Concurrent Dashboard reload + new entry save | Dashboard stats updated after transaction commits; no stale read. |
| TP-433 | P2 | Concurrent payroll month switch + attendance mark | Payroll reflects latest attendance after both complete. |
| TP-434 | P2 | Concurrent dues summary + payment record | Dues summary updated after payment commit. |

---

## SECTION 19 — PHASE 4: UI BOT WALKTHROUGH

| ID | Priority | Description | Expected |
|---|---|---|---|
| TP-450 | P1 | Full walkthrough: Splash -> Dashboard -> all 13 screens via TestLauncher | No crash on any screen; all screens render completely within 6s. |
| TP-451 | P2 | Screenshot audit: verify all screens show non-placeholder content (no blank RecyclerViews where data should exist) | All screens with seeded DB show populated content. |
| TP-452 | P2 | Navigation back-stack integrity: Dashboard -> Entry -> back -> Entries -> back -> Reports | No crash on back press; stack depth correct. |
| TP-453 | P2 | Bottom-nav active indicator matches current screen | Active tab highlighted; other tabs not highlighted. |
| TP-454 | P3 | Toolbar title correct per destination | Toolbar shows screen name matching current fragment. |

---

## SECTION 20 — PHASE 5: SECURITY RE-VERIFICATION

| ID | Priority | Gap | Description | Expected |
|---|---|---|---|---|
| TP-460 | P1 | #8 (login bypassed) | Verify LoginFragment still functional but bypassed in normal flow | Login works when directly invoked; normal flow skips it. |
| TP-461 | P1 | #6 (fake cloud sync) | Verify syncWithGoogleDrive() does NOT call Drive REST API | No GoogleApiClient or Drive API call in syncWithGoogleDrive method body. |
| TP-462 | P1 | #5 (unused real OTP) | Verify OtpManager.sendOtp() not called from any production path | Zero call sites for OtpManager.sendOtp. WhatsApp OTP is used instead. |
| TP-463 | P2 | FileProvider scope | Verify FileProvider paths restricted to app-specific directories | No global external storage FileProvider path. |
| TP-464 | P2 | Backup file share MIME type | Verify export uses application/octet-stream (not text/plain) | Intent MIME = application/octet-stream. |
| TP-465 | P3 | CAMERA permission runtime gating | Verify camera requested at runtime per Android 13+ rules | No-timeout crash on Camerapath; graceful denial message shown. |

---

## SECTION 21 — PHASE 6: EXECUTION MATRIX

| Batch | Scenarios | Priority Focus | Commit Trigger |
|---|---|---|---|
| 6-A | TP-001 to TP-200 | Auth/Dashboard/Repair/Dues/Reports — P1 only | Commits: crash regressions; fails = stop and fix |
| 6-B | TP-201 to TP-368 | Master/Payroll/Expenses/Profile/Backup/AI/Notifications — P1+P2 | Commits: new features; 50-scenario green batch = commit |
| 6-C | TP-369 to TP-445 | Phase 2-3 financial chains and concurrency | Commits: one per Phase 2 chain once reconciled |
| 6-D | TP-446 to TP-520 | Phase 4-5 UI bot + security | Commits: verification + security findings |
| 6-E | TP-521+ | Regression re-run of all P1s after each bug fix | Commits: regression test additions |

---

## SECTION 22 — PHASE 7: FINAL REPORT TEMPLATE

FULL_COVERAGE_REPORT.md will contain:
1. Coverage table — all 71 features mapped to: TESTED/PASSED, TESTED/FIXED, FLAGGED, STUB, NOT WIRED
2. 9 financial chain reconciliation results
3. Total scenarios executed / pass count
4. Bug table: all found bugs with fix status
5. Known-gap status table
6. Honest verdict on account correctness
