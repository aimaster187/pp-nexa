package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
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
  var autoSignInJob by remember { mutableStateOf<Job?>(null) }

  var authTabIndex by remember { mutableIntStateOf(0) }
  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  val authCardAnim = rememberInfiniteTransition(label = "auth_3d_card")
  val cardTiltY by
    authCardAnim.animateFloat(
      initialValue = -1.8f,
      targetValue = 1.8f,
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
        .systemBarsPadding()
        .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    NexaTechAtmosphereBackground(modifier = Modifier.fillMaxSize())

    Column(
      modifier =
        Modifier
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .statusBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 22.dp, vertical = 24.dp),
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

      Spacer(modifier = Modifier.height(12.dp))

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

      Spacer(modifier = Modifier.height(14.dp))

      NeuralCoreOrb3D(
        isListening = false,
        isBusy = isLoading,
        accessibilityActive = false,
        orbSize = 188.dp,
      )

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 18.dp,
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.88f)),
        modifier =
          Modifier.graphicsLayer {
            cameraDistance = 16f * density
            rotationY = cardTiltY
            rotationX = 1.2f
          },
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          TabRow(
            selectedTabIndex = authTabIndex,
            containerColor = Color.Transparent,
            contentColor = NexaBlueCore,
          ) {
            Tab(
              selected = authTabIndex == 0,
              onClick = {
                authTabIndex = 0
                errorText = null
              },
              text = {
                Text(
                  "Sign In",
                  fontWeight = if (authTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                )
              },
            )
            Tab(
              selected = authTabIndex == 1,
              onClick = {
                authTabIndex = 1
                errorText = null
              },
              text = {
                Text(
                  "Sign Up",
                  fontWeight = if (authTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                )
              },
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (errorText != null) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("auth_error_banner"),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = errorText!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center,
                )
                if (showAddAccountAction) {
                  Spacer(modifier = Modifier.height(6.dp))
                  OutlinedButton(
                    onClick = { openDeviceAccountSettings(context) },
                    shape = RoundedCornerShape(12.dp),
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

          if (authTabIndex == 1) {
            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text("Full Name") },
              leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = phoneNumber,
              onValueChange = { phoneNumber = it },
              label = { Text("Phone Number") },
              leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
          }

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                  contentDescription = null,
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              autoSignInJob?.cancel()
              errorText = null
              showAddAccountAction = false
              val cleanEmail = email.trim()
              val cleanPass = password.trim()
              if (cleanEmail.isBlank() || cleanPass.length < 6) {
                errorText = "Sahi Email aur kam se kam 6 akshar ka Password dalein."
                return@Button
              }
              if (authTabIndex == 1 && fullName.trim().isBlank()) {
                errorText = "Apna Full Name zaroor dalein."
                return@Button
              }
              isLoading = true
              scope.launch {
                try {
                  if (authTabIndex == 0) {
                    Firebase.auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                  } else {
                    val res = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                    val user = res.user
                    if (user != null) {
                      val profileUpdates = userProfileChangeRequest {
                        displayName = fullName.trim()
                      }
                      runCatching { user.updateProfile(profileUpdates).await() }
                      val profileData = hashMapOf(
                        "name" to fullName.trim(),
                        "phone" to phoneNumber.trim(),
                        "email" to cleanEmail,
                        "updatedAt" to System.currentTimeMillis(),
                      )
                      runCatching {
                        Firebase.firestore.collection("users").document(user.uid)
                          .set(profileData, SetOptions.merge()).await()
                      }
                    }
                  }
                  isLoading = false
                  onAuthSuccess()
                } catch (e: Exception) {
                  isLoading = false
                  errorText = e.localizedMessage ?: "Authentication failed. Please check details."
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
            modifier = Modifier.fillMaxWidth().height(50.dp),
          ) {
            Text(
              text = if (authTabIndex == 0) "Sign In to NEXA" else "Create Account",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
              text = "  OR  ",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedButton(
            onClick = {
              autoSignInJob?.cancel()
              errorText = null
              showAddAccountAction = false
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
                },
                scope = scope,
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, NexaBlueCore.copy(alpha = 0.5f)),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NexaBlueCore,
                strokeWidth = 2.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Connecting...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            } else {
              Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(26.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  GoogleGLogo(modifier = Modifier.size(16.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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

    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -145f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 145f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 35f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
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
      Log.w("Auth", "Primary Google Sign-In cancelled, trying Google ID picker: ${e.message}")
      trySecondaryGoogleIdSignIn(activity, clientId, credentialManager, onAuthSuccess, onAuthError)
    } catch (e: NoCredentialException) {
      Log.w("Auth", "NoCredential on primary, trying Google ID picker")
      trySecondaryGoogleIdSignIn(activity, clientId, credentialManager, onAuthSuccess, onAuthError)
    } catch (e: GetCredentialException) {
      Log.e("Auth", "Google Sign-In failed (${e.type})", e)
      onAuthError("Google Error (${e.type}): ${e.errorMessage ?: e.localizedMessage}", false)
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
    onAuthError("Google Sign-In cancel hua (${e.message ?: "Cancelled"}). Upar Email/Password se bhi login kar sakte hain.", false)
  } catch (e: NoCredentialException) {
    onAuthError("Google account nahi mila. Upar Sign Up / Sign In se account banayein ya Device Settings me Google jodein.", true)
  } catch (e: Exception) {
    onAuthError("Google Sign-In Error: ${e.localizedMessage ?: "Failed"}", false)
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
