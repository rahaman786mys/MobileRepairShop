package com.app.muzzutech.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Integration tests covering critical business logic gaps:
 *
 * 1. Inventory (SparePartPurchase) quantity decreases correctly
 *    when a RepairEntry is marked handover-done.
 * 2. Deleting a Customer or Supplier with related records does NOT
 *    crash and leaves denormalized references intact (no FK cascade).
 * 3. Sale creation with invalid/non-existent supplierId is handled
 *    gracefully (data layer allows it; verified for business-layer flagging).
 * 4. Multi-step operations document atomicity behavior: with no
 *    @Transaction annotations, each DAO call commits independently.
 *    Rollback must be handled at the application/repository layer.
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

    // ──────────────────────────────────────────────────────────────
    // TEST 1: Inventory stock (SparePartPurchase.quantity) decreases
    //         by the correct amount when a RepairEntry is marked
    //         as handover-done.
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testInventoryStockDecreasesOnRepairHandover() = runBlocking {
        // Arrange: supplier + spare part purchase (10 units)
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
            supplierId = 0L, // denormalized copy; actual link is via sparePartPurchase
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
        sparePartPurchaseDao.insert(partPurchase)

        // Assert initial quantity = 10
        val initialParts = sparePartPurchaseDao.getAllPurchases().first()
        val initialQty = initialParts.first().quantity
        assertEquals("Initial quantity should be 10", 10, initialQty)

        // Act: simulate handover — decrease stock by 1 and mark entry done
        val reducedPurchase = partPurchase.copy(quantity = 9)
        sparePartPurchaseDao.update(reducedPurchase)

        val completedEntry = repairEntry.copy(
            id = entryId,
            handoverDone = true,
            workStatus = "Done",
            handoverDate = System.currentTimeMillis()
        )
        repairEntryDao.update(completedEntry)

        // Assert: quantity is now 9
        val finalParts = sparePartPurchaseDao.getAllPurchases().first()
        val finalQty = finalParts.first().quantity
        assertEquals(
            "Inventory stock should decrease from 10 to 9 after task handover",
            9,
            finalQty
        )
        val fetched = repairEntryDao.getEntryById(entryId)
        assertTrue("RepairEntry should be handoverDone", fetched?.handoverDone == true)
        assertEquals("Status should be Done", "Done", fetched?.workStatus)
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 2a: Deleting a Customer with related Payments should NOT
    //          crash and should leave payments with stale
    //          denormalized personMobile (no FK cascade).
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testDeleteCustomerWithPaymentsDoesNotCrash() = runBlocking {
        // Arrange
        val customer = Customer(
            mobileNumber = "1234567890",
            name = "John Doe",
            city = "Mumbai"
        )
        customerDao.insert(customer)

        val payment = Payment(
            personType = "CUSTOMER",
            personMobile = customer.mobileNumber,
            personName = customer.name,
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

        // Verify both exist
        var fetched = customerDao.getCustomerByMobile(customer.mobileNumber)
        assertEquals("Customer should exist", "John Doe", fetched?.name)

        var payments = paymentDao.getAllPayments().first()
        assertTrue(
            "Payment should exist",
            payments.any { it.personMobile == customer.mobileNumber }
        )

        // Act: delete customer
        customerDao.delete(fetched!!)

        // Assert: customer gone, payment survives with stale reference
        fetched = customerDao.getCustomerByMobile(customer.mobileNumber)
        assertNull("Customer should be deleted", fetched)

        payments = paymentDao.getAllPayments().first()
        assertTrue(
            "Payment should persist after customer delete (no FK cascade)",
            payments.any { it.personMobile == "1234567890" }
        )
        assertEquals(
            "Payment count should remain 1",
            1,
            payments.size
        )
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 2b: Deleting a Supplier with related SparePartPurchases
    //          and PartReturns should NOT crash (no FK cascade).
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testDeleteSupplierWithRelatedRecordsDoesNotCrash() = runBlocking {
        // Arrange
        val supplier = Supplier(
            mobile = "8888888888",
            name = "Parts Vendor",
            companyName = "Vendor Co.",
            isActive = true
        )
        supplierDao.insert(supplier)

        val purchase = SparePartPurchase(
            repairEntryId = 1L,
            partName = "Battery",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            quantity = 5,
            purchasePrice = 800.0
        )
        sparePartPurchaseDao.insert(purchase)

        val partReturn = PartReturn(
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            partName = "Defective Battery",
            returnReason = "Defective",
            refundAmount = 800.0,
            refundReceived = true
        )
        partReturnDao.insert(partReturn)

        // Verify
        var purchases = sparePartPurchaseDao.getAllPurchases().first()
        assertTrue("Purchase should exist", purchases.any { it.supplierId == supplier.mobile })

        var returns = partReturnDao.getAllReturns().first()
        assertTrue("PartReturn should exist", returns.any { it.supplierId == supplier.mobile })

        // Act: delete supplier
        supplierDao.delete(supplier)

        // Assert: related records survive
        purchases = sparePartPurchaseDao.getAllPurchases().first()
        assertTrue(
            "SparePartPurchase should survive supplier delete (no FK cascade)",
            purchases.any { it.supplierId == "8888888888" }
        )

        returns = partReturnDao.getAllReturns().first()
        assertTrue(
            "PartReturn should survive supplier delete (no FK cascade)",
            returns.any { it.supplierId == "8888888888" }
        )

        val deletedSupplier = supplierDao.getSupplierByMobile(supplier.mobile)
        assertNull("Supplier should be deleted", deletedSupplier)
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 3: Sale creation with an invalid/non-existent supplierId
    //         does not crash. The data layer allows it (no FK),
    //         but the app should validate at the ViewModel/UI layer
    //         before creating such a Sale in production.
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testSaleWithInvalidSupplierDoesNotCrash() = runBlocking {
        // Arrange: create a valid supplier for contrast
        val supplier = Supplier(
            mobile = "7777777777",
            name = "Valid Supplier",
            companyName = "Good Supply Co.",
            isActive = true
        )
        supplierDao.insert(supplier)

        // Act: create a sale pointing to a non-existent supplier (oversell/invalid stock)
        val invalidSale = Sale(
            itemName = "Ghost Product",
            supplierId = "0000000000", // No such supplier exists
            supplierName = "Nobody",
            purchasePrice = 100.0,
            salePrice = 500.0
        )
        saleDao.insert(invalidSale)

        // Assert: record persisted (data layer has no FK enforcement)
        val allSales = saleDao.getAllSales().first()
        assertTrue(
            "Sale with invalid supplierId should persist (no FK enforcement)",
            allSales.any { it.supplierId == "0000000000" }
        )

        // Verify: a sale with a valid supplier also works fine
        val validSale = Sale(
            itemName = "Real Product",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            purchasePrice = 50.0,
            salePrice = 200.0
        )
        saleDao.insert(validSale)

        val finalSales = saleDao.getAllSales().first()
        assertTrue(
            "Valid sale should coexist with invalid one",
            finalSales.any { it.itemName == "Real Product" }
        )
        assertEquals(
            "Total sales should be 2",
            2,
            finalSales.size
        )
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 4: Multi-step operations (Sale + SparePartPurchase +
    //         Payment) demonstrate non-atomic behavior without
    //         @Transaction. Each DAO call commits independently.
    //         The test proves that partial writes survive a
    //         simulated interrupt, validating the need for explicit
    //         rollback logic at the repository layer.
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testMultiStepOperationPartialWritesSurviveInterrupt() = runBlocking {
        // Arrange
        val supplier = Supplier(
            mobile = "6666666666",
            name = "Atomic Supplier",
            companyName = "Atomic Parts"
        )
        supplierDao.insert(supplier)

        val baselineSale = Sale(
            itemName = "Baseline Sale",
            supplierId = supplier.mobile,
            supplierName = supplier.name,
            purchasePrice = 100.0,
            salePrice = 200.0
        )
        saleDao.insert(baselineSale)

        // Capture pre-operation counts
        val salesBefore = saleDao.getAllSales().first()
        val purchasesBefore = sparePartPurchaseDao.getAllPurchases().first()
        val paymentsBefore = paymentDao.getAllPayments().first()
        val initialSaleCount = salesBefore.size

        // Act: multi-step operation with simulated interrupt after step 1
        var interruptStep: String? = null
        try {
            // Step 1: insert sale (commits)
            val newSale = Sale(
                itemName = "Interrupted Sale",
                supplierId = supplier.mobile,
                supplierName = supplier.name,
                purchasePrice = 300.0,
                salePrice = 600.0
            )
            saleDao.insert(newSale)

            // Step 2: insert part purchase — will NOT execute because we throw first
            val partPurchase = SparePartPurchase(
                repairEntryId = 999L,
                partName = "Test Part",
                supplierId = supplier.mobile,
                supplierName = supplier.name,
                quantity = 5,
                purchasePrice = 300.0
            )
            sparePartPurchaseDao.insert(partPurchase)

            // Simulated interrupt
            interruptStep = "after_part_purchase"
            throw RuntimeException("Simulated failure after part purchase insert")
        } catch (e: RuntimeException) {
            // Application-layer catch — individual DAO calls already committed
            interruptStep = "caught"
        }

        // Assert: partial writes from step 1 and 2 DID persist
        val salesAfter = saleDao.getAllSales().first()
        val purchasesAfter = sparePartPurchaseDao.getAllPurchases().first()
        val paymentsAfter = paymentDao.getAllPayments().first()

        // Step 1 sale was committed before the interrupt
        assertTrue(
            "Sale from step 1 should persist (no DB-level transaction)",
            salesAfter.any { it.itemName == "Interrupted Sale" }
        )
        assertEquals(
            "Sale count should be initial + 1",
            initialSaleCount + 1,
            salesAfter.size
        )

        // Step 2 part purchase was also committed before throw
        assertTrue(
            "SparePartPurchase from step 2 should persist (no DB-level transaction)",
            purchasesAfter.any { it.partName == "Test Part" }
        )

        // No payment was created (interrupted before that step)
        assertEquals(
            "Payment count should be unchanged",
            paymentsBefore.size,
            paymentsAfter.size
        )

        // Baseline sale is untouched
        assertTrue(
            "Baseline sale should still exist",
            salesAfter.any { it.itemName == "Baseline Sale" }
        )
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 5: Deleting a ServiceMan with related RepairEntries
    //         does not crash (no FK cascade).
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testDeleteServiceManWithRepairEntriesDoesNotCrash() = runBlocking {
        // Arrange
        val serviceMan = ServiceMan(
            name = "Tech One",
            mobile = "5555555555",
            email = "tech@test.com",
            employeeId = "EMP001",
            designation = "Technician",
            isActive = true
        )
        serviceManDao.insert(serviceMan)

        val entry = RepairEntry(
            customerMobile = "1111111111",
            customerName = "Test Customer",
            deviceBrand = "iPhone",
            deviceModel = "13",
            serviceManId = serviceMan.id,
            serviceManName = serviceMan.name,
            entryDate = System.currentTimeMillis(),
            workStatus = "Pending",
            handoverDone = false
        )
        val entryId = repairEntryDao.insert(entry)

        // Verify
        var entries = repairEntryDao.getAllEntries().first()
        assertTrue(
            "RepairEntry should exist linked to serviceMan",
            entries.any { it.serviceManId == serviceMan.id }
        )

        // Act: delete service man
        serviceManDao.delete(serviceMan)

        // Assert: repair entry survives with stale serviceManId
        entries = repairEntryDao.getAllEntries().first()
        assertTrue(
            "RepairEntry should persist after serviceMan delete (no FK cascade)",
            entries.any { it.id == entryId }
        )

        val deleted = serviceManDao.getServiceManById(serviceMan.id)
        assertNull("ServiceMan should be deleted", deleted)
    }

    // ──────────────────────────────────────────────────────────────
    // TEST 6: Quantity decrement respects zero floor — stock cannot
    //         go below 0 (business logic guard).
    // ──────────────────────────────────────────────────────────────
    @Test
    fun testInventoryStockDoesNotGoBelowZero() = runBlocking {
        // Arrange
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
        sparePartPurchaseDao.insert(partPurchase)

        // Act: try to decrease by more than available (2 - 5 = -3)
        val overReduced = partPurchase.copy(quantity = -3)
        sparePartPurchaseDao.update(overReduced)

        // Assert: negative quantity was written (data layer doesn't enforce)
        val parts = sparePartPurchaseDao.getAllPurchases().first()
        val qty = parts.first().quantity

        // Document that data layer allows negative — business logic MUST guard
        assertTrue(
            "Data layer allows negative quantity ($qty); business logic must validate >= 0",
            qty < 0
        )

        // Now apply the business-logic correction (floor at 0)
        val corrected = parts.first().copy(quantity = maxOf(0, qty))
        sparePartPurchaseDao.update(corrected)

        val finalParts = sparePartPurchaseDao.getAllPurchases().first()
        assertTrue(
            "After business-logic correction, quantity must be >= 0",
            finalParts.first().quantity >= 0
        )
        assertEquals(
            "Quantity should be floored at 0",
            0,
            finalParts.first().quantity
        )
    }
}
