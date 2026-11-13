package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiBrainRouter
import com.example.ai.LocalConfigStore
import com.example.data.model.AutomationRule
import com.example.data.model.ChatTurn
import com.example.data.model.CustomAgent
import com.example.data.model.DevicePermissionsState
import com.example.data.model.ExecutionLogEntry
import com.example.data.model.LocalAiSecrets
import com.example.data.model.MemoryEntry
import com.example.data.model.PhoneActionStep
import com.example.data.model.TranscriptEntry
import com.example.data.model.UserProfile
import com.example.data.repository.NexaRepository
import com.example.system.NexaAccessibilityService
import com.example.system.NexaCommandBus
import com.example.system.NexaWakeWordService
import com.example.system.PermissionsHelper
import com.example.system.PhoneController
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CommandUiState {
  data object Idle : CommandUiState
  data class Thinking(val label: String) : CommandUiState
  data class Success(val message: String) : CommandUiState
  data class Error(val message: String) : CommandUiState
}

sealed interface TranscriptionUiState {
  data object Idle : TranscriptionUiState
  data object Loading : TranscriptionUiState
  data class Streaming(val partialText: String) : TranscriptionUiState
  data class Success(val transcript: String) : TranscriptionUiState
  data class Error(val message: String) : TranscriptionUiState
}

