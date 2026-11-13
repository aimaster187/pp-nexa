package com.example.ai

import android.util.Log
import com.example.data.model.AiCommandPlan
import com.example.data.model.AiProviderType
import com.example.data.model.CustomAgent
import com.example.data.model.LocalAiSecrets
import com.example.data.model.PhoneActionStep
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiBrainRouter {

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val commandSchema by lazy {
    Schema.obj(
      properties =
        mapOf(
          "spokenReply" to
            Schema.string(
              description =
                "Friendly, concise response to the Boss in Hinglish/English explaining what ZED is doing."
            ),
          "memoryNote" to
            Schema.string(
              description =
                "Optional brief memory summary to save in Yaadein if user shared a preference/fact, else empty string."
            ),
          "steps" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "action" to
                        Schema.enumeration(
                          values =
                            listOf(
                              "OPEN_APP",
                              "TAP_TEXT",
                              "INPUT_TEXT",
                              "SCROLL",
                              "BACK",
                              "HOME",
                              "RECENTS",
                              "GLOBAL_NOTIFICATIONS",
                              "SEND_EMAIL_INTENT",
                              "WHATSAPP_SEND",
                              "PLAY_STORE_INSTALL",
                              "TOGGLE_FLASHLIGHT",
                              "DIAL_OR_CALL",
                              "SET_TIMER_OR_ALARM",
                              "WEB_SEARCH",
                              "SEND_SMS_INTENT",
                              "MEDIA_CONTROL",
                              "SYSTEM_SETTINGS_PANEL",
                              "COPY_TO_CLIPBOARD",
                              "TAKE_SCREENSHOT",
                              "READ_SCREEN",
                              "SPEAK",
                              "WAIT",
                            ),
                          description = "Exact Android phone control action to execute",
                        ),
                      "target" to
                        Schema.string(
                          description =
                            "Target app name, UI button text, recipient email/phone, or package name"
                        ),
                      "value" to
                        Schema.string(
                          description = "Text to type, email subject/body, message content, or parameter"
                        ),
                      "description" to
                        Schema.string(description = "Human-readable description of this step"),
                    ),
                  optionalProperties = listOf("target", "value"),
                ),
              description = "Ordered list of phone-control steps to execute. Empty if purely conversational.",
            ),
        ),
      optionalProperties = listOf("memoryNote"),
    )
  }

  private val firebaseCommandModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = commandSchema
          temperature = 0.35f
        },
      systemInstruction = content { text(CORE_SYSTEM_PROMPT) },
    )
  }

  private val firebaseAutoReplyModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig { temperature = 0.45f },
    )
  }

  private val firebaseTranscribeModel by lazy {
    Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
  }

  fun transcribeAudioStream(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4",
    languageHint: String = "Auto",
  ): Flow<String> = flow {
    require(audioBytes.isNotEmpty()) { "Audio recording is empty." }
    require(audioBytes.size <= MAX_INLINE_AUDIO_BYTES) {
      "Audio clip exceeds 20 MB limit. Please record a shorter clip."
    }
    val promptText =
      if (languageHint.isBlank() || languageHint == "Auto") {
        "Transcribe accurately what is spoken in this audio recording. Preserve the spoken language (Hindi, English, or Hinglish) cleanly."
      } else {
        "Transcribe accurately what is spoken in this audio recording. Primary language hint: $languageHint."
      }

    val accumulated = StringBuilder()
    withTimeout(60_000L) {
      val inputContent =
        content {
          inlineData(audioBytes, mimeType)
          text(promptText)
        }
      firebaseTranscribeModel.generateContentStream(inputContent).collect { chunk ->
        val piece = chunk.text.orEmpty()
        if (piece.isNotEmpty()) {
          accumulated.append(piece)
          emit(accumulated.toString())
        }
      }
    }
    if (accumulated.isBlank()) {
      throw IllegalStateException("gemini-2.5-flash returned an empty transcript.")
    }
  }

  suspend fun planCommand(
    userPrompt: String,
    activeAgent: CustomAgent?,
    screenContext: String,
    secrets: LocalAiSecrets,
    recentHistory: String = "",
    memoriesContext: String = "",
    imageBytes: ByteArray? = null,
    imageMimeType: String = "image/jpeg",
  ): AiCommandPlan {
    val provider = AiProviderType.fromId(secrets.activeProvider)
    val composedPrompt = buildString {
      if (activeAgent != null) {
        appendLine("ACTIVE AGENT: ${activeAgent.name} (${activeAgent.role})")
        appendLine("AGENT INSTRUCTIONS: ${activeAgent.systemPrompt}")
        appendLine("AGENT ALLOWED TOOLS: ${activeAgent.allowedTools.joinToString(", ")}")
      }
      if (memoriesContext.isNotBlank()) {
        appendLine("BOSS SAVED MEMORIES (YAADEIN):")
        appendLine(memoriesContext.take(1000))
      }
      if (recentHistory.isNotBlank()) {
        appendLine("RECENT CONVERSATION CONTEXT:")
        appendLine(recentHistory.take(900))
      }
      if (screenContext.isNotBlank()) {
        appendLine("CURRENT PHONE SCREEN SNAPSHOT:")
        appendLine(screenContext.take(1600))
      }
      appendLine("BOSS COMMAND:")
      appendLine(userPrompt)
    }

    return withTimeout(60_000L) {
      try {
        if (imageBytes != null && imageBytes.isNotEmpty() && provider == AiProviderType.FIREBASE_GEMINI) {
          val visionContent =
            content {
              inlineData(imageBytes, imageMimeType)
              text(composedPrompt)
            }
          val response = firebaseCommandModel.generateContent(visionContent)
          val raw = response.text ?: throw IllegalStateException("Gemini Vision returned an empty response.")
          return@withTimeout parsePlanJson(raw, "${provider.displayName} Vision")
        }
        when (provider) {
          AiProviderType.FIREBASE_GEMINI -> {
            val response = firebaseCommandModel.generateContent(composedPrompt)
            val raw = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            parsePlanJson(raw, provider.displayName)
          }
          AiProviderType.CUSTOM_GEMINI -> {
            val key = secrets.geminiApiKey.trim()
            require(key.isNotEmpty()) { "Custom Gemini API key is empty. Add your key in API Keys Vault." }
            val raw = callDirectGeminiRest(key, secrets.geminiModel.ifBlank { "gemini-2.5-flash" }, composedPrompt)
            parsePlanJson(raw, "Custom Gemini (${secrets.geminiModel})")
          }
          AiProviderType.OPENAI -> {
            val key = secrets.openAiApiKey.trim()
            require(key.isNotEmpty()) { "OpenAI API key is empty. Add your key in API Keys Vault." }
            val raw =
              callOpenAiCompatibleRest(
                url = "https://api.openai.com/v1/chat/completions",
                apiKey = key,
                model = secrets.openAiModel.ifBlank { "gpt-4o-mini" },
                prompt = composedPrompt,
              )
            parsePlanJson(raw, "OpenAI (${secrets.openAiModel})")
          }
          AiProviderType.GROQ -> {
            val key = secrets.groqApiKey.trim()
            require(key.isNotEmpty()) { "Groq API key is empty. Add your key in API Keys Vault." }
            val raw =
              callOpenAiCompatibleRest(
                url = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = key,
                model = secrets.groqModel.ifBlank { "llama-3.3-70b-versatile" },
                prompt = composedPrompt,
              )
            parsePlanJson(raw, "Groq (${secrets.groqModel})")
          }
          AiProviderType.CUSTOM_ENDPOINT -> {
            val url = secrets.customEndpointUrl.trim()
            val key = secrets.customApiKey.trim()
            val model = secrets.customModel.trim()
            require(url.isNotEmpty() && key.isNotEmpty() && model.isNotEmpty()) {
              "Custom endpoint URL, API key, and model are required in API Keys Vault."
            }
            val raw = callOpenAiCompatibleRest(url = url, apiKey = key, model = model, prompt = composedPrompt)
            parsePlanJson(raw, "Custom ($model)")
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AiBrainRouter planCommand failed for ${provider.id}", e)
        throw e
      }
    }
  }

  suspend fun generateAutoReply(
    sender: String,
    incomingText: String,
    goalOrTemplate: String,
    secrets: LocalAiSecrets,
  ): String {
    val prompt =
      """
      Generate a single short, natural reply message (under 240 chars, Hinglish/English matching incoming tone).
      Sender: $sender
      Incoming message: $incomingText
      User automation instruction: $goalOrTemplate
      Return ONLY the reply text with no quotes or markdown.
      """.trimIndent()
    return withTimeout(30_000L) {
      when (AiProviderType.fromId(secrets.activeProvider)) {
        AiProviderType.FIREBASE_GEMINI -> {
          firebaseAutoReplyModel.generateContent(prompt).text?.trim()
            ?: "ZED AI: Message received. Will respond shortly."
        }
        AiProviderType.CUSTOM_GEMINI -> {
          if (secrets.geminiApiKey.isBlank()) goalOrTemplate
          else callDirectGeminiPlain(secrets.geminiApiKey, secrets.geminiModel, prompt)
        }
        AiProviderType.OPENAI -> {
          if (secrets.openAiApiKey.isBlank()) goalOrTemplate
          else callOpenAiPlain("https://api.openai.com/v1/chat/completions", secrets.openAiApiKey, secrets.openAiModel, prompt)
        }
        AiProviderType.GROQ -> {
          if (secrets.groqApiKey.isBlank()) goalOrTemplate
          else callOpenAiPlain("https://api.groq.com/openai/v1/chat/completions", secrets.groqApiKey, secrets.groqModel, prompt)
        }
        AiProviderType.CUSTOM_ENDPOINT -> {
          if (secrets.customEndpointUrl.isBlank() || secrets.customApiKey.isBlank()) goalOrTemplate
          else callOpenAiPlain(secrets.customEndpointUrl, secrets.customApiKey, secrets.customModel, prompt)
        }
      }
    }
  }


  private fun cleanModelName(raw: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9._/-]"), "")
      .removePrefix("models/")
      .trim()
    return cleaned.ifBlank { "gemini-2.5-flash" }
  }

  private fun cleanApiKey(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9._-]"), "").trim()
  }

  private suspend fun callDirectGeminiRest(apiKey: String, model: String, prompt: String): String =
    withContext(Dispatchers.IO) {
      val safeKey = cleanApiKey(apiKey)
      val safeModel = cleanModelName(model)
      val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$safeKey"
      val bodyJson =
        JSONObject().apply {
          put(
            "systemInstruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", CORE_SYSTEM_PROMPT))),
          )
          put(
            "contents",
            JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))),
          )
          put(
            "generationConfig",
            JSONObject().apply {
              put("responseMimeType", "application/json")
              put("temperature", 0.35)
            },
          )
        }
      val request =
        Request.Builder()
          .url(endpoint)
          .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
          .build()
      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          throw IllegalStateException("Gemini API HTTP ${response.code}: ${raw.take(240)}")
        }
        val root = JSONObject(raw)
        root.getJSONArray("candidates")
          .getJSONObject(0)
          .getJSONObject("content")
          .getJSONArray("parts")
          .getJSONObject(0)
          .getString("text")
      }
    }

  private suspend fun callDirectGeminiPlain(apiKey: String, model: String, prompt: String): String =
    withContext(Dispatchers.IO) {
      val safeKey = cleanApiKey(apiKey)
      val safeModel = cleanModelName(model)
      val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$safeKey"
      val bodyJson =
        JSONObject().put(
          "contents",
          JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))),
        )
      val request =
        Request.Builder()
          .url(endpoint)
          .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
          .build()
      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IllegalStateException("Gemini HTTP ${response.code}")
        JSONObject(raw)
          .getJSONArray("candidates")
          .getJSONObject(0)
          .getJSONObject("content")
          .getJSONArray("parts")
          .getJSONObject(0)
          .getString("text")
          .trim()
      }
    }

  private suspend fun callOpenAiCompatibleRest(
    url: String,
    apiKey: String,
    model: String,
    prompt: String,
  ): String =
    withContext(Dispatchers.IO) {
      val bodyJson =
        JSONObject().apply {
          put("model", model)
          put("temperature", 0.35)
          put("response_format", JSONObject().put("type", "json_object"))
          put(
            "messages",
            JSONArray()
              .put(JSONObject().put("role", "system").put("content", CORE_SYSTEM_PROMPT))
              .put(JSONObject().put("role", "user").put("content", prompt)),
          )
        }
      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", "Bearer $apiKey")
          .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
          .build()
      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          throw IllegalStateException("Provider HTTP ${response.code}: ${raw.take(240)}")
        }
        JSONObject(raw)
          .getJSONArray("choices")
          .getJSONObject(0)
          .getJSONObject("message")
          .getString("content")
      }
    }

  private suspend fun callOpenAiPlain(
    url: String,
    apiKey: String,
    model: String,
    prompt: String,
  ): String =
    withContext(Dispatchers.IO) {
      val bodyJson =
        JSONObject().apply {
          put("model", model)
          put("temperature", 0.45)
          put(
            "messages",
            JSONArray().put(JSONObject().put("role", "user").put("content", prompt)),
          )
        }
      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", "Bearer $apiKey")
          .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
          .build()
      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IllegalStateException("Provider HTTP ${response.code}")
        JSONObject(raw)
          .getJSONArray("choices")
          .getJSONObject(0)
          .getJSONObject("message")
          .getString("content")
          .trim()
      }
    }

  private fun parsePlanJson(rawText: String, providerLabel: String): AiCommandPlan {
    val cleaned =
      rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return try {
      val json = JSONObject(cleaned)
      val reply = json.optString("spokenReply", "Command processed, Boss.")
      val memory = json.optString("memoryNote", "")
      val stepsArr = json.optJSONArray("steps") ?: JSONArray()
      val steps =
        (0 until stepsArr.length()).mapNotNull { idx ->
          val item = stepsArr.optJSONObject(idx) ?: return@mapNotNull null
          val action = item.optString("action", "").trim()
          if (action.isEmpty()) return@mapNotNull null
          PhoneActionStep(
            action = action,
            target = item.optString("target", ""),
            value = item.optString("value", ""),
            description = item.optString("description", action),
          )
        }
      AiCommandPlan(
        spokenReply = reply,
        steps = steps,
        memoryNote = memory,
        providerUsed = providerLabel,
      )
    } catch (e: Exception) {
      AiCommandPlan(
        spokenReply = rawText.trim(),
        steps = emptyList(),
        providerUsed = providerLabel,
      )
    }
  }

  private companion object {
    const val TAG = "AiBrainRouter"
    const val MAX_INLINE_AUDIO_BYTES = 19 * 1024 * 1024
    val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    val CORE_SYSTEM_PROMPT =
      """
      IDENTITY & CREATOR PROTOCOL: Your name is ZED AI. If anyone asks who made or created you ('tumhe kisne banaya hai' / 'who made you'), ALWAYS answer: 'Mujhe The Titan AI ne banaya hai.' If anyone asks who created or founded The Titan AI ('The Titan AI ko kisne banaya hai' / 'who made The Titan AI'), ALWAYS answer: 'The Titan AI ko Chief Developer Pranav Patel ne banaya aur design kiya hai.' You are an ultra-advanced 4D autonomous AI assistant engineered by The Titan AI with built-in Chrome-grade browsing, 24x7 background voice intelligence, and full device control. You are ZED AI, an advanced Android autonomous phone-control agent assistant serving the user ("Boss").
      You understand Hindi, Hinglish, and English fluently.
      Return a valid JSON object with keys:
      - "spokenReply": concise friendly confirmation or answer in Boss's language.
      - "memoryNote": optional short fact to remember in Yaadein, else "".
      - "steps": array of objects {"action","target","value","description"}.

      Available phone-control actions:
      - OPEN_APP: target = app name or package (e.g. "WhatsApp", "Gmail", "YouTube", "Chrome", "Play Store", "Settings", "Camera")
      - PLAY_STORE_INSTALL: target = app name/query to open in Google Play Store & tap Install
      - WHATSAPP_SEND: target = phone or contact name, value = message text
      - SEND_EMAIL_INTENT: target = recipient email, value = "Subject||Body"
      - TAP_TEXT: target = visible button/label text on screen to click via Accessibility
      - INPUT_TEXT: value = text to type into focused/active input box
      - SCROLL: target = "down" or "up"
      - BACK, HOME, RECENTS, GLOBAL_NOTIFICATIONS
      - TOGGLE_FLASHLIGHT: target = "on" or "off"
      - DIAL_OR_CALL: target = phone number
      - SET_TIMER_OR_ALARM: target = "timer" or "alarm", value = seconds/duration (e.g. "300" or "5 min") or "HH:mm"
      - WEB_SEARCH: target = search query or https:// URL
      - SEND_SMS_INTENT: target = phone number, value = SMS body text
      - MEDIA_CONTROL: target = "play_pause", "next", "previous", "volume_up", "volume_down", or "mute"
      - SYSTEM_SETTINGS_PANEL: target = "wifi", "internet", "volume", "bluetooth", or "settings"
      - COPY_TO_CLIPBOARD: value = exact text/OTP/reply to copy to clipboard
      - TAKE_SCREENSHOT: capture system screenshot via Accessibility
      - READ_SCREEN: capture active window text via Accessibility
      - SPEAK: value = text to speak aloud
      - WAIT: value = milliseconds string e.g. "800"

      For purely conversational questions or image/screen Q&A, set "steps": [] unless the user asks for an action (like copying extracted text or opening an app).
      For multi-step phone commands (e.g. "Play Store se Telegram install karo", "Gmail par OTP bhejo", "5 minute ka timer lagao", "Wi-Fi panel kholo"), break into clean sequential steps.
      """.trimIndent()
  }
}
