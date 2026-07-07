# V7 Security, Privacy & Production-Readiness Audit

**Project:** MobileRepairShop (MuZZu Tech Repair Shop)  
**Reviewer:** Principal Mobile Security Engineer / Google Play Store Reviewer  
**Date:** 2026-07-07  
**Severity:** P0 (reject) → P1 (critical) → P2 (major) → P3 (minor)

---

## 1. Data at Rest — Local Storage Security

### V7-S01 [P1] — Room database stores all customer PII + financial data in plain-text SQLite

**Location:** `AppDatabase.kt:289-302`, `MobileRepairShop` database file at `/data/data/com.app.muzzutech/databases/mobile_repair_shop_db`

**Vulnerability:** The entire Room database contains:
- Customer phone numbers (`customerMobile`, `dealerMobile`, `personMobile`)
- Customer names, addresses (`customerCity`)
- Full financial ledger (`finalAmount`, `paidAmount`, `dueAmount`, `chargeAmount`, `advanceAmount`, etc.)
- Supplier and dealer contact details
- Service man salary data

This data is stored in plain-text SQLite. On a rooted device or via `adb backup` (if backup were enabled), any app or user with root access can dump the entire database. The `android:allowBackup="false"` flag in the manifest (line 28) mitigates backup extraction, but does **not** protect against rooted device access or Forensic Mass Storage extraction.

**Remediation — Add SQLCipher (encrypted Room database):**

1. Add to `app/build.gradle` dependencies:
```groovy
implementation 'net.zetetic:android-database-sslcipher:2.2.4'
```

2. In `AppDatabase.getDatabase()`, replace `Room.databaseBuilder` with `SupportFactory`:
```kotlin
// app/src/main/java/com/app/muzzutech/data/db/AppDatabase.kt
import net.zetetic.database.sqlcipher.SupportFactory

fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
    val factory = SupportFactory(passphrase)
    return Room.databaseBuilder(context, AppDatabase::class.java, "mobile_repair_shop_db")
        .openHelperFactory(factory)
        .addMigrations(...)
        .fallbackToDestructiveMigration()
        .build()
}
```

3. Derive the passphrase from Android Keystore (never hardcoded):
```kotlin
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

fun getDatabasePassphrase(context: Context): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val alias = "muzzu_db_key"
    if (!keyStore.containsAlias(alias)) {
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        kg.generateKey()
    }
    val secretKey = (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
    return secretKey.encoded // 32-byte AES key → passphrase for SQLCipher
}
```

---

### V7-S02 [P2] — Biometric unlock state stored in unencrypted SharedPreferences

**Locations:**
- `SplashActivity.kt:32` — reads `"biometric_enabled"` from `app_settings` SharedPreferences
- `MoreFragment.kt:142-148` — writes `"biometric_enabled"` to `app_settings` SharedPreferences

**Vulnerability:** The `biometric_enabled` boolean flag is stored in plain-text SharedPreferences at `/data/data/com.app.muzzutech/shared_prefs/app_settings.xml`. On a rooted device, an attacker can trivially set this flag to `false` to bypass biometric authentication entirely. The same preference file also stores `dark_mode`.

Additionally, the `auth_prefs` SharedPreferences stores:
- `is_logged_in` (boolean) — `MainActivity.kt:62`
- `logged_in_email` (string) — `LoginFragment.kt:128`

**Remediation — Use EncryptedSharedPreferences:**

Replace all `getSharedPreferences("app_settings", MODE_PRIVATE)` with EncryptedSharedPreferences:

```kotlin
// In Application.onCreate() or a utility:
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val securePrefs = EncryptedSharedPreferences.create(
    context,
    "app_settings_encrypted",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Replace:
//   prefs.getBoolean("biometric_enabled", false)
// With:
//   securePrefs.getBoolean("biometric_enabled", false)
```

