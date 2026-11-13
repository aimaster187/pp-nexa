package com.example.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.NexaAiTheme
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUpFirebase() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(ctx)
    }
  }

  @Test
  fun authScreen_displays3dCoreAndGoogleSignInButton() {
    composeTestRule.setContent {
      NexaAiTheme {
        AuthScreen(onAuthSuccess = {})
      }
    }

    composeTestRule.onNodeWithTag("neural_core_orb_3d").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
  }
}
