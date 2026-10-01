package com.example.ibanregistry.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF4D5D92),
    onSecondary = Color.White,
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF1A1B20),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF00458F),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBBC6E4),
    onSecondary = Color(0xFF25304D),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
)

private val SurpriseColors = lightColorScheme(
    primary = Color(0xFF5A189A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0DBFF),
    onPrimaryContainer = Color(0xFF250047),
    secondary = Color(0xFF006D77),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9CF1F7),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF9C2F00),
    onTertiary = Color.White,
    background = Color(0xFFFFF8E7),
    onBackground = Color(0xFF251A2E),
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF251A2E),
    surfaceVariant = Color(0xFFEEDFF2),
    onSurfaceVariant = Color(0xFF4D4352),
)

@Composable
fun IbanRegistryTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.SURPRISE -> SurpriseColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
