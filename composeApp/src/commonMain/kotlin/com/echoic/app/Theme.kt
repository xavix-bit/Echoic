package com.echoic.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Light theme ──────────────────────────────────────────────────
private val LightBackground = Color(0xFFFAFAF9)
private val LightForeground = Color(0xFF1C1917)
private val LightCard = Color(0xFFFFFFFF)
private val LightMuted = Color(0xFFF0EFEC)
private val LightMutedFg = Color(0xFF78716C)
private val LightPrimary = Color(0xFF1C1917)
private val LightPrimaryFg = Color(0xFFFAFAF9)
private val LightSecondary = Color(0xFFF5F5F4)
private val LightSecondaryFg = Color(0xFF1C1917)
private val LightAccent = Color(0xFFF5F5F4)
private val LightAccentFg = Color(0xFF1C1917)
private val LightBorder = Color(0xFFE7E5E4)
private val LightInput = Color(0xFFE7E5E4)
private val LightRing = Color(0xFFA8A29E)
private val LightDestructive = Color(0xFFDC2626)

// ─── Dark theme ───────────────────────────────────────────────────
private val DarkBackground = Color(0xFF0C0A09)
private val DarkForeground = Color(0xFFFAFAF9)
private val DarkCard = Color(0xFF171412)
private val DarkMuted = Color(0xFF24201D)
private val DarkMutedFg = Color(0xFFA8A29E)
private val DarkPrimary = Color(0xFFE7E5E4)
private val DarkPrimaryFg = Color(0xFF1C1917)
private val DarkSecondary = Color(0xFF292524)
private val DarkSecondaryFg = Color(0xFFFAFAF9)
private val DarkAccent = Color(0xFF292524)
private val DarkAccentFg = Color(0xFFFAFAF9)
private val DarkBorder = Color(0x1AFFFFFF)
private val DarkInput = Color(0x26FFFFFF)
private val DarkRing = Color(0xFF78716C)
private val DarkDestructive = Color(0xFFEF4444)

// ─── Color schemes ────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedFg,
    primary = LightPrimary,
    onPrimary = LightPrimaryFg,
    secondary = LightSecondary,
    onSecondary = LightSecondaryFg,
    tertiary = LightAccent,
    onTertiary = LightAccentFg,
    outline = LightBorder,
    outlineVariant = LightInput,
    error = LightDestructive,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedFg,
    primary = DarkPrimary,
    onPrimary = DarkPrimaryFg,
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryFg,
    tertiary = DarkAccent,
    onTertiary = DarkAccentFg,
    outline = DarkBorder,
    outlineVariant = DarkInput,
    error = DarkDestructive,
    onError = Color.White,
)

// ─── Typography ───────────────────────────────────────────────────
private val EchoicTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun EchoicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EchoicTypography,
        content = content,
    )
}
