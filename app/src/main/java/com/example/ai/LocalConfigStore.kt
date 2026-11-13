package com.example.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProviderType
import com.example.data.model.LocalAiSecrets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexa_local_config")

class LocalConfigStore(private val context: Context) {

  private object Keys {
    val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    val OPENAI_MODEL = stringPreferencesKey("openai_model")
    val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
    val GROQ_MODEL = stringPreferencesKey("groq_model")
    val CUSTOM_ENDPOINT_URL = stringPreferencesKey("custom_endpoint_url")
    val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    val CUSTOM_MODEL = stringPreferencesKey("custom_model")
    val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val FLOATING_ORB_ENABLED = booleanPreferencesKey("floating_orb_enabled")
    val ACCESSIBILITY_AGENT_ENABLED = booleanPreferencesKey("accessibility_agent_enabled")
    val WAKE_WORDS_CSV = stringPreferencesKey("wake_words_csv")
  }

  val configFlow: Flow<LocalAiSecrets> =
    context.nexaDataStore.data.map { prefs ->
      LocalAiSecrets(
        activeProvider = prefs[Keys.ACTIVE_PROVIDER] ?: AiProviderType.FIREBASE_GEMINI.id,
        geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
        geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-2.5-flash",
        openAiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
        openAiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o-mini",
        groqApiKey = prefs[Keys.GROQ_API_KEY] ?: "",
        groqModel = prefs[Keys.GROQ_MODEL] ?: "llama-3.3-70b-versatile",
        customEndpointUrl = prefs[Keys.CUSTOM_ENDPOINT_URL] ?: "",
        customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: "",
        customModel = prefs[Keys.CUSTOM_MODEL] ?: "",
        wakeWordListeningEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
        ttsVoiceReplyEnabled = prefs[Keys.TTS_ENABLED] ?: true,
        floatingOrbEnabled = prefs[Keys.FLOATING_ORB_ENABLED] ?: false,
        accessibilityAgentControlEnabled = prefs[Keys.ACCESSIBILITY_AGENT_ENABLED] ?: true,
        wakeWordsCsv = prefs[Keys.WAKE_WORDS_CSV] ?: "hey nexa, hello nexa, nexa, naksha, alexa",
      )
    }

  suspend fun saveConfig(secrets: LocalAiSecrets) {
    context.nexaDataStore.edit { prefs ->
      prefs[Keys.ACTIVE_PROVIDER] = secrets.activeProvider
      prefs[Keys.GEMINI_API_KEY] = secrets.geminiApiKey.trim()
      prefs[Keys.GEMINI_MODEL] = secrets.geminiModel.trim().ifEmpty { "gemini-2.5-flash" }
      prefs[Keys.OPENAI_API_KEY] = secrets.openAiApiKey.trim()
      prefs[Keys.OPENAI_MODEL] = secrets.openAiModel.trim().ifEmpty { "gpt-4o-mini" }
      prefs[Keys.GROQ_API_KEY] = secrets.groqApiKey.trim()
      prefs[Keys.GROQ_MODEL] = secrets.groqModel.trim().ifEmpty { "llama-3.3-70b-versatile" }
      prefs[Keys.CUSTOM_ENDPOINT_URL] = secrets.customEndpointUrl.trim()
      prefs[Keys.CUSTOM_API_KEY] = secrets.customApiKey.trim()
      prefs[Keys.CUSTOM_MODEL] = secrets.customModel.trim()
      prefs[Keys.WAKE_WORD_ENABLED] = secrets.wakeWordListeningEnabled
      prefs[Keys.TTS_ENABLED] = secrets.ttsVoiceReplyEnabled
      prefs[Keys.FLOATING_ORB_ENABLED] = secrets.floatingOrbEnabled
      prefs[Keys.ACCESSIBILITY_AGENT_ENABLED] = secrets.accessibilityAgentControlEnabled
      prefs[Keys.WAKE_WORDS_CSV] =
        secrets.wakeWordsCsv.trim().ifEmpty { "hey nexa, hello nexa, nexa, naksha, alexa" }
    }
  }
}
