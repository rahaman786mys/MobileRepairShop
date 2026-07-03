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
    version = 9, // Bumped from 8 for Payroll + Expenses feature
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobile_repair_shop_db"
                )
                    .addMigrations(MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
