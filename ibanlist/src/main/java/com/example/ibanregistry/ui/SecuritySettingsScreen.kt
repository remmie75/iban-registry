package com.example.ibanregistry.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ibanregistry.R
import com.example.ibanregistry.security.AppLockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    appLockManager: AppLockManager,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onChanged: () -> Unit,
) {
    var pinEnabled by remember { mutableStateOf(appLockManager.isPinEnabled) }
    var biometricEnabled by remember { mutableStateOf(appLockManager.isBiometricEnabled) }
    var pin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidPinMessage = stringResource(R.string.pin_requirements)
    val mismatchMessage = stringResource(R.string.pin_mismatch)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_title)) },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                if (pinEnabled) {
                    stringResource(R.string.startup_lock_enabled)
                } else {
                    stringResource(R.string.startup_lock_disabled)
                },
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) {
                        pin = it
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        stringResource(
                            if (pinEnabled) R.string.new_pin_label else R.string.pin_label,
                        ),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmedPin,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) {
                        confirmedPin = it
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.confirm_pin_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    error = when {
                        !AppLockManager.isValidPin(pin) -> invalidPinMessage
                        pin != confirmedPin -> mismatchMessage
                        else -> null
                    }
                    if (error == null) {
                        appLockManager.setPin(pin)
                        pinEnabled = true
                        pin = ""
                        confirmedPin = ""
                        onChanged()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (pinEnabled) R.string.change_pin else R.string.enable_startup_lock,
                    ),
                )
            }
            Spacer(Modifier.height(20.dp))
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.biometric_unlock))
                    Text(
                        stringResource(
                            if (biometricAvailable) {
                                R.string.biometric_description
                            } else {
                                R.string.biometric_unavailable
                            },
                        ),
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = {
                        appLockManager.isBiometricEnabled = it
                        biometricEnabled = it
                    },
                    enabled = pinEnabled && biometricAvailable,
                )
            }
            if (pinEnabled) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        appLockManager.disable()
                        pinEnabled = false
                        biometricEnabled = false
                        onChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.disable_startup_lock))
                }
            }
        }
    }
}
