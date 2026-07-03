package com.app.muzzutech.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario-driven tests for the [Expense] model and its category constants.
 *
 * Pure-JVM (no DB, no Android) — verifies data integrity invariants the DAOs
 * and UI rely on.
 */
class ExpenseModelTest {

    @Test
    fun `default category is OTHER`() {
        assertEquals(Expense.CATEGORY_OTHER, Expense().category)
    }

    @Test
    fun `default expense is paid once-off`() {
        val e = Expense()
        assertTrue(e.paid)
        assertFalse(e.isRecurring)
    }

    @Test
    fun `default amount is zero`() {
        assertEquals(0.0, Expense().amount, 0.001)
    }

    @Test
    fun `default title is empty`() {
        assertEquals("", Expense().title)
    }

    @Test
    fun `all category constants are distinct`() {
        val cats = listOf(
            Expense.CATEGORY_RENT,
            Expense.CATEGORY_ELECTRICITY,
            Expense.CATEGORY_SALARY,
            Expense.CATEGORY_INTERNET,
            Expense.CATEGORY_SUPPLIES,
            Expense.CATEGORY_OTHER
        )
        assertEquals(cats.size, cats.toSet().size)
    }

    @Test
    fun `copy preserves id but updates paid flag`() {
        val original = Expense(id = 101, title = "Rent July", amount = 15000.0, paid = false)
        val toggled = original.copy(paid = true)
        assertEquals(101L, toggled.id)
        assertTrue(toggled.paid)
        assertNotEquals(original.paid, toggled.paid)
    }

    @Test
    fun `copy preserves id but updates recurring flag`() {
        val original = Expense(id = 202, title = "Internet", amount = 800.0, isRecurring = false)
        val recurring = original.copy(isRecurring = true)
        assertEquals(202L, recurring.id)
        assertTrue(recurring.isRecurring)
    }

    @Test
    fun `rental expense has correct category`() {
        val rent = Expense(title = "Shop Rent", amount = 22000.0, category = Expense.CATEGORY_RENT)
        assertEquals("Rent", rent.category)
    }

    @Test
    fun `salary expense has correct category`() {
        val salary = Expense(
            title = "Sita month-end",
            amount = 18000.0,
            category = Expense.CATEGORY_SALARY,
            isRecurring = true
        )
        assertEquals("Salary", salary.category)
        assertTrue(salary.isRecurring)
    }

    @Test
    fun `round-trip copy preserves all fields`() {
        val e = Expense(
            id = 5L, title = "Electricity bill",
            amount = 4500.0, category = Expense.CATEGORY_ELECTRICITY,
            date = 123456L, isRecurring = true, paid = true, note = "monthly"
        )
        val copy = e.copy()
        assertEquals(e, copy)
    }

    @Test
    fun `amount mutable via copy`() {
        val e = Expense(id = 1L, amount = 1000.0)
        val updated = e.copy(amount = 1200.0)
        assertEquals(1200.0, updated.amount, 0.001)
        assertEquals(1000.0, e.amount, 0.001) // original unchanged (immutable data class)
    }
}
