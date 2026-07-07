# A-to-Z App Survey & Audit Report

**Project:** MobileRepairShop / MuZZu Tech  
**Date:** 2026-07-07  
**Scope:** Build config, manifest, security, database, backup/restore, update flow, finance ledger, UI crash risks, background work, Play Store readiness.

---

## Executive Summary

The app has improved significantly after the V6/V7 fixes, but it is **not yet production-safe**. The largest remaining risks are not cosmetic; they can cause data loss, broken update checks, Play Store exposure, and incorrect dashboard values.

**Overall grade:** B- for internal/sideload use, C for Play Store readiness.

**Top blockers before release:**
1. `PlayUpdateHelper.tryImmediateUpdate()` returns `true` even when no Play update exists, blocking GitHub fallback updates.
2. SQLCipher migration currently deletes the existing database and only caches a backup, causing silent production data loss.
3. `fallbackToDestructiveMigration()` can wipe user data on future schema mismatches.
4. `google-services.json` is still tracked in git even though `.gitignore` now ignores future additions.
5. `TestLauncherActivity` is exported in production and can be invoked externally.
6. Dashboard profit still double-counts actual cash collections in some flows.

---

## P0 Findings

### AZ-01 [P0] Play update helper blocks fallback update flow

**Location:** `app/src/main/java/com/app/muzzutech/utils/update/PlayUpdateHelper.kt:15`  
**Location:** `app/src/main/java/com/app/muzzutech/utils/UpdateManager.kt:41`

`tryImmediateUpdate()` returns `true` immediately after attaching an async listener, even when:
- Play Store is unavailable.
- No update exists.
- The app is sideloaded.
- `appUpdateInfo` succeeds with `UPDATE_NOT_AVAILABLE`.

Because `UpdateManager.checkForUpdates()` returns when this method returns `true`, the GitHub/version.json fallback never runs in normal cases.

**Impact:** Update checks can silently stop working for sideload users and for Play users with no immediate update available.

**Fix:** Make `tryImmediateUpdate()` callback-based/suspend and return `true` only when `startUpdateFlowForResult()` is actually called. Otherwise continue fallback check.

---

### AZ-02 [P0] SQLCipher migration deletes existing user database

**Location:** `app/src/main/java/com/app/muzzutech/data/db/AppDatabase.kt:326`

`migrateToEncrypted()` currently:
1. Copies the existing plain SQLite DB to `cacheDir/legacy_db_backup.db`.
2. Deletes the live database.
3. Lets Room create a fresh encrypted DB.

It does **not** import existing records into SQLCipher.

**Impact:** Existing production users lose all shop data after upgrading to this build. The backup is stored in cache, which Android may clear and users cannot restore from easily.

**Fix:** Use SQLCipher export migration or a staged app-level migration that reads old Room data and writes into encrypted Room before deleting the original. Do not delete the live DB until verification succeeds.

---

## P1 Findings

### AZ-03 [P1] Destructive migration can wipe the shop database

**Location:** `app/src/main/java/com/app/muzzutech/data/db/AppDatabase.kt:305`

`fallbackToDestructiveMigration()` remains enabled.

**Impact:** Any missing/invalid Room migration in a future release can drop all repair, dues, customer, supplier, payroll, and ledger data.

**Fix:** Remove `fallbackToDestructiveMigration()` for release builds. Add explicit migrations for every schema bump and run migration tests.

---

### AZ-04 [P1] `google-services.json` is still tracked in git

**Location:** `app/google-services.json`

`.gitignore` now contains `google-services.json`, but the file is already tracked. `git ls-files app/google-services.json` confirms it is still in the repository.

**Impact:** Firebase API key, OAuth client IDs, project number, and project metadata remain exposed in git history and current source.

**Fix:** `git rm --cached app/google-services.json`, rotate/restrict Firebase/OAuth credentials in Google Cloud, and inject config through environment or private build files.

---

### AZ-05 [P1] Exported `TestLauncherActivity` ships in production

**Location:** `app/src/main/AndroidManifest.xml:41`  
**Location:** `app/src/main/java/com/app/muzzutech/TestLauncherActivity.kt:7`

