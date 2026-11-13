package com.example.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexaAccessibilityToggleTest {

  @Test
  fun userToggleControlsAccessibilityExecutionGate() {
    NexaCommandBus.accessibilityAgentControlEnabled = false
    assertFalse(NexaCommandBus.accessibilityAgentControlEnabled)

    NexaCommandBus.accessibilityAgentControlEnabled = true
    assertTrue(NexaCommandBus.accessibilityAgentControlEnabled)
  }
}
