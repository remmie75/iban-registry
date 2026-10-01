package com.example.ibanregistry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibanregistry.R
import com.example.ibanregistry.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            ThemeOption(
                title = stringResource(R.string.theme_light),
                description = stringResource(R.string.theme_light_description),
                icon = Icons.Default.LightMode,
                selected = selectedTheme == AppTheme.LIGHT,
                onClick = { onThemeSelected(AppTheme.LIGHT) },
            )
            ThemeOption(
                title = stringResource(R.string.theme_dark),
                description = stringResource(R.string.theme_dark_description),
                icon = Icons.Default.DarkMode,
                selected = selectedTheme == AppTheme.DARK,
                onClick = { onThemeSelected(AppTheme.DARK) },
            )
            ThemeOption(
                title = stringResource(R.string.theme_surprise),
                description = stringResource(R.string.theme_surprise_description),
                icon = Icons.Default.AutoAwesome,
                selected = selectedTheme == AppTheme.SURPRISE,
                onClick = { onThemeSelected(AppTheme.SURPRISE) },
            )
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(title)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
        }
    }
}
