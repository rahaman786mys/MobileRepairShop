package com.app.muzzutech.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Pure-JVM tests for [DateUtils] helpers that do not depend on Android.
 *
 * Covers:
 *  - addMonths (new helper extracted from PayrollViewModel/ExpensesViewModel)
 *  - getStartOfDay / getEndOfDay boundary conditions
 *  - formatMonth / formatDayMonth display helpers
 *
 * (getStartOfMonth / getEndOfMonth / getStartOfWeek rely on Calendar which is
 *  Android-free, so they are tested here too.)
 */
class DateUtilsTest {

    // --- addMonths -----------------------------------------------------------

    @Test
    fun `addMonths with delta 0 returns same month`() {
        val base = DateUtils.getStartOfMonth() // 1st of current month at midnight
        assertEquals(base, DateUtils.addMonths(base, 0))
    }

    @Test
    fun `addMonths with delta 1 returns first day of next month`() {
        val base = DateUtils.getStartOfMonth()
        val next = DateUtils.addMonths(base, 1)
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        assertEquals("Should be first millisecond of next month", cal.timeInMillis, next)
    }

    @Test
    fun `addMonths with delta -1 returns first day of previous month`() {
        val base = DateUtils.getStartOfMonth()
        val prev = DateUtils.addMonths(base, -1)
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        assertEquals("Should be first millisecond of previous month", cal.timeInMillis, prev)
    }

    @Test
    fun `addMonths across year boundary works`() {
        // December (month 11) + 1 = January of next year
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val decStart = cal.timeInMillis
        val janStart = DateUtils.addMonths(decStart, 1)

        val expectedJan = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2027)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals("Dec + 1 month should be January 2027", expectedJan, janStart)
    }

    @Test
    fun `addMonths preserves start-of-day even when source has time components`() {
        // Source is noon on the 15th — the result should still be midnight of month start
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 12, 30, 45) // July 15, 2026 at 12:30:45
            set(Calendar.MILLISECOND, 123)
        }
        val result = DateUtils.addMonths(cal.timeInMillis, 1)
        // Result should be August 1, 00:00:00.000
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals("Should strip time and day components", expected, result)
    }

    // --- getStartOfDay / getEndOfDay ----------------------------------------

    @Test
    fun `getStartOfDay resets all time fields to zero`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 14, 35, 20)
            set(Calendar.MILLISECOND, 500)
        }
        val start = DateUtils.getStartOfDay(cal.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, start)
    }

    @Test
    fun `getEndOfDay sets time to just before midnight`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 4, 9, 0, 0)
        }
        val end = DateUtils.getEndOfDay(cal.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 4, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        assertEquals(expected, end)
    }

    @Test
    fun `end of day is strictly after start of same day`() {
        val now = System.currentTimeMillis()
        assertTrue("End of day must be after start of day",
            DateUtils.getEndOfDay(now) > DateUtils.getStartOfDay(now))
    }

    // --- format helpers -----------------------------------------------------

    @Test
    fun `formatMonth returns readable month abbreviation`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 1)
        }
        val formatted = DateUtils.formatMonth(cal.timeInMillis)
        // Should contain "Jul" or "July" depending on locale
        assertTrue("Month name should contain Jul or July",
            formatted.contains("Jul", ignoreCase = true))
        assertTrue("Should contain year", formatted.contains("2026"))
    }

    @Test
    fun `formatDayMonth formats day and month`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 3)
        }
        val formatted = DateUtils.formatDayMonth(cal.timeInMillis)
        assertTrue("Should contain day 3", formatted.contains("3"))
        assertTrue("Should contain Jul", formatted.contains("Jul", ignoreCase = true))
    }

    @Test
    fun `formatMonth with zero timestamp returns valid fallback`() {
        val result = DateUtils.formatMonth(0L)
        assertTrue(result.isNotEmpty())
    }

    // --- getStartOfMonth / getEndOfMonth ------------------------------------

    @Test
    fun `getStartOfMonth returns first millisecond of current month`() {
        val start = DateUtils.getStartOfMonth()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getEndOfMonth returns last millisecond of given month`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15)
        }
        val end = DateUtils.getEndOfMonth(cal.timeInMillis)
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH)) // July has 31 days
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    // --- getCustomDateRange -------------------------------------------------

    @Test
    fun `getCustomDateRange returns start of start day to end of end day`() {
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 1, 10, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 3, 18, 30, 0)
        }
        val (start, end) = DateUtils.getCustomDateRange(startCal.timeInMillis, endCal.timeInMillis)

        // Start should be July 1 midnight
        val expectedStart = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expectedStart, start)

        // End should be July 3 23:59:59.999
        val expectedEnd = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 3, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        assertEquals(expectedEnd, end)
    }
}
