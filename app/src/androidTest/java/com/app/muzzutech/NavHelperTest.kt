package com.app.muzzutech

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavHelperTest {
    private val device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"

    @Test
    fun openMore() {
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 5000)
        device.findObject(By.res(pkg, "moreFragment"))?.click()
        device.waitForIdle(500)
    }
}
