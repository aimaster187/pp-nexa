package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AiProviderType
import com.example.data.model.AutomationRule
import com.example.data.model.CustomAgent
import com.example.data.model.MemoryEntry
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NexaRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun <T> Flow<T>.logFirestoreErrors(
    operationType: OperationType,
    path: String,
    userId: String,
  ): Flow<T> = catch { error ->
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db.collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .logFirestoreErrors(OperationType.GET, path, userId)
  }

  suspend fun ensureUserProfile(bossDisplayName: String? = null) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val cleanName =
          bossDisplayName?.trim()?.takeIf { it.isNotEmpty() }?.take(40) ?: "BOSS"
        val payload =
          mapOf(
            "userId" to uid,
            "bossName" to cleanName,
            "wakeWords" to listOf("hey zed", "hello zed", "nexa", "alexa"),
            "activeProvider" to AiProviderType.FIREBASE_GEMINI.id,
            "energyPoints" to 100L,
            "moodStatus" to "Neural Core Ready",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfileFields(
    bossName: String? = null,
    wakeWords: List<String>? = null,
    activeProvider: String? = null,
    energyPoints: Long? = null,
    moodStatus: String? = null,
  ) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
    bossName?.let { updates["bossName"] = it.trim().ifEmpty { "BOSS" }.take(40) }
    wakeWords?.let { updates["wakeWords"] = it.take(10).map { w -> w.take(40) } }
    activeProvider?.let { updates["activeProvider"] = it }
    energyPoints?.let { updates["energyPoints"] = it.coerceIn(0L, 99999L) }
    moodStatus?.let { updates["moodStatus"] = it.trim().ifEmpty { "Ready" }.take(60) }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  fun observeAgents(userId: String): Flow<List<CustomAgent>> {
    val path = "users/$userId/agents"
    return db.collection("users")
      .document(userId)
      .collection("agents")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(CustomAgent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .logFirestoreErrors(OperationType.LIST, path, userId)
  }

  suspend fun createAgent(agent: CustomAgent): String {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("agents")
    val docRef = colRef.document()
    val payload =
      mapOf(
        "userId" to uid,
        "name" to agent.name.trim().ifEmpty { "Agent" }.take(60),
        "role" to agent.role.trim().ifEmpty { "Autonomous Assistant" }.take(120),
        "systemPrompt" to agent.systemPrompt.trim().ifEmpty { "Assist the user safely." }.take(2000),
        "triggerType" to agent.triggerType,
        "allowedTools" to agent.allowedTools.take(12).map { it.take(40) },
        "isEnabled" to agent.isEnabled,
        "autoExecute" to agent.autoExecute,
        "runCount" to agent.runCount.coerceAtLeast(0L),
        "accentColorHex" to agent.accentColorHex.take(12),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateAgent(agent: CustomAgent) {
    val uid = requireUserId()
    require(agent.id.isNotBlank()) { "Agent id is required for update" }
    val docRef = db.collection("users").document(uid).collection("agents").document(agent.id)
    val updates =
      mapOf(
        "name" to agent.name.trim().ifEmpty { "Agent" }.take(60),
        "role" to agent.role.trim().ifEmpty { "Autonomous Assistant" }.take(120),
        "systemPrompt" to agent.systemPrompt.trim().ifEmpty { "Assist the user safely." }.take(2000),
        "triggerType" to agent.triggerType,
        "allowedTools" to agent.allowedTools.take(12).map { it.take(40) },
        "isEnabled" to agent.isEnabled,
        "autoExecute" to agent.autoExecute,
        "runCount" to agent.runCount.coerceAtLeast(0L),
        "accentColorHex" to agent.accentColorHex.take(12),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteAgent(agentId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("agents").document(agentId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeAutomations(userId: String): Flow<List<AutomationRule>> {
    val path = "users/$userId/automations"
    return db.collection("users")
      .document(userId)
      .collection("automations")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          AutomationRule::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .logFirestoreErrors(OperationType.LIST, path, userId)
  }

  suspend fun createAutomation(rule: AutomationRule): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("automations").document()
    val payload =
      mapOf(
        "userId" to uid,
        "title" to rule.title.trim().ifEmpty { "Auto Rule" }.take(80),
        "targetApp" to rule.targetApp,
        "triggerPattern" to rule.triggerPattern.trim().ifEmpty { "*" }.take(200),
        "replyTemplateOrGoal" to rule.replyTemplateOrGoal.trim().ifEmpty { "Reply politely" }.take(1000),
        "useAiReply" to rule.useAiReply,
        "isActive" to rule.isActive,
        "requireConfirmation" to rule.requireConfirmation,
        "triggeredCount" to rule.triggeredCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateAutomation(rule: AutomationRule) {
    val uid = requireUserId()
    require(rule.id.isNotBlank()) { "Automation rule id is required" }
    val docRef = db.collection("users").document(uid).collection("automations").document(rule.id)
    val updates =
      mapOf(
        "title" to rule.title.trim().ifEmpty { "Auto Rule" }.take(80),
        "targetApp" to rule.targetApp,
        "triggerPattern" to rule.triggerPattern.trim().ifEmpty { "*" }.take(200),
        "replyTemplateOrGoal" to rule.replyTemplateOrGoal.trim().ifEmpty { "Reply politely" }.take(1000),
        "useAiReply" to rule.useAiReply,
        "isActive" to rule.isActive,
        "requireConfirmation" to rule.requireConfirmation,
        "triggeredCount" to rule.triggeredCount.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteAutomation(ruleId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("automations").document(ruleId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeMemories(userId: String): Flow<List<MemoryEntry>> {
    val path = "users/$userId/memories"
    return db.collection("users")
      .document(userId)
      .collection("memories")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(MemoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .logFirestoreErrors(OperationType.LIST, path, userId)
  }

  suspend fun createMemory(entry: MemoryEntry): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("memories").document()
    val payload =
      mapOf(
        "userId" to uid,
        "title" to entry.title.trim().ifEmpty { "Memory" }.take(100),
        "content" to entry.content.trim().ifEmpty { "Saved note" }.take(2000),
        "category" to entry.category,
        "agentName" to entry.agentName.trim().take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteMemory(memoryId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun seedStarterWorkspaceIfEmpty() {
    val uid = requireUserId()
    val agentsSnap = db.collection("users").document(uid).collection("agents").limit(1).get().await()
    if (agentsSnap.isEmpty) {
      val starterAgents =
        listOf(
          CustomAgent(
            name = "WhatsApp Auto-Pilot",
            role = "Smart Hinglish & English WhatsApp auto-responder",
            systemPrompt =
              "You are ZED WhatsApp Auto-Pilot. Read incoming message notifications and reply concisely, politely, and accurately based on user instructions.",
            triggerType = "WHATSAPP_INCOMING",
            allowedTools = listOf("AUTO_REPLY", "OPEN_APP", "TAP_TEXT", "INPUT_TEXT"),
            isEnabled = true,
            autoExecute = false,
            accentColorHex = "#22C55E",
          ),
          CustomAgent(
            name = "Gmail & OTP Relay",
            role = "Reads Gmail/OTP alerts & drafts/sends replies or OTP forwards",
            systemPrompt =
              "You are ZED Gmail & OTP Agent. Help read verification OTP notifications, draft smart email replies, and send OTPs only to user-approved contacts/emails.",
            triggerType = "GMAIL_INCOMING",
            allowedTools = listOf("READ_OTP", "SEND_EMAIL_INTENT", "AUTO_REPLY"),
            isEnabled = true,
            autoExecute = false,
            accentColorHex = "#FB7185",
          ),
          CustomAgent(
            name = "App Installer & Navigator",
            role = "Opens Play Store, searches apps & automates install taps",
            systemPrompt =
              "You are ZED Phone Navigator. Plan accessibility UI steps: OPEN_APP, TAP_TEXT, INPUT_TEXT, SCROLL, and PLAY_STORE_INSTALL.",
            triggerType = "MANUAL",
            allowedTools = listOf("OPEN_APP", "PLAY_STORE_INSTALL", "TAP_TEXT", "SCROLL", "INPUT_TEXT"),
            isEnabled = true,
            autoExecute = true,
            accentColorHex = "#38BDF8",
          ),
          CustomAgent(
            name = "Voice Wake Commander",
            role = "Always-ready agent triggered by Hey Zed / Hello Zed / Alexa",
            systemPrompt =
              "You are ZED Voice Commander. Convert natural spoken Hinglish/English commands into immediate phone-control steps and brief spoken feedback.",
            triggerType = "VOICE_WAKE",
            allowedTools = listOf("OPEN_APP", "TOGGLE_FLASHLIGHT", "DIAL_OR_CALL", "SPEAK", "BACK", "HOME"),
            isEnabled = true,
            autoExecute = true,
            accentColorHex = "#A855F7",
          ),
        )
      starterAgents.forEach { createAgent(it) }
    }

    val rulesSnap =
      db.collection("users").document(uid).collection("automations").limit(1).get().await()
    if (rulesSnap.isEmpty) {
      val starterRules =
        listOf(
          AutomationRule(
            title = "WhatsApp Smart Auto-Reply",
            targetApp = "WHATSAPP",
            triggerPattern = "*",
            replyTemplateOrGoal =
              "Reply warmly in Hinglish: 'Abhi thoda busy hoon, ZED AI ne aapka message note kar liya hai. Jaldi call/reply karta hoon!'",
            useAiReply = true,
            isActive = false,
            requireConfirmation = false,
          ),
          AutomationRule(
            title = "Gmail Auto-Reply Draft & Send",
            targetApp = "GMAIL",
            triggerPattern = "meeting|urgent|project|reply",
            replyTemplateOrGoal =
              "Draft a crisp professional acknowledgment email and prepare Gmail compose action.",
            useAiReply = true,
            isActive = false,
            requireConfirmation = true,
          ),
          AutomationRule(
            title = "OTP Detection & Email Relay",
            targetApp = "SMS",
            triggerPattern = "OTP|verification code|code is",
            replyTemplateOrGoal =
              "Extract numeric OTP from incoming alert and prepare secure Gmail relay / clipboard copy with user consent.",
            useAiReply = false,
            isActive = false,
            requireConfirmation = true,
          ),
        )
      starterRules.forEach { createAutomation(it) }
    }
  }
}
