package com.wellcontrol.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Core palette: deep navy ink, water blues, soft grey page.
val Navy = Color(0xFF0B1B33)
val Ink = Navy
val Muted = Color(0xFF7A869A)
val AppBg = Color(0xFFF2F4F8)
val Sky = Color(0xFF1A6FEE)
val SkyLight = Color(0xFF4DB4FF)
val Line = Color(0xFFE5E9F0)
val Chip = Color(0xFFEDF2FA)

// Status colours.
val IosGreen = Color(0xFF34C759)
val IosGrey = Color(0xFFE5E5EA)
val GreenDark = Color(0xFF168A45)
val RedOff = Color(0xFFE5484D)
val Amber = Color(0xFFF08A1C)

val TintOn = Color(0xFFE3F6EA)
val TintAmber = Color(0xFFFFF1DE)
val TintRed = Color(0xFFFDECEC)

/** Outfit (SIL Open Font License), bundled in res/font so it works offline. */
val AppFont = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val Base = Typography()
private fun TextStyle.app(): TextStyle = copy(fontFamily = AppFont)

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.app(),
    displayMedium = Base.displayMedium.app(),
    displaySmall = Base.displaySmall.app(),
    headlineLarge = Base.headlineLarge.app(),
    headlineMedium = Base.headlineMedium.app(),
    headlineSmall = Base.headlineSmall.app(),
    titleLarge = Base.titleLarge.app(),
    titleMedium = Base.titleMedium.app(),
    titleSmall = Base.titleSmall.app(),
    bodyLarge = Base.bodyLarge.app(),
    bodyMedium = Base.bodyMedium.app(),
    bodySmall = Base.bodySmall.app(),
    labelLarge = Base.labelLarge.app(),
    labelMedium = Base.labelMedium.app(),
    labelSmall = Base.labelSmall.app(),
)

@Composable
fun WellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            secondary = Sky,
            secondaryContainer = Color(0xFFDCE9FF),
            onSecondaryContainer = Navy,
            background = AppBg,
            onBackground = Navy,
            surface = Color.White,
            onSurface = Navy,
            onSurfaceVariant = Muted,
            outline = Color(0xFFC3CBD8),
            error = RedOff,
        ),
        typography = AppTypography,
        content = content,
    )
}
