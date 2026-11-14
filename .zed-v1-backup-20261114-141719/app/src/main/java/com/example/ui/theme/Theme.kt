package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = NexaInkPrimary,
    secondary = NexaRoseRadar,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF881337),
    tertiary = NexaCyanGlow,
    onTertiary = NexaOrbSpace,
    background = NexaFrostBgMid,
    onBackground = NexaInkPrimary,
    surface = NexaSurfaceWhite,
    onSurface = NexaInkPrimary,
    surfaceVariant = Color(0xFFE7EFFC),
    onSurfaceVariant = NexaInkSecondary,
    outline = Color(0xFFC7D7F2),
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
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
  )

@Composable
fun NexaAiTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) NexaDarkScheme else NexaLightScheme
  MaterialTheme(
    colorScheme = colors,
    typography = NexaTypography,
    shapes = NexaShapes,
    content = content,
  )
}
