package com.example.system

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.ai.AiBrainRouter
import com.example.data.model.AutomationRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NexaNotificationListenerService : NotificationListenerService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val aiRouter = AiBrainRouter()
  private val otpRegex = Regex("""(?:otp|code|pin|password|verification)[^0-9]{0,16}(\d{4,8})\b|\b(\d{4,8})\b[^a-zA-Z]{0,12}(?:otp|code|verification)""", RegexOption.IGNORE_CASE)

  override fun onListenerConnected() {
    super.onListenerConnected()
    isConnected = true
    NexaCommandBus.addLog(
      source = "NOTIFICATION",
      title = "Notification Listener Ready",
      detail = "Monitoring WhatsApp, Gmail & OTP rules per user toggles.",
    )
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    isConnected = false
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null || sbn.packageName == packageName) return
    val extras = sbn.notification?.extras ?: return
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    if (title.isBlank() && text.isBlank()) return

    val pkg = sbn.packageName
    val appKey =
      when {
        pkg.contains("whatsapp", ignoreCase = true) -> "WHATSAPP"
        pkg == "com.google.android.gm" || pkg.contains("mail") -> "GMAIL"
        pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") -> "SMS"
        else -> return
      }

    // 1. Check for OTP codes if SMS or Gmail rule is active
    val otpMatch = otpRegex.find("$title $text")
    val detectedOtp = otpMatch?.groups?.get(1)?.value ?: otpMatch?.groups?.get(2)?.value
    if (!detectedOtp.isNullOrBlank()) {
      NexaCommandBus.recordOtp(detectedOtp)
      NexaCommandBus.addLog(
        source = "NOTIFICATION",
        title = "OTP Detected ($appKey)",
        detail = "Code $detectedOtp from $title captured for user-approved relay.",
      )
    }

    // 2. Match active automation rules
    val rules =
      NexaCommandBus.activeRulesSnapshot.filter {
        it.isActive && (it.targetApp == appKey || (detectedOtp != null && it.targetApp == "SMS"))
      }
    if (rules.isEmpty()) return

    val matchedRule =
      rules.firstOrNull { rule ->
        val pattern = rule.triggerPattern.trim()
        pattern == "*" ||
          pattern.split("|").any { token ->
            token.isNotBlank() && "$title $text".contains(token.trim(), ignoreCase = true)
          }
      } ?: return

    handleMatchedRule(sbn, appKey, title, text, detectedOtp, matchedRule)
  }

  private fun handleMatchedRule(
    sbn: StatusBarNotification,
    appKey: String,
    sender: String,
    body: String,
    detectedOtp: String?,
    rule: AutomationRule,
  ) {
    scope.launch {
      try {
        val replyText =
          when {
            !detectedOtp.isNullOrBlank() && rule.targetApp == "SMS" ->
              "Your requested verification OTP from $sender is: $detectedOtp"
            rule.useAiReply ->
              aiRouter.generateAutoReply(
                sender = sender,
                incomingText = body,
                goalOrTemplate = rule.replyTemplateOrGoal,
                secrets = NexaCommandBus.latestSecretsSnapshot,
              )
            else -> rule.replyTemplateOrGoal
          }

        if (rule.requireConfirmation) {
          NexaCommandBus.addLog(
            source = "AGENT",
            title = "Rule Ready: ${rule.title}",
            detail = "Prepared $appKey reply for $sender: \"$replyText\" (Confirmation required)",
            status = "PENDING",
          )
          return@launch
        }

        val sentInline = tryInlineNotificationReply(sbn.notification, replyText)
        if (sentInline) {
          NexaCommandBus.addLog(
            source = "AGENT",
            title = "Auto-Replied on $appKey",
            detail = "Sent to $sender: \"$replyText\"",
            status = "SUCCESS",
          )
        } else {
          NexaCommandBus.addLog(
            source = "AGENT",
            title = "Rule Triggered ($appKey)",
            detail = "Inline reply unavailable; drafted: \"$replyText\"",
            status = "PENDING",
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Auto-reply failed", e)
        NexaCommandBus.addLog(
          source = "AGENT",
          title = "Auto-Reply Error",
          detail = e.localizedMessage ?: "Failed to process $appKey rule",
          status = "ERROR",
        )
      }
    }
  }

  private fun tryInlineNotificationReply(notification: Notification?, replyText: String): Boolean {
    val actions = notification?.actions ?: return false
    for (action in actions) {
      val remoteInputs = action.remoteInputs ?: continue
      if (remoteInputs.isNotEmpty()) {
        val intent = Intent()
        val bundle = Bundle()
        for (ri in remoteInputs) {
          bundle.putCharSequence(ri.resultKey, replyText)
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
        try {
          action.actionIntent.send(this, 0, intent)
          return true
        } catch (e: PendingIntent.CanceledException) {
          Log.w(TAG, "Inline reply PendingIntent cancelled", e)
        }
      }
    }
    return false
  }

  companion object {
    private const val TAG = "NexaNotifListener"
    @Volatile var isConnected: Boolean = false
      private set
  }
}
