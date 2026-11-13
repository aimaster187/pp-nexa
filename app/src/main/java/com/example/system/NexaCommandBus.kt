package com.example.system

import com.example.data.model.AutomationRule
import com.example.data.model.ExecutionLogEntry
import com.example.data.model.LocalAiSecrets
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NexaCommandBus {
  private val _incomingVoiceCommands = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val incomingVoiceCommands: SharedFlow<String> = _incomingVoiceCommands.asSharedFlow()

  private val _wakeWordStatus = MutableStateFlow("Standby")
  val wakeWordStatus: StateFlow<String> = _wakeWordStatus.asStateFlow()

  private val _wakeServiceRunning = MutableStateFlow(false)
  val wakeServiceRunning: StateFlow<Boolean> = _wakeServiceRunning.asStateFlow()

  private val _lastDetectedOtp = MutableStateFlow<String?>(null)
  val lastDetectedOtp: StateFlow<String?> = _lastDetectedOtp.asStateFlow()

  private val _logs = MutableStateFlow<List<ExecutionLogEntry>>(emptyList())
  val logs: StateFlow<List<ExecutionLogEntry>> = _logs.asStateFlow()

  @Volatile var activeRulesSnapshot: List<AutomationRule> = emptyList()
  @Volatile var latestSecretsSnapshot: LocalAiSecrets = LocalAiSecrets()
  @Volatile var accessibilityAgentControlEnabled: Boolean = true

  fun emitVoiceCommand(command: String) {
    if (command.isNotBlank()) {
      _incomingVoiceCommands.tryEmit(command.trim())
    }
  }

  fun updateWakeStatus(status: String, running: Boolean? = null) {
    _wakeWordStatus.value = status
    if (running != null) _wakeServiceRunning.value = running
  }

  fun recordOtp(otp: String) {
    _lastDetectedOtp.value = otp
  }

  fun addLog(source: String, title: String, detail: String, status: String = "SUCCESS") {
    val entry =
      ExecutionLogEntry(
        id = UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis(),
        source = source,
        title = title,
        detail = detail,
        status = status,
      )
    _logs.update { current -> (listOf(entry) + current).take(60) }
  }
}
