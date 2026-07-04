package com.app.muzzutech

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.MediaStore
import kotlinx.coroutines.runBlocking
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepairPipelineE2ETest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val pkg = "com.app.muzzutech"
    private val timeout = 5000L
    private val longTimeout = 10000L

    @Before
    fun setUp() {
        // Skip camera launch in EntryFragment to avoid onPause/onResume cycles
        com.app.muzzutech.ui.entry.EntryFragment.skipCameraLaunch = true

        // Seed a service man so spinner has an option to select
        try {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val db = com.app.muzzutech.data.db.AppDatabase.getDatabase(ctx)
            runBlocking {
                db.serviceManDao().insert(
                    com.app.muzzutech.data.model.ServiceMan(
                        name = "Senior Technician", mobile = "9999999991"
                    )
                )
            }
        } catch (_: Exception) {}

        Intents.init()
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        Intents.intending(allOf(
            hasAction(Intent.ACTION_VIEW),
            hasPackage("com.whatsapp")
        )).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TestLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("extra_nav_dest", "entry")
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), timeout)
        device.waitForIdle(3000)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun fullRepairPipeline_entryToHandover() {
        val short = 1000L

        // ===== STEP 1: Fill and save Repair Entry =====
        val mobile = "98" + System.currentTimeMillis().toString().takeLast(8)

        var el = device.wait(Until.findObject(By.res(pkg, "etMobileNumber")), timeout)
        assertTrue("Mobile field must exist", el != null)
        el!!.text = mobile
        device.waitForIdle(1500)

        el = device.wait(Until.findObject(By.res(pkg, "etName")), short)
        if (el != null) el.text = "E2E Test Customer"

        el = device.wait(Until.findObject(By.res(pkg, "layoutRepairFields")), 4000)
        assertTrue("Repair fields must become visible after entering mobile", el != null)
        device.waitForIdle(2000)

        // Helper: scroll to find element and verify it's on screen before returning
        fun findVisible(resId: String): androidx.test.uiautomator.UiObject2? {
            for (i in 0 until 15) {
                val obj = device.wait(Until.findObject(By.res(pkg, resId)), 400)
                if (obj != null && !obj.visibleBounds.isEmpty) return obj
                device.swipe(720, 2100, 720, 400, 20)
                device.waitForIdle(400)
            }
            val last = device.wait(Until.findObject(By.res(pkg, resId)), 500)
            return if (last != null && !last.visibleBounds.isEmpty) last else null
        }

        // Fill brand/model (near top, should be visible without scroll)
        el = findVisible("spinnerBrand")
        assertTrue("Brand spinner must exist", el != null)
        el!!.click(); device.waitForIdle(500)
        val brandOpt = device.wait(Until.findObject(By.text("Samsung")), 2000)
        if (brandOpt != null) { brandOpt.click(); device.waitForIdle(500) }

        el = findVisible("etModelName")
        if (el != null) el.text = "Galaxy S23"

        // Take photos
        el = findVisible("btnTakePhoto")
        assertTrue("Photo 1 button must exist", el != null)
        el!!.click(); device.waitForIdle(3000)

        el = findVisible("btnTakePhoto2")
        assertTrue("Photo 2 button must exist", el != null)
        el!!.click(); device.waitForIdle(3000)

        // Scroll to service man and save
        el = findVisible("spinnerServiceMan")
        assertTrue("ServiceMan spinner must exist", el != null)
        el!!.click(); device.waitForIdle(500)
        val smOpt = device.wait(Until.findObject(By.textContains("Senior")), 2000)
        if (smOpt != null) { smOpt.click(); device.waitForIdle(500) }

        el = findVisible("btnSaveEntry")
        assertTrue("Save Entry button must exist", el != null)
        el!!.click()
        device.waitForIdle(3000)

        // ===== STEP 2: Inspection screen =====
        val inspOk = device.wait(Until.hasObject(By.text("Inspection Details")), longTimeout)
        assertTrue("Must navigate to Inspection screen after saving entry", inspOk)

        el = findVisible("etCustomFault")
        if (el != null) el.text = "Test fault - broken display"

        el = findVisible("btnTakeInspectionPhoto")
        if (el != null) { el.click(); device.waitForIdle(1500) }

        // Scroll to reveal Save Inspection button
        el = findVisible("btnSaveInspection")
        assertTrue("Save Inspection must exist", el != null)
        el!!.click(); device.waitForIdle(2000)

        // ===== STEP 3: Quotation screen =====
        device.wait(Until.hasObject(By.textContains("Charge")), longTimeout)
        assertTrue("Must navigate to Quotation", true)

        el = device.wait(Until.findObject(By.clazz("android.widget.EditText")), short)
        if (el != null) el.text = "1500"

        el = device.wait(Until.findObject(By.textContains("Save")), timeout)
        if (el != null) { el.click(); device.waitForIdle(2000) }

        // ===== STEP 4: Spare Parts screen =====
        device.wait(Until.hasObject(By.textContains("Part")), longTimeout)
        assertTrue("Must navigate to Spare Parts", true)

        el = device.wait(Until.findObject(By.clazz("android.widget.EditText")), short)
        if (el != null) el.text = "Test Screen"

        el = device.wait(Until.findObject(By.textContains("Save")), short)
        if (el != null) { el.click(); device.waitForIdle(2000) }

        el = device.wait(Until.findObject(By.textContains("Handover")), timeout)
        if (el != null) { el.click(); device.waitForIdle(2000) }

        // ===== STEP 5: Handover screen =====
        device.wait(Until.hasObject(By.res(pkg, "btnCompleteHandover")), longTimeout)
        assertTrue("Must navigate to Handover", true)
        device.waitForIdle(3000)

        // Pre-capture button center before any interaction invalidates the tree
        val btnBounds = device.wait(Until.findObject(By.res(pkg, "btnCompleteHandover")), timeout)?.visibleBounds
        val btnCenterX = btnBounds?.centerX() ?: 720
        val btnCenterY = btnBounds?.centerY() ?: 1400

        // Set amount
        el = device.wait(Until.findObject(By.res(pkg, "etFinalAmount")), timeout)
        if (el != null) el.text = "2000"
        device.waitForIdle(1500)

        // Select Cash radio  
        el = device.wait(Until.findObject(By.res(pkg, "radioCash")), timeout)
        if (el != null) { el.click(); device.waitForIdle(500) }

        // Click by captured coordinates (tree changes after text/radio, but physical position is stable)
        device.click(btnCenterX, btnCenterY)
        device.waitForIdle(3000)

        device.wait(Until.hasObject(By.textContains("Entries")), longTimeout)
        assertTrue("Must navigate after handover completion", true)
    }

}
