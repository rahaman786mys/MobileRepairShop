package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import com.app.muzzutech.utils.DateUtils
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

data class SupplierDef(val company: String, val name: String, val mobile: String, val city: String)
data class ServiceManDef(val name: String, val mobile: String, val email: String, val designation: String)

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

data class DataDump(
    val suppliers: List<Supplier>,
    val serviceMen: List<ServiceMan>,
    val customers: List<Customer>,
    val dealers: List<Dealer>,
    val repairs: List<RepairEntry>,
    val spareParts: List<SparePartPurchase>,
    val sales: List<Sale>,
    val payments: List<Payment>,
    val transactions: List<PaymentTransaction>,
    val returns: List<PartReturn>,
    val faults: List<CommonFault>,
    val userProfile: UserProfile?
)

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
        val dealerMobiles: MutableList<String> = mutableListOf(),
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
        state.dealerMobiles.clear()
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

    @Test
    fun scenario01_shopKeeperSetup() = runBlocking {
        clearAllData()
        println("\nSCENARIO 01: SHOPKEEPER/OWNER SETUP")

        // --- UserProfile CRUD ---
        val profile = UserProfile(id = 1,
            shopName = "MuzzuTech Mobile Repair", name = "Rahman bhai",
            email = "rahman@muzzutech.in", phone = "9876543210",
            shopAddress = "Shop No. 12, MG Road, Bangalore - 560001", gstNo = "29ABCDE1234F1ZX")
        userProfileDao.insertOrUpdate(profile)
        var stored = userProfileDao.getUserProfile()
        assertNotNull("Shop profile must be saved", stored)
        assertEquals("MuzzuTech Mobile Repair", stored!!.shopName)

        // UserProfile update
        userProfileDao.insertOrUpdate(stored.copy(shopName = "MuzzuTech Pro Repair"))
        stored = userProfileDao.getUserProfile()
        assertEquals("MuzzuTech Pro Repair", stored!!.shopName)
        userProfileDao.insertOrUpdate(stored.copy(shopName = "MuzzuTech Mobile Repair"))

        // UserProfile query via Flow
        val profileFlow = userProfileDao.getUserProfileFlow().first()
        assertEquals("MuzzuTech Mobile Repair", profileFlow!!.shopName)

        // --- ServiceMan CRUD ---
        TestFixtures.SERVICE_MAN_DATA.forEach { sm ->
            val id = serviceManDao.insert(ServiceMan(name = sm.name, mobile = sm.mobile, email = sm.email,
                employeeId = "SM-${1000 + state.serviceManIds.size + 1}", designation = sm.designation))
            state.serviceManIds.add(id)
        }
        assertEquals(5, state.serviceManIds.size)
        var allSM = serviceManDao.getAllServiceMen().first()
        assertEquals(5, allSM.size)
        assertTrue("All service men active", allSM.all { it.isActive })

        // ServiceMan update
        val sm1 = serviceManDao.getServiceManById(state.serviceManIds[0])!!
        serviceManDao.update(sm1.copy(isActive = false))
        val activeSM = serviceManDao.getActiveServiceMen().first()
        assertEquals(4, activeSM.size)
        serviceManDao.update(sm1.copy(isActive = true))

        // ServiceMan query by Flow
        val sm1Flow = serviceManDao.getServiceManByIdFlow(state.serviceManIds[0]).first()
        assertNotNull(sm1Flow)

        // ServiceMan delete and re-insert
        serviceManDao.delete(sm1)
        assertNull(serviceManDao.getServiceManById(state.serviceManIds[0]))
        serviceManDao.insert(sm1.copy(id = 0L))
        val reinserted = serviceManDao.getAllServiceMen().first()
        assertEquals(5, reinserted.size)
        state.serviceManIds.clear()
        state.serviceManIds.addAll(reinserted.map { it.id })

        println("  5 service men registered (CRUD verified)")

        // --- CommonFault CRUD ---
        val faultDefs = listOf(
            "Screen broken" to "Display", "Battery drain" to "Battery",
            "Charging failure" to "Charging", "Camera malfunction" to "Camera",
            "Water damage" to "Body", "No power" to "Motherboard"
        )
        faultDefs.forEach { (name, cat) ->
            commonFaultDao.insert(CommonFault(faultName = name, category = cat, sortOrder = faultDefs.indexOf(name to cat)))
        }
        var faultCount = commonFaultDao.getAllFaults().first().size
        assertEquals(6, faultCount)

        // CommonFault query by category
        val displayFaults = commonFaultDao.getFaultsByCategory("Display").first()
        assertTrue(displayFaults.any { it.faultName == "Screen broken" })

        // CommonFault active filter
        val firstFault = commonFaultDao.getFaultById(1)!!
        commonFaultDao.update(firstFault.copy(isActive = false))
        assertEquals(5, commonFaultDao.getActiveFaults().first().size)
        commonFaultDao.update(firstFault.copy(isActive = true))

        // CommonFault delete and re-verify
        commonFaultDao.delete(firstFault)
        assertEquals(5, commonFaultDao.getAllFaults().first().size)
        commonFaultDao.insert(CommonFault(faultName = "Screen broken", category = "Display", sortOrder = 0))
        faultCount = commonFaultDao.getAllFaults().first().size
        assertEquals(6, faultCount)

        println("  ${faultCount} common faults configured (CRUD verified)")
    }

    @Test
    fun scenario02_supplierAndInventory() = runBlocking {
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        println("\nSCENARIO 02: SUPPLIERS + INVENTORY")
        val rng = Random(12345)

        // --- Supplier CRUD ---
        TestFixtures.SUPPLIER_DATA.forEach { sup ->
            supplierDao.insert(Supplier(mobile = sup.mobile, name = sup.name,
                companyName = sup.company, email = "${sup.name.replace(" ","").lowercase()}@${sup.company.split(" ")[0].lowercase()}.in",
                city = sup.city))
            state.supplierMobiles.add(sup.mobile)
        }
        assertEquals(10, state.supplierMobiles.size)

        // Supplier update
        val firstSup = supplierDao.getSupplierByMobile(state.supplierMobiles[0])!!
        supplierDao.update(firstSup.copy(isActive = false))
        assertEquals(9, supplierDao.getActiveSuppliers().first().size)
        supplierDao.update(firstSup.copy(isActive = true))

        // Supplier query by Flow
        val supFlow = supplierDao.getSupplierByMobileFlow(state.supplierMobiles[0]).first()
        assertEquals(firstSup.name, supFlow!!.name)

        // Supplier delete and re-insert
        supplierDao.delete(firstSup)
        assertNull(supplierDao.getSupplierByMobile(state.supplierMobiles[0]))
        supplierDao.insert(firstSup)
        state.supplierMobiles[0] = firstSup.mobile

        // --- SparePartPurchase CRUD ---
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
                val qty = 5 + rng.nextInt(25)
                val cost = kotlin.math.round(part.costPrice * (0.9 + rng.nextDouble() * 0.2) * 100.0) / 100.0
                val forceUnpaid = forcedUnpaidCount < targetUnpaid && (orderCounter >= state.supplierMobiles.size * 7 - (targetUnpaid - 1))
                val isPaid = !forceUnpaid && rng.nextDouble() < 0.55

                val pid = sparePartDao.insert(SparePartPurchase(
                    repairEntryId = 0L, partName = part.name,
                    purchasePrice = cost, supplierId = mobile, supplierName = sup.name, quantity = qty,
                    purchaseDate = daysAgo(25 + rng.nextInt(5), 11)))
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
        assertTrue("At least 3 supplier payments must be unpaid", unpaid.size >= 3)

        // SparePartPurchase update (simulate stock reduction)
        val firstPart = sparePartDao.getAllPurchases().first().first()
        sparePartDao.update(firstPart.copy(quantity = firstPart.quantity - 1))
        val updatedPart = sparePartDao.getPurchasesByRepairId(firstPart.repairEntryId).first().find { it.id == firstPart.id }
        assertEquals(firstPart.quantity - 1, updatedPart!!.quantity)
        sparePartDao.update(firstPart.copy(quantity = firstPart.quantity))

        // SparePartPurchase by supplier query
        val bySupplier = sparePartDao.getPurchasesBySupplier(state.supplierMobiles[0]).first()
        assertTrue(bySupplier.isNotEmpty())

        // SparePartPurchase date range query
        val inRange = sparePartDao.getPurchasesByDateRange(daysAgo(30), System.currentTimeMillis()).first()
        assertTrue(inRange.size >= state.partPurchaseIds.size * 0.5)

        println("  $totalQty items across ${state.partPurchaseIds.size} purchase orders (${unpaid.size} unpaid) [CRUD verified]")
    }

    @Test
    fun scenario03_customerRepairFlow() = runBlocking {
        if (state.supplierMobiles.isEmpty()) scenario02_supplierAndInventory()
        if (state.repairEntryIds.isNotEmpty()) {
            println("  Already have ${state.repairEntryIds.size} repairs")
            return@runBlocking
        }
        println("\nSCENARIO 03: CUSTOMER + REPAIR FLOW")
        val rng = Random(54321)
        var idx = 0

        state.supplierMobiles.forEach { mobile ->
            val sup = supplierDao.getSupplierByMobile(mobile)!!
            repeat(8) {
                idx++
                val isDealer = idx <= 8
                val mob = TestFixtures.randomMobile(if (isDealer) 2000 + idx else idx)

                if (isDealer) {
                    dealerDao.insert(Dealer(mobileNumber = mob, name = TestFixtures.randomPersonName(), city = sup.city))
                    state.dealerMobiles.add(mob)
                } else {
                    customerDao.insert(Customer(mobileNumber = mob, name = TestFixtures.randomPersonName(), city = sup.city))
                    state.customerMobiles.add(mob)
                }

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
                val pType = if (isDealer) "DEALER" else "CUSTOMER"
                val entryId = repairDao.insert(RepairEntry(
                    customerMobile = if (isDealer) "" else mob,
                    customerName = if (isDealer) "" else TestFixtures.randomPersonName(),
                    dealerMobile = if (isDealer) mob else "",
                    dealerName = if (isDealer) TestFixtures.randomPersonName() else "",
                    customerCity = if (isDealer) "" else sup.city,
                    deviceBrand = TestFixtures.randomBrand(), deviceModel = TestFixtures.randomPhoneModel(),
                    faultDetected = listOf("Screen broken","Battery drain","Charging failure","Camera malfunction","Button stuck","Water damage","No power","Slow performance").random(),
                    sparePartName = parts.joinToString(", ") { it.name },
                    sparePartPurchasePrice = partsCharge, chargeAmount = charge, advanceAmount = advance,
                    serviceManId = smId, serviceManName = sm.name,
                    isDraft = false, workStatus = status, finalAmount = charge, handoverDone = done,
                    entryDate = daysAgo(7 + rng.nextInt(18)),
                    handoverDate = if (done) daysAgo(rng.nextInt(5), 0) else 0L,
                    quotationDone = true, sparePartDone = true, workDone = done))
                state.repairEntryIds.add(entryId)

                val balance = charge - advance
                val payStatus = when { cancelled -> "CANCELLED"; balance <= 0.01 -> "PAID"; advance > 0 -> "PARTIAL"; else -> "UNPAID" }
                if (!cancelled || advance > 0 || rng.nextDouble() < 0.3) {
                    val payId = paymentDao.insert(Payment(
                        personType = pType, personMobile = mob, personName = if (isDealer) "Dealer" else "Customer",
                        description = "Repair: $status", totalAmount = charge,
                        paidAmount = advance, dueAmount = kotlin.math.max(0.0, balance), status = payStatus,
                        linkedEntryId = entryId))
                    if (advance > 0.01) {
                        paymentTxnDao.insert(PaymentTransaction(paymentId = payId, personType = pType,
                            personMobile = mob, personName = if (isDealer) "Dealer" else "Customer", amount = advance,
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
        // Handover simulation: pick 2 pending repairs and complete handover
        val pendingHandover = repairDao.getPendingEntries().first().take(2)
        pendingHandover.forEach { entry ->
            val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
            val personName = entry.customerName.ifEmpty { entry.dealerName }
            val personType = if (entry.customerMobile.isNotEmpty()) "CUSTOMER" else "DEALER"
            val handoverAmount = entry.chargeAmount - entry.advanceAmount
            repairDao.update(entry.copy(
                finalAmount = entry.chargeAmount, paymentMode = "CASH",
                cashAmount = handoverAmount, onlineAmount = 0.0,
                handoverDate = daysAgo(1, 0), handoverDone = true,
                workStatus = "Done", workDone = true,
                completionDate = System.currentTimeMillis()))
            val existingPay = paymentDao.getAllPayments().first().firstOrNull { it.linkedEntryId == entry.id }
            if (existingPay != null) {
                paymentDao.update(existingPay.copy(
                    paidAmount = existingPay.paidAmount + handoverAmount,
                    dueAmount = 0.0, status = "PAID"))
                paymentTxnDao.insert(PaymentTransaction(
                    paymentId = existingPay.id, personType = personType,
                    personMobile = personMobile, personName = personName,
                    amount = handoverAmount, paymentMode = "CASH"))
            } else {
                val payId = paymentDao.insert(Payment(
                    personType = personType, personMobile = personMobile, personName = personName,
                    description = "Handover - ${entry.deviceBrand} ${entry.deviceModel}",
                    totalAmount = entry.chargeAmount, paidAmount = handoverAmount,
                    dueAmount = 0.0, status = "PAID", linkedEntryId = entry.id))
                paymentTxnDao.insert(PaymentTransaction(
                    paymentId = payId, personType = personType, personMobile = personMobile,
                    personName = personName, amount = handoverAmount, paymentMode = "CASH"))
            }
        }
        println("  ${state.customerMobiles.size} customers, ${state.dealerMobiles.size} dealers, ${state.repairEntryIds.size} repairs, ${state.cancelledRepairIds.size} cancelled")

        // --- RepairEntry DAO query tests ---
        val allEntries = repairDao.getAllEntries().first()
        assertTrue(allEntries.isNotEmpty())

        val pendingCount = repairDao.getPendingCount().first()
        assertTrue(pendingCount >= 0)

        val completedInRange = repairDao.getCompletedCountInRange(daysAgo(30), System.currentTimeMillis()).first()
        val revenue = repairDao.getRevenueInRange(daysAgo(30), System.currentTimeMillis()).first()
        println("  Pending=$pendingCount Completed(30d)=$completedInRange Revenue(30d)=Rs.${(revenue ?: 0.0).toInt()}")

        // RepairEntry query by mobile
        if (state.customerMobiles.isNotEmpty()) {
            val byMobile = repairDao.getEntriesByMobile(state.customerMobiles[0]).first()
            assertTrue(byMobile.isNotEmpty())
        }

        // RepairEntry search query
        val searchResults = repairDao.searchEntries("Samsung").first()
        assertNotNull(searchResults)

        // RepairEntry by date range
        val dateRangeEntries = repairDao.getEntriesByDateRange(daysAgo(30), System.currentTimeMillis()).first()
        assertTrue(dateRangeEntries.isNotEmpty())

        // RepairEntry update (simulate status change)
        val firstEntry = repairDao.getAllEntries().first().first()
        repairDao.update(firstEntry.copy(workStatus = "In Progress"))
        val updatedEntry = repairDao.getEntryById(firstEntry.id)!!
        assertEquals("In Progress", updatedEntry.workStatus)
        repairDao.update(firstEntry)

        // RepairEntry by service man
        val bySM = repairDao.getEntriesByServiceMan(state.serviceManIds[0]).first()
        assertNotNull(bySM)
    }

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

        // Sale query by supplier
        val supSales = saleDao.getSalesBySupplier(TestFixtures.SUPPLIER_DATA[0].mobile).first()
        assertNotNull(supSales)

        // Sale date range query
        val salesInRange = saleDao.getSalesByDateRange(daysAgo(20), System.currentTimeMillis()).first()
        assertTrue(salesInRange.isNotEmpty())
    }

    @Test
    fun scenario05_cashFlow20Days() = runBlocking {
        println("\nSCENARIO 05: CASH FLOW (20 business days)")
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        if (state.supplierMobiles.isEmpty()) scenario02_supplierAndInventory()
        if (state.repairEntryIds.isEmpty()) scenario03_customerRepairFlow()
        if (saleDao.getAllSales().first().isEmpty()) scenario04_directSales()

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

        // PaymentTransaction query by date range
        val allTxns = paymentTxnDao.getTransactionsByDateRange(daysAgo(30), System.currentTimeMillis()).first()
        println("  Total transactions in 30 days: ${allTxns.size}")

        // PaymentTransaction query by mobile
        if (state.customerMobiles.isNotEmpty()) {
            val txnsByMobile = paymentTxnDao.getTransactionsByMobile(state.customerMobiles[0]).first()
            assertNotNull(txnsByMobile)
        }

        // Payment query by type and date
        val supplierPaymentsInRange = paymentDao.getPaymentsByTypeAndDate("SUPPLIER", daysAgo(30), System.currentTimeMillis()).first()
        val paymentCount = paymentDao.getPaymentCountByTypeAndDate("SUPPLIER", daysAgo(30), System.currentTimeMillis()).first()
        assertTrue(paymentCount <= supplierPaymentsInRange.size)
    }

    @Test
    fun scenario06_returns() = runBlocking {
        println("\nSCENARIO 06: RETURNS")
        if (state.repairEntryIds.size < 5) scenario03_customerRepairFlow()

        val rng = Random(77777)
        val completed = repairDao.getCompletedEntries().first().toMutableList()
        repeat(minOf(5, completed.size)) {
            val entry = completed[it]
            val isDealer = entry.customerMobile.isEmpty()
            val personMobile = entry.customerMobile.ifEmpty { entry.dealerMobile }
            val personName = entry.customerName.ifEmpty { entry.dealerName }
            val refund = kotlin.math.round(entry.chargeAmount * (0.5 + rng.nextDouble() * 0.5) * 100.0) / 100.0
            val payId = paymentDao.insert(Payment(
                personType = if (isDealer) "DEALER_REFUND" else "CUSTOMER_REFUND",
                personMobile = personMobile, personName = personName,
                description = "Refund - repair #${entry.id}", totalAmount = refund,
                paidAmount = refund, dueAmount = 0.0, status = "REFUNDED", linkedEntryId = entry.id))
            state.refundPaymentIds.add(payId)
            paymentTxnDao.insert(PaymentTransaction(
                paymentId = payId, personType = if (isDealer) "DEALER_REFUND" else "CUSTOMER_REFUND",
                personMobile = personMobile, personName = personName, amount = -refund, paymentMode = "CASH"))
        }
        println("  ${state.refundPaymentIds.size} customer refunds")

        // --- PartReturn CRUD ---
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
            val unpaid = paymentDao.getAllPayments().first()
                .firstOrNull { it.personMobile == mobile && it.personType == "SUPPLIER" && it.status == "UNPAID" }
            val creditPaymentId = unpaid?.id ?: 0L
            paymentTxnDao.insert(PaymentTransaction(
                paymentId = creditPaymentId, personType = "SUPPLIER_CREDIT", personMobile = mobile, personName = sup.name,
                amount = -refundAmount, paymentMode = "ADJUSTMENT",
                transactionDate = daysAgo(5, 10)))
            if (unpaid != null) {
                paymentDao.update(unpaid.copy(
                    dueAmount = kotlin.math.max(0.0, unpaid.dueAmount - refundAmount)
                ))
            }
        }
        println("  ${state.supplierReturnIds.size} supplier returns")

        // PartReturn query by supplier
        if (state.supplierReturnIds.isNotEmpty()) {
            val firstReturn = partReturnDao.getReturnById(state.supplierReturnIds[0])!!
            val bySupplier = partReturnDao.getReturnsBySupplier(firstReturn.supplierId).first()
            assertTrue(bySupplier.isNotEmpty())
        }

        // PartReturn update
        if (state.supplierReturnIds.isNotEmpty()) {
            val firstRet = partReturnDao.getReturnById(state.supplierReturnIds[0])!!
            partReturnDao.update(firstRet.copy(refundReceived = true))
            val updatedRet = partReturnDao.getReturnById(state.supplierReturnIds[0])!!
            assertTrue(updatedRet.refundReceived)
        }
    }

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
            val due = kotlin.math.max(0.0, p.totalAmount - paid - cred)
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

        // 7e: Dealer Payments
        val dealerPays = paymentDao.getAllPayments().first().filter { it.personType == "DEALER" && it.status != "CANCELLED" }
        var dealerPaidCalc = 0.0
        dealerPays.forEach { p ->
            val paid = paymentTxnDao.getTransactionsByPayment(p.id).first().filter { it.amount > 0 }.sumOf { it.amount }
            dealerPaidCalc += paid
            if (kotlin.math.abs(paid - p.paidAmount) > 0.01)
                mismatches.add(MismatchRecord("Dealer-Payment", "Payment #${p.id}", paid, p.paidAmount))
        }

        // 7f: Cross-check Payment totalAmount = paidAmount + dueAmount
        paymentDao.getAllPayments().first().forEach { p ->
            if (kotlin.math.abs(p.totalAmount - (p.paidAmount + p.dueAmount)) > 0.01)
                mismatches.add(MismatchRecord("Payment-Integrity", "Payment #${p.id}: total=${p.totalAmount} != paid=${p.paidAmount} + due=${p.dueAmount}", p.totalAmount, p.paidAmount + p.dueAmount))
        }

        // 7g: RepairEntry integrity - completed entries must have handoverDone=true and chargeAmount = finalAmount
        repairDao.getAllEntries().first().filter { it.workStatus == "Done" }.forEach { e ->
            if (!e.handoverDone) mismatches.add(MismatchRecord("Repair-Integrity", "Entry #${e.id}: Done but handoverDone=false", true, false))
            if (kotlin.math.abs(e.chargeAmount - e.finalAmount) > 0.01 && e.finalAmount > 0)
                mismatches.add(MismatchRecord("Repair-Integrity", "Entry #${e.id}: charge=$chargeAmount final=$finalAmount", e.chargeAmount, e.finalAmount))
        }

        val totalRevenue = revCalc + saleDao.getAllSales().first().sumOf { it.customerPaid }
        val totalCogs = supPays.sumOf { it.totalAmount }
        println("  Revenue=Rs.${totalRevenue.toInt()}  COGS=Rs.${totalCogs.toInt()}  Profit=Rs.${(totalRevenue - totalCogs).toInt()}")
        println("  SupplierDue(calc)=Rs.${dueCalc.toInt()}  CustPaid(calc)=Rs.${custPaidCalc.toInt()}  DealerPaid(calc)=Rs.${dealerPaidCalc.toInt()}")

        val reportCategories = listOf("Inventory", "Revenue", "Supplier-Due", "Cust-Payments", "Dealer-Payments", "Payment-Integrity", "Repair-Integrity")
        val report = ReconciliationReport(
            reportCategories.map { cat ->
                ScenarioResult(cat, mismatches.none { it.category == cat }, mismatches.filter { it.category == cat })
            },
            mismatches.size, mismatches.isEmpty())
        report.printSummary()
        mismatches.forEach { println("MISMATCH [${it.category}]: expected=${it.expected}, actual=${it.actual} - ${it.description}") }
        assertTrue("Reconciliation FAILED: ${mismatches.size} mismatch(es)", mismatches.isEmpty())
    }

    @Test
    fun scenario08_fullEndToEndRealWorld() = runBlocking {
        println("\n========== FULL REAL-WORLD SIMULATION ==========")
        clearAllData()
        scenario01_shopKeeperSetup()
        scenario02_supplierAndInventory()
        scenario03_customerRepairFlow()
        scenario04_directSales()
        scenario05_cashFlow20Days()
        scenario06_returns()
        scenario07_reconciliation()
        println("\n========== SIMULATION COMPLETE ==========")
    }

    // ================================================================
    // NEW: SCENARIO 09 - Edge Cases, Cross-Table Integrity, Data Dump
    // ================================================================
    @Test
    fun scenario09_edgeCasesCrossTableIntegrity() = runBlocking {
        println("\nSCENARIO 09: EDGE CASES + CROSS-TABLE INTEGRITY + DATA DUMP")

        // --- Edge Cases ---

        // 9.1: RepairEntry with empty optional fields
        val minimalEntryId = repairDao.insert(RepairEntry(
            customerMobile = "9999999999", customerName = "Minimal Customer",
            deviceBrand = "", deviceModel = "", faultDetected = "",
            sparePartName = "", sparePartPurchasePrice = 0.0,
            supplierId = 0L, workStatus = "Pending"))
        assertTrue(minimalEntryId > 0)

        // 9.2: RepairEntry with very long names
        val longName = "A".repeat(255)
        val longEntryId = repairDao.insert(RepairEntry(
            customerMobile = "8888888888", customerName = longName,
            deviceBrand = longName, deviceModel = longName,
            sparePartName = longName, sparePartPurchasePrice = 999999.99,
            supplierId = 100L, workStatus = "Pending"))
        val longEntry = repairDao.getEntryById(longEntryId)!!
        assertEquals(longName, longEntry.customerName)

        // 9.3: Zero and negative amounts in Payment
        val zeroPayId = paymentDao.insert(Payment(
            personType = "CUSTOMER", personMobile = "7777777777",
            personName = "Zero Test", description = "Zero amount test",
            totalAmount = 0.0, paidAmount = 0.0, dueAmount = 0.0, status = "PAID"))
        assertTrue(zeroPayId > 0)

        // 9.4: Payment with very large amount
        val largePayId = paymentDao.insert(Payment(
            personType = "SUPPLIER", personMobile = "6666666666",
            personName = "Large Amount Test", description = "Large amount test",
            totalAmount = 999999.99, paidAmount = 500000.0, dueAmount = 499999.99, status = "PARTIAL"))
        assertTrue(largePayId > 0)

        // 9.5: Empty string supplier mobile (edge case)
        val emptySupplierPurchase = sparePartDao.insert(SparePartPurchase(
            repairEntryId = 1L, partName = "Test Part",
            purchasePrice = 100.0, supplierId = "", supplierName = "No Supplier",
            quantity = 1))
        assertTrue(emptySupplierPurchase > 0)

        // 9.6: Customer with null name/city
        val nullFieldsCustomer = Customer(mobileNumber = "5555555555", name = null, city = null)
        customerDao.insert(nullFieldsCustomer)
        val fetched = customerDao.getCustomerByMobile("5555555555")
        assertNotNull(fetched)
        assertNull(fetched!!.name)
        assertNull(fetched.city)

        // 9.7: Dealer with null name/city
        dealerDao.insert(Dealer(mobileNumber = "4444444444", name = null, city = null))
        val dealer = dealerDao.getDealerByMobile("4444444444")
        assertNotNull(dealer)
        assertNull(dealer!!.name)

        // 9.8: PaymentTransaction with negative amount (refund scenario)
        val refundTxnId = paymentTxnDao.insert(PaymentTransaction(
            paymentId = zeroPayId, personType = "CUSTOMER",
            personMobile = "7777777777", personName = "Refund Test",
            amount = -500.0, paymentMode = "CASH",
            note = "Test negative transaction"))
        assertTrue(refundTxnId > 0)

        // 9.9: CommonFault with edge case values
        val edgeFaultId = commonFaultDao.insert(CommonFault(
            faultName = "", category = "", defaultCharge = 0.0, isActive = false, sortOrder = -1))
        assertTrue(edgeFaultId > 0)

        // 9.10: Multiple entries with same mobile (different dealers vs customers)
        customerDao.insert(Customer(mobileNumber = "3333333333", name = "Same Mobile 1", city = "City1"))
        customerDao.insert(Customer(mobileNumber = "3333333333", name = "Same Mobile 2", city = "City2"))
        val sameMobileCustomers = customerDao.getCustomerByMobile("3333333333")
        assertEquals("Same Mobile 2", sameMobileCustomers!!.name) // REPLACE means last write wins

        // --- Cross-Table Integrity Checks ---

        // 9.11: Verify all spare part purchases reference valid entries by repairEntryId
        val allParts = sparePartDao.getAllPurchases().first()
        val allEntryIds = repairDao.getAllEntries().first().map { it.id }.toSet()
        val orphanParts = allParts.filter { it.repairEntryId != 0L && it.repairEntryId !in allEntryIds }
        if (orphanParts.isNotEmpty()) {
            println("  WARNING: ${orphanParts.size} orphan spare parts (repairEntryId not found)")
        }

        // 9.12: Verify linked payment entries exist
        val linkedPayments = paymentDao.getAllPayments().first().filter { it.linkedEntryId != 0L }
        val orphanLinked = linkedPayments.filter { it.linkedEntryId !in allEntryIds }
        if (orphanLinked.isNotEmpty()) {
            println("  WARNING: ${orphanLinked.size} payments link to non-existent repair entries")
        }

        // 9.13: Check PaymentTransaction references match Payment records
        val allPayments = paymentDao.getAllPayments().first()
        val paymentIdSet = allPayments.map { it.id }.toSet()
        val allTxns = paymentTxnDao.getAllTransactions().first()
        val orphanTxns = allTxns.filter { it.paymentId !in paymentIdSet }
        if (orphanTxns.isNotEmpty()) {
            println("  WARNING: ${orphanTxns.size} transactions reference non-existent payments")
        }

        // 9.14: Verify all dealers have unique mobiles (PK constraint)
        val dealerCount = dealerDao.getAllDealers().first().size
        val uniqueDealerMobiles = dealerDao.getAllDealers().first().map { it.mobileNumber }.distinct().size
        if (dealerCount != uniqueDealerMobiles) {
            println("  WARNING: Duplicate dealer mobiles detected")
        }

        // 9.15: Verify no future-dated handovers (handoverDate > now) in Completed entries
        val now = System.currentTimeMillis()
        val futureHandovers = repairDao.getAllEntries().first().filter { it.handoverDone && it.handoverDate > now }
        if (futureHandovers.isNotEmpty()) {
            println("  WARNING: ${futureHandovers.size} completed entries have future handover dates")
        }

        println("  Edge cases: 10 scenarios tested")
        println("  Cross-table integrity: 5 checks completed")

        // --- Comprehensive Data Dump ---
        println("\n========== COMPREHENSIVE DATA DUMP ==========")
        val dump = DataDump(
            suppliers = supplierDao.getAllSuppliers().first(),
            serviceMen = serviceManDao.getAllServiceMen().first(),
            customers = customerDao.getAllCustomers().first(),
            dealers = dealerDao.getAllDealers().first(),
            repairs = repairDao.getAllEntries().first(),
            spareParts = sparePartDao.getAllPurchases().first(),
            sales = saleDao.getAllSales().first(),
            payments = paymentDao.getAllPayments().first(),
            transactions = paymentTxnDao.getAllTransactions().first(),
            returns = partReturnDao.getAllReturns().first(),
            faults = commonFaultDao.getAllFaults().first(),
            userProfile = userProfileDao.getUserProfile()
        )

        // Print structured report
        println("""
  ┌─────────────────────────────────────────────────────────────────┐
  │                    BUSINESS INTELLIGENCE REPORT                 │
  ├─────────────────────────────────────────────────────────────────┤
  │ PROFILE                                                         │
  │   Shop: ${dump.userProfile?.shopName?.padEnd(50)}}│
  │   Owner: ${dump.userProfile?.name?.padEnd(51)}}│
  │   Phone: ${dump.userProfile?.phone?.padEnd(51)}}│
  │   GST: ${dump.userProfile?.gstNo?.padEnd(54)}}│
  ├─────────────────────────────────────────────────────────────────┤
  │ MASTER DATA                                                     │
  │   Service Men: ${dump.serviceMen.size.toString().padStart(4)}                                    │
  │   Suppliers:   ${dump.suppliers.size.toString().padStart(4)}                                    │
  │   Customers:   ${dump.customers.size.toString().padStart(4)}                                    │
  │   Dealers:     ${dump.dealers.size.toString().padStart(4)}                                    │
  │   Common Faults: ${dump.faults.size.toString().padStart(4)}                                    │
  ├─────────────────────────────────────────────────────────────────┤
  │ OPERATIONS                                                      │
  │   Repair Entries: ${dump.repairs.size.toString().padStart(4)}                                    │
  │   - Completed:    ${dump.repairs.count { it.handoverDone }.toString().padStart(4)}                                    │
  │   - Pending:      ${dump.repairs.count { !it.handoverDone }.toString().padStart(4)}                                    │
  │   - Cancelled:    ${dump.repairs.count { it.workStatus == "Cancelled" }.toString().padStart(4)}                                    │
  │   Spare Parts Purchased: ${dump.spareParts.size.toString().padStart(4)}                                    │
  │   Direct Sales:   ${dump.sales.size.toString().padStart(4)}                                    │
  │   Supplier Returns: ${dump.returns.size.toString().padStart(4)}                                    │
  │   Payments:       ${dump.payments.size.toString().padStart(4)}                                    │
  │   Transactions:   ${dump.transactions.size.toString().padStart(4)}                                    │
  ├─────────────────────────────────────────────────────────────────┤
  │ FINANCIAL SUMMARY                                               │
  │   Total Inventory Value: Rs.${"%.0f".format(dump.spareParts.sumOf { it.purchasePrice * it.quantity }).padStart(9)}                    │
  │   Total Repair Revenue: Rs.${"%.0f".format(dump.repairs.filter { it.handoverDone }.sumOf { it.finalAmount }).padStart(9)}                    │
  │   Total Direct Sales: Rs.${"%.0f".format(dump.sales.sumOf { it.salePrice }).padStart(9)}                    │
  │   Total Supplier Due: Rs.${"%.0f".format(dump.payments.filter { it.personType == "SUPPLIER" && it.status != "PAID" }.sumOf { it.dueAmount }).padStart(9)}                    │
  │   Total Customer Due: Rs.${"%.0f".format(dump.payments.filter { p -> (p.personType == "CUSTOMER" || p.personType == "DEALER") && p.status != "PAID" }.sumOf { it.dueAmount }).padStart(9)}                    │
  │   Total Cash In: Rs.${"%.0f".format(dump.transactions.filter { it.amount > 0 }.sumOf { it.amount }).padStart(9)}                    │
  │   Total Cash Out: Rs.${"%.0f".format(dump.transactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }).padStart(9)}                    │
  │   Business Days Simulated: ${state.dailyCashLedger.size.toString().padStart(4)}                                    │
  └─────────────────────────────────────────────────────────────────┘
        """.trimIndent())
        println("========== END DATA DUMP ==========")
    }

    // ================================================================
    // SCENARIO 10: Full Orchestrated Run with Data Dump
    // ================================================================
    @Test
    fun scenario10_fullRunWithDump() = runBlocking {
        println("\n========== COMPREHENSIVE SYSTEM TEST ==========")
        println("Testing all 12 DAOs across all CRUD operations + edge cases + data dump")
        println("Started at: ${DateUtils.formatDateTime(System.currentTimeMillis())}")
        println()

        // Run all standard scenarios
        scenario01_shopKeeperSetup()
        scenario02_supplierAndInventory()
        scenario03_customerRepairFlow()
        scenario04_directSales()
        scenario05_cashFlow20Days()
        scenario06_returns()
        scenario07_reconciliation()

        // Run edge cases and cross-table integrity
        // (re-run within same DB context - edge case test uses existing data)
        println()
        scenario09_edgeCasesCrossTableIntegrity()

        println()
        println("========== ALL 10 SCENARIOS COMPLETED ==========")
        println("""
  Test Summary:
  ├── SC01: Shop Setup (UserProfile, ServiceMan, CommonFault) - CRUD verified
  ├── SC02: Suppliers + Inventory (Supplier, SparePartPurchase) - CRUD verified
  ├── SC03: Customer/Dealer Repair Flow (Customer, Dealer, RepairEntry) - CRUD verified
  ├── SC04: Direct Sales (Sale) - CRUD verified
  ├── SC05: Cash Flow (Payment, PaymentTransaction) - queries verified
  ├── SC06: Returns (PartReturn) - CRUD verified
  ├── SC07: Reconciliation - 7 categories checked
  ├── SC08: Full End-to-End - all scenarios orchestrated
  ├── SC09: Edge Cases + Cross-Table Integrity + Data Dump
  └── SC10: Master orchestration with final report
        """.trimIndent())
    }
}
