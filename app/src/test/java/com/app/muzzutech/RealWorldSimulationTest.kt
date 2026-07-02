package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.random.Random

// Data classes to replace Triple (4-element Triple causes compile errors)
data class SupplierDef(val company: String, val name: String, val mobile: String, val city: String)
data class ServiceManDef(val name: String, val mobile: String, val email: String, val designation: String)

// Result types
data class MismatchRecord(val category: String, val description: String, val expected: Any, val actual: Any)
data class ScenarioResult(val name: String, val passed: Boolean, val mismatches: List<MismatchRecord>) {
    fun orFail() {
        if (!passed) fail("FAILED: $name\n" + mismatches.joinToString("\n") { "  [${it.category}] expected=${it.expected}, actual=${it.actual} - ${it.description}" })
    }
}
data class ReconciliationReport(val scenarios: List<ScenarioResult>, val totalMismatches: Int, val overallPassed: Boolean) {
    fun printSummary() {
        println("\n${if (overallPassed) "OVERALL PASS" else "OVERALL FAIL"}")
        scenarios.forEach { println("  ${if (it.passed) "PASS" else "FAIL"} ${it.name} (${it.mismatches.size} mismatches)") }
        println("Total mismatches: $totalMismatches")
    }
}

// *** TEST DATA ONLY - DO NOT USE IN PRODUCTION ***
object TestFixtures {
    val FIRST_NAMES = listOf("Rajesh","Amit","Suresh","Vijay","Ravi","Mohit","Prakash","Deepak","Arjun","Karan",
        "Rahul","Sanjay","Nikhil","Rohit","Aditya","Manish","Pooja","Priya","Neha","Anita",
        "Sunil","Ramesh","Dinesh","Ganesh","Mukesh","Vivek","Ashok","Bharat","Chandan","Devendra",
        "Firoz","Girish","Harish","Ibrahim","Jagdish","Kishore","Lalit","Mohan","Nilesh","Omkar",
        "Pavan","Qasim","Rajiv","Sachin","Tarun","Umesh","Vasant","Yogesh","Zaheer","Anil")
    val LAST_NAMES = listOf("Sharma","Verma","Patel","Singh","Kumar","Gupta","Joshi","Reddy","Rao","Iyer",
        "Naidu","Choudhary","Agarwal","Pandey","Mishra","Tiwari","Yadav","Jain","Bansal","Malhotra")
    fun randomMobile(idx: Int): String = "98${(10000000 + idx).toString().padStart(8, '0')}"
    data class PartDef(val name: String, val category: String, val costPrice: Double, val retailPrice: Double)
    val PARTS_CATALOG = listOf(
        PartDef("Samsung Display OLED", "Display", 3200.0, 5500.0),
        PartDef("Redmi Display LCD", "Display", 1500.0, 2800.0),
        PartDef("iPhone OLED Display", "Display", 5000.0, 8500.0),
        PartDef("Generic Battery 4000mAh", "Battery", 350.0, 700.0),
        PartDef("Samsung Original Battery", "Battery", 800.0, 1400.0),
        PartDef("Generic Battery 3000mAh", "Battery", 280.0, 550.0),
        PartDef("USB-C Charging Port", "Charging", 180.0, 400.0),
        PartDef("Lightning Charging Port", "Charging", 450.0, 900.0),
        PartDef("Wireless Qi Module", "Charging", 600.0, 1200.0),
        PartDef("Rear Camera 48MP", "Camera", 1100.0, 2000.0),
        PartDef("Front Camera 12MP", "Camera", 500.0, 950.0),
        PartDef("iPhone Triple Camera", "Camera", 2200.0, 3800.0),
        PartDef("Power Button Flex", "Button", 150.0, 350.0),
        PartDef("Volume Button Flex", "Button", 100.0, 250.0),
        PartDef("Reconditioned Motherboard", "Board", 3000.0, 5500.0)
    )
    val PHONE_MODELS = listOf("Samsung Galaxy S21","Samsung Galaxy A54","Redmi Note 12","Redmi 13 Pro",
        "iPhone 14","iPhone 13","OnePlus Nord CE 3","OnePlus 11R",
        "Vivo V27","Realme 12 Pro","Poco X6","Motorola Edge 40",
        "Nothing Phone 2","Asus ROG Phone 7","Google Pixel 8")
    val BRANDS = listOf("Samsung","Apple","Xiaomi","Redmi","OnePlus","Vivo","Realme","Poco","Motorola","Nothing")
    val SUPPLIER_DATA = listOf(
        SupplierDef("Rajesh Mobile Parts", "Rajesh Sharma", "9398123456", "Mumbai"),
        SupplierDef("Sharma Electronics", "Mukesh Sharma", "9845123456", "Delhi"),
        SupplierDef("Patel Distributors", "Kiran Patel", "9876123456", "Ahmedabad"),
        SupplierDef("Gupta Spare Mart", "Suresh Gupta", "9824123456", "Kolkata"),
        SupplierDef("Singh Mobile Hub", "Ravi Singh", "9811123456", "Delhi"),
        SupplierDef("Reddy Components", "Srinivas Reddy", "9000123456", "Hyderabad"),
        SupplierDef("Joshi Electronics", "Prakash Joshi", "9762123456", "Pune"),
        SupplierDef("Kumar Accessories", "Ajit Kumar", "9544123456", "Chennai"),
        SupplierDef("Malhotra Tech Traders", "Anil Malhotra", "9310123456", "Jaipur"),
        SupplierDef("Iyer Mobile Supplies", "Venkat Iyer", "9688123456", "Bangalore")
    )
    val SERVICE_MAN_DATA = listOf(
        ServiceManDef("Ravi Kumar Sharma", "9900111222", "ravi@muzzutech.com", "Senior Technician"),
        ServiceManDef("Mohit Verma", "9900111333", "mohit@muzzutech.com", "Technician"),
        ServiceManDef("Deepak Joshi", "9900111444", "deepak@muzzutech.com", "Junior Technician"),
        ServiceManDef("Sachin Patel", "9900111555", "sachin@muzzutech.com", "Technician"),
        ServiceManDef("Nikhil Gupta", "9900111666", "nikhil@muzzutech.com", "Apprentice")
    )
    fun randomPersonName(): String = "${FIRST_NAMES.random()} ${LAST_NAMES.random()}"
    fun randomPhoneModel(): String = PHONE_MODELS.random()
    fun randomBrand(): String = BRANDS.random()
}

