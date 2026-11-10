package com.wellcontrol.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF0D4F6B)
val Accent = Color(0xFF2A9FD6)
val Ink = Color(0xFF14212B)
val Muted = Color(0xFF64798A)
val AppBg = Color(0xFFF2F5F8)

val IosGreen = Color(0xFF34C759)
val IosGrey = Color(0xFFE5E5EA)
val GreenDark = Color(0xFF1B8A3E)
val RedOff = Color(0xFFE5484D)
val Amber = Color(0xFFE8831A)
val Slate = Color(0xFF6B7C8A)

val TintOn = Color(0xFFE3F6EA)
val TintOff = Color(0xFFE6EFF4)
val TintAmber = Color(0xFFFFF3E3)
val TintRed = Color(0xFFFDECEC)
val TintSlate = Color(0xFFE8EEF2)

@Composable
fun WellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brand,
            secondary = Accent,
            background = AppBg,
            surface = Color.White,
        ),
        content = content,
    )
}
