package com.app.muzzutech.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for date formatting and calculations
 */
object DateUtils {

    private val displayFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val reportDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDateTime(timestamp: Long): String {
        return if (timestamp > 0) displayFormat.format(Date(timestamp)) else "-"
    }

    fun formatDate(timestamp: Long): String {
        return if (timestamp > 0) dateOnlyFormat.format(Date(timestamp)) else "-"
    }

    fun formatTime(timestamp: Long): String {
        return if (timestamp > 0) timeOnlyFormat.format(Date(timestamp)) else "-"
    }

    /**
     * Get start of day (12:00:00 AM) for a given timestamp
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get end of day (11:59:59 PM) for a given timestamp
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Get start of the week (Monday)
     */
    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get start of the month for "now".
     */
    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get start of the month that contains [timestamp].
     */
    fun getStartOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get end of the month that contains [timestamp] (last millisecond of the month).
     */
    fun getEndOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /** Format a month for display (e.g. "Jul 2026"). */
    fun formatMonth(timestamp: Long): String {
        return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    /** Format a date for display (e.g. "03 Jul"). */
    fun formatDayMonth(timestamp: Long): String {
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Get custom date range
     */
    fun getCustomDateRange(startTimestamp: Long, endTimestamp: Long): Pair<Long, Long> {
        return Pair(getStartOfDay(startTimestamp), getEndOfDay(endTimestamp))
    }
}