@RunWith(RobolectricTestRunner::class)
class RealWorldSimulationTest {

    private lateinit var db: AppDatabase
    private lateinit var repairDao: RepairEntryDao
    private lateinit var serviceManDao: ServiceManDao
    private lateinit var supplierDao: SupplierDao
    private lateinit var commonFaultDao: CommonFaultDao
    private lateinit var sparePartDao: SparePartPurchaseDao
    private lateinit var customerDao: CustomerDao
    private lateinit var dealerDao: DealerDao
    private lateinit var saleDao: SaleDao
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var partReturnDao: PartReturnDao
    private lateinit var paymentTxnDao: PaymentTransactionDao

    data class SimState(
        val serviceManIds: MutableList<Long> = mutableListOf(),
        val supplierMobiles: MutableList<String> = mutableListOf(),
        val partPurchaseIds: MutableList<Long> = mutableListOf(),
        val supplierPaymentIds: MutableList<Long> = mutableListOf(),
        val customerMobiles: MutableList<String> = mutableListOf(),
        val repairEntryIds: MutableList<Long> = mutableListOf(),
        val cancelledRepairIds: MutableList<Long> = mutableListOf(),
        val partReturnIds: MutableList<Long> = mutableListOf(),
        val directSaleIds: MutableList<Long> = mutableListOf(),
        val dailyCashLedger: MutableList<DayLedger> = mutableListOf(),
        val refundPaymentIds: MutableList<Long> = mutableListOf(),
        val supplierReturnIds: MutableList<Long> = mutableListOf(),
        var directSaleCount: Int = 0
    )
    data class DayLedger(val dayIndex: Int, val cashIn: Double = 0.0, val cashOut: Double = 0.0, val personalWithdrawal: Double = 0.0) {
        val net: Double get() = cashIn - cashOut
    }
    private val state = SimState()
    private val MILLIS_PER_DAY = 86_400_000L

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repairDao = db.repairEntryDao()
        serviceManDao = db.serviceManDao()
        supplierDao = db.supplierDao()
        commonFaultDao = db.commonFaultDao()
        sparePartDao = db.sparePartPurchaseDao()
        customerDao = db.customerDao()
        dealerDao = db.dealerDao()
        saleDao = db.saleDao()
        userProfileDao = db.userProfileDao()
        paymentDao = db.paymentDao()
        partReturnDao = db.partReturnDao()
        paymentTxnDao = db.paymentTransactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { db.close() }

