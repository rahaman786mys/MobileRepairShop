package com.app.muzzutech

import android.app.Application
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.repository.RepairRepository

class MobileRepairApp : Application() {

    @Volatile
    private var _database: AppDatabase? = null

    val database: AppDatabase
        get() = _database ?: synchronized(this) {
            _database ?: AppDatabase.getDatabase(this).also { _database = it }
        }

    val repairRepository: RepairRepository by lazy {
        RepairRepository(database.repairEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        lateinit var instance: MobileRepairApp
            private set

        fun resetDatabaseInstance() {
            instance._database = null
        }
    }
}
