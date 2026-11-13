package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.NexaCommandUiState
import com.example.ui.components.NexaTab
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
    secrets: LocalSecrets,
    commandUiState: NexaCommandUiState,
    wakeStatus: String,
    wakeRunning: Boolean,
    lastOtp: String?,
    isVoiceListening: Boolean,
    onOrbVoiceClick: () -> Unit,
    onSendCommand: (String) -> Unit,
    onSendVisionCommand: (String, ByteArray, String) -> Unit,
    onSelectAgent: (String) -> Unit,
    onNavigateTab: (NexaTab) -> Unit,
    onOpenApiKeys: () -> Unit,
    isOrbMuted: Boolean = false,
    onToggleMuteOrb: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenSubRoute: (String) -> Unit = {}
) {
    var promptText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
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

    val infiniteTransition = rememberInfiniteTransition(label = "zed4d")
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val activeAudio = !isOrbMuted && (isVoiceListening || wakeRunning || commandUiState.isProcessing)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (activeAudio) 1.14f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeAudio) 400 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val frostBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFEAF1FC), Color(0xFFDCE7F9), Color(0xFFE5EEFC))
    )
    val cardColor = Color(0xFFF4F8FF)
    val textDark = Color(0xFF1E293B)
    val textSub = Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(frostBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(greeting, fontSize = 18.sp, color = textSub)
                Text(bossName.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Light, color = textDark, letterSpacing = 1.sp)
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

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .clickable { onToggleMuteOrb() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.30f * pulseScale
                val activeGlow = when {
                    isOrbMuted -> Color(0xFF94A3B8)
                    activeAudio -> Color(0xFFFF3B7F)
                    else -> Color(0xFF3B82F6)
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeGlow.copy(alpha = 0.32f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.49f
                    ),
                    radius = size.minDimension * 0.49f,
                    center = center
                )

                val tickCount = 64
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount)) + (orbRotation * 0.5f)
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val waveBoost = if (activeAudio) {
                        (sin(Math.toRadians((i * 25f + orbRotation * 4f).toDouble())).toFloat() * 10f).coerceAtLeast(0f)
                    } else 2f

                    val innerR = baseRadius + 10f
                    val outerR = innerR + 14f + waveBoost
                    val start = Offset(
                        center.x + (cos(rad) * innerR).toFloat(),
                        center.y + (sin(rad) * innerR).toFloat()
                    )
                    val end = Offset(
                        center.x + (cos(rad) * outerR).toFloat(),
                        center.y + (sin(rad) * outerR).toFloat()
                    )
                    drawLine(
                        color = when {
                            isOrbMuted -> Color(0xFF94A3B8).copy(alpha = 0.5f)
                            activeAudio -> Color(0xFFFF4D8D)
                            else -> Color(0xFF60A5FA).copy(alpha = 0.75f)
                        },
                        start = start,
                        end = end,
                        strokeWidth = 2.6f,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = when {
                        isOrbMuted -> Color(0xFF64748B)
                        activeAudio -> Color(0xFFFF4D8D)
                        else -> Color(0xFF38BDF8)
                    },
                    radius = baseRadius + 4f,
                    center = center
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1A120B), Color(0xFF07090F)),
                        center = center,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )

                val nodeCount = 22
                val nodes = (0 until nodeCount).map { idx ->
                    val a = Math.toRadians((idx * (360.0 / nodeCount)) + orbRotation)
                    val r = baseRadius * (0.28f + 0.52f * ((idx % 4) / 3f))
                    Offset(
                        center.x + (cos(a) * r).toFloat(),
                        center.y + (sin(a) * r).toFloat()
                    )
                }
                for (i in nodes.indices) {
                    val next = nodes[(i + 3) % nodes.size]
                    drawLine(
                        color = if (isOrbMuted) Color(0xFF64748B).copy(alpha = 0.35f) else Color(0xFFFBBF24).copy(alpha = 0.45f),
                        start = nodes[i],
                        end = next,
                        strokeWidth = 1.5f
                    )
                    drawCircle(
                        color = if (isOrbMuted) Color(0xFF94A3B8) else Color(0xFFFDE68A),
                        radius = 3.2f,
                        center = nodes[i]
                    )
                }
            }

            if (isOrbMuted) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC0F172A),
                    modifier = Modifier.size(44.dp)
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
                else -> "by The Titan AI • Tap Orb to Mute/Unmute"
            },
            fontSize = 11.sp,
            color = if (isOrbMuted || isVoiceListening) Color(0xFFE11D48) else textSub,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
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
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rules", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { onSendCommand("Aaj ka mausam kaisa hai?") },
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f).height(108.dp)
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
                modifier = Modifier.weight(1f).height(108.dp)
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
                modifier = Modifier.weight(1f).height(108.dp)
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

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cardColor,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = textSub, modifier = Modifier.size(20.dp))
                TextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("ZED se kuch bhi poochho...", fontSize = 14.sp, color = textSub) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onSendCommand(promptText)
                            promptText = ""
                        }
                    },
                    shape = CircleShape,
                    color = Color(0xFFE8EFFA),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = textDark, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(86.dp))
    }
}