    private fun clearAllData() = runBlocking {
        paymentTxnDao.getAllTransactions().first().forEach { paymentTxnDao.delete(it) }
        paymentDao.getAllPayments().first().forEach { paymentDao.delete(it) }
        partReturnDao.getAllReturns().first().forEach { partReturnDao.delete(it) }
        // NOTE: SaleDao has no @Delete - Sale entities cleared via fresh in-memory DB per @Before
        saleDao.getAllSales().first()
        sparePartDao.getAllPurchases().first().forEach { sparePartDao.delete(it) }
        repairDao.getAllEntries().first().forEach { repairDao.delete(it) }
        serviceManDao.getAllServiceMen().first().forEach { serviceManDao.delete(it) }
        supplierDao.getAllSuppliers().first().forEach { supplierDao.delete(it) }
        customerDao.getAllCustomers().first().forEach { customerDao.deleteByMobile(it.mobileNumber) }
        userProfileDao.getUserProfile()?.let {
            userProfileDao.insertOrUpdate(it.copy(shopName = "", name = "", email = "", phone = "", shopAddress = "", gstNo = ""))
        }
        state.serviceManIds.clear()
        state.supplierMobiles.clear()
        state.partPurchaseIds.clear()
        state.supplierPaymentIds.clear()
        state.customerMobiles.clear()
        state.repairEntryIds.clear()
        state.cancelledRepairIds.clear()
        state.partReturnIds.clear()
        state.directSaleIds.clear()
        state.dailyCashLedger.clear()
        state.refundPaymentIds.clear()
        state.supplierReturnIds.clear()
        state.directSaleCount = 0
    }

    @Test
    fun clearAllTestData() {
        clearAllData()
        println("[CLEAR] All test data wiped.")
    }

    private fun daysAgo(daysAgo: Int, hourOfDay: Int = 10): Long {
        return System.currentTimeMillis() - daysAgo * MILLIS_PER_DAY + hourOfDay * 3600_000L
    }

    // SCENARIO 1: ShopKeeper Setup
    @Test
    fun scenario01_shopKeeperSetup() = runBlocking {
        clearAllData()
        println("\nSCENARIO 01: SHOPKEEPER/OWNER SETUP")

        val profile = UserProfile(id = 1,
            shopName = "MuzzuTech Mobile Repair", name = "Rahman bhai",
            email = "rahman@muzzutech.in", phone = "9876543210",
            shopAddress = "Shop No. 12, MG Road, Bangalore - 560001", gstNo = "29ABCDE1234F1ZX")
        userProfileDao.insertOrUpdate(profile)
        val stored = userProfileDao.getUserProfile()
        assertNotNull("Shop profile must be saved", stored)
        assertEquals("MuzzuTech Mobile Repair", stored!!.shopName)

        TestFixtures.SERVICE_MAN_DATA.forEach { sm ->
            val id = serviceManDao.insert(ServiceMan(name = sm.name, mobile = sm.mobile, email = sm.email,
                employeeId = "SM-${1000 + state.serviceManIds.size + 1}", designation = sm.designation))
            state.serviceManIds.add(id)
        }
        assertEquals(5, state.serviceManIds.size)
        val allSM = serviceManDao.getAllServiceMen().first()
        assertEquals(5, allSM.size)
        assertTrue("All service men active", allSM.all { it.isActive })
        println("  5 service men registered")
    }

