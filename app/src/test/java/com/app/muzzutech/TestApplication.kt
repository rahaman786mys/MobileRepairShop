package com.app.muzzutech

import android.app.Application
import androidx.room.Room
import com.app.muzzutech.data.db.AppDatabase

class TestApplication : MobileRepairApp() {
    
    override fun onCreate() {
        instance = this
        // No super call to avoid encrypted init
    }

    override val database: AppDatabase
        get() = _database ?: synchronized(this) {
            _database ?: Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build().also { _database = it }
        }
}
