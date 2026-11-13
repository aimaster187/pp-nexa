package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.ui.theme.NexaAiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceCommandTriggerDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun voiceCommandTriggerDialog_dispatchesTypedOrSpokenCommandAndDismisses() {
    var executedCommand: String? = null
    var dismissed = false

    composeTestRule.setContent {
      NexaAiTheme {
        VoiceCommandTriggerDialog(
          targetAgentName = "WhatsApp Bot",
          onTranscribeAudio = { "Read unread messages" },
          onExecuteCommand = { executedCommand = it },
          onDismiss = { dismissed = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("voice_trigger_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Agent: WhatsApp Bot").assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("voice_trigger_transcript_input")
      .performTextReplacement("Send WhatsApp reply to Rahul")
    composeTestRule.onNodeWithTag("voice_trigger_run_button").performClick()

    assertEquals("Send WhatsApp reply to Rahul", executedCommand)
    assertTrue(dismissed)
  }

  @Test
  fun voiceCommandTriggerDialog_quickChipTriggersAgentImmediately() {
    var executedCommand: String? = null
    var dismissed = false

    composeTestRule.setContent {
      NexaAiTheme {
        VoiceCommandTriggerDialog(
          targetAgentName = "NEXA Core",
          onTranscribeAudio = { "" },
          onExecuteCommand = { executedCommand = it },
          onDismiss = { dismissed = true },
        )
      }
    }

    composeTestRule.onNodeWithText("Read my screen").performClick()

    assertEquals("Read my screen", executedCommand)
    assertTrue(dismissed)
  }
}
