package com.example.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.system.AudioRecorderHelper
import com.example.system.NexaCommandBus
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaCyanGlow
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VoiceTriggerPhase {
  SPEECH_LISTENING,
  AUDIO_RECORDING,
  TRANSCRIBING,
  READY_OR_IDLE,
  ERROR,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandTriggerDialog(
  targetAgentName: String,
  onTranscribeAudio: suspend (ByteArray) -> String,
  onExecuteCommand: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val recorder = remember(context) { AudioRecorderHelper(context.applicationContext) }

  var phase by remember { mutableStateOf(VoiceTriggerPhase.READY_OR_IDLE) }
  var transcriptText by remember { mutableStateOf("") }
  var statusMessage by remember { mutableStateOf("Initializing microphone...") }
  var rmsLevel by remember { mutableFloatStateOf(0f) }
  var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
  var hasDispatched by remember { mutableStateOf(false) }

  fun dispatchCommand(command: String) {
    val clean = command.trim()
    if (clean.isBlank() || hasDispatched) return
    hasDispatched = true
    NexaCommandBus.addLog("VOICE", "Voice Trigger -> $targetAgentName", clean)
    onExecuteCommand(clean)
    onDismiss()
  }

  fun startGeminiAudioFallback() {
    try {
      speechRecognizer?.cancel()
      speechRecognizer?.destroy()
    } catch (_: Exception) {
    }
    speechRecognizer = null
    try {
      recorder.startRecording()
      phase = VoiceTriggerPhase.AUDIO_RECORDING
      statusMessage = "Listening via Gemini 3.5 Transcribe Mic... Speak naturally"
    } catch (e: Exception) {
      phase = VoiceTriggerPhase.ERROR
      statusMessage = "Microphone unavailable: ${e.localizedMessage ?: "Permission or hardware error"}"
    }
  }

  fun stopGeminiAudioAndTrigger() {
    if (phase != VoiceTriggerPhase.AUDIO_RECORDING) return
    val bytes = recorder.stopAndReadBytes()
    if (bytes.isEmpty()) {
      phase = VoiceTriggerPhase.ERROR
      statusMessage = "No audio captured. Please try speaking again."
      return
    }
    phase = VoiceTriggerPhase.TRANSCRIBING
    statusMessage = "Converting speech to command via gemini-3.5-transcribe..."
    scope.launch {
      try {
        val text = onTranscribeAudio(bytes)
        if (text.isNotBlank()) {
          transcriptText = text
          dispatchCommand(text)
        } else {
          phase = VoiceTriggerPhase.ERROR
          statusMessage = "Could not hear clear speech. Edit or try again."
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("VoiceTrigger", "Gemini transcription trigger failed", e)
        phase = VoiceTriggerPhase.ERROR
        statusMessage = e.localizedMessage ?: "Voice transcription failed."
      }
    }
  }

  fun startSpeechRecognizerOrFallback() {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      startGeminiAudioFallback()
      return
    }
    try {
      speechRecognizer?.destroy()
      val sr = SpeechRecognizer.createSpeechRecognizer(context)
      speechRecognizer = sr
      sr.setRecognitionListener(
        object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            phase = VoiceTriggerPhase.SPEECH_LISTENING
            statusMessage = "Speak naturally to $targetAgentName..."
          }

          override fun onBeginningOfSpeech() {
            statusMessage = "Listening..."
          }

          override fun onRmsChanged(rmsdB: Float) {
            rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
          }

          override fun onBufferReceived(buffer: ByteArray?) = Unit

          override fun onEndOfSpeech() {
            statusMessage = "Processing command..."
          }

          override fun onError(error: Int) {
            rmsLevel = 0f
            when (error) {
              SpeechRecognizer.ERROR_CLIENT,
              SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
              SpeechRecognizer.ERROR_SERVER,
              SpeechRecognizer.ERROR_NETWORK,
              SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                startGeminiAudioFallback()
              }
              SpeechRecognizer.ERROR_NO_MATCH,
              SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                phase = VoiceTriggerPhase.READY_OR_IDLE
                statusMessage = "Didn't catch speech — tap mic to retry or switch to Gemini 3.5 Mic."
              }
              else -> {
                phase = VoiceTriggerPhase.ERROR
                statusMessage = "Speech recognition error ($error). Try Gemini 3.5 Mic below."
              }
            }
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            if (best.isNotBlank()) {
              transcriptText = best
              dispatchCommand(best)
            } else {
              phase = VoiceTriggerPhase.READY_OR_IDLE
              statusMessage = "No speech detected. Tap to speak again."
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val partial =
              partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (!partial.isNullOrBlank()) {
              transcriptText = partial
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
      )

      val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
      phase = VoiceTriggerPhase.SPEECH_LISTENING
      statusMessage = "Speak naturally to $targetAgentName..."
      sr.startListening(intent)
    } catch (_: Exception) {
      startGeminiAudioFallback()
    }
  }

  LaunchedEffect(Unit) {
    startSpeechRecognizerOrFallback()
  }

  LaunchedEffect(phase) {
    if (phase == VoiceTriggerPhase.AUDIO_RECORDING) {
      while (phase == VoiceTriggerPhase.AUDIO_RECORDING) {
        rmsLevel = recorder.currentAmplitudeNormalized()
        delay(100L)
      }
    } else if (phase != VoiceTriggerPhase.SPEECH_LISTENING) {
      rmsLevel = 0f
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      try {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
      } catch (_: Exception) {
      }
      recorder.release()
    }
  }

  val micScale by
    animateFloatAsState(
      targetValue =
        if (phase == VoiceTriggerPhase.SPEECH_LISTENING || phase == VoiceTriggerPhase.AUDIO_RECORDING)
          1.05f + rmsLevel * 0.18f
        else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "voice_trigger_mic_scale",
    )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      shadowElevation = 20.dp,
      border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.85f)),
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .testTag("voice_trigger_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexaBlueCore.copy(alpha = 0.12f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Agent: $targetAgentName",
                style = MaterialTheme.typography.labelLarge,
                color = NexaBlueCore,
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Voice Trigger")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animated 3D Mic Trigger Orb
        Box(
          modifier =
            Modifier
              .size(112.dp)
              .graphicsLayer {
                scaleX = micScale
                scaleY = micScale
              },
          contentAlignment = Alignment.Center,
        ) {
          val activeColor =
            when (phase) {
              VoiceTriggerPhase.SPEECH_LISTENING,
              VoiceTriggerPhase.AUDIO_RECORDING -> NexaRoseRadar
              VoiceTriggerPhase.TRANSCRIBING -> NexaCyanGlow
              else -> NexaBlueCore
            }
          Box(
            modifier =
              Modifier
                .size(106.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(activeColor.copy(alpha = 0.28f), Color.Transparent)))
          )
          Surface(
            onClick = {
              when (phase) {
                VoiceTriggerPhase.SPEECH_LISTENING -> {
                  speechRecognizer?.stopListening()
                }
                VoiceTriggerPhase.AUDIO_RECORDING -> stopGeminiAudioAndTrigger()
                VoiceTriggerPhase.TRANSCRIBING -> Unit
                else -> startSpeechRecognizerOrFallback()
              }
            },
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(80.dp).shadow(12.dp, CircleShape).testTag("voice_trigger_stop_run_button"),
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .background(
                    Brush.linearGradient(
                      when (phase) {
                        VoiceTriggerPhase.SPEECH_LISTENING,
                        VoiceTriggerPhase.AUDIO_RECORDING -> listOf(NexaRoseRadar, Color(0xFFE11D48))
                        else -> listOf(NexaCyanGlow, NexaBlueCore)
                      }
                    )
                  ),
              contentAlignment = Alignment.Center,
            ) {
              when (phase) {
                VoiceTriggerPhase.TRANSCRIBING -> {
                  CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
                }
                VoiceTriggerPhase.AUDIO_RECORDING,
                VoiceTriggerPhase.SPEECH_LISTENING -> {
                  Icon(Icons.Rounded.Stop, contentDescription = "Stop Listening and Run", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                else -> {
                  Icon(Icons.Rounded.Mic, contentDescription = "Start Voice Trigger", tint = Color.White, modifier = Modifier.size(34.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = statusMessage,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("voice_trigger_status"),
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = transcriptText,
          onValueChange = { transcriptText = it },
          label = { Text("Recognized natural language command") },
          placeholder = { Text("e.g. Open WhatsApp and reply, or Read my screen") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("voice_trigger_transcript_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick natural-language trigger chips
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("Read my screen", "WhatsApp open karo", "Open Gmail").forEach { sample ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.clickable { dispatchCommand(sample) },
            ) {
              Text(
                text = sample,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              if (phase == VoiceTriggerPhase.AUDIO_RECORDING) {
                stopGeminiAudioAndTrigger()
              } else {
                startGeminiAudioFallback()
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (phase == VoiceTriggerPhase.AUDIO_RECORDING) "Finish Mic" else "Gemini 3.5 Mic",
              maxLines = 1,
            )
          }
          Button(
            onClick = {
              when {
                phase == VoiceTriggerPhase.AUDIO_RECORDING && transcriptText.isBlank() -> {
                  stopGeminiAudioAndTrigger()
                }
                transcriptText.isNotBlank() -> {
                  dispatchCommand(transcriptText)
                }
                else -> startSpeechRecognizerOrFallback()
              }
            },
            enabled = phase != VoiceTriggerPhase.TRANSCRIBING,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexaEmeraldOk),
            modifier = Modifier.weight(1f).testTag("voice_trigger_run_button"),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Run Agent", color = Color.White)
          }
        }
      }
    }
  }
}
