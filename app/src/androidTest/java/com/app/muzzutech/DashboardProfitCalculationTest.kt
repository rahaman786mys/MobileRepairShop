package com.app.muzzutech

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.Expense
import com.app.muzzutech.data.model.PaymentTransaction
import com.app.muzzutech.data.model.SalaryPayment
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardProfitCalculationTest {

    private lateinit var db: AppDatabase
    private val today = System.currentTimeMillis()

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun repair10000_advance3000_display5000_salary500_electricity600_profitEquals3900() = runBlocking {
        val todayStart = DateUtils.getStartOfDay(today)
        val todayEnd = DateUtils.getEndOfDay(today)
        val monthStart = DateUtils.getStartOfMonth(today)
        val monthEnd = DateUtils.getEndOfMonth(monthStart)

        // 1. Device repair ₹10,000 — customer paid via cash-in
        db.paymentTransactionDao().insert(
            PaymentTransaction(
                personType = "CUSTOMER",
                personMobile = "9999999999",
                personName = "Test Customer",
                amount = 10_000 * 100, // ₹10,000 in paise = 1,000,000
                paymentMode = "CASH",
                transactionDate = today
            )
        )

        // 2. Display from supplier ₹5,000 — part purchase
        db.sparePartPurchaseDao().insert(
            SparePartPurchase(
                partName = "Display",
                purchasePrice = 5_000 * 100, // ₹5,000 in paise = 500,000
                quantity = 1,
                supplierId = "8888888888",
                supplierName = "Test Supplier",
                purchaseDate = today
            )
        )

        // 3. Electricity ₹600 — shop expense
        db.expenseDao().insert(
            Expense(
                title = "Electricity",
                amount = 600 * 100, // ₹600 in paise = 60,000
                category = "Electricity",
                paid = true,
                date = today
            )
        )

        // 4. Worker salary ₹500 — salary payment (needs a ServiceMan record for FK)
        db.serviceManDao().insert(
            ServiceMan(
                id = 1,
                name = "Worker",
                mobile = "7777777777"
            )
        )
        db.salaryDao().insert(
            SalaryPayment(
                servicemanId = 1,
                servicemanName = "Worker",
                paidAmount = 500 * 100, // ₹500 in paise = 50,000
                dueAmount = 0,
                status = "PAID",
                monthStart = monthStart
            )
        )

        // Run the same queries the dashboard uses
        val customerCashIn = db.paymentTransactionDao()
            .getTransactionsByDateRange(todayStart, todayEnd)
            .first()
            .filter { (it.personType == "CUSTOMER" || it.personType == "DEALER") && it.amount > 0L }
            .sumOf { it.amount }

        val saleRevenue = db.saleDao()
            .getSalesByDateRange(todayStart, todayEnd)
            .first()
            .sumOf { it.salePrice }

        val partReturnRefunds = db.partReturnDao()
            .getReturnsByDateRangeQuery(todayStart, todayEnd)
            .first()
            .sumOf { it.refundAmount }

        val partPurchases = db.sparePartPurchaseDao()
            .getPurchasesByDateRange(todayStart, todayEnd)
            .first()
            .sumOf { it.purchasePrice * it.quantity }

        val shopExpenses = db.expenseDao()
            .getByDateRange(todayStart, todayEnd)
            .first()
            .sumOf { it.amount }

        val salaries = db.salaryDao()
            .getByMonth(monthStart, monthEnd)
            .first()
        val salaryPayouts = salaries.sumOf { it.paidAmount }

        val supplierPayments = db.paymentTransactionDao()
            .getTransactionsByDateRange(todayStart, todayEnd)
            .first()
            .filter { it.personType == "SUPPLIER" && it.amount > 0L }
            .sumOf { it.amount }

        // Cash-basis profit formula from DashboardViewModel
        val totalRevenue = customerCashIn + saleRevenue + partReturnRefunds
        val totalCost = partPurchases + shopExpenses + salaryPayouts + supplierPayments
        val profit = totalRevenue - totalCost

        // Revenue: ₹10,000 cash-in = 1,000,000 paise
        assertEquals("customer cash-in", 1_000_000, customerCashIn)
        assertEquals("sale revenue", 0, saleRevenue)
        assertEquals("part return refunds", 0, partReturnRefunds)
        assertEquals("total revenue", 1_000_000, totalRevenue)

        // Costs: display ₹5,000 + electricity ₹600 + salary ₹500 = ₹6,100 = 610,000 paise
        assertEquals("part purchases", 500_000, partPurchases)
        assertEquals("shop expenses", 60_000, shopExpenses)
        assertEquals("salary payouts", 50_000, salaryPayouts)
        assertEquals("supplier payments", 0, supplierPayments)
        assertEquals("total cost", 610_000, totalCost)

        // Profit = 1,000,000 − 610,000 = 390,000 paise = ₹3,900
        assertEquals("profit in paise", 390_000, profit)
        assertEquals("profit in rupees", 3_900, profit / 100)
    }
}