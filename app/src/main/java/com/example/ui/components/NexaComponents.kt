package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaCyanGlow
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar

@Composable
fun NexaTechAtmosphereBackground(modifier: Modifier = Modifier) {
  val isDark = isSystemInDarkTheme()
  val lineColor =
    if (isDark) Color(0xFF60A5FA).copy(alpha = 0.09f)
    else Color(0xFF7FA3DA).copy(alpha = 0.17f)
  val glowColor =
    if (isDark) NexaCyanGlow.copy(alpha = 0.08f)
    else Color.White.copy(alpha = 0.48f)

  val infinite = rememberInfiniteTransition(label = "nexa_3d_bg")
  val gridPhase by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 5200, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "bg_grid_phase",
    )

  Canvas(modifier = modifier.fillMaxSize()) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.36f
    val maxR = size.minDimension

    // Soft top-center ambient halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(glowColor, Color.Transparent),
          center = Offset(cx, cy),
          radius = maxR * 0.72f,
        ),
      radius = maxR * 0.72f,
      center = Offset(cx, cy),
    )

    // Concentric atmospheric HUD radar arcs behind the 3D orb
    drawCircle(
      color = lineColor,
      radius = maxR * 0.44f,
      center = Offset(cx, cy),
      style = Stroke(width = 1.4f),
    )
    drawCircle(
      color = lineColor.copy(alpha = lineColor.alpha * 0.75f),
      radius = maxR * 0.62f,
      center = Offset(cx, cy),
      style = Stroke(width = 1.1f),
    )

    // 3D perspective horizon cybernetic floor grid near bottom
    val horizonY = size.height * 0.58f
    val vanishX = cx + (gridPhase - 0.5f) * 28f
    val floorLineColor = lineColor.copy(alpha = lineColor.alpha * 0.65f)
    for (i in -4..4) {
      val bottomX = cx + i * (size.width * 0.22f)
      drawLine(
        color = floorLineColor,
        start = Offset(vanishX + i * 16f, horizonY),
        end = Offset(bottomX, size.height),
        strokeWidth = 1.1f,
      )
    }
    for (row in 0..4) {
      val t = (row / 4f)
      val y = horizonY + (size.height - horizonY) * (t * t)
      drawLine(
        color = floorLineColor.copy(alpha = floorLineColor.alpha * (0.4f + t * 0.6f)),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.0f,
      )
    }

    // Geometric circuit trace lines matching reference UI atmosphere
    drawLine(
      color = lineColor,
      start = Offset(0f, cy - maxR * 0.12f),
      end = Offset(size.width * 0.24f, cy + maxR * 0.04f),
      strokeWidth = 1.5f,
    )
    drawLine(
      color = lineColor,
      start = Offset(size.width * 0.24f, cy + maxR * 0.04f),
      end = Offset(size.width * 0.36f, cy + maxR * 0.04f),
      strokeWidth = 1.5f,
    )
    drawLine(
      color = lineColor,
      start = Offset(size.width, cy - maxR * 0.05f),
      end = Offset(size.width * 0.74f, cy + maxR * 0.11f),
      strokeWidth = 1.5f,
    )
    drawLine(
      color = lineColor,
      start = Offset(size.width * 0.74f, cy + maxR * 0.11f),
      end = Offset(size.width * 0.62f, cy + maxR * 0.11f),
      strokeWidth = 1.5f,
    )
  }
}

