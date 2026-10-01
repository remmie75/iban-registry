package com.example.ibanregistry.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppLockManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val isPinEnabled: Boolean
        get() = preferences.contains(KEY_PIN_HASH) && preferences.contains(KEY_PIN_SALT)

    var isBiometricEnabled: Boolean
        get() = isPinEnabled && preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) {
            check(!value || isPinEnabled) { "A PIN is required before enabling biometrics" }
            preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()
        }

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must contain 4 to 8 digits" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = PinHasher.hash(pin, salt)
        preferences.edit()
            .putString(KEY_PIN_SALT, salt.encode())
            .putString(KEY_PIN_HASH, hash.encode())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = preferences.getBytes(KEY_PIN_SALT) ?: return false
        val expectedHash = preferences.getBytes(KEY_PIN_HASH) ?: return false
        return MessageDigest.isEqual(expectedHash, PinHasher.hash(pin, salt))
    }

    fun disable() {
        preferences.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "app_lock"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val SALT_BYTES = 16

        fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all(Char::isDigit)
    }
}

internal object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val specification = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded
        } finally {
            specification.clearPassword()
        }
    }
}

private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun SharedPreferences.getBytes(key: String): ByteArray? =
    getString(key, null)?.let {
        try {
            Base64.decode(it, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
