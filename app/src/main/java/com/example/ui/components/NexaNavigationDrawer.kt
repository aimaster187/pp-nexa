package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar

data class NexaDrawerStateModel(
  val selectedTab: NexaTab,
  val userDisplayName: String,
  val userEmail: String,
  val activeProviderName: String,
  val enabledAgentCount: Int,
  val historyCount: Int,
  val latestHistorySnippet: String?,
  val accessibilityBound: Boolean,
  val accessibilityAgentEnabled: Boolean,
  val wakeRunning: Boolean,
  val wakeEnabled: Boolean,
)

@Composable
fun NexaResponsiveNavigationDrawer(
  drawerState: DrawerState,
  model: NexaDrawerStateModel,
  onSelectTab: (NexaTab) -> Unit,
  onCloseDrawer: () -> Unit,
  onToggleAccessibilityAgent: (Boolean) -> Unit,
  onToggleWakeWord: (Boolean) -> Unit,
  onOpenApiKeys: () -> Unit,
  onOpenHistoryFeed: () -> Unit,
  onSignOut: () -> Unit,
  content: @Composable (showHamburgerButton: Boolean) -> Unit,
) {
  BackHandler(enabled = drawerState.isOpen) {
    onCloseDrawer()
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val maxWidth = maxWidth
    when {
      maxWidth >= 840.dp -> {
        PermanentNavigationDrawer(
          drawerContent = {
            PermanentDrawerSheet(
              drawerContainerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.width(308.dp),
            ) {
              NexaDrawerContent(
                model = model,
                onSelectTab = onSelectTab,
                onToggleAccessibilityAgent = onToggleAccessibilityAgent,
                onToggleWakeWord = onToggleWakeWord,
                onOpenApiKeys = onOpenApiKeys,
                onOpenHistoryFeed = onOpenHistoryFeed,
                onSignOut = onSignOut,
              )
            }
          },
        ) {
          content(false)
        }
      }
      maxWidth >= 600.dp -> {
        DismissibleNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            DismissibleDrawerSheet(
              drawerContainerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.width(296.dp),
            ) {
              NexaDrawerContent(
                model = model,
                onSelectTab = {
                  onSelectTab(it)
                  onCloseDrawer()
                },
                onToggleAccessibilityAgent = onToggleAccessibilityAgent,
                onToggleWakeWord = onToggleWakeWord,
                onOpenApiKeys = {
                  onCloseDrawer()
                  onOpenApiKeys()
                },
                onOpenHistoryFeed = {
                  onCloseDrawer()
                  onOpenHistoryFeed()
                },
                onSignOut = {
                  onCloseDrawer()
                  onSignOut()
                },
              )
            }
          },
        ) {
          content(true)
        }
      }
      else -> {
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet(
              drawerContainerColor = MaterialTheme.colorScheme.surface,
              drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
              modifier = Modifier.widthIn(min = 280.dp, max = 328.dp),
            ) {
              NexaDrawerContent(
                model = model,
                onSelectTab = {
                  onSelectTab(it)
                  onCloseDrawer()
                },
                onToggleAccessibilityAgent = onToggleAccessibilityAgent,
                onToggleWakeWord = onToggleWakeWord,
                onOpenApiKeys = {
                  onCloseDrawer()
                  onOpenApiKeys()
                },
                onOpenHistoryFeed = {
                  onCloseDrawer()
                  onOpenHistoryFeed()
                },
                onSignOut = {
                  onCloseDrawer()
                  onSignOut()
                },
              )
            }
          },
        ) {
          content(true)
        }
      }
    }
  }
}

