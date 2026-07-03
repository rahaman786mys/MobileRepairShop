package com.app.muzzutech

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TestLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dest = intent.getStringExtra(EXTRA_DEST) ?: DEST_DASHBOARD
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAV_DEST, dest)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(mainIntent)
        finish()
    }

    companion object {
        const val EXTRA_DEST = "extra_dest"
        const val DEST_DASHBOARD = "dashboard"
        const val DEST_ENTRY = "entry"
        const val DEST_SALE = "sale"
        const val DEST_DUES = "dues"
        const val DEST_REPORTS = "reports"
        const val DEST_MORE = "more"
        const val DEST_PAYROLL = "payroll"
        const val DEST_EXPENSES = "expenses"
        const val DEST_SUPPLIERS = "suppliers"
        const val DEST_CUSTOMERS = "customers"
        const val DEST_FAULTS = "faults"
        const val DEST_INVENTORY = "inventory"
        const val DEST_PROFILE = "profile"
    }
}
