package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatTurn
import com.example.data.model.MemoryEntry
import com.example.data.model.PhoneActionStep
import com.example.ui.CommandUiState
import com.example.ui.components.NexaGlassCard
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryChatScreen(
  chatMessages: List<ChatTurn>,
  memories: List<MemoryEntry>,
  commandUiState: CommandUiState,
  onSendMessage: (String) -> Unit,
  onRerunSteps: (List<PhoneActionStep>) -> Unit,
  onCreateMemory: (String, String) -> Unit,
  onDeleteMemory: (String) -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) }
  var messageDraft by remember { mutableStateOf("") }
  var showAddMemory by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Neural Chat & Yaadein", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("AI command history & persistent memory vault", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (subTab == 1) {
        Button(
          onClick = { showAddMemory = true },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add")
        }
      }
    }

    TabRow(selectedTabIndex = subTab, containerColor = Color.Transparent) {
      Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Chat (${chatMessages.size})") })
      Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Yaadein (${memories.size})") })
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (subTab == 0) {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(chatMessages, key = { it.id }) { turn ->
          val align = if (turn.isUser) Alignment.CenterEnd else Alignment.CenterStart
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color =
                if (turn.isUser) NexaBlueCore
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              border = if (turn.isUser) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
              shadowElevation = 4.dp,
              modifier = Modifier.fillMaxWidth(0.90f),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = turn.agentName,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (turn.isUser) Color.White.copy(alpha = 0.8f) else NexaBlueCore,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = turn.text,
                  style = MaterialTheme.typography.bodyLarge,
                  color = if (turn.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                if (turn.plannedSteps.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    turn.plannedSteps.forEach { step ->
                      Surface(shape = RoundedCornerShape(10.dp), color = NexaBlueCore.copy(alpha = 0.12f)) {
                        Text(
                          text = "${step.action}: ${step.target.ifBlank { step.value }}",
                          style = MaterialTheme.typography.labelSmall,
                          color = NexaBlueCore,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  TextButton(onClick = { onRerunSteps(turn.plannedSteps) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = NexaEmeraldOk)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-run Steps", color = NexaEmeraldOk)
                  }
                }
                if (turn.executionStatus.isNotBlank()) {
                  Text(
                    text = turn.executionStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 104.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = messageDraft,
          onValueChange = { messageDraft = it },
          placeholder = { Text("Ask NEXA or give phone command...") },
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.weight(1f).testTag("chat_tab_input"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(shape = CircleShape, color = NexaBlueCore, modifier = Modifier.size(50.dp)) {
          IconButton(
            enabled = commandUiState !is CommandUiState.Thinking,
            onClick = {
              if (messageDraft.isNotBlank()) {
                onSendMessage(messageDraft)
                messageDraft = ""
              }
            },
          ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
          }
        }
      }
    } else {
      if (memories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(44.dp), tint = NexaBlueCore)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No saved Yaadein yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(memories, key = { it.id }) { mem ->
            NexaGlassCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(mem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text("${mem.category} · ${mem.agentName}", style = MaterialTheme.typography.labelSmall, color = NexaBlueCore)
                }
                IconButton(onClick = { onDeleteMemory(mem.id) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Memory", tint = NexaRoseRadar)
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(mem.content, style = MaterialTheme.typography.bodyMedium)
            }
          }
          item { Spacer(modifier = Modifier.height(104.dp)) }
        }
      }
    }
  }

  if (showAddMemory) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddMemory = false },
      title = { Text("Save to Yaadein Vault") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
          OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Preference / Note / OTP Rule") }, minLines = 3)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank() && body.isNotBlank()) {
              onCreateMemory(title, body)
              showAddMemory = false
            }
          }
        ) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showAddMemory = false }) { Text("Cancel") } },
    )
  }
}