@Composable
private fun NexaDrawerContent(
  model: NexaDrawerStateModel,
  onSelectTab: (NexaTab) -> Unit,
  onToggleAccessibilityAgent: (Boolean) -> Unit,
  onToggleWakeWord: (Boolean) -> Unit,
  onOpenApiKeys: () -> Unit,
  onOpenHistoryFeed: () -> Unit,
  onSignOut: () -> Unit,
) {
  val itemColors =
    NavigationDrawerItemDefaults.colors(
      selectedContainerColor = NexaBlueCore.copy(alpha = 0.14f),
      selectedIconColor = NexaBlueCore,
      selectedTextColor = NexaBlueCore,
    )

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag("nexa_navigation_drawer"),
  ) {
    // ACCOUNT SECTION
    Text(
      text = "ACCOUNT",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("drawer_section_account"),
    )
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        NeuralCoreOrb3D(
          isListening = model.wakeRunning,
          isBusy = false,
          accessibilityActive = model.accessibilityBound && model.accessibilityAgentEnabled,
          orbSize = 60.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "NEXA AI",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = model.userDisplayName.ifBlank { "BOSS" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (model.userEmail.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = NexaEmeraldOk,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = model.userEmail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // WORKSPACES SECTION
    Text(
      text = "WORKSPACES",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
    NavigationDrawerItem(
      label = { Text("3D Neural Home") },
      selected = model.selectedTab == NexaTab.HOME,
      colors = itemColors,
      icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.HOME) },
    )
    NavigationDrawerItem(
      label = { Text("Agents & Auto-Reply") },
      badge = { Text("${model.enabledAgentCount}") },
      selected = model.selectedTab == NexaTab.AGENTS,
      colors = itemColors,
      icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.AGENTS) },
    )
    NavigationDrawerItem(
      label = { Text("Transcribe Audio") },
      badge = { Text("3.5") },
      selected = model.selectedTab == NexaTab.TRANSCRIBE,
      colors = itemColors,
      icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.TRANSCRIBE) },
      modifier = Modifier.testTag("drawer_item_transcribe"),
    )

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(10.dp))

    // HISTORY SECTION
    Text(
      text = "HISTORY",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("drawer_section_history"),
    )
    NavigationDrawerItem(
      label = { Text("Chat & Yaadein History") },
      badge = { Text("${model.historyCount}") },
      selected = model.selectedTab == NexaTab.CHAT,
      colors = itemColors,
      icon = { Icon(Icons.Rounded.ChatBubble, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.CHAT) },
      modifier = Modifier.testTag("drawer_item_history"),
    )
    NavigationDrawerItem(
      label = {
        Column {
          Text("Execution & OTP Log")
          if (!model.latestHistorySnippet.isNullOrBlank()) {
            Text(
              text = model.latestHistorySnippet,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      },
      selected = false,
      icon = { Icon(Icons.Rounded.History, contentDescription = null) },
      onClick = onOpenHistoryFeed,
    )

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(10.dp))

    // SETTINGS SECTION
    Text(
      text = "SETTINGS",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("drawer_section_settings"),
    )
    NavigationDrawerItem(
      label = { Text("Permissions & Phone Control") },
      selected = model.selectedTab == NexaTab.CONTROL,
      colors = itemColors,
      icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.CONTROL) },
      modifier = Modifier.testTag("drawer_item_settings"),
    )
    NavigationDrawerItem(
      label = { Text("AI Keys & Account (${model.activeProviderName})") },
      selected = false,
      icon = { Icon(Icons.Rounded.Key, contentDescription = null) },
      onClick = onOpenApiKeys,
    )
    NavigationDrawerItem(
      label = { Text("Settings Overview") },
      selected = false,
      icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
      onClick = { onSelectTab(NexaTab.CONTROL) },
    )

    // Quick Autonomous Switches inside Settings
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Accessibility Agent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text =
                if (!model.accessibilityBound) "Tap Control to grant"
                else if (model.accessibilityAgentEnabled) "Active"
                else "Paused",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Switch(
          checked = model.accessibilityBound && model.accessibilityAgentEnabled,
          onCheckedChange = onToggleAccessibilityAgent,
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = NexaRoseRadar, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Hey Nexa / Alexa", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = if (model.wakeRunning) "Listening" else "Standby",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Switch(
          checked = model.wakeEnabled || model.wakeRunning,
          onCheckedChange = onToggleWakeWord,
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    NavigationDrawerItem(
      label = { Text("Sign Out (Google)", color = NexaRoseRadar) },
      selected = false,
      icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = NexaRoseRadar) },
      onClick = onSignOut,
      modifier = Modifier.testTag("drawer_item_sign_out"),
    )
    Spacer(modifier = Modifier.height(18.dp))
  }
}