    // SCENARIO 2: 10 Suppliers + Inventory (60-120 orders, mixed paid/unpaid)
    @Test
    fun scenario02_supplierAndInventory() = runBlocking {
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        println("\nSCENARIO 02: SUPPLIERS + INVENTORY")
        val rng = Random(12345)

        TestFixtures.SUPPLIER_DATA.forEach { sup ->
            supplierDao.insert(Supplier(mobile = sup.mobile, name = sup.name,
                companyName = sup.company, email = "${sup.name.replace(" ","").lowercase()}@${sup.company.split(" ")[0].lowercase()}.in",
                city = sup.city))
            state.supplierMobiles.add(sup.mobile)
        }
        assertEquals(10, state.supplierMobiles.size)

        var totalQty = 0
        var orderCounter = 0
        var forcedUnpaidCount = 0
        val targetUnpaid = 3
        state.supplierMobiles.forEach { mobile ->
            val sup = supplierDao.getSupplierByMobile(mobile)!!
            val orders = 6 + rng.nextInt(7)
            repeat(orders) {
                orderCounter++
                val part = TestFixtures.PARTS_CATALOG.random()
                val qty = 1 + rng.nextInt(20)
                val cost = kotlin.math.round(part.costPrice * (0.9 + rng.nextDouble() * 0.2) * 100.0) / 100.0
                // Force last 3 orders to be unpaid regardless of random, to ensure reconciliation always has something to verify
                val forceUnpaid = forcedUnpaidCount < targetUnpaid && (orderCounter >= state.supplierMobiles.size * 7 - (targetUnpaid - 1))
                val isPaid = !forceUnpaid && rng.nextDouble() < 0.55

                val pid = sparePartDao.insert(SparePartPurchase(
                    repairEntryId = 0L, partName = part.name,
                    purchasePrice = cost, supplierId = mobile, supplierName = sup.name, quantity = qty))
                state.partPurchaseIds.add(pid)
                totalQty += qty

                if (!isPaid) {
                    val amt = kotlin.math.round(cost * qty * 100.0) / 100.0
                    paymentDao.insert(Payment(
                        personType = "SUPPLIER", personMobile = mobile, personName = sup.name,
                        description = "Purchase - ${part.name}", totalAmount = amt,
                        paidAmount = 0.0, dueAmount = amt, status = "UNPAID"))
                    if (forceUnpaid) forcedUnpaidCount++
                } else {
                    val amt = kotlin.math.round(cost * qty * 100.0) / 100.0
                    val payId = paymentDao.insert(Payment(
                        personType = "SUPPLIER", personMobile = mobile, personName = sup.name,
                        description = "Purchase - ${part.name}", totalAmount = amt,
                        paidAmount = amt, dueAmount = 0.0, status = "PAID"))
                    state.supplierPaymentIds.add(payId)
                    paymentTxnDao.insert(PaymentTransaction(
                        paymentId = payId, personType = "SUPPLIER", personMobile = mobile,
                        personName = sup.name, amount = amt,
paymentMode = if (rng.nextBoolean()) "CASH" else "ONLINE",
			transactionDate = daysAgo(25 + rng.nextInt(5), 11)))
                }
            }
        }
        val unpaid = paymentDao.getAllPayments().first().filter { it.status != "PAID" }
        assertTrue("At least 3 supplier payments must be unpaid for meaningful reconciliation", unpaid.size >= 3)
        println("  $totalQty items across ${state.partPurchaseIds.size} purchase orders (${unpaid.size} unpaid)")
    }

