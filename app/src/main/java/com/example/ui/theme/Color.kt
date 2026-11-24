package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage

private val KiryanaLightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = SaffronSecondary,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = TerracottaTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = Color(0xFF1E1B4B),
    background = WarmParchmentBg,
    onBackground = InkDark,
    surface = WarmSurface,
    onSurface = InkDark,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarm,
    error = UdharCrimson,
    onError = Color.White,
    errorContainer = UdharCrimsonBg,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val KiryanaDarkColors = darkColorScheme(
    primary = EmeraldLightDark,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = EmeraldContainer,
    secondary = SaffronLightDark,
    onSecondary = Color(0xFF1E3A8A),
    secondaryContainer = Color(0xFF1E40AF),
    onSecondaryContainer = SaffronContainer,
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF1E1B4B),
    background = DarkForestBg,
    onBackground = InkLightDark,
    surface = DarkForestSurface,
    onSurface = InkLightDark,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = InkMutedDark,
    outline = OutlineDark,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val KiryanaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun KiryanaTheme(
    language: AppLanguage = AppLanguage.URDU,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KiryanaDarkColors else KiryanaLightColors
    val typography = if (language == AppLanguage.URDU) UrduTypography else EnglishTypography
    val layoutDirection = if (language == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = KiryanaShapes,
            content = content
        )
    }
}
