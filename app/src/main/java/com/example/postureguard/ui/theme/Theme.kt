package com.example.postureguard.ui.theme

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

// PostureGuard Brand Colors
val PgGreen = Color(0xFF4CAF50)
val PgGreenDark = Color(0xFF388E3C)
val PgGreenLight = Color(0xFF81C784)
val PgRed = Color(0xFFEF5350)
val PgRedDark = Color(0xFFC62828)
val PgBlue = Color(0xFF42A5F5)
val PgBlueDark = Color(0xFF1565C0)
val PgOrange = Color(0xFFFF9800)
val PgGray = Color(0xFF9E9E9E)

// Surface colors for the camera-overlay UI
val SurfaceDark = Color(0xFF121212)
val SurfaceCard = Color(0x99000000)
val SurfaceCardLight = Color(0x66000000)
val TextPrimary = Color.White
val TextSecondary = Color.White.copy(alpha = 0.7f)
val TextMuted = Color.White.copy(alpha = 0.5f)

private val DarkColorScheme = darkColorScheme(
    primary = PgGreen,
    onPrimary = Color.White,
    secondary = PgBlue,
    onSecondary = Color.White,
    error = PgRed,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = PgGreenDark,
    onPrimary = Color.White,
    secondary = PgBlueDark,
    onSecondary = Color.White,
    error = PgRedDark,
)

@Composable
fun PostureGuardTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