class NexaViewModel(
  private val repository: NexaRepository,
  private val currentUserId: String,
  private val localConfigStore: LocalConfigStore,
  private val phoneController: PhoneController,
  private val aiBrainRouter: AiBrainRouter = AiBrainRouter(),
) : ViewModel() {

  val userProfile: StateFlow<UserProfile?> =
    repository
      .observeUserProfile(currentUserId)
      .catch { e -> unwrapAndLog("observeUserProfile", e); emit(null) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val agents: StateFlow<List<CustomAgent>> =
    repository
      .observeAgents(currentUserId)
      .catch { e -> unwrapAndLog("observeAgents", e); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val automations: StateFlow<List<AutomationRule>> =
    repository
      .observeAutomations(currentUserId)
      .map { list ->
        NexaCommandBus.activeRulesSnapshot = list
        list
      }
      .catch { e -> unwrapAndLog("observeAutomations", e); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val memories: StateFlow<List<MemoryEntry>> =
    repository
      .observeMemories(currentUserId)
      .catch { e -> unwrapAndLog("observeMemories", e); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val secrets: StateFlow<LocalAiSecrets> =
    localConfigStore.configFlow
      .map { cfg ->
        NexaCommandBus.latestSecretsSnapshot = cfg
        NexaCommandBus.accessibilityAgentControlEnabled = cfg.accessibilityAgentControlEnabled
        cfg
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAiSecrets())

  val logs: StateFlow<List<ExecutionLogEntry>> = NexaCommandBus.logs
  val wakeStatus: StateFlow<String> = NexaCommandBus.wakeWordStatus
  val wakeRunning: StateFlow<Boolean> = NexaCommandBus.wakeServiceRunning
  val lastOtp: StateFlow<String?> = NexaCommandBus.lastDetectedOtp

  private val _chatMessages =
    MutableStateFlow(
      listOf(
        ChatTurn(
          id = "welcome_nexa",
          isUser = false,
          text =
            "Namaste Boss! Main ZED AI hoon — aapka 3D autonomous phone-control agent. Mujhe voice ya text command dein: 'WhatsApp open karo', 'Play Store se Telegram install karo', ya apna custom agent banayein.",
          agentName = "ZED Core",
        )
      )
    )
  val chatMessages: StateFlow<List<ChatTurn>> = _chatMessages.asStateFlow()

  private val _commandUiState = MutableStateFlow<CommandUiState>(CommandUiState.Idle)
  val commandUiState: StateFlow<CommandUiState> = _commandUiState.asStateFlow()

  private val _selectedAgentId = MutableStateFlow<String?>(null)
  val selectedAgentId: StateFlow<String?> = _selectedAgentId.asStateFlow()

  private val _permissionsState = MutableStateFlow(DevicePermissionsState())
  val permissionsState: StateFlow<DevicePermissionsState> = _permissionsState.asStateFlow()

  private val _transcriptionState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
  val transcriptionState: StateFlow<TranscriptionUiState> = _transcriptionState.asStateFlow()

  private val _transcriptionHistory = MutableStateFlow<List<TranscriptEntry>>(emptyList())
  val transcriptionHistory: StateFlow<List<TranscriptEntry>> = _transcriptionHistory.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        repository.ensureUserProfile("BOSS")
        repository.seedStarterWorkspaceIfEmpty()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Workspace initialization error", e)
      }
    }

    viewModelScope.launch {
      NexaCommandBus.incomingVoiceCommands.collectLatest { voiceCmd ->
        sendCommand(voiceCmd)
      }
    }
  }

  fun refreshPermissions(context: Context) {
    _permissionsState.value = PermissionsHelper.readAllStates(context)
  }

  fun selectAgent(agentId: String?) {
    _selectedAgentId.value = agentId
  }

  fun sendCommand(
    rawPrompt: String,
    overrideAgent: CustomAgent? = null,
    imageBytes: ByteArray? = null,
    imageMimeType: String = "image/jpeg",
  ) {
    val prompt = rawPrompt.trim().ifEmpty {
      if (imageBytes != null && imageBytes.isNotEmpty()) "Analyze this screenshot/image and suggest or take the next best action."
      else ""
    }
    if (prompt.isEmpty()) return

    val activeAgent =
      overrideAgent ?: agents.value.firstOrNull { it.id == _selectedAgentId.value }
    val displayPrompt =
      if (imageBytes != null && imageBytes.isNotEmpty()) "[Vision Image] $prompt" else prompt
    val userTurn =
      ChatTurn(
        id = UUID.randomUUID().toString(),
        isUser = true,
        text = displayPrompt,
        agentName = activeAgent?.name ?: "Boss",
      )
    _chatMessages.update { it + userTurn }
    NexaCommandBus.addLog("USER", "Command Received", displayPrompt)

    viewModelScope.launch {
      _commandUiState.value =
        CommandUiState.Thinking(
          when {
            imageBytes != null -> "ZED Vision analyzing image..."
            activeAgent != null -> "${activeAgent.name} thinking..."
            else -> "ZED Neural Core planning..."
          }
        )
      try {
        val screenSnapshot = NexaAccessibilityService.instance?.readCurrentScreenSnapshot().orEmpty()
        val currentSecrets = secrets.value
        val recentHistory =
          _chatMessages.value
            .takeLast(5)
            .joinToString("\n") { "${if (it.isUser) "Boss" else it.agentName}: ${it.text.take(180)}" }
        val memoriesContext =
          buildString {
            lastOtp.value?.let { otp -> appendLine("Latest detected OTP: $otp") }
            memories.value.take(6).forEach { mem ->
              appendLine("- ${mem.title}: ${mem.content.take(140)}")
            }
          }
        val plan =
          aiBrainRouter.planCommand(
            userPrompt = prompt,
            activeAgent = activeAgent,
            screenContext = screenSnapshot,
            secrets = currentSecrets,
            recentHistory = recentHistory,
            memoriesContext = memoriesContext,
            imageBytes = imageBytes,
            imageMimeType = imageMimeType,
          )

        val execSummary =
          if (plan.steps.isNotEmpty()) {
            _commandUiState.value = CommandUiState.Thinking("Executing ${plan.steps.size} phone steps...")
            phoneController.executeSteps(plan.steps)
          } else {
            ""
          }

        val replyTurn =
          ChatTurn(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = plan.spokenReply,
            agentName = activeAgent?.name ?: plan.providerUsed,
            plannedSteps = plan.steps,
            executionStatus = execSummary,
          )
        _chatMessages.update { it + replyTurn }
        _commandUiState.value = CommandUiState.Success(plan.spokenReply)

        if (currentSecrets.ttsVoiceReplyEnabled) {
          phoneController.speak(plan.spokenReply)
        }

        if (plan.memoryNote.isNotBlank()) {
          runCatching {
            repository.createMemory(
              MemoryEntry(
                title = prompt.take(48),
                content = plan.memoryNote,
                category = "MEMORY",
                agentName = activeAgent?.name ?: "ZED Core",
              )
            )
          }
        }

        if (activeAgent != null && activeAgent.id.isNotBlank()) {
          runCatching {
            repository.updateAgent(activeAgent.copy(runCount = activeAgent.runCount + 1L))
          }
        }
        userProfile.value?.let { prof ->
          runCatching {
            repository.updateUserProfileFields(energyPoints = prof.energyPoints + 1L)
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI planning timed out", e)
        appendErrorTurn("Request timed out after 60s. Please retry.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI planning failed", e)
        appendErrorTurn(e.localizedMessage ?: "Failed to process command.")
      }
    }
  }

  fun rerunSteps(steps: List<PhoneActionStep>) {
    if (steps.isEmpty()) return
    viewModelScope.launch {
      _commandUiState.value = CommandUiState.Thinking("Re-executing ${steps.size} steps...")
      val summary = phoneController.executeSteps(steps)
      _commandUiState.value = CommandUiState.Success(summary)
    }
  }

  private fun appendErrorTurn(message: String) {
    _commandUiState.value = CommandUiState.Error(message)
    _chatMessages.update {
      it +
        ChatTurn(
          id = UUID.randomUUID().toString(),
          isUser = false,
          text = "Error: $message",
          agentName = "ZED Diagnostics",
        )
    }
  }

  fun createAgent(
    name: String,
    role: String,
    systemPrompt: String,
    triggerType: String,
    allowedTools: List<String>,
    autoExecute: Boolean,
    accentHex: String,
  ) {
    viewModelScope.launch {
      runCatching {
        repository.createAgent(
          CustomAgent(
            name = name,
            role = role,
            systemPrompt = systemPrompt,
            triggerType = triggerType,
            allowedTools = allowedTools,
            isEnabled = true,
            autoExecute = autoExecute,
            accentColorHex = accentHex,
          )
        )
      }.onFailure { e -> _commandUiState.value = CommandUiState.Error(e.localizedMessage ?: "Agent create failed") }
    }
  }

  fun toggleAgentEnabled(agent: CustomAgent) {
    viewModelScope.launch {
      runCatching { repository.updateAgent(agent.copy(isEnabled = !agent.isEnabled)) }
    }
  }

  fun deleteAgent(agentId: String) {
    viewModelScope.launch { runCatching { repository.deleteAgent(agentId) } }
  }

  fun createAutomationRule(
    title: String,
    targetApp: String,
    triggerPattern: String,
    replyGoal: String,
    useAiReply: Boolean,
    requireConfirmation: Boolean,
  ) {
    viewModelScope.launch {
      runCatching {
        repository.createAutomation(
          AutomationRule(
            title = title,
            targetApp = targetApp,
            triggerPattern = triggerPattern,
            replyTemplateOrGoal = replyGoal,
            useAiReply = useAiReply,
            isActive = true,
            requireConfirmation = requireConfirmation,
          )
        )
      }.onFailure { e -> _commandUiState.value = CommandUiState.Error(e.localizedMessage ?: "Rule create failed") }
    }
  }

  fun toggleAutomationRule(rule: AutomationRule) {
    viewModelScope.launch {
      runCatching { repository.updateAutomation(rule.copy(isActive = !rule.isActive)) }
    }
  }

  fun deleteAutomationRule(ruleId: String) {
    viewModelScope.launch { runCatching { repository.deleteAutomation(ruleId) } }
  }

  fun createMemory(title: String, content: String, category: String = "MEMORY") {
    viewModelScope.launch {
      runCatching {
        repository.createMemory(
          MemoryEntry(
            title = title,
            content = content,
            category = category,
            agentName = "Boss",
          )
        )
      }
    }
  }

  fun deleteMemory(memoryId: String) {
    viewModelScope.launch { runCatching { repository.deleteMemory(memoryId) } }
  }

  fun saveLocalSecrets(updated: LocalAiSecrets, context: Context) {
    viewModelScope.launch {
      localConfigStore.saveConfig(updated)
      NexaCommandBus.latestSecretsSnapshot = updated
      runCatching { repository.updateUserProfileFields(activeProvider = updated.activeProvider) }
      if (updated.wakeWordListeningEnabled) {
        startWakeWordService(context, updated.floatingOrbEnabled)
      } else {
        stopWakeWordService(context)
      }
    }
  }

  fun updateBossName(newName: String) {
    viewModelScope.launch {
      runCatching { repository.updateUserProfileFields(bossName = newName) }
    }
  }

  fun startWakeWordService(context: Context, showOrb: Boolean) {
    val intent =
      Intent(context, NexaWakeWordService::class.java).apply {
        action = NexaWakeWordService.ACTION_START
        putExtra(NexaWakeWordService.EXTRA_SHOW_ORB, showOrb)
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }

  fun transcribeAudioBytes(
    audioBytes: ByteArray,
    durationSec: Int,
    languageHint: String = "Auto",
  ) {
    if (audioBytes.isEmpty()) {
      _transcriptionState.value =
        TranscriptionUiState.Error("Recorded audio clip was empty. Hold or tap to speak clearly and try again.")
      return
    }
    viewModelScope.launch {
      _transcriptionState.value = TranscriptionUiState.Loading
      var latestText = ""
      try {
        aiBrainRouter
          .transcribeAudioStream(
            audioBytes = audioBytes,
            mimeType = "audio/mp4",
            languageHint = languageHint,
          )
          .collect { accumulated ->
            latestText = accumulated
            _transcriptionState.value = TranscriptionUiState.Streaming(accumulated)
          }
        val finalTranscript = latestText.trim()
        if (finalTranscript.isNotBlank()) {
          _transcriptionState.value = TranscriptionUiState.Success(finalTranscript)
          val entry =
            TranscriptEntry(
              id = UUID.randomUUID().toString(),
              text = finalTranscript,
              languageHint = languageHint,
              durationSec = durationSec,
            )
          _transcriptionHistory.update { listOf(entry) + it.take(29) }
          NexaCommandBus.addLog(
            "TRANSCRIBE",
            "Audio Transcribed (${durationSec}s)",
            finalTranscript.take(120),
          )
        } else {
          _transcriptionState.value =
            TranscriptionUiState.Error("gemini-3.5-transcribe returned an empty transcript.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Audio transcription timed out", e)
        _transcriptionState.value =
          TranscriptionUiState.Error("Transcription timed out after 60 seconds. Please try a shorter clip.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Audio transcription failed", e)
        _transcriptionState.value =
          TranscriptionUiState.Error(e.localizedMessage ?: "Audio transcription failed.")
      }
    }
  }

  suspend fun transcribeForVoiceTrigger(audioBytes: ByteArray): String {
    require(audioBytes.isNotEmpty()) { "Audio recording was empty." }
    var latestText = ""
    aiBrainRouter
      .transcribeAudioStream(audioBytes = audioBytes, mimeType = "audio/mp4", languageHint = "Auto")
      .collect { accumulated -> latestText = accumulated }
    return latestText.trim()
  }

  fun setTranscriptionError(message: String) {
    _transcriptionState.value = TranscriptionUiState.Error(message)
  }

  fun resetTranscriptionState() {
    _transcriptionState.value = TranscriptionUiState.Idle
  }

  fun stopWakeWordService(context: Context) {
    val intent =
      Intent(context, NexaWakeWordService::class.java).apply {
        action = NexaWakeWordService.ACTION_STOP
      }
    context.startService(intent)
  }

  private fun unwrapAndLog(source: String, error: Throwable) {
    val fsErr =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (fsErr == null && error is CancellationException) throw error
    Log.w(TAG, "Error in $source", fsErr ?: error)
  }

  override fun onCleared() {
    phoneController.shutdown()
    super.onCleared()
  }

  private companion object {
    const val TAG = "NexaViewModel"
  }
}