Add to `build.gradle`:
```groovy
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

---

## 2. Reverse Engineering & Code Obfuscation

### V7-S03 [P0] — `minifyEnabled false` on release builds — app ships fully unobfuscated

**Location:** `app/build.gradle:58-59`
```groovy
release {
    signingConfig signingConfigs.debug
    minifyEnabled false    // ← CRITICAL: no obfuscation
    shrinkResources false   // ← no resource shrinking
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

**Vulnerability:** The release APK ships with `minifyEnabled false`, meaning:
1. All Kotlin class/method/variable names are human-readable
2. All Room entity field names (customerMobile, paidAmount, etc.) are unobfuscated
3. All API endpoints, database schema, and business logic are trivially reverse-engineerable
4. `google-services.json` embedded values (Firebase API key, OAuth client IDs) are easily extractable
5. **Google Play Console warns** for apps without obfuscation — this can delay or block production deployment

**Remediation — Enable R8 with proper keep rules:**

In `app/build.gradle`:
```groovy
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

---

### V7-S04 [P1] — ProGuard rules are over-permissive — `-keep class com.app.muzzutech.** { *; }` disables all obfuscation for the entire app

**Location:** `app/proguard-rules.pro:40`
```
-keep class com.app.muzzutech.** { *; }
```

**Vulnerability:** The wildcard rule `com.app.muzzutech.** { *; }` disables ProGuard/R8 for the **entire application package**. This cancels out the obfuscation that `minifyEnabled true` would provide. Even if `minifyEnabled` were set to `true`, this rule ensures no class, method, or field in the app package is renamed or optimized.

The rule was presumably added for Gson serialization but is far too broad. Only classes that need reflection (Room entities with Gson, data classes used with Gson) should be kept.

**Remediation — Tighten ProGuard rules to only what reflection requires:**

Replace the over-broad rule with targeted ones:

```
# === Room Entities ===
# Room uses reflection on entity constructors and field names.
# The @Entity annotation itself triggers keep, but be explicit:
-keep class com.app.muzzutech.data.model.** { *; }

# === Gson Data Classes (used with fromJson/toJson) ===
-keep class com.app.muzzutech.utils.update.VersionInfo { *; }

# === ML Kit ===
-keep class com.google.mlkit.** { *; }

# === Remove the blanket wildcard ===
# -keep class com.app.muzzutech.** { *; }   ← DELETE THIS LINE

# === If using Gson with generic types in collections ===
-keepattributes Signature
-keepattributes *Annotation*
```

Additionally, for any data class that is serialized/deserialized via Gson in the `UpdateRepository`, add a `@Keep` annotation:

```kotlin
import androidx.annotation.Keep

@Keep
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val sizeBytes: Long? = null,
    val forceUpdate: Boolean = false
)
```

---

### V7-S05 [P2] — No R8 mapping file or debug symbol preservation

**Location:** `app/build.gradle` release block

**Vulnerability:** Without `mappingFile` preservation, crash reports from production (Play Console, Firebase Crashlytics, etc.) will show obfuscated stack traces that cannot be deobfuscated. If `minifyEnabled` is eventually set to `true`, crash debugging becomes impossible.

**Remediation — Preserve mapping file for crash deobfuscation:**

```groovy
release {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    // Preserve mapping file for Play Console crash deobfuscation
    android.applicationVariants.all { variant ->
        variant.assembleProvider.configure {
            doLast {
                copy {
                    from variant.mappingFile
                    into "${rootDir}/build/outputs/mapping/${variant.name}"
                    rename { String fileName -> "mapping-${variant.versionName}.txt" }
                }
            }
        }
    }
}
```

---

## 3. Permission Hygiene (Google Play Policy)

### V7-S06 [P2] — `READ_MEDIA_IMAGES` declared but never needed — will trigger Google Play review

**Location:** `AndroidManifest.xml:10`
```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**Vulnerability:** The app declares `READ_MEDIA_IMAGES` (API 33+) but **never actually uses it**. All camera operations use `ActivityResultContracts.TakePicture()` which writes to a `FileProvider` URI — no media read permission is required. The codebase never calls `MediaStore.Images` or `ContentResolver` to read gallery images.