@Composable
fun NexaGlassCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(24.dp)
  val isDark = isSystemInDarkTheme()
  val shadowClr = if (isDark) Color(0x66000000) else Color(0x366D89BA)
  val cardSurface =
    if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.91f)
  val borderBrush =
    if (isDark) {
      Brush.linearGradient(listOf(Color.White.copy(alpha = 0.26f), NexaCyanGlow.copy(alpha = 0.18f)))
    } else {
      Brush.linearGradient(listOf(Color.White.copy(alpha = 0.96f), NexaBlueCore.copy(alpha = 0.24f)))
    }

  var pressTiltX by remember { mutableStateOf(0f) }
  var pressTiltY by remember { mutableStateOf(0f) }
  LaunchedEffect(pressTiltX, pressTiltY) {
    if (pressTiltX != 0f || pressTiltY != 0f) {
      kotlinx.coroutines.delay(180L)
      pressTiltX = 0f
      pressTiltY = 0f
    }
  }
  val animatedTiltX by
    animateFloatAsState(
      targetValue = pressTiltX,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "card_tilt_x",
    )
  val animatedTiltY by
    animateFloatAsState(
      targetValue = pressTiltY,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "card_tilt_y",
    )

  Surface(
    modifier =
      modifier
        .graphicsLayer {
          cameraDistance = 14f * density
          rotationX = animatedTiltX
          rotationY = animatedTiltY
        }
        .shadow(elevation = 14.dp, shape = shape, ambientColor = shadowClr, spotColor = shadowClr)
        .then(
          if (onClick != null) {
            Modifier.clickable {
              pressTiltX = 2.2f
              pressTiltY = -2.2f
              onClick()
            }
          } else {
            Modifier
          }
        ),
    shape = shape,
    color = cardSurface,
    border = BorderStroke(1.3.dp, borderBrush),
  ) {
    Column(modifier = Modifier.padding(16.dp), content = content)
  }
}

@Composable
fun NexaTopBar(
  title: String = "ZED AI",
  showDrawerButton: Boolean = true,
  onOpenDrawer: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenTranscribe: () -> Unit = {},
  onOpenApiKeys: () -> Unit,
  onOpenPermissions: () -> Unit,
  onToggleWakeWord: () -> Unit,
  onSignOut: () -> Unit,
  wakeActive: Boolean,
  hasAlertBadge: Boolean,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val isDark = isSystemInDarkTheme()
  val pillBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.88f)
  val pillBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (showDrawerButton) {
        Surface(
          shape = CircleShape,
          color = pillBg,
          shadowElevation = 10.dp,
          border = BorderStroke(1.2.dp, pillBorder),
        ) {
          IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("top_menu_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Menu,
              contentDescription = "Open Navigation Drawer",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        Spacer(modifier = Modifier.width(14.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Medium,
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Live Notifications / OTP Activity Pill (matches screenshot top-right Bell)
      Box {
        Surface(
          shape = CircleShape,
          color = pillBg,
          shadowElevation = 10.dp,
          border = BorderStroke(1.2.dp, pillBorder),
        ) {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("notifications_bell_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Activity & OTP Alerts",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        if (hasAlertBadge) {
          Box(
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(NexaEmeraldOk)
          )
        }
      }

      // Three-Dot Overflow Menu Pill
      Box {
        Surface(
          shape = CircleShape,
          color = pillBg,
          shadowElevation = 10.dp,
          border = BorderStroke(1.2.dp, pillBorder),
        ) {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("three_dot_menu_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "More Options",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text("Transcribe Audio (Gemini 3.5)") },
            leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onOpenTranscribe()
            },
          )
          DropdownMenuItem(
            text = { Text("API Keys Vault (Gemini / OpenAI / Groq)") },
            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onOpenApiKeys()
            },
          )
          DropdownMenuItem(
            text = { Text("Permissions & Accessibility Hub") },
            leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onOpenPermissions()
            },
          )
          DropdownMenuItem(
            text = { Text(if (wakeActive) "Stop Wake-Word Core" else "Start Wake-Word Core") },
            leadingIcon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onToggleWakeWord()
            },
          )
          DropdownMenuItem(
            text = { Text("Sign Out (Google Account)") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = NexaRoseRadar) },
            onClick = {
              menuExpanded = false
              onSignOut()
            },
          )
        }
      }
    }
  }
}

enum class NexaTab(val label: String) {
  HOME("Home"),
  AGENTS("Agents"),
  CONTROL("Control"),
  CHAT("Yaadein"),
  TRANSCRIBE("Transcribe"),
}

