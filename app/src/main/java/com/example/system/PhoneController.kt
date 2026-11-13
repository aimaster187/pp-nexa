package com.example.system

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import com.example.data.model.PhoneActionStep
import java.util.Locale
import kotlinx.coroutines.delay

class PhoneController(private val context: Context) {

  private var tts: TextToSpeech? = null
  private var torchOn = false

  init {
    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale("en", "IN")
        }
      }
  }

  fun speak(text: String) {
    if (text.isBlank()) return
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexa_tts_${System.currentTimeMillis()}")
  }

  fun shutdown() {
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {}
  }

  suspend fun executeSteps(steps: List<PhoneActionStep>): String {
    if (steps.isEmpty()) return "No device actions needed."
    val results = mutableListOf<String>()
    val a11y = NexaAccessibilityService.instance

    for (step in steps) {
      val outcome =
        when (step.action.uppercase(Locale.ROOT)) {
          "OPEN_APP" -> openAppByName(step.target)
          "PLAY_STORE_INSTALL" -> openPlayStoreAndTryInstall(step.target, a11y)
          "WHATSAPP_SEND" -> sendWhatsAppMessage(step.target, step.value, a11y)
          "SEND_EMAIL_INTENT" -> openEmailCompose(step.target, step.value)
          "TAP_TEXT" -> {
            if (a11y == null) "Accessibility disabled (enable to tap '${step.target}')"
            else if (a11y.tapNodeByText(step.target)) "Tapped '${step.target}'"
            else "Could not find '${step.target}' on screen"
          }
          "INPUT_TEXT" -> {
            if (a11y == null) "Accessibility disabled (enable to type)"
            else if (a11y.inputText(step.value)) "Typed text"
            else "No editable field focused"
          }
          "SCROLL" -> {
            val down = !step.target.equals("up", ignoreCase = true)
            if (a11y == null) "Accessibility disabled"
            else if (a11y.scroll(down)) "Scrolled ${if (down) "down" else "up"}"
            else "No scrollable view found"
          }
          "BACK" -> if (a11y?.globalActionBack() == true) "Back pressed" else "Accessibility disabled"
          "HOME" -> if (a11y?.globalActionHome() == true) "Home pressed" else "Accessibility disabled"
          "RECENTS" -> if (a11y?.globalActionRecents() == true) "Recents opened" else "Accessibility disabled"
          "GLOBAL_NOTIFICATIONS" ->
            if (a11y?.globalActionNotifications() == true) "Notifications opened" else "Accessibility disabled"
          "TOGGLE_FLASHLIGHT" -> toggleTorch(step.target)
          "DIAL_OR_CALL" -> dialPhoneNumber(step.target)
          "SET_TIMER_OR_ALARM" -> setTimerOrAlarm(step.target, step.value)
          "WEB_SEARCH" -> openWebSearch(step.target.ifBlank { step.value })
          "SEND_SMS_INTENT" -> openSmsCompose(step.target, step.value)
          "MEDIA_CONTROL" -> controlMediaOrVolume(step.target.ifBlank { step.value })
          "SYSTEM_SETTINGS_PANEL" -> openSystemSettingsPanel(step.target.ifBlank { step.value })
          "COPY_TO_CLIPBOARD" -> copyToClipboard(step.value.ifBlank { step.target })
          "TAKE_SCREENSHOT" ->
            if (a11y?.takeScreenshotAction() == true) "Screenshot triggered"
            else "Accessibility disabled or unsupported Android version"
          "READ_SCREEN" -> {
            val snap = a11y?.readCurrentScreenSnapshot().orEmpty()
            if (snap.isNotBlank()) {
              NexaCommandBus.addLog("ACCESSIBILITY", "Screen Read", snap.take(180))
              "Read screen content"
            } else {
              "Accessibility disabled or screen empty"
            }
          }
          "SPEAK" -> {
            speak(step.value)
            "Spoke feedback"
          }
          "WAIT" -> {
            val ms = step.value.toLongOrNull()?.coerceIn(200L, 4000L) ?: 800L
            delay(ms)
            "Waited ${ms}ms"
          }
          else -> "Unsupported step: ${step.action}"
        }

      NexaCommandBus.addLog(
        source = "AGENT",
        title = step.action,
        detail = "${step.description.ifBlank { step.target }} -> $outcome",
        status = if (outcome.contains("disabled", ignoreCase = true)) "PENDING" else "SUCCESS",
      )
      results.add(outcome)
      delay(450L)
    }
    return results.joinToString(" • ")
  }

  private fun openAppByName(rawTarget: String): String {
    val query = rawTarget.trim().lowercase(Locale.ROOT)
    val knownPackages =
      mapOf(
        "whatsapp" to "com.whatsapp",
        "gmail" to "com.google.android.gm",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "play store" to "com.android.vending",
        "playstore" to "com.android.vending",
        "settings" to "com.android.settings",
        "camera" to "com.android.camera",
        "maps" to "com.google.android.apps.maps",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "spotify" to "com.spotify.music",
      )

    if (query == "settings") {
      context.startActivity(
        Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
      return "Opened Settings"
    }

    val pm = context.packageManager
    val mappedPkg = knownPackages.entries.firstOrNull { query.contains(it.key) }?.value
    if (mappedPkg != null) {
      pm.getLaunchIntentForPackage(mappedPkg)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Launched $rawTarget"
      }
    }

    val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val matchedApp =
      installed.firstOrNull { appInfo ->
        val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)
        label.contains(query) || appInfo.packageName.lowercase(Locale.ROOT).contains(query)
      }
    if (matchedApp != null) {
      pm.getLaunchIntentForPackage(matchedApp.packageName)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Launched ${pm.getApplicationLabel(matchedApp)}"
      }
    }
    return "App '$rawTarget' not found"
  }

  private suspend fun openPlayStoreAndTryInstall(
    appName: String,
    a11y: NexaAccessibilityService?,
  ): String {
    val clean = appName.trim().ifEmpty { "app" }
    val uri = Uri.parse("market://search?q=${Uri.encode(clean)}&c=apps")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      val webIntent =
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/search?q=${Uri.encode(clean)}&c=apps"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(webIntent)
    }
    if (a11y != null) {
      delay(1600L)
      val tapped = a11y.tapNodeByText("Install")
      return if (tapped) "Opened Play Store & tapped Install for $clean"
      else "Opened Play Store search for $clean"
    }
    return "Opened Play Store search for $clean"
  }

  private suspend fun sendWhatsAppMessage(
    target: String,
    message: String,
    a11y: NexaAccessibilityService?,
  ): String {
    val digits = target.filter { it.isDigit() }
    val intent =
      if (digits.length >= 10) {
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      } else {
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          setPackage("com.whatsapp")
          putExtra(Intent.EXTRA_TEXT, message)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }
    return try {
      context.startActivity(intent)
      if (a11y != null) {
        delay(1400L)
        a11y.tapNodeByText("Send")
      }
      "Opened WhatsApp message flow"
    } catch (_: Exception) {
      "WhatsApp not installed on this device"
    }
  }

  private fun openEmailCompose(recipient: String, rawSubjectBody: String): String {
    val parts = rawSubjectBody.split("||", limit = 2)
    val subject = parts.getOrElse(0) { "Message from NEXA AI" }
    val body = parts.getOrElse(1) { rawSubjectBody }
    val intent =
      Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${Uri.encode(recipient.trim())}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return try {
      context.startActivity(intent)
      "Prepared Gmail compose for ${recipient.ifBlank { "recipient" }}"
    } catch (_: Exception) {
      "No email client found"
    }
  }

  private fun toggleTorch(target: String): String {
    return try {
      val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
      val cameraId = cm.cameraIdList.firstOrNull() ?: return "No camera flash available"
      val desired =
        when {
          target.equals("on", ignoreCase = true) -> true
          target.equals("off", ignoreCase = true) -> false
          else -> !torchOn
        }
      cm.setTorchMode(cameraId, desired)
      torchOn = desired
      "Flashlight ${if (desired) "ON" else "OFF"}"
    } catch (e: Exception) {
      Log.w(TAG, "Torch toggle failed", e)
      "Flashlight unavailable on this device"
    }
  }

  private fun dialPhoneNumber(number: String): String {
    val clean = number.filter { it.isDigit() || it == '+' }
    if (clean.isEmpty()) return "No phone number provided"
    val intent =
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    return "Opened dialer for $clean"
  }

  private fun setTimerOrAlarm(targetType: String, rawValue: String): String {
    return try {
      if (targetType.contains("alarm", ignoreCase = true) || rawValue.contains(":")) {
        val parts = rawValue.trim().split(":")
        val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val min = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val intent =
          Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, min)
            putExtra(AlarmClock.EXTRA_MESSAGE, "NEXA AI Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(intent)
        "Prepared alarm for %02d:%02d".format(hour, min)
      } else {
        val digits = rawValue.filter { it.isDigit() }.toIntOrNull() ?: 300
        val seconds =
          when {
            rawValue.contains("min", ignoreCase = true) || targetType.contains("min", ignoreCase = true) ->
              digits * 60
            rawValue.contains("hour", ignoreCase = true) || rawValue.contains("hr", ignoreCase = true) ->
              digits * 3600
            else -> digits
          }.coerceIn(5, 86400)
        val intent =
          Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "NEXA AI Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(intent)
        "Prepared ${seconds}s timer"
      }
    } catch (e: Exception) {
      Log.w(TAG, "Alarm/Timer failed", e)
      "Clock app unavailable for timer/alarm"
    }
  }

  private fun openWebSearch(query: String): String {
    val clean = query.trim()
    if (clean.isEmpty()) return "Empty search query"
    return try {
      val intent =
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
          Intent(Intent.ACTION_VIEW, Uri.parse(clean))
        } else {
          Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra(SearchManager.QUERY, clean) }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      "Opened web search for '$clean'"
    } catch (_: Exception) {
      val fallback =
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://www.google.com/search?q=${Uri.encode(clean)}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(fallback)
      "Searched browser for '$clean'"
    }
  }

  private fun openSmsCompose(recipient: String, body: String): String {
    val digits = recipient.filter { it.isDigit() || it == '+' }
    return try {
      val intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(digits)}")).apply {
          putExtra("sms_body", body)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      "Prepared SMS compose"
    } catch (_: Exception) {
      "No SMS app available"
    }
  }

  private fun controlMediaOrVolume(rawCommand: String): String {
    val cmd = rawCommand.trim().lowercase(Locale.ROOT)
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      ?: return "Audio service unavailable"
    return when {
      cmd.contains("next") || cmd.contains("skip") -> {
        dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_NEXT)
        "Skipped to next track"
      }
      cmd.contains("prev") || cmd.contains("back") -> {
        dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        "Previous track"
      }
      cmd.contains("pause") || cmd.contains("stop") || cmd.contains("play") -> {
        dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        "Toggled media play/pause"
      }
      cmd.contains("up") || cmd.contains("louder") || cmd.contains("badha") -> {
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        "Increased media volume"
      }
      cmd.contains("down") || cmd.contains("kam") || cmd.contains("lower") -> {
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        "Decreased media volume"
      }
      cmd.contains("mute") || cmd.contains("silent") -> {
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        "Toggled media mute"
      }
      else -> {
        dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        "Toggled media play/pause"
      }
    }
  }

  private fun dispatchMediaKey(am: AudioManager, keyCode: Int) {
    val now = SystemClock.uptimeMillis()
    am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
    am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
  }

  private fun openSystemSettingsPanel(panelName: String): String {
    val cmd = panelName.trim().lowercase(Locale.ROOT)
    val intent =
      when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cmd.contains("wifi") ->
          Intent(Settings.Panel.ACTION_WIFI)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          (cmd.contains("internet") || cmd.contains("data") || cmd.contains("network")) ->
          Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          (cmd.contains("volume") || cmd.contains("sound") || cmd.contains("audio")) ->
          Intent(Settings.Panel.ACTION_VOLUME)
        cmd.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        else -> Intent(Settings.ACTION_SETTINGS)
      }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    return "Opened ${panelName.ifBlank { "Settings" }} panel"
  }

  private fun copyToClipboard(text: String): String {
    if (text.isBlank()) return "Nothing to copy"
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      ?: return "Clipboard unavailable"
    cm.setPrimaryClip(ClipData.newPlainText("NEXA AI", text))
    return "Copied to clipboard"
  }

  companion object {
    private const val TAG = "PhoneController"
  }
}
