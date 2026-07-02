package com.app.muzzutech

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.db.dao.*
import com.app.muzzutech.data.model.*
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random

data class SupplierDef(val company: String, val name: String, val mobile: String, val city: String)
data class ServiceManDef(val name: String, val mobile: String, val email: String, val designation: String)

data class MismatchRecord(val category: String, val description: String, val expected: Any, val actual: Any)
data class ScenarioResult(val name: String, val passed: Boolean, val mismatches: List<MismatchRecord>) {
    fun orFail() { if (!passed) fail("FAILED: $name\n" + mismatches.joinToString("\n") { "  [${it.category}] expected=${it.expected}, actual=${it.actual} - ${it.description}" }) }
}
data class ReconciliationReport(val scenarios: List<ScenarioResult>, val totalMismatches: Int, val overallPassed: Boolean) {
    fun printSummary() {
        println("\n${if (overallPassed) "OVERALL PASS" else "OVERALL FAIL"}")
        scenarios.forEach { println("  ${if (it.passed) "PASS" else "FAIL"} ${it.name} (${it.mismatches.size} mismatches)") }
        println("Total mismatches: $totalMismatches")
    }
}

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
    private val MILLIS_PER_DAY = 86_400_000L

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().fallbackToDestructiveMigration().build()
        repairDao = db.repairEntryDao(); serviceManDao = db.serviceManDao(); supplierDao = db.supplierDao()
        commonFaultDao = db.commonFaultDao(); sparePartDao = db.sparePartPurchaseDao(); customerDao = db.customerDao()
        dealerDao = db.dealerDao(); saleDao = db.saleDao(); userProfileDao = db.userProfileDao()
        paymentDao = db.paymentDao(); partReturnDao = db.partReturnDao(); paymentTxnDao = db.paymentTransactionDao()
    }

    @After @Throws(IOException::class) fun closeDb() { db.close() }

    private fun daysAgo(d: Int, h: Int = 10): Long = System.currentTimeMillis() - d * MILLIS_PER_DAY + h * 3600_000L

    // ========================================================================
    // SECTION 1: UserProfile DAO — 5 scenarios (001-005)
    // ========================================================================
    @Test fun sc001_userProfileInsertAndRead() = runBlocking {
        val p = UserProfile(id=1,shopName="Test Shop",name="Owner",email="o@t.com",phone="1111111111",shopAddress="Addr",gstNo="GST001")
        userProfileDao.insertOrUpdate(p)
        val r = userProfileDao.getUserProfile()
        assertNotNull(r); assertEquals("Test Shop",r!!.shopName)
        println("SC001: UserProfile insert+read PASS")
    }
    @Test fun sc002_userProfileUpdate() = runBlocking {
        userProfileDao.insertOrUpdate(UserProfile(id=1,shopName="Old",name="Owner",email="o@t.com",phone="111"))
        userProfileDao.insertOrUpdate(UserProfile(id=1,shopName="New",name="Owner",email="o@t.com",phone="111"))
        assertEquals("New",userProfileDao.getUserProfile()!!.shopName)
        println("SC002: UserProfile update PASS")
    }
    @Test fun sc003_userProfileFlow() = runBlocking {
        userProfileDao.insertOrUpdate(UserProfile(id=1,shopName="Flow",name="O",email="e",phone="1"))
        val f = userProfileDao.getUserProfileFlow().first()
        assertEquals("Flow",f!!.shopName)
        println("SC003: UserProfile flow PASS")
    }
    @Test fun sc004_userProfileDelete() = runBlocking {
        userProfileDao.insertOrUpdate(UserProfile(id=1,shopName="Del",name="O",email="e",phone="1"))
        userProfileDao.delete(userProfileDao.getUserProfile()!!)
        assertNull(userProfileDao.getUserProfile())
        println("SC004: UserProfile delete PASS")
    }
    @Test fun sc005_userProfileEdgeEmpty() = runBlocking {
        userProfileDao.insertOrUpdate(UserProfile(id=1))
        val r = userProfileDao.getUserProfile()
        assertNotNull(r); assertTrue(r!!.shopName.isEmpty())
        println("SC005: UserProfile empty fields PASS")
    }

    // ========================================================================
    // SECTION 2: ServiceMan DAO — 8 scenarios (006-013)
    // ========================================================================
    @Test fun sc006_serviceManInsert() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="Raj",mobile="9900111000",email="r@t.com",employeeId="SM01",designation="Tech"))
        assertTrue(id > 0)
        assertEquals("Raj",serviceManDao.getServiceManById(id)!!.name)
        println("SC006: ServiceMan insert PASS")
    }
    @Test fun sc007_serviceManUpdate() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="Raj",mobile="9900111000",email="r@t.com",employeeId="SM01",designation="Tech"))
        serviceManDao.update(serviceManDao.getServiceManById(id)!!.copy(name="Raj Updated",designation="Senior Tech"))
        assertEquals("Raj Updated",serviceManDao.getServiceManById(id)!!.name)
        println("SC007: ServiceMan update PASS")
    }
    @Test fun sc008_serviceManDelete() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="Del",mobile="9900111001",email="d@t.com",employeeId="SM02",designation="Tech"))
        serviceManDao.delete(serviceManDao.getServiceManById(id)!!)
        assertNull(serviceManDao.getServiceManById(id))
        println("SC008: ServiceMan delete PASS")
    }
    @Test fun sc009_serviceManGetAll() = runBlocking {
        serviceManDao.insert(ServiceMan(name="A",mobile="1",email="a@t.com",employeeId="E1",designation="T"))
        serviceManDao.insert(ServiceMan(name="B",mobile="2",email="b@t.com",employeeId="E2",designation="T"))
        assertTrue(serviceManDao.getAllServiceMen().first().size >= 2)
        println("SC009: ServiceMan getAll (>=2) PASS")
    }
    @Test fun sc010_serviceManActiveFilter() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="A",mobile="1",email="a",employeeId="E1",designation="T"))
        serviceManDao.update(serviceManDao.getServiceManById(id)!!.copy(isActive=false))
        val activeMen = serviceManDao.getActiveServiceMen().first()
        assertFalse(activeMen.any { it.id == id })
        println("SC010: ServiceMan active filter PASS")
    }
    @Test fun sc011_serviceManFlow() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="Flow",mobile="3",email="f",employeeId="E2",designation="T"))
        assertNotNull(serviceManDao.getServiceManByIdFlow(id).first())
        println("SC011: ServiceMan flow PASS")
    }
    @Test fun sc012_serviceManEdgeEmptyEmail() = runBlocking {
        val id = serviceManDao.insert(ServiceMan(name="NoEmail",mobile="4",email="",employeeId="E3",designation="T"))
        assertTrue(id > 0)
        println("SC012: ServiceMan empty email PASS")
    }
    @Test fun sc013_serviceManEdgeLongName() = runBlocking {
        val long = "X".repeat(500)
        val id = serviceManDao.insert(ServiceMan(name=long,mobile="5",email="l",employeeId="E4",designation=long))
        assertEquals(long,serviceManDao.getServiceManById(id)!!.name)
        println("SC013: ServiceMan long name PASS")
    }

    // ========================================================================
    // SECTION 3: CommonFault DAO — 8 scenarios (014-021)
    // ========================================================================
    @Test fun sc014_commonFaultInsert() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Screen Broken",category="Display",defaultCharge=500.0,sortOrder=1))
        assertTrue(id > 0)
        assertEquals("Screen Broken",commonFaultDao.getFaultById(id)!!.faultName)
        println("SC014: CommonFault insert PASS")
    }
    @Test fun sc015_commonFaultUpdate() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Old",category="Display",sortOrder=1))
        commonFaultDao.update(commonFaultDao.getFaultById(id)!!.copy(faultName="New",defaultCharge=1000.0))
        assertEquals("New",commonFaultDao.getFaultById(id)!!.faultName)
        assertEquals(1000.0,commonFaultDao.getFaultById(id)!!.defaultCharge,0.01)
        println("SC015: CommonFault update PASS")
    }
    @Test fun sc016_commonFaultDelete() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Del",category="Display",sortOrder=1))
        commonFaultDao.delete(commonFaultDao.getFaultById(id)!!)
        assertNull(commonFaultDao.getFaultById(id))
        println("SC016: CommonFault delete PASS")
    }
    @Test fun sc017_commonFaultGetAll() = runBlocking {
        commonFaultDao.insert(CommonFault(faultName="F1",category="A",sortOrder=1))
        commonFaultDao.insert(CommonFault(faultName="F2",category="B",sortOrder=2))
        assertTrue(commonFaultDao.getAllFaults().first().size >= 2)
        println("SC017: CommonFault getAll (>=2) PASS")
    }
    @Test fun sc018_commonFaultActiveFilter() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Inactive",category="A",sortOrder=1,isActive=false))
        val active = commonFaultDao.getActiveFaults().first()
        assertFalse(active.any { it.id == id })
        println("SC018: CommonFault active filter PASS")
    }
    @Test fun sc019_commonFaultByCategory() = runBlocking {
        commonFaultDao.insert(CommonFault(faultName="Screen",category="Display",sortOrder=1))
        commonFaultDao.insert(CommonFault(faultName="Battery",category="Battery",sortOrder=2))
        val display = commonFaultDao.getFaultsByCategory("Display").first()
        assertTrue(display.any { it.faultName == "Screen" })
        println("SC019: CommonFault by category PASS")
    }
    @Test fun sc020_commonFaultEdgeZeroCharge() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Free",category="Other",defaultCharge=0.0,sortOrder=0))
        assertEquals(0.0,commonFaultDao.getFaultById(id)!!.defaultCharge,0.01)
        println("SC020: CommonFault zero charge PASS")
    }
    @Test fun sc021_commonFaultEdgeNegativeSortOrder() = runBlocking {
        val id = commonFaultDao.insert(CommonFault(faultName="Neg",category="A",sortOrder=-1))
        assertEquals(-1,commonFaultDao.getFaultById(id)!!.sortOrder)
        println("SC021: CommonFault negative sort PASS")
    }

    // ========================================================================
    // SECTION 4: Supplier DAO — 8 scenarios (022-029)
    // ========================================================================
    @Test fun sc022_supplierInsert() = runBlocking {
        supplierDao.insert(Supplier(mobile="9398123456",name="Rajesh",companyName="Rajesh Mobile Parts",city="Mumbai"))
        val s = supplierDao.getSupplierByMobile("9398123456")
        assertNotNull(s); assertEquals("Rajesh",s!!.name)
        println("SC022: Supplier insert PASS")
    }
    @Test fun sc023_supplierUpdate() = runBlocking {
        supplierDao.insert(Supplier(mobile="9398123456",name="Old",companyName="C1",city="C1"))
        supplierDao.update(Supplier(mobile="9398123456",name="New",companyName="C2",city="C2"))
        assertEquals("New",supplierDao.getSupplierByMobile("9398123456")!!.name)
        println("SC023: Supplier update PASS")
    }
    @Test fun sc024_supplierDelete() = runBlocking {
        supplierDao.insert(Supplier(mobile="9398123456",name="Del",companyName="C",city="C"))
        supplierDao.delete(supplierDao.getSupplierByMobile("9398123456")!!)
        assertNull(supplierDao.getSupplierByMobile("9398123456"))
        println("SC024: Supplier delete PASS")
    }
    @Test fun sc025_supplierGetAll() = runBlocking {
        supplierDao.insert(Supplier(mobile="1",name="A",companyName="CA",city="C"))
        supplierDao.insert(Supplier(mobile="2",name="B",companyName="CB",city="C"))
        assertTrue(supplierDao.getAllSuppliers().first().size >= 2)
        println("SC025: Supplier getAll (>=2) PASS")
    }
    @Test fun sc026_supplierActiveFilter() = runBlocking {
        supplierDao.insert(Supplier(mobile="1",name="A",companyName="C",city="C",isActive=false))
        val active = supplierDao.getActiveSuppliers().first()
        assertFalse(active.any { it.mobile == "1" })
        println("SC026: Supplier active filter PASS")
    }
    @Test fun sc027_supplierFlow() = runBlocking {
        supplierDao.insert(Supplier(mobile="1",name="F",companyName="C",city="C"))
        assertNotNull(supplierDao.getSupplierByMobileFlow("1").first())
        println("SC027: Supplier flow PASS")
    }
    @Test fun sc028_supplierEdgeEmptyEmail() = runBlocking {
        supplierDao.insert(Supplier(mobile="1",name="NoEmail",companyName="C",city="C",email="",address="",gstNo=""))
        val s = supplierDao.getSupplierByMobile("1")!!
        assertTrue(s.email.isEmpty())
        println("SC028: Supplier empty fields PASS")
    }
    @Test fun sc029_supplierEdgeMaxFields() = runBlocking {
        val long = "Z".repeat(500)
        supplierDao.insert(Supplier(mobile="1",name=long,companyName=long,email=long,address=long,city=long,gstNo=long, suppliesTypes=long))
        assertEquals(long,supplierDao.getSupplierByMobile("1")!!.name)
        println("SC029: Supplier long fields PASS")
    }

    // ========================================================================
    // SECTION 5: Customer DAO — 8 scenarios (030-037)
    // ========================================================================
    @Test fun sc030_customerInsert() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1111111111",name="Rahul",city="Delhi"))
        val c = customerDao.getCustomerByMobile("1111111111")
        assertNotNull(c); assertEquals("Rahul",c!!.name)
        println("SC030: Customer insert PASS")
    }
    @Test fun sc031_customerUpdate() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1111111111",name="Old",city="C1"))
        customerDao.update(Customer(mobileNumber="1111111111",name="New",city="C2"))
        assertEquals("New",customerDao.getCustomerByMobile("1111111111")!!.name)
        println("SC031: Customer update PASS")
    }
    @Test fun sc032_customerDelete() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1111111111",name="Del",city="C"))
        customerDao.deleteByMobile("1111111111")
        assertNull(customerDao.getCustomerByMobile("1111111111"))
        println("SC032: Customer delete PASS")
    }
    @Test fun sc033_customerGetAll() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1",name="A",city="C"))
        customerDao.insert(Customer(mobileNumber="2",name="B",city="C"))
        assertTrue(customerDao.getAllCustomers().first().size >= 2)
        println("SC033: Customer getAll (>=2) PASS")
    }
    @Test fun sc034_customerFlow() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1",name="F",city="C"))
        assertNotNull(customerDao.getCustomerByMobileFlow("1").first())
        println("SC034: Customer flow PASS")
    }
    @Test fun sc035_customerReplaceOnConflict() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1",name="First",city="C1"))
        customerDao.insert(Customer(mobileNumber="1",name="Second",city="C2"))
        assertEquals("Second",customerDao.getCustomerByMobile("1")!!.name)
        println("SC035: Customer replace on conflict PASS")
    }
    @Test fun sc036_customerEdgeNullName() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1",name=null,city=null))
        assertNull(customerDao.getCustomerByMobile("1")!!.name)
        println("SC036: Customer null name PASS")
    }
    @Test fun sc037_customerEdgeLongName() = runBlocking {
        val long = "A".repeat(500)
        customerDao.insert(Customer(mobileNumber="1",name=long,city=long))
        assertEquals(long,customerDao.getCustomerByMobile("1")!!.name)
        println("SC037: Customer long name PASS")
    }

    // ========================================================================
    // SECTION 6: Dealer DAO — 8 scenarios (038-045)
    // ========================================================================
    @Test fun sc038_dealerInsert() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="2222222222",name="Dealer A",city="Mumbai"))
        val d = dealerDao.getDealerByMobile("2222222222")
        assertNotNull(d); assertEquals("Dealer A",d!!.name)
        println("SC038: Dealer insert PASS")
    }
    @Test fun sc039_dealerUpdate() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="2222222222",name="Old",city="C1"))
        dealerDao.update(Dealer(mobileNumber="2222222222",name="New",city="C2"))
        assertEquals("New",dealerDao.getDealerByMobile("2222222222")!!.name)
        println("SC039: Dealer update PASS")
    }
    @Test fun sc040_dealerGetAll() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="1",name="A",city="C"))
        dealerDao.insert(Dealer(mobileNumber="2",name="B",city="C"))
        assertTrue(dealerDao.getAllDealers().first().size >= 2)
        println("SC040: Dealer getAll (>=2) PASS")
    }
    @Test fun sc041_dealerFlow() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="1",name="F",city="C"))
        assertNotNull(dealerDao.getDealerByMobileFlow("1").first())
        println("SC041: Dealer flow PASS")
    }
    @Test fun sc042_dealerReplaceOnConflict() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="1",name="First",city="C1"))
        dealerDao.insert(Dealer(mobileNumber="1",name="Second",city="C2"))
        assertEquals("Second",dealerDao.getDealerByMobile("1")!!.name)
        println("SC042: Dealer replace on conflict PASS")
    }
    @Test fun sc043_dealerEdgeNullName() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="1",name=null,city=null))
        assertNull(dealerDao.getDealerByMobile("1")!!.name)
        println("SC043: Dealer null name PASS")
    }
    @Test fun sc044_dealerEdgeEmptyName() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="1",name="",city=""))
        assertEquals("",dealerDao.getDealerByMobile("1")!!.name)
        println("SC044: Dealer empty name PASS")
    }
    @Test fun sc045_dealerEdgeLongName() = runBlocking {
        val long = "B".repeat(500)
        dealerDao.insert(Dealer(mobileNumber="1",name=long,city=long))
        assertEquals(long,dealerDao.getDealerByMobile("1")!!.name)
        println("SC045: Dealer long name PASS")
    }

    // ========================================================================
    // SECTION 7: RepairEntry DAO — 12 scenarios (046-057)
    // ========================================================================
    private fun seedEntry(): Long = runBlocking {
        repairDao.insert(RepairEntry(customerMobile="1111111111",customerName="C1",deviceBrand="Samsung",deviceModel="S21",
            faultDetected="Screen",sparePartName="Display",sparePartPurchasePrice=3200.0,supplierId=0L,workStatus="Pending"))
    }
    @Test fun sc046_repairEntryInsert() = runBlocking {
        val id = seedEntry()
        assertTrue(id > 0)
        assertNotNull(repairDao.getEntryById(id))
        println("SC046: RepairEntry insert PASS")
    }
    @Test fun sc047_repairEntryUpdate() = runBlocking {
        val id = seedEntry()
        repairDao.update(repairDao.getEntryById(id)!!.copy(workStatus="Done"))
        assertEquals("Done",repairDao.getEntryById(id)!!.workStatus)
        println("SC047: RepairEntry update PASS")
    }
    @Test fun sc048_repairEntryDelete() = runBlocking {
        val id = seedEntry()
        repairDao.delete(repairDao.getEntryById(id)!!)
        assertNull(repairDao.getEntryById(id))
        println("SC048: RepairEntry delete PASS")
    }
    @Test fun sc049_repairEntryGetAll() = runBlocking {
        seedEntry(); seedEntry()
        assertTrue(repairDao.getAllEntries().first().size >= 2)
        println("SC049: RepairEntry getAll (>=2) PASS")
    }
    @Test fun sc050_repairEntryPendingFilter() = runBlocking {
        val id = seedEntry()
        val pending = repairDao.getPendingEntries().first()
        assertTrue(pending.any { it.id == id })
        repairDao.update(repairDao.getEntryById(id)!!.copy(handoverDone=true,workStatus="Done"))
        assertFalse(repairDao.getPendingEntries().first().any { it.id == id })
        println("SC050: RepairEntry pending filter PASS")
    }
    @Test fun sc051_repairEntryCompletedFilter() = runBlocking {
        seedEntry()
        val id = repairDao.insert(RepairEntry(customerMobile="2",customerName="C2",deviceBrand="A",deviceModel="B",
            sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,workStatus="Done",handoverDone=true,handoverDate=daysAgo(1,0)))
        assertTrue(repairDao.getCompletedEntries().first().any { it.id == id })
        println("SC051: RepairEntry completed filter PASS")
    }
    @Test fun sc052_repairEntryByMobile() = runBlocking {
        seedEntry()
        val byMobile = repairDao.getEntriesByMobile("1111111111").first()
        assertTrue(byMobile.isNotEmpty())
        println("SC052: RepairEntry by mobile PASS")
    }
    @Test fun sc053_repairEntryByServiceMan() = runBlocking {
        val smId = serviceManDao.insert(ServiceMan(name="SM",mobile="1",email="e",employeeId="E",designation="T"))
        repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            serviceManId=smId,sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="Pending"))
        assertTrue(repairDao.getEntriesByServiceMan(smId).first().isNotEmpty())
        println("SC053: RepairEntry by serviceMan PASS")
    }
    @Test fun sc054_repairEntryDateRange() = runBlocking {
        seedEntry()
        val inRange = repairDao.getEntriesByDateRange(daysAgo(10),System.currentTimeMillis()).first()
        assertTrue(inRange.isNotEmpty())
        println("SC054: RepairEntry date range PASS")
    }
    @Test fun sc055_repairEntrySearch() = runBlocking {
        seedEntry()
        val results = repairDao.searchEntries("C1").first()
        assertTrue(results.isNotEmpty())
        println("SC055: RepairEntry search PASS")
    }
    @Test fun sc056_repairEntryFlow() = runBlocking {
        val id = seedEntry()
        assertNotNull(repairDao.getEntryByIdFlow(id).first())
        println("SC056: RepairEntry flow PASS")
    }
    @Test fun sc057_repairEntryRevenueQuery() = runBlocking {
        repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,workStatus="Done",
            finalAmount=5000.0,handoverDone=true,handoverDate=daysAgo(1,0)))
        val rev = repairDao.getRevenueInRange(daysAgo(10),System.currentTimeMillis()).first() ?: 0.0
        assertTrue(rev > 0)
        println("SC057: RepairEntry revenue query=$rev PASS")
    }

    // ========================================================================
    // SECTION 8: SparePartPurchase DAO — 8 scenarios (058-065)
    // ========================================================================
    @Test fun sc058_partPurchaseInsert() = runBlocking {
        val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="Display",purchasePrice=3200.0,supplierId="1",supplierName="S1",quantity=5))
        assertTrue(id > 0)
        println("SC058: SparePartPurchase insert PASS")
    }
    @Test fun sc059_partPurchaseUpdate() = runBlocking {
        val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="Display",purchasePrice=3200.0,supplierId="1",supplierName="S1",quantity=5))
        sparePartDao.update(SparePartPurchase(id=id,repairEntryId=1L,partName="Display",purchasePrice=3000.0,supplierId="1",supplierName="S1",quantity=3))
        val p = sparePartDao.getAllPurchases().first().first { it.id == id }
        assertEquals(3,p.quantity); assertEquals(3000.0,p.purchasePrice,0.01)
        println("SC059: SparePartPurchase update PASS")
    }
    @Test fun sc060_partPurchaseDelete() = runBlocking {
        val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId="1",supplierName="S1",quantity=1))
        sparePartDao.delete(sparePartDao.getAllPurchases().first().first { it.id == id })
        assertTrue(sparePartDao.getAllPurchases().first().none { it.id == id })
        println("SC060: SparePartPurchase delete PASS")
    }
    @Test fun sc061_partPurchaseByRepairId() = runBlocking {
        val eid = repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="Pending"))
        sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="P1",purchasePrice=100.0,supplierId="1",supplierName="S1",quantity=1))
        sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="P2",purchasePrice=200.0,supplierId="2",supplierName="S2",quantity=2))
        assertEquals(2,sparePartDao.getPurchasesByRepairId(eid).first().size)
        println("SC061: SparePartPurchase by repairId PASS")
    }
    @Test fun sc062_partPurchaseBySupplier() = runBlocking {
        sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId="SUP1",supplierName="S1",quantity=1))
        assertTrue(sparePartDao.getPurchasesBySupplier("SUP1").first().isNotEmpty())
        println("SC062: SparePartPurchase by supplier PASS")
    }
    @Test fun sc063_partPurchaseDateRange() = runBlocking {
        sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId="1",supplierName="S1",quantity=1,purchaseDate=daysAgo(5,11)))
        assertTrue(sparePartDao.getPurchasesByDateRange(daysAgo(10),System.currentTimeMillis()).first().isNotEmpty())
        println("SC063: SparePartPurchase date range PASS")
    }
    @Test fun sc064_partPurchaseTotalInRange() = runBlocking {
        sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId="1",supplierName="S1",quantity=3,purchaseDate=daysAgo(3,11)))
        val total = sparePartDao.getTotalPurchaseInRange(daysAgo(10),System.currentTimeMillis()).first() ?: 0.0
        assertTrue(total >= 300.0)
        println("SC064: SparePartPurchase total in range=$total (>=300) PASS")
    }
    @Test fun sc065_partPurchaseEdgeZeroQty() = runBlocking {
        val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="Zero",purchasePrice=100.0,supplierId="1",supplierName="S1",quantity=0))
        assertEquals(0,sparePartDao.getAllPurchases().first().first { it.id == id }.quantity)
        println("SC065: SparePartPurchase zero qty PASS")
    }

    // ========================================================================
    // SECTION 9: Sale DAO — 6 scenarios (066-071)
    // ========================================================================
    @Test fun sc066_saleInsert() = runBlocking {
        saleDao.insert(Sale(itemName="Screen",supplierId="1",supplierName="S1",purchasePrice=200.0,salePrice=400.0))
        assertEquals(1,saleDao.getAllSales().first().size)
        println("SC066: Sale insert PASS")
    }
    @Test fun sc067_saleBySupplier() = runBlocking {
        saleDao.insert(Sale(itemName="Item1",supplierId="SUP1",supplierName="S1",purchasePrice=100.0,salePrice=200.0))
        assertTrue(saleDao.getSalesBySupplier("SUP1").first().isNotEmpty())
        println("SC067: Sale by supplier PASS")
    }
    @Test fun sc068_saleDateRange() = runBlocking {
        saleDao.insert(Sale(itemName="Item1",supplierId="1",supplierName="S1",purchasePrice=100.0,salePrice=200.0,saleDate=daysAgo(3,12)))
        assertTrue(saleDao.getSalesByDateRange(daysAgo(10),System.currentTimeMillis()).first().isNotEmpty())
        println("SC068: Sale date range PASS")
    }
    @Test fun sc069_saleEdgeZeroPrices() = runBlocking {
        saleDao.insert(Sale(itemName="Free",supplierId="1",supplierName="S1",purchasePrice=0.0,salePrice=0.0))
        val s = saleDao.getAllSales().first().first { it.itemName == "Free" }
        assertEquals(0.0,s.purchasePrice,0.01); assertEquals(0.0,s.salePrice,0.01)
        println("SC069: Sale zero prices PASS")
    }
    @Test fun sc070_saleEdgeLargeAmounts() = runBlocking {
        saleDao.insert(Sale(itemName="Expensive",supplierId="1",supplierName="S1",purchasePrice=999999.99,salePrice=1999999.99))
        val s = saleDao.getAllSales().first().first { it.itemName == "Expensive" }
        assertEquals(999999.99,s.purchasePrice,0.01)
        println("SC070: Sale large amounts PASS")
    }
    @Test fun sc071_saleEdgeEmptyItemName() = runBlocking {
        saleDao.insert(Sale(itemName="",supplierId="1",supplierName="S1",purchasePrice=100.0,salePrice=200.0))
        assertTrue(saleDao.getAllSales().first().any { it.itemName.isEmpty() })
        println("SC071: Sale empty item name PASS")
    }

    // ========================================================================
    // SECTION 10: Payment DAO — 8 scenarios (072-079)
    // ========================================================================
    @Test fun sc072_paymentInsert() = runBlocking {
        val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1111111111",personName="C1",
            description="Repair",totalAmount=2000.0,paidAmount=1000.0,dueAmount=1000.0,status="PARTIAL"))
        assertTrue(id > 0)
        assertNotNull(paymentDao.getPaymentById(id))
        println("SC072: Payment insert PASS")
    }
    @Test fun sc073_paymentUpdate() = runBlocking {
        val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="R",totalAmount=2000.0,paidAmount=1000.0,dueAmount=1000.0,status="PARTIAL"))
        paymentDao.update(paymentDao.getPaymentById(id)!!.copy(paidAmount=2000.0,dueAmount=0.0,status="PAID"))
        val p = paymentDao.getPaymentById(id)!!
        assertEquals(2000.0,p.paidAmount,0.01); assertEquals("PAID",p.status)
        println("SC073: Payment update PASS")
    }
    @Test fun sc074_paymentDelete() = runBlocking {
        val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="R",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID"))
        paymentDao.delete(paymentDao.getPaymentById(id)!!)
        assertNull(paymentDao.getPaymentById(id))
        println("SC074: Payment delete PASS")
    }
    @Test fun sc075_paymentPendingDues() = runBlocking {
        paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="D",totalAmount=1000.0,paidAmount=0.0,dueAmount=1000.0,status="UNPAID"))
        paymentDao.insert(Payment(personType="CUSTOMER",personMobile="2",personName="C2",
            description="P",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID"))
        val dues = paymentDao.getPendingDues().first()
        assertTrue(dues.any { it.personMobile == "1" })
        assertFalse(dues.any { it.personMobile == "2" })
        println("SC075: Payment pending dues PASS")
    }
    @Test fun sc076_paymentByMobile() = runBlocking {
        paymentDao.insert(Payment(personType="CUSTOMER",personMobile="MOB1",personName="C",
            description="R",totalAmount=500.0,paidAmount=0.0,dueAmount=500.0,status="UNPAID"))
        assertTrue(paymentDao.getPaymentsByMobile("MOB1").first().isNotEmpty())
        println("SC076: Payment by mobile PASS")
    }
    @Test fun sc077_paymentByTypeAndDate() = runBlocking {
        paymentDao.insert(Payment(personType="SUPPLIER",personMobile="1",personName="S",
            description="P",totalAmount=1000.0,paidAmount=0.0,dueAmount=1000.0,status="UNPAID"))
        val byType = paymentDao.getPaymentsByTypeAndDate("SUPPLIER",daysAgo(10),System.currentTimeMillis()).first()
        assertTrue(byType.isNotEmpty())
        println("SC077: Payment by type+date PASS")
    }
    @Test fun sc078_paymentTotalDue() = runBlocking {
        paymentDao.insert(Payment(personType="SUPPLIER",personMobile="1",personName="S",
            description="P",totalAmount=5000.0,paidAmount=0.0,dueAmount=5000.0,status="UNPAID"))
        val totalDue = paymentDao.getTotalDueAmount().first()
        assertTrue(totalDue > 0)
        val supDue = paymentDao.getTotalDueByType("SUPPLIER").first()
        assertTrue(supDue > 0)
        println("SC078: Payment total due=$totalDue supDue=$supDue PASS")
    }
    @Test fun sc079_paymentEdgeNegativeAmount() = runBlocking {
        val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="Negative",totalAmount=-100.0,paidAmount=-100.0,dueAmount=0.0,status="REFUNDED"))
        assertTrue(id > 0)
        println("SC079: Payment negative amount PASS")
    }

    // ========================================================================
    // SECTION 11: PaymentTransaction DAO — 6 scenarios (080-085)
    // ========================================================================
    @Test fun sc080_txnInsert() = runBlocking {
        val pid = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="R",totalAmount=1000.0,paidAmount=500.0,dueAmount=500.0,status="PARTIAL"))
        val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=pid,personType="CUSTOMER",personMobile="1",personName="C",
            amount=500.0,paymentMode="CASH",note="Advance"))
        assertTrue(tid > 0)
        println("SC080: PaymentTransaction insert PASS")
    }
    @Test fun sc081_txnByPayment() = runBlocking {
        val pid = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="R",totalAmount=2000.0,paidAmount=2000.0,dueAmount=0.0,status="PAID"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=pid,personType="CUSTOMER",personMobile="1",personName="C",
            amount=2000.0,paymentMode="ONLINE"))
        assertEquals(1,paymentTxnDao.getTransactionsByPayment(pid).first().size)
        println("SC081: PaymentTransaction by payment PASS")
    }
    @Test fun sc082_txnByMobile() = runBlocking {
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="CUSTOMER",personMobile="MOB1",personName="C",
            amount=500.0,paymentMode="CASH"))
        assertTrue(paymentTxnDao.getTransactionsByMobile("MOB1").first().isNotEmpty())
        println("SC082: PaymentTransaction by mobile PASS")
    }
    @Test fun sc083_txnDateRange() = runBlocking {
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="CUSTOMER",personMobile="1",personName="C",
            amount=100.0,paymentMode="CASH",transactionDate=daysAgo(3,12)))
        assertTrue(paymentTxnDao.getTransactionsByDateRange(daysAgo(10),System.currentTimeMillis()).first().isNotEmpty())
        println("SC083: PaymentTransaction date range PASS")
    }
    @Test fun sc084_txnTotalPaidByMobile() = runBlocking {
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="CUSTOMER",personMobile="MOBSUM",personName="C",
            amount=500.0,paymentMode="CASH"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="CUSTOMER",personMobile="MOBSUM",personName="C",
            amount=300.0,paymentMode="ONLINE"))
        val total = paymentTxnDao.getTotalPaidByMobile("MOBSUM").first()
        assertEquals(800.0,total,0.01)
        println("SC084: PaymentTransaction total by mobile=$total PASS")
    }
    @Test fun sc085_txnUpdateDelete() = runBlocking {
        val pid = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID"))
        val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=pid,personType="CUSTOMER",personMobile="1",personName="C",
            amount=500.0,paymentMode="CASH"))
        paymentTxnDao.update(PaymentTransaction(id=tid,paymentId=pid,personType="CUSTOMER",personMobile="1",personName="C",
            amount=600.0,paymentMode="ONLINE"))
        assertEquals(600.0,paymentTxnDao.getTransactionsByPayment(pid).first().first().amount,0.01)
        paymentTxnDao.delete(paymentTxnDao.getAllTransactions().first().first { it.id == tid })
        assertFalse(paymentTxnDao.getAllTransactions().first().any { it.id == tid })
        println("SC085: PaymentTransaction update+delete PASS")
    }

    // ========================================================================
    // SECTION 12: PartReturn DAO — 5 scenarios (086-090)
    // ========================================================================
    @Test fun sc086_partReturnInsert() = runBlocking {
        val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="Display",
            returnReason="Defective",refundAmount=3200.0))
        assertTrue(id > 0)
        assertNotNull(partReturnDao.getReturnById(id))
        println("SC086: PartReturn insert PASS")
    }
    @Test fun sc087_partReturnUpdate() = runBlocking {
        val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="Display",
            returnReason="Defective",refundAmount=3200.0))
        partReturnDao.update(partReturnDao.getReturnById(id)!!.copy(refundReceived=true,refundAmount=3000.0))
        val r = partReturnDao.getReturnById(id)!!
        assertTrue(r.refundReceived); assertEquals(3000.0,r.refundAmount,0.01)
        println("SC087: PartReturn update PASS")
    }
    @Test fun sc088_partReturnDelete() = runBlocking {
        val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="P",returnReason="R",refundAmount=100.0))
        partReturnDao.delete(partReturnDao.getReturnById(id)!!)
        assertNull(partReturnDao.getReturnById(id))
        println("SC088: PartReturn delete PASS")
    }
    @Test fun sc089_partReturnBySupplier() = runBlocking {
        partReturnDao.insert(PartReturn(supplierId="SUPR1",supplierName="S1",partName="P",returnReason="R",refundAmount=100.0))
        assertTrue(partReturnDao.getReturnsBySupplier("SUPR1").first().isNotEmpty())
        println("SC089: PartReturn by supplier PASS")
    }
    @Test fun sc090_partReturnEdgeZero() = runBlocking {
        val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="Free",returnReason="Free",refundAmount=0.0))
        assertEquals(0.0,partReturnDao.getReturnById(id)!!.refundAmount,0.01)
        println("SC090: PartReturn zero refund PASS")
    }

    // ========================================================================
    // SECTION 13: Cross-Entity Business Workflows — 10 scenarios (091-100)
    // ========================================================================
    @Test fun sc091_fullRepairWorkflow() = runBlocking {
        val smId = serviceManDao.insert(ServiceMan(name="Raj",mobile="1",email="e",employeeId="E",designation="T"))
        val cusId = "1111111111"
        customerDao.insert(Customer(mobileNumber=cusId,name="Rahul",city="Delhi"))
        val eid = repairDao.insert(RepairEntry(customerMobile=cusId,customerName="Rahul",deviceBrand="Samsung",deviceModel="S21",
            faultDetected="Screen broken",sparePartName="Display",sparePartPurchasePrice=3200.0,serviceManId=smId,
            supplierId=0L,chargeAmount=5500.0,advanceAmount=1000.0,workStatus="In Progress",quotationDone=true))
        val pid = sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="Display",purchasePrice=3200.0,
            supplierId="9398123456",supplierName="Rajesh",quantity=1))
        val payId = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=cusId,personName="Rahul",
            description="Advance",totalAmount=5500.0,paidAmount=1000.0,dueAmount=4500.0,status="PARTIAL",linkedEntryId=eid))
        paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="CUSTOMER",personMobile=cusId,personName="Rahul",
            amount=1000.0,paymentMode="CASH"))
        repairDao.update(repairDao.getEntryById(eid)!!.copy(
            finalAmount=5500.0,handoverDone=true,handoverDate=daysAgo(1,0),workStatus="Done",workDone=true))
        paymentDao.update(paymentDao.getPaymentById(payId)!!.copy(paidAmount=5500.0,dueAmount=0.0,status="PAID"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="CUSTOMER",personMobile=cusId,personName="Rahul",
            amount=4500.0,paymentMode="CASH"))
        val finalEntry = repairDao.getEntryById(eid)!!
        assertTrue(finalEntry.handoverDone)
        assertEquals("Done",finalEntry.workStatus)
        val finalPay = paymentDao.getPaymentById(payId)!!
        assertEquals("PAID",finalPay.status)
        assertEquals(0.0,finalPay.dueAmount,0.01)
        println("SC091: Full repair workflow (Entry→Inspection→Quotation→Parts→Handover) PASS")
    }
    @Test fun sc092_supplierPurchaseAndReturn() = runBlocking {
        supplierDao.insert(Supplier(mobile="9398123456",name="Rajesh",companyName="Rajesh Mobile Parts",city="Mumbai"))
        val pid = sparePartDao.insert(SparePartPurchase(repairEntryId=0L,partName="Battery",purchasePrice=350.0,
            supplierId="9398123456",supplierName="Rajesh",quantity=10))
        val rid = partReturnDao.insert(PartReturn(supplierId="9398123456",supplierName="Rajesh",partName="Battery",
            returnReason="Defective batch",refundAmount=3500.0))
        sparePartDao.update(sparePartDao.getAllPurchases().first().first { it.id == pid }.copy(quantity=5))
        assertEquals(5,sparePartDao.getAllPurchases().first().first { it.id == pid }.quantity)
        assertEquals(3500.0,partReturnDao.getReturnById(rid)!!.refundAmount,0.01)
        println("SC092: Supplier purchase+return PASS")
    }
    @Test fun sc093_dealerWorkflowWithPayment() = runBlocking {
        dealerDao.insert(Dealer(mobileNumber="2222222222",name="Dealer A",city="Mumbai"))
        val eid = repairDao.insert(RepairEntry(dealerMobile="2222222222",dealerName="Dealer A",customerMobile="",
            customerName="",deviceBrand="Apple",deviceModel="iPhone 14",faultDetected="Battery drain",
            sparePartName="Battery",sparePartPurchasePrice=800.0,supplierId=0L,chargeAmount=2500.0,
            advanceAmount=1000.0,workStatus="In Progress"))
        val payId = paymentDao.insert(Payment(personType="DEALER",personMobile="2222222222",personName="Dealer A",
            description="Repair advance",totalAmount=2500.0,paidAmount=1000.0,dueAmount=1500.0,status="PARTIAL",linkedEntryId=eid))
        paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="DEALER",personMobile="2222222222",
            personName="Dealer A",amount=1000.0,paymentMode="CASH"))
        assertEquals(1000.0,paymentTxnDao.getTotalPaidByMobile("2222222222").first(),0.01)
        println("SC093: Dealer workflow with payment PASS")
    }
    @Test fun sc094_directSaleWithSupplierPayment() = runBlocking {
        supplierDao.insert(Supplier(mobile="9398123456",name="Rajesh",companyName="Rajesh Mobile Parts",city="Mumbai"))
        saleDao.insert(Sale(itemName="Charger",supplierId="9398123456",supplierName="Rajesh",purchasePrice=200.0,salePrice=400.0))
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="CUSTOMER",personMobile="DIRECT_SALE",
            personName="Cash Customer",amount=400.0,paymentMode="CASH"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="SUPPLIER",personMobile="9398123456",
            personName="Rajesh",amount=200.0,paymentMode="CASH"))
        assertTrue(saleDao.getAllSales().first().any { it.itemName == "Charger" })
        println("SC094: Direct sale with supplier payment PASS")
    }
    @Test fun sc095_supplierDueManagement() = runBlocking {
        supplierDao.insert(Supplier(mobile="1",name="S1",companyName="SC1",city="C1"))
        val payId = paymentDao.insert(Payment(personType="SUPPLIER",personMobile="1",personName="S1",
            description="Purchase",totalAmount=10000.0,paidAmount=0.0,dueAmount=10000.0,status="UNPAID"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="SUPPLIER",personMobile="1",personName="S1",
            amount=4000.0,paymentMode="CASH"))
        paymentDao.update(paymentDao.getPaymentById(payId)!!.copy(paidAmount=4000.0,dueAmount=6000.0,status="PARTIAL"))
        val p = paymentDao.getPaymentById(payId)!!
        assertEquals(4000.0,p.paidAmount,0.01); assertEquals(6000.0,p.dueAmount,0.01)
        println("SC095: Supplier due management PASS")
    }
    @Test fun sc096_multiPaymentForSingleRepair() = runBlocking {
        val eid = repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,chargeAmount=5000.0,workStatus="Pending"))
        val pay1 = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="Advance",totalAmount=5000.0,paidAmount=2000.0,dueAmount=3000.0,status="PARTIAL",linkedEntryId=eid))
        paymentTxnDao.insert(PaymentTransaction(paymentId=pay1,personType="CUSTOMER",personMobile="1",personName="C",
            amount=2000.0,paymentMode="CASH"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=pay1,personType="CUSTOMER",personMobile="1",personName="C",
            amount=3000.0,paymentMode="ONLINE"))
        paymentDao.update(paymentDao.getPaymentById(pay1)!!.copy(paidAmount=5000.0,dueAmount=0.0,status="PAID"))
        val txns = paymentTxnDao.getTransactionsByPayment(pay1).first()
        assertEquals(2,txns.size)
        assertEquals(5000.0,txns.sumOf { it.amount },0.01)
        println("SC096: Multi-payment for single repair PASS")
    }
    @Test fun sc097_repairCancellationWithRefund() = runBlocking {
        val eid = repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,chargeAmount=3000.0,advanceAmount=1000.0,
            workStatus="Pending"))
        val payId = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="Advance",totalAmount=3000.0,paidAmount=1000.0,dueAmount=2000.0,status="PARTIAL",linkedEntryId=eid))
        repairDao.update(repairDao.getEntryById(eid)!!.copy(workStatus="Cancelled",isDraft=true))
        paymentDao.update(paymentDao.getPaymentById(payId)!!.copy(paidAmount=0.0,dueAmount=0.0,status="CANCELLED"))
        paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="CUSTOMER_REFUND",personMobile="1",personName="C",
            amount=-1000.0,paymentMode="CASH",note="Refund for cancellation"))
        val cancelEntry = repairDao.getEntryById(eid)!!
        assertEquals("Cancelled",cancelEntry.workStatus)
        assertTrue(cancelEntry.isDraft)
        println("SC097: Repair cancellation with refund PASS")
    }
    @Test fun sc098_duplicateMobileHandling() = runBlocking {
        customerDao.insert(Customer(mobileNumber="1111111111",name="Original",city="Delhi"))
        customerDao.insert(Customer(mobileNumber="1111111111",name="Override",city="Mumbai"))
        val c = customerDao.getCustomerByMobile("1111111111")!!
        assertEquals("Override",c.name)
        assertEquals("Mumbai",c.city)
        dealerDao.insert(Dealer(mobileNumber="2222222222",name="D1",city="C1"))
        dealerDao.insert(Dealer(mobileNumber="2222222222",name="D2",city="C2"))
        assertEquals("D2",dealerDao.getDealerByMobile("2222222222")!!.name)
        println("SC098: Duplicate mobile (REPLACE) PASS")
    }
    @Test fun sc099_dataIntegrityCrossCheck() = runBlocking {
        val smId = serviceManDao.insert(ServiceMan(name="Raj",mobile="1",email="e",employeeId="E",designation="T"))
        supplierDao.insert(Supplier(mobile="1",name="S1",companyName="SC",city="C"))
        val eid = repairDao.insert(RepairEntry(customerMobile="1",customerName="C",deviceBrand="A",deviceModel="B",
            serviceManId=smId,sparePartName="Display",sparePartPurchasePrice=3200.0,supplierId=0L,chargeAmount=5500.0,workStatus="Pending"))
        val ppid = sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="Display",purchasePrice=3200.0,
            supplierId="1",supplierName="S1",quantity=1))
        val payId = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",
            description="Repair",totalAmount=5500.0,paidAmount=1000.0,dueAmount=4500.0,status="PARTIAL",linkedEntryId=eid))
        assertTrue(repairDao.getEntriesByServiceMan(smId).first().any { it.id == eid })
        assertTrue(repairDao.getEntriesByMobile("1").first().any { it.id == eid })
        assertTrue(sparePartDao.getPurchasesByRepairId(eid).first().any { it.id == ppid })
        assertTrue(paymentDao.getPaymentsByMobile("1").first().any { it.id == payId })
        assertTrue(paymentDao.getPaymentsByTypeAndDate("CUSTOMER",daysAgo(10),System.currentTimeMillis()).first().any { it.id == payId })
        println("SC099: Cross-table data integrity PASS")
    }
    @Test fun sc100_comprehensiveStressTest() = runBlocking {
        println("\nSC100: COMPREHENSIVE STRESS TEST — 100 operations")
        val rng = Random(42)
        val start = System.currentTimeMillis()
        val results = mutableListOf<String>()

        // 20 customers
        repeat(20) { i -> customerDao.insert(Customer(mobileNumber=TestFixtures.randomMobile(i),name=TestFixtures.randomPersonName(),city=listOf("Mumbai","Delhi","Bangalore","Pune","Chennai").random())) }
        results.add("${customerDao.getAllCustomers().first().size} customers")

        // 10 dealers
        repeat(10) { i -> dealerDao.insert(Dealer(mobileNumber=TestFixtures.randomMobile(100+i),name=TestFixtures.randomPersonName(),city=listOf("Mumbai","Delhi","Bangalore").random())) }
        results.add("${dealerDao.getAllDealers().first().size} dealers")

        // 20 repair entries
        repeat(20) { i ->
            val mob = TestFixtures.randomMobile(i)
            val part = TestFixtures.PARTS_CATALOG.random()
            val charge = part.retailPrice + 500 + rng.nextInt(2000)
            repairDao.insert(RepairEntry(customerMobile=mob,customerName=TestFixtures.randomPersonName(),
                deviceBrand=TestFixtures.randomBrand(),deviceModel=TestFixtures.randomPhoneModel(),
                faultDetected=part.name,sparePartName=part.name,sparePartPurchasePrice=part.costPrice,
                supplierId=0L,chargeAmount=charge,advanceAmount= round(charge*0.3*100)/100.0,
                workStatus=if (rng.nextBoolean()) "Done" else "Pending", finalAmount=charge,
                handoverDone=rng.nextBoolean(), handoverDate=if (rng.nextBoolean()) daysAgo(rng.nextInt(5),0) else 0L,
                entryDate=daysAgo(rng.nextInt(15))))
        }
        results.add("${repairDao.getAllEntries().first().size} repairs")

        // 30 spare parts
        repeat(30) {
            val part = TestFixtures.PARTS_CATALOG.random()
            sparePartDao.insert(SparePartPurchase(repairEntryId= (1+ rng.nextInt(20)).toLong(),
                partName=part.name,purchasePrice=part.costPrice,supplierId=TestFixtures.SUPPLIER_DATA.random().mobile,
                supplierName=TestFixtures.SUPPLIER_DATA.random().name,quantity=1+ rng.nextInt(10)))
        }
        results.add("${sparePartDao.getAllPurchases().first().size} spare parts")

        // 20 payments with transactions
        repeat(20) {
            val amt = round((1000.0 + rng.nextDouble()*9000.0)*100.0)/100.0
            val paid = round(amt*(0.3+rng.nextDouble()*0.7)*100.0)/100.0
            val payId = paymentDao.insert(Payment(personType=listOf("CUSTOMER","DEALER","SUPPLIER").random(),
                personMobile=TestFixtures.randomMobile(rng.nextInt(130)), personName=TestFixtures.randomPersonName(),
                description="Stress test",totalAmount=amt,paidAmount=paid,dueAmount= round((amt-paid)*100.0)/100.0,
                status=if (paid>=amt) "PAID" else if (paid>0) "PARTIAL" else "UNPAID"))
            if (paid > 0) paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="CUSTOMER",
                personMobile=TestFixtures.randomMobile(rng.nextInt(130)),personName=TestFixtures.randomPersonName(),
                amount=paid,paymentMode=if (rng.nextBoolean()) "CASH" else "ONLINE"))
        }
        results.add("${paymentDao.getAllPayments().first().size} payments")
        results.add("${paymentTxnDao.getAllTransactions().first().size} transactions")

        // 10 sales
        repeat(10) {
            val part = TestFixtures.PARTS_CATALOG.random()
            saleDao.insert(Sale(itemName="${part.name} x${1+rng.nextInt(3)}",supplierId=TestFixtures.SUPPLIER_DATA.random().mobile,
                supplierName=TestFixtures.SUPPLIER_DATA.random().name,purchasePrice=part.costPrice,salePrice=part.retailPrice))
        }
        results.add("${saleDao.getAllSales().first().size} sales")

        // 5 returns
        repeat(5) {
            partReturnDao.insert(PartReturn(supplierId=TestFixtures.SUPPLIER_DATA.random().mobile,
                supplierName=TestFixtures.SUPPLIER_DATA.random().name,partName=TestFixtures.PARTS_CATALOG.random().name,
                returnReason= listOf("Defective","Wrong Item","Not Needed").random(),refundAmount= round((100.0+rng.nextDouble()*1000.0)*100.0)/100.0))
        }
        results.add("${partReturnDao.getAllReturns().first().size} returns")

        // Verify queries work with all this data
        assertTrue(repairDao.getPendingCount().first() >= 0)
        assertTrue(repairDao.getCompletedCountInRange(daysAgo(30),System.currentTimeMillis()).first() >= 0)
        assertTrue((repairDao.getRevenueInRange(daysAgo(30),System.currentTimeMillis()).first()?:0.0) >= 0)
        assertTrue(paymentDao.getTotalDueAmount().first() >= 0)
        assertTrue(paymentDao.getPendingDues().first().isNotEmpty())

        val elapsed = System.currentTimeMillis() - start
        println("  Stress test: ${results.joinToString(", ")}")
        println("  All 100 operations completed in ${elapsed}ms")
        println("SC100: Comprehensive stress test PASS")
    }

    // ========================================================================
    // SC101: Final Orchestration — runs all 100 scenarios
    // ========================================================================
    @Test fun scenario101_runAll100() = runBlocking {
        println("\n========== RUNNING ALL 100 SCENARIOS ==========")
        val start = System.currentTimeMillis()
        val results = mutableListOf<Pair<String,Boolean>>()
        val tests = listOf(
            "SC001" to { sc001_userProfileInsertAndRead() },
            "SC002" to { sc002_userProfileUpdate() },
            "SC003" to { sc003_userProfileFlow() },
            "SC004" to { sc004_userProfileDelete() },
            "SC005" to { sc005_userProfileEdgeEmpty() },
            "SC006" to { sc006_serviceManInsert() },
            "SC007" to { sc007_serviceManUpdate() },
            "SC008" to { sc008_serviceManDelete() },
            "SC009" to { sc009_serviceManGetAll() },
            "SC010" to { sc010_serviceManActiveFilter() },
            "SC011" to { sc011_serviceManFlow() },
            "SC012" to { sc012_serviceManEdgeEmptyEmail() },
            "SC013" to { sc013_serviceManEdgeLongName() },
            "SC014" to { sc014_commonFaultInsert() },
            "SC015" to { sc015_commonFaultUpdate() },
            "SC016" to { sc016_commonFaultDelete() },
            "SC017" to { sc017_commonFaultGetAll() },
            "SC018" to { sc018_commonFaultActiveFilter() },
            "SC019" to { sc019_commonFaultByCategory() },
            "SC020" to { sc020_commonFaultEdgeZeroCharge() },
            "SC021" to { sc021_commonFaultEdgeNegativeSortOrder() },
            "SC022" to { sc022_supplierInsert() },
            "SC023" to { sc023_supplierUpdate() },
            "SC024" to { sc024_supplierDelete() },
            "SC025" to { sc025_supplierGetAll() },
            "SC026" to { sc026_supplierActiveFilter() },
            "SC027" to { sc027_supplierFlow() },
            "SC028" to { sc028_supplierEdgeEmptyEmail() },
            "SC029" to { sc029_supplierEdgeMaxFields() },
            "SC030" to { sc030_customerInsert() },
            "SC031" to { sc031_customerUpdate() },
            "SC032" to { sc032_customerDelete() },
            "SC033" to { sc033_customerGetAll() },
            "SC034" to { sc034_customerFlow() },
            "SC035" to { sc035_customerReplaceOnConflict() },
            "SC036" to { sc036_customerEdgeNullName() },
            "SC037" to { sc037_customerEdgeLongName() },
            "SC038" to { sc038_dealerInsert() },
            "SC039" to { sc039_dealerUpdate() },
            "SC040" to { sc040_dealerGetAll() },
            "SC041" to { sc041_dealerFlow() },
            "SC042" to { sc042_dealerReplaceOnConflict() },
            "SC043" to { sc043_dealerEdgeNullName() },
            "SC044" to { sc044_dealerEdgeEmptyName() },
            "SC045" to { sc045_dealerEdgeLongName() },
            "SC046" to { sc046_repairEntryInsert() },
            "SC047" to { sc047_repairEntryUpdate() },
            "SC048" to { sc048_repairEntryDelete() },
            "SC049" to { sc049_repairEntryGetAll() },
            "SC050" to { sc050_repairEntryPendingFilter() },
            "SC051" to { sc051_repairEntryCompletedFilter() },
            "SC052" to { sc052_repairEntryByMobile() },
            "SC053" to { sc053_repairEntryByServiceMan() },
            "SC054" to { sc054_repairEntryDateRange() },
            "SC055" to { sc055_repairEntrySearch() },
            "SC056" to { sc056_repairEntryFlow() },
            "SC057" to { sc057_repairEntryRevenueQuery() },
            "SC058" to { sc058_partPurchaseInsert() },
            "SC059" to { sc059_partPurchaseUpdate() },
            "SC060" to { sc060_partPurchaseDelete() },
            "SC061" to { sc061_partPurchaseByRepairId() },
            "SC062" to { sc062_partPurchaseBySupplier() },
            "SC063" to { sc063_partPurchaseDateRange() },
            "SC064" to { sc064_partPurchaseTotalInRange() },
            "SC065" to { sc065_partPurchaseEdgeZeroQty() },
            "SC066" to { sc066_saleInsert() },
            "SC067" to { sc067_saleBySupplier() },
            "SC068" to { sc068_saleDateRange() },
            "SC069" to { sc069_saleEdgeZeroPrices() },
            "SC070" to { sc070_saleEdgeLargeAmounts() },
            "SC071" to { sc071_saleEdgeEmptyItemName() },
            "SC072" to { sc072_paymentInsert() },
            "SC073" to { sc073_paymentUpdate() },
            "SC074" to { sc074_paymentDelete() },
            "SC075" to { sc075_paymentPendingDues() },
            "SC076" to { sc076_paymentByMobile() },
            "SC077" to { sc077_paymentByTypeAndDate() },
            "SC078" to { sc078_paymentTotalDue() },
            "SC079" to { sc079_paymentEdgeNegativeAmount() },
            "SC080" to { sc080_txnInsert() },
            "SC081" to { sc081_txnByPayment() },
            "SC082" to { sc082_txnByMobile() },
            "SC083" to { sc083_txnDateRange() },
            "SC084" to { sc084_txnTotalPaidByMobile() },
            "SC085" to { sc085_txnUpdateDelete() },
            "SC086" to { sc086_partReturnInsert() },
            "SC087" to { sc087_partReturnUpdate() },
            "SC088" to { sc088_partReturnDelete() },
            "SC089" to { sc089_partReturnBySupplier() },
            "SC090" to { sc090_partReturnEdgeZero() },
            "SC091" to { sc091_fullRepairWorkflow() },
            "SC092" to { sc092_supplierPurchaseAndReturn() },
            "SC093" to { sc093_dealerWorkflowWithPayment() },
            "SC094" to { sc094_directSaleWithSupplierPayment() },
            "SC095" to { sc095_supplierDueManagement() },
            "SC096" to { sc096_multiPaymentForSingleRepair() },
            "SC097" to { sc097_repairCancellationWithRefund() },
            "SC098" to { sc098_duplicateMobileHandling() },
            "SC099" to { sc099_dataIntegrityCrossCheck() },
            "SC100" to { sc100_comprehensiveStressTest() }
        )
        var passed = 0; var failed = 0
        tests.forEach { (name, test) ->
            try { test(); results.add(name to true); passed++ }
            catch (e: Throwable) { results.add(name to false); failed++; System.err.println("  $name FAILED: ${e.message}"); System.err.println("    ${e.stackTrace.take(3).joinToString("\n    ") { it.toString() }}") }
        }
        val elapsed = System.currentTimeMillis() - start
        println("\n========== RESULTS: $passed PASSED / $failed FAILED in ${elapsed}ms ==========")
        results.forEach { (n, ok) -> println("  ${if (ok) "PASS" else "FAIL"}  $n") }
        if (failed > 0) fail("$failed scenario(s) failed")
    }
}
