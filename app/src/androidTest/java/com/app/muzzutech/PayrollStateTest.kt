package com.app.muzzutech

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayrollStateTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"
    private val timeout = 5000L

    @Before
    fun setUp() {
        Intents.init()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("extra_nav_dest", "payroll")
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), timeout)
        device.waitForIdle(4000)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun payrollScreenLoads() {
        val hasContent = device.wait(Until.hasObject(By.textContains("Payroll")), 10000) ||
            device.wait(Until.hasObject(By.textContains("Monthly")), 10000)
        assertTrue("Payroll screen must show Payroll or Monthly header", hasContent)
    }

    @Test
    fun payrollShowsMonthNavigation() {
        val hasNav = device.wait(Until.hasObject(By.textContains("Previous")), timeout) ||
            device.findObject(By.descContains("Previous")) != null ||
            device.wait(Until.hasObject(By.textContains("Next")), timeout) ||
            device.findObject(By.descContains("Next")) != null
        assertTrue("Payroll screen must have month navigation", hasNav)
    }

    @Test
    fun payrollLoadsContent() {
        val hasContent = device.wait(Until.hasObject(By.textContains("Service")), 10000) ||
            device.findObject(By.textContains("Technician")) != null ||
            device.findObject(By.textContains("Helper")) != null ||
            device.wait(Until.hasObject(By.textContains("Present")), 10000)
        assertTrue("Payroll screen should load content", hasContent)
    }
}
