package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import com.example.data.model.TranscriptEntry
import com.example.system.AudioRecorderHelper
import com.example.ui.TranscriptionUiState
import com.example.ui.components.NexaGlassCard
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaCyanGlow
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaRoseRadar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioTranscribeScreen(
  transcriptionState: TranscriptionUiState,
  history: List<TranscriptEntry>,
  onTranscribeAudio: (audioBytes: ByteArray, durationSec: Int, languageHint: String) -> Unit,
  onReportError: (String) -> Unit,
  onSaveToMemory: (String) -> Unit,
  onRunAsCommand: (String) -> Unit,
  onRefreshPermissions: () -> Unit,
) {
  val context = LocalContext.current
  val recorder = remember(context) { AudioRecorderHelper(context.applicationContext) }
  var isRecording by remember { mutableStateOf(false) }
  var elapsedSec by remember { mutableIntStateOf(0) }
  var liveAmplitude by remember { mutableFloatStateOf(0f) }
  var selectedLanguage by remember { mutableStateOf("Auto") }
  var copiedNotice by remember { mutableStateOf(false) }

  DisposableEffect(recorder) {
    onDispose {
      recorder.release()
    }
  }

  LaunchedEffect(isRecording) {
    if (isRecording) {
      elapsedSec = 0
      var ticks = 0
      while (isRecording) {
        liveAmplitude = recorder.currentAmplitudeNormalized()
        delay(125L)
        ticks++
        if (ticks % 8 == 0) {
          elapsedSec++
        }
      }
    } else {
      liveAmplitude = 0f
    }
  }

  fun beginRecording() {
    try {
      copiedNotice = false
      recorder.startRecording()
      isRecording = true
    } catch (e: Exception) {
      isRecording = false
      onReportError("Microphone unavailable: ${e.localizedMessage ?: "Failed to start recording"}")
    }
  }

  fun stopAndTranscribe() {
    val recordedDuration = elapsedSec.coerceAtLeast(1)
    isRecording = false
    val bytes = recorder.stopAndReadBytes()
    onTranscribeAudio(bytes, recordedDuration, selectedLanguage)
  }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      onRefreshPermissions()
      if (granted) {
        beginRecording()
      } else {
        onReportError("Microphone permission is required to record and transcribe audio.")
      }
    }

  val recordOrbScale by
    animateFloatAsState(
      targetValue = if (isRecording) 1.08f + liveAmplitude * 0.14f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "transcribe_orb_scale",
    )

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth()) {
        Text(
          text = "Audio Transcription",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Speak in Hindi, Hinglish, or English · Powered by gemini-3.5-transcribe",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      NexaGlassCard(modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth()) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf("Auto", "English", "Hindi", "Hinglish").forEach { lang ->
              FilterChip(
                selected = selectedLanguage == lang,
                onClick = { if (!isRecording) selectedLanguage = lang },
                label = { Text(lang) },
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Animated 3D Microphone Studio Button
          Box(
            modifier =
              Modifier
                .size(128.dp)
                .graphicsLayer {
                  scaleX = recordOrbScale
                  scaleY = recordOrbScale
                },
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier
                  .size(122.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      colors =
                        listOf(
                          (if (isRecording) NexaRoseRadar else NexaBlueCore).copy(alpha = 0.30f),
                          Color.Transparent,
                        )
                    )
                  )
            )
            Surface(
              onClick = {
                if (isRecording) {
                  stopAndTranscribe()
                } else {
                  val granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                      PackageManager.PERMISSION_GRANTED
                  if (granted) {
                    beginRecording()
                  } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
              },
              shape = CircleShape,
              color = Color.Transparent,
              modifier =
                Modifier
                  .size(92.dp)
                  .shadow(14.dp, CircleShape)
                  .testTag("transcribe_record_button"),
            ) {
              Box(
                modifier =
                  Modifier
                    .fillMaxSize()
                    .background(
                      Brush.linearGradient(
                        if (isRecording) listOf(NexaRoseRadar, Color(0xFFE11D48))
                        else listOf(NexaCyanGlow, NexaBlueCore)
                      )
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                  contentDescription = if (isRecording) "Stop and Transcribe" else "Record Audio",
                  tint = Color.White,
                  modifier = Modifier.size(38.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Live Waveform Meter Bars
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(28.dp),
          ) {
            for (bar in 0 until 15) {
              val weightFactor = 1f - kotlin.math.abs(bar - 7) / 8f
              val heightDp =
                if (isRecording) (6f + 20f * liveAmplitude * weightFactor).dp
                else 6.dp
              Box(
                modifier =
                  Modifier
                    .width(4.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isRecording) NexaRoseRadar else NexaBlueCore.copy(alpha = 0.35f))
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              if (isRecording) {
                val mins = elapsedSec / 60
                val secs = elapsedSec % 60
                "Recording ${"%02d:%02d".format(mins, secs)} — Tap Stop to Transcribe"
              } else {
                "Tap microphone to record and transcribe live"
              },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isRecording) NexaRoseRadar else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    // Live Streaming / Result / Error Card
    if (transcriptionState !is TranscriptionUiState.Idle) {
      item {
        NexaGlassCard(
          modifier =
            Modifier
              .widthIn(max = 580.dp)
              .fillMaxWidth()
              .testTag("transcribe_result_card"),
        ) {
          when (transcriptionState) {
            is TranscriptionUiState.Loading -> {
              Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  "Transcribing audio with gemini-3.5-transcribe...",
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
            is TranscriptionUiState.Streaming -> {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = NexaBlueCore, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Streaming Transcript...",
                  style = MaterialTheme.typography.labelLarge,
                  color = NexaBlueCore,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(transcriptionState.partialText, style = MaterialTheme.typography.bodyLarge)
            }
            is TranscriptionUiState.Success -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NexaEmeraldOk, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    "Transcript Ready",
                    style = MaterialTheme.typography.labelLarge,
                    color = NexaEmeraldOk,
                  )
                }
                if (copiedNotice) {
                  Text("Copied", style = MaterialTheme.typography.labelSmall, color = NexaBlueCore)
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = transcriptionState.transcript,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("transcribe_output_text"),
              )
              Spacer(modifier = Modifier.height(14.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(
                      ClipData.newPlainText("NEXA Transcript", transcriptionState.transcript)
                    )
                    copiedNotice = true
                  },
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Copy")
                }
                OutlinedButton(
                  onClick = { onSaveToMemory(transcriptionState.transcript) },
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Save Yaadein")
                }
                Button(
                  onClick = { onRunAsCommand(transcriptionState.transcript) },
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
                ) {
                  Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Run Command", color = Color.White)
                }
              }
            }
            is TranscriptionUiState.Error -> {
              Text(
                text = transcriptionState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }
            TranscriptionUiState.Idle -> Unit
          }
        }
      }
    }

    if (history.isNotEmpty()) {
      item {
        Text(
          text = "Recent Transcriptions (${history.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth().padding(top = 6.dp),
        )
      }
      items(history, key = { it.id }) { item ->
        val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        NexaGlassCard(modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${item.languageHint} · ${item.durationSec}s",
              style = MaterialTheme.typography.labelSmall,
              color = NexaBlueCore,
            )
            Text(
              text = fmt.format(Date(item.timestampMs)),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(item.text, style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onSaveToMemory(item.text) },
              shape = RoundedCornerShape(12.dp),
            ) {
              Text("Save")
            }
            Button(
              onClick = { onRunAsCommand(item.text) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
            ) {
              Text("Run", color = Color.White)
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(104.dp)) }
  }
}