@Composable
fun NexaFloatingBottomBar(
  selectedTab: NexaTab,
  isListening: Boolean,
  onSelectTab: (NexaTab) -> Unit,
  onMicClick: () -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val dockBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.88f)

  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Surface(
      shape = RoundedCornerShape(32.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      shadowElevation = 16.dp,
      border = BorderStroke(1.2.dp, dockBorder),
      modifier = Modifier.fillMaxWidth().height(70.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomNavItem(
          tab = NexaTab.HOME,
          selected = selectedTab == NexaTab.HOME,
          activeIcon = Icons.Rounded.Home,
          inactiveIcon = Icons.Outlined.Home,
          onClick = { onSelectTab(NexaTab.HOME) },
        )
        BottomNavItem(
          tab = NexaTab.AGENTS,
          selected = selectedTab == NexaTab.AGENTS,
          activeIcon = Icons.Rounded.AutoAwesome,
          inactiveIcon = Icons.Outlined.AutoAwesome,
          onClick = { onSelectTab(NexaTab.AGENTS) },
        )
        Spacer(modifier = Modifier.width(68.dp))
        BottomNavItem(
          tab = NexaTab.CONTROL,
          selected = selectedTab == NexaTab.CONTROL,
          activeIcon = Icons.Rounded.Tune,
          inactiveIcon = Icons.Outlined.Tune,
          onClick = { onSelectTab(NexaTab.CONTROL) },
        )
        BottomNavItem(
          tab = NexaTab.CHAT,
          selected = selectedTab == NexaTab.CHAT,
          activeIcon = Icons.Rounded.ChatBubble,
          inactiveIcon = Icons.Outlined.ChatBubbleOutline,
          onClick = { onSelectTab(NexaTab.CHAT) },
        )
      }
    }

    // Center raised glowing 3D Mic Orb with Material spring & 3D rotationY animation
    val micScope = rememberCoroutineScope()
    val micSpinY = remember { Animatable(0f) }
    val micScale by
      animateFloatAsState(
        targetValue = if (isListening) 1.09f else 1.0f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
        label = "mic_scale",
      )
    val micInfinite = rememberInfiniteTransition(label = "mic_3d_tilt")
    val micGyroTilt by
      micInfinite.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1100 else 3400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "mic_gyro",
      )
    LaunchedEffect(isListening) {
      if (isListening) {
        micSpinY.animateTo(
          targetValue = micSpinY.value + 360f,
          animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        )
      }
    }

    Box(
      modifier =
        Modifier
          .offset(y = (-16).dp)
          .size(72.dp)
          .graphicsLayer {
            cameraDistance = 12f * density
            scaleX = micScale
            scaleY = micScale
            rotationX = micGyroTilt * 0.45f
            rotationY = micSpinY.value + micGyroTilt
          }
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors =
                listOf(
                  (if (isListening) NexaRoseRadar else NexaBlueCore).copy(alpha = 0.38f),
                  Color.Transparent,
                )
            )
          ),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier
            .size(62.dp)
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                if (isListening) listOf(NexaRoseRadar, Color(0xFFE11D48))
                else listOf(Color(0xFF7DD3FC), NexaBlueCore)
              )
            )
            .clickable {
              micScope.launch {
                micSpinY.animateTo(
                  targetValue = micSpinY.value + 360f,
                  animationSpec = tween(durationMillis = 640, easing = FastOutSlowInEasing),
                )
              }
              onMicClick()
            }
            .testTag("center_voice_mic_button"),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.96f),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Mic,
              contentDescription = "Activate Voice Command",
              tint = if (isListening) NexaRoseRadar else NexaBlueCore,
              modifier = Modifier.size(23.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BottomNavItem(
  tab: NexaTab,
  selected: Boolean,
  activeIcon: ImageVector,
  inactiveIcon: ImageVector,
  onClick: () -> Unit,
) {
  val pillBrush =
    if (selected) Brush.verticalGradient(listOf(Color(0xFF60A5FA), NexaBlueCore))
    else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
  val contentColor =
    if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

  Column(
    modifier =
      Modifier
        .clip(RoundedCornerShape(18.dp))
        .background(pillBrush)
        .clickable(onClick = onClick)
        .padding(horizontal = 13.dp, vertical = 7.dp)
        .testTag("nav_tab_${tab.name.lowercase()}"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      imageVector = if (selected) activeIcon else inactiveIcon,
      contentDescription = tab.label,
      tint = contentColor,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = tab.label,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      color = contentColor,
    )
  }
}
