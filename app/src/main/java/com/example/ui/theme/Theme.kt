package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NexaLightScheme =
  lightColorScheme(
    primary = NexaBlueCore,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF1FF),
    onPrimaryContainer = Color(0xFF173B83),
    secondary = NexaVioletAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EDFF),
    onSecondaryContainer = Color(0xFF382477),
    tertiary = NexaCyanGlow,
    onTertiary = Color(0xFF06243A),
    background = Color(0xFFFFFFFF),
    onBackground = NexaInkPrimary,
    surface = Color(0xFFFFFFFF),
    onSurface = NexaInkPrimary,
    surfaceVariant = Color(0xFFF5F7FB),
    onSurfaceVariant = NexaInkSecondary,
    outline = Color(0xFFDCE3ED),
    outlineVariant = Color(0xFFEDF1F6),
  )

private val NexaDarkScheme =
  darkColorScheme(
    primary = NexaCyanGlow,
    onPrimary = NexaOrbSpace,
    primaryContainer = Color(0xFF172D54),
    onPrimaryContainer = NexaDarkInk,
    secondary = NexaRoseRadar,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D30),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = NexaVioletAccent,
    onTertiary = Color.White,
    background = NexaDarkBgTop,
    onBackground = NexaDarkInk,
    surface = NexaDarkSurface,
    onSurface = NexaDarkInk,
    surfaceVariant = NexaDarkSurfaceVariant,
    onSurfaceVariant = NexaDarkInkMuted,
    outline = Color(0xFF2E3F6B),
  )

val NexaShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun NexaAiTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) NexaDarkScheme else NexaLightScheme,
    typography = NexaTypography,
    shapes = NexaShapes,
    content = content,
  )
}
