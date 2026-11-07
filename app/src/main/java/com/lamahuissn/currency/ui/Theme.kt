package com.lamahuissn.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The palette is taken from the screens the app is built from: pure black page, #1C1C1E surfaces,
 * one neon green (#00D632) that means "value / interactive", and grey #8E8E93 for quiet facts.
 */
object Ink {
    val Page = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigh = Color(0xFF2C2C2E)
    val Divider = Color(0xFF1A1A1C)
    val Green = Color(0xFF00D632)
    val GreenDeep = Color(0xFF0E2A16)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFF8E8E93)
    val Red = Color(0xFFFF3B30)
    val Amber = Color(0xFFFFB020)

    // The amount sheet is the one warm surface: a deep burgundy the original uses to separate
    // "enter an amount" from "read the list".
    val SheetTop = Color(0xFF4A3536)
    val SheetMid = Color(0xFF3D2B2C)
    val SheetBottom = Color(0xFF2A1F20)
}

private val scheme = darkColorScheme(
    primary = Ink.Green,
    onPrimary = Color.White,
    secondary = Ink.Green,
    background = Ink.Page,
    onBackground = Ink.Text,
    surface = Ink.Page,
    onSurface = Ink.Text,
    surfaceVariant = Ink.Surface,
    onSurfaceVariant = Ink.Muted,
    outline = Ink.Divider,
    error = Ink.Red,
)

private val typography = Typography(
    // fontFeatureSettings = "tnum": digits keep one width, so a figure that changes does not shove
    // the rest of the line sideways while it animates.
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp, fontFeatureSettings = "tnum"),
)

@Composable
fun CurrencyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        Surface(modifier = Modifier.fillMaxSize().background(Ink.Page), color = Ink.Page) {
            Box(Modifier.fillMaxSize().background(Ink.Page)) { content() }
        }
    }
}
