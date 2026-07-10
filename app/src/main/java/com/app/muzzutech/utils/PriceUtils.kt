package com.app.muzzutech.utils

import java.math.BigDecimal
import java.math.RoundingMode

object PriceUtils {

    private val CURRENCY_SYMBOL = "\u20B9"
    private val ROUNDING = RoundingMode.HALF_EVEN

    /**
     * Formats paise to Rupees with symbol: e.g. 15000 → "₹ 150.00"
     */
    fun formatPrice(paise: Long): String {
        val rupees = BigDecimal(paise).divide(BigDecimal(100), 2, ROUNDING)
        return "$CURRENCY_SYMBOL ${rupees.setScale(2, ROUNDING)}"
    }

    /**
     * Formats paise to Rupees as plain number string.
     */
    fun formatAmount(paise: Long): String {
        val rupees = BigDecimal(paise).divide(BigDecimal(100), 2, ROUNDING)
        return rupees.setScale(2, ROUNDING).toString()
    }

    /**
     * Formats paise to Rupees with symbol. If amount is negative, labels as "Credit".
     */
    fun formatDueBalance(paise: Long): String {
        return if (paise < 0) {
            "Credit: ${formatPrice(kotlin.math.abs(paise))}"
        } else {
            formatPrice(paise)
        }
    }

    /**
     * Compute (baseAmountPaise × percentage / 100) with Banker's Rounding (HALF_EVEN).
     * Example: computePercentage(15000, 18.0) = 2700 paise (₹27.00 GST on ₹150.00 at 18%).
     * Uses BigDecimal throughout — integer division is strictly forbidden.
     */
    fun computePercentage(baseAmountPaise: Long, percentage: Double): Long {
        val base = BigDecimal(baseAmountPaise)
        val pct = BigDecimal(percentage)
        val result = base.multiply(pct).divide(BigDecimal(100), 0, ROUNDING)
        return result.longValueExact()
    }
}
