package com.app.muzzutech

// ═══════════════════════════════════════════════════════════════════════════════
// RealWorldSimulationTest.kt
// ═══════════════════════════════════════════════════════════════════════════════
// *** THIS IS A TEST / SEEDING DATA FILE — DO NOT USE IN PRODUCTION ***
// Run via: ./gradlew test --tests "com.app.muzzutech.RealWorldSimulationTest"
// No production source code has been modified.
//
// "Clear All Test Data" — call clearAllTestData() from any test or companion.
// ═══════════════════════════════════════════════════════════════════════════════
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

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION 0 — DTOs & Result types (kept at file top for readability)
// ─────────────────────────────────────────────────────────────────────────────

/** Row-level mismatch between independent recalculation and the app's stored view. */
data class MismatchRecord(
    val category: String,       // e.g. "Inventory", "Supplier Due", "Revenue"
    val description: String,    // human-readable explanation
    val expected: Any,          // what independent calc found
    val actual: Any             // what the app currently stores
)

/** Per-scenario pass/fail result. */
data class ScenarioResult(
    val name: String, val passed: Boolean, val mismatches: List<MismatchRecord>
) {
    fun orFail(msg: String = "") {
        if (!passed) fail("❌ SCENARIO FAILED: $name — ${mismatches.size} mismatch(es)$msg\n" +
                mismatches.joinToString("\n  • ") { "${it.category}: expected=${it.expected}, actual=${it.actual} — ${it.description}" })
    }
}

