package com.app.muzzutech.utils.crpto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object DatabasePassphraseProvider {

    private const val PREFS_NAME = "db_crypto_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase_hex"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = getSecurePrefs(context)
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) {
            return hexStringToByteArray(stored)
        }
        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)
        prefs.edit().putString(KEY_PASSPHRASE, byteArrayToHexString(passphrase)).apply()
        return passphrase
    }

    private fun getSecurePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun byteArrayToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}
