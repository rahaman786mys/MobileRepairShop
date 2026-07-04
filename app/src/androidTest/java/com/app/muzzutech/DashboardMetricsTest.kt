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
class DashboardMetricsTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"
    private val timeout = 8000L

    @Before
    fun setUp() {
        Intents.init()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("extra_nav_dest", "dashboard")
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.res(pkg, "tvTodayProfit")), timeout)
        device.waitForIdle(3000)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun dashboardShowsPending() {
        val el = device.wait(Until.findObject(By.text("PENDING")), timeout)
        assertTrue("Dashboard must show PENDING metric label", el != null)
    }

    @Test
    fun dashboardShowsCompleted() {
        val el = device.wait(Until.findObject(By.text("COMPLETED")), timeout)
        assertTrue("Dashboard must show COMPLETED metric label", el != null)
    }

    @Test
    fun dashboardShowsTodayProfit() {
        val el = device.wait(Until.findObject(By.textContains("TODAY'S PROFIT")), timeout)
        assertTrue("Dashboard must show TODAY'S PROFIT", el != null)
    }

    @Test
    fun dashboardShowsInvestment() {
        val el = device.wait(Until.findObject(By.text("INVESTMENT")), timeout)
        assertTrue("Dashboard must show INVESTMENT metric", el != null)
    }

    @Test
    fun dashboardShowsAccountsReceivable() {
        val el = device.wait(Until.findObject(By.text("ACCOUNTS RECEIVABLE")), timeout)
        assertTrue("Dashboard must show ACCOUNTS RECEIVABLE", el != null)
    }

    @Test
    fun dashboardShowsAccountsPayable() {
        val el = device.wait(Until.findObject(By.text("ACCOUNTS PAYABLE")), timeout)
        assertTrue("Dashboard must show ACCOUNTS PAYABLE", el != null)
    }

    @Test
    fun dashboardShowsRevenueInAICard() {
        val el = device.wait(Until.findObject(By.text("Revenue")), timeout)
        assertTrue("Dashboard AI card must show Revenue", el != null)
    }
}
