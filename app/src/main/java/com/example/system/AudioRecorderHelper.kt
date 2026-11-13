package com.example.system

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {

  private var recorder: MediaRecorder? = null
  private var outputFile: File? = null

  fun startRecording() {
    release()
    val file = File(context.cacheDir, "nexa_audio_${System.currentTimeMillis()}.m4a")
    outputFile = file
    val mediaRecorder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
    mediaRecorder.apply {
      setAudioSource(MediaRecorder.AudioSource.MIC)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      setAudioSamplingRate(16000)
      setAudioEncodingBitRate(64000)
      setOutputFile(file.absolutePath)
      prepare()
      start()
    }
    recorder = mediaRecorder
  }

  fun currentAmplitudeNormalized(): Float {
    val maxAmp =
      try {
        recorder?.maxAmplitude ?: 0
      } catch (_: Exception) {
        0
      }
    return (maxAmp / 24000f).coerceIn(0f, 1f)
  }

  fun stopAndReadBytes(): ByteArray {
    val active = recorder
    val file = outputFile
    recorder = null
    outputFile = null
    try {
      active?.stop()
    } catch (_: Exception) {
    } finally {
      try {
        active?.release()
      } catch (_: Exception) {
      }
    }
    val bytes =
      try {
        if (file != null && file.exists()) file.readBytes() else ByteArray(0)
      } finally {
        file?.delete()
      }
    return bytes
  }

  fun release() {
    try {
      recorder?.reset()
    } catch (_: Exception) {
    }
    try {
      recorder?.release()
    } catch (_: Exception) {
    }
    recorder = null
    outputFile?.delete()
    outputFile = null
  }
}
