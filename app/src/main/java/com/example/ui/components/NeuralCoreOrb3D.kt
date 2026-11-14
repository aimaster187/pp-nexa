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

/**
 * ZED V1 animated neural core.
 * A layered Compose-rendered visual (not a true 3D model); uses highlights,
 * elliptical energy bands, metallic gradients and state-aware animation.
 */
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
    val infinite = rememberInfiniteTransition(label = "zed_neural_v1")
    val spinOuter by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "outer"
    )
    val spinInner by infinite.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "inner"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = if (isListening || isSpeaking || isBusy) 1.045f else 1.015f,
        animationSpec = infiniteRepeatable(
            tween(if (isListening || isSpeaking || isBusy) 650 else 1900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val core = when {
        isListening -> Color(0xFF0EA5E9)
        isSpeaking -> Color(0xFF2563EB)
        isBusy -> Color(0xFF7657E8)
        else -> accentColor
    }

    Box(modifier = modifier.size(effectiveSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(effectiveSize)) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension * 0.34f * pulse

            // Soft blue aura on the white canvas.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(core.copy(alpha = 0.20f), Color(0xFF93C5FD).copy(alpha = 0.10f), Color.Transparent),
                    center = c, radius = r * 2.05f
                ),
                radius = r * 2.05f, center = c
            )

            // Outer metallic orbit with a subtle silver under-ring.
            drawCircle(
                color = Color(0xFFB8C4D6).copy(alpha = 0.62f),
                radius = r * 1.68f, center = c,
                style = Stroke(width = r * 0.075f)
            )
            rotate(spinOuter, pivot = c) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFDBEAFE), core, Color.White, Color(0xFF93C5FD), core)
                    ),
                    startAngle = 205f, sweepAngle = 285f, useCenter = false,
                    topLeft = Offset(c.x - r * 1.68f, c.y - r * 1.68f),
                    size = Size(r * 3.36f, r * 3.36f),
                    style = Stroke(width = r * 0.085f, cap = StrokeCap.Round)
                )
                for (i in 0 until 48) {
                    val a = Math.toRadians(i * 7.5)
                    val inner = r * 1.79f
                    val outer = if (i % 4 == 0) r * 1.98f else r * 1.88f
                    drawLine(
                        color = if (i % 4 == 0) core.copy(alpha = 0.9f) else Color(0xFF94A3B8).copy(alpha = 0.45f),
                        start = Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()),
                        end = Offset(c.x + (outer * cos(a)).toFloat(), c.y + (outer * sin(a)).toFloat()),
                        strokeWidth = if (i % 4 == 0) 2.6f else 1.4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Inner glass sphere: silver-white, deliberately not a black hole.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFF4F8FF), Color(0xFFDCE8F8), Color(0xFFB9D0F2)),
                    center = Offset(c.x - r * 0.38f, c.y - r * 0.42f),
                    radius = r * 1.75f
                ),
                radius = r * 1.30f, center = c
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = r * 1.17f, center = c,
                style = Stroke(width = r * 0.035f)
            )

            // Rotating inner energy bands.
            rotate(spinInner, pivot = c) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(core, Color.White, Color(0xFF60A5FA), core)),
                    startAngle = 15f, sweepAngle = 300f, useCenter = false,
                    topLeft = Offset(c.x - r * 1.02f, c.y - r * 0.72f),
                    size = Size(r * 2.04f, r * 1.44f),
                    style = Stroke(width = r * 0.10f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF94A3B8).copy(alpha = 0.75f),
                    startAngle = 195f, sweepAngle = 145f, useCenter = false,
                    topLeft = Offset(c.x - r * 0.80f, c.y - r * 1.03f),
                    size = Size(r * 1.60f, r * 2.06f),
                    style = Stroke(width = r * 0.045f, cap = StrokeCap.Round)
                )
            }

            // Central neural nucleus and orbiting data nodes.
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, core, Color(0xFF1D4ED8))),
                radius = r * 0.28f, center = c
            )
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0 + spinOuter * 0.35)
                val nr = r * 0.78f
                val p = Offset(c.x + (nr * cos(a)).toFloat(), c.y + (nr * sin(a)).toFloat())
                drawCircle(Color.White.copy(alpha = 0.95f), radius = r * 0.075f, center = p)
                drawCircle(core.copy(alpha = 0.85f), radius = r * 0.045f, center = p)
            }

            // Specular highlight.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.0f)),
                    center = Offset(c.x - r * 0.50f, c.y - r * 0.58f),
                    radius = r * 0.60f
                ),
                radius = r * 0.60f,
                center = Offset(c.x - r * 0.50f, c.y - r * 0.58f)
            )
        }
    }
}
