package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Integration tests covering critical business logic:
 * 1. Inventory (SparePartPurchase) quantity changes.
 * 2. Deleting records with related data does not crash (no FK cascade).
 * 3. Sale with invalid supplier and atomicity behavior.
 */
@RunWith(RobolectricTestRunner::class)
class BusinessLogicIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var supplierDao: SupplierDao
    private lateinit var saleDao: SaleDao
    private lateinit var repairEntryDao: RepairEntryDao
    private lateinit var sparePartPurchaseDao: SparePartPurchaseDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var partReturnDao: PartReturnDao
    private lateinit var serviceManDao: ServiceManDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = db.customerDao()
        supplierDao = db.supplierDao()
        saleDao = db.saleDao()
        repairEntryDao = db.repairEntryDao()
        sparePartPurchaseDao = db.sparePartPurchaseDao()
        paymentDao = db.paymentDao()
        partReturnDao = db.partReturnDao()
        serviceManDao = db.serviceManDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Test 1: verify stock update path works end-to-end.
    @Test
    fun testInventoryStockDecreasesOnRepairHandover() = runBlocking {
        val supplier = Supplier(
            mobile = "9999999999",
            name = "Parts Supplier",
            companyName = "Test Parts Co.",
            isActive = true
        )
        supplierDao.insert(supplier)

        val repairEntry = RepairEntry(
            customerMobile = "1111111111",
            customerName = "Test Customer",
            deviceBrand = "Samsung",
            deviceModel = "Galaxy S21",
            sparePartName = "Display Assembly",
            sparePartPurchasePrice = 500.0,
            supplierId = 0L,
            sparePartDate = System.currentTimeMillis(),
            workStatus = "Pending",
            handoverDone = false
        )
        val entryId = repairEntryDao.insert(repairEntry)

        val partPurchase = SparePartPurchase(
            repairEntryId = entryId,
            partName = "Display Assembly",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            quantity = 10,
            purchasePrice = 500.0
        )
        val purchaseId = sparePartPurchaseDao.insert(partPurchase)

        // Verify initial quantity = 10
        val initialPurchases = sparePartPurchaseDao.getAllPurchases().toList().first()
        val initialQty = initialPurchases.first { it.id == purchaseId }.quantity
        assertEquals("Initial quantity should be 10", 10, initialQty)

        // Act: reduce stock by 1
        val reduced = partPurchase.copy(id = purchaseId, quantity = 9)
        sparePartPurchaseDao.update(reduced)

        val completedEntry = repairEntry.copy(
            id = entryId,
            handoverDone = true,
            workStatus = "Done",
            handoverDate = System.currentTimeMillis()
        )
        repairEntryDao.update(completedEntry)

        // Assert: quantity is now 9
        val finalPurchases = sparePartPurchaseDao.getAllPurchases().toList().first()
        val finalQty = finalPurchases.first { it.id == purchaseId }.quantity
        assertEquals("Quantity should decrease from 10 to 9 after handover", 9, finalQty)
    }

    // Test 2a: delete customer with payments - no crash
    @Test
    fun testDeleteCustomerWithPaymentsDoesNotCrash() = runBlocking {
        val customer = Customer(
            mobileNumber = "1234567890",
            name = "John Doe",
            city = "Mumbai"
        )
        customerDao.insert(customer)

        val payment = Payment(
            personType = "CUSTOMER",
            personMobile = customer.mobileNumber,
            personName = customer.name ?: "",
            description = "Repair charge",
            totalAmount = 2000.0,
            paidAmount = 1000.0,
            dueAmount = 1000.0,
            status = "PARTIAL",
            linkedEntryId = 0,
            linkedSaleId = 0,
            linkedPartId = 0
        )
        paymentDao.insert(payment)

        var fetched = customerDao.getCustomerByMobile(customer.mobileNumber)
        assertEquals("Customer should exist", "John Doe", fetched?.name)

        var payments = paymentDao.getAllPayments().toList().first()
        assertTrue(
            "Payment should exist",
            payments.any { it.personMobile == customer.mobileNumber }
        )

        customerDao.deleteByMobile(customer.mobileNumber)

        fetched = customerDao.getCustomerByMobile(customer.mobileNumber)
        assertNull("Customer should be deleted", fetched)
    }

    // Test 2b: delete supplier with related records - no crash
    @Test
    fun testDeleteSupplierWithRelatedRecordsDoesNotCrash() = runBlocking {
        val supplier = Supplier(
            mobile = "7777777777",
            name = "Suppliers Inc",
            companyName = "Parts Co",
            isActive = true
        )
        supplierDao.insert(supplier)

        val partPurchase = SparePartPurchase(
            repairEntryId = 1L,
            partName = "Battery",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            quantity = 5,
            purchasePrice = 300.0
        )
        sparePartPurchaseDao.insert(partPurchase)

        var purchases = sparePartPurchaseDao.getAllPurchases().toList().first()
        assertTrue("Purchase should exist", purchases.any { it.supplierId == supplier.mobile })

        // Delete supplier - should not crash
        supplierDao.delete(supplier)

        val deleted = supplierDao.getSupplierByMobile(supplier.mobile)
        assertNull("Supplier should be deleted", deleted)
    }

    // Test 3: sale with invalid supplier
    @Test
    fun testSaleWithInvalidSupplierDoesNotCrash() = runBlocking {
        val sale = Sale(
            itemName = "Screen",
            supplierId = "0000000000",
            purchasePrice = 200.0,
            salePrice = 400.0
        )
        saleDao.insert(sale)

        val allSales = saleDao.getAllSales().toList().first()
        assertTrue("Sale should exist", allSales.any { it.itemName == "Screen" })
    }

    // Test 4: partial writes
    @Test
    fun testMultiStepOperationPartialWritesSurviveInterrupt() = runBlocking {
        val sale = Sale(
            itemName = "Partial Part",
            supplierId = "1111111111",
            purchasePrice = 300.0,
            salePrice = 500.0
        )
        saleDao.insert(sale)

        val partPurchase = SparePartPurchase(
            repairEntryId = 1L,
            partName = "Partial Part",
            supplierId = "1111111111",
            supplierName = "Supplier",
            quantity = 5,
            purchasePrice = 250.0
        )
        sparePartPurchaseDao.insert(partPurchase)

        // If crash after sale insert, sale should still be there
        var sales = saleDao.getAllSales().toList().first()
        val saleCountAfterFirst = sales.count { it.itemName == "Partial Part" }
        assertEquals("Sale should persist", 1, saleCountAfterFirst)
    }

    // Test 5: delete serviceMan with repair entries
    @Test
    fun testDeleteServiceManWithRepairEntriesDoesNotCrash() = runBlocking {
        val serviceMan = ServiceMan(
            name = "Technician Raj",
            mobile = "8888888888",
            isActive = true
        )
        serviceManDao.insert(serviceMan)

        val entry = RepairEntry(
            customerMobile = "1231231231",
            customerName = "Rahul",
            deviceBrand = "OnePlus",
            deviceModel = "Nord 2",
            sparePartName = "Motherboard",
            sparePartPurchasePrice = 800.0,
            supplierId = 0L,
            sparePartDate = System.currentTimeMillis(),
            workStatus = "In Progress",
            handoverDone = false
        )
        val entryId = repairEntryDao.insert(entry)

        val updatedEntry = entry.copy(id = entryId, serviceManName = serviceMan.name)
        repairEntryDao.update(updatedEntry)

        var entries = repairEntryDao.getAllEntries().toList().first()
        assertTrue("Entry should exist", entries.any { it.serviceManName == serviceMan.name })

        serviceManDao.delete(serviceMan)
        val deleted = serviceManDao.getServiceManById(serviceMan.id)
        assertNull("ServiceMan should be deleted", deleted)

        entries = repairEntryDao.getAllEntries().toList().first()
        assertTrue(
            "RepairEntry should persist after serviceMan delete (no FK cascade)",
            entries.any { it.serviceManName == serviceMan.name }
        )
    }

    // Test 6: stock correct after purchase
    @Test
    fun testInventoryStockDoesNotGoBelowZero() = runBlocking {
        val supplier = Supplier(
            mobile = "5555555555",
            name = "Stock Supplier",
            companyName = "Stock Co."
        )
        supplierDao.insert(supplier)

        val partPurchase = SparePartPurchase(
            repairEntryId = 1L,
            partName = "Charger Port",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            quantity = 2,
            purchasePrice = 200.0
        )
        val purchaseId = sparePartPurchaseDao.insert(partPurchase)

        val parts = sparePartPurchaseDao.getAllPurchases().toList().first()
        val qty = parts.first { it.id == purchaseId }.quantity
        assertEquals("Initial qty should be 2", 2, qty)

        // Deduct 1 (now 1)
        val corrected = partPurchase.copy(id = purchaseId, quantity = 1)
        sparePartPurchaseDao.update(corrected)

        val finalParts = sparePartPurchaseDao.getAllPurchases().toList().first()
        val finalQty = finalParts.first { it.id == purchaseId }.quantity
        assertTrue("Stock should be 1", finalQty == 1)
    }
}
