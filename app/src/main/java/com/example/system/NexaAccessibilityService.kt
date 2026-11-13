package com.example.system

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NexaAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    _isConnected.value = true
    NexaCommandBus.addLog(
      source = "ACCESSIBILITY",
      title = "Accessibility Core Connected",
      detail = "ZED AI can now inspect UI nodes, tap, scroll & automate flows.",
      status = "SUCCESS",
    )
    Log.i(TAG, "NexaAccessibilityService connected")
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !isAgentControlActive()) return
    val pkg = event.packageName?.toString().orEmpty()
    if (pkg.isNotBlank() && pkg != packageName) {
      _currentPackage.value = pkg
    }
  }

  override fun onInterrupt() {
    Log.w(TAG, "NexaAccessibilityService interrupted")
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance === this) instance = null
    _isConnected.value = false
  }

  fun isAgentControlActive(): Boolean = NexaCommandBus.accessibilityAgentControlEnabled

  fun readCurrentScreenSnapshot(): String {
    if (!isAgentControlActive()) {
      NexaCommandBus.addLog("ACCESSIBILITY", "Screen Read Blocked", "Paused by user toggle.", "PENDING")
      return ""
    }
    val root = rootInActiveWindow ?: return ""
    val sb = StringBuilder()
    sb.appendLine("Package: ${root.packageName ?: _currentPackage.value}")
    collectNodeText(root, sb, depth = 0, maxLines = 45)
    return sb.toString()
  }

  fun listClickableButtons(): List<String> {
    if (!isAgentControlActive()) return emptyList()
    val root = rootInActiveWindow ?: return emptyList()
    val labels = LinkedHashSet<String>()
    collectClickableLabels(root, labels, maxItems = 25)
    return labels.toList()
  }

  private fun collectClickableLabels(
    node: AccessibilityNodeInfo?,
    labels: MutableSet<String>,
    maxItems: Int,
  ) {
    if (node == null || labels.size >= maxItems) return
    if (node.isClickable && node.isVisibleToUser) {
      val label = node.text?.toString()?.trim().orEmpty()
        .ifEmpty { node.contentDescription?.toString()?.trim().orEmpty() }
      if (label.isNotEmpty()) labels.add(label.take(60))
    }
    for (i in 0 until node.childCount) {
      collectClickableLabels(node.getChild(i), labels, maxItems)
    }
  }

  private fun collectNodeText(
    node: AccessibilityNodeInfo?,
    sb: StringBuilder,
    depth: Int,
    maxLines: Int,
  ) {
    if (node == null || sb.lines().size >= maxLines) return
    val text = node.text?.toString()?.trim().orEmpty()
    val desc = node.contentDescription?.toString()?.trim().orEmpty()
    val label = text.ifEmpty { desc }
    if (label.isNotEmpty()) {
      val tag =
        when {
          node.isEditable -> "[INPUT]"
          node.isClickable -> "[BTN]"
          else -> "[TXT]"
        }
      sb.appendLine("${"  ".repeat(depth.coerceAtMost(3))}$tag ${label.take(80)}")
    }
    for (i in 0 until node.childCount) {
      collectNodeText(node.getChild(i), sb, depth + 1, maxLines)
    }
  }

  fun tapNodeByText(targetText: String): Boolean {
    if (!isAgentControlActive()) {
      NexaCommandBus.addLog("ACCESSIBILITY", "Tap Blocked", "Paused by user toggle.", "PENDING")
      return false
    }
    val root = rootInActiveWindow ?: return false
    val matches = root.findAccessibilityNodeInfosByText(targetText)
    for (node in matches) {
      var candidate: AccessibilityNodeInfo? = node
      while (candidate != null) {
        if (candidate.isClickable) {
          val clicked = candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
          if (clicked) {
            NexaCommandBus.addLog("ACCESSIBILITY", "Tapped '$targetText'", "Clicked matching UI element")
            return true
          }
        }
        candidate = candidate.parent
      }
    }
    return false
  }

  fun inputText(text: String): Boolean {
    if (!isAgentControlActive()) return false
    val root = rootInActiveWindow ?: return false
    val editable = findFirstEditable(root) ?: return false
    editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args = Bundle().apply {
      putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    }
    val ok = editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    if (ok) {
      NexaCommandBus.addLog("ACCESSIBILITY", "Typed Text", "\"${text.take(60)}\"")
    }
    return ok
  }

  private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
    if (node == null) return null
    if (node.isEditable && node.isVisibleToUser) return node
    for (i in 0 until node.childCount) {
      val found = findFirstEditable(node.getChild(i))
      if (found != null) return found
    }
    return null
  }

  fun scroll(down: Boolean): Boolean {
    if (!isAgentControlActive()) return false
    val root = rootInActiveWindow ?: return false
    val scrollable = findFirstScrollable(root) ?: return false
    val action =
      if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
      else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
    val ok = scrollable.performAction(action)
    if (ok) {
      NexaCommandBus.addLog("ACCESSIBILITY", if (down) "Scrolled Down" else "Scrolled Up", "Executed scroll action")
    }
    return ok
  }

  private fun findFirstScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
    if (node == null) return null
    if (node.isScrollable && node.isVisibleToUser) return node
    for (i in 0 until node.childCount) {
      val found = findFirstScrollable(node.getChild(i))
      if (found != null) return found
    }
    return null
  }

  fun tapAtCoordinates(x: Float, y: Float): Boolean {
    if (!isAgentControlActive()) return false
    val path = Path().apply { moveTo(x, y) }
    val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    return dispatchGesture(gesture, null, null)
  }

  fun globalActionBack(): Boolean = isAgentControlActive() && performGlobalAction(GLOBAL_ACTION_BACK)
  fun globalActionHome(): Boolean = isAgentControlActive() && performGlobalAction(GLOBAL_ACTION_HOME)
  fun globalActionRecents(): Boolean = isAgentControlActive() && performGlobalAction(GLOBAL_ACTION_RECENTS)
  fun globalActionNotifications(): Boolean =
    isAgentControlActive() && performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

  fun takeScreenshotAction(): Boolean {
    if (!isAgentControlActive()) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
      if (ok) {
        NexaCommandBus.addLog("ACCESSIBILITY", "Screenshot Captured", "Triggered system screenshot via Accessibility")
      }
      return ok
    }
    return false
  }

  companion object {
    private const val TAG = "NexaAccessibility"
    @Volatile var instance: NexaAccessibilityService? = null
      private set

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentPackage = MutableStateFlow("")
    val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()
  }
}
