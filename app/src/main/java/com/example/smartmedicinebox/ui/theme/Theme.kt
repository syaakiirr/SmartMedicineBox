package com.example.smartmedicinebox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MedicalTealDark,
    onPrimary = OnMedicalTealDark,
    primaryContainer = MedicalTealContainerDark,
    onPrimaryContainer = OnMedicalTealContainerDark,
    secondary = CalmBlueDark,
    onSecondary = OnCalmBlueDark,
    secondaryContainer = CalmBlueContainerDark,
    onSecondaryContainer = OnCalmBlueContainerDark,
    tertiary = HealthyGreenDark,
    onTertiary = OnHealthyGreenDark,
    tertiaryContainer = HealthyGreenContainerDark,
    onTertiaryContainer = OnHealthyGreenContainerDark,
    background = AppBackgroundDark,
    onBackground = InkDark,
    surface = AppSurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = Color(0xFF09100F),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635),
    surfaceTint = MedicalTealDark,
    outline = OutlineSoftDark,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalTeal,
    onPrimary = OnMedicalTeal,
    primaryContainer = MedicalTealContainer,
    onPrimaryContainer = OnMedicalTealContainer,
    secondary = CalmBlue,
    onSecondary = OnCalmBlue,
    secondaryContainer = CalmBlueContainer,
    onSecondaryContainer = OnCalmBlueContainer,
    tertiary = HealthyGreen,
    onTertiary = OnHealthyGreen,
    tertiaryContainer = HealthyGreenContainer,
    onTertiaryContainer = OnHealthyGreenContainer,
    background = AppBackground,
    onBackground = Ink,
    surface = AppSurface,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F4),
    surfaceContainer = Color(0xFFE9EFEE),
    surfaceContainerHigh = Color(0xFFE3EAE8),
    surfaceContainerHighest = Color(0xFFDDE5E4),
    surfaceTint = MedicalTeal,
    outline = OutlineSoft,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun SmartMedicineBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
