package com.securehomes.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted password hashing with PBKDF2-HMAC-SHA256.
 *
 * Every password gets its own random salt, so two users with the same password get
 * different hashes and pre-computed (rainbow table) attacks do not work. The stored value
 * is "iterations:saltHex:hashHex". Passwords are never stored in plain text.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 60_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        return "$ITERATIONS:${salt.toHex()}:${hash.toHex()}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3) return false
        return try {
            val iterations = parts[0].toInt()
            val salt = parts[1].hexToBytes()
            val expected = parts[2].hexToBytes()
            // Constant-time comparison: response time does not reveal how much matched.
            MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations))
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Invalid hex" }
        return ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