Google Play's policy requires that permissions be used for their declared purpose. An unused `READ_MEDIA_IMAGES` will trigger a warning or rejection during the review process.

**Remediation — Remove `READ_MEDIA_IMAGES` from manifest:**

```xml
<!-- DELETE this line: -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

### V7-S07 [P2] — `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` is legacy and can be removed

**Location:** `AndroidManifest.xml:8-9`
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**Vulnerability:** The app uses `TakePicture` with FileProvider URIs — it never reads external storage directly. The backup export (`BackupManager.kt:28`) writes to `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)` which on API 29+ uses scoped storage and does not require `READ_EXTERNAL_STORAGE`. This permission is dead weight.

**Remediation — Remove both legacy storage permissions:**

```xml
<!-- DELETE both: -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

---

### V7-S08 [P1] — `REQUEST_INSTALL_PACKAGES` triggers Google Play "High Risk" flag

**Location:** `AndroidManifest.xml:12`
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

**Vulnerability:** `REQUEST_INSTALL_PACKAGES` is classified by Google as a "High Risk" permission. Apps that request it must undergo an **additional sensitivity review** during Play Store publishing. The app uses this for the in-app update feature (`UpdateManager.installApk()` at `UpdateManager.kt:225`).

Google Play policy states that `REQUEST_INSTALL_PACKAGES` is only permitted for "core functionality" of installing packages (e.g., app stores, file managers, browsers). A repair shop management app using it for self-updates may be rejected because:
1. The update mechanism should use Play In-App Updates API instead
2. Google Play reviews app self-update mechanisms strictly

**Remediation — Use Play In-App Updates API instead of custom APK download:**

Replace the custom `UpdateManager.downloadAndInstall()` + `installApk()` with the official Play Core In-App Updates API:

1. Add to `build.gradle`:
```groovy
implementation 'com.google.android.play:app-update-ktx:2.1.0'
```

2. Replace the update flow:
```kotlin
// In UpdateManager.checkForUpdates():
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

val appUpdateManager = AppUpdateManagerFactory.create(activity)
appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
        appUpdateManager.startUpdateFlowForResult(
            info, AppUpdateType.IMMEDIATE, activity, REQUEST_APP_UPDATE
        )
    }
}
```

3. Remove `REQUEST_INSTALL_PACKAGES` from manifest.

---

### V7-S09 [P3] — `android:required="true"` on camera feature rejects install on devices without camera

**Location:** `AndroidManifest.xml:15`
```xml
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

**Vulnerability:** The app cannot be installed on tablets, Android TV, or any device without a camera. Since the app's core repair-tracking functionality does not require a camera (it's only for optional photos), this should be `required="false"`.

**Remediation:**
```xml
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

---

## 4. Hardcoded Secrets & API Keys

### V7-S10 [P2] — `google-services.json` committed to git with Firebase API key and OAuth client secrets

**Locations:**
- `app/google-services.json` (tracked in git)
- `app/src/main/res/values/strings.xml:4` — `default_web_client_id`

**Vulnerability:** The `google-services.json` file at commit contains:
- **Firebase Web API Key:** `AIzaSyC-5-9Kn1mgbJC8SSVv3mpwf6BBjxxpuoA` (line 31)
- **OAuth Client ID (Android):** `816690590049-csagvpo4493k7pcclj97stvh4mda3tu9.apps.googleusercontent.com` (line 17)
- **OAuth Client ID (Web):** `816690590049-efj39799qqgl18qs17kr10iutfiqhv18.apps.googleusercontent.com` (line 25)
- **Project number:** `816690590049`

While Firebase API keys are not secret by design (they are restricted by SHA-1 fingerprint on the Google Cloud side), the **OAuth client secrets and project structure** being in a public repo is a risk. The `strings.xml` file also exposes `default_web_client_id` which is required at runtime for Google Sign-In.

Note: The `google-services.json` is **NOT** in `.gitignore`, meaning it is tracked and pushed on every commit.

