package com.app.muzzutech

import android.app.Application
import androidx.room.Room
import com.app.muzzutech.data.db.AppDatabase

class TestApplication : Application() {
    companion object {
        lateinit var instance: TestApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
