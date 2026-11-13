package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AutomationRule
import com.example.data.model.CustomAgent
import com.example.ui.components.NexaGlassCard
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentsControlScreen(
  agents: List<CustomAgent>,
  automations: List<AutomationRule>,
  onCreateAgent: (String, String, String, String, List<String>, Boolean, String) -> Unit,
  onToggleAgent: (CustomAgent) -> Unit,
  onDeleteAgent: (String) -> Unit,
  onRunAgentCommand: (CustomAgent, String) -> Unit,
  onCreateRule: (String, String, String, String, Boolean, Boolean) -> Unit,
  onToggleRule: (AutomationRule) -> Unit,
  onDeleteRule: (String) -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) }
  var showCreateAgentDialog by remember { mutableStateOf(false) }
  var showCreateRuleDialog by remember { mutableStateOf(false) }
  var agentToRun by remember { mutableStateOf<CustomAgent?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Autonomous Agents", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Create custom AI agents & auto-reply rules", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(
        onClick = { if (subTab == 0) showCreateAgentDialog = true else showCreateRuleDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
        modifier = Modifier.testTag("add_agent_or_rule_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (subTab == 0) "New Agent" else "New Rule")
      }
    }

    TabRow(
      selectedTabIndex = subTab,
      containerColor = Color.Transparent,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Agents (${agents.size})") })
      Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Auto-Reply & OTP (${automations.size})") })
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (subTab == 0) {
      if (agents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No custom agents yet. Tap + New Agent to build one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(agents, key = { it.id }) { agent ->
            NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Box(
                    modifier =
                      Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexaBlueCore.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = NexaBlueCore)
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(agent.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(agent.role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                Switch(checked = agent.isEnabled, onCheckedChange = { onToggleAgent(agent) })
              }

              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = NexaBlueCore.copy(alpha = 0.12f)) {
                  Text(
                    text = agent.triggerType,
                    style = MaterialTheme.typography.labelSmall,
                    color = NexaBlueCore,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
                agent.allowedTools.take(4).forEach { tool ->
                  Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                      text = tool,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Runs: ${agent.runCount} · ${if (agent.autoExecute) "Auto" else "Ask First"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                  IconButton(onClick = { onDeleteAgent(agent.id) }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Agent", tint = NexaRoseRadar)
                  }
                  Button(
                    onClick = { agentToRun = agent },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexaEmeraldOk),
                  ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run")
                  }
                }
              }
            }
          }
          item { Spacer(modifier = Modifier.height(104.dp)) }
        }
      }
    } else {
      if (automations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No automation rules yet. Tap + New Rule to automate WhatsApp/Gmail/OTP!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(automations, key = { it.id }) { rule ->
            NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(rule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${rule.targetApp} · Match: ${rule.triggerPattern}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexaBlueCore,
                  )
                }
                Switch(checked = rule.isActive, onCheckedChange = { onToggleRule(rule) })
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(rule.replyTemplateOrGoal, style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text =
                    "${if (rule.useAiReply) "AI Reply" else "Template"} · ${if (rule.requireConfirmation) "Confirm First" else "Full Auto"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { onDeleteRule(rule.id) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Rule", tint = NexaRoseRadar)
                }
              }
            }
          }
          item { Spacer(modifier = Modifier.height(104.dp)) }
        }
      }
    }
  }

  // Run Agent Dialog
  agentToRun?.let { agent ->
    var quickCmd by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { agentToRun = null },
      title = { Text("Run ${agent.name}") },
      text = {
        Column {
          Text("Give one command for this agent to execute:", style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = quickCmd,
            onValueChange = { quickCmd = it },
            placeholder = { Text("e.g. WhatsApp pe Boss ko Hi bhejo") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (quickCmd.isNotBlank()) {
              onRunAgentCommand(agent, quickCmd)
              agentToRun = null
            }
          }
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Execute")
        }
      },
      dismissButton = { TextButton(onClick = { agentToRun = null }) { Text("Cancel") } },
    )
  }

  if (showCreateAgentDialog) {
    CreateAgentDialog(
      onDismiss = { showCreateAgentDialog = false },
      onCreate = { name, role, prompt, trigger, tools, autoExec ->
        onCreateAgent(name, role, prompt, trigger, tools, autoExec, "#38BDF8")
        showCreateAgentDialog = false
      },
    )
  }

  if (showCreateRuleDialog) {
    CreateRuleDialog(
      onDismiss = { showCreateRuleDialog = false },
      onCreate = { title, app, pattern, goal, aiReply, confirm ->
        onCreateRule(title, app, pattern, goal, aiReply, confirm)
        showCreateRuleDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateAgentDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, List<String>, Boolean) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("") }
  var prompt by remember { mutableStateOf("") }
  var trigger by remember { mutableStateOf("MANUAL") }
  var autoExec by remember { mutableStateOf(true) }
  val selectedTools = remember { mutableStateListOf("OPEN_APP", "TAP_TEXT", "AUTO_REPLY") }
  val triggers = listOf("MANUAL", "WHATSAPP_INCOMING", "GMAIL_INCOMING", "OTP_EVENT", "VOICE_WAKE")
  val allTools = listOf("OPEN_APP", "TAP_TEXT", "INPUT_TEXT", "AUTO_REPLY", "SEND_EMAIL_INTENT", "PLAY_STORE_INSTALL", "READ_OTP")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Custom Agent") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Agent Name") }, singleLine = true)
        OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role Summary") }, singleLine = true)
        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("System Instructions") }, minLines = 2)
        Text("Trigger", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          triggers.forEach { t ->
            FilterChip(selected = trigger == t, onClick = { trigger = t }, label = { Text(t.substringBefore("_")) })
          }
        }
        Text("Allowed Phone Tools", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          allTools.forEach { tool ->
            val checked = selectedTools.contains(tool)
            FilterChip(
              selected = checked,
              onClick = { if (checked) selectedTools.remove(tool) else selectedTools.add(tool) },
              label = { Text(tool.replace("_", " ")) },
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = autoExec, onCheckedChange = { autoExec = it })
          Text("Auto-execute planned steps", style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onCreate(
              name,
              role.ifBlank { "Custom Autonomous Agent" },
              prompt.ifBlank { "Execute boss command safely and accurately." },
              trigger,
              selectedTools.toList().ifEmpty { listOf("OPEN_APP") },
              autoExec,
            )
          }
        }
      ) { Text("Create") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateRuleDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, Boolean, Boolean) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var targetApp by remember { mutableStateOf("WHATSAPP") }
  var pattern by remember { mutableStateOf("*") }
  var goal by remember { mutableStateOf("") }
  var useAi by remember { mutableStateOf(true) }
  var confirm by remember { mutableStateOf(true) }
  val apps = listOf("WHATSAPP", "GMAIL", "SMS", "PLAY_STORE", "SYSTEM")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Auto-Reply / OTP Rule") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Rule Title") }, singleLine = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          apps.forEach { app ->
            FilterChip(selected = targetApp == app, onClick = { targetApp = app }, label = { Text(app) })
          }
        }
        OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Match Keyword (* for all)") }, singleLine = true)
        OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("AI Goal or Reply Template") }, minLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = useAi, onCheckedChange = { useAi = it })
          Text("Generate reply using AI", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = confirm, onCheckedChange = { confirm = it })
          Text("Require confirmation before sending", style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && goal.isNotBlank()) {
            onCreate(title, targetApp, pattern.ifBlank { "*" }, goal, useAi, confirm)
          }
        }
      ) { Text("Save Rule") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
