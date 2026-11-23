package com.costemp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** The single source of truth for the Costemp dark workspace palette. */
object CostempPalette {
    val Workspace = Color(0xFF08080A)
    val Panel = Color(0xFF101014)
    val PanelRaised = Color(0xFF17171C)
    val ViewportTop = Color(0xFF0D0D12)
    val ViewportBottom = Color(0xFF070709)
    val Outline = Color(0xFF26262E)
    val OutlineStrong = Color(0xFF35353F)
    val TextPrimary = Color(0xFFE8E8EE)
    val TextSecondary = Color(0xFF8B8B99)
    val Accent = Color(0xFF9B6CFF)
    val AccentSoft = Color(0x339B6CFF)
    val Teal = Color(0xFF35D6C0)
    val Warning = Color(0xFFFF7A59)
    val Grid = Color(0xFF2A2A33)
}

private val CostempColors = darkColorScheme(
    primary = CostempPalette.Accent,
    onPrimary = Color.Black,
    secondary = CostempPalette.Teal,
    onSecondary = Color.Black,
    background = CostempPalette.Workspace,
    onBackground = CostempPalette.TextPrimary,
    surface = CostempPalette.Panel,
    onSurface = CostempPalette.TextPrimary,
    surfaceVariant = CostempPalette.PanelRaised,
    onSurfaceVariant = CostempPalette.TextSecondary,
    outline = CostempPalette.Outline,
    error = CostempPalette.Warning,
)

private val CostempTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
    ),
)

@Composable
fun CostempTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CostempColors,
        typography = CostempTypography,
        content = content,
    )
}
