package com.example.dindoripranityadnyiki.core.design

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SacredCopperLight,
    onPrimary = ObsidianBase,
    secondary = DivineTeal,
    background = ObsidianBase,
    surface = ObsidianSurface,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSubtle,
    outline = Color(0xFF2D2D33)
)

private val LightColorScheme = lightColorScheme(
    primary = SacredCopper,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3F4F6),
    onPrimaryContainer = SacredCopper,
    secondary = DivineTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF7EF),
    onSecondaryContainer = Color(0xFF14532D),
    background = PorcelainBase,
    surface = PorcelainSurface,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = TextSecondaryLight,
    outline = PorcelainLine,
    outlineVariant = Color(0xFFF1F5F9),
    error = DivineError
)

@Composable
@Suppress("DEPRECATION")
fun DindoriPranitYadnyikiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DivineTypography, // Defined in Type.kt
        shapes = DivineShapes,
        content = content
    )
}
