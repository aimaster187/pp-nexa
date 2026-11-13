package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexaAiTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NexaResponsiveNavigationDrawerTest {

  @get:Rule
  val composeRule = createComposeRule()

  private fun sampleModel(tab: NexaTab = NexaTab.HOME) =
    NexaDrawerStateModel(
      selectedTab = tab,
      userDisplayName = "Boss",
      userEmail = "boss@example.com",
      activeProviderName = "Gemini",
      enabledAgentCount = 2,
      historyCount = 5,
      latestHistorySnippet = "COMMAND: Open Gmail",
      accessibilityBound = true,
      accessibilityAgentEnabled = true,
      wakeRunning = false,
      wakeEnabled = false,
    )

  @Test
  fun compactWidth_opensDrawerViaHamburger_andDisplaysSections() {
    var selectedTab by mutableStateOf(NexaTab.HOME)
    var hamburgerVisible = false

    composeRule.setContent {
      NexaAiTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
          NexaResponsiveNavigationDrawer(
            drawerState = drawerState,
            model = sampleModel(selectedTab),
            onSelectTab = { selectedTab = it },
            onCloseDrawer = { scope.launch { drawerState.close() } },
            onToggleAccessibilityAgent = {},
            onToggleWakeWord = {},
            onOpenApiKeys = {},
            onOpenHistoryFeed = {},
            onSignOut = {},
          ) { showHamburger ->
            hamburgerVisible = showHamburger
            Box(modifier = Modifier.fillMaxSize()) {
              if (showHamburger) {
                Button(
                  onClick = { scope.launch { drawerState.open() } },
                  modifier = Modifier.testTag("test_open_drawer"),
                ) {
                  Text("Menu")
                }
              }
            }
          }
        }
      }
    }

    assertTrue(hamburgerVisible)
    composeRule.onNodeWithTag("test_open_drawer").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("drawer_section_account").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_section_history").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_section_settings").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_item_transcribe").performScrollTo().performClick()
    composeRule.waitForIdle()

    assertEquals(NexaTab.TRANSCRIBE, selectedTab)
  }

  @Test
  @Config(qualifiers = "w1024dp-h800dp")
  fun expandedWidth_rendersPermanentDrawerWithSectionsVisible() {
    var hamburgerVisible = true

    composeRule.setContent {
      NexaAiTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        Box(modifier = Modifier.fillMaxSize()) {
          NexaResponsiveNavigationDrawer(
            drawerState = drawerState,
            model = sampleModel(NexaTab.HOME),
            onSelectTab = {},
            onCloseDrawer = { scope.launch { drawerState.close() } },
            onToggleAccessibilityAgent = {},
            onToggleWakeWord = {},
            onOpenApiKeys = {},
            onOpenHistoryFeed = {},
            onSignOut = {},
          ) { showHamburger ->
            hamburgerVisible = showHamburger
            Box(modifier = Modifier.fillMaxSize())
          }
        }
      }
    }

    assertFalse(hamburgerVisible)
    composeRule.onNodeWithTag("drawer_section_account").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_section_history").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_section_settings").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("drawer_item_transcribe").performScrollTo().assertIsDisplayed()
  }
}
