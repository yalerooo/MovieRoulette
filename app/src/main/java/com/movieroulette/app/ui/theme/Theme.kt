package com.movieroulette.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.movieroulette.app.data.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueVariant,
    onPrimaryContainer = TextPrimary,
    
    secondary = SecondaryGreen,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryGreen,
    onSecondaryContainer = TextPrimary,
    
    tertiary = AccentOrange,
    onTertiary = TextPrimary,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = AccentRed,
    onError = TextPrimary,
    
    outline = SystemGray3,
    outlineVariant = SystemGray5,
    
    scrim = Color(0x80000000)
)

private fun getColorSchemeForTheme(theme: AppTheme) = when (theme) {
    AppTheme.BLUE -> darkColorScheme(
        primary = Color(0xFFADD8E6),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF87CEEB),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFB0E0E6),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFAFEEEE),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.RED -> darkColorScheme(
        primary = Color(0xFFFFB3BA),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFFF9999),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFFFDAC1),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFFFCCCC),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.PINK -> darkColorScheme(
        primary = Color(0xFFF9CCCC),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFFFB6C1),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFFFE4E1),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFFFDAE9),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.ORANGE -> darkColorScheme(
        primary = Color(0xFFFFDAB9),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFFFCDA3),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFFFE5B4),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFFFEBCD),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.GREEN -> darkColorScheme(
        primary = Color(0xFFB4E7CE),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF98D8C8),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFC1E1C1),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFD0F0C0),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.PURPLE -> darkColorScheme(
        primary = Color(0xFFE0BBE4),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFD8BFD8),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFDDA0DD),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFE6E6FA),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
    AppTheme.BLUE -> darkColorScheme(
        primary = Color(0xFFADD8E6),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF87CEEB),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFB0E0E6),
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFAFEEEE),
        onTertiary = Color(0xFF000000),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = AccentRed,
        onError = TextPrimary,
        outline = SystemGray3,
        outlineVariant = SystemGray5,
        scrim = Color(0x80000000)
    )
}

@Composable
fun MovieRouletteTheme(
    appTheme: AppTheme = AppTheme.BLUE,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorSchemeForTheme(appTheme)
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
