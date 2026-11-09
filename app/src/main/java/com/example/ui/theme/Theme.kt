package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AthletixDarkColorScheme = darkColorScheme(
    primary = AthletixPrimaryAccent,
    onPrimary = AthletixPrimaryBg,
    primaryContainer = Color(0xFF253410),
    onPrimaryContainer = AthletixPrimaryAccent,
    secondary = AthletixSecondaryAccent,
    onSecondary = AthletixPrimaryBg,
    secondaryContainer = AthletixSecondaryBg,
    onSecondaryContainer = AthletixPrimaryText,
    tertiary = AthletixWarning,
    onTertiary = AthletixPrimaryBg,
    background = AthletixPrimaryBg,
    onBackground = AthletixPrimaryText,
    surface = AthletixSecondaryBg,
    onSurface = AthletixPrimaryText,
    surfaceVariant = AthletixCardBg,
    onSurfaceVariant = AthletixSecondaryText,
    outline = AthletixBorder,
    error = AthletixDanger,
    onError = Color.White
)

private val AthletixLightColorScheme = lightColorScheme(
    primary = AthletixLightPrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF5A5),
    onPrimaryContainer = Color(0xFF1E3500),
    secondary = AthletixLightSecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = AthletixLightSecondaryBg,
    onSecondaryContainer = AthletixLightPrimaryText,
    tertiary = AthletixWarning,
    onTertiary = Color.Black,
    background = AthletixLightPrimaryBg,
    onBackground = AthletixLightPrimaryText,
    surface = AthletixLightSecondaryBg,
    onSurface = AthletixLightPrimaryText,
    surfaceVariant = AthletixLightCardBg,
    onSurfaceVariant = AthletixLightSecondaryText,
    outline = AthletixLightBorder,
    error = AthletixDanger,
    onError = Color.White
)

val AthletixShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AthletixTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AthletixDarkColorScheme else AthletixLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AthletixShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AthletixTheme(darkTheme = darkTheme, content = content)
}
