package com.app.muzzutech

import android.app.Application
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.repository.RepairRepository

class MobileRepairApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val repairRepository: RepairRepository by lazy {
        RepairRepository(database.repairEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MobileRepairApp
            private set
    }
}
