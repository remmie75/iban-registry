package com.example.ibanregistry.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockManagerTest {
    @Test
    fun validPinRequiresFourToEightDigits() {
        assertTrue(AppLockManager.isValidPin("1234"))
        assertTrue(AppLockManager.isValidPin("12345678"))
        assertFalse(AppLockManager.isValidPin("123"))
        assertFalse(AppLockManager.isValidPin("123456789"))
        assertFalse(AppLockManager.isValidPin("12a4"))
    }

    @Test
    fun pinHashUsesSaltAndVerifiesDeterministically() {
        val firstSalt = ByteArray(16) { it.toByte() }
        val secondSalt = ByteArray(16) { (it + 1).toByte() }

        val expected = PinHasher.hash("1234", firstSalt)

        assertTrue(expected.contentEquals(PinHasher.hash("1234", firstSalt)))
        assertFalse(expected.contentEquals(PinHasher.hash("4321", firstSalt)))
        assertFalse(expected.contentEquals(PinHasher.hash("1234", secondSalt)))
    }
}