**Remediation — Add to .gitignore and use BuildConfig for client IDs:**

1. Add `google-services.json` to `.gitignore`:
```
google-services.json
```

2. For the `default_web_client_id`, use `local.properties` injection (same pattern as OTP keys in `build.gradle:24-37`):
```groovy
// In app/build.gradle defaultConfig:
def webClientId = props.getProperty('google.web.client.id', '')
buildConfigField "String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\""
```

3. In `strings.xml`, remove the actual value and reference `BuildConfig`:
```kotlin
// In LoginFragment.kt and ProfileFragment.kt:
// Replace: getString(R.string.default_web_client_id)
// With:    BuildConfig.GOOGLE_WEB_CLIENT_ID
```

---

### V7-S11 [P1] — OTP API keys use placeholder fallback — hardcoded sentinel in APK

**Location:** `app/build.gradle:25-27`
```groovy
def otpApiKey = "PLACEHOLDER_KEY"
def otpSenderId = "PLACEHOLDER_SENDER"
def otpTemplateId = "PLACEHOLDER_TEMPLATE"
```

**Vulnerability:** If `local.properties` is missing or the `otp.api.key` property is not defined, the build continues with `"PLACEHOLDER_KEY"` as the actual API key. This value becomes a `BuildConfig.OTP_API_KEY` field in the APK. An attacker who decompiles the APK would see `PLACEHOLDER_KEY` and know the real key was not properly configured, making the OTP system trivially bypassable.

Additionally, the WhatsApp OTP system (`WhatsAppOtpUtil.kt`) generates a 6-digit OTP client-side and stores it in a `@Volatile` static field — this is not a server-verified OTP. Anyone who decompiles the APK can see the exact OTP generation and validation logic.

**Remediation — Fail the build if secrets are missing:**

Replace placeholders with a build failure:
```groovy
def otpApiKey = props.getProperty('otp.api.key')
if (otpApiKey == null || otpApiKey.isEmpty()) {
    throw new GradleException("otp.api.key not found in local.properties. " +
        "Create the file with: otp.api.key=your_key")
}
```

---

### V7-S12 [P1] — `version.json` URL and GitHub API URL hardcoded in source

**Location:** `UpdateRepository.kt:34-37`
```kotlin
private const val VERSION_URL =
    "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
private const val GITHUB_LATEST_URL =
    "https://api.github.com/repos/rahaman786mys/MobileRepairShop/releases/latest"
```

**Vulnerability:** The GitHub API URL is hardcoded with the developer's personal repository path. While not a traditional "secret," this is a production configuration that:
1. Exposes the developer's GitHub username (`rahaman786mys`) and repository name
2. Creates a tied dependency on a specific GitHub account — if the repo is moved, all deployed apps break
3. The GitHub API has rate limits (60 requests/hour for unauthenticated requests) — once exceeded, update checks silently fail

**Remediation — Make the update URL configurable with a fallback:**

```kotlin
private const val VERSION_URL = BuildConfig.UPDATE_CHECK_URL
    // Fallback: "https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json"
```

Add to `build.gradle`:
```groovy
buildConfigField "String", "UPDATE_CHECK_URL", "\"${props.getProperty('update.check.url', 'https://raw.githubusercontent.com/rahaman786mys/MobileRepairShop/master/version.json')}\""
```

---

## 5. Cloud Sync Resilience & Data Loss Vectors

### V7-S13 [P2] — `BackupManager.importDatabase()` closes the database while active flows are collecting

**Location:** `BackupManager.kt:90-107`

```kotlin
suspend fun importDatabase(context: Context, backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        MobileRepairApp.instance.database.close()
        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openInputStream(backupUri)?.use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
        MobileRepairApp.resetDatabaseInstance()
        true
    } catch (e: Exception) {
        MobileRepairApp.resetDatabaseInstance()
        false
    }
}
```

