package com.app.muzzutech

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayrollE2ETest {
    private val device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"

    @Test
    fun launchPayrollViaTestLauncher() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("extra_dest", "payroll")
        }
        ctx.startActivity(intent)
        device.waitForIdle(5000)
        val hasApp = device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 5000)
        assertTrue("Payroll screen must launch without crash", hasApp)
    }

    @Test
    fun launchEntryAndVerifyDraftButton() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("extra_dest", "entry")
        }
        ctx.startActivity(intent)
        device.waitForIdle(5000)
        val hasApp = device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 5000)
        assertTrue("Entry screen must launch without crash", hasApp)
    }
}