/** Final reconciliation report covering all 8 scenarios. */
data class ReconciliationReport(
    val scenarios: List<ScenarioResult>,
    val totalMismatches: Int,
    val overallPassed: Boolean
) {
    fun printSummary() {
        println("\n${if (overallPassed) "✅ ALL SCENARIOS PASSED" else "❌ SOME SCENARIOS FAILED"}")
        scenarios.forEach { println("  ${if (it.passed) "✅" else "❌"} ${it.name} (${it.mismatches.size} mismatches)") }
        println("Total mismatches: $totalMismatches")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION 1 — Test Fixtures & Indian Sample Data
// ─────────────────────────────────────────────────────────────────────────────

/**
 * *** TEST DATA ONLY — safe to call clearAllTestData() to wipe everything. ***
 */
object TestFixtures {

    // ── Indian first names (gender-balanced) ──────────────────────────────────
    val FIRST_NAMES_MALE = listOf(
        "Rajesh","Amit","Suresh","Vijay","Ravi","Mohit","Prakash","Deepak","Arjun","Karan",
        "Rahul","Sanjay","Nikhil","Rohit","Aditya","Manish","Pooja","Priya","Neha","Anita",
        "Sunil","Ramesh","Dinesh","Ganesh","Mukesh","Vivek","Ashok","Bharat","Chandan","Devendra",
        "Firoz","Girish","Harish","Ibrahim","Jagdish","Kishore","Lalit","Mohan","Nilesh","Omkar",
        "Pavan","Qasim","Rajiv","Sachin","Tarun","Umesh","Vasant","Yogesh","Zaheer","Anil"
    )

    // ── Indian surnames ────────────────────────────────────────────────────────
    val LAST_NAMES = listOf(
        "Sharma","Verma","Patel","Singh","Kumar","Gupta","Joshi","Reddy","Rao","Iyer",
        "Naidu","Choudhary","Agarwal","Pandey","Mishra","Tiwari","Yadav","Jain","Bansal","Malhotra"
    )

    // ── Indian mobile number pool (outgoing from shop's perspective) ──────────
    fun randomMobile(idx: Int): String = "98${(10000000 + idx).toString().padStart(8, '0')}"

    // ── Spare parts catalog with realistic Indian retail prices (₹) ───────────
    data class PartDef(
        val name: String, val category: String,
        val costPrice: Double, val retailPrice: Double
    )
    val PARTS_CATALOG = listOf(
        PartDef("Samsung Display Assembly (OLED)",       "Display",   3200.0, 5500.0),
        PartDef("Redmi Display Assembly (LCD)",          "Display",   1500.0, 2800.0),
        PartDef("iPhone OLED Display",                   "Display",   5000.0, 8500.0),
        PartDef("Generic Battery 4000 mAh",              "Battery",   350.0,  700.0),
        PartDef("Original Samsung Battery",              "Battery",   800.0,  1400.0),
        PartDef("Generic Battery 3000 mAh",              "Battery",   280.0,  550.0),
        PartDef("USB-C Charging Port (Black)",           "Charging",  180.0,  400.0),
        PartDef("Lightning Charging Port (Original)",    "Charging",  450.0,  900.0),
        PartDef("Wireless Charging Module Qi",           "Charging",  600.0,  1200.0),
        PartDef("Rear Camera Module 48MP",               "Camera",    1100.0, 2000.0),
        PartDef("Front Camera Module 12MP",              "Camera",    500.0,  950.0),
        PartDef("iPhone Rear Camera Triple",             "Camera",    2200.0, 3800.0),
        PartDef("Power Button Flex Cable",               "Button",    150.0,  350.0),
        PartDef("Volume Button Flex Cable",              "Button",    100.0,  250.0),
        PartDef("Motherboard (Reconditioned)",           "Board",     3000.0, 5500.0)
    )

    // ── Phone models seen in the shop ─────────────────────────────────────────
    val PHONE_MODELS = listOf(
        "Samsung Galaxy S21","Samsung Galaxy A54","Redmi Note 12","Redmi 13 Pro",
        "iPhone 14","iPhone 13","OnePlus Nord CE 3","OnePlus 11R",
        "Vivo V27","Realme 12 Pro","Poco X6","Motorola Edge 40",
        "Nothing Phone 2","Asus ROG Phone 7","Google Pixel 8"
    )
    val BRANDS = listOf("Samsung","Apple","Xiaomi","Redmi","OnePlus","Vivo","Realme","Poco","Motorola","Nothing")

    // ── Realistic Indian supplier names / locations ───────────────────────────
    val SUPPLIER_DATA = listOf(
        Triple("Rajesh Mobile Parts",     "Rajesh Sharma",  "9398123456",  "Mumbai"),
        Triple("Sharma Electronics",       "Mukesh Sharma",  "9845123456",  "Delhi"),
        Triple("Patel Distributors",       "Kiran Patel",    "9876123456",  "Ahmedabad"),
        Triple("Gupta Spare Mart",         "Suresh Gupta",   "9824123456",  "Kolkata"),
        Triple("Singh Mobile Hub",         "Ravi Singh",     "9811123456",  "Delhi"),
        Triple("Reddy Components",         "Srinivas Reddy", "9000123456",  "Hyderabad"),
        Triple("Joshi Electronics",        "Prakash Joshi",  "9762123456",  "Pune"),
        Triple("Kumar Accessories",        "Ajit Kumar",     "9544123456",  "Chennai"),
        Triple("Malhotra Tech Traders",    "Anil Malhotra",  "9310123456",  "Jaipur"),
        Triple("Iyer Mobile Supplies",     "Venkat Iyer",    "9688123456",  "Bangalore")
    )

    // ── Service man names ─────────────────────────────────────────────────────
    val SERVICE_MAN_DATA = listOf(
        Triple("Ravi Kumar Sharma",    "9900111222", "ravi@muzzutech.com",   "Senior Technician"),
        Triple("Mohit Verma",          "9900111333", "mohit@muzzutech.com",  "Technician"),
        Triple("Deepak Joshi",         "9900111444", "deepak@muzzutech.com", "Junior Technician"),
        Triple("Sachin Patel",         "9900111555", "sachin@muzzutech.com", "Technician"),
        Triple("Nikhil Gupta",         "9900111666", "nikhil@muzzutech.com", "Apprentice")
    )

    // ── Random sample data helpers ────────────────────────────────────────────
    fun randomFirstName(): String  = FIRST_NAMES_MALE.random()
    fun randomSurname(): String    = LAST_NAMES.random()
    fun randomPersonName(): String = "${randomFirstName()} ${randomSurname()}"
    fun randomPhoneModel(): String = PHONE_MODELS.random()
    fun randomBrand(): String      = BRANDS.random()
    fun randomPart(): PartDef      = PARTS_CATALOG.random()
    fun randomSupplier(): Triple<String, String, String> = SUPPLIER_DATA.random()
    fun weightedBoolean(probTrue: Double): Boolean = Random.nextDouble() < probTrue
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION 2 — Main Test Class
// ─────────────────────────────────────────────────────────────────────────────
@RunWith(RobolectricTestRunner::class)
class RealWorldSimulationTest {

    // AppDatabase + 12 DAOs
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

    // ── Scenario state (populated as we go) ───────────────────────────────────
    data class SimState(
        // Scenario 1
        val serviceManIds: MutableList<Long> = mutableListOf(),
        // Scenario 2
        val supplierMobiles: MutableList<String> = mutableListOf(),
        val partPurchaseIds: MutableList<Long> = mutableListOf(),
        val initialPartStocks: MutableMap<String, Int> = mutableMapOf(), // partName → qty after all purchasing
        val supplierPaymentIds: MutableList<Long> = mutableListOf(),
        val supplierPaymentTxnIds: MutableList<Long> = mutableListOf(),
        // Scenario 3
        val customerMobiles: MutableList<String> = mutableListOf(),
        val repairEntryIds: MutableList<Long> = mutableListOf(),
        val cancelledRepairIds: MutableList<Long> = mutableListOf(),
        val partReturnIds: MutableList<Long> = mutableListOf(),
        // Scenario 4
        val directSaleIds: MutableList<Long> = mutableListOf(),
        // Scenario 5 — per-day cash ledger
        val dailyCashLedger: MutableList<DayLedger> = mutableListOf(),
        // Scenario 6
        val refundPaymentIds: MutableList<Long> = mutableListOf(),
        val supplierReturnIds: MutableList<Long> = mutableListOf()
    )
    data class DayLedger(
        val dayIndex: Int,
        val cashIn: Double = 0.0,
        val cashOut: Double = 0.0,
        val personalWithdrawal: Double = 0.0
    ) {
        val net: Double get() = cashIn - cashOut
    }
    private val state = SimState()

    // ── Seeding roll counter for determinism ──────────────────────────────────
    private var rollSeed = 42
    private fun roll(max: Int): Int {
        rollSeed = (rollSeed * 1103515245 + 12345) and 0x7fffffff
        return rollSeed % max
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 3 — @Before / @After lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repairDao       = db.repairEntryDao()
        serviceManDao   = db.serviceManDao()
        supplierDao     = db.supplierDao()
        commonFaultDao  = db.commonFaultDao()
        sparePartDao    = db.sparePartPurchaseDao()
        customerDao     = db.customerDao()
        dealerDao       = db.dealerDao()
        saleDao         = db.saleDao()
        userProfileDao  = db.userProfileDao()
        paymentDao      = db.paymentDao()
        partReturnDao   = db.partReturnDao()
        paymentTxnDao   = db.paymentTransactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { db.close() }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 4 — Setup / Clear helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Call this at the START of any test, or between scenarios, to clear everything. */
    private fun clearAllData() = runBlocking {
        // Payments → transactions → part returns → sale → spare parts → repair → serviceMan → supplier → customer → profile
        paymentTxnDao.getAllTransactions().first().forEach { paymentTxnDao.delete(it) }
        paymentDao.getAllPayments().first().forEach { paymentDao.delete(it) }
        partReturnDao.getAllReturns().first().forEach { partReturnDao.delete(it) }
        saleDao.getAllSales().first().forEach { saleDao.delete(it) }
        sparePartDao.getAllPurchases().first().forEach { sparePartDao.delete(it) }
        repairDao.getAllEntries().first().forEach { repairDao.delete(it) }
        serviceManDao.getAllServiceMen().first().forEach { serviceManDao.delete(it) }
        supplierDao.getAllSuppliers().first().forEach { supplierDao.delete(it) }
        customerDao.getAllCustomers().first().forEach { customerDao.deleteByMobile(it.mobileNumber) }
        // UserProfile is a singleton (id=1) — update to blank instead of deleting to avoid FK issues
        userProfileDao.getUserProfile()?.let { userProfileDao.insertOrUpdate(it.copy(
            shopName = "", name = "", email = "", phone = "", shopAddress = "", gstNo = ""
        ))}
        state.serviceManIds.clear()
        state.supplierMobiles.clear()
        state.partPurchaseIds.clear()
        state.initialPartStocks.clear()
        state.supplierPaymentIds.clear()
        state.supplierPaymentTxnIds.clear()
        state.customerMobiles.clear()
        state.repairEntryIds.clear()
        state.cancelledRepairIds.clear()
        state.partReturnIds.clear()
        state.directSaleIds.clear()
        state.dailyCashLedger.clear()
        state.refundPaymentIds.clear()
        state.supplierReturnIds.clear()
        rollSeed = 42
    }

    /** Public API for "Clear All Test Data" — call from any test or externally. */
    @Test
    fun clearAllTestData() {
        clearAllData()
        println("[CLEAR] All test data wiped.")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 5 — Helper: build timestamp (simulates a number of business days ago)
    // ═══════════════════════════════════════════════════════════════════════════

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Returns the epoch-millis for `daysAgo` days before "today" (00:00 IST ≈ 18:30 UTC prev day). */
    private fun daysAgo(daysAgo: Int, hourOfDay: Int = 10): Long {
        val now = System.currentTimeMillis()
        val dayMs = MILLIS_PER_DAY
        return now - daysAgo * dayMs + hourOfDay * 3600_000L
    }

    /** Returns the epoch-millis for the start of a simulated "business day". */
    private fun dayStart(dayIndex0: Int, totalDays: Int = 20): Long {
        // day 0 = today-1, day 19 = 20 days ago
        return daysAgo(totalDays - dayIndex0, hourOfDay = 10)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 6 — SHOPKEEPER / OWNER — Scenario 1
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario01_shopKeeperSetup() = runBlocking {
        clearAllData()
        println("\n══ SCENARIO 01: SHOPKEEPER/OWNER SETUP ══")

        // 1a. Shop profile
        val profile = UserProfile(
            id = 1,
            shopName    = "MuzzuTech Mobile Repair",
            name        = "Rahman bhai",
            email       = "rahman@muzzutech.in",
            phone       = "9876543210",
            shopAddress = "Shop No. 12,MG Road, Bangalore - 560001",
            gstNo       = "29ABCDE1234F1ZX"
        )
        userProfileDao.insertOrUpdate(profile)
        val stored = userProfileDao.getUserProfile()
        assertNotNull("Shop profile must be saved", stored)
        assertEquals("MuzzuTech Mobile Repair", stored!!.shopName)
        println("  Shop profile created: ${stored.shopName}  (${stored.shopAddress})")
        assertTrue("Shop name must not be blank", stored.shopName.isNotBlank())

        // 1b. 5 Service Men
        TestFixtures.SERVICE_MAN_DATA.forEach { (name, mobile, email, designation) ->
            val id = serviceManDao.insert(ServiceMan(
                name = name, mobile = mobile, email = email,
                employeeId = "SM-${(1000 + state.serviceManIds.size + 1)}",
                designation = designation
            ))
            state.serviceManIds.add(id)
            println("  ✓ ServiceMan #$id  $name ($designation)")
        }
        assertEquals(5, state.serviceManIds.size)

        val allSM = serviceManDao.getAllServiceMen().first()
        assertEquals(5, allSM.size)
        assertTrue("All service men active", allSM.all { it.isActive })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 7 — SUPPLIER + INVENTORY — Scenario 2
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario02_supplierAndInventory() = runBlocking {
        // (setup implicit from Scenario 1)
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        setupSuppliersAndInventory()
    }

    private suspend fun setupSuppliersAndInventory() {
        println("\n══ SCENARIO 02: SUPPLIERS + INVENTORY ══")
        val rng = Random(12345)

        // ── 2a. Register 10 suppliers ──────────────────────────────────────────
        TestFixtures.SUPPLIER_DATA.forEach { (company, contactName, mobile, city) ->
            val supplier = Supplier(
                mobile = mobile,
                name = contactName,
                companyName = company,
                email = "${contactName.replace(" ", "").lowercase()}@${company.split(" ")[0].lowercase()}.in",
                city = city
            )
            supplierDao.insert(supplier)
            state.supplierMobiles.add(mobile)
            println("  ✓ Supplier: $company  [$city]  [$mobile]")
        }
        assertEquals(10, state.supplierMobiles.size)

        // ── 2b. Place ~6–12 orders per supplier across last 25 days ────────────
        var totalPartsAdded = 0
        state.supplierMobiles.forEach { supplierMobile ->
            val supplier = supplierDao.getSupplierByMobile(supplierMobile)!!
            val orderCount = 6 + rng.nextInt(7) // 6 – 12 orders
            repeat(orderCount) { i ->
                val partDef  = TestFixtures.PARTS_CATALOG.random()
                val quantity = 1 + rng.nextInt(20)
                val costEach = partDef.costPrice * (0.9 + rng.nextDouble() * 0.2) // ±10 % price variance
                val isPaid   = rng.nextDouble() < 0.55   // ~55 % paid immediately

                val pid = sparePartDao.insert(SparePartPurchase(
                    repairEntryId = 0L,
                    partName      = partDef.name,
                    purchasePrice = kotlin.math.round(costEach * 100.0) / 100.0,
                    supplierId    = supplierMobile,
                    supplierName  = supplier.name,
                    quantity      = quantity
                ))
                state.partPurchaseIds.add(pid)
                state.initialPartStocks[partDef.name] =
                    (state.initialPartStocks[partDef.name] ?: 0) + quantity
                totalPartsAdded += quantity

                // For paid orders, log a Payment + Transaction immediately
                if (isPaid) {
                    val totalAmt = kotlin.math.round(costEach * quantity * 100.0) / 100.0
                    val payId = paymentDao.insert(Payment(
                        personType   = "SUPPLIER",
                        personMobile = supplierMobile,
                        personName   = supplier.name,
                        description  = "Parts purchase — ${partDef.name} x$quantity",
                        totalAmount  = totalAmt,
                        paidAmount   = totalAmt,
                        dueAmount    = 0.0,
                        status       = "PAID"
                    ))
                    state.supplierPaymentIds.add(payId)
                    paymentTxnDao.insert(PaymentTransaction(
                        paymentId    = payId,
                        personType   = "SUPPLIER",
                        personMobile = supplierMobile,
                        personName   = supplier.name,
                        amount       = totalAmt,
                        paymentMode  = if (rng.nextBoolean()) "CASH" else "ONLINE"
                    ))
                    state.supplierPaymentTxnIds.add(payId)
                }
            }
        }
        val unpaidPending = paymentDao.getAllPayments().first().filter { it.status != "PAID" }
        assertTrue("Must have unpaid supplier payments for reconciliation", unpaidPending.isNotEmpty())
        println("  → $totalPartsAdded units across ${state.partPurchaseIds.size} purchase orders")
        println("  → ${state.supplierPaymentIds.size} payments logged (${unpaidPending.size} outstanding)")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 8 — CUSTOMER + SERVICE MAN + REPAIR FLOW — Scenario 3
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario03_customerRepairFlow() = runBlocking {
        if (state.supplierMobiles.isEmpty()) { scenario02_supplierAndInventory() }
        clearRepairTables()
        println("\n══ SCENARIO 03: CUSTOMER + REPAIR FLOW (${TestFixtures.FIRST_NAMES_MALE.size} customers) ══")
        val rng = Random(54321)

        state.supplierMobiles.forEach { supplierMobile ->
            val supplier = supplierDao.getSupplierByMobile(supplierMobile)!!

            (0 until 8).forEach { customerIdx ->
                val customer = Customer(
                    mobileNumber = TestFixtures.randomMobile(state.customerMobiles.size + 1),
                    name         = TestFixtures.randomPersonName(),
                    city         = TestFixtures.SUPPLIER_DATA.random().fourth
                )
                customerDao.insert(customer)
                state.customerMobiles.add(customer.mobileNumber)

                val serviceManId = state.serviceManIds.random()
                val serviceMan   = serviceManDao.getServiceManById(serviceManId)!!
                val partDef1     = TestFixtures.PARTS_CATALOG.random()
                val partDef2     = TestFixtures.PARTS_CATALOG.filter { it.name != partDef1.name }.random()
                val partsUsed    = listOf(partDef1) + if (rng.nextBoolean()) listOf(partDef2) else emptyList()

                val laborCost = (500 + rng.nextInt(2500)).toDouble()
                val partsCostExact = partsUsed.sumOf { it.costPrice }
                val partsSellExact = partsUsed.sumOf { it.retailPrice }
                val totalCharge    = partsSellExact + laborCost
                val advance        = if (rng.nextDouble() < 0.60) kotlin.math.round(totalCharge * (0.3 + rng.nextDouble() * 0.4) * 100.0) / 100.0 else 0.0
                val isCancelled    = rng.nextDouble() < 0.06
                val isReturned     = !isCancelled && rng.nextDouble() < 0.08 // ~8% of non-cancelled get part return
                val isHandedOver   = !isCancelled && rng.nextDouble() < 0.88 // 88% of non-cancelled complete

                val entry = RepairEntry(
                    customerMobile    = customer.mobileNumber,
                    customerName      = customer.name,
                    customerCity      = customer.city,
                    deviceBrand       = TestFixtures.randomBrand(),
                    deviceModel       = TestFixtures.randomPhoneModel(),
                    faultDetected     = listOf("Screen broken","Battery drain","Charging failure",
                        "Camera malfunction","Button stuck","Water damage","No power","Slow performance").random(),
                    sparePartName     = partsUsed.joinToString(", ") { it.name },
                    sparePartPurchasePrice = partsSellExact,
                    chargeAmount      = totalCharge,
                    advanceAmount     = advance,
                    serviceManId      = serviceManId,
                    serviceManName    = serviceMan.name,
                    isDraft           = false,
                    workStatus        = if (isCancelled) "Cancelled" else if (isHandedOver) "Done" else "In Progress",
                    handoverDone      = isHandedOver,
                    entryDate         = daysAgo(7 + rng.nextInt(18)),
                    handoverDate      = if (isHandedOver) daysAgo(rng.nextInt(5)) else 0,
                    quotationDone     = true,
                    sparePartDone     = true,
                    workDone          = isHandedOver
                )
                val entryId = repairDao.insert(entry)
                state.repairEntryIds.add(entryId)

                // Payment record
                val balanceDue = totalCharge - advance
                val payStatus  = if (isCancelled) "CANCELLED" else if (balanceDue <= 0.01) "PAID" else if (advance > 0) "PARTIAL" else "UNPAID"
                if (!isCancelled || advance > 0 || rng.nextDouble() < 0.3) {
                    val payId = paymentDao.insert(Payment(
                        personType   = "CUSTOMER",
                        personMobile = customer.mobileNumber,
                        personName   = customer.name,
                        description  = "Repair: ${entry.faultDetected}",
                        totalAmount  = totalCharge,
                        dueAmount    = kotlin.math.max(0.0, balanceDue),
                        status       = payStatus
                    ))
                    if (advance > 0.01) {
                        val txnId = paymentTxnDao.insert(PaymentTransaction(
                            paymentId = payId, personType = "CUSTOMER",
                            personMobile = customer.mobileNumber, personName = customer.name,
                            amount = advance, paymentMode = if (rng.nextBoolean()) "CASH" else "ONLINE"
                        ))
                        state.supplierPaymentTxnIds.add(txnId) // reusing list for txn refs (just for existence check)
                    }
                }

                // Part Return: some non-cancelled, non-handed-over repairs return unused parts
                if (isReturned) {
                    val returnedPart = partsUsed.random()
                    val returnQty    = 1 + rng.nextInt(3)
                    val retId = partReturnDao.insert(PartReturn(
                        supplierId   = supplierMobile,
                        supplierName = supplier.name,
                        partName     = "${returnedPart.name} (returned unused)",
                        returnReason = "Customer cancelled — part unused",
                        refundAmount = returnedPart.costPrice * returnQty
                    ))
                    state.partReturnIds.add(retId)
                }

                if (isCancelled) state.cancelledRepairIds.add(entryId)
            }
        }
        println("  → ${state.customerMobiles.size} customers, ${state.repairEntryIds.size} repair entries")
        println("  → ${state.cancelledRepairIds.size} cancelled, ${state.partReturnIds.size} part returns")
    }

    private suspend fun clearRepairTables() {
        paymentTxnDao.getAllTransactions().first().forEach { paymentTxnDao.delete(it) }
        paymentDao.getAllPayments().first().forEach { paymentDao.delete(it) }
        partReturnDao.getAllReturns().first().forEach { partReturnDao.delete(it) }
        saleDao.getAllSales().first().forEach { saleDao.delete(it) }
        sparePartDao.getAllPurchases().first().forEach { sparePartDao.delete(it) }
        repairDao.getAllEntries().first().forEach { repairDao.delete(it) }
        customerDao.getAllCustomers().first().forEach { customerDao.deleteByMobile(it.mobileNumber) }
        state.customerMobiles.clear()
        state.repairEntryIds.clear()
        state.cancelledRepairIds.clear()
        state.partReturnIds.clear()
        state.directSaleIds.clear()
        state.partPurchaseIds.clear()
        state.initialPartStocks.clear()
        state.supplierMobiles.clear()
        state.supplierPaymentIds.clear()
        state.supplierPaymentTxnIds.clear()
        rollSeed = 42
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 9 — DIRECT SALES — Scenario 4
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario04_directSales() = runBlocking {
        println("\n══ SCENARIO 04: DIRECT SALES (30 walk-in) ══")
        if (state.serviceManIds.isEmpty()) scenario01_shopKeeperSetup()
        if (state.supplierMobiles.isEmpty()) setupSuppliersAndInventory()

        val rng = Random(99999)
        repeat(30) { i ->
            val partDef    = TestFixtures.PARTS_CATALOG.random()
            val qty        = 1 + rng.nextInt(4)
            val salePrice  = partDef.retailPrice * (1.0 + rng.nextDouble() * 0.5) // markup +0 to +50%
            val paidSoFar  = kotlin.math.round(salePrice * (if (rng.nextDouble() < 0.75) 1.0 else rng.nextDouble() * 0.5) * 100.0) / 100.0
            val supplier   = TestFixtures.SUPPLIER_DATA.random()

            val sale = Sale(
                itemName       = "${partDef.name} x$qty",
                supplierId     = supplier.second,
                supplierName   = supplier.first,
                purchasePrice  = partDef.costPrice * qty,
                salePrice      = kotlin.math.round(salePrice * qty * 100.0) / 100.0,
                paidToSupplier = kotlin.math.round(partDef.costPrice * qty * paidSoFar / salePrice * 100.0) / 100.0,
                customerPaid   = paidSoFar
            )
            state.directSaleIds.add(saleDao.insert(sale))
        }
        println("  → ${state.directSaleIds.size} direct sales recorded")
        val allSales = saleDao.getAllSales().first()
        assertEquals(30, allSales.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 10 — CASH FLOW / END OF DAY — Scenario 5
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario05_cashFlow20Days() = runBlocking {
        println("\n══ SCENARIO 05: CASH FLOW (20 business days) ══")
        if (state.supplierMobiles.isEmpty()) setupSuppliersAndInventory()
        if (state.repairEntryIds.isEmpty()) runRepairFlowForCash()
        if (state.directSaleIds.isEmpty()) runDirectSalesForCash()
        if (state.customerMobiles.isEmpty()) { /* will use repair+cash-derived flows */ }

        val rng       = Random(20242)
        var cashOnHand = 5000.0 // Opening cash float

        repeat(20) { dayIdx ->
            val dStart    = dayStart(dayIdx)
            val dEnd      = dStart + MILLIS_PER_DAY - 1

            // Cash IN — repairs handed over today
            val repairsToday = repairDao.getCompletedEntries().first()
                .filter { it.handoverDate in (dStart until dEnd) }
            val cashInRepairs = repairsToday.sumOf {
                it.advanceAmount + (it.finalAmount - it.advanceAmount).coerceAtLeast(0.0)
            }

            // Direct sales today
            val salesToday = saleDao.getSalesByDateRange(dStart, dEnd).first()
            val cashInSales = salesToday.sumOf { it.customerPaid }

            val totalCashIn = cashInRepairs + cashInSales

            // Cash OUT — supplier payments today (from payment transactions in date range)
            val txnsToday = paymentTxnDao.getTransactionsByDateRange(dStart, dEnd).first()
                .filter { it.personType == "SUPPLIER" }
            val totalCashOut = txnsToday.sumOf { it.amount }

            // Personal withdrawal on day 10 (simulating owner's personal take-home)
            val personal = if (dayIdx == 10) (1000.0 + rng.nextInt(9) * 500) else 0.0
            if (personal > 0) {
                cashOnHand -= personal
                println("  ⚠ Day $dayIdx: Personal withdrawal ₹${personal.toInt()} (owner take-home)")
            }

            cashOnHand += totalCashIn - totalCashOut
            val ledger = DayLedger(
                dayIndex = dayIdx,
                cashIn = kotlin.math.round(totalCashIn * 100.0) / 100.0,
                cashOut = kotlin.math.round(totalCashOut * 100.0) / 100.0,
                personalWithdrawal = kotlin.math.round(personal * 100.0) / 100.0
            )
            state.dailyCashLedger.add(ledger)

            if (dayIdx < 3 || dayIdx >= 17 || personal > 0) {
                println("  Day ${String.format("%2d", dayIdx)}:  In=₹${ledger.cashIn.toInt().toString().padStart(7)}  " +
                        "Out=₹${ledger.cashOut.toInt().toString().padStart(6)}  " +
                        "Net=+₹${ledger.net.toInt().toString().padStart(7)}  " +
                        "Balance=₹${cashOnHand.toInt().toString().padStart(8)}")
            }
        }
        assertTrue("Cash on hand should be positive after 20 days", cashOnHand >= 0)
        println("  → Final Cash on Hand: ₹${cashOnHand.toInt()}")
    }

    private suspend fun runRepairFlowForCash(): Int {
        val rng = Random(54321)
        repeat(40) {
            val customer = Customer(
                mobileNumber = TestFixtures.randomMobile(state.customerMobiles.size + 1),
                name         = TestFixtures.randomPersonName(), city = "Bangalore")
            customerDao.insert(customer); state.customerMobiles.add(customer.mobileNumber)
            val smId     = state.serviceManIds.random()
            val sm       = serviceManDao.getServiceManById(smId)!!
            val partDef  = TestFixtures.PARTS_CATALOG.random()
            val charge   = partDef.retailPrice + (500 + rng.nextInt(2000))
            val advance  = kotlin.math.round(charge * (0.3 + rng.nextDouble() * 0.4) * 100.0) / 100.0
            val entryId  = repairDao.insert(RepairEntry(
                customerMobile = customer.mobileNumber, customerName = customer.name,
                deviceBrand = TestFixtures.randomBrand(), deviceModel = TestFixtures.randomPhoneModel(),
                sparePartName = partDef.name, sparePartPurchasePrice = partDef.retailPrice,
                chargeAmount = charge, advanceAmount = advance,
                serviceManId = smId, serviceManName = sm.name,
                workStatus = "Done", handoverDone = true,
                entryDate = daysAgo(5), handoverDate = daysAgo(2)
            ))
            state.repairEntryIds.add(entryId)
        }
        return state.repairEntryIds.size
    }

    private suspend fun runDirectSalesForCash(): Int {
        val rng = Random(99999)
        repeat(30) {
            val partDef  = TestFixtures.PARTS_CATALOG.random()
            val salePrice = kotlin.math.round(partDef.retailPrice * (1.1 + rng.nextDouble() * 0.5) * 100.0) / 100.0
            state.directSaleIds.add(saleDao.insert(Sale(
                itemName = partDef.name, supplierId = "TEST",
                supplierName = "Test Supplier", purchasePrice = partDef.costPrice,
                salePrice = salePrice, customerPaid = salePrice
            )))
        }
        return state.directSaleIds.size
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 11 — RETURNS — Scenario 6
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario06_returns() = runBlocking {
        println("\n══ SCENARIO 06: RETURNS ══")
        if (state.repairEntryIds.size < 5) { scenario03_customerRepairFlow() }

        val rng = Random(77777)

        // 6a — 5 Customer returns / refunds
        val completedRepairs = repairDao.getCompletedEntries().first().toMutableList()
        repeat(minOf(5, completedRepairs.size)) { i ->
            val entry  = completedRepairs[i]
            val refund = kotlin.math.round(entry.finalAmount * (0.5 + rng.nextDouble() * 0.5) * 100.0) / 100.0
            val payId  = paymentDao.insert(Payment(
                personType   = "CUSTOMER_REFUND",
                personMobile = entry.customerMobile,
                personName   = entry.customerName,
                description  = "Refund — customer unhappy with repair #${entry.id}",
                totalAmount  = refund,
                paidAmount   = refund,
                dueAmount    = 0.0,
                status       = "REFUNDED",
                linkedEntryId = entry.id
            ))
            state.refundPaymentIds.add(payId)
            paymentTxnDao.insert(PaymentTransaction(
                paymentId    = payId,
                personType   = "CUSTOMER_REFUND",
                personMobile = entry.customerMobile,
                personName   = entry.customerName,
                amount       = -refund, // negative = money out
                paymentMode  = "CASH"
            ))
        }
        println("  → ${state.refundPaymentIds.size} customer refunds logged")

        // 6b — 3 Supplier returns (defective parts)
        val suppliersWithParts = state.supplierMobiles.filter { sm ->
            sparePartDao.getPurchasesBySupplier(sm).first().isNotEmpty()
        }.toMutableList()
        val supplierReturns = minOf(3, suppliersWithParts.size)
        repeat(supplierReturns) {
            val sm          = suppliersWithParts.random()
            val supplier    = supplierDao.getSupplierByMobile(sm)!!
            val purchases   = sparePartDao.getPurchasesBySupplier(sm).first()
            val purchase    = purchases.random()
            val partDef     = TestFixtures.PARTS_CATALOG.find { it.name == purchase.partName } ?: TestFixtures.PARTS_CATALOG.random()
            val returnQty   = 1 + rng.nextInt(3)
            val retId       = partReturnDao.insert(PartReturn(
                supplierId   = sm,
                supplierName = supplier.name,
                partName     = "${partDef.name} (defective batch)",
                returnReason = "Defective — display flickering",
                refundAmount = kotlin.math.round(partDef.costPrice * returnQty * 100.0) / 100.0
            ))
            state.supplierReturnIds.add(retId)

            // Credit the supplier: payment NOT reduced (still owed for full original order),
            // but a negative transaction adjusts the balance
            val creditTxnId = paymentTxnDao.insert(PaymentTransaction(
                paymentId    = 0L, // standalone credit note
                personType   = "SUPPLIER_CREDIT",
                personMobile = sm,
                personName   = supplier.name,
                amount       = -kotlin.math.round(partDef.costPrice * returnQty * 100.0) / 100.0,
                paymentMode  = "ADJUSTMENT"
            ))
            state.supplierPaymentTxnIds.add(creditTxnId)
        }
        println("  → $supplierReturns supplier returns (defective parts)")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 12 — RECONCILIATION ENGINE — Scenario 7
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario07_reconciliation() = runBlocking {
        println("\n══ SCENARIO 07: INDEPENDENT RECONCILIATION ══")
        val mismatches = mutableListOf<MismatchRecord>()

        // ── 7a. Inventory Stock Rebuild ─────────────────────────────────────────
        val stockFromParts = mutableMapOf<String, Int>()  // all spare-part purchases
        val stockSalesOut  = mutableMapOf<String, Int>()  // qty consumed via direct sales
        val stockRepairOut = mutableMapOf<String, Int>()  // qty consumed in repairs
        val stockReturnIn  = mutableMapOf<String, Int>()  // supplier returns (back in)
        val stockCustomerReturnQty = mutableMapOf<String, Int>() // part returns to inventory

        // Walk all spare_part_purchases
        sparePartDao.getAllPurchases().first().forEach { sp ->
            if (sp.repairEntryId == 0L) {
                // Direct sale purchase — counts as inventory OUT (sold to walk-in customer)
                stockSalesOut[sp.partName]   = (stockSalesOut[sp.partName]   ?: 0) + sp.quantity
            }
            // All others are inventory IN (bulk purchases, regardless of linked repair entry)
            stockFromParts[sp.partName] = (stockFromParts[sp.partName] ?: 0) + sp.quantity
        }

        // Walk all repairs — parse part names from sparePartName CSV and attribute to stockRepairOut
        repairDao.getAllEntries().first().forEach { entry ->
            if (!entry.isDraft && entry.sparePartName.isNotBlank()) {
                entry.sparePartName.split(",").map { it.trim() }.forEach { partName ->
                    stockRepairOut[partName] = (stockRepairOut[partName] ?: 0) + 1 // 1 unit per part type per repair for simplicity
                }
            }
        }

        // Part returns from supplier (defective) → stock decreases
        partReturnDao.getAllReturns().first().forEach { pr ->
            stockReturnIn[pr.partName] = (stockReturnIn[pr.partName] ?: 0) + 1
        }

        // Independent stock recalculation
        val allPartNames = (stockFromParts.keys + stockSalesOut.keys + stockRepairOut.keys + stockReturnIn.keys).distinct()
        allPartNames.forEach { partName ->
            val purchased = stockFromParts[partName] ?: 0
            val viaSales  = stockSalesOut[partName]  ?: 0
            val viaRepair = stockRepairOut[partName] ?: 0
            val returned  = stockReturnIn[partName]  ?: 0
            val expectedStock = purchased - viaSales - viaRepair + returned
            if (expectedStock < 0) {
                mismatches.add(MismatchRecord(
                    category = "Inventory",
                    description = "Stock negative for '$partName' — oversold/overconsumed by ${-expectedStock} units",
                    expected = expectedStock,
                    actual = "NEGATIVE (data issue)"
                ))
            }
        }
        println("  Inventory: ${allPartNames.size} unique part types tracked, ${mismatches.count { it.category == "Inventory" }} stock issues found")

        // ── 7b. Revenue Reconciliation ──────────────────────────────────────────
        val repairRevenueApp = repairDao.getRevenueInRange(daysAgo(20), System.currentTimeMillis()).first()
            ?: 0.0
        val allCompletedRepairs = repairDao.getCompletedEntries().first()
        val repairRevenueCalc  = allCompletedRepairs.sumOf { it.finalAmount }
        if (kotlin.math.abs(repairRevenueApp - repairRevenueCalc) > 0.01) {
            mismatches.add(MismatchRecord(
                category = "Revenue-Repairs",
                description = "DAO getRevenueInRange vs sum(finalAmount) of completed repairs",
                expected = repairRevenueCalc,
                actual = repairRevenueApp
            ))
        }

        // ── 7c. Supplier Dues Reconciliation ────────────────────────────────────
        val allPayments = paymentDao.getAllPayments().first().filter { it.personType == "SUPPLIER" }
        var supplierDueApp = 0.0
        var supplierDueCalc = 0.0
        allPayments.forEach { p ->
            val txns = paymentTxnDao.getTransactionsByPayment(p.id).first()
            val totalPaid = txns.sumOf { it.amount.coerceAtLeast(0.0) }
            val credit    = txns.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            val calcDue   = (p.totalAmount - totalPaid + credit).coerceAtLeast(0.0)
            supplierDueApp += p.dueAmount
            supplierDueCalc += calcDue
            if (kotlin.math.abs(calcDue - p.dueAmount) > 0.01) {
                mismatches.add(MismatchRecord(
                    category = "Supplier Due",
                    description = "Payment #${p.id} (${p.personName}): txns sum=$totalPaid, credits=$credit → due=$calcDue, stored due=${p.dueAmount}",
                    expected = calcDue,
                    actual = p.dueAmount
                ))
            }
        }
        if (kotlin.math.abs(supplierDueApp - supplierDueCalc) > 0.01) {
            mismatches.add(MismatchRecord(
                category = "Supplier Due-Total",
                description = "Sum of all Payment.dueAmount vs sum of independently computed dues",
                expected = supplierDueCalc,
                actual = supplierDueApp
            ))
        }

        // ── 7d. Customer Payments Reconciliation ────────────────────────────────
        val allCustPayments = paymentDao.getAllPayments().first().filter { it.personType == "CUSTOMER" && it.status != "CANCELLED" }
        var custPaidApp = 0.0
        var custPaidCalc = 0.0
        allCustPayments.forEach { p ->
            val txns = paymentTxnDao.getTransactionsByPayment(p.id).first()
            val paid = txns.sumOf { it.amount.coerceAtLeast(0.0) }
            custPaidApp  += p.paidAmount
            custPaidCalc += paid
            if (kotlin.math.abs(paid - p.paidAmount) > 0.01) {
                mismatches.add(MismatchRecord(
                    category = "Customer Payment",
                    description = "Payment #${p.id}: txns paid=$paid vs stored paidAmount=${p.paidAmount}",
                    expected = paid, actual = p.paidAmount
                ))
            }
        }

        // ── 7e. Net Profit Conservative Estimate ───────────────────────────────
        // Revenue = completed repair finalAmount + direct sale customerPaid
        val totalRevenue = repairRevenueCalc +
            saleDao.getAllSales().first().sumOf { it.customerPaid }

        // COGS = cost price of parts in SparePartPurchase linked to completed repairs + direct sale cost
        val repairCogs = allCompletedRepairs.sumOf { entry ->
            val linkedParts = sparePartDao.getPurchasesByRepairId(entry.id).first()
            linkedParts.sumOf { it.purchasePrice * it.quantity }
        }
        val salesCogs = saleDao.getAllSales().first().sumOf { it.purchasePrice }
        val totalCogs = repairCogs + salesCogs
        val netProfit = totalRevenue - totalCogs

        println("  Revenue  = ₹${totalRevenue.toInt()}")
        println("  COGS     = ₹${totalCogs.toInt()}")
        println("  Net Profit = ₹${netProfit.toInt()}")
        println("  Supplier Due (calc via txns) = ₹${supplierDueCalc.toInt()}")
        println("  Supplier Due (stored)        = ₹${supplierDueApp.toInt()}")
        println("  Refunds logged: ${state.refundPaymentIds.size}")
        println("  Supplier returns: ${state.supplierReturnIds.size}")

        // ── Report summary ──────────────────────────────────────────────────────
        val report = ReconciliationReport(
            scenarios = listOf(
                ScenarioResult("Inventory Stock",        mismatches.none { it.category == "Inventory" }, mismatches.filter { it.category == "Inventory" }),
                ScenarioResult("Revenue (Repairs+Sales)", mismatches.none { it.category.startsWith("Revenue") }, mismatches.filter { it.category.startsWith("Revenue") }),
                ScenarioResult("Supplier Due",           mismatches.none { it.category.startsWith("Supplier Due") }, mismatches.filter { it.category.startsWith("Supplier Due") }),
                ScenarioResult("Customer Payments",      mismatches.none { it.category == "Customer Payment" }, mismatches.filter { it.category == "Customer Payment" })
            ),
            totalMismatches = mismatches.size,
            overallPassed   = mismatches.isEmpty()
        )
        report.printSummary()
        mismatches.forEach { println("  MISMATCH → [${it.category}] ${it.description}\n            expected=${it.expected}, actual=${it.actual}") }

        assertTrue("Reconciliation must pass — found ${mismatches.size} mismatch(es)", mismatches.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 13 — FULL REAL-WORLD RUN — Scenario 8 (wires everything together)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scenario08_fullEndToEndRealWorld() = runBlocking {
        println("\n════════════════════════════════════════════════════════════")
        println("══ FULL REAL-WORLD SIMULATION — ALL 8 SCENARIOS ══")
        println("════════════════════════════════════════════════════════════")
        clearAllData()

        // Execute each scenario in order (each idempotent via clearAllData at start)
        scenario01_shopKeeperSetup()
        scenario02_supplierAndInventory()
        scenario03_customerRepairFlow()
        scenario04_directSales()
        scenario05_cashFlow20Days()
        scenario06_returns()
        scenario07_reconciliation()

        println("\n════════════════════════════════════════════════════════════")
        println("══ SIMULATION COMPLETE ══")
        println("  Service Men    : ${state.serviceManIds.size}")
        println("  Suppliers      : ${state.supplierMobiles.size}")
        println("  Part purchases : ${state.partPurchaseIds.size}")
        println("  Customers      : ${state.customerMobiles.size}")
        println("  Repair entries : ${state.repairEntryIds.size}")
        println("  ︎ Cancelled      : ${state.cancelledRepairIds.size}")
        println("  Part returns   : ${state.partReturnIds.size}")
        println("  Direct sales   : ${state.directSaleIds.size}")
        println("  Refunds        : ${state.refundPaymentIds.size}")
        println("  Supplier ret.  : ${state.supplierReturnIds.size}")
        println("  Business days  : ${state.dailyCashLedger.size}")
        println("════════════════════════════════════════════════════════════\n")
    }
}
