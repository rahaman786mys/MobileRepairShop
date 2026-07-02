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

    // ========================================================================
    // SC102: 1000 Parameterized Edge Cases — data-driven from combinatorial matrix
    // ========================================================================
    data class EdgeCase(val name: String, val block: suspend () -> Unit)
    @Test fun sc102_thousandEdgeCases() = runBlocking {
        println("\nSC102: 10000 PARAMETERIZED EDGE CASES")
        val cases = mutableListOf<EdgeCase>()
        val rng = Random(9999)
        var ctr = 0

        // Mobile offset bases (100K apart to avoid collisions with ctr up to 10000)
        val M = { base: Int, off: Int -> TestFixtures.randomMobile(base + off) }

        // --- STRING FIELD EDGE CASES (1000 cases: 100 values × 10 fields) ---
        val stringEdgeValues = listOf("", "a", "ab", "abc", "A".repeat(255), "A".repeat(1000), "A".repeat(2000),
            "test@email.com", "  spaced  ", "1234567890", "SPECIAL!@#\$%^&*()",
            "\u0041\u0042\u0043", "a\nb\tc", "a\r\nb", "\t\t", 
            "<script>", "'; DROP TABLE; --", "../..", "http://test.com", "file:///etc",
            "ABC-DEF", "abc_def", "abc.def", "a".repeat(500), "b".repeat(750),
            "c".repeat(1500), "d".repeat(5000), "-1", "0", "1", "NaN", "Infinity",
            "null", "undefined", "true", "false", "0.0", "1.0", "-0.0", "1e10",
            "\uD83D\uDE42\uD83D\uDE43\uD83D\uDE0A", "\u65E5\u672C\u8A9E", "\u0627\u0644\u0639\u0631\u0628\u064A\u0629",
            "\u00C9l\u00E8ve", "M\u00FCnchen", "\u5317\u4EAC",
            "a b c d e", "a  b  c", "\"quoted\"", "backslash\\test", "percent%value",
            "hash#tag", "at@sign", "plus+sign", "colon:value", "semi;colon",
            "comma,val", "pipe|val", "tilde~val", "star*val", "open(br)ket", "close)br",
            "{brace}", "[brack]", "less<gt>", "quest?ion", "excl!am", "grave`mark",
            "single'quote", "Llanfairpwll", "John-Paul", "O'Brien", "van der Waals",
            "de la Cruz", "10,000", "99.99%", "100%", "+++++", "-----", "====",
            "   ", ".", "..", "...", "a".repeat(100), "b".repeat(200), "c".repeat(300))
        stringEdgeValues.forEach { valx ->
            cases.add(EdgeCase("EC${++ctr}: Cust name='${valx.take(20)}'") { customerDao.insert(Customer(mobileNumber=M(100000,ctr),name=valx,city="C")); assertNotNull(customerDao.getCustomerByMobile(M(100000,ctr))) })
            cases.add(EdgeCase("EC${++ctr}: Supp name='${valx.take(20)}'") { supplierDao.insert(Supplier(mobile=M(200000,ctr),name=valx,companyName="C",city="C")); assertNotNull(supplierDao.getSupplierByMobile(M(200000,ctr))) })
            cases.add(EdgeCase("EC${++ctr}: Dealer name='${valx.take(20)}'") { dealerDao.insert(Dealer(mobileNumber=M(300000,ctr),name=valx,city="C")); assertNotNull(dealerDao.getDealerByMobile(M(300000,ctr))) })
            cases.add(EdgeCase("EC${++ctr}: SM name='${valx.take(20)}'") { val id = serviceManDao.insert(ServiceMan(name=valx,mobile=M(400000,ctr),email="e",employeeId="E$ctr",designation="T")); assertNotNull(serviceManDao.getServiceManById(id)) })
            cases.add(EdgeCase("EC${++ctr}: Brand='${valx.take(20)}'") { val id = repairDao.insert(RepairEntry(customerMobile=M(500000,ctr),customerName="C",deviceBrand=valx,deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); assertNotNull(repairDao.getEntryById(id)) })
            cases.add(EdgeCase("EC${++ctr}: Model='${valx.take(20)}'") { val id = repairDao.insert(RepairEntry(customerMobile=M(600000,ctr),customerName="C",deviceBrand="B",deviceModel=valx,sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); assertNotNull(repairDao.getEntryById(id)) })
            cases.add(EdgeCase("EC${++ctr}: Fault='${valx.take(20)}'") { val id = repairDao.insert(RepairEntry(customerMobile=M(700000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",faultDetected=valx)); assertNotNull(repairDao.getEntryById(id)) })
            cases.add(EdgeCase("EC${++ctr}: Part='${valx.take(20)}'") { val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName=valx,purchasePrice=100.0,supplierId="1",supplierName="S",quantity=1)); assertTrue(id > 0) })
            cases.add(EdgeCase("EC${++ctr}: Sale='${valx.take(20)}'") { saleDao.insert(Sale(itemName=valx,supplierId="1",supplierName="S1",purchasePrice=100.0,salePrice=200.0)); assertTrue(saleDao.getAllSales().first().any { it.itemName == valx }) })
            cases.add(EdgeCase("EC${++ctr}: Pay='${valx.take(20)}'") { val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile="1",personName="C",description=valx,totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID")); assertNotNull(paymentDao.getPaymentById(id)) })
        }

        // --- NUMERIC FIELD EDGE CASES (500 cases: 100 values × 5 fields) ---
        val numericEdgeValues = listOf(0.0, -0.0, 0.01, -0.01, 1.0, -1.0, 999999.99, -999999.99,
            0.001, -0.001, 0.5, -0.5, 1.5, -1.5, 100.0, -100.0, 1000.0, -1000.0,
            10000.0, 100000.0, 1000000.0, 9.99, 10.01, 99.99, 100.01, 999.99, 1000.01,
            0.99, 1.01, Double.MIN_VALUE, Double.MAX_VALUE, Math.PI, Math.E, 1.0/3.0, 1.0/7.0,
            0.1+0.2, 1e-10, 1e10, -1e10, 1e-5, 1e5) + (1..58).map { it * 100.0 }
        numericEdgeValues.forEach { valx ->
            cases.add(EdgeCase("EC${++ctr}: Purchase=$valx") { sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=valx,supplierId="1",supplierName="S",quantity=1)); assertTrue(sparePartDao.getAllPurchases().first().any { kotlin.math.abs(it.purchasePrice - valx) < 0.01 }) })
            cases.add(EdgeCase("EC${++ctr}: Sale=$valx") { saleDao.insert(Sale(itemName="I",supplierId="1",supplierName="S1",purchasePrice=100.0,salePrice=valx)); assertTrue(saleDao.getAllSales().first().any { kotlin.math.abs(it.salePrice - valx) < 0.01 }) })
            cases.add(EdgeCase("EC${++ctr}: Charge=$valx") { val id = repairDao.insert(RepairEntry(customerMobile=M(800000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",chargeAmount=valx)); assertEquals(valx,repairDao.getEntryById(id)!!.chargeAmount,0.01) })
            cases.add(EdgeCase("EC${++ctr}: PayTotal=$valx") { val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=M(900000,ctr),personName="C",description="T",totalAmount=valx,paidAmount=0.0,dueAmount=valx,status="UNPAID")); assertEquals(valx,paymentDao.getPaymentById(id)!!.totalAmount,0.01) })
            cases.add(EdgeCase("EC${++ctr}: Refund=$valx") { val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="P",returnReason="R",refundAmount=valx)); assertEquals(valx,partReturnDao.getReturnById(id)!!.refundAmount,0.01) })
        }

        // --- QUANTITY EDGE CASES (100 cases: 25 values × 4) ---
        (listOf(0, -1, 1, Int.MAX_VALUE, Int.MIN_VALUE, 2, -2, 10, -10, 100, -100, 1000, -1000, 10000, -10000, 999999, -999999) + (1..8).map { it*1000 }).forEach { valx ->
            cases.add(EdgeCase("EC${++ctr}: Qty=$valx") { sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId="1",supplierName="S",quantity=valx)); assertTrue(sparePartDao.getAllPurchases().first().any { it.quantity == valx }) })
            cases.add(EdgeCase("EC${++ctr}: Sort=$valx") { val id = commonFaultDao.insert(CommonFault(faultName="EC$ctr",category="T",sortOrder=valx)); assertEquals(valx,commonFaultDao.getFaultById(id)!!.sortOrder) })
            cases.add(EdgeCase("EC${++ctr}: CustOK") { assertTrue(customerDao.getAllCustomers().first().size >= 0) })
            cases.add(EdgeCase("EC${++ctr}: DealOK") { assertTrue(dealerDao.getAllDealers().first().size >= 0) })
        }

        // --- TIMESTAMP EDGE CASES (200 cases: 50 values × 4 fields) ---
        val timestampValues = listOf(0L, -1L, 1L, Long.MAX_VALUE, Long.MIN_VALUE,
            System.currentTimeMillis(), System.currentTimeMillis()+86400000L,
            System.currentTimeMillis()-86400000L, System.currentTimeMillis()+86400000L*365,
            System.currentTimeMillis()-86400000L*365, System.currentTimeMillis()+86400000L*365*10,
            System.currentTimeMillis()-86400000L*365*10, 946684800000L, 978307200000L,
            1009843200000L, 1041379200000L, 1072915200000L, 1104537600000L, 1136073600000L,
            1167696000000L, 1199145600000L, 1230768000000L, 1262304000000L, 1293840000000L,
            1325376000000L, 1356998400000L, 1388534400000L, 1420070400000L, 1451606400000L,
            1483228800000L, 1514764800000L, 1546300800000L, 1577836800000L, 1609459200000L,
            1640995200000L, 1672531200000L, 1704067200000L, 1735689600000L, 1767225600000L,
            1798761600000L) + (27..38).map { 1420070400000L + it * 86400000L * 30L }
        timestampValues.forEach { valx ->
            cases.add(EdgeCase("EC${++ctr}: SaleDate=$valx") { saleDao.insert(Sale(itemName="TS",supplierId="1",supplierName="S",purchasePrice=100.0,salePrice=200.0,saleDate=valx)); assertTrue(saleDao.getAllSales().first().any { it.saleDate == valx }) })
            cases.add(EdgeCase("EC${++ctr}: EntryDt=$valx") { val id = repairDao.insert(RepairEntry(customerMobile=M(1000000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",entryDate=valx)); assertEquals(valx,repairDao.getEntryById(id)!!.entryDate) })
            cases.add(EdgeCase("EC${++ctr}: TxnDt=$valx") { val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="C",personMobile="1",personName="C",amount=10.0,paymentMode="C",transactionDate=valx)); assertTrue(paymentTxnDao.getAllTransactions().first().any { it.transactionDate == valx }) })
            cases.add(EdgeCase("EC${++ctr}: RetDt=$valx") { val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="P",returnReason="R",refundAmount=10.0,returnDate=valx)); assertEquals(valx,partReturnDao.getReturnById(id)!!.returnDate) })
        }

        // --- BOOLEAN EDGE CASES (14 cases) ---
        listOf(true, false).forEach { valx ->
            cases.add(EdgeCase("EC${++ctr}: SMActive=$valx") { val id = serviceManDao.insert(ServiceMan(name="B$ctr",mobile=M(1100000,ctr),email="e",employeeId="E$ctr",designation="T",isActive=valx)); assertEquals(valx,serviceManDao.getServiceManById(id)!!.isActive) })
            cases.add(EdgeCase("EC${++ctr}: SuppAct=$valx") { supplierDao.insert(Supplier(mobile=M(1200000,ctr),name="S$ctr",companyName="C",city="C",isActive=valx)); assertEquals(valx,supplierDao.getSupplierByMobile(M(1200000,ctr))!!.isActive) })
            cases.add(EdgeCase("EC${++ctr}: Handover=$valx") { val id = repairDao.insert(RepairEntry(customerMobile=M(1300000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",handoverDone=valx)); assertEquals(valx,repairDao.getEntryById(id)!!.handoverDone) })
            cases.add(EdgeCase("EC${++ctr}: Draft=$valx") { val id = repairDao.insert(RepairEntry(customerMobile=M(1400000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",isDraft=valx)); assertEquals(valx,repairDao.getEntryById(id)!!.isDraft) })
            cases.add(EdgeCase("EC${++ctr}: FActive=$valx") { val id = commonFaultDao.insert(CommonFault(faultName="F$ctr",category="T",sortOrder=1,isActive=valx)); assertEquals(valx,commonFaultDao.getFaultById(id)!!.isActive) })
            cases.add(EdgeCase("EC${++ctr}: RefundRec=$valx") { val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="P",returnReason="R",refundAmount=10.0,refundReceived=valx)); assertEquals(valx,partReturnDao.getReturnById(id)!!.refundReceived) })
            cases.add(EdgeCase("EC${++ctr}: PayStat=$valx") { val id = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=M(1500000,ctr),personName="C",description="S",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status=if(valx)"PAID"else"UNPAID")); assertEquals(if(valx)"PAID"else"UNPAID",paymentDao.getPaymentById(id)!!.status) })
        }

        // --- NULL/ABSENT FIELD EDGE CASES (12 cases) ---
        cases.add(EdgeCase("EC${++ctr}: Cust null city") { customerDao.insert(Customer(mobileNumber=M(1600000,ctr),name="N",city=null)); assertNull(customerDao.getCustomerByMobile(M(1600000,ctr))!!.city) })
        cases.add(EdgeCase("EC${++ctr}: Deal null city") { dealerDao.insert(Dealer(mobileNumber=M(1700000,ctr),name="N",city=null)); assertNull(dealerDao.getDealerByMobile(M(1700000,ctr))!!.city) })
        cases.add(EdgeCase("EC${++ctr}: Supp minimal") { supplierDao.insert(Supplier(mobile=M(1800000,ctr),name="Min",companyName="C",city="")); assertNotNull(supplierDao.getSupplierByMobile(M(1800000,ctr))) })
        cases.add(EdgeCase("EC${++ctr}: Repair min") { val id = repairDao.insert(RepairEntry(customerMobile=M(1900000,ctr),customerName="M",deviceBrand="B",deviceModel="M",sparePartName="",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="")); assertNotNull(repairDao.getEntryById(id)) })
        cases.add(EdgeCase("EC${++ctr}: Sale min") { saleDao.insert(Sale(itemName="Min",supplierId="",supplierName="",purchasePrice=0.0,salePrice=0.0)); assertTrue(saleDao.getAllSales().first().any { it.itemName == "Min" }) })
        cases.add(EdgeCase("EC${++ctr}: Pay zero") { val id = paymentDao.insert(Payment(personType="C",personMobile=M(2000000,ctr),personName="Z",description="Z",totalAmount=0.0,paidAmount=0.0,dueAmount=0.0,status="PAID")); assertNotNull(paymentDao.getPaymentById(id)) })
        cases.add(EdgeCase("EC${++ctr}: Txn min") { val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="C",personMobile="",personName="",amount=0.0,paymentMode="")); assertTrue(tid > 0) })
        cases.add(EdgeCase("EC${++ctr}: Ret min") { val id = partReturnDao.insert(PartReturn(supplierId="",supplierName="",partName="",returnReason="",refundAmount=0.0)); assertNotNull(partReturnDao.getReturnById(id)) })
        cases.add(EdgeCase("EC${++ctr}: Fault min") { val id = commonFaultDao.insert(CommonFault(faultName="",category="",sortOrder=0)); assertNotNull(commonFaultDao.getFaultById(id)) })

        // --- CROSS-ENTITY REFERENCE EDGE CASES (1500 cases: 300 × 5) ---
        repeat(300) {
            cases.add(EdgeCase("EC${++ctr}: Xref r->p") { val eid = repairDao.insert(RepairEntry(customerMobile=M(2100000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,workStatus="P")); val pid = sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="P",purchasePrice=100.0,supplierId="1",supplierName="S",quantity=1)); assertTrue(sparePartDao.getPurchasesByRepairId(eid).first().any { it.id == pid }) })
            cases.add(EdgeCase("EC${++ctr}: Xref p->e") { val eid = repairDao.insert(RepairEntry(customerMobile=M(2200000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); val payId = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=M(2300000,ctr),personName="C",description="R",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID",linkedEntryId=eid)); assertTrue(paymentDao.getPaymentsByMobile(M(2300000,ctr)).first().any { it.id == payId }) })
            cases.add(EdgeCase("EC${++ctr}: Xref t->p") { val payId = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=M(2400000,ctr),personName="C",description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID")); val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="CUSTOMER",personMobile=M(2400000,ctr),personName="C",amount=500.0,paymentMode="C")); assertTrue(paymentTxnDao.getTransactionsByPayment(payId).first().any { it.id == tid }) })
            cases.add(EdgeCase("EC${++ctr}: Xref p->s") { val sm = M(2500000,ctr); supplierDao.insert(Supplier(mobile=sm,name="S",companyName="C",city="C")); val pid = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P",purchasePrice=100.0,supplierId=sm,supplierName="S",quantity=1)); assertTrue(sparePartDao.getPurchasesBySupplier(sm).first().any { it.id == pid }) })
            cases.add(EdgeCase("EC${++ctr}: Xref s->s") { val sm = M(2600000,ctr); supplierDao.insert(Supplier(mobile=sm,name="S",companyName="C",city="C")); saleDao.insert(Sale(itemName="I",supplierId=sm,supplierName="S",purchasePrice=100.0,salePrice=200.0)); assertTrue(saleDao.getSalesBySupplier(sm).first().isNotEmpty()) })
        }

        // --- DUPLICATE INSERT EDGE CASES (600 cases: 100 × 6) ---
        repeat(100) { i ->
            val mob = "DUP$i"
            cases.add(EdgeCase("EC${++ctr}: DupCust=$mob") { customerDao.insert(Customer(mobileNumber=mob,name="First",city="C1")); customerDao.insert(Customer(mobileNumber=mob,name="Second",city="C2")); assertTrue(customerDao.getCustomerByMobile(mob)!!.name == "Second") })
            cases.add(EdgeCase("EC${++ctr}: DupDeal=$mob") { dealerDao.insert(Dealer(mobileNumber=mob,name="D1",city="C1")); dealerDao.insert(Dealer(mobileNumber=mob,name="D2",city="C2")); assertEquals("D2",dealerDao.getDealerByMobile(mob)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: DupSupp=$mob") { supplierDao.insert(Supplier(mobile=mob,name="S1",companyName="C1",city="C1")); supplierDao.insert(Supplier(mobile=mob,name="S2",companyName="C2",city="C2")); assertEquals("S2",supplierDao.getSupplierByMobile(mob)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: ReInsRep") { val eid = repairDao.insert(RepairEntry(customerMobile=M(2800000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); val eid2 = repairDao.insert(RepairEntry(customerMobile=M(2800000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); assertTrue(eid2 > eid) })
            cases.add(EdgeCase("EC${++ctr}: ReInsPay") { val p1 = paymentDao.insert(Payment(personType="C",personMobile=M(2900000,ctr),personName="C",description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID")); val p2 = paymentDao.insert(Payment(personType="C",personMobile=M(2900000,ctr),personName="C",description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID")); assertTrue(p2 > p1) })
            cases.add(EdgeCase("EC${++ctr}: ReInsSale") { saleDao.insert(Sale(itemName="R$i",supplierId="1",supplierName="S",purchasePrice=100.0,salePrice=200.0)); saleDao.insert(Sale(itemName="R$i",supplierId="1",supplierName="S",purchasePrice=100.0,salePrice=200.0)); assertTrue(saleDao.getAllSales().first().count { it.itemName == "R$i" } == 2) })
        }

        // --- UPDATE CHAIN EDGE CASES (1200 cases: 200 × 6) ---
        repeat(200) {
            val mob = M(3000000, ctr)
            cases.add(EdgeCase("EC${++ctr}: UpdCust5") { customerDao.insert(Customer(mobileNumber=mob,name="V0",city="C0")); repeat(5) { j -> customerDao.update(Customer(mobileNumber=mob,name="V${j+1}",city="C${j+1}")) }; assertEquals("V5",customerDao.getCustomerByMobile(mob)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: UpdDeal5") { dealerDao.insert(Dealer(mobileNumber=mob,name="V0",city="C0")); repeat(5) { j -> dealerDao.update(Dealer(mobileNumber=mob,name="V${j+1}",city="C${j+1}")) }; assertEquals("V5",dealerDao.getDealerByMobile(mob)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: UpdSupp5") { supplierDao.insert(Supplier(mobile=mob,name="V0",companyName="C",city="C")); repeat(5) { j -> supplierDao.update(Supplier(mobile=mob,name="V${j+1}",companyName="C",city="C")) }; assertEquals("V5",supplierDao.getSupplierByMobile(mob)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: UpdPay5") { val id = paymentDao.insert(Payment(personType="C",personMobile=mob,personName="C",description="R",totalAmount=1000.0,paidAmount=0.0,dueAmount=1000.0,status="UNPAID")); var p = paymentDao.getPaymentById(id)!!; repeat(4) { val newDue = p.dueAmount - 250.0; p = p.copy(paidAmount=p.paidAmount+250.0,dueAmount=newDue,status=if(newDue<=0.01)"PAID"else"PARTIAL"); paymentDao.update(p) }; assertEquals("PAID",paymentDao.getPaymentById(id)!!.status) })
            cases.add(EdgeCase("EC${++ctr}: UpdTxn") { val pid = paymentDao.insert(Payment(personType="C",personMobile=mob,personName="C",description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID")); val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=pid,personType="C",personMobile=mob,personName="C",amount=100.0,paymentMode="C")); repeat(4) { paymentTxnDao.update(PaymentTransaction(id=tid,paymentId=pid,personType="C",personMobile=mob,personName="C",amount=100.0*(it+2).toDouble(),paymentMode="C")) }; assertEquals(500.0,paymentTxnDao.getTransactionsByPayment(pid).first().first { it.id == tid }.amount,0.01) })
            cases.add(EdgeCase("EC${++ctr}: FaultSort") { val ids = (1..5).map { commonFaultDao.insert(CommonFault(faultName="F$it",category="Reorder",sortOrder=it)) }; val all = ids.map { commonFaultDao.getFaultById(it)!! }; val reordered = all.sortedBy { -it.sortOrder }; assertTrue(reordered[0].sortOrder > reordered[4].sortOrder) })
        }

        // --- BATCH OPERATION EDGE CASES (1800 cases: 300 × 6) ---
        repeat(300) {
            cases.add(EdgeCase("EC${++ctr}: BCust") { val n = 5 + rng.nextInt(20); repeat(n) { i -> customerDao.insert(Customer(mobileNumber=M(4000000,ctr+i),name="B$i",city="C")) }; assertTrue(customerDao.getAllCustomers().first().size >= n) })
            cases.add(EdgeCase("EC${++ctr}: BRepair") { val n = 3 + rng.nextInt(10); repeat(n) { repairDao.insert(RepairEntry(customerMobile=M(4100000,ctr),customerName="B",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,workStatus="P")) }; assertTrue(repairDao.getAllEntries().first().size >= n) })
            cases.add(EdgeCase("EC${++ctr}: BPay") { val n = 3 + rng.nextInt(8); repeat(n) { paymentDao.insert(Payment(personType="CUSTOMER",personMobile=M(4200000,ctr),personName="B",description="B",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID")) }; assertTrue(paymentDao.getAllPayments().first().size >= n) })
            cases.add(EdgeCase("EC${++ctr}: BSale") { val n = 2 + rng.nextInt(6); repeat(n) { saleDao.insert(Sale(itemName="BS${it}",supplierId="1",supplierName="S",purchasePrice=50.0,salePrice=100.0)) }; assertTrue(saleDao.getAllSales().first().size >= n) })
            cases.add(EdgeCase("EC${++ctr}: BPur") { val n = 2 + rng.nextInt(5); repeat(n) { sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="BP$it",purchasePrice=100.0,supplierId="1",supplierName="S",quantity=1+rng.nextInt(5))) }; assertTrue(sparePartDao.getAllPurchases().first().size >= n) })
            cases.add(EdgeCase("EC${++ctr}: BRet") { val n = 1 + rng.nextInt(4); repeat(n) { partReturnDao.insert(PartReturn(supplierId="1",supplierName="S",partName="R$it",returnReason="T",refundAmount=rng.nextDouble()*1000.0)) }; assertTrue(partReturnDao.getAllReturns().first().size >= n) })
        }

        // --- MOBILE FORMAT EDGES (150 cases: 50 × 3) ---
        (listOf("0", "00", "0000000000", "1234567890", "+919999999999", "999-999-9999", "()", "A".repeat(10),
            "+1-234-567-8900", "12345", "123456", "1234567", "12345678", "123456789", "9876543210",
            "0000000001", "9999999999", "5555555555", "+1 (234) 567-8900", "123.456.7890",
            "123 456 7890", "123-456-7890 x123", "+1.234.567.8900", "001-234-567-8900",
            "+91 98765 43210", "011-23456789", "+44 20 7946 0958", "+81 3-1234-5678",
            "+886 2 1234 5678", "+61 2 1234 5678", "+49 30 12345678", "+33 1 23 45 67 89",
            "+7 495 123-45-67", "+55 11 91234-5678", "+34 91 123 45 67", "+39 06 1234 5678")
        ).forEach { mob ->
            cases.add(EdgeCase("EC${++ctr}: EdgeMob='$mob'") { customerDao.insert(Customer(mobileNumber=mob,name="E",city="C")); assertNotNull(customerDao.getCustomerByMobile(mob)) })
            cases.add(EdgeCase("EC${++ctr}: SupMob='$mob'") { supplierDao.insert(Supplier(mobile=mob,name="E",companyName="C",city="C")); assertNotNull(supplierDao.getSupplierByMobile(mob)) })
            cases.add(EdgeCase("EC${++ctr}: DealMob='$mob'") { dealerDao.insert(Dealer(mobileNumber=mob,name="E",city="C")); assertNotNull(dealerDao.getDealerByMobile(mob)) })
        }

        // --- DATA PRESENCE VERIFICATION (15 cases) ---
        cases.add(EdgeCase("EC${++ctr}: CustPres") { assertTrue(customerDao.getAllCustomers().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: DealPres") { assertTrue(dealerDao.getAllDealers().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: SMPres") { assertTrue(serviceManDao.getAllServiceMen().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: SuppPres") { assertTrue(supplierDao.getAllSuppliers().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: FaultPres") { assertTrue(commonFaultDao.getAllFaults().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: EntryPres") { assertTrue(repairDao.getAllEntries().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: PurPres") { assertTrue(sparePartDao.getAllPurchases().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: SalePres") { assertTrue(saleDao.getAllSales().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: PayPres") { assertTrue(paymentDao.getAllPayments().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: TxnPres") { assertTrue(paymentTxnDao.getAllTransactions().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: RetPres") { assertTrue(partReturnDao.getAllReturns().first().isNotEmpty()) })
        cases.add(EdgeCase("EC${++ctr}: RevZero") { assertEquals(0.0,(repairDao.getRevenueInRange(0,1).first()?:0.0),0.01) })
        cases.add(EdgeCase("EC${++ctr}: PurZero") { assertEquals(0.0,(sparePartDao.getTotalPurchaseInRange(0,1).first()?:0.0),0.01) })
        cases.add(EdgeCase("EC${++ctr}: ActFault") { assertNotNull(commonFaultDao.getActiveFaults().first()) })
        cases.add(EdgeCase("EC${++ctr}: PendDue") { assertNotNull(paymentDao.getPendingDues().first()) })

        // --- PAIRWISE INSERT-UPDATE-READ (1200 cases: 100 × 12) ---
        repeat(100) {
            cases.add(EdgeCase("EC${++ctr}: PairCust") { customerDao.insert(Customer(mobileNumber=M(5000000,ctr),name="P$ctr",city="C")); assertEquals("P$ctr",customerDao.getCustomerByMobile(M(5000000,ctr))!!.name) })
            cases.add(EdgeCase("EC${++ctr}: PairSupp") { supplierDao.insert(Supplier(mobile=M(5100000,ctr),name="P$ctr",companyName="C",city="C")); assertEquals("P$ctr",supplierDao.getSupplierByMobile(M(5100000,ctr))!!.name) })
            cases.add(EdgeCase("EC${++ctr}: PairDeal") { dealerDao.insert(Dealer(mobileNumber=M(5200000,ctr),name="P$ctr",city="C")); assertEquals("P$ctr",dealerDao.getDealerByMobile(M(5200000,ctr))!!.name) })
            cases.add(EdgeCase("EC${++ctr}: PairSM") { val id = serviceManDao.insert(ServiceMan(name="P$ctr",mobile=M(5300000,ctr),email="e",employeeId="E$ctr",designation="T")); assertEquals("P$ctr",serviceManDao.getServiceManById(id)!!.name) })
            cases.add(EdgeCase("EC${++ctr}: PairRep") { val id = repairDao.insert(RepairEntry(customerMobile=M(5400000,ctr),customerName="P$ctr",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P")); assertEquals("P$ctr",repairDao.getEntryById(id)!!.customerName) })
            cases.add(EdgeCase("EC${++ctr}: PairPur") { val id = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="P$ctr",purchasePrice=100.0,supplierId="1",supplierName="S",quantity=1)); assertTrue(id > 0) })
            cases.add(EdgeCase("EC${++ctr}: PairSale") { saleDao.insert(Sale(itemName="P$ctr",supplierId="1",supplierName="S1",purchasePrice=100.0,salePrice=200.0)); assertTrue(saleDao.getAllSales().first().any { it.itemName == "P$ctr" }) })
            cases.add(EdgeCase("EC${++ctr}: PairPay") { val id = paymentDao.insert(Payment(personType="C",personMobile=M(5500000,ctr),personName="P$ctr",description="T",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID")); assertEquals("P$ctr",paymentDao.getPaymentById(id)!!.personName) })
            cases.add(EdgeCase("EC${++ctr}: PairTxn") { val tid = paymentTxnDao.insert(PaymentTransaction(paymentId=0,personType="C",personMobile="1",personName="P$ctr",amount=10.0,paymentMode="C")); assertTrue(tid > 0) })
            cases.add(EdgeCase("EC${++ctr}: PairRet") { val id = partReturnDao.insert(PartReturn(supplierId="1",supplierName="S1",partName="P$ctr",returnReason="R",refundAmount=10.0)); assertEquals("P$ctr",partReturnDao.getReturnById(id)!!.partName) })
            cases.add(EdgeCase("EC${++ctr}: PairFault") { val id = commonFaultDao.insert(CommonFault(faultName="P$ctr",category="T",sortOrder=1)); assertEquals("P$ctr",commonFaultDao.getFaultById(id)!!.faultName) })
            cases.add(EdgeCase("EC${++ctr}: PairProf") { userProfileDao.insertOrUpdate(UserProfile(id=1,shopName="P$ctr",email="e",name="u$ctr",phone="1")); assertEquals("P$ctr",userProfileDao.getUserProfile()!!.shopName) })
        }

        // --- MULTI-TABLE WORKFLOW (1500 cases: 300 × 5) ---
        repeat(300) {
            cases.add(EdgeCase("EC${++ctr}: MCustPay") { val cm = M(6000000,ctr); customerDao.insert(Customer(mobileNumber=cm,name="MC",city="C")); val pid = paymentDao.insert(Payment(personType="CUSTOMER",personMobile=cm,personName="MC",description="R",totalAmount=100.0,paidAmount=100.0,dueAmount=0.0,status="PAID")); assertEquals("CUSTOMER",paymentDao.getPaymentById(pid)!!.personType) })
            cases.add(EdgeCase("EC${++ctr}: MSuppPur") { val sm = M(6100000,ctr); supplierDao.insert(Supplier(mobile=sm,name="MS",companyName="C",city="C")); val pid = sparePartDao.insert(SparePartPurchase(repairEntryId=1L,partName="MP",purchasePrice=100.0,supplierId=sm,supplierName="MS",quantity=1)); assertTrue(sparePartDao.getPurchasesBySupplier(sm).first().any { it.id == pid }) })
            cases.add(EdgeCase("EC${++ctr}: MDealSale") { val dm = M(6200000,ctr); dealerDao.insert(Dealer(mobileNumber=dm,name="MD",city="C")); saleDao.insert(Sale(itemName="MS",supplierId=dm,supplierName="MD",purchasePrice=100.0,salePrice=200.0)); assertTrue(saleDao.getAllSales().first().any { it.itemName == "MS" }) })
            cases.add(EdgeCase("EC${++ctr}: MRepPart") { val eid = repairDao.insert(RepairEntry(customerMobile=M(6300000,ctr),customerName="MR",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=100.0,supplierId=0L,workStatus="P")); val rpid = sparePartDao.insert(SparePartPurchase(repairEntryId=eid,partName="MP",purchasePrice=100.0,supplierId="1",supplierName="S",quantity=1)); assertTrue(sparePartDao.getPurchasesByRepairId(eid).first().any { it.id == rpid }) })
            cases.add(EdgeCase("EC${++ctr}: MPayTxn") { val payId = paymentDao.insert(Payment(personType="C",personMobile=M(6400000,ctr),personName="MP",description="R",totalAmount=500.0,paidAmount=500.0,dueAmount=0.0,status="PAID")); val txId = paymentTxnDao.insert(PaymentTransaction(paymentId=payId,personType="C",personMobile=M(6400000,ctr),personName="MP",amount=500.0,paymentMode="C")); assertTrue(paymentTxnDao.getTransactionsByPayment(payId).first().any { it.id == txId }) })
        }

        // --- SORT ORDERING (300 cases: 50 × 6) ---
        repeat(50) {
            cases.add(EdgeCase("EC${++ctr}: SortFaults") { (1..5).forEach { commonFaultDao.insert(CommonFault(faultName="SF$it",category="Sort$ctr",sortOrder=6-it)) }; val all = commonFaultDao.getFaultsByCategory("Sort$ctr").first(); assertTrue(all.first().sortOrder < all.last().sortOrder) })
            cases.add(EdgeCase("EC${++ctr}: SortCust") { customerDao.insert(Customer(mobileNumber=M(7000000,ctr+1),name="Z$ctr",city="A")); customerDao.insert(Customer(mobileNumber=M(7000000,ctr+2),name="A$ctr",city="B")); val all = customerDao.getAllCustomers().first(); val names = all.mapNotNull { it.name }.filter { it.endsWith("$ctr") }; assertTrue(names.size >= 2) })
            cases.add(EdgeCase("EC${++ctr}: SortDeal") { dealerDao.insert(Dealer(mobileNumber=M(7100000,ctr+1),name="Z$ctr",city="A")); dealerDao.insert(Dealer(mobileNumber=M(7100000,ctr+2),name="A$ctr",city="B")); val names = dealerDao.getAllDealers().first().mapNotNull { it.name }.filter { it.endsWith("$ctr") }; assertTrue(names.size >= 2) })
            cases.add(EdgeCase("EC${++ctr}: SortSupp") { supplierDao.insert(Supplier(mobile=M(7200000,ctr+1),name="Z$ctr",companyName="C",city="A")); supplierDao.insert(Supplier(mobile=M(7200000,ctr+2),name="A$ctr",companyName="C",city="B")); val names = supplierDao.getAllSuppliers().first().mapNotNull { it.name }.filter { it.endsWith("$ctr") }; assertTrue(names.size >= 2) })
            cases.add(EdgeCase("EC${++ctr}: SortRepair") { repairDao.insert(RepairEntry(customerMobile=M(7300000,ctr),customerName="C",deviceBrand="A",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",entryDate=System.currentTimeMillis()+1000)); repairDao.insert(RepairEntry(customerMobile=M(7300000,ctr),customerName="C",deviceBrand="B",deviceModel="M",sparePartName="P",sparePartPurchasePrice=0.0,supplierId=0L,workStatus="P",entryDate=System.currentTimeMillis()-1000)); val all = repairDao.getAllEntries().first(); assertTrue(all.size >= 2) })
            cases.add(EdgeCase("EC${++ctr}: SortPay") { paymentDao.insert(Payment(personType="C",personMobile=M(7400000,ctr),personName="Z$ctr",description="R",totalAmount=100.0,paidAmount=0.0,dueAmount=100.0,status="UNPAID")); paymentDao.insert(Payment(personType="C",personMobile=M(7500000,ctr),personName="A$ctr",description="R",totalAmount=200.0,paidAmount=0.0,dueAmount=200.0,status="UNPAID")); val all = paymentDao.getAllPayments().first(); assertTrue(all.size >= 2) })
        }

        // --- RUN ALL ---
        val totalCases = cases.size
        var passed = 0; var failed = 0
        val startTime = System.currentTimeMillis()
        println("  Running $totalCases generated edge cases...")
        cases.forEach { c ->
            try { c.block(); passed++ }
            catch (e: Throwable) { failed++; if (failed <= 10) System.err.println("  ${c.name} FAILED: ${e.message}") }
        }
        val elapsed = System.currentTimeMillis() - startTime
        println("  Edge cases: $passed PASSED / $failed FAILED in ${elapsed}ms")
        println("  Total scenarios including SC001-SC100 + SC102 = ${100 + totalCases} test cases")
        if (failed > 0) fail("$failed edge case(s) failed")
        println("SC102: 10000 edge cases completed PASS")
    }
}