    // SCENARIO 3: Customer + Repair Flow (80 repairs across 4 service men)
    @Test
    fun scenario03_customerRepairFlow() = runBlocking {
        if (state.supplierMobiles.isEmpty()) scenario02_supplierAndInventory()
        clearRepairTables()
        println("\nSCENARIO 03: CUSTOMER + REPAIR FLOW")
        val rng = Random(54321)
        var idx = 0

        state.supplierMobiles.forEach { mobile ->
            val sup = supplierDao.getSupplierByMobile(mobile)!!
            repeat(8) {
                idx++
                val mob = TestFixtures.randomMobile(idx)
                customerDao.insert(Customer(mobileNumber = mob, name = TestFixtures.randomPersonName(), city = sup.city))
                state.customerMobiles.add(mob)

                val smId = state.serviceManIds.random()
                val sm = serviceManDao.getServiceManById(smId)!!
                val part1 = TestFixtures.PARTS_CATALOG.random()
                val part2 = if (rng.nextBoolean()) TestFixtures.PARTS_CATALOG.filter { it.name != part1.name }.random() else null
                val parts = listOf(part1) + if (part2 != null) listOf(part2) else emptyList()

                val labor = (500 + rng.nextInt(2500)).toDouble()
                val partsCharge = parts.sumOf { it.retailPrice }
                val charge = partsCharge + labor
                val advance = if (rng.nextDouble() < 0.60) kotlin.math.round(charge * (0.3 + rng.nextDouble() * 0.4) * 100.0) / 100.0 else 0.0
                val cancelled = rng.nextDouble() < 0.06
                val returned = !cancelled && rng.nextDouble() < 0.08
                val done = !cancelled && rng.nextDouble() < 0.88

                val status = when { cancelled -> "Cancelled"; done -> "Done"; else -> "In Progress" }
                val entryId = repairDao.insert(RepairEntry(
                    customerMobile = mob, customerName = TestFixtures.randomPersonName(), customerCity = sup.city,
                    deviceBrand = TestFixtures.randomBrand(), deviceModel = TestFixtures.randomPhoneModel(),
                    faultDetected = listOf("Screen broken","Battery drain","Charging failure","Camera malfunction","Button stuck","Water damage","No power","Slow performance").random(),
                    sparePartName = parts.joinToString(", ") { it.name },
                    sparePartPurchasePrice = partsCharge, chargeAmount = charge, advanceAmount = advance,
                    serviceManId = smId, serviceManName = sm.name,
                    isDraft = false, workStatus = status, handoverDone = done,
                    entryDate = daysAgo(7 + rng.nextInt(18)),
                    handoverDate = if (done) daysAgo(rng.nextInt(5)) else 0L,
                    quotationDone = true, sparePartDone = true, workDone = done))
                state.repairEntryIds.add(entryId)

                val balance = charge - advance
                val payStatus = when { cancelled -> "CANCELLED"; balance <= 0.01 -> "PAID"; advance > 0 -> "PARTIAL"; else -> "UNPAID" }
                if (!cancelled || advance > 0 || rng.nextDouble() < 0.3) {
                    val payId = paymentDao.insert(Payment(
                        personType = "CUSTOMER", personMobile = mob, personName = "Customer",
                        description = "Repair: $status", totalAmount = charge,
                        dueAmount = kotlin.math.max(0.0, balance), status = payStatus))
                    if (advance > 0.01) {
                        paymentTxnDao.insert(PaymentTransaction(paymentId = payId, personType = "CUSTOMER",
                            personMobile = mob, personName = "Customer", amount = advance,
                            paymentMode = if (rng.nextBoolean()) "CASH" else "ONLINE"))
                    }
                }
                if (returned && parts.isNotEmpty()) {
                    val rp = parts.random()
                    val rq = 1 + rng.nextInt(3)
                    state.partReturnIds.add(partReturnDao.insert(PartReturn(
                        supplierId = mobile, supplierName = sup.name,
                        partName = "${rp.name} (returned)", returnReason = "Customer cancelled",
                        refundAmount = rp.costPrice * rq)))
                }
                if (cancelled) state.cancelledRepairIds.add(entryId)
            }
        }
        println("  ${state.customerMobiles.size} customers, ${state.repairEntryIds.size} repairs, ${state.cancelledRepairIds.size} cancelled")
    }

    private suspend fun clearRepairTables() {
        paymentTxnDao.getAllTransactions().first().forEach { paymentTxnDao.delete(it) }
        paymentDao.getAllPayments().first().forEach { paymentDao.delete(it) }
        partReturnDao.getAllReturns().first().forEach { partReturnDao.delete(it) }
        // SaleDao no @Delete - cleared by fresh in-memory DB
        saleDao.getAllSales().first()
        sparePartDao.getAllPurchases().first().forEach { sparePartDao.delete(it) }
        repairDao.getAllEntries().first().forEach { repairDao.delete(it) }
        customerDao.getAllCustomers().first().forEach { customerDao.deleteByMobile(it.mobileNumber) }
        state.customerMobiles.clear()
        state.repairEntryIds.clear()
        state.cancelledRepairIds.clear()
        state.partReturnIds.clear()
        state.directSaleIds.clear()
        state.directSaleCount = 0
        state.partPurchaseIds.clear()
        state.supplierMobiles.clear()
        state.supplierPaymentIds.clear()
    }

