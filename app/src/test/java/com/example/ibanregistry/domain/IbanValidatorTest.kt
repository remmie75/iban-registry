package com.example.ibanregistry.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IbanValidatorTest {
    @Test
    fun acceptsValidIbansFromMultipleCountries() {
        assertTrue(IbanValidator.isValid("GB82 WEST 1234 5698 7654 32"))
        assertTrue(IbanValidator.isValid("nl91 abna 0417 1643 00"))
        assertTrue(IbanValidator.isValid("DE89 3704 0044 0532 0130 00"))
    }

    @Test
    fun rejectsInvalidChecksum() {
        assertFalse(IbanValidator.isValid("GB82 WEST 1234 5698 7654 33"))
    }

    @Test
    fun rejectsUnknownCountryAndWrongLength() {
        assertFalse(IbanValidator.isValid("ZZ82WEST12345698765432"))
        assertFalse(IbanValidator.isValid("NL91ABNA041716430"))
    }

    @Test
    fun rejectsInvalidCharacters() {
        assertFalse(IbanValidator.isValid("GB82-WEST-1234-5698-7654-32"))
    }

    @Test
    fun normalizesAndFormatsInput() {
        assertEquals(
            "NL91ABNA0417164300",
            IbanValidator.normalize(" nl91 abna 0417 1643 00 "),
        )
        assertEquals(
            "NL91 ABNA 0417 1643 00",
            IbanValidator.format("nl91abna0417164300"),
        )
    }
}
