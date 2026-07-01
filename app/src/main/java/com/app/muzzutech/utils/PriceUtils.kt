package com.app.muzzutech.utils

import java.util.Locale

object PriceUtils {

    /**
     * Formats a double amount into a currency string with Rupee symbol and no decimals if possible.
     */
    fun formatPrice(amount: Double): String {
        return String.format(Locale.getDefault(), "₹ %.0f", amount)
    }

    /**
     * Formats a double amount into a string with no decimals.
     */
    fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.0f", amount)
    }
}