    // SCENARIO 4: 30 Direct Walk-in Sales
    @Test
    fun scenario04_directSales() = runBlocking {
        println("\nSCENARIO 04: DIRECT SALES")
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        if (state.supplierMobiles.isEmpty()) scenario02_supplierAndInventory()

        val rng = Random(99999)
        repeat(30) {
            val part = TestFixtures.PARTS_CATALOG.random()
            val qty = 1 + rng.nextInt(4)
            val sPrice = kotlin.math.round(part.retailPrice * (1.0 + rng.nextDouble() * 0.5) * qty * 100.0) / 100.0
            val paid = kotlin.math.round(sPrice * (if (rng.nextDouble() < 0.75) 1.0 else rng.nextDouble() * 0.5) * 100.0) / 100.0
            val sup = TestFixtures.SUPPLIER_DATA.random()

            val saleDate = daysAgo(18 - (it % 18), 12 + rng.nextInt(6))
            saleDao.insert(Sale(
                itemName = "${part.name} x$qty", supplierId = sup.mobile, supplierName = sup.company,
                purchasePrice = part.costPrice * qty, salePrice = sPrice,
                paidToSupplier = kotlin.math.round(part.costPrice * qty * paid / sPrice * 100.0) / 100.0,
customerPaid = paid,
		saleDate = saleDate))
            state.directSaleCount++
        }
        println("  ${state.directSaleCount} direct sales recorded")
        assertEquals(30, saleDao.getAllSales().first().size)
    }

    // SCENARIO 5: 20-day Cash Flow
    @Test
    fun scenario05_cashFlow20Days() = runBlocking {
        println("\nSCENARIO 05: CASH FLOW (20 business days)")
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        if (state.supplierMobiles.isEmpty()) scenario02_supplierAndInventory()
        if (state.repairEntryIds.isEmpty()) scenario03_customerRepairFlow()
        if (state.directSaleCount == 0) scenario04_directSales()

        val rng = Random(20242)
        var cashOnHand = 5000.0

        repeat(20) { day ->
            val dStart = daysAgo(20 - day)
            val dEnd = dStart + MILLIS_PER_DAY - 1
            val repairsToday = repairDao.getCompletedEntries().first().filter { it.handoverDate in dStart until dEnd }
            val cashInRepairs = repairsToday.sumOf { it.advanceAmount + kotlin.math.max(0.0, it.chargeAmount - it.advanceAmount) }
            val salesToday = saleDao.getSalesByDateRange(dStart, dEnd).first()
            val cashInSales = salesToday.sumOf { it.customerPaid }
            val totalIn = cashInRepairs + cashInSales
            val txnsOut = paymentTxnDao.getTransactionsByDateRange(dStart, dEnd).first().filter { it.personType == "SUPPLIER" }
            val totalOut = txnsOut.sumOf { it.amount.coerceAtLeast(0.0) }
            val personal = if (day == 10) (1000.0 + rng.nextInt(9) * 500) else 0.0
            if (personal > 0) println("  Day $day: Personal withdrawal Rs.${personal.toInt()}")
            cashOnHand += totalIn - totalOut - personal
            state.dailyCashLedger.add(DayLedger(day, kotlin.math.round(totalIn * 100.0) / 100.0, kotlin.math.round(totalOut * 100.0) / 100.0, personal))
            if (day < 3 || day >= 17 || personal > 0) {
                val l = state.dailyCashLedger[day]
                println("  Day ${String.format("%2d", day)}: In=Rs.${l.cashIn.toInt().toString().padStart(7)} Out=Rs.${l.cashOut.toInt().toString().padStart(6)} Net=${if (l.net >= 0) "+" else ""}Rs.${l.net.toInt().toString().padStart(7)} Bal=Rs.${cashOnHand.toInt().toString().padStart(8)}")
            }
        }
        assertTrue("Cash on hand must stay positive", cashOnHand >= 0)
        println("  Final Cash on Hand: Rs.${cashOnHand.toInt()}")
    }

