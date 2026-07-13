package com.app.muzzutech.auth

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 60000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
        val hash = hashPassword(password, salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        return "$saltHex:$hashHex"
    }

    fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val expectedHash = parts[1].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val actualHash = hashPassword(password, salt)
        return expectedHash.contentEquals(actualHash)
    }

    fun generateRandomPassword(length: Int = 8): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        return (1..length).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }

    /**
     * Cryptographically-secure random password for worker logins.
     * Uses a single [SecureRandom] and GUARANTEES at least one uppercase,
     * one lowercase, and one digit. Alphanumeric only (no symbols) so it is
     * easy to read aloud and type on a worker's phone. Ambiguous glyphs
     * (I, O, l, 0, 1) are excluded to avoid transcription errors.
     */
    fun generateStrongPassword(length: Int = 10): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"   // no I, O
        val lower = "abcdefghijkmnpqrstuvwxyz"   // no l, o
        val digits = "23456789"                  // no 0, 1
        val all = upper + lower + digits
        val rnd = SecureRandom()
        val target = maxOf(length, 4)
        val out = ArrayList<Char>(target)
        // Guarantee one of each required class first.
        out.add(upper[rnd.nextInt(upper.length)])
        out.add(lower[rnd.nextInt(lower.length)])
        out.add(digits[rnd.nextInt(digits.length)])
        // Fill the remainder from the full alphanumeric set.
        while (out.size < target) out.add(all[rnd.nextInt(all.length)])
        // Fisher-Yates shuffle (SecureRandom) so guaranteed chars aren't positional.
        for (i in out.indices.reversed()) {
            val j = rnd.nextInt(i + 1)
            val tmp = out[i]; out[i] = out[j]; out[j] = tmp
        }
        return out.joinToString("")
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }
}
