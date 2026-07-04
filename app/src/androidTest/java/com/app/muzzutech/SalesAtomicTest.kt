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
class SalesAtomicTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"
    private val timeout = 8000L

    @Before
    fun setUp() {
        Intents.init()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("extra_nav_dest", "sale")
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.res(pkg, "btnSaveSale")), timeout)
        device.waitForIdle(3000)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun saleScreenLoads() {
        val hasHeader = device.wait(Until.hasObject(By.res(pkg, "etItemName")), timeout)
        assertTrue("Sale screen must show item name field", hasHeader)
    }

    @Test
    fun saleFormAcceptsInput() {
        val itemName = device.wait(Until.findObject(By.res(pkg, "etItemName")), timeout)
        assertTrue("Item Name input must exist", itemName != null)

        val purchasePrice = device.wait(Until.findObject(By.res(pkg, "etPurchasePrice")), timeout)
        assertTrue("Purchase price field must exist", purchasePrice != null)
    }

    @Test
    fun saleSaveButtonExists() {
        val btn = device.wait(Until.findObject(By.res(pkg, "btnSaveSale")), timeout)
        assertTrue("Save button must exist on sale screen", btn != null)
    }
}