The activity is exported and browsable via `${applicationId}://test-launcher`. It can deep-link into internal screens.

**Impact:** External apps can trigger navigation paths intended for testing. This increases attack surface and may bypass intended app flow.

**Fix:** Move it to debug-only manifest/source set or set `android:exported="false"` and remove intent filters in release.

---

### AZ-06 [P1] Release build signs with debug keystore

**Location:** `app/build.gradle:68`

The release block uses `signingConfig signingConfigs.debug`.

**Impact:** Release artifacts are signed with a debug key, unsuitable for Play Store, app integrity, and long-term upgrade trust.

**Fix:** Add a real release signing config loaded from secure env/local properties. Never publish debug-signed releases.

---

### AZ-07 [P1] Dashboard profit can double-count cash collection

**Location:** `app/src/main/java/com/app/muzzutech/ui/dashboard/DashboardViewModel.kt:129`

The dashboard revenue equation includes both `handoverRevenue` (`finalAmount`) and linked `dueCollections`. On the handover day, cash/online handover payment transactions are linked to the same `Payment`, so the dashboard can count both the invoice total and the payment received.

**Impact:** Daily profit can be inflated, especially when handover is paid immediately.

**Fix:** Decide one accounting mode:
- Accrual: revenue = invoices/handovers/sales only; cash collections affect cashflow, not profit.
- Cash basis: revenue = actual cash-in transactions only; do not add invoice totals.

---

### AZ-08 [P1] Backup restore validation uses plain SQLite only

**Location:** `app/src/main/java/com/app/muzzutech/utils/BackupManager.kt:100`

Restore integrity check uses `android.database.sqlite.SQLiteDatabase.openDatabase()`. After SQLCipher, encrypted backups cannot be opened by plain SQLite.

**Impact:** Valid encrypted backups may be rejected, or plaintext restore paths can conflict with encrypted live DB expectations.

**Fix:** Validate using the same SQLCipher/Room configuration and passphrase as production. Also validate expected schema tables/version.

---

## P2 Findings

### AZ-09 [P2] App update dialog still references install-package permission flow

**Location:** `app/src/main/java/com/app/muzzutech/ui/update/UpdateBottomSheet.kt:73`

`installPermissionLauncher` and `canRequestPackageInstalls()` remain even though `REQUEST_INSTALL_PACKAGES` was removed.

**Impact:** Dead/incorrect sideload update UX remains. On Android O+, sideload installation may fail because the app no longer has install-package permission.

**Fix:** If Play Store only: remove APK install UI entirely. If sideload still required: use product flavors, with install permission only in non-Play builds.

---

### AZ-10 [P2] WorkManager observer is never removed

**Location:** `app/src/main/java/com/app/muzzutech/utils/UpdateManager.kt:162`

`observeForever` is used without removing the observer after the work finishes.

**Impact:** Potential memory leak and duplicate callbacks after repeated update attempts.

**Fix:** Store the observer and call `removeObserver()` when `info.state.isFinished`.

---

### AZ-11 [P2] Client-side WhatsApp OTP is not real authentication

**Location:** `app/src/main/java/com/app/muzzutech/utils/WhatsAppOtpUtil.kt:10`  
**Location:** `app/src/main/java/com/app/muzzutech/ui/auth/LoginFragment.kt:75`

OTP is generated locally and validated against a static in-memory variable.

**Impact:** This is not server-side verification. Reverse engineering or instrumentation can bypass it.

**Fix:** Use `OtpManager` server verification or Firebase Auth. Remove local OTP validation from login.

---

### AZ-12 [P2] `OtpManager` exists but is unused by login

**Location:** `app/src/main/java/com/app/muzzutech/utils/OtpManager.kt:15`

There is a proper API-based OTP manager, but the login UI uses `WhatsAppOtpUtil` instead.

**Impact:** Dead security code and misleading authentication posture.

**Fix:** Route login through `OtpManager.sendOtp()` and `OtpManager.verifyOtp()`; keep WhatsApp only as a notification channel if needed.

---

### AZ-13 [P2] Release hard-fails if OTP properties are missing, including non-auth builds

**Location:** `app/build.gradle:29`

The Gradle build fails when OTP properties are absent.

