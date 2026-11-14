package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Clay5DBaseTop = Color(0xFFEAF2FF)
val Clay5DBaseMid = Color(0xFFDCE8FC)
val Clay5DBaseBot = Color(0xFFE6EFFE)
val Clay5DCardTop = Color(0xFFFAFCFF)
val Clay5DCardBot = Color(0xFFE8F0FE)
val Clay5DAccentBlue = Color(0xFF2563EB)
val Clay5DHoloCyan = Color(0xFF38BDF8)

@Composable
fun Clay5DSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    elevation: Dp = 14.dp,
    glowColor: Color = Clay5DHoloCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "clay5d_scale"
    )
    val tiltX by animateFloatAsState(
        targetValue = if (isPressed) 3.5f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f),
        label = "clay5d_tilt"
    )

    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            glowColor.copy(alpha = 0.32f),
            Color(0xFF93C5FD).copy(alpha = 0.45f)
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 600f)
    )

    val cardModifier = modifier
        .scale(scale)
        .graphicsLayer {
            rotationX = tiltX
            cameraDistance = 12f * density
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        shadowElevation = elevation,
        border = BorderStroke(1.3.dp, rimBrush),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Clay5DCardTop, Clay5DCardBot)))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun Clay5DOrbButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "orb5d_scale"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 12.dp,
        border = BorderStroke(1.4.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF93C5FD)))),
        modifier = modifier.size(size).scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFFE6EFFD)), radius = 90f)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
