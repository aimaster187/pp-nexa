package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.NeuralCoreOrb3D
import com.example.ui.components.NexaTechAtmosphereBackground
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaEmeraldOk
import com.example.ui.theme.NexaFrostBgBottom
import com.example.ui.theme.NexaFrostBgMid
import com.example.ui.theme.NexaFrostBgTop
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var showAddAccountAction by remember { mutableStateOf(false) }
  var showFallbackPicker by remember { mutableStateOf(false) }
  var autoSignInJob by remember { mutableStateOf<Job?>(null) }

  val authCardAnim = rememberInfiniteTransition(label = "auth_3d_card")
  val cardTiltY by
    authCardAnim.animateFloat(
      initialValue = -2.2f,
      targetValue = 2.2f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4200, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "auth_card_tilt_y",
    )

  LaunchedEffect(Unit) {
    autoSignInJob =
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
        onUnauthenticated = {},
        scope = scope,
      )
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(listOf(NexaFrostBgTop, NexaFrostBgMid, NexaFrostBgBottom))
        )
        .systemBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    NexaTechAtmosphereBackground(modifier = Modifier.fillMaxSize())

    Column(
      modifier =
        Modifier
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
        shadowElevation = 6.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = NexaEmeraldOk,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "3D Autonomous Phone OS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "NEXA AI",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Multi-Agent Control · WhatsApp/Gmail Auto-Reply · Hey Nexa",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(18.dp))

      NeuralCoreOrb3D(
        isListening = false,
        isBusy = isLoading,
        accessibilityActive = false,
        orbSize = 236.dp,
      )

      Spacer(modifier = Modifier.height(18.dp))

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 18.dp,
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.88f)),
        modifier =
          Modifier.graphicsLayer {
            cameraDistance = 16f * density
            rotationY = cardTiltY
            rotationX = 1.6f
          },
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Apne custom AI agents, WhatsApp/Gmail auto-reply rules aur Yaadein memory ko securely cloud me sync karein.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(18.dp))

          if (errorText != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
              modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("auth_error_banner"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = errorText!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center,
                )
                if (showFallbackPicker) {
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = {
                      autoSignInJob?.cancel()
                      errorText = null
                      isLoading = true
                      val activity = context.findActivity()
                      val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
                      if (activity == null || clientId.isNullOrBlank()) {
                        isLoading = false
                        errorText = "Unable to start Google ID picker."
                      } else {
                        scope.launch {
                          trySecondaryGoogleIdSignIn(
                            activity = activity,
                            clientId = clientId,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                              isLoading = false
                              onAuthSuccess()
                            },
                            onAuthError = { msg, needsSetup ->
                              isLoading = false
                              errorText = msg
                              showAddAccountAction = needsSetup
                            },
                            onAuthCancelled = { cancelledMsg ->
                              isLoading = false
                              errorText = cancelledMsg
                              showFallbackPicker = true
                              showAddAccountAction = true
                            },
                          )
                        }
                      }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.AccountCircle,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Google ID Account Picker")
                  }
                }
                if (showAddAccountAction) {
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = { openDeviceAccountSettings(context) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.ManageAccounts,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Google Account Settings")
                  }
                }
              }
            }
          }

          Button(
            onClick = {
              autoSignInJob?.cancel()
              errorText = null
              showAddAccountAction = false
              showFallbackPicker = false
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg, needsAccountSetup ->
                  isLoading = false
                  errorText = msg
                  showAddAccountAction = needsAccountSetup
                  showFallbackPicker = true
                },
                onAuthCancelled = { cancelledMsg ->
                  isLoading = false
                  errorText = cancelledMsg
                  showFallbackPicker = true
                  showAddAccountAction = true
                },
                scope = scope,
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Connecting Google...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
            } else {
              Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(28.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  GoogleGLogo(modifier = Modifier.size(16.dp))
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val stroke = size.minDimension * 0.22f
    val inset = stroke / 2f
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)

    // Red top arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -145f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    // Yellow left arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 145f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 35f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    // Blue right arc + crossbar
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -5f,
      sweepAngle = 45f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, size.height * 0.5f),
      end = Offset(size.width - inset, size.height * 0.5f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private fun openDeviceAccountSettings(context: Context) {
  val intents =
    listOf(
      Intent(Settings.ACTION_ADD_ACCOUNT).apply {
        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
      },
      Intent(Settings.ACTION_SYNC_SETTINGS),
      Intent(Settings.ACTION_SETTINGS),
    )
  for (intent in intents) {
    try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      return
    } catch (_: Exception) {
    }
  }
}

private fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
): Job? {
  val alreadySignedIn = runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)
  if (alreadySignedIn) {
    onAuthSuccess()
    return null
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return null
    }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  return scope.launch {
    try {
      val hostContext: Context = context.findActivity() ?: context
      val result = credentialManager.getCredential(hostContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCred = GoogleAuthProvider.getCredential(token, null)
        Firebase.auth.signInWithCredential(authCred).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

private fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String, Boolean) -> Unit,
  onAuthCancelled: (String) -> Unit,
  scope: CoroutineScope,
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Sign-In configuration missing (default_web_client_id).", false)
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    val activity = context.findActivity()
    if (activity == null) {
      onAuthError("Unable to resolve Activity window for Google Sign-In.", false)
      return@launch
    }
    try {
      val result = credentialManager.getCredential(activity, primaryRequest)
      completeFirebaseGoogleSignIn(result.credential, onAuthSuccess) { err ->
        onAuthError(err, false)
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled(
        "Google Sign-In complete nahi ho paya (Play Services cancelled). Agar app Testing mode me hai to owner Gmail account chunein, ya niche 'Try Google ID Account Picker' try karein."
      )
    } catch (e: NoCredentialException) {
      Log.w("Auth", "GetSignInWithGoogleOption returned NoCredentialException, trying GoogleId fallback", e)
      trySecondaryGoogleIdSignIn(
        activity = activity,
        clientId = clientId,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
        onAuthError = onAuthError,
        onAuthCancelled = onAuthCancelled,
      )
    } catch (e: GetCredentialException) {
      Log.e("Auth", "Google Sign-In failed (${e.type})", e)
      val needsSetup = e.type.contains("NO_CREDENTIAL", ignoreCase = true)
      val friendly =
        if (needsSetup) {
          "No Google account is active on this device yet. Add a Google account in Device Settings and try again."
        } else {
          e.errorMessage?.toString() ?: e.localizedMessage ?: "Google Sign-In failed. Please retry."
        }
      onAuthError(friendly, needsSetup)
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign-in failed", false)
    }
  }
}

private suspend fun trySecondaryGoogleIdSignIn(
  activity: Activity,
  clientId: String,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String, Boolean) -> Unit,
  onAuthCancelled: (String) -> Unit,
) {
  try {
    val googleIdOption =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(false)
        .build()
    val fallbackRequest = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    val result = credentialManager.getCredential(activity, fallbackRequest)
    completeFirebaseGoogleSignIn(result.credential, onAuthSuccess) { err ->
      onAuthError(err, false)
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w("Auth", "Google ID fallback cancelled: ${e.message}", e)
    onAuthCancelled(
      "Google ID sign-in cancel/block hua. Ensure APK signing SHA-1 Firebase se match ho aur selected Gmail Google Cloud Test Users me ho."
    )
  } catch (e: NoCredentialException) {
    Log.w("Auth", "No Google credentials found on device", e)
    onAuthError(
      "Is device/emulator par koi Google account logged-in nahi hai. Niche button se Google account add karein aur fir try karein.",
      true,
    )
  } catch (e: Exception) {
    Log.e("Auth", "Google ID fallback failed", e)
    onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.", true)
  }
}

private suspend fun completeFirebaseGoogleSignIn(
  credential: androidx.credentials.Credential,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
) {
  if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
    val authCred = GoogleAuthProvider.getCredential(token, null)
    Firebase.auth.signInWithCredential(authCred).await()
    onAuthSuccess()
  } else {
    onAuthError("Unexpected credential type returned.")
  }
}