**Impact:** CI/release builds fail unless secrets are present even for builds that may not use OTP. This is secure but brittle.

**Fix:** Keep strict release checks, but allow debug/test builds to use safe fake values or variant-specific secrets.

---

### AZ-14 [P2] App uses deprecated Play update API

**Location:** `app/src/main/java/com/app/muzzutech/utils/update/PlayUpdateHelper.kt:22`  
**Location:** `app/src/main/java/com/app/muzzutech/MainActivity.kt:150`

`startUpdateFlowForResult()` is deprecated.

**Impact:** Not a current blocker, but it creates future SDK maintenance risk.

**Fix:** Move to `startUpdateFlowForResult()` replacement using `ActivityResultLauncher` / current Play Core flow APIs.

---

### AZ-15 [P2] Auto-save can create drafts during navigation/pause unexpectedly

**Location:** `app/src/main/java/com/app/muzzutech/ui/entry/EntryFragment.kt:246`

`onPause()` auto-saves drafts whenever mobile length is >= 4.

**Impact:** Can create partial records when user backs out, switches tabs, or changes screens unintentionally.

**Fix:** Track dirty state and only auto-save after explicit user intent or confirmed draft mode.

---

## P3 Findings

### AZ-16 [P3] `!!` patterns remain in fragments

**Location:** `app/src/main/java/com/app/muzzutech/ui/quotation/QuotationFragment.kt:83`  
**Location:** `app/src/main/java/com/app/muzzutech/ui/entry/EntryFragment.kt:177`

Most binding `!!` usages are common but several data dereferences can crash if state changes.

**Impact:** Intermittent crashes during navigation, deleted records, or configuration changes.

**Fix:** Replace critical data `!!` with null-safe early returns and user-visible error messages.

---

### AZ-17 [P3] Logs expose business/update internals

**Location:** `app/src/main/java/com/app/muzzutech/utils/UpdateManager.kt:51`  
**Location:** `app/src/main/java/com/app/muzzutech/ui/entry/EntryFragment.kt:83`

Debug logs include update metadata and local photo paths.

**Impact:** Minor privacy/debug exposure in production logs.

**Fix:** Wrap verbose logs in `if (BuildConfig.DEBUG)` or remove production logs.

---

### AZ-18 [P3] AppScheduler formatting/structure is hard to maintain

**Location:** `app/src/main/java/com/app/muzzutech/work/AppScheduler.kt:23`

Indentation is inconsistent and multiple scheduler blocks are mixed in one method.

**Impact:** Low runtime risk but increases maintenance mistakes.

**Fix:** Split into `enqueueReorderWorker`, `enqueueSalaryWorker`, `enqueueUpdateWorker`, and `enqueueLedgerAuditWorker`.

---

## What Looks Good

- Release minification and resource shrinking are enabled.
- Dangerous storage/media permissions were removed.
- `allowBackup=false` and `usesCleartextTraffic=false` are set.
- Dues payment update is atomic and guarded against overpayment.
- Handover cancellation creates a refund transaction for advances.
- ProGuard blanket keep rule was removed.
- WorkManager is present for background jobs and update downloads.
- Financial amounts are mostly represented as `Long` paise.

---

## Recommended Fix Order

1. **AZ-01:** Fix Play update helper return behavior.
2. **AZ-02:** Replace destructive SQLCipher migration with real data-preserving migration.
3. **AZ-03:** Remove `fallbackToDestructiveMigration()` in release.
4. **AZ-04:** Untrack and rotate/restrict `google-services.json` credentials.
5. **AZ-05:** Remove exported `TestLauncherActivity` from release.
6. **AZ-06:** Add secure release signing config.
7. **AZ-07:** Choose accrual vs cash-basis dashboard accounting and fix double count.
8. **AZ-08:** Make backup restore SQLCipher-aware.
9. **AZ-10:** Remove WorkManager `observeForever` leak.
10. **AZ-11/AZ-12:** Replace local WhatsApp OTP auth with server OTP verification.

---

## Verdict

The app is close, but the remaining issues include **two production data-loss risks** and **one update-flow regression**. Fix P0/P1 before any real customer rollout or Play Store submission.
