package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.CommandUiState
import com.example.ui.components.NexaTab
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeOrbScreen(
    profile: UserProfile?,
    agents: List<CustomAgent>,
    selectedAgentId: String?,
    permissions: DevicePermissionsState,
    secrets: LocalAiSecrets,
    commandUiState: CommandUiState,
    wakeStatus: String,
    wakeRunning: Boolean,
    lastOtp: String?,
    isVoiceListening: Boolean,
    onOrbVoiceClick: () -> Unit,
    onSendCommand: (String) -> Unit,
    onSendVisionCommand: (String, ByteArray, String) -> Unit = { _, _, _ -> },
    onSelectAgent: (String?) -> Unit,
    onNavigateTab: (NexaTab) -> Unit,
    onOpenApiKeys: () -> Unit,
    isOrbMuted: Boolean = false,
    onToggleMuteOrb: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenSubRoute: (String) -> Unit = {},
    isFreeTrialActive: Boolean = false,
    freeSecondsRemaining: Int = 0
) {
    var promptText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val bossName = profile?.bossName?.ifBlank { "BOSS" } ?: "BOSS"
    val energyPoints = profile?.energyPoints ?: 4
    val moodStatus = profile?.moodStatus?.ifBlank { "Sab badhiya" } ?: "Sab badhiya"

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        in 17..20 -> "Good evening,"
        else -> "Good night,"
    }
    val dayNum = remember { SimpleDateFormat("dd", Locale.getDefault()).format(Date()) }
    val dayMonth = remember { SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(Date()) }

    val infiniteTransition = rememberInfiniteTransition(label = "zed5d")
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crot"
    )
    val activeAudio = !isOrbMuted && (isVoiceListening || wakeRunning || commandUiState is CommandUiState.Thinking)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (activeAudio) 1.13f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeAudio) 380 else 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val frostBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFEAF1FC), Color(0xFFDCE7F9), Color(0xFFE5EEFC))
    )
    val cardColor = Color(0xFFF4F8FF)
    val textDark = Color(0xFF0F172A)
    val textSub = Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(frostBg)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Greeting + Free Trial Timer (if free) + Energy Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(greeting, fontSize = 17.sp, color = textSub)
                Text(bossName.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Light, color = textDark, letterSpacing = 1.sp)
                if (isFreeTrialActive) {
                    val mins = freeSecondsRemaining / 60
                    val secs = freeSecondsRemaining % 60
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDBEAFE),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = String.format("FREE TRIAL: %02d:%02d left today", mins, secs),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("$energyPoints", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                        Text("Energy", fontSize = 11.sp, color = textSub)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5D HOLOGRAPHIC QUANTUM CORE ORB -> TAP TO MUTE / UNMUTE
        Box(
            modifier = Modifier
                .size(245.dp)
                .clickable { onToggleMuteOrb() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.28f * pulseScale
                val primaryGlow = when {
                    isOrbMuted -> Color(0xFF94A3B8)
                    activeAudio -> Color(0xFFFF2E7E)
                    else -> Color(0xFF2563EB)
                }
                val secondaryGlow = when {
                    isOrbMuted -> Color(0xFFCBD5E1)
                    activeAudio -> Color(0xFF8B5CF6)
                    else -> Color(0xFF06B6D4)
                }

                // Layer 1: Deep 5D Atmospheric Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow.copy(alpha = 0.36f), secondaryGlow.copy(alpha = 0.12f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.50f
                    ),
                    radius = size.minDimension * 0.50f,
                    center = center
                )

                // Layer 2: 64 Reactive Outer Quantum Ticks
                val tickCount = 64
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount)) + (orbRotation * 0.6f)
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val waveBoost = if (activeAudio) {
                        (sin(Math.toRadians((i * 28f + orbRotation * 5f).toDouble())).toFloat() * 11f).coerceAtLeast(0f)
                    } else 2.5f

                    val innerR = baseRadius + 14f
                    val outerR = innerR + 13f + waveBoost
                    drawLine(
                        color = if (isOrbMuted) Color(0xFF94A3B8).copy(alpha = 0.45f)
                        else if (i % 2 == 0) primaryGlow.copy(alpha = 0.85f) else secondaryGlow.copy(alpha = 0.75f),
                        start = Offset(center.x + (cos(rad) * innerR).toFloat(), center.y + (sin(rad) * innerR).toFloat()),
                        end = Offset(center.x + (cos(rad) * outerR).toFloat(), center.y + (sin(rad) * outerR).toFloat()),
                        strokeWidth = 2.6f,
                        cap = StrokeCap.Round
                    )
                }

                // Layer 3: Counter-Rotating 5D Orbital Gyro Arcs
                val gyroRadius = baseRadius + 8f
                drawArc(
                    color = secondaryGlow.copy(alpha = 0.85f),
                    startAngle = orbRotation,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - gyroRadius, center.y - gyroRadius),
                    size = Size(gyroRadius * 2f, gyroRadius * 2f),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryGlow.copy(alpha = 0.9f),
                    startAngle = counterRotation,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(center.x - (gyroRadius + 4f), center.y - (gyroRadius + 4f)),
                    size = Size((gyroRadius + 4f) * 2f, (gyroRadius + 4f) * 2f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Layer 4: Outer Energy Rim
                drawCircle(
                    color = primaryGlow,
                    radius = baseRadius + 3f,
                    center = center
                )

                // Layer 5: Deep Obsidian 5D Core Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF090D16), Color(0xFF03050A)),
                        center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                        radius = baseRadius * 1.15f
                    ),
                    radius = baseRadius,
                    center = center
                )

                // Layer 6: 3D Latitude/Longitude Perspective Rings inside Sphere
                val tiltFactor = (0.35f + 0.25f * sin(Math.toRadians(orbRotation.toDouble())).toFloat())
                drawOval(
                    color = secondaryGlow.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * tiltFactor),
                    size = Size(baseRadius * 1.7f, baseRadius * 2f * tiltFactor),
                    style = Stroke(width = 1.6f)
                )
                drawOval(
                    color = primaryGlow.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - baseRadius * tiltFactor, center.y - baseRadius * 0.85f),
                    size = Size(baseRadius * 2f * tiltFactor, baseRadius * 1.7f),
                    style = Stroke(width = 1.6f)
                )

                // Layer 7: Golden Neural Constellation Mesh
                val nodeCount = 24
                val nodes = (0 until nodeCount).map { idx ->
                    val a = Math.toRadians((idx * (360.0 / nodeCount)) + orbRotation * 1.2)
                    val r = baseRadius * (0.25f + 0.56f * ((idx % 4) / 3f))
                    Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat())
                }
                for (i in nodes.indices) {
                    val next = nodes[(i + 3) % nodes.size]
                    drawLine(
                        color = if (isOrbMuted) Color(0xFF64748B).copy(alpha = 0.3f) else Color(0xFFFBBF24).copy(alpha = 0.55f),
                        start = nodes[i],
                        end = next,
                        strokeWidth = 1.5f
                    )
                    drawCircle(
                        color = if (isOrbMuted) Color(0xFF94A3B8) else Color(0xFFFEF08A),
                        radius = 3.3f,
                        center = nodes[i]
                    )
                }

                // Layer 8: 5D Glass Specular Reflection Highlight (Top-Left Gloss)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(center.x - baseRadius * 0.38f, center.y - baseRadius * 0.38f),
                        radius = baseRadius * 0.42f
                    ),
                    radius = baseRadius * 0.42f,
                    center = Offset(center.x - baseRadius * 0.38f, center.y - baseRadius * 0.38f)
                )
            }

            if (isOrbMuted) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xDD0F172A),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Muted", tint = Color(0xFFFF4D8D), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Text(
            text = when {
                isOrbMuted -> "MUTED • Bade gole par tap karke Unmute karein"
                isVoiceListening -> "Live Voice ON • Sun raha hoon (Orb tap = Mute)"
                else -> "by The Titan AI • 5D Core (Tap Orb to Mute/Unmute)"
            },
            fontSize = 11.sp,
            color = if (isOrbMuted || isVoiceListening) Color(0xFFE11D48) else textSub,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Module Row: Browser, License, Rules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onOpenBrowser,
                shape = RoundedCornerShape(22.dp),
                color = cardColor,
                shadowElevation = 7.dp,
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browser", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textDark)
                }
            }
            Surface(
                onClick = { onOpenSubRoute("upgrade") },
                shape = RoundedCornerShape(22.dp),
                color = cardColor,
                shadowElevation = 7.dp,
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("License", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textDark)
                }
            }
            Surface(
                onClick = { onOpenSubRoute("rules") },
                shape = RoundedCornerShape(22.dp),
                color = cardColor,
                shadowElevation = 7.dp,
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rules", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Info Cards: Mausam, Aaj, Mood
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { onSendCommand("Aaj ka mausam kaisa hai?") },
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f).height(104.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = textSub, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mausam", fontSize = 11.sp, color = textSub)
                    }
                    Text("22°", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                    Text("Clear • 78%", fontSize = 11.sp, color = textSub)
                }
            }

            Surface(
                onClick = { onSendCommand("Aaj ka schedule dikhao") },
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f).height(104.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = textSub, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aaj", fontSize = 11.sp, color = textSub)
                    }
                    Text(dayNum, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                    Text(dayMonth, fontSize = 11.sp, color = textSub)
                }
            }

            Surface(
                onClick = { onSendCommand("Mera mood kaisa hai?") },
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f).height(104.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mood", fontSize = 11.sp, color = textSub)
                    }
                    Text("Pyaar b...", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(moodStatus, fontSize = 11.sp, color = textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Crisp Visible Prompt Input Bar (Always visible above keyboard + dark text color)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                TextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("ZED se kuch bhi poochho...", fontSize = 14.sp, color = textSub) },
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (promptText.isNotBlank()) {
                            onSendCommand(promptText)
                            promptText = ""
                            focusManager.clearFocus()
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        cursorColor = Color(0xFF2563EB),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        }
                )
                Surface(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onSendCommand(promptText)
                            promptText = ""
                            focusManager.clearFocus()
                        }
                    },
                    shape = CircleShape,
                    color = Color(0xFF2563EB),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(94.dp))
    }
}
