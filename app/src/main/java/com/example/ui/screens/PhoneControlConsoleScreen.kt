package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayForWork
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DevicePermissionsState
import com.example.data.model.ExecutionLogEntry
import com.example.data.model.LocalAiSecrets
import com.example.system.NexaAccessibilityService
import com.example.system.NexaCommandBus
import com.example.system.PermissionsHelper
import com.example.ui.components.NexaGlassCard
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneControlConsoleScreen(
  permissions: DevicePermissionsState,
  secrets: LocalAiSecrets,
  wakeRunning: Boolean,
  wakeStatus: String,
  logs: List<ExecutionLogEntry>,
  onSaveSecrets: (LocalAiSecrets) -> Unit,
  onRunQuickCommand: (String) -> Unit,
  onRefreshPermissions: () -> Unit,
) {
  val context = LocalContext.current
  var wakeWordsDraft by remember(secrets.wakeWordsCsv) { mutableStateOf(secrets.wakeWordsCsv) }
  var showA11yDisclosure by remember { mutableStateOf(false) }
  var testButtonLabel by remember { mutableStateOf("") }
  var lastScreenPreview by remember { mutableStateOf("") }

  val micLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      onRefreshPermissions()
    }
  val notifLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      onRefreshPermissions()
    }
  val callLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      onRefreshPermissions()
    }

  if (showA11yDisclosure) {
    AlertDialog(
      onDismissRequest = { showA11yDisclosure = false },
      title = { Text("Enable Accessibility Agent Control") },
      text = {
        Text(
          "NEXA AI uses Android AccessibilityService strictly when you toggle it on to read visible screen elements, click buttons, type text, and run your voice or agent commands. You can pause or disable it anytime."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showA11yDisclosure = false
            PermissionsHelper.openAccessibilitySettings(context)
          }
        ) { Text("Open Accessibility Settings") }
      },
      dismissButton = {
        TextButton(onClick = { showA11yDisclosure = false }) { Text("Cancel") }
      },
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text("Phone Control & Permissions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text("Enable only the Android permissions you approve (user ki marji se).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Accessibility Agent Master Control Card
    item {
      NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Accessibility AI Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text =
                when {
                  !permissions.accessibilityEnabled -> "System service off — tap Enable to grant"
                  secrets.accessibilityAgentControlEnabled -> "Active — agent can read screen & click buttons"
                  else -> "Paused by your toggle — screen actions blocked"
                },
              style = MaterialTheme.typography.bodyMedium,
              color =
                if (permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled) NexaEmeraldOk
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled,
            onCheckedChange = { enable ->
              if (enable && !permissions.accessibilityEnabled) {
                showA11yDisclosure = true
              } else {
                onSaveSecrets(secrets.copy(accessibilityAgentControlEnabled = enable))
                NexaCommandBus.addLog(
                  "ACCESSIBILITY",
                  if (enable) "Agent Control Resumed" else "Agent Control Paused",
                  if (enable) "AI agent can read screen and click buttons." else "User paused AI Accessibility actions.",
                )
              }
            },
            modifier = Modifier.testTag("accessibility_agent_toggle"),
          )
        }

        if (!permissions.accessibilityEnabled) {
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = { showA11yDisclosure = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexaRoseRadar),
            modifier = Modifier.testTag("open_accessibility_settings_button"),
          ) {
            Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enable System Accessibility", color = Color.White)
          }
        } else {
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = testButtonLabel,
              onValueChange = { testButtonLabel = it },
              label = { Text("Button label to click") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            Button(
              onClick = {
                val svc = NexaAccessibilityService.instance
                if (svc != null && testButtonLabel.isNotBlank()) {
                  svc.tapNodeByText(testButtonLabel.trim())
                }
              },
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Click")
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = {
              val svc = NexaAccessibilityService.instance
              lastScreenPreview =
                svc?.readCurrentScreenSnapshot()?.ifBlank { "No active screen text or paused." }
                  ?: "Accessibility service not bound."
            },
          ) {
            Text("Inspect Screen Nodes")
          }
          if (lastScreenPreview.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(lastScreenPreview.take(360), style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Background Wake-Word & 3D Floating Orb Card
    item {
      NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Background Wake-Word Core", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = if (wakeRunning) wakeStatus else "Say 'Hey Nexa', 'Hello Nexa', or 'Alexa' anywhere",
              style = MaterialTheme.typography.bodyMedium,
              color = if (wakeRunning) NexaEmeraldOk else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = secrets.wakeWordListeningEnabled || wakeRunning,
            onCheckedChange = { enable ->
              if (enable && !permissions.micGranted) {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
              } else {
                onSaveSecrets(secrets.copy(wakeWordListeningEnabled = enable))
              }
            },
            modifier = Modifier.testTag("wake_word_switch"),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Floating 3D Orb Overlay", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = secrets.floatingOrbEnabled,
            onCheckedChange = { enable ->
              if (enable && !permissions.overlayEnabled) {
                PermissionsHelper.openOverlaySettings(context)
              } else {
                onSaveSecrets(secrets.copy(floatingOrbEnabled = enable))
              }
            },
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Voice TTS Spoken Replies", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = secrets.ttsVoiceReplyEnabled,
            onCheckedChange = { enable -> onSaveSecrets(secrets.copy(ttsVoiceReplyEnabled = enable)) },
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = wakeWordsDraft,
          onValueChange = { wakeWordsDraft = it },
          label = { Text("Wake Words (comma separated)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = { onSaveSecrets(secrets.copy(wakeWordsCsv = wakeWordsDraft)) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
        ) {
          Text("Save Wake Phrases")
        }
      }
    }

    // Permission Consent Hub
    item {
      NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Android System Permissions Hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        PermissionActionRow(
          icon = Icons.Rounded.AccessibilityNew,
          title = "Accessibility Full Control",
          subtitle = "Tap buttons, read screen, scroll & automate WhatsApp/Play Store",
          granted = permissions.accessibilityEnabled,
          onEnableClick = { PermissionsHelper.openAccessibilitySettings(context) },
        )
        PermissionActionRow(
          icon = Icons.Rounded.NotificationsActive,
          title = "Notification & Auto-Reply Access",
          subtitle = "Monitor WhatsApp, Gmail & OTP alerts for auto-reply",
          granted = permissions.notificationListenerEnabled,
          onEnableClick = { PermissionsHelper.openNotificationListenerSettings(context) },
        )
        PermissionActionRow(
          icon = Icons.Rounded.Layers,
          title = "Floating Overlay HUD",
          subtitle = "Keep NEXA Orb accessible over other apps",
          granted = permissions.overlayEnabled,
          onEnableClick = { PermissionsHelper.openOverlaySettings(context) },
        )
        PermissionActionRow(
          icon = Icons.Rounded.Mic,
          title = "Microphone (Voice & Wake Word)",
          subtitle = "Required for voice commands & Hey Nexa detection",
          granted = permissions.micGranted,
          onEnableClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          PermissionActionRow(
            icon = Icons.Rounded.NotificationsActive,
            title = "Post Notifications",
            subtitle = "Keep foreground voice service active & show alerts",
            granted = permissions.notificationsGranted,
            onEnableClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          )
        }
        PermissionActionRow(
          icon = Icons.Rounded.Phone,
          title = "Direct Phone Call",
          subtitle = "Allow voice command calling",
          granted = permissions.callPhoneGranted,
          onEnableClick = { callLauncher.launch(Manifest.permission.CALL_PHONE) },
        )
        PermissionActionRow(
          icon = Icons.Rounded.BatteryChargingFull,
          title = "Unrestricted Background Battery",
          subtitle = "Prevent Android from sleeping wake-word service",
          granted = permissions.batteryOptimizationIgnored,
          onEnableClick = { PermissionsHelper.openBatteryOptimizationSettings(context) },
        )
      }
    }

    // Quick Hardware & App Controls
    item {
      NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Quick Device Macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { onRunQuickCommand("Toggle flashlight") }) {
            Icon(Icons.Rounded.FlashlightOn, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Flashlight")
          }
          OutlinedButton(onClick = { onRunQuickCommand("Open WhatsApp") }) {
            Text("Open WhatsApp")
          }
          OutlinedButton(onClick = { onRunQuickCommand("Open Gmail") }) {
            Text("Open Gmail")
          }
          OutlinedButton(onClick = { onRunQuickCommand("Play Store se Telegram install karo") }) {
            Icon(Icons.Rounded.PlayForWork, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Install Test")
          }
          OutlinedButton(onClick = { onRunQuickCommand("Read current screen") }) {
            Text("Read Screen")
          }
        }
      }
    }

    // Live Execution Log Header
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Terminal, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Live Execution Console (${logs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }

    if (logs.isEmpty()) {
      item {
        NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
          Text("No actions logged yet. Run a voice/text command or toggle a permission!", style = MaterialTheme.typography.bodyMedium)
        }
      }
    } else {
      items(logs, key = { it.id }) { log ->
        val time = remember(log.timestampMs) { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestampMs)) }
        NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("[${log.source}] ${log.title}", style = MaterialTheme.typography.labelLarge, color = NexaBlueCore)
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(log.detail, style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    item { Spacer(modifier = Modifier.height(104.dp)) }
  }
}

@Composable
private fun PermissionActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  granted: Boolean,
  onEnableClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (granted) NexaEmeraldOk else NexaBlueCore,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    if (granted) {
      Surface(shape = RoundedCornerShape(12.dp), color = NexaEmeraldOk.copy(alpha = 0.14f)) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NexaEmeraldOk, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Active", style = MaterialTheme.typography.labelSmall, color = NexaEmeraldOk)
        }
      }
    } else {
      Button(
        onClick = onEnableClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexaRoseRadar),
      ) {
        Text("Enable", color = Color.White)
      }
    }
  }
}
