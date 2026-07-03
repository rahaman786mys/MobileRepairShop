package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*

@Database(
    entities = [
        RepairEntry::class,
        ServiceMan::class,
        Supplier::class,
        CommonFault::class,
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
        Expense::class
    ],
    version = 11, // Bumped from 10: paymentId nullable + SET NULL FK
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repairEntryDao(): RepairEntryDao
    abstract fun serviceManDao(): ServiceManDao
    abstract fun supplierDao(): SupplierDao
    abstract fun commonFaultDao(): CommonFaultDao
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobile_repair_shop_db"
                )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
