package com.app.muzzutech

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayrollSmokeTest {
    private val device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"

    @Test
    fun openPayrollFromDashboard() {
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 10000)
        device.waitForIdle(2000)

        device.findObject(By.res(pkg, "cardMoreGrid"))?.click()
        device.waitForIdle(3000)

        val payroll = device.wait(Until.findObject(By.text("Payroll & Attendance")), 5000)
        assertTrue("Payroll & Attendance not found", payroll != null)
        payroll?.click()
        device.waitForIdle(3000)

        assertTrue("Payroll screen not visible", device.wait(Until.hasObject(By.textContains("Monthly")), 3000))
    }
}
