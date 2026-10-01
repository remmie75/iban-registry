package com.example.ibanregistry.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(val storageValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SURPRISE("surprise");

    companion object {
        fun fromStorage(value: String?): AppTheme =
            entries.firstOrNull { it.storageValue == value } ?: LIGHT
    }
}

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(
        AppTheme.fromStorage(preferences.getString(KEY_THEME, null)),
    )

    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.storageValue).apply()
        _theme.value = theme
    }

    companion object {
        private const val PREFERENCES_NAME = "appearance"
        private const val KEY_THEME = "theme"
    }
}
