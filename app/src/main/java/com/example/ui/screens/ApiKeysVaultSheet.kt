package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AiProviderType
import com.example.data.model.LocalAiSecrets
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaRoseRadar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApiKeysVaultSheet(
  secrets: LocalAiSecrets,
  bossName: String,
  onDismiss: () -> Unit,
  onSaveSecrets: (LocalAiSecrets) -> Unit,
  onSaveBossName: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedProvider by remember(secrets) { mutableStateOf(secrets.activeProvider) }
  var geminiKey by remember(secrets) { mutableStateOf(secrets.geminiApiKey) }
  var geminiModel by remember(secrets) { mutableStateOf(secrets.geminiModel) }
  var openAiKey by remember(secrets) { mutableStateOf(secrets.openAiApiKey) }
  var openAiModel by remember(secrets) { mutableStateOf(secrets.openAiModel) }
  var groqKey by remember(secrets) { mutableStateOf(secrets.groqApiKey) }
  var groqModel by remember(secrets) { mutableStateOf(secrets.groqModel) }
  var customUrl by remember(secrets) { mutableStateOf(secrets.customEndpointUrl) }
  var customKey by remember(secrets) { mutableStateOf(secrets.customApiKey) }
  var customModel by remember(secrets) { mutableStateOf(secrets.customModel) }
  var bossDraft by remember(bossName) { mutableStateOf(bossName) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("AI Engine & API Keys Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(
        "Use built-in Gemini Neural Core or plug in your own Gemini, OpenAI, Groq, or custom LLM API key (stored locally on device).",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = bossDraft,
        onValueChange = { bossDraft = it },
        label = { Text("Your Greeting Name (e.g. BOSS)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Active AI Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AiProviderType.entries.forEach { provider ->
          FilterChip(
            selected = selectedProvider == provider.id,
            onClick = { selectedProvider = provider.id },
            label = { Text(provider.displayName) },
          )
        }
      }

      when (AiProviderType.fromId(selectedProvider)) {
        AiProviderType.FIREBASE_GEMINI -> {
          Text(
            "Built-in Gemini 3.8 Flash via Firebase AI Logic is ready — zero API key needed!",
            style = MaterialTheme.typography.bodyMedium,
            color = NexaBlueCore,
          )
        }
        AiProviderType.CUSTOM_GEMINI -> {
          OutlinedTextField(
            value = geminiKey,
            onValueChange = { geminiKey = it },
            label = { Text("Gemini API Key (AIza...)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = geminiModel,
            onValueChange = { geminiModel = it },
            label = { Text("Gemini Model ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        AiProviderType.OPENAI -> {
          OutlinedTextField(
            value = openAiKey,
            onValueChange = { openAiKey = it },
            label = { Text("OpenAI API Key (sk-...)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = openAiModel,
            onValueChange = { openAiModel = it },
            label = { Text("OpenAI Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        AiProviderType.GROQ -> {
          OutlinedTextField(
            value = groqKey,
            onValueChange = { groqKey = it },
            label = { Text("Groq API Key (gsk_...)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = groqModel,
            onValueChange = { groqModel = it },
            label = { Text("Groq Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        AiProviderType.CUSTOM_ENDPOINT -> {
          OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("Chat Completions URL (https://.../v1/chat/completions)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = customKey,
            onValueChange = { customKey = it },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = customModel,
            onValueChange = { customModel = it },
            label = { Text("Model Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onSignOut()
          },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.weight(1f),
        ) {
          Text("Sign Out", color = NexaRoseRadar)
        }
        Button(
          onClick = {
            onSaveSecrets(
              secrets.copy(
                activeProvider = selectedProvider,
                geminiApiKey = geminiKey,
                geminiModel = geminiModel,
                openAiApiKey = openAiKey,
                openAiModel = openAiModel,
                groqApiKey = groqKey,
                groqModel = groqModel,
                customEndpointUrl = customUrl,
                customApiKey = customKey,
                customModel = customModel,
              )
            )
            if (bossDraft.isNotBlank() && bossDraft != bossName) {
              onSaveBossName(bossDraft)
            }
            onDismiss()
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
          modifier = Modifier.weight(1f),
        ) {
          Text("Save Engine", color = Color.White)
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
