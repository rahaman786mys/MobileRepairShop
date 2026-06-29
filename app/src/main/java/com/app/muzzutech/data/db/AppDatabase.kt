package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        UserProfile::class
    ],
    version = 5, // Incremented version for UserProfile
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobile_repair_shop_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
