package com.parkspot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentSoft,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSoft,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
    outlineVariant = HairlineLight,
    error = ErrorRed,
    onError = OnAccent,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentSoftDark,
    onPrimaryContainer = AccentDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentSoftDark,
    onSecondaryContainer = InkDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = PaperSoftDark,
    onSurfaceVariant = InkMutedDark,
    outline = InkMutedDark,
    outlineVariant = HairlineDark,
    error = ErrorRedDark,
    onError = OnAccentDark,
)

private val ParkSpotShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Deliberately not dynamic colour: the whole point of this palette is that exactly one thing on
 * screen is coloured, and wallpaper-derived theming would tint the neutrals too.
 */
@Composable
fun ParkSpotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ParkSpotTypography,
        shapes = ParkSpotShapes,
        content = content,
    )
}
