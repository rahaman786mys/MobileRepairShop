package com.app.muzzutech.utils

import com.google.android.material.textfield.TextInputLayout

object ValidationUtils {

    /**
     * Validates if a phone number is exactly 10 digits.
     * Sets error on TextInputLayout if invalid.
     */
    fun validatePhoneNumber(til: TextInputLayout): Boolean {
        val phone = til.editText?.text.toString().trim()
        return if (phone.length == 10 && phone.all { it.isDigit() }) {
            til.error = null
            til.isErrorEnabled = false
            true
        } else {
            til.error = "Enter valid 10-digit number"
            til.isErrorEnabled = true
            false
        }
    }
}
