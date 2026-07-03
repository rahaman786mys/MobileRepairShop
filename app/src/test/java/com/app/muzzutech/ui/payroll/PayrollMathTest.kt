package com.app.muzzutech.ui.payroll

import com.app.muzzutech.data.model.SalaryPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario-driven tests for the pure-Kotlin payroll math.
 *
 * Rule (per design):
 *   - Effective per-day rate =
 *       explicit perDaySalary if > 0, else
 *       monthlySalary / 30 if monthlySalary > 0, else 0.
 *   - Payable = effective-per-day × workedDays.
 *   - half-day counts 0.5, full-present 1.0, absent 0.
 *   - Status: UNPAID (paid<=0), PAID (paid>=computed), PARTIAL (otherwise).
 */
class PayrollMathTest {

    // --- computeWorkedDays ---------------------------------------------------

    @Test
    fun `full present counts as full day`() {
        assertEquals(1.0, PayrollMath.computeWorkedDays(1, 0, 0), 0.001)
    }

    @Test
    fun `half present counts as half day`() {
        assertEquals(0.5, PayrollMath.computeWorkedDays(0, 1, 0), 0.001)
    }

    @Test
    fun `absent counts zero`() {
        assertEquals(0.0, PayrollMath.computeWorkedDays(0, 0, 1), 0.001)
    }

    @Test
    fun `mixed month with full half and absent`() {
        // 20 full + 4 half + 6 absent -> 22.0
        assertEquals(22.0, PayrollMath.computeWorkedDays(20, 4, 6), 0.001)
    }

    @Test
    fun `all absent returns zero`() {
        assertEquals(0.0, PayrollMath.computeWorkedDays(0, 0, 30), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative full count throws`() {
        PayrollMath.computeWorkedDays(-1, 0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative half count throws`() {
        PayrollMath.computeWorkedDays(0, -1, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative absent count throws`() {
        PayrollMath.computeWorkedDays(0, 0, -1)
    }

    // --- computePayable -----------------------------------------------------

    @Test
    fun `explicit perDaySalary used when set`() {
        assertEquals(500.0, PayrollMath.computePayable(0.0, 500.0, 1.0), 0.01)
    }

    @Test
    fun `monthly salary divided by 30 when perDaySalary is zero`() {
        assertEquals(15000.0 / 30.0, PayrollMath.computePayable(15000.0, 0.0, 1.0), 0.01)
    }

    @Test
    fun `zero salary fields produce zero payable`() {
        assertEquals(0.0, PayrollMath.computePayable(0.0, 0.0, 25.0), 0.01)
    }

    @Test
    fun `zero worked days always zero payable even with salary`() {
        assertEquals(0.0, PayrollMath.computePayable(30000.0, 1000.0, 0.0), 0.01)
    }

    @Test
    fun `explicit perDay wins over monthly when both set`() {
        // monthly 30000 (1000/day implied) BUT explicit 700 -> 700 used
        assertEquals(700.0, PayrollMath.computePayable(30000.0, 700.0, 1.0), 0.01)
    }

    @Test
    fun `payable scales linearly with worked days`() {
        val perDay = 600.0
        assertEquals(600.0, PayrollMath.computePayable(0.0, perDay, 1.0), 0.01)
        assertEquals(6000.0, PayrollMath.computePayable(0.0, perDay, 10.0), 0.01)
        assertEquals(18000.0, PayrollMath.computePayable(0.0, perDay, 30.0), 0.01)
    }

    @Test
    fun `half-day worked at 0_5 multiplies to half-day pay`() {
        assertEquals(175.0, PayrollMath.computePayable(0.0, 350.0, 0.5), 0.01)
    }

    // --- buildSalaryPayment --------------------------------------------------

    @Test
    fun `buildSalaryPayment snapshots per-day rate from explicit value`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 1, smName = "Ramu",
            monthStart = 1_720_000_000_000L,
            workedDays = 28.0,
            monthlySalary = 15000.0,
            perDaySalary = 600.0,
            paidAmount = 0.0
        )
        assertEquals(600.0, slip.perDaySalary, 0.01)
        assertEquals(16800.0, slip.computedAmount, 0.01)
        assertEquals(16800.0, slip.dueAmount, 0.01)
        assertEquals("UNPAID", slip.status)
    }

    @Test
    fun `buildSalaryPayment derives per-day from monthly when explicit is zero`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 2, smName = "Sita",
            monthStart = 1_720_000_000_000L,
            workedDays = 30.0,
            monthlySalary = 18000.0,
            perDaySalary = 0.0,
            paidAmount = 0.0
        )
        assertEquals(18000.0 / 30.0, slip.perDaySalary, 0.01)
        assertEquals(18000.0, slip.computedAmount, 0.01)
    }

    @Test
    fun `buildSalaryPayment marks PAID when paid equals computed`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 3, smName = "T",
            monthStart = 1L, workedDays = 25.0,
            monthlySalary = 0.0, perDaySalary = 400.0,
            paidAmount = 10000.0
        )
        assertEquals("PAID", slip.status)
        assertEquals(0.0, slip.dueAmount, 0.01)
    }

    @Test
    fun `buildSalaryPayment marks PARTIAL when partially paid`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 3, smName = "T",
            monthStart = 1L, workedDays = 25.0,
            monthlySalary = 0.0, perDaySalary = 400.0,
            paidAmount = 5000.0
        )
        assertEquals("PARTIAL", slip.status)
        assertEquals(5000.0, slip.dueAmount, 0.01)
    }

    @Test
    fun `buildSalaryPayment populates snapshot fields`() {
        val slip: SalaryPayment = PayrollMath.buildSalaryPayment(
            smId = 7, smName = "Javed",
            monthStart = 99L, workedDays = 12.5,
            monthlySalary = 0.0, perDaySalary = 800.0,
            paidAmount = 0.0, note = "advance pending"
        )
        assertEquals(7L, slip.servicemanId)
        assertEquals("Javed", slip.servicemanName)
        assertEquals(99L, slip.monthStart)
        assertEquals(12.5, slip.daysWorked, 0.001)
        assertEquals("advance pending", slip.note)
        assertEquals(10000.0, slip.computedAmount, 0.01)
    }

    @Test
    fun `buildSalaryPayment with zero worked days yields zero amount`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 1, smName = "x",
            monthStart = 1L, workedDays = 0.0,
            monthlySalary = 30000.0, perDaySalary = 0.0,
            paidAmount = 0.0
        )
        assertEquals(0.0, slip.computedAmount, 0.01)
        assertEquals("UNPAID", slip.status)
    }

    @Test
    fun `due amount never goes negative even if paid exceeds computed`() {
        val slip = PayrollMath.buildSalaryPayment(
            smId = 1, smName = "x",
            monthStart = 1L, workedDays = 5.0,
            monthlySalary = 0.0, perDaySalary = 200.0,
            paidAmount = 99999.0
        )
        assertEquals(1000.0, slip.computedAmount, 0.01)
        assertTrue("Due should be 0 not negative, was ${slip.dueAmount}", slip.dueAmount <= 0.0)
        assertEquals("PAID", slip.status)
    }
}