    // SCENARIO 6: Returns (5 customer refunds + 3 defective supplier returns)
    @Test
    fun scenario06_returns() = runBlocking {
        println("\nSCENARIO 06: RETURNS")
        if (state.repairEntryIds.size < 5) scenario03_customerRepairFlow()

        val rng = Random(77777)
        val completed = repairDao.getCompletedEntries().first().toMutableList()
        repeat(minOf(5, completed.size)) {
            val entry = completed[it]
            val refund = kotlin.math.round(entry.chargeAmount * (0.5 + rng.nextDouble() * 0.5) * 100.0) / 100.0
            val payId = paymentDao.insert(Payment(
                personType = "CUSTOMER_REFUND", personMobile = entry.customerMobile, personName = entry.customerName,
                description = "Refund - repair #${entry.id}", totalAmount = refund,
                paidAmount = refund, dueAmount = 0.0, status = "REFUNDED", linkedEntryId = entry.id))
            state.refundPaymentIds.add(payId)
            paymentTxnDao.insert(PaymentTransaction(
                paymentId = payId, personType = "CUSTOMER_REFUND", personMobile = entry.customerMobile,
                personName = entry.customerName, amount = -refund, paymentMode = "CASH"))
        }
        println("  ${state.refundPaymentIds.size} customer refunds")

        val swp = state.supplierMobiles.filter { sparePartDao.getPurchasesBySupplier(it).first().isNotEmpty() }
        repeat(minOf(3, swp.size)) { i ->
            val mobile = swp[i]
            val sup = supplierDao.getSupplierByMobile(mobile)!!
            val pp = sparePartDao.getPurchasesBySupplier(mobile).first().random()
            val cp = TestFixtures.PARTS_CATALOG.find { it.name == pp.partName } ?: TestFixtures.PARTS_CATALOG.random()
            val qty = 1 + rng.nextInt(3)
            val refundAmount = kotlin.math.round(cp.costPrice * qty * 100.0) / 100.0
            state.supplierReturnIds.add(partReturnDao.insert(PartReturn(
                supplierId = mobile, supplierName = sup.name,
                partName = cp.name, returnReason = "Defective batch",
                refundAmount = refundAmount)))
            paymentTxnDao.insert(PaymentTransaction(
                paymentId = 0L, personType = "SUPPLIER_CREDIT", personMobile = mobile, personName = sup.name,
                amount = -refundAmount, paymentMode = "ADJUSTMENT",
                transactionDate = daysAgo(5, 10)))
            // update unpaid supplier payment to reflect credit
            val unpaid = paymentDao.getAllPayments().first()
                .firstOrNull { it.personMobile == mobile && it.personType == "SUPPLIER" && it.status == "UNPAID" }
            if (unpaid != null) {
                paymentDao.update(unpaid.copy(
                    dueAmount = kotlin.math.max(0.0, unpaid.dueAmount - refundAmount)
                ))
            }
        }
        println("  ${state.supplierReturnIds.size} supplier returns")
    }

