package com.app.muzzutech.utils

import java.math.BigDecimal
import java.math.RoundingMode

object PriceUtils {

    private val CURRENCY_SYMBOL = "\u20B9"

    /**
     * Formats paise to Rupees with symbol: e.g. 15000 → "₹ 150.00"
     */
    fun formatPrice(paise: Long): String {
        val rupees = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return "$CURRENCY_SYMBOL ${rupees.setScale(2, RoundingMode.HALF_UP)}"
    }

    /**
     * Formats paise to Rupees as plain number string.
     */
    fun formatAmount(paise: Long): String {
        val rupees = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return rupees.setScale(2, RoundingMode.HALF_UP).toString()
    }

    /**
     * Converts paise to Double (for legacy calculations / charting only).
     */
    fun toDouble(paise: Long): Double = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toDouble()
}
