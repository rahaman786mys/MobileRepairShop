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
class ExpensesStateTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"
    private val timeout = 5000L

    @Before
    fun setUp() {
        Intents.init()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("extra_nav_dest", "expenses")
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
    fun expensesScreenLoads() {
        val hasContent = device.wait(Until.hasObject(By.textContains("This Month")), 10000) ||
            device.wait(Until.hasObject(By.textContains("expenses")), 10000)
        assertTrue("Expenses screen must show This Month or empty state", hasContent)
    }

    @Test
    fun expensesShowsAddButton() {
        val hasAdd = device.wait(Until.hasObject(By.descContains("Add")), timeout)
        assertTrue("Expenses screen must have FAB with Add description", hasAdd)
    }

    @Test
    fun expensesShowsNavigation() {
        val hasNav = device.wait(Until.hasObject(By.textContains("This Month")), timeout)
        assertTrue("Expenses screen must show summary", hasNav)
    }
}
