package com.app.muzzutech

import android.app.Application
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.repository.RepairRepository
import com.app.muzzutech.utils.crpto.DatabasePassphraseProvider
import com.app.muzzutech.work.AppScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

open class MobileRepairApp : Application() {

    @Volatile
    internal var _database: AppDatabase? = null

    open val database: AppDatabase
        get() = _database ?: synchronized(this) {
            val passphrase = DatabasePassphraseProvider.getOrCreatePassphrase(this)
            _database ?: AppDatabase.getDatabase(this, passphrase).also { _database = it }
        }

    val repairRepository: RepairRepository by lazy {
        RepairRepository(database.repairEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            AppScheduler.enqueueDailyJobs(this)
        } catch (e: Throwable) {
            // WorkManager may not be available in test/instrumentation contexts.
            android.util.Log.w("MobileRepairApp", "WorkManager init skipped: ${e.message}")
        }
        initializeStaticData()
        applySavedTheme()
    }

    private fun applySavedTheme() {
        val prefs = com.app.muzzutech.utils.crpto.SecurePrefs.appSettings(this)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun initializeStaticData() {
        val db = database
        // Using a controlled scope instead of GlobalScope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 2. Demo data for easier testing in debug builds if DB is empty
                if (com.app.muzzutech.BuildConfig.DEBUG) {
                    if (db.supplierDao().getCount() == 0) {
                        db.supplierDao().insert(com.app.muzzutech.data.model.Supplier(mobile = "9999911111", name = "Global Spare Parts", companyName = "GSP Wholesalers", city = "Mumbai"))
                        db.supplierDao().insert(com.app.muzzutech.data.model.Supplier(mobile = "9999922222", name = "Modern Electronics", companyName = "ME Ltd", city = "Delhi"))
                    }
                    if (db.serviceManDao().getCount() == 0) {
                        db.serviceManDao().insert(com.app.muzzutech.data.model.ServiceMan(name = "Senior Technician", mobile = "9000000001", designation = "Lead Specialist", monthlySalary = 4500000L))
                        db.serviceManDao().insert(com.app.muzzutech.data.model.ServiceMan(name = "Junior Helper", mobile = "9000000002", designation = "Trainee", monthlySalary = 1500000L))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MobileRepairApp", "Static data init failed", e)
            }
        }
    }

    companion object {
        @Volatile
        var instance: MobileRepairApp = MobileRepairApp()
            internal set

        fun resetDatabaseInstance() {
            instance._database = null
        }
    }

    fun setTestDatabase(testDb: AppDatabase) {
        _database = testDb
    }
}