    // SCENARIO 7: Independent Reconciliation Engine
    @Test
    fun scenario07_reconciliation() = runBlocking {
        println("\nSCENARIO 07: INDEPENDENT RECONCILIATION")
        val mismatches = mutableListOf<MismatchRecord>()

        // 7a: Inventory
        val stockIn = sparePartDao.getAllPurchases().first().groupBy { it.partName }.mapValues { (_, v) -> v.sumOf { it.quantity } }
        val stockOutSales = saleDao.getAllSales().first()
            .flatMap { s -> s.itemName.split(" x").let { p -> if (p.size > 1) listOf(p[0].trim() to p[1].toInt()) else emptyList() } }
            .groupBy { it.first }.mapValues { (_, v) -> v.sumOf { it.second } }
        val stockOutRepair = repairDao.getAllEntries().first()
            .filter { !it.isDraft && it.sparePartName.isNotBlank() }
            .flatMap { e -> e.sparePartName.split(",").map { it.trim() } }
            .groupBy { it }.mapValues { (_, v) -> v.size }
        val returnsIn = partReturnDao.getAllReturns().first().groupBy { it.partName }.mapValues { (_, v) -> v.size }

        (stockIn.keys + stockOutSales.keys + stockOutRepair.keys + returnsIn.keys).distinct().forEach { pn ->
            val net = (stockIn[pn] ?: 0) - (stockOutSales[pn] ?: 0) - (stockOutRepair[pn] ?: 0) + (returnsIn[pn] ?: 0)
            if (net < 0) mismatches.add(MismatchRecord("Inventory", "Negative stock '$pn'", net, "NEGATIVE"))
        }
        println("  Inventory: ${(stockIn.keys + stockOutSales.keys + stockOutRepair.keys).distinct().size} part types, ${mismatches.count { it.category == "Inventory" }} issues")

        // 7b: Revenue
        val revApp = repairDao.getRevenueInRange(daysAgo(20), System.currentTimeMillis()).first() ?: 0.0
        val revCalc = repairDao.getCompletedEntries().first().sumOf { it.chargeAmount }
        if (kotlin.math.abs(revApp - revCalc) > 0.01) mismatches.add(MismatchRecord("Revenue-Repairs", "getRevenueInRange=$revApp vs sum=$revCalc", revCalc, revApp))

        // 7c: Supplier Dues
        val supPays = paymentDao.getAllPayments().first().filter { it.personType == "SUPPLIER" }
        var dueCalc = 0.0
        supPays.forEach { p ->
            val txns = paymentTxnDao.getTransactionsByPayment(p.id).first()
            val paid = txns.filter { it.amount > 0 }.sumOf { it.amount }
            val cred = txns.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            val due = kotlin.math.max(0.0, p.totalAmount - paid + cred)
            dueCalc += due
            if (kotlin.math.abs(due - p.dueAmount) > 0.01)
                mismatches.add(MismatchRecord("Supplier-Due", "Payment #${p.id}: calc=$due stored=${p.dueAmount}", due, p.dueAmount))
        }

        // 7d: Customer Payments
        val custPays = paymentDao.getAllPayments().first().filter { it.personType == "CUSTOMER" && it.status != "CANCELLED" }
        var custPaidCalc = 0.0
        custPays.forEach { p ->
            val paid = paymentTxnDao.getTransactionsByPayment(p.id).first().filter { it.amount > 0 }.sumOf { it.amount }
            custPaidCalc += paid
            if (kotlin.math.abs(paid - p.paidAmount) > 0.01)
                mismatches.add(MismatchRecord("Cust-Payment", "Payment #${p.id}", paid, p.paidAmount))
        }

        val totalRevenue = revCalc + saleDao.getAllSales().first().sumOf { it.customerPaid }
        val totalCogs = supPays.sumOf { it.totalAmount }
        println("  Revenue=Rs.${totalRevenue.toInt()}  COGS=Rs.${totalCogs.toInt()}  Profit=Rs.${(totalRevenue - totalCogs).toInt()}")
        println("  SupplierDue(calc)=Rs.${dueCalc.toInt()}  CustPaid(calc)=Rs.${custPaidCalc.toInt()}")

        val report = ReconciliationReport(listOf(
            ScenarioResult("Inventory", mismatches.none { it.category == "Inventory" }, mismatches.filter { it.category == "Inventory" }),
            ScenarioResult("Revenue", mismatches.none { it.category.startsWith("Revenue") }, mismatches.filter { it.category.startsWith("Revenue") }),
            ScenarioResult("Supplier-Due", mismatches.none { it.category.startsWith("Supplier-Due") }, mismatches.filter { it.category.startsWith("Supplier-Due") }),
            ScenarioResult("Cust-Payments", mismatches.none { it.category.startsWith("Cust") }, mismatches.filter { it.category.startsWith("Cust") })
        ), mismatches.size, mismatches.isEmpty())
        report.printSummary()
        mismatches.forEach { println("MISMATCH [${it.category}]: expected=${it.expected}, actual=${it.actual} - ${it.description}") }
        assertTrue("Reconciliation FAILED: ${mismatches.size} mismatch(es)", mismatches.isEmpty())
    }

    // SCENARIO 8: Full End-to-End
    @Test
    fun scenario08_fullEndToEndRealWorld() = runBlocking {
        println("\nFULL REAL-WORLD SIMULATION - ALL 8 SCENARIOS")
        clearAllData()
        scenario01_shopKeeperSetup()
        scenario02_supplierAndInventory()
        scenario03_customerRepairFlow()
        scenario04_directSales()
        scenario05_cashFlow20Days()
        scenario06_returns()
        scenario07_reconciliation()
        println("\nSIMULATION COMPLETE")
        println("  ServiceMen: ${state.serviceManIds.size}  Suppliers: ${state.supplierMobiles.size}")
        println("  PartPurchases: ${state.partPurchaseIds.size}  Customers: ${state.customerMobiles.size}")
        println("  Repairs: ${state.repairEntryIds.size}  Cancelled: ${state.cancelledRepairIds.size}")
        println("  PartReturns: ${state.partReturnIds.size}  DirectSales: ${state.directSaleCount}")
        println("  Refunds: ${state.refundPaymentIds.size}  SupplierReturns: ${state.supplierReturnIds.size}")
        println("  BusinessDays: ${state.dailyCashLedger.size}")
    }
}
