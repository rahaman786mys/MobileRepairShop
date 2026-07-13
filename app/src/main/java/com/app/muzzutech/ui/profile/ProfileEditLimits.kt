package com.app.muzzutech.ui.profile

import android.content.Context
import com.app.muzzutech.utils.crpto.SecurePrefs

/**
 * Tracks how many times each profile field has been edited and enforces per-field
 * limits. Counts are persisted in encrypted app settings.
 *
 * Limits (edits allowed AFTER the value entered at registration):
 *  - Profile photo: 3
 *  - Full name:     2
 *  - Shop name:     2
 *  - Shop address:  2
 *  - GST number:    unlimited (optional field)
 *  - Phone number:  not editable here (must raise a ticket)
 */
object ProfileEditLimits {

    const val MAX_NAME = 2
    const val MAX_SHOP_NAME = 2
    const val MAX_ADDRESS = 2
    const val MAX_PHOTO = 3

    // Number of self-service phone-number change requests before it becomes
    // manual-review only. The last allowed request shows a "last attempt" warning.
    const val MAX_PHONE_CHANGE = 2

    const val KEY_NAME = "pf_edits_name"
    const val KEY_SHOP_NAME = "pf_edits_shopname"
    const val KEY_ADDRESS = "pf_edits_address"
    const val KEY_PHOTO = "pf_edits_photo"
    const val KEY_PHONE_CHANGE = "pf_phone_change_reqs"

    fun count(context: Context, key: String): Int =
        SecurePrefs.appSettings(context).getInt(key, 0)

    fun increment(context: Context, key: String) {
        val prefs = SecurePrefs.appSettings(context)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun remaining(context: Context, key: String, max: Int): Int =
        (max - count(context, key)).coerceAtLeast(0)

    fun isLocked(context: Context, key: String, max: Int): Boolean =
        count(context, key) >= max
}
