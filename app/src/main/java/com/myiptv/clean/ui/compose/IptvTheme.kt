package com.myiptv.clean.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.myiptv.clean.R

// Keep in sync with @color/background for the window and system bars.
private val Background = Color(0xFF08090C)
private val Surface = Color(0xFF111113)
private val SurfaceHigh = Color(0xFF242429)
private val Sky = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF7F7F8)
private val TextSecondary = Color(0xFFA8A8AF)

private val Inter = FontFamily(
    Font(R.font.inter_light, weight = FontWeight.Light),
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_bold, weight = FontWeight.Bold),
    Font(R.font.inter_extrabold, weight = FontWeight.ExtraBold),
    Font(R.font.inter_black, weight = FontWeight.Black),
    Font(R.font.inter_black_italic, weight = FontWeight.Black, style = FontStyle.Italic)
)

private val BaseTypography = Typography()
private val InterTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = Inter),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = Inter),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = Inter),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = Inter),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = Inter),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = Inter),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = Inter),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = Inter),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = Inter),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = Inter),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = Inter),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = Inter)
)

private val CleanDarkColorScheme = darkColorScheme(
    primary = Sky,
    onPrimary = Color(0xFF001F2C),
    secondary = Color(0xFFE5F8FF),
    onSecondary = Color(0xFF003544),
    tertiary = Color(0xFF73E3C5),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF48484F),
    error = Color(0xFFFF7086),
    onError = Color(0xFF3A0713)
)

@Composable
fun CleanIPTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CleanDarkColorScheme,
        typography = InterTypography,
        content = content
    )
}
