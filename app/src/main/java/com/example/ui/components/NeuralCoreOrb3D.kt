package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralCoreOrb3D(
    modifier: Modifier = Modifier,
    size: Dp = 225.dp,
    orbSize: Dp? = null,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isBusy: Boolean = false,
    accessibilityActive: Boolean = false,
    accentColor: Color = Color(0xFF2563EB)
) {
    val effectiveSize = orbSize ?: size
    val infinite = rememberInfiniteTransition(label = "orb_5d")
    val spinOuter by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin_outer"
    )
    val spinInner by infinite.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing)), label = "spin_inner"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isListening || isSpeaking) 1.09f else 1.02f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_5d"
    )

    val coreColor = when {
        isListening -> Color(0xFFE11D48)
        isSpeaking -> Color(0xFF0EA5E9)
        else -> accentColor
    }

    Box(modifier = modifier.size(effectiveSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(effectiveSize)) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseR = (this.size.minDimension / 2f) * 0.78f * pulse

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(coreColor.copy(alpha = 0.34f), Color(0xFF38BDF8).copy(alpha = 0.16f), Color.Transparent),
                    center = c, radius = baseR * 1.32f
                ),
                radius = baseR * 1.32f, center = c
            )

            rotate(spinOuter, pivot = c) {
                for (i in 0 until 36) {
                    val rad = Math.toRadians((i * 10).toDouble())
                    val r1 = baseR * 1.08f
                    val r2 = if (i % 3 == 0) baseR * 1.18f else baseR * 1.13f
                    drawLine(
                        color = if (i % 3 == 0) coreColor.copy(alpha = 0.85f) else Color(0xFF60A5FA).copy(alpha = 0.45f),
                        start = Offset(c.x + (r1 * cos(rad)).toFloat(), c.y + (r1 * sin(rad)).toFloat()),
                        end = Offset(c.x + (r2 * cos(rad)).toFloat(), c.y + (r2 * sin(rad)).toFloat()),
                        strokeWidth = if (i % 3 == 0) 3.5f else 2f, cap = StrokeCap.Round
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF090E1A), Color(0xFF020617)),
                    center = Offset(c.x - baseR * 0.25f, c.y - baseR * 0.25f), radius = baseR * 1.1f
                ),
                radius = baseR * 0.88f, center = c
            )

            rotate(spinInner, pivot = c) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF38BDF8), coreColor, Color.White, Color(0xFF38BDF8))),
                    startAngle = 0f, sweepAngle = 260f, useCenter = false,
                    topLeft = Offset(c.x - baseR * 0.72f, c.y - baseR * 0.72f),
                    size = Size(baseR * 1.44f, baseR * 1.44f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
                for (k in 0 until 8) {
                    val a = Math.toRadians((k * 45).toDouble())
                    val nr = baseR * 0.52f
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = 0.85f), radius = 4.5f,
                        center = Offset(c.x + (nr * cos(a)).toFloat(), c.y + (nr * sin(a)).toFloat())
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c.x - baseR * 0.32f, c.y - baseR * 0.34f), radius = baseR * 0.42f
                ),
                radius = baseR * 0.42f,
                center = Offset(c.x - baseR * 0.32f, c.y - baseR * 0.34f)
            )
        }
    }
}
