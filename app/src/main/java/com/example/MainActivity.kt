package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.LocalConfigStore
import com.example.ai.configureAppCheck
import com.example.data.model.AiProviderType
import com.example.data.repository.NexaRepository
import com.example.system.NexaCommandBus
import com.example.system.NexaWakeWordService
import com.example.system.PhoneController
import com.example.ui.NexaViewModel
import com.example.ui.components.NexaDrawerStateModel
import com.example.ui.components.NexaFloatingBottomBar
import com.example.ui.components.NexaResponsiveNavigationDrawer
import com.example.ui.components.NexaTab
import com.example.ui.components.NexaTechAtmosphereBackground
import com.example.ui.components.NexaTopBar
import com.example.ui.components.VoiceCommandTriggerDialog
import com.example.ui.screens.AgentsControlScreen
import com.example.ui.screens.ApiKeysVaultSheet
import com.example.ui.screens.AudioTranscribeScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeOrbScreen
import com.example.ui.screens.MemoryChatScreen
import com.example.ui.screens.PhoneControlConsoleScreen
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import com.example.system.NexaLicenseManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ZedBrowserScreen
import com.example.ui.screens.ZedSubRouteScreen

import androidx.compose.ui.Alignment
import com.example.ui.theme.NexaAiTheme
import com.example.ui.theme.NexaBlueCore
import com.example.ui.theme.NexaDarkBgBottom
import com.example.ui.theme.NexaDarkBgTop
import com.example.ui.theme.NexaFrostBgBottom
import com.example.ui.theme.NexaFrostBgMid
import com.example.ui.theme.NexaFrostBgTop
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // STEP 1: Configure App Check with launch intent before UI, ViewModels, or Firebase calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    handleWakeWordIntent(intent)

    setContent {
      NexaAiTheme(darkTheme = false) {
        NexaRootNavigation()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleWakeWordIntent(intent)
  }

  private fun handleWakeWordIntent(intent: Intent?) {
    val cmd = intent?.getStringExtra(NexaWakeWordService.EXTRA_VOICE_COMMAND)
    if (!cmd.isNullOrBlank()) {
      NexaCommandBus.emitVoiceCommand(cmd)
    }
  }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NexaRootNavigation(auth: FirebaseAuth = Firebase.auth) {
  var authRefreshTick by remember { mutableIntStateOf(0) }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = remember(currentUser, authRefreshTick) { currentUser ?: auth.currentUser }
  if (user == null) {
    AuthScreen(onAuthSuccess = { authRefreshTick++ })
  } else {
    key(user.uid) {
      NexaMainWorkspace(
        currentUserId = user.uid,
        userEmail = user.email ?: "",
        userDisplayName = user.displayName ?: "",
      )
    }
  }
}

@Composable
fun NexaMainWorkspace(
  currentUserId: String,
  userEmail: String,
  userDisplayName: String,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: NexaViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val dbId = appContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            NexaViewModel(
              repository = NexaRepository(firestore, Firebase.auth),
              currentUserId = currentUserId,
              localConfigStore = LocalConfigStore(appContext),
              phoneController = PhoneController(appContext),
            )
          }
        },
    )

  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val agents by viewModel.agents.collectAsStateWithLifecycle()
  val automations by viewModel.automations.collectAsStateWithLifecycle()
  val memories by viewModel.memories.collectAsStateWithLifecycle()
  val secrets by viewModel.secrets.collectAsStateWithLifecycle()
  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val commandUiState by viewModel.commandUiState.collectAsStateWithLifecycle()
  val selectedAgentId by viewModel.selectedAgentId.collectAsStateWithLifecycle()
  val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
  val logs by viewModel.logs.collectAsStateWithLifecycle()
  val wakeStatus by viewModel.wakeStatus.collectAsStateWithLifecycle()
  val wakeRunning by viewModel.wakeRunning.collectAsStateWithLifecycle()
  val lastOtp by viewModel.lastOtp.collectAsStateWithLifecycle()
  val transcriptionState by viewModel.transcriptionState.collectAsStateWithLifecycle()
  val transcriptionHistory by viewModel.transcriptionHistory.collectAsStateWithLifecycle()

  val licState by NexaLicenseManager.state.collectAsState()
  val trialPrefs = remember { context.getSharedPreferences("zed_daily_free_trial", Context.MODE_PRIVATE) }
  val todayKey = remember { java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date()) }
  var freeSecondsUsedToday by remember { mutableIntStateOf(trialPrefs.getInt("used_$todayKey", 0)) }
  val freeSecondsRemaining = (licState.dailyFreeLimitSec - freeSecondsUsedToday).coerceAtLeast(0)

  var isOrbMuted by remember { mutableStateOf(false) }
  var continuousVoiceLocked by remember { mutableStateOf(false) }
  var showZedBrowser by remember { mutableStateOf(false) }
  var browserStartUrl by remember { mutableStateOf("https://www.google.com") }
  var activeSubRoute by remember { mutableStateOf<String?>(null) }

  // Real Android SpeechRecognizer Continuous Hands-Free Listener
  DisposableEffect(continuousVoiceLocked, isOrbMuted) {
    var recognizer: SpeechRecognizer? = null
    if (continuousVoiceLocked && !isOrbMuted && SpeechRecognizer.isRecognitionAvailable(context)) {
      recognizer = SpeechRecognizer.createSpeechRecognizer(context)
      val recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      }
      fun startListeningLoop() {
        try {
          if (continuousVoiceLocked && !isOrbMuted) recognizer?.startListening(recIntent)
        } catch (_: Exception) {}
      }
      recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
          if (continuousVoiceLocked && !isOrbMuted) startListeningLoop()
        }
        override fun onResults(results: Bundle?) {
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val spoken = matches?.firstOrNull().orEmpty()
          if (spoken.trim().length >= 5 && !isOrbMuted) {
            viewModel.sendCommand(spoken)
          }
          if (continuousVoiceLocked && !isOrbMuted) startListeningLoop()
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
      })
      startListeningLoop()
    }
    onDispose {
      try { recognizer?.destroy() } catch (_: Exception) {}
    }
  }

  var selectedTab by remember { mutableStateOf(NexaTab.HOME) }
  var showApiKeysSheet by remember { mutableStateOf(false) }
  var showNotificationsDialog by remember { mutableStateOf(false) }
  var showVoiceTriggerDialog by remember { mutableStateOf(false) }
  val drawerState = rememberDrawerState(DrawerValue.Closed)

  // Refresh device permissions when app resumes from System Settings
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  LaunchedEffect(Unit) { viewModel.refreshPermissions(context) }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      viewModel.refreshPermissions(context)
      if (granted) {
        showVoiceTriggerDialog = true
      } else {
        NexaCommandBus.addLog(
          "VOICE",
          "Mic Permission Denied",
          "Grant microphone permission to use voice triggers.",
          "ERROR",
        )
      }
    }

  fun launchVoicePrompt() {
    val hasMicPermission =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (hasMicPermission) {
      showVoiceTriggerDialog = true
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  fun performSignOut() {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("Auth", "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
        }
      }
    }
  }

  if (selectedTab != NexaTab.HOME && !drawerState.isOpen) {
    BackHandler { selectedTab = NexaTab.HOME }
  }

  val darkTheme = false
  val bgColors =
    if (darkTheme) listOf(NexaDarkBgTop, NexaDarkBgBottom)
    else listOf(NexaFrostBgTop, NexaFrostBgMid, NexaFrostBgBottom)

  val drawerModel =
    NexaDrawerStateModel(
      selectedTab = selectedTab,
      userDisplayName = userDisplayName.ifBlank { profile?.bossName ?: "BOSS" },
      userEmail = userEmail,
      activeProviderName = AiProviderType.fromId(secrets.activeProvider).displayName,
      enabledAgentCount = agents.count { it.isEnabled },
      historyCount = chatMessages.size + logs.size,
      latestHistorySnippet = logs.firstOrNull()?.let { "${it.source}: ${it.title}" },
      accessibilityBound = permissions.accessibilityEnabled,
      accessibilityAgentEnabled = secrets.accessibilityAgentControlEnabled,
      wakeRunning = wakeRunning,
      wakeEnabled = secrets.wakeWordListeningEnabled,
    )

  NexaResponsiveNavigationDrawer(
    drawerState = drawerState,
    model = drawerModel,
    onSelectTab = { selectedTab = it },
    onCloseDrawer = { scope.launch { drawerState.close() } },
    onToggleAccessibilityAgent = { enable ->
      if (enable && !permissions.accessibilityEnabled) {
        selectedTab = NexaTab.CONTROL
        scope.launch { drawerState.close() }
      } else {
        viewModel.saveLocalSecrets(secrets.copy(accessibilityAgentControlEnabled = enable), context)
      }
    },
    onToggleWakeWord = { enable ->
      if (enable && !permissions.micGranted) {
        selectedTab = NexaTab.CONTROL
        scope.launch { drawerState.close() }
      } else {
        viewModel.saveLocalSecrets(secrets.copy(wakeWordListeningEnabled = enable), context)
      }
    },
    onOpenApiKeys = { showApiKeysSheet = true },
    onOpenHistoryFeed = { showNotificationsDialog = true },
    onSignOut = { performSignOut() },
  ) { showHamburgerButton ->
    Box(
      modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors))
    ) {
      NexaTechAtmosphereBackground(modifier = Modifier.fillMaxSize())

      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          Box(modifier = Modifier.statusBarsPadding()) {
            NexaTopBar(
              title = "ZED AI",
              showDrawerButton = showHamburgerButton,
              onOpenDrawer = {
                scope.launch {
                  if (drawerState.isClosed) drawerState.open() else drawerState.close()
                }
              },
              onOpenNotifications = { showNotificationsDialog = true },
              onOpenTranscribe = { selectedTab = NexaTab.TRANSCRIBE },
              onOpenApiKeys = { showApiKeysSheet = true },
              onOpenPermissions = { selectedTab = NexaTab.CONTROL },
              onToggleWakeWord = {
                viewModel.saveLocalSecrets(
                  secrets.copy(wakeWordListeningEnabled = !secrets.wakeWordListeningEnabled),
                  context,
                )
              },
              onSignOut = { performSignOut() },
              wakeActive = wakeRunning,
              hasAlertBadge = wakeRunning || !lastOtp.isNullOrBlank(),
            )
          }
        },
        bottomBar = {
      if (!showZedBrowser) {
          NexaFloatingBottomBar(
            selectedTab = selectedTab,
            isListening = (continuousVoiceLocked && !isOrbMuted) || showVoiceTriggerDialog || wakeRunning,
            onSelectTab = { selectedTab = it },
            onMicClick = {
              val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
              if (hasMic) {
                continuousVoiceLocked = !continuousVoiceLocked
              } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            },
          )
        
      }
    },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
          when (selectedTab) {
            NexaTab.HOME -> {
              
              
              LaunchedEffect(Unit) {
                NexaLicenseManager.initAndSyncUser(context)
              }
              if (licState.isBanned) {
                Box(
                  modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFEAF1FC)).padding(24.dp),
                  contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                  Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("ACCOUNT SUSPENDED", color = androidx.compose.ui.graphics.Color(0xFFFF2A6D), style = MaterialTheme.typography.headlineMedium)
                    Text("ZED Admin dwara aapka access band kar diya gaya hai.", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(top = 8.dp))
                  }
                }
              } else {
                if (licState.needsPhoneNumber) {
                  var phoneDraft by remember { mutableStateOf("") }
                  AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Verify Contact Number") },
                    text = {
                      OutlinedTextField(
                        value = phoneDraft,
                        onValueChange = { phoneDraft = it },
                        label = { Text("WhatsApp / Mobile Number") },
                        singleLine = true
                      )
                    },
                    confirmButton = {
                      Button(onClick = { NexaLicenseManager.saveUserPhone(context, phoneDraft) }) {
                        Text("Save & Continue")
                      }
                    }
                  )
                }
              }
              if (!licState.isVipUnlocked) {
                var gateKeyInput by remember { mutableStateOf(licState.activeKey) }
                Box(
                  modifier = Modifier.fillMaxSize().background(Color(0xFFE8EFFA)).padding(24.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFF4F8FF),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.padding(bottom = 10.dp))
                      Text("ZED AI VIP LOCKED", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                      Text("by The Titan AI", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                        if (!licState.isLoggedIn) {
                          var authName by remember { mutableStateOf(licState.userDisplayName) }
                          var authEmail by remember { mutableStateOf(licState.userEmail) }
                          Spacer(modifier = Modifier.height(10.dp))
                          OutlinedTextField(
                            value = authName,
                            onValueChange = { authName = it },
                            label = { Text("Aapka Naam (e.g. Pranav)") },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A)),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                          )
                          Spacer(modifier = Modifier.height(6.dp))
                          OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it },
                            label = { Text("Google / Gmail Address") },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A)),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                          )
                          Button(
                            onClick = {
                              val em = authEmail.trim().ifBlank { "pranav_user_${System.currentTimeMillis() % 9999}@gmail.com" }
                              val nm = authName.trim().ifBlank { em.substringBefore("@") }
                              scope.launch { NexaLicenseManager.registerOrLoginUser(context, nm, em, "Google Sign-In") }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                          ) {
                            Text("Continue with Google (10 Min Free)", color = Color.White, fontWeight = FontWeight.Bold)
                          }
                        }
                      Text(
                        "Aaj ka 10-minute FREE trial poora ho gaya hai! Unlimited 24x7 use ke liye VIP License Key dalein.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 12.dp)
                      )
                      OutlinedTextField(
                        value = gateKeyInput,
                        onValueChange = { gateKeyInput = it },
                        label = { Text("Enter ZED-VIP-XXXX-XXXX Key") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                      )
                      Button(
                        onClick = { scope.launch { NexaLicenseManager.activateKey(context, gateKeyInput) } },
                        enabled = !licState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                      ) {
                        Text(if (licState.isLoading) "Verifying..." else "UNLOCK ZED AI", color = Color.White, fontWeight = FontWeight.Bold)
                      }
                      if (licState.statusMessage.isNotBlank()) {
                        Text(licState.statusMessage, color = Color(0xFFE11D48), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                      }
                    }
                  }
                }
              } else if (activeSubRoute != null) {
                ZedSubRouteScreen(
                  route = activeSubRoute!!,
                  onBack = { activeSubRoute = null },
                  onOpenBrowserWithUrl = { url ->
                    browserStartUrl = url
                    activeSubRoute = null
                    showZedBrowser = true
                  }
                )
              } else if (showZedBrowser) {
                ZedBrowserScreen(
                  initialUrl = browserStartUrl,
                  onClose = { showZedBrowser = false }
                )
              } else {
                HomeOrbScreen(
                  profile = profile,
                  agents = agents,
                  selectedAgentId = selectedAgentId,
                  permissions = permissions,
                  secrets = secrets,
                  commandUiState = commandUiState,
                  wakeStatus = wakeStatus,
                  wakeRunning = wakeRunning,
                  lastOtp = lastOtp,
                  isVoiceListening = (continuousVoiceLocked && !isOrbMuted) || showVoiceTriggerDialog,
                  onOrbVoiceClick = { isOrbMuted = !isOrbMuted },
                  onSendCommand = { viewModel.sendCommand(it) },
                  onSendVisionCommand = { prompt, bytes, mime ->
                    viewModel.sendCommand(prompt, imageBytes = bytes, imageMimeType = mime)
                  },
                  onSelectAgent = { viewModel.selectAgent(it) },
                  onNavigateTab = { selectedTab = it },
                  onOpenApiKeys = { showApiKeysSheet = true },
                  isOrbMuted = isOrbMuted,
                  onToggleMuteOrb = { isOrbMuted = !isOrbMuted },
                  onOpenBrowser = { showZedBrowser = true },
                  onOpenSubRoute = { activeSubRoute = it },
                  isFreeTrialActive = !licState.isVipUnlocked,
                  freeSecondsRemaining = freeSecondsRemaining,
                )
              }
            }
            NexaTab.AGENTS -> {
              ZedSubRouteScreen(
                route = "scanner",
                onBack = { selectedTab = NexaTab.HOME },
                onOpenRoute = { activeSubRoute = it; selectedTab = NexaTab.HOME },
                onOpenBrowserWithUrl = { browserStartUrl = it; showZedBrowser = true },
                onSendQuickPrompt = { selectedTab = NexaTab.TRANSCRIBE; viewModel.sendCommand(it) }
              )
            }
            NexaTab.CONTROL -> {
              ZedSubRouteScreen(
                route = "yaadein",
                onBack = { selectedTab = NexaTab.HOME },
                onOpenRoute = { activeSubRoute = it; selectedTab = NexaTab.HOME },
                onOpenApiKeys = { showApiKeysSheet = true },
                onOpenBrowserWithUrl = { browserStartUrl = it; showZedBrowser = true },
                onSendQuickPrompt = { selectedTab = NexaTab.TRANSCRIBE; viewModel.sendCommand(it) }
              )
            }
            NexaTab.CHAT -> {
              MemoryChatScreen(
                chatMessages = chatMessages,
                memories = memories,
                commandUiState = commandUiState,
                onSendMessage = { viewModel.sendCommand(it) },
                onRerunSteps = { viewModel.rerunSteps(it) },
                onCreateMemory = { t, c -> viewModel.createMemory(t, c) },
                onDeleteMemory = { viewModel.deleteMemory(it) },
              )
            }
            NexaTab.TRANSCRIBE -> {
              AudioTranscribeScreen(
                transcriptionState = transcriptionState,
                history = transcriptionHistory,
                onTranscribeAudio = { bytes, sec, lang ->
                  viewModel.transcribeAudioBytes(bytes, sec, lang)
                },
                onReportError = { viewModel.setTranscriptionError(it) },
                onSaveToMemory = { text ->
                  viewModel.createMemory("Audio Transcript", text, category = "TRANSCRIPT")
                },
                onRunAsCommand = { cmd ->
                  viewModel.sendCommand(cmd)
                  selectedTab = NexaTab.HOME
                },
                onRefreshPermissions = { viewModel.refreshPermissions(context) },
              )
            }
          }
        }
      }
    }
  }

  if (licState.popupId.isNotBlank() && licState.globalPopupMessage.isNotBlank()) {
    AlertDialog(
      onDismissRequest = { NexaLicenseManager.dismissPopup(context, licState.popupId) },
      containerColor = Color(0xFFF4F8FF),
      title = { Text(licState.globalPopupTitle.ifBlank { "ZED AI Announcement" }, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
      text = { Text(licState.globalPopupMessage, color = Color(0xFF334155)) },
      confirmButton = {
        Button(
          onClick = { NexaLicenseManager.dismissPopup(context, licState.popupId) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) { Text("OK", color = Color.White) }
      }
    )
  }

  if (showApiKeysSheet) {
    ApiKeysVaultSheet(
      secrets = secrets,
      bossName = profile?.bossName ?: "BOSS",
      onDismiss = { showApiKeysSheet = false },
      onSaveSecrets = { viewModel.saveLocalSecrets(it, context) },
      onSaveBossName = { viewModel.updateBossName(it) },
      onSignOut = { performSignOut() },
    )
  }

  if (showNotificationsDialog) {
    AlertDialog(
      onDismissRequest = { showNotificationsDialog = false },
      title = { Text("ZED History & Live OTP") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Accessibility: ${if (permissions.accessibilityEnabled && secrets.accessibilityAgentControlEnabled) "ACTIVE" else "PAUSED / OFF"}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "Wake-Word Core: ${if (wakeRunning) wakeStatus else "Standby"}",
            style = MaterialTheme.typography.bodyMedium,
          )
          if (!lastOtp.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                  cm?.setPrimaryClip(ClipData.newPlainText("ZED OTP", lastOtp))
                  NexaCommandBus.addLog("OTP", "Copied OTP", "Copied $lastOtp to clipboard")
                },
            ) {
              Text(
                text = "Latest Detected OTP: $lastOtp (Tap to copy)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          HorizontalDivider()
          if (logs.isEmpty()) {
            Text(
              text = "No recent actions logged yet. Run a voice, transcribe, or text command to inspect history.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            logs.take(5).forEach { entry ->
              Text(
                text = "• [${entry.source}] ${entry.title}: ${entry.detail}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showNotificationsDialog = false
            selectedTab = NexaTab.CONTROL
          },
          colors = ButtonDefaults.buttonColors(containerColor = NexaBlueCore),
        ) {
          Text("Open Control Console", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") }
      },
    )
  }

  if (showVoiceTriggerDialog) {
    VoiceCommandTriggerDialog(
      targetAgentName = agents.firstOrNull { it.id == selectedAgentId }?.name ?: "ZED Core",
      onTranscribeAudio = { audioBytes -> viewModel.transcribeForVoiceTrigger(audioBytes) },
      onExecuteCommand = { spokenCommand -> viewModel.sendCommand(spokenCommand) },
      onDismiss = { showVoiceTriggerDialog = false },
    )
  }
}
