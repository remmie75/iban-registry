package com.example.ibanregistry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ibanregistry.R

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onPinSubmitted: (String) -> Boolean,
    onBiometricRequested: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var invalidPin by remember { mutableStateOf(false) }

    LaunchedEffect(biometricEnabled, biometricAvailable) {
        if (biometricEnabled && biometricAvailable) onBiometricRequested()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 8 && it.all(Char::isDigit)) {
                    pin = it
                    invalidPin = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = invalidPin,
            supportingText = {
                if (invalidPin) Text(stringResource(R.string.incorrect_pin))
            },
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { invalidPin = !onPinSubmitted(pin) },
            enabled = pin.length >= 4,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.unlock))
        }
        if (biometricEnabled && biometricAvailable) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBiometricRequested,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Text(
                    text = stringResource(R.string.use_biometrics),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
