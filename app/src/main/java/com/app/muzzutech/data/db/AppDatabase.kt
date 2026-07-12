package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.io.IOException

@Database(
    entities = [
        RepairEntry::class,
        ServiceMan::class,
        Supplier::class,
        SparePartPurchase::class,
        Customer::class,
        Dealer::class,
        Sale::class,
        UserProfile::class,
        Payment::class,
        PartReturn::class,
        PaymentTransaction::class,
        Attendance::class,
        SalaryPayment::class,
        Expense::class,
        LedgerAlert::class,
        Owner::class,
        AuthSession::class
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repairEntryDao(): RepairEntryDao
    abstract fun serviceManDao(): ServiceManDao
    abstract fun supplierDao(): SupplierDao
    abstract fun sparePartPurchaseDao(): SparePartPurchaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun dealerDao(): DealerDao
    abstract fun saleDao(): SaleDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun paymentDao(): PaymentDao
    abstract fun partReturnDao(): PartReturnDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun salaryDao(): SalaryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun ledgerAlertDao(): LedgerAlertDao
    abstract fun ownerDao(): OwnerDao
    abstract fun authSessionDao(): AuthSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 8 -> 9: adds Payroll + Expenses tables. Also adds two new columns
         * (monthlySalary, perDaySalary) to service_men. The existing schema (v8) is
         * preserved; no existing tables are dropped.
         */
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add payroll + expenses tables
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS attendance (
                        servicemanId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        present INTEGER NOT NULL,
                        halfDay INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(servicemanId, date)
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_date ON attendance(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_servicemanId ON attendance(servicemanId)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS salary_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        servicemanId INTEGER NOT NULL,
                        servicemanName TEXT NOT NULL,
                        monthStart INTEGER NOT NULL,
                        daysWorked REAL NOT NULL,
                        perDaySalary REAL NOT NULL,
                        fixedMonthlySalary REAL NOT NULL,
                        computedAmount REAL NOT NULL,
                        paidAmount REAL NOT NULL,
                        dueAmount REAL NOT NULL,
                        status TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_salary_payments_servicemanId ON salary_payments(servicemanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_salary_payments_monthStart ON salary_payments(monthStart)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        isRecurring INTEGER NOT NULL,
                        paid INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_category ON expenses(category)")

                // Add salary columns to service_men (existing rows get default 0.0)
                db.execSQL("ALTER TABLE service_men ADD COLUMN monthlySalary REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE service_men ADD COLUMN perDaySalary REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Migration 9 -> 10: adds Foreign Key constraints for data integrity.
         * Note: SQLite doesn't support ALTER TABLE ADD FOREIGN KEY, so we must
         * recreate the tables. 
         */
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreating attendance with FK
                db.execSQL("CREATE TABLE attendance_new (servicemanId INTEGER NOT NULL, date INTEGER NOT NULL, present INTEGER NOT NULL, halfDay INTEGER NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(servicemanId, date), FOREIGN KEY(servicemanId) REFERENCES service_men(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO attendance_new (servicemanId, date, present, halfDay, note, createdAt) SELECT servicemanId, date, present, halfDay, note, createdAt FROM attendance")
                db.execSQL("DROP TABLE attendance")
                db.execSQL("ALTER TABLE attendance_new RENAME TO attendance")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_date ON attendance(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_servicemanId ON attendance(servicemanId)")

                // Recreating salary_payments with FK
                db.execSQL("CREATE TABLE salary_payments_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, servicemanId INTEGER NOT NULL, servicemanName TEXT NOT NULL, monthStart INTEGER NOT NULL, daysWorked REAL NOT NULL, perDaySalary REAL NOT NULL, fixedMonthlySalary REAL NOT NULL, computedAmount REAL NOT NULL, paidAmount REAL NOT NULL, dueAmount REAL NOT NULL, status TEXT NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(servicemanId) REFERENCES service_men(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO salary_payments_new (id, servicemanId, servicemanName, monthStart, daysWorked, perDaySalary, fixedMonthlySalary, computedAmount, paidAmount, dueAmount, status, note, createdAt) SELECT id, servicemanId, servicemanName, monthStart, daysWorked, perDaySalary, fixedMonthlySalary, computedAmount, paidAmount, dueAmount, status, note, createdAt FROM salary_payments")
                db.execSQL("DROP TABLE salary_payments")
                db.execSQL("ALTER TABLE salary_payments_new RENAME TO salary_payments")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_salary_payments_servicemanId ON salary_payments(servicemanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_salary_payments_monthStart ON salary_payments(monthStart)")
                
                // Recreating payment_transactions with FK
                db.execSQL("CREATE TABLE payment_transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, paymentId INTEGER NOT NULL, personType TEXT NOT NULL, personMobile TEXT NOT NULL, personName TEXT NOT NULL, amount REAL NOT NULL, paymentMode TEXT NOT NULL, note TEXT NOT NULL, transactionDate INTEGER NOT NULL, FOREIGN KEY(paymentId) REFERENCES payments(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO payment_transactions_new (id, paymentId, personType, personMobile, personName, amount, paymentMode, note, transactionDate) SELECT id, paymentId, personType, personMobile, personName, amount, paymentMode, note, transactionDate FROM payment_transactions")
                db.execSQL("DROP TABLE payment_transactions")
                db.execSQL("ALTER TABLE payment_transactions_new RENAME TO payment_transactions")
                // RoomOpenHelper compares the live schema to the @Entity declaration; both
                // indices declared on PaymentTransaction (paymentId + personMobile) must
                // exist post-migration, otherwise Room throws
                // "Migration didn't properly handle... expected index ..._personMobile".
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_paymentId ON payment_transactions(paymentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_personMobile ON payment_transactions(personMobile)")

                // Recreating spare_part_purchases with FK
                db.execSQL("CREATE TABLE spare_part_purchases_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, repairEntryId INTEGER, partName TEXT NOT NULL, partPhotoPath TEXT NOT NULL, purchasePrice REAL NOT NULL, supplierId TEXT NOT NULL, supplierName TEXT NOT NULL, quantity INTEGER NOT NULL, purchaseDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(repairEntryId) REFERENCES repair_entries(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO spare_part_purchases_new (id, repairEntryId, partName, partPhotoPath, purchasePrice, supplierId, supplierName, quantity, purchaseDate, createdAt) SELECT id, CASE WHEN repairEntryId \u003d 0 THEN NULL ELSE repairEntryId END, partName, partPhotoPath, purchasePrice, supplierId, supplierName, quantity, purchaseDate, createdAt FROM spare_part_purchases")
                db.execSQL("DROP TABLE spare_part_purchases")
                db.execSQL("ALTER TABLE spare_part_purchases_new RENAME TO spare_part_purchases")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_spare_part_purchases_supplierId ON spare_part_purchases(supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_spare_part_purchases_repairEntryId ON spare_part_purchases(repairEntryId)")
            }
        }

        /**
         * Migration 10 -> 11: paymentId becomes nullable, FK changes from CASCADE to SET NULL.
         * This fixes the crash where Expenses/Payroll/Sale ViewModels inserted
         * PaymentTransaction rows with paymentId=0 (no parent Payment), violating the FK.
         * Existing rows with paymentId=0 are set to NULL during the migration.
         */
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate payment_transactions with nullable paymentId + SET NULL FK
                db.execSQL("CREATE TABLE payment_transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, paymentId INTEGER, personType TEXT NOT NULL, personMobile TEXT NOT NULL, personName TEXT NOT NULL, amount REAL NOT NULL, paymentMode TEXT NOT NULL, note TEXT NOT NULL, transactionDate INTEGER NOT NULL, FOREIGN KEY(paymentId) REFERENCES payments(id) ON DELETE SET NULL)")
                // Clean dangling rows: paymentId=0 means no parent Payment existed
                db.execSQL("INSERT INTO payment_transactions_new (id, paymentId, personType, personMobile, personName, amount, paymentMode, note, transactionDate) SELECT id, CASE WHEN paymentId \u003d 0 THEN NULL ELSE paymentId END, personType, personMobile, personName, amount, paymentMode, note, transactionDate FROM payment_transactions")
                db.execSQL("DROP TABLE payment_transactions")
                db.execSQL("ALTER TABLE payment_transactions_new RENAME TO payment_transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_paymentId ON payment_transactions(paymentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_personMobile ON payment_transactions(personMobile)")
            }
        }

        /**
         * Migration 11 -> 12: adds performance indices on commonly queried columns
         * for repair_entries, payments, part_returns, and sales.
         */
        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_entries_customerMobile ON repair_entries(customerMobile)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_entries_serviceManId ON repair_entries(serviceManId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_entries_entryDate ON repair_entries(entryDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_entries_workStatus ON repair_entries(workStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_entries_handoverDone ON repair_entries(handoverDone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_personMobile ON payments(personMobile)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_personType ON payments(personType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_status ON payments(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedEntryId ON payments(linkedEntryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedSaleId ON payments(linkedSaleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedPartId ON payments(linkedPartId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_part_returns_supplierId ON part_returns(supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_part_returns_returnDate ON part_returns(returnDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_supplierId ON sales(supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_saleDate ON sales(saleDate)")
            }
        }

        /**
         * Migration 12 -> 13: adds expenseId column + index on payment_transactions.
         * This enables bidirectional cleanup: deleting an expense also removes its
         * linked PaymentTransaction, and toggling paid/unpaid creates/removes the txn.
         */
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payment_transactions ADD COLUMN expenseId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_expenseId ON payment_transactions(expenseId)")
            }
        }

        /**
         * Migration 13 -> 14: adds LedgerAlert table for Nightly Auditor results.
         */
        val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS ledger_alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    alertDate INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    expectedAmount REAL NOT NULL DEFAULT 0.0,
                    actualAmount REAL NOT NULL DEFAULT 0.0,
                    mismatchAmount REAL NOT NULL DEFAULT 0.0,
                    resolved INTEGER NOT NULL DEFAULT 0,
                    resolvedAt INTEGER
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_alerts_alertDate ON ledger_alerts(alertDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_alerts_resolved ON ledger_alerts(resolved)")
            }
        }

        /**
         * Migration 14 -> 15:
         *  - BUG #11: adds refundTransactionId to part_returns
         *  - BUG #10: adds salaryPaymentId to expenses + payment_transactions
         *  - BUG #9:  adds FK constraint on payments.linkedEntryId (SET NULL on delete)
         */
        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // BUG #11: refundTransactionId on part_returns
                db.execSQL("ALTER TABLE part_returns ADD COLUMN refundTransactionId INTEGER DEFAULT NULL")

                // BUG #10: salaryPaymentId on expenses + payment_transactions
                db.execSQL("ALTER TABLE expenses ADD COLUMN salaryPaymentId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_transactions ADD COLUMN salaryPaymentId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_transactions_salaryPaymentId ON payment_transactions(salaryPaymentId)")

                // BUG #9: FK on payments.linkedEntryId — requires table recreation
                db.execSQL("CREATE TABLE payments_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "personType TEXT NOT NULL," +
                        "personMobile TEXT NOT NULL," +
                        "personName TEXT NOT NULL," +
                        "description TEXT NOT NULL," +
                        "totalAmount REAL NOT NULL," +
                        "paidAmount REAL NOT NULL," +
                        "dueAmount REAL NOT NULL," +
                        "status TEXT NOT NULL," +
                        "linkedEntryId INTEGER," +
                        "linkedSaleId INTEGER NOT NULL DEFAULT 0," +
                        "linkedPartId INTEGER NOT NULL DEFAULT 0," +
                        "createdAt INTEGER NOT NULL," +
                        "updatedAt INTEGER NOT NULL," +
                        "FOREIGN KEY (linkedEntryId) REFERENCES repair_entries(id) ON DELETE SET NULL" +
                        ")")
                db.execSQL("INSERT INTO payments_new (id, personType, personMobile, personName, description, totalAmount, paidAmount, dueAmount, status, linkedEntryId, linkedSaleId, linkedPartId, createdAt, updatedAt) " +
                        "SELECT id, personType, personMobile, personName, description, totalAmount, paidAmount, dueAmount, status, linkedEntryId, linkedSaleId, linkedPartId, createdAt, updatedAt FROM payments")
                db.execSQL("DROP TABLE payments")
                db.execSQL("ALTER TABLE payments_new RENAME TO payments")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_personMobile ON payments(personMobile)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_personType ON payments(personType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_status ON payments(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedEntryId ON payments(linkedEntryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedSaleId ON payments(linkedSaleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_linkedPartId ON payments(linkedPartId)")
            }
        }

        /**
         * Migration 16 -> 17: drop common_faults table (Inspection + Quotation flows removed).
         */
        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS common_faults")
            }
        }

        /**
         * Migration 17 -> 18: adds direction and transactionType to payment_transactions
         * for the Unified Ledger logic.
         */
        val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payment_transactions ADD COLUMN direction TEXT NOT NULL DEFAULT 'IN'")
                db.execSQL("ALTER TABLE payment_transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'REVENUE'")

                // Backfill direction for old records based on personType
                db.execSQL("UPDATE payment_transactions SET direction = 'OUT', transactionType = 'EXPENSE' WHERE personType IN ('EXPENSE', 'SALARY', 'SUPPLIER')")
            }
        }

        /**
         * Migration 18 -> 19: adds lastSyncEmail to user_profile for Google Drive sync tracking.
         */
        val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN lastSyncEmail TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE owners ADD COLUMN shopAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owners ADD COLUMN gstNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE owners ADD COLUMN profilePhotoBase64 TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS owners (
                    id TEXT NOT NULL PRIMARY KEY,
                    businessName TEXT NOT NULL DEFAULT '',
                    ownerName TEXT NOT NULL DEFAULT '',
                    phoneNumber TEXT NOT NULL DEFAULT '',
                    email TEXT NOT NULL DEFAULT '',
                    googleAccountId TEXT,
                    createdAt INTEGER NOT NULL,
                    subscriptionTier TEXT NOT NULL DEFAULT 'FREE',
                    subscriptionExpiresAt INTEGER
                )""".trimIndent())
                db.execSQL("""CREATE TABLE IF NOT EXISTS auth_sessions (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    userId TEXT NOT NULL DEFAULT '',
                    userType TEXT NOT NULL DEFAULT '',
                    firebaseUid TEXT NOT NULL DEFAULT '',
                    loginTimestamp INTEGER NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1
                )""".trimIndent())
                db.execSQL("ALTER TABLE service_men ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE service_men ADD COLUMN passwordHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE service_men ADD COLUMN workerAuthUid TEXT")
                db.execSQL("ALTER TABLE service_men ADD COLUMN canLogin INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_men_ownerId ON service_men(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auth_sessions_firebaseUid ON auth_sessions(firebaseUid)")
            }
        }

        fun getDatabase(context: Context, passphrase: ByteArray? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath("mobile_repair_shop_db")
                if (passphrase != null && dbFile.exists() && !isEncrypted(dbFile)) {
                    migrateToEncrypted(context, dbFile, passphrase)
                }
                val factory = if (passphrase != null) SupportFactory(passphrase) else null
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobile_repair_shop_db"
                )
                    .apply { if (factory != null) openHelperFactory(factory) }
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun isEncrypted(file: File): Boolean {
            return try {
                file.inputStream().buffered().use { stream ->
                    val header = ByteArray(16)
                    val read = stream.read(header)
                    if (read < 16) return false
                    val headerStr = String(header, Charsets.US_ASCII)
                    headerStr != "SQLite format 3\u0000"
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun migrateToEncrypted(context: Context, dbFile: File, passphrase: ByteArray) {
            val backupFile = File(context.noBackupFilesDir, "legacy_db_plaintext_backup.db")
            val encryptedTempFile = File(context.noBackupFilesDir, "mobile_repair_shop_db_encrypted.tmp")
            try {
                if (backupFile.exists()) backupFile.delete()
                if (encryptedTempFile.exists()) encryptedTempFile.delete()

                // Preserve the live DB until the encrypted copy is fully verified.
                dbFile.copyTo(backupFile, overwrite = true)

                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                val plainDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    backupFile.absolutePath,
                    "",
                    null as net.sqlcipher.database.SQLiteDatabase.CursorFactory?,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                )
                val encryptedPath = encryptedTempFile.absolutePath.replace("'", "''")
                val keyHex = passphrase.joinToString(separator = "") { "%02x".format(it) }
                plainDb.rawExecSQL("ATTACH DATABASE '$encryptedPath' AS encrypted KEY x'$keyHex'")
                plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                val versionCursor = plainDb.rawQuery("PRAGMA user_version", arrayOfNulls<String>(0))
                val userVersion = if (versionCursor.moveToFirst()) versionCursor.getInt(0) else 0
                versionCursor.close()
                plainDb.rawExecSQL("PRAGMA encrypted.user_version = $userVersion")
                plainDb.rawExecSQL("DETACH DATABASE encrypted")
                plainDb.close()

                val keyHexForOpen = passphrase.joinToString(separator = "") { "%02x".format(it) }
                val encryptedDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    encryptedTempFile.absolutePath,
                    keyHexForOpen,
                    null as net.sqlcipher.database.SQLiteDatabase.CursorFactory?,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY
                )
                encryptedDb.rawQuery("SELECT COUNT(*) FROM sqlite_master", arrayOfNulls<String>(0)).use { it.moveToFirst() }
                encryptedDb.close()

                val replaceBackup = File(context.noBackupFilesDir, "mobile_repair_shop_db.pre_encryption")
                if (replaceBackup.exists()) replaceBackup.delete()
                dbFile.copyTo(replaceBackup, overwrite = true)
                if (!dbFile.delete()) throw IOException("Unable to replace plaintext database")
                encryptedTempFile.copyTo(dbFile, overwrite = true)
                encryptedTempFile.delete()
                deleteSidecarFiles(context, "mobile_repair_shop_db")
                android.util.Log.i("AppDatabase", "Plaintext database encrypted successfully")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "SQLCipher migration failed; keeping plaintext database", e)
                if (encryptedTempFile.exists()) encryptedTempFile.delete()
            }
        }

        private fun deleteSidecarFiles(context: Context, dbName: String) {
            listOf("$dbName-wal", "$dbName-shm", "$dbName-journal").forEach { name ->
                val sidecar = context.getDatabasePath(name)
                if (sidecar.exists()) sidecar.delete()
            }
        }
    }
}
