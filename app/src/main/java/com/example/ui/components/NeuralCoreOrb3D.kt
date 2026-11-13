package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaCyanGlow
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaGoldNode
import com.example.ui.theme.NexaOrbSpace
import com.example.ui.theme.NexaRoseRadar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

private data class Vec3(val x: Float, val y: Float, val z: Float)

@Composable
fun NeuralCoreOrb3D(
  isListening: Boolean,
  isBusy: Boolean,
  accessibilityActive: Boolean,
  modifier: Modifier = Modifier,
  orbSize: Dp = 276.dp,
  onOrbTapped: () -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  val infinite = rememberInfiniteTransition(label = "nexa_orb_3d")

  // Continuous 3D yaw rotation for internal constellation mesh & rings
  val baseRotation by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isBusy) 4800 else 10500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orb_yaw",
    )

  // Continuous gentle 3D gyroscopic tilt precession across X and Y axes
  val gyroTiltX by
    infinite.animateFloat(
      initialValue = -6.5f,
      targetValue = 6.5f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isListening || isBusy) 2200 else 4600, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "gyro_tilt_x",
    )
  val gyroTiltY by
    infinite.animateFloat(
      initialValue = 8f,
      targetValue = -8f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isListening || isBusy) 2800 else 5800, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "gyro_tilt_y",
    )

  val pulse by
    infinite.animateFloat(
      initialValue = 0.96f,
      targetValue = 1.04f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isListening) 850 else 2200, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "orb_pulse",
    )
  val shockwave by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isListening || isBusy) 1600 else 3200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orb_shockwave",
    )

  // Interactive 3D drag tilt (springs back smoothly on release) + persistent internal mesh offset
  var userYawOffset by remember { mutableFloatStateOf(0f) }
  var userPitchOffset by remember { mutableFloatStateOf(0.28f) }
  var targetCardTiltX by remember { mutableFloatStateOf(0f) }
  var targetCardTiltY by remember { mutableFloatStateOf(0f) }

  val springCardTiltX by
    animateFloatAsState(
      targetValue = targetCardTiltX,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        ),
      label = "spring_tilt_x",
    )
  val springCardTiltY by
    animateFloatAsState(
      targetValue = targetCardTiltY,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        ),
      label = "spring_tilt_y",
    )

  // Material-style spring scale expansion when AI is listening or thinking
  val activeScale by
    animateFloatAsState(
      targetValue = if (isListening || isBusy) 1.06f else 1.0f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "orb_active_scale",
    )

  // Smooth color morphing across states (Idle -> Listening -> Thinking -> Accessibility Active)
  val targetAccent =
    when {
      isBusy -> NexaCyanGlow
      isListening -> NexaRoseRadar
      accessibilityActive -> NexaEmeraldOk
      else -> NexaRoseRadar
    }
  val ringAccent by
    animateColorAsState(
      targetValue = targetAccent,
      animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
      label = "orb_ring_accent",
    )

  // 360-degree 3D rotationY impulse on tap or AI activation
  val spinBurstY = remember { Animatable(0f) }
  LaunchedEffect(isListening, isBusy) {
    if (isListening || isBusy) {
      val target = spinBurstY.value + 360f
      spinBurstY.animateTo(
        targetValue = target,
        animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
      )
    }
  }

  // Precompute 42 Fibonacci 3D sphere lattice points + neural filament connections
  val spherePoints =
    remember {
      val count = 42
      val goldenAngle = PI * (3.0 - sqrt(5.0))
      (0 until count).map { i ->
        val y = 1f - (i / (count - 1f)) * 2f
        val radiusAtY = sqrt((1f - y * y).coerceAtLeast(0f))
        val theta = (goldenAngle * i).toFloat()
        Vec3(cos(theta) * radiusAtY, y, sin(theta) * radiusAtY)
      }
    }

  val edges =
    remember(spherePoints) {
      val pairs = mutableListOf<Pair<Int, Int>>()
      for (i in spherePoints.indices) {
        for (j in i + 1 until spherePoints.size) {
          val a = spherePoints[i]
          val b = spherePoints[j]
          val dx = a.x - b.x
          val dy = a.y - b.y
          val dz = a.z - b.z
          val distSq = dx * dx + dy * dy + dz * dz
          if (distSq < 0.50f) pairs.add(i to j)
        }
      }
      pairs
    }

  val hudLabels =
    remember {
      listOf(
        "LINK" to -2.15f,
        "MEM" to -0.75f,
        "NEURAL" to 0.12f,
        "SYS 01" to 1.25f,
        "VOX" to 2.45f,
        "CORE" to 3.05f,
      )
    }

  val hudTextPaint =
    remember {
      Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
      }
    }

  Box(
    modifier =
      modifier
        .size(orbSize)
        .testTag("neural_core_orb_3d")
        .semantics { contentDescription = "NEXA 3D Neural Core Orb" }
        .graphicsLayer {
          cameraDistance = 14f * density
          rotationX = gyroTiltX + springCardTiltX
          rotationY = gyroTiltY + springCardTiltY
          scaleX = activeScale
          scaleY = activeScale
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragEnd = {
              targetCardTiltX = 0f
              targetCardTiltY = 0f
            },
            onDragCancel = {
              targetCardTiltX = 0f
              targetCardTiltY = 0f
            },
          ) { change, dragAmount ->
            change.consume()
            userYawOffset += dragAmount.x * 0.01f
            userPitchOffset = (userPitchOffset + dragAmount.y * 0.01f).coerceIn(-1.25f, 1.25f)
            targetCardTiltY = (targetCardTiltY + dragAmount.x * 0.18f).coerceIn(-22f, 22f)
            targetCardTiltX = (targetCardTiltX - dragAmount.y * 0.18f).coerceIn(-22f, 22f)
          }
        }
        .pointerInput(Unit) {
          detectTapGestures {
            scope.launch {
              spinBurstY.animateTo(
                targetValue = spinBurstY.value + 360f,
                animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
              )
            }
            onOrbTapped()
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    // Layer 1: Outer atmospheric glow, shockwave, telemetry ring, HUD markers & tick ring
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val minDim = size.minDimension
      val sphereRadius = minDim * 0.275f * pulse
      val tickOuter = minDim * 0.405f
      val tickInner = minDim * 0.345f

      // Wide ambient halo
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                ringAccent.copy(alpha = if (isListening || isBusy) 0.42f else 0.28f),
                NexaRoseRadar.copy(alpha = 0.12f),
                NexaBlueCore.copy(alpha = 0.05f),
                Color.Transparent,
              ),
            center = Offset(cx, cy),
            radius = minDim * 0.50f,
          ),
        radius = minDim * 0.50f,
        center = Offset(cx, cy),
      )

      // Expanding radar shockwave ring
      val waveRadius = tickInner + (minDim * 0.49f - tickInner) * shockwave
      drawCircle(
        color = ringAccent.copy(alpha = (1f - shockwave) * if (isListening || isBusy) 0.42f else 0.18f),
        radius = waveRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2f),
      )

      // Outer telemetry orbit circle
      drawCircle(
        color = ringAccent.copy(alpha = 0.16f),
        radius = minDim * 0.455f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f),
      )

      // Dotted telemetry orbit ring
      val dotCount = 60
      for (i in 0 until dotCount) {
        val angle = (i.toFloat() / dotCount) * (2f * PI.toFloat()) - baseRotation * 0.30f
        val r = minDim * 0.455f
        val px = cx + cos(angle) * r
        val py = cy + sin(angle) * r
        drawCircle(
          color = ringAccent.copy(alpha = if (i % 5 == 0) 0.58f else 0.22f),
          radius = if (i % 5 == 0) 2.5f else 1.4f,
          center = Offset(px, py),
        )
      }

      // Faint HUD telemetry labels around outer radar ring
      hudTextPaint.textSize = minDim * 0.032f
      hudTextPaint.color = ringAccent.copy(alpha = 0.54f).toArgb()
      val labelRadius = minDim * 0.482f
      for ((label, angleOffset) in hudLabels) {
        val a = angleOffset + baseRotation * 0.08f
        val lx = cx + cos(a) * labelRadius
        val ly = cy + sin(a) * labelRadius
        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, hudTextPaint)
      }

      // Concentric segmented radar tick ring
      val tickCount = 72
      for (i in 0 until tickCount) {
        val angle = (i.toFloat() / tickCount) * (2f * PI.toFloat()) + baseRotation * 0.18f
        val isMajor = i % 6 == 0
        val startR = if (isMajor) tickInner - 4f else tickInner
        val endR = if (isMajor) tickOuter + 5f else tickOuter
        val start = Offset(cx + cos(angle) * startR, cy + sin(angle) * startR)
        val end = Offset(cx + cos(angle) * endR, cy + sin(angle) * endR)
        drawLine(
          color = ringAccent.copy(alpha = if (isMajor) 0.78f else 0.40f),
          start = start,
          end = end,
          strokeWidth = if (isMajor) 3.0f else 1.7f,
          cap = StrokeCap.Round,
        )
      }

      // 3D inclined holographic gyro orbit ring & orbiting satellite nodes
      val holoRadiusX = minDim * 0.435f
      val holoRadiusY = minDim * 0.145f
      val holoTilt = 0.28f * sin(baseRotation * 0.5f)
      val cosHolo = cos(holoTilt)
      val sinHolo = sin(holoTilt)
      val holoSteps = 60
      var prevHolo: Offset? = null
      for (h in 0..holoSteps) {
        val t = (h.toFloat() / holoSteps) * (2f * PI.toFloat())
        val hx = cos(t) * holoRadiusX
        val hy = sin(t) * holoRadiusY
        val rx = hx * cosHolo - hy * sinHolo
        val ry = hx * sinHolo + hy * cosHolo
        val pt = Offset(cx + rx, cy + ry)
        val depth = sin(t)
        if (prevHolo != null && depth >= -0.15f) {
          drawLine(
            color = NexaCyanGlow.copy(alpha = 0.22f + ((depth + 1f) * 0.5f) * 0.42f),
            start = prevHolo,
            end = pt,
            strokeWidth = 2.6f,
            cap = StrokeCap.Round,
          )
        }
        prevHolo = pt
      }
      for (sat in 0 until 4) {
        val satAngle = baseRotation * 0.65f + sat * (PI.toFloat() / 2f)
        val sx0 = cos(satAngle) * holoRadiusX
        val sy0 = sin(satAngle) * holoRadiusY
        val sx = cx + (sx0 * cosHolo - sy0 * sinHolo)
        val sy = cy + (sx0 * sinHolo + sy0 * cosHolo)
        val frontFactor = ((sin(satAngle) + 1f) * 0.5f).coerceIn(0.25f, 1f)
        drawCircle(
          color = NexaCyanGlow.copy(alpha = frontFactor * 0.35f),
          radius = 9f * frontFactor,
          center = Offset(sx, sy),
        )
        drawCircle(
          color = Color.White.copy(alpha = frontFactor),
          radius = 3.2f * frontFactor + 1.2f,
          center = Offset(sx, sy),
        )
      }

      // Crisp double outer rim around 3D core
      drawCircle(
        color = ringAccent.copy(alpha = 0.44f),
        radius = sphereRadius + 13f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.0f),
      )
      drawCircle(
        brush = Brush.sweepGradient(listOf(ringAccent, Color(0xFFFFA6B7), NexaBlueCore, ringAccent)),
        radius = sphereRadius + 6.5f,
        center = Offset(cx, cy),
        style = Stroke(width = 4.6f),
      )
    }

    // Layer 2: Inner 3D Core Sphere with parallax graphicsLayer 3D rotationY spin impulse
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            cameraDistance = 11f * density
            rotationX = (gyroTiltX + springCardTiltX) * 0.45f
            rotationY = spinBurstY.value + (gyroTiltY + springCardTiltY) * 0.45f
          }
    ) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val minDim = size.minDimension
      val sphereRadius = minDim * 0.275f * pulse

      // Dark obsidian 3D core background with inner warm-gold core glow
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0xFF281918), NexaOrbSpace, Color(0xFF04060E)),
            center = Offset(cx, cy),
            radius = sphereRadius * 1.12f,
          ),
        radius = sphereRadius + 3.5f,
        center = Offset(cx, cy),
      )

      // Warm central golden synaptic bloom inside sphere
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                NexaGoldNode.copy(alpha = if (isBusy) 0.42f else 0.28f),
                NexaRoseRadar.copy(alpha = 0.12f),
                Color.Transparent,
              ),
            center = Offset(cx, cy),
            radius = sphereRadius * 0.72f,
          ),
        radius = sphereRadius * 0.72f,
        center = Offset(cx, cy),
      )

      // Project 3D Fibonacci sphere points with dynamic harmonic pitch + yaw precession
      val yaw = baseRotation + userYawOffset
      val pitch = userPitchOffset + sin(baseRotation * 0.5f) * 0.24f
      val cosY = cos(yaw)
      val sinY = sin(yaw)
      val cosP = cos(pitch)
      val sinP = sin(pitch)

      val projected =
        spherePoints.map { p ->
          val rx = p.x * cosY - p.z * sinY
          val rz1 = p.x * sinY + p.z * cosY
          val ry = p.y * cosP - rz1 * sinP
          val rz = p.y * sinP + rz1 * cosP
          val perspective = 2.5f / (2.5f - rz * 0.80f)
          val sx = cx + rx * sphereRadius * 0.76f * perspective
          val sy = cy + ry * sphereRadius * 0.76f * perspective
          Triple(Offset(sx, sy), rz, perspective)
        }

      // 3D edges sorted back-to-front
      val sortedEdges =
        edges.sortedBy { (i, j) -> (projected[i].second + projected[j].second) * 0.5f }
      for ((i, j) in sortedEdges) {
        val (p1, z1, _) = projected[i]
        val (p2, z2, _) = projected[j]
        val avgZ = (z1 + z2) * 0.5f
        val alpha = ((avgZ + 1f) * 0.5f).coerceIn(0.16f, 0.92f)
        val lineColor =
          when {
            isBusy && avgZ < 0f -> NexaCyanGlow
            avgZ > 0.12f -> NexaGoldNode
            else -> Color(0xFFD97706)
          }
        drawLine(
          color = lineColor.copy(alpha = alpha * 0.74f),
          start = p1,
          end = p2,
          strokeWidth = 1.4f + alpha * 1.4f,
        )
      }

      // 3D tilted orbital ellipses inside core
      for (ringIdx in 0..2) {
        val tilt =
          when (ringIdx) {
            0 -> 0.50f
            1 -> -0.62f
            else -> 1.15f
          }
        val ringSteps = 44
        val ringRadius = sphereRadius * (0.86f - ringIdx * 0.08f)
        var prev: Offset? = null
        val orbitSpeed = baseRotation * (0.28f + ringIdx * 0.12f)
        for (s in 0..ringSteps) {
          val a = (s.toFloat() / ringSteps) * (2f * PI.toFloat())
          val ox = cos(a) * ringRadius
          val oy = sin(a) * ringRadius * 0.30f
          val rx = ox * cos(tilt + orbitSpeed) - oy * sin(tilt + orbitSpeed)
          val ry = ox * sin(tilt + orbitSpeed) + oy * cos(tilt + orbitSpeed)
          val curr = Offset(cx + rx, cy + ry)
          if (prev != null) {
            val orbitColor =
              when (ringIdx) {
                0 -> NexaGoldNode
                1 -> NexaRoseRadar
                else -> NexaCyanGlow
              }
            drawLine(
              color = orbitColor.copy(alpha = 0.44f),
              start = prev,
              end = curr,
              strokeWidth = 1.8f,
            )
          }
          prev = curr
        }
      }

      // 3D constellation nodes sorted back-to-front
      val sortedNodes = projected.withIndex().sortedBy { it.value.second }
      for ((idx, item) in sortedNodes) {
        val (pos, z, persp) = item
        val depth = ((z + 1f) * 0.5f).coerceIn(0.22f, 1f)
        val nodeColor =
          when {
            idx % 7 == 0 -> NexaRoseRadar
            idx % 5 == 0 && isBusy -> NexaCyanGlow
            idx % 2 == 0 -> NexaGoldNode
            else -> Color(0xFFFDE68A)
          }
        if (depth > 0.52f) {
          drawCircle(
            color = nodeColor.copy(alpha = depth * 0.34f),
            radius = 8.5f * persp,
            center = pos,
          )
        }
        drawCircle(
          color = nodeColor.copy(alpha = depth),
          radius = (2.3f + depth * 2.8f) * persp,
          center = pos,
        )
      }

      // 3D glass sphere top-left specular sheen
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.19f), Color.Transparent),
            center = Offset(cx - sphereRadius * 0.36f, cy - sphereRadius * 0.38f),
            radius = sphereRadius * 0.65f,
          ),
        radius = sphereRadius * 0.65f,
        center = Offset(cx - sphereRadius * 0.36f, cy - sphereRadius * 0.38f),
      )
    }
  }
}
