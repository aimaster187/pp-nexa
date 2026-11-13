package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AiProviderType(val id: String, val displayName: String, val subtitle: String) {
  FIREBASE_GEMINI("FIREBASE_GEMINI", "Gemini Neural Core", "Built-in Firebase AI Logic (gemini-3.8-flash)"),
  CUSTOM_GEMINI("CUSTOM_GEMINI", "Gemini API Key", "Direct Google AI Studio REST key"),
  OPENAI("OPENAI", "OpenAI GPT", "Custom OpenAI API Key (gpt-4o-mini)"),
  GROQ("GROQ", "Groq Llama", "Ultra-fast Groq Cloud API Key"),
  CUSTOM_ENDPOINT("CUSTOM_ENDPOINT", "Custom LLM Endpoint", "Any OpenAI-compatible base URL + key");

  companion object {
    fun fromId(id: String): AiProviderType = entries.firstOrNull { it.id == id } ?: FIREBASE_GEMINI
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bossName: String = "BOSS",
  val wakeWords: List<String> = listOf("hey zed", "hello zed", "nexa", "alexa"),
  val activeProvider: String = AiProviderType.FIREBASE_GEMINI.id,
  val energyPoints: Long = 100L,
  val moodStatus: String = "Neural Core Active",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomAgent(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val role: String = "",
  val systemPrompt: String = "",
  val triggerType: String = "MANUAL",
  val allowedTools: List<String> = listOf("OPEN_APP", "TAP_TEXT", "AUTO_REPLY"),
  val isEnabled: Boolean = true,
  val autoExecute: Boolean = false,
  val runCount: Long = 0L,
  val accentColorHex: String = "#38BDF8",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AutomationRule(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val targetApp: String = "WHATSAPP",
  val triggerPattern: String = "*",
  val replyTemplateOrGoal: String = "",
  val useAiReply: Boolean = true,
  val isActive: Boolean = true,
  val requireConfirmation: Boolean = true,
  val triggeredCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MemoryEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val content: String = "",
  val category: String = "MEMORY",
  val agentName: String = "ZED Core",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class PhoneActionStep(
  val action: String,
  val target: String = "",
  val value: String = "",
  val description: String = "",
)

data class AiCommandPlan(
  val spokenReply: String,
  val steps: List<PhoneActionStep> = emptyList(),
  val memoryNote: String = "",
  val providerUsed: String = "Gemini",
)

data class ChatTurn(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val agentName: String = "ZED AI",
  val plannedSteps: List<PhoneActionStep> = emptyList(),
  val executionStatus: String = "",
  val timestampMs: Long = System.currentTimeMillis(),
)

data class ExecutionLogEntry(
  val id: String,
  val timestampMs: Long,
  val source: String,
  val title: String,
  val detail: String,
  val status: String,
)

data class TranscriptEntry(
  val id: String,
  val text: String,
  val languageHint: String = "Auto",
  val durationSec: Int = 0,
  val timestampMs: Long = System.currentTimeMillis(),
)

data class LocalAiSecrets(
  val activeProvider: String = AiProviderType.FIREBASE_GEMINI.id,
  val geminiApiKey: String = "",
  val geminiModel: String = "gemini-2.5-flash",
  val openAiApiKey: String = "",
  val openAiModel: String = "gpt-4o-mini",
  val groqApiKey: String = "",
  val groqModel: String = "llama-3.3-70b-versatile",
  val customEndpointUrl: String = "",
  val customApiKey: String = "",
  val customModel: String = "",
  val wakeWordListeningEnabled: Boolean = false,
  val ttsVoiceReplyEnabled: Boolean = true,
  val floatingOrbEnabled: Boolean = false,
  val accessibilityAgentControlEnabled: Boolean = true,
  val wakeWordsCsv: String = "hey zed, hello zed, nexa, alexa",
)

data class DevicePermissionsState(
  val accessibilityEnabled: Boolean = false,
  val notificationListenerEnabled: Boolean = false,
  val overlayEnabled: Boolean = false,
  val micGranted: Boolean = false,
  val notificationsGranted: Boolean = false,
  val callPhoneGranted: Boolean = false,
  val batteryOptimizationIgnored: Boolean = false,
)
