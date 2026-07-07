package com.app.muzzutech.utils.crpto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private const val APP_SETTINGS = "app_settings_encrypted"
    private const val AUTH_PREFS = "auth_prefs_encrypted"

    private val masterKeyCache = HashMap<String, MasterKey>()
    private var migrated = false

    fun appSettings(context: Context): SharedPreferences {
        migrateFromLegacy(context)
        return getPrefs(context, APP_SETTINGS)
    }

    fun authPrefs(context: Context): SharedPreferences {
        migrateFromLegacy(context)
        return getPrefs(context, AUTH_PREFS)
    }

    private fun getPrefs(context: Context, name: String): SharedPreferences {
        val masterKey = masterKeyCache.getOrPut(name) {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migrateFromLegacy(context: Context) {
        if (migrated) return
        migrated = true

        val legacyNames = mapOf(
            "app_settings" to "app_settings_encrypted",
            "auth_prefs" to "auth_prefs_encrypted"
        )

        for ((legacyName, encryptedName) in legacyNames) {
            val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
            if (legacy.all.isEmpty()) continue

            val encrypted = getPrefs(context, encryptedName)
            if (encrypted.all.isNotEmpty()) continue // Already migrated

            encrypted.edit().apply {
                legacy.all.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        else -> putString(key, value.toString())
                    }
                }
                apply()
            }
        }
    }
}
