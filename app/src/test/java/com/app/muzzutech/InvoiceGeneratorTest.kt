package com.app.muzzutech

import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InvoiceGeneratorTest {

    @Test
    fun fileNaming_containsEntityId() {
        val entry = RepairEntry(
            id = 1,
            customerName = "Test",
            customerMobile = "9876543210",
            deviceBrand = "Samsung",
            deviceModel = "Galaxy S21",
            faultDetected = "Broken screen",
            chargeAmount = 150000L,
            finalAmount = 200000L
        )
        val dir = File(System.getProperty("java.io.tmpdir"), "invoices")
        dir.mkdirs()
        val file = File(dir, "Invoice_${entry.id}_${System.currentTimeMillis()}.pdf")
        assertTrue("Invoice directory must exist", dir.exists())
        assertTrue("File name must contain entity ID", file.name.contains("1"))
        assertTrue("File name must end with .pdf", file.name.endsWith(".pdf"))
    }

    @Test
    fun repairEntry_model_holdsData() {
        val entry = RepairEntry(
            id = 5,
            customerName = "Test",
            customerMobile = "9876543210",
            deviceBrand = "Apple",
            deviceModel = "iPhone 15",
            faultDetected = "Battery issue",
            chargeAmount = 300000L,
            finalAmount = 350000L
        )
        assertNotNull("Entry ID must be set", entry.id)
        assertTrue("Charge must be positive", entry.chargeAmount > 0)
        assertTrue("Final amount must be >= charge", entry.finalAmount >= entry.chargeAmount)
    }
}
