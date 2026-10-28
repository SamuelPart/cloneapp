package com.myiptv.clean.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF090D14)
private val Surface = Color(0xFF111824)
private val SurfaceHigh = Color(0xFF1B2534)
private val Lavender = Color(0xFFB39AFF)
private val Mint = Color(0xFF6DE3CF)
private val TextPrimary = Color(0xFFF4F5FA)
private val TextSecondary = Color(0xFFA0A9B8)

private val CleanDarkColorScheme = darkColorScheme(
    primary = Lavender,
    onPrimary = Color(0xFF1C1238),
    secondary = Mint,
    onSecondary = Color(0xFF062B25),
    tertiary = Color(0xFFFFCA85),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF344052),
    error = Color(0xFFFF7086),
    onError = Color(0xFF3A0713)
)

@Composable
fun CleanIPTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CleanDarkColorScheme,
        content = content
    )
}
