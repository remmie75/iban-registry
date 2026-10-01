package com.example.ibanregistry.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockViewModel(
    private val appLockManager: AppLockManager,
) : ViewModel() {
    private val _isUnlocked = MutableStateFlow(!appLockManager.isPinEnabled)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlockWithPin(pin: String): Boolean {
        val valid = appLockManager.verifyPin(pin)
        if (valid) _isUnlocked.value = true
        return valid
    }

    fun unlockWithBiometric() {
        _isUnlocked.value = true
    }

    fun lock() {
        if (appLockManager.isPinEnabled) _isUnlocked.value = false
    }

    fun refreshAfterSettingsChange() {
        if (!appLockManager.isPinEnabled) _isUnlocked.value = true
    }

    companion object {
        fun factory(appLockManager: AppLockManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = AppLockViewModel(appLockManager) as T
            }
    }
}
