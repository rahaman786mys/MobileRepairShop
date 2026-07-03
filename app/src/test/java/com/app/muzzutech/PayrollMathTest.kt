package com.app.muzzutech

import com.app.muzzutech.ui.payroll.PayrollMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollMathTest {

    @Test
    fun computeWorkedDays_allPresent() {
        assertEquals(30.0, PayrollMath.computeWorkedDays(30, 0, 0), 0.001)
    }

    @Test
    fun computeWorkedDays_mixed() {
        assertEquals(25.0, PayrollMath.computeWorkedDays(20, 10, 0), 0.001)
    }

    @Test
    fun computeWorkedDays_allHalf() {
        assertEquals(15.0, PayrollMath.computeWorkedDays(0, 30, 0), 0.001)
    }

    @Test
    fun computeWorkedDays_allAbsent() {
        assertEquals(0.0, PayrollMath.computeWorkedDays(0, 0, 30), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun computeWorkedDays_negativeFull() {
        PayrollMath.computeWorkedDays(-1, 0, 0)
    }

    @Test
    fun computePayable_usesPerDaySalary_whenSet() {
        assertEquals(3000.0, PayrollMath.computePayable(0.0, 300.0, 10.0), 0.001)
    }

    @Test
    fun computePayable_derivesFromMonthly_whenNoPerDay() {
        assertEquals(10000.0, PayrollMath.computePayable(30000.0, 0.0, 10.0), 0.001)
    }

    @Test
    fun computePayable_zeroWhenNone() {
        assertEquals(0.0, PayrollMath.computePayable(0.0, 0.0, 10.0), 0.001)
    }

    @Test
    fun buildSalaryPayment_unpaid() {
        val slip = PayrollMath.buildSalaryPayment(1L, "Test", 1000L, 20.0, 30000.0, 0.0, 0.0)
        assertEquals("UNPAID", slip.status)
        assertEquals(20000.0, slip.computedAmount, 0.001)
        assertEquals(20000.0, slip.dueAmount, 0.001)
    }

    @Test
    fun buildSalaryPayment_paid() {
        val slip = PayrollMath.buildSalaryPayment(1L, "Test", 1000L, 20.0, 30000.0, 0.0, 20000.0)
        assertEquals("PAID", slip.status)
        assertEquals(0.0, slip.dueAmount, 0.001)
    }

    @Test
    fun buildSalaryPayment_partial() {
        val slip = PayrollMath.buildSalaryPayment(1L, "Test", 1000L, 20.0, 30000.0, 0.0, 10000.0)
        assertEquals("PARTIAL", slip.status)
        assertEquals(10000.0, slip.dueAmount, 0.001)
    }
}
