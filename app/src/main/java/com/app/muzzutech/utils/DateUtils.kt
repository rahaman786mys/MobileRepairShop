package com.app.muzzutech.utils

import java.util.*

/**
 * Thread-safe utility for date formatting and calculations.
 *
 * All [SimpleDateFormat] instances are created per-call to avoid
 * concurrency issues (SimpleDateFormat is NOT thread-safe).
 */
object DateUtils {

    private const val DISPLAY_PATTERN = "dd-MMM-yyyy hh:mm a"
    private const val DATE_ONLY_PATTERN = "dd-MMM-yyyy"
    private const val TIME_ONLY_PATTERN = "hh:mm a"
    private const val REPORT_DATE_PATTERN = "yyyy-MM-dd"
    private const val MONTH_PATTERN = "MMM yyyy"
    private const val DAY_MONTH_PATTERN = "dd MMM"

    fun formatDateTime(timestamp: Long): String {
        return if (timestamp > 0) {
            java.text.SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault()).format(Date(timestamp))
        } else {
            "-"
        }
    }

    fun formatDate(timestamp: Long): String {
        return if (timestamp > 0) {
            java.text.SimpleDateFormat(DATE_ONLY_PATTERN, Locale.getDefault()).format(Date(timestamp))
        } else {
            "-"
        }
    }

    fun formatTime(timestamp: Long): String {
        return if (timestamp > 0) {
            java.text.SimpleDateFormat(TIME_ONLY_PATTERN, Locale.getDefault()).format(Date(timestamp))
        } else {
            "-"
        }
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
        return java.text.SimpleDateFormat(MONTH_PATTERN, Locale.getDefault()).format(Date(timestamp))
    }

    /** Format a date for display (e.g. "03 Jul"). */
    fun formatDayMonth(timestamp: Long): String {
        return java.text.SimpleDateFormat(DAY_MONTH_PATTERN, Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Get custom date range
     */
    fun getCustomDateRange(startTimestamp: Long, endTimestamp: Long): Pair<Long, Long> {
        return Pair(getStartOfDay(startTimestamp), getEndOfDay(endTimestamp))
    }

    /**
     * Move a month start forward or backward by [delta] months.
     * Returns the first millisecond of the resulting month.
     */
    fun addMonths(monthStart: Long, delta: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(java.util.Calendar.MONTH, delta)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