**Vulnerability:** This method:
1. Calls `database.close()` while other coroutines may be actively collecting Flows from DAOs
2. Writes to the database file directly via `FileOutputStream` while the Room database might not be fully closed (thread races)
3. If the write fails midway (e.g., the backup URI is a corrupt file, or the storage runs out of space), the database file is left in a partial/corrupt state
4. No checksum or integrity verification is performed on the backup before overwriting
5. If `resetDatabaseInstance()` is called but the replacement database file is corrupt, the app crashes on the next Room access

**Remediation — Add backup integrity verification + atomic swap:**

```kotlin
suspend fun importDatabase(context: Context, backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        // 1. Copy backup to a temp file first
        val tempFile = File(context.cacheDir, "restore_temp.db")
        context.contentResolver.openInputStream(backupUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext false

        // 2. Verify integrity by attempting to open the temp file with Room
        val integrityOk = try {
            val passphrase = byteArrayOf() // same passphrase as production
            val testDb = Room.databaseBuilder(context, AppDatabase::class.java, tempFile.absolutePath)
                .build()
            testDb.openHelper.readableDatabase  // throws if corrupt
            testDb.close()
            true
        } catch (e: Exception) {
            tempFile.delete()
            false
        }
        if (!integrityOk) return@withContext false

        // 3. Close the live database
        MobileRepairApp.instance.database.close()

        // 4. Atomic replace
        val dbFile = context.getDatabasePath(DB_NAME)
        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()

        // 5. Reset singleton
        MobileRepairApp.resetDatabaseInstance()
        true
    } catch (e: Exception) {
        MobileRepairApp.resetDatabaseInstance()
        false
    }
}
```

---

### V7-S14 [P2] — `UpdateManager.downloadAndInstall()` downloads APK on the calling thread via OkHttp enqueue without WorkManager constraints

**Location:** `UpdateManager.kt:139-215`

**Vulnerability:** The APK download uses `OkHttp.enqueue()` which runs on OkHttp's thread pool. While this is technically background-capable:
1. There is **no WorkManager network constraint** — the download will fail on slow 2G connections without automatic retry
2. There is **no battery-not-low constraint** — a large download could drain the battery
3. There is **no storage check** before downloading — if device storage is low, the download fails with an unhelpful error
4. The download **does not survive process death** — if the user switches apps and Android kills the process, the partial download is lost

**Remediation — Use WorkManager DownloadWorker with constraints:**

```kotlin
// Create a dedicated DownloadWorker
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString("download_url") ?: return Result.failure()
        return try {
            downloadApk(url)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// Enqueue with constraints
val request = OneTimeWorkRequestBuilder<DownloadWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    )
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
    .build()
WorkManager.getInstance(context).enqueue(request)
```

---

### V7-S15 [P1] — `UpdateRepository` disables all SSL/TLS certificate validation

**Location:** `UpdateRepository.kt:46-58`
```kotlin
private val client: OkHttpClient = run {
    val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAll, SecureRandom()) }
    OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .build()
}
```

**Vulnerability:** This custom `OkHttpClient` **completely disables SSL certificate validation**:
- `checkServerTrusted` — empty body → accepts ANY SSL certificate, including self-signed, expired, or malicious
- `checkClientTrusted` — empty body → accepts any client certificate
- `hostnameVerifier` — always returns `true` → accepts ANY hostname, enabling man-in-the-middle attacks
- `getAcceptedIssuers` — returns empty array

This means all update checks against `raw.githubusercontent.com` and `api.github.com` are **vulnerable to MITM attacks**. An attacker on the same WiFi network could serve a malicious "update" APK that gets installed.

**Remediation — Remove the custom trust-all SSL configuration entirely. Use the default OkHttp client:**

```kotlin
private val client: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .cache(null)
    .build()
```

The default OkHttp client uses the Android system trust store which correctly validates GitHub's SSL certificates.

---

### V7-S16 [P2] — Google Drive sync is a `NotImplementedError` dead code path

