package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProviderType
import com.example.data.model.CustomAgent
import com.example.data.model.DevicePermissionsState
import com.example.data.model.LocalAiSecrets
import com.example.data.model.UserProfile
import com.example.ui.CommandUiState
import com.example.ui.components.NeuralCoreOrb3D
import com.example.ui.components.NexaGlassCard
import com.example.ui.components.NexaTab
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaGoldNode
import com.example.ui.theme.NexaRoseRadar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
) {
  val context = LocalContext.current
  var commandInput by remember { mutableStateOf("") }
  var attachedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
  var attachedImageMime by remember { mutableStateOf("image/jpeg") }
  var otpCopiedHint by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        val bytes =
          runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            .getOrNull()
        if (bytes != null && bytes.isNotEmpty()) {
          attachedImageBytes = bytes
          attachedImageMime = context.contentResolver.getType(uri) ?: "image/jpeg"
        }
      }
    }

  fun submitCurrentInput() {
    val img = attachedImageBytes
    if (img != null && img.isNotEmpty()) {
      onSendVisionCommand(commandInput, img, attachedImageMime)
      attachedImageBytes = null
      commandInput = ""
    } else if (commandInput.isNotBlank()) {
      onSendCommand(commandInput)
      commandInput = ""
    }
  }

  val scrollState = rememberScrollState()
  val bossName = profile?.bossName?.ifBlank { "BOSS" } ?: "BOSS"
  val energy = profile?.energyPoints ?: 100L
  val greeting =
    remember {
      val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        hour < 21 -> "Good evening,"
        else -> "Good night,"
      }
    }
  val dayNum = remember { SimpleDateFormat("dd", Locale.getDefault()).format(Date()) }
  val daySub = remember { SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(Date()) }
  val activeProvider = AiProviderType.fromId(secrets.activeProvider)
  val selectedAgent = agents.firstOrNull { it.id == selectedAgentId }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth()) {
      // Greeting + Energy Pill Row (matching screenshot header)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column {
          Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = bossName,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
          )
        }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          shadowElevation = 10.dp,
          border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.86f)),
          modifier = Modifier.clickable(onClick = onOpenApiKeys).testTag("energy_provider_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = "Active AI Energy",
              tint = NexaGoldNode,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "$energy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = activeProvider.displayName.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 3D Neural Orb Centerpiece
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        NeuralCoreOrb3D(
          isListening = isVoiceListening || wakeRunning,
          isBusy = commandUiState is CommandUiState.Thinking,
          accessibilityActive = permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled,
          orbSize = 266.dp,
          onOrbTapped = onOrbVoiceClick,
        )
      }

      // Live Telemetry Status Pill below Orb
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f)),
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val dotColor =
              when {
                commandUiState is CommandUiState.Thinking -> NexaBlueCore
                isVoiceListening || wakeRunning -> NexaRoseRadar
                permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled -> NexaEmeraldOk
                else -> NexaGoldNode
              }
            Box(
              modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                when (commandUiState) {
                  is CommandUiState.Thinking -> commandUiState.label
                  else -> if (wakeRunning) wakeStatus else "Tap 3D Core or say 'Hey Zed'"
                },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick Action Pills Row (matching screenshot's 3-pill frosted strip + API Keys)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickPillChip(
          icon = Icons.Rounded.SmartToy,
          label = "Agents (${agents.count { it.isEnabled }})",
          tint = NexaBlueCore,
          onClick = { onNavigateTab(NexaTab.AGENTS) },
        )
        QuickPillChip(
          icon = Icons.Rounded.GraphicEq,
          label = "Transcribe",
          tint = NexaRoseRadar,
          onClick = { onNavigateTab(NexaTab.TRANSCRIBE) },
        )
        QuickPillChip(
          icon = Icons.Rounded.MarkEmailRead,
          label = "Auto-Reply",
          tint = NexaBlueCore,
          onClick = { onNavigateTab(NexaTab.AGENTS) },
        )
        QuickPillChip(
          icon = Icons.Rounded.Security,
          label = "Phone Ctrl",
          tint = NexaBlueCore,
          onClick = { onNavigateTab(NexaTab.CONTROL) },
        )
        QuickPillChip(
          icon = Icons.Rounded.Key,
          label = "API Keys",
          tint = NexaGoldNode,
          onClick = onOpenApiKeys,
        )
        QuickPillChip(
          icon = Icons.Rounded.Alarm,
          label = "5m Timer",
          tint = NexaBlueCore,
          onClick = { onSendCommand("5 minute ka timer lagao") },
        )
        QuickPillChip(
          icon = Icons.Rounded.MusicNote,
          label = "Media",
          tint = NexaEmeraldOk,
          onClick = { onSendCommand("Media play pause karo") },
        )
        QuickPillChip(
          icon = Icons.Rounded.Wifi,
          label = "Wi-Fi Panel",
          tint = NexaBlueCore,
          onClick = { onSendCommand("Wi-Fi settings panel kholo") },
        )
        QuickPillChip(
          icon = Icons.Rounded.Public,
          label = "Web Search",
          tint = NexaGoldNode,
          onClick = { onSendCommand("Google par latest AI news search karo") },
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3 Bento Status Cards (Mausam / Aaj / Mood style from screenshot)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        NexaGlassCard(
          modifier = Modifier.weight(1f),
          onClick = { onNavigateTab(NexaTab.CONTROL) },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.WbSunny,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Control", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(modifier = Modifier.height(8.dp))
          val a11yReady = permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled
          Text(
            text = if (a11yReady) "FULL" else "OFF",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (a11yReady) NexaEmeraldOk else NexaRoseRadar,
          )
          Text(
            text = if (permissions.notificationListenerEnabled) "Auto-Reply ON" else "Setup Needed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        NexaGlassCard(
          modifier = Modifier.weight(1f),
          onClick = { onNavigateTab(NexaTab.AGENTS) },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aaj", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = dayNum,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "$daySub · ${agents.size} Agents",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        NexaGlassCard(
          modifier = Modifier.weight(1f),
          onClick = {
            if (!lastOtp.isNullOrBlank()) {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              cm?.setPrimaryClip(ClipData.newPlainText("ZED OTP", lastOtp))
              otpCopiedHint = true
            } else {
              onNavigateTab(NexaTab.CONTROL)
            }
          },
        ) {
          Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = null,
              tint = NexaRoseRadar,
              modifier =
                Modifier
                  .align(Alignment.CenterEnd)
                  .size(46.dp)
                  .alpha(0.18f),
            )
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.Favorite,
                  contentDescription = null,
                  tint = NexaRoseRadar,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wake", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (wakeRunning) "LIVE" else "READY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (wakeRunning) NexaBlueCore else MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  when {
                    otpCopiedHint -> "OTP Copied!"
                    !lastOtp.isNullOrBlank() -> "OTP: $lastOtp"
                    else -> "Hey Zed"
                  },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Active Agent Selector Row
      if (agents.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AgentFilterChip(
            label = "ZED Core",
            selected = selectedAgentId == null,
            onClick = { onSelectAgent(null) },
          )
          agents.filter { it.isEnabled }.forEach { agent ->
            AgentFilterChip(
              label = agent.name,
              selected = selectedAgentId == agent.id,
              onClick = { onSelectAgent(agent.id) },
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Latest Execution / Reply Feedback Card
      when (commandUiState) {
        is CommandUiState.Thinking -> {
          NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Text(commandUiState.label, style = MaterialTheme.typography.bodyMedium)
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }
        is CommandUiState.Success -> {
          NexaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigateTab(NexaTab.CHAT) },
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = selectedAgent?.name ?: "ZED AI",
                style = MaterialTheme.typography.labelLarge,
                color = NexaBlueCore,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = commandUiState.message,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }
        is CommandUiState.Error -> {
          NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = commandUiState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }
        CommandUiState.Idle -> Unit
      }

      if (attachedImageBytes != null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = NexaBlueCore.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, NexaBlueCore.copy(alpha = 0.35f)),
          modifier = Modifier.padding(bottom = 8.dp).testTag("attached_vision_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Image attached for Vision AI",
              style = MaterialTheme.typography.labelMedium,
              color = NexaBlueCore,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Remove attached image",
              tint = NexaRoseRadar,
              modifier = Modifier.size(16.dp).clickable { attachedImageBytes = null },
            )
          }
        }
      }

      // Bottom Frosted Command Input Bar (matching screenshot)
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 12.dp,
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.86f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.size(44.dp).testTag("home_attach_photo_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AddPhotoAlternate,
              contentDescription = "Attach Photo or Screenshot for Vision AI",
              tint = if (attachedImageBytes != null) NexaBlueCore else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Box(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (commandInput.isEmpty()) {
              Text(
                text =
                  if (attachedImageBytes != null) "Ask Vision AI about this image..."
                  else "ZED se kuch bhi poochho...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = commandInput,
              onValueChange = { commandInput = it },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
              cursorBrush = SolidColor(NexaBlueCore),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = { submitCurrentInput() }),
              modifier = Modifier.fillMaxWidth().testTag("home_command_input"),
            )
          }
          Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(44.dp),
          ) {
            IconButton(
              onClick = { submitCurrentInput() },
              modifier = Modifier.testTag("home_send_command_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send Command",
                tint = NexaBlueCore,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(106.dp))
    }
  }
}

@Composable
private fun QuickPillChip(
  icon: ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    shadowElevation = 8.dp,
    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.85f)),
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun AgentFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (selected) NexaBlueCore else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
    border = BorderStroke(1.dp, if (selected) NexaBlueCore else Color.White.copy(alpha = 0.80f)),
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}
