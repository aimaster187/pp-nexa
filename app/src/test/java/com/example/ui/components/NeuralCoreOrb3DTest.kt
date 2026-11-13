package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.NexaAiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NeuralCoreOrb3DTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun neuralCoreOrb3D_renders_animatesStates_andHandlesTap() {
    var tappedCount = 0
    var listeningState by mutableStateOf(false)
    var busyState by mutableStateOf(false)
    var a11yState by mutableStateOf(false)

    composeRule.setContent {
      NexaAiTheme {
        NeuralCoreOrb3D(
          isListening = listeningState,
          isBusy = busyState,
          accessibilityActive = a11yState,
          onOrbTapped = { tappedCount++ },
        )
      }
    }

    composeRule.onNodeWithTag("neural_core_orb_3d").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("NEXA 3D Neural Core Orb").assertIsDisplayed()

    composeRule.onNodeWithTag("neural_core_orb_3d").performClick()
    composeRule.mainClock.advanceTimeBy(800L)
    assertEquals(1, tappedCount)

    composeRule.runOnIdle {
      listeningState = true
      a11yState = true
    }
    composeRule.mainClock.advanceTimeBy(900L)

    composeRule.runOnIdle {
      listeningState = false
      busyState = true
    }
    composeRule.mainClock.advanceTimeBy(900L)

    composeRule.onNodeWithTag("neural_core_orb_3d").assertIsDisplayed()
  }
}