**Location:** `BackupManager.kt:110-113`
```kotlin
@Deprecated("Google Drive API not integrated. Tracked at TODO-123")
suspend fun syncWithGoogleDrive(context: Context, email: String): Nothing = withContext(Dispatchers.IO) {
    throw NotImplementedError("Google Drive API not integrated. Tracked at TODO-123")
}
```

**Vulnerability:** The "Google Cloud Sync" button in `MoreFragment.kt:76` calls this method, which **crashes the app** with a `NotImplementedError`. This is a user-facing crash that will be reported as a 1-star review on Google Play.

**Remediation — Either implement the sync or hide the button:**
```kotlin
// In MoreFragment.kt, wrap the sync call:
binding.btnGoogleSync.setOnClickListener {
    Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
    // Or navigate to a "not yet available" screen
}
```

---

## Summary of Findings

| ID | Severity | Category | File | Description |
|----|----------|----------|------|-------------|
| V7-S03 | **P0** | Obfuscation | `app/build.gradle:58` | `minifyEnabled false` — app ships fully unobfuscated |
| V7-S04 | **P1** | Obfuscation | `proguard-rules.pro:40` | Wildcard keep disables ALL obfuscation for app package |
| V7-S08 | **P1** | Permissions | `AndroidManifest.xml:12` | `REQUEST_INSTALL_PACKAGES` = high-risk → Play Store rejection risk |
| V7-S15 | **P1** | Secrets | `UpdateRepository.kt:46-58` | SSL trust-all = MITM vulnerability on update downloads |
| V7-S11 | **P1** | Secrets | `app/build.gradle:25` | OTP placeholder API key builds into APK if local.properties missing |
| V7-S12 | **P1** | Secrets | `UpdateRepository.kt:34` | Hardcoded GitHub repo path with rate-limited API |
| V7-S01 | **P1** | Data at Rest | `AppDatabase.kt` | Plain-text SQLite with financial PII — no encryption |
| V7-S06 | **P2** | Permissions | `AndroidManifest.xml:10` | Unused `READ_MEDIA_IMAGES` — triggers Play review |
| V7-S07 | **P2** | Permissions | `AndroidManifest.xml:8` | Legacy `READ/WRITE_EXTERNAL_STORAGE` — dead weight |
| V7-S09 | **P3** | Permissions | `AndroidManifest.xml:15` | Camera `required="true"` — blocks tablet installs |
| V7-S02 | **P2** | Data at Rest | `SplashActivity.kt:32` | Biometric toggle in unencrypted SharedPreferences |
| V7-S10 | **P2** | Secrets | `google-services.json` | Firebase API key + OAuth IDs in git-tracked file |
| V7-S13 | **P2** | Sync | `BackupManager.kt:90` | DB restore overwrites live file without integrity check |
| V7-S14 | **P2** | Sync | `UpdateManager.kt:139` | APK download without WorkManager constraints or retry |
| V7-S16 | **P2** | Sync | `BackupManager.kt:111` | Google Drive sync crashes app with NotImplementedError |
| V7-S05 | **P2** | Obfuscation | `build.gradle` | No mapping file preservation for crash deobfuscation |

**Grade: D** — 16 findings, including 1 P0 (reject at Play Store level), 5 P1 (critical security/privacy), 9 P2 (major), 1 P3 (minor).

**Priority remediation order for production deployment:**
1. **V7-S03** — Enable `minifyEnabled true` (P0 — required by Google Play)
2. **V7-S15** — Remove SSL trust-all (P1 — MITM on updates)
3. **V7-S08** — Replace APK download with Play In-App Updates (P1 — Play Store policy)
4. **V7-S04** — Tighten ProGuard rules (P1 — enables effective obfuscation)
5. **V7-S11** — Hard-fail build on missing secrets (P1 — prevents placeholder leakage)
6. **V7-S01** — Add SQLCipher (P1 — financial PII at rest)
7. **V7-S06/S7-S07** — Clean up unused permissions (P2 — Play Store review)
8. **V7-S13** — Add atomic backup restore (P2 — prevents data loss)
9. **V7-S02** — Encrypt SharedPreferences (P2 — biometric bypass)
