package com.app.muzzutech

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayrollSmokeTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"

    @Test
    fun openPayrollFromDashboard() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("extra_nav_dest", "more")
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.text("Payroll & Attendance")), 10000)
        device.waitForIdle(2000)

        val payroll = device.wait(Until.findObject(By.text("Payroll & Attendance")), 5000)
        assertTrue("Payroll & Attendance not found", payroll != null)
        payroll?.click()
        device.waitForIdle(3000)

        assertTrue("Payroll screen not visible", device.wait(Until.hasObject(By.textContains("Monthly")), 3000))
    }
}
