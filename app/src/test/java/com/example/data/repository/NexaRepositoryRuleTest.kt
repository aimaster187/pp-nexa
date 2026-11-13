package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AutomationRule
import com.example.data.model.CustomAgent
import com.example.data.model.MemoryEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NexaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveAgent_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = NexaRepository(firestore, auth)

    withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("BOSS") }
    val agentId =
      withTimeout(TIMEOUT_MS) {
        repo.createAgent(
          CustomAgent(
            name = "WhatsApp Bot",
            role = "Auto reply",
            systemPrompt = "Reply clearly",
            triggerType = "WHATSAPP_INCOMING",
            allowedTools = listOf("AUTO_REPLY"),
          )
        )
      }
    assertTrue(agentId.isNotEmpty())

    val agents = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAgents(uid).first { list -> list.any { it.id == agentId } }
    }
    assertTrue(agents.any { it.id == agentId && it.name == "WhatsApp Bot" })
  }

  @Test
  fun createAutomationAndMemory_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = NexaRepository(firestore, auth)

    val ruleId =
      withTimeout(TIMEOUT_MS) {
        repo.createAutomation(
          AutomationRule(
            title = "Gmail OTP",
            targetApp = "GMAIL",
            triggerPattern = "OTP",
            replyTemplateOrGoal = "Forward OTP",
          )
        )
      }
    val memId =
      withTimeout(TIMEOUT_MS) {
        repo.createMemory(
          MemoryEntry(
            title = "Boss Preference",
            content = "Reply in Hinglish",
            category = "MEMORY",
            agentName = "NEXA Core",
          )
        )
      }
    val rules = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAutomations(uid).first { list -> list.any { it.id == ruleId } }
    }
    val memories = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMemories(uid).first { list -> list.any { it.id == memId } }
    }
    assertTrue(rules.any { it.id == ruleId })
    assertTrue(memories.any { it.id == memId })
  }

  @Test
  fun crossUserRead_deniedWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = NexaRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      aliceRepo.createAgent(
        CustomAgent(
          name = "Private Agent",
          role = "Private",
          systemPrompt = "Secret",
          triggerType = "MANUAL",
        )
      )
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = NexaRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeAgents(aliceUid).first() }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = checkNotNull(caught?.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $caught" }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
  }

  @Test
  fun unauthenticatedObserve_deniedWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = NexaRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeAgents("alice_uid").first() }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = checkNotNull(caught?.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $caught" }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_nexa@test.com"
    const val BOB_EMAIL = "bob_nexa@test.com"
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
