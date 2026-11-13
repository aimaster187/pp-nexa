package com.example.system

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.example.data.model.DevicePermissionsState

object PermissionsHelper {

  fun readAllStates(context: Context): DevicePermissionsState {
    return DevicePermissionsState(
      accessibilityEnabled = isAccessibilityEnabled(context),
      notificationListenerEnabled = isNotificationListenerEnabled(context),
      overlayEnabled = Settings.canDrawOverlays(context),
      micGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED,
      notificationsGranted =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        } else {
          true
        },
      callPhoneGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
          PackageManager.PERMISSION_GRANTED,
      batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context),
    )
  }

  fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
      ?: return false
    val enabledServices =
      am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val expectedId = "${context.packageName}/${NexaAccessibilityService::class.java.name}"
    val shortId = "${context.packageName}/.system.NexaAccessibilityService"
    if (enabledServices.any { it.id == expectedId || it.id == shortId }) return true
    val settingValue =
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
    return settingValue.contains(NexaAccessibilityService::class.java.name) ||
      settingValue.contains(".system.NexaAccessibilityService")
  }

  fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat =
      Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    val cn = ComponentName(context, NexaNotificationListenerService::class.java)
    return flat.split(":").any {
      ComponentName.unflattenFromString(it) == cn ||
        it.contains(NexaNotificationListenerService::class.java.name)
    }
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun openAccessibilitySettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun openNotificationListenerSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun openOverlaySettings(context: Context) {
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
      ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun openBatteryOptimizationSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }
}
