package com.app.muzzutech.utils

import com.app.muzzutech.data.model.RepairEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario-driven tests for the predicate "should the InspectionFragment fire a
 * 'Repair Started' WhatsApp notification after inspection save?".
 *
 * Source contract (per InspectionFragment.saveInspection wiring):
 *   1. Inspection must be saved successfully (entry has inspectionDone=true and faultDetected non-blank)
 *   2. customerMobile must be non-empty and present
 *   3. (Hard-fault-branch) If customer is empty AND dealer is present we DO NOT fire —
 *      because the canned message addresses `customerName`/`customerMobile` which would
 *      be blank or misleading.
 *
 * We test the decision logic independent of Android Context / Intent resolution
 * so it stays a plain JVM unit test. The Fragment calls NotificationUtils.sendRepairStartedWhatsApp
 * only when [shouldFireRepairStartedNotification] is true.
 */
class NotificationTriggerLogicTest {

    private fun shouldFireRepairStartedNotification(entry: RepairEntry): Boolean =
        entry.inspectionDone &&
            entry.faultDetected.isNotBlank() &&
            entry.customerMobile.isNotBlank()

    @Test
    fun `fires when customer mobile present and inspection complete`() {
        val entry = RepairEntry(
            customerName = "Ravi",
            customerMobile = "9876543210",
            faultDetected = "Display cracked",
            inspectionDone = true
        )
        assertTrue(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when customer mobile is blank`() {
        val entry = RepairEntry(
            customerName = "Ravi",
            customerMobile = "",
            faultDetected = "Display cracked",
            inspectionDone = true
        )
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when inspection not done`() {
        val entry = RepairEntry(
            customerMobile = "9876543210",
            faultDetected = "Display cracked",
            inspectionDone = false
        )
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when fault field is blank`() {
        val entry = RepairEntry(
            customerMobile = "9876543210",
            faultDetected = "",
            inspectionDone = true
        )
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when fault is only whitespace`() {
        val entry = RepairEntry(
            customerMobile = "9876543210",
            faultDetected = "   ",
            inspectionDone = true
        )
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when dealer-only repair even with inspection done`() {
        // Dealer channel: customer fields blank, dealer present.
        val entry = RepairEntry(
            dealerName = "Suresh phones",
            dealerMobile = "9123456789",
            customerName = "",
            customerMobile = "",
            faultDetected = "Camera issue",
            inspectionDone = true
        )
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `fires with whitespace-padded mobile that still has digits`() {
        // Note: isNotBlank() returns true even with leading/trailing spaces — Fragment only sends if customer present.
        val entry = RepairEntry(
            customerMobile = " 9876543210 ",
            faultDetected = "Battery drain",
            inspectionDone = true
        )
        assertTrue(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `placeholder fault values still pass trigger logic`() {
        val entry = RepairEntry(
            customerMobile = "9123456789",
            faultDetected = "Unknown",
            inspectionDone = true
        )
        assertTrue(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire when all conditions missing`() {
        val entry = RepairEntry()
        assertFalse(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `does not fire on default constructed entry without customer`() {
        assertFalse(shouldFireRepairStartedNotification(RepairEntry()))
    }

    // --- Edge: notification sends after entering inspection, even before handover finalAmount ---

    @Test
    fun `fires for entry without finalAmount (handover not yet done)`() {
        val entry = RepairEntry(
            customerMobile = "9123456789",
            faultDetected = "Display issue",
            inspectionDone = true,
            handoverDone = false
        )
        assertTrue(shouldFireRepairStartedNotification(entry))
    }

    @Test
    fun `fires regardless of quotationDone stage`() {
        val entry = RepairEntry(
            customerMobile = "9123456789",
            faultDetected = "Charging port",
            inspectionDone = true,
            quotationDone = false
        )
        assertTrue(shouldFireRepairStartedNotification(entry))
    }
}
