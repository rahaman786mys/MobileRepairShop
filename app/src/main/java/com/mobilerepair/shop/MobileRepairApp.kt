package com.mobilerepair.shop

import android.app.Application
import com.mobilerepair.shop.data.db.AppDatabase
import com.mobilerepair.shop.data.repository.RepairRepository

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
