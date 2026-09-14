package com.parkspot.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = ParkGreen,
    onPrimary = Color.White,
    primaryContainer = ParkGreenLight,
    onPrimaryContainer = ParkGreenDark,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = Color(0xFF2A1800),
    background = SurfaceLight,
    surface = SurfaceLight,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = ParkGreenLight,
    onPrimary = ParkGreenDark,
    primaryContainer = ParkGreen,
    onPrimaryContainer = Color.White,
    secondary = AmberLight,
    onSecondary = Color(0xFF2A1800),
    secondaryContainer = Color(0xFF6B4300),
    onSecondaryContainer = AmberLight,
    background = SurfaceDark,
    surface = SurfaceDark,
)

@Composable
fun ParkSpotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ParkSpotTypography,
        content = content,
    )
}
