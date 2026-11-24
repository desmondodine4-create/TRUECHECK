package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private val random = SecureRandom()

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return try {
            val spec = PBEKeySpec(password.toCharArray(), hexToBytes(saltHex), ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(hexToBytes(saltHex))
            md.digest(password.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHash: String): Boolean {
        val actual = hashPassword(password, saltHex)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
