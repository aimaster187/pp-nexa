package com.example.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class NexaWakeWordService : Service() {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var speechRecognizer: SpeechRecognizer? = null
  private var tts: TextToSpeech? = null
  private var isListeningForCommand = false
  private var isDestroyed = false

  private var windowManager: WindowManager? = null
  private var floatingOrbView: View? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    tts =
      TextToSpeech(this) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale("en", "IN")
        }
      }
    initSpeechRecognizer()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    if (action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildForegroundNotification("Listening for \"Hey Nexa\" / \"Alexa\"...")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    NexaCommandBus.updateWakeStatus("Listening for wake word...", running = true)
    NexaCommandBus.addLog("WAKE_WORD", "Background Voice Core Started", "Say 'Hey Nexa', 'Hello Nexa', or 'Alexa'.")

    if (intent?.getBooleanExtra(EXTRA_SHOW_ORB, false) == true) {
      showFloatingOrbIfPermitted()
    } else if (intent?.hasExtra(EXTRA_SHOW_ORB) == true) {
      removeFloatingOrb()
    }

    mainHandler.postDelayed({ startListeningCycle() }, 350L)
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    isDestroyed = true
    mainHandler.removeCallbacksAndMessages(null)
    try {
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
    speechRecognizer = null
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {}
    removeFloatingOrb()
    NexaCommandBus.updateWakeStatus("Standby", running = false)
    super.onDestroy()
  }

  private fun initSpeechRecognizer() {
    if (!SpeechRecognizer.isRecognitionAvailable(this)) {
      NexaCommandBus.updateWakeStatus("Speech recognition unavailable", running = false)
      return
    }
    speechRecognizer =
      SpeechRecognizer.createSpeechRecognizer(this).apply {
        setRecognitionListener(
          object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              NexaCommandBus.updateWakeStatus(
                if (isListeningForCommand) "Listening for command..." else "Wake-word active...",
                running = true,
              )
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
              if (isDestroyed) return
              val restartDelay =
                when (error) {
                  SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 900L
                  SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    NexaCommandBus.updateWakeStatus("Mic permission needed", running = false)
                    stopSelf()
                    return
                  }
                  else -> 550L
                }
              mainHandler.postDelayed({ startListeningCycle() }, restartDelay)
            }

            override fun onResults(results: Bundle?) {
              val matches =
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
              val spoken = matches.firstOrNull().orEmpty().trim()
              if (spoken.isNotEmpty()) {
                handleRecognizedSpeech(spoken)
              }
              if (!isDestroyed) {
                mainHandler.postDelayed({ startListeningCycle() }, 400L)
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
          }
        )
      }
  }

  private fun startListeningCycle() {
    if (isDestroyed) return
    val sr = speechRecognizer ?: return
    val intent =
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
      }
    try {
      sr.startListening(intent)
    } catch (e: Exception) {
      Log.w(TAG, "startListening retry", e)
      mainHandler.postDelayed({ startListeningCycle() }, 1000L)
    }
  }

  private fun handleRecognizedSpeech(rawTranscript: String) {
    val lower = rawTranscript.lowercase(Locale.ROOT)
    val wakeWords =
      NexaCommandBus.latestSecretsSnapshot.wakeWordsCsv
        .split(",")
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf("hey nexa", "hello nexa", "nexa", "naksha", "alexa") }

    if (isListeningForCommand) {
      isListeningForCommand = false
      dispatchCommand(rawTranscript)
      return
    }

    val matchedWake = wakeWords.firstOrNull { lower.contains(it) }
    if (matchedWake != null) {
      val idx = lower.indexOf(matchedWake)
      val remainder = rawTranscript.substring(idx + matchedWake.length).trim(' ', ',', '.', ':')
      NexaCommandBus.addLog("WAKE_WORD", "Wake Word Heard: \"$matchedWake\"", "Transcript: $rawTranscript")
      if (remainder.length >= 3) {
        dispatchCommand(remainder)
      } else {
        isListeningForCommand = true
        NexaCommandBus.updateWakeStatus("Wake Ready! Speak command...", running = true)
        if (NexaCommandBus.latestSecretsSnapshot.ttsVoiceReplyEnabled) {
          tts?.speak("Yes Boss, listening", TextToSpeech.QUEUE_FLUSH, null, "nexa_wake_ack")
        }
      }
    }
  }

  private fun dispatchCommand(commandText: String) {
    NexaCommandBus.updateWakeStatus("Executing: ${commandText.take(28)}", running = true)
    NexaCommandBus.emitVoiceCommand(commandText)
    val openIntent =
      Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(EXTRA_VOICE_COMMAND, commandText)
      }
    startActivity(openIntent)
  }

  private fun showFloatingOrbIfPermitted() {
    if (!Settings.canDrawOverlays(this) || floatingOrbView != null) return
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager = wm

    val orb =
      TextView(this).apply {
        text = "NEXA"
        setTextColor(Color.WHITE)
        textSize = 11f
        gravity = Gravity.CENTER
        val bg =
          GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#38BDF8"), Color.parseColor("#6366F1"), Color.parseColor("#FB7185")),
          ).apply {
            shape = GradientDrawable.OVAL
            setStroke(3, Color.WHITE)
          }
        background = bg
        elevation = 16f
      }

    val params =
      WindowManager.LayoutParams(
        138,
        138,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = 24
        y = 320
      }

    var startX = 0
    var startY = 0
    var touchX = 0f
    var touchY = 0f
    var moved = false

    orb.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          startX = params.x
          startY = params.y
          touchX = event.rawX
          touchY = event.rawY
          moved = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = (touchX - event.rawX).toInt()
          val dy = (event.rawY - touchY).toInt()
          if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) moved = true
          params.x = startX + dx
          params.y = startY + dy
          wm.updateViewLayout(orb, params)
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!moved) {
            isListeningForCommand = true
            NexaCommandBus.updateWakeStatus("Orb Tapped: Speak command!", running = true)
            tts?.speak("Yes Boss", TextToSpeech.QUEUE_FLUSH, null, "orb_ack")
            val launch =
              Intent(this@NexaWakeWordService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
              }
            startActivity(launch)
          }
          true
        }
        else -> false
      }
    }

    try {
      wm.addView(orb, params)
      floatingOrbView = orb
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add floating orb overlay", e)
    }
  }

  private fun removeFloatingOrb() {
    floatingOrbView?.let { view ->
      try {
        windowManager?.removeView(view)
      } catch (_: Exception) {}
    }
    floatingOrbView = null
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "NEXA Voice & Wake-Word Core",
          NotificationManager.IMPORTANCE_LOW,
        )
      val nm = getSystemService(NotificationManager::class.java)
      nm.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(content: String): Notification {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("NEXA AI Neural Core Active")
      .setContentText(content)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentIntent(openIntent)
      .setOngoing(true)
      .build()
  }

  companion object {
    private const val TAG = "NexaWakeWordService"
    private const val CHANNEL_ID = "nexa_wake_channel"
    private const val NOTIFICATION_ID = 7401
    const val ACTION_START = "com.example.action.START_WAKE"
    const val ACTION_STOP = "com.example.action.STOP_WAKE"
    const val EXTRA_SHOW_ORB = "extra_show_orb"
    const val EXTRA_VOICE_COMMAND = "extra_voice_command"
  }
}
