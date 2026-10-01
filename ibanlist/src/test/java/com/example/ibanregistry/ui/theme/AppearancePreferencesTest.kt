package com.example.ibanregistry.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun storedValuesMapToThemes() {
        assertEquals(AppTheme.LIGHT, AppTheme.fromStorage("light"))
        assertEquals(AppTheme.DARK, AppTheme.fromStorage("dark"))
        assertEquals(AppTheme.SURPRISE, AppTheme.fromStorage("surprise"))
    }

    @Test
    fun missingOrUnknownValuesUseReadableLightDefault() {
        assertEquals(AppTheme.LIGHT, AppTheme.fromStorage(null))
        assertEquals(AppTheme.LIGHT, AppTheme.fromStorage("unknown"))
    }
}
