package com.example.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LicenseUiState(
    val isVipUnlocked: Boolean = false,
    val activeKey: String = "",
    val clientName: String = "",
    val userEmail: String = "",
    val userDisplayName: String = "",
    val loginProvider: String = "",
    val isLoggedIn: Boolean = true,
    val isBanned: Boolean = false,
    val isChecking: Boolean = false,
    val errorMessage: String? = null,
    val dailyFreeLimitSec: Int = 600,
    val globalMaintenance: Boolean = false,
    val globalPopupTitle: String = "",
    val globalPopupMessage: String = "",
    val popupId: String = ""
) {
    val isLoading: Boolean get() = isChecking
    val statusMessage: String get() = errorMessage ?: ""
    val needsPhoneNumber: Boolean get() = false
}

object NexaLicenseManager {
    private const val PREFS = "zed_vip_license_prefs"
    private const val KEY_CODE = "vip_key_code"
    private const val KEY_CLIENT = "vip_client_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_display_name"
    private const val KEY_LOGIN_PROVIDER = "login_provider"
    private const val KEY_DISMISSED_POPUP = "dismissed_popup_id"

    private const val BASE_REST = "https://firestore.googleapis.com/v1/projects/ai-studio-android-nexaai/databases/ai-studio-android-nexaai-74a37412-09eb-422f-8076-ca2691f1d973/documents"

    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedKey = p.getString(KEY_CODE, "") ?: ""
        val savedClient = p.getString(KEY_CLIENT, "") ?: ""
        val savedEmail = p.getString(KEY_USER_EMAIL, "") ?: ""
        val savedName = p.getString(KEY_USER_NAME, "Pranav Boss") ?: "Pranav Boss"
        val savedProv = p.getString(KEY_LOGIN_PROVIDER, "10-Min Free Trial") ?: "10-Min Free Trial"

        _state.value = _state.value.copy(
            isVipUnlocked = savedKey.isNotBlank(),
            activeKey = savedKey,
            clientName = savedClient,
            userEmail = savedEmail,
            userDisplayName = savedName,
            loginProvider = savedProv,
            isLoggedIn = true
        )
    }

    fun dismissPopup(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DISMISSED_POPUP, id).apply()
        _state.value = _state.value.copy(popupId = "")
    }

    private fun restGetDoc(collection: String, docId: String): JSONObject? {
        return runCatching {
            val url = URL("$BASE_REST/$collection/$docId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optJSONObject("fields")
            } else null
        }.getOrNull()
    }

    private fun restPatchDoc(collection: String, docId: String, fieldsJson: JSONObject) {
        runCatching {
            val keys = fieldsJson.keys()
            val maskParams = mutableListOf<String>()
            while (keys.hasNext()) {
                maskParams.add("updateMask.fieldPaths=${keys.next()}")
            }
            val url = URL("$BASE_REST/$collection/$docId?${maskParams.joinToString("&")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            val payload = JSONObject().put("fields", fieldsJson).toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
        }
    }

    private fun strField(v: String) = JSONObject().put("stringValue", v)
    private fun boolField(v: Boolean) = JSONObject().put("booleanValue", v)
    private fun intField(v: Long) = JSONObject().put("integerValue", v.toString())

    suspend fun initAndSyncUser(context: Context) {
        init(context)
        withContext(Dispatchers.IO) {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val dismissedId = p.getString(KEY_DISMISSED_POPUP, "") ?: ""

            val cfg = restGetDoc("zed_config", "app_control")
            if (cfg != null) {
                val freeMins = cfg.optJSONObject("freeTrialMinutes")?.optString("integerValue")?.toIntOrNull() ?: 10
                val maint = cfg.optJSONObject("maintenanceMode")?.optBoolean("booleanValue") ?: false
                val popTitle = cfg.optJSONObject("popupTitle")?.optString("stringValue") ?: "ZED AI Notice"
                val popMsg = cfg.optJSONObject("globalBanner")?.optString("stringValue") ?: ""
                val popId = cfg.optJSONObject("popupId")?.optString("stringValue") ?: ""
                _state.value = _state.value.copy(
                    dailyFreeLimitSec = (freeMins * 60).coerceAtLeast(0),
                    globalMaintenance = maint,
                    globalPopupTitle = popTitle,
                    globalPopupMessage = popMsg,
                    popupId = if (popMsg.isNotBlank() && popId != dismissedId) popId else ""
                )
            }

            val devId = getDeviceId(context)
            val email = _state.value.userEmail.ifBlank { "user_$devId@zed.ai" }
            val docId = email.replace(Regex("[^a-z0-9]"), "_")
            val userFields = restGetDoc("zed_users", docId)
            if (userFields != null) {
                val banned = userFields.optJSONObject("banned")?.optBoolean("booleanValue") ?: false
                val grantedKey = userFields.optJSONObject("grantedVipKey")?.optString("stringValue") ?: ""
                if (banned) {
                    _state.value = _state.value.copy(isBanned = true, isVipUnlocked = false)
                } else if (grantedKey.isNotBlank() && !_state.value.isVipUnlocked) {
                    p.edit().putString(KEY_CODE, grantedKey).apply()
                    _state.value = _state.value.copy(isVipUnlocked = true, activeKey = grantedKey)
                }
            }
            val syncFields = JSONObject()
                .put("uid", strField(docId))
                .put("name", strField(_state.value.userDisplayName))
                .put("email", strField(email))
                .put("provider", strField(_state.value.loginProvider))
                .put("deviceId", strField(devId))
                .put("deviceModel", strField("${Build.MANUFACTURER} ${Build.MODEL}"))
                .put("isVip", boolField(_state.value.isVipUnlocked))
                .put("activeKey", strField(_state.value.activeKey))
                .put("lastActiveAt", intField(System.currentTimeMillis()))
            restPatchDoc("zed_users", docId, syncFields)
        }
    }

    fun saveUserPhone(context: Context, phone: String) {}

    suspend fun registerOrLoginUser(context: Context, name: String, email: String, provider: String) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
        val devId = getDeviceId(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER_EMAIL, cleanEmail)
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_LOGIN_PROVIDER, provider)
            .apply()

        _state.value = _state.value.copy(
            userEmail = cleanEmail,
            userDisplayName = cleanName,
            loginProvider = provider,
            isLoggedIn = true,
            errorMessage = null
        )

        withContext(Dispatchers.IO) {
            val docId = cleanEmail.replace(Regex("[^a-z0-9]"), "_")
            val fields = JSONObject()
                .put("uid", strField(docId))
                .put("name", strField(cleanName))
                .put("email", strField(cleanEmail))
                .put("provider", strField(provider))
                .put("deviceId", strField(devId))
                .put("deviceModel", strField("${Build.MANUFACTURER} ${Build.MODEL}"))
                .put("isVip", boolField(_state.value.isVipUnlocked))
                .put("lastActiveAt", intField(System.currentTimeMillis()))
            restPatchDoc("zed_users", docId, fields)
        }
    }

    fun logoutKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_CODE).remove(KEY_CLIENT).apply()
        _state.value = _state.value.copy(isVipUnlocked = false, activeKey = "", clientName = "")
    }

    fun logoutUser(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_USER_EMAIL).apply()
        _state.value = _state.value.copy(userEmail = "")
    }

    suspend fun activateKey(context: Context, rawKey: String): Boolean = verifyAndActivateKey(context, rawKey)

    suspend fun verifyAndActivateKey(context: Context, rawKey: String): Boolean {
        val key = rawKey.trim().uppercase()
        if (key.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Kripya VIP License Key dalein.")
            return false
        }
        _state.value = _state.value.copy(isChecking = true, errorMessage = null)
        return withContext(Dispatchers.IO) {
            try {
                val fields = restGetDoc("zed_licenses", key)
                if (fields == null) {
                    _state.value = _state.value.copy(isChecking = false, errorMessage = "Invalid License Key! Admin Panel me sahi key check karein.")
                    false
                } else {
                    val status = fields.optJSONObject("status")?.optString("stringValue") ?: "active"
                    val boundDevice = fields.optJSONObject("deviceId")?.optString("stringValue") ?: ""
                    val client = fields.optJSONObject("clientName")?.optString("stringValue") ?: "VIP Boss"
                    val expiresAt = fields.optJSONObject("expiresAt")?.optString("integerValue")?.toLongOrNull() ?: 0L
                    val myDevice = getDeviceId(context)

                    if (status == "banned") {
                        _state.value = _state.value.copy(isChecking = false, errorMessage = "Ye License Key Admin dwara BLOCK kar di gayi hai.")
                        false
                    } else if (expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
                        _state.value = _state.value.copy(isChecking = false, errorMessage = "Ye License Key expire ho chuki hai.")
                        false
                    } else if (boundDevice.isNotBlank() && boundDevice != myDevice) {
                        _state.value = _state.value.copy(isChecking = false, errorMessage = "Ye key kisi aur phone me pehle se active hai.")
                        false
                    } else {
                        val userEmail = _state.value.userEmail.ifBlank { "vip_$myDevice@zed.ai" }
                        val userName = _state.value.userDisplayName.ifBlank { client }

                        restPatchDoc("zed_licenses", key, JSONObject()
                            .put("deviceId", strField(myDevice))
                            .put("deviceModel", strField("${Build.MANUFACTURER} ${Build.MODEL}"))
                            .put("activatedByEmail", strField(userEmail))
                            .put("activatedByName", strField(userName))
                            .put("activatedAt", intField(System.currentTimeMillis()))
                            .put("status", strField("used"))
                        )

                        val docId = userEmail.replace(Regex("[^a-z0-9]"), "_")
                        restPatchDoc("zed_users", docId, JSONObject()
                            .put("uid", strField(docId))
                            .put("name", strField(userName))
                            .put("email", strField(userEmail))
                            .put("isVip", boolField(true))
                            .put("activeKey", strField(key))
                            .put("deviceModel", strField("${Build.MANUFACTURER} ${Build.MODEL}"))
                            .put("lastActiveAt", intField(System.currentTimeMillis()))
                        )

                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_CODE, key)
                            .putString(KEY_CLIENT, client)
                            .apply()

                        _state.value = _state.value.copy(
                            isVipUnlocked = true,
                            isLoggedIn = true,
                            activeKey = key,
                            clientName = client,
                            isChecking = false,
                            errorMessage = null
                        )
                        true
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isChecking = false, errorMessage = "Internet Error: ${e.localizedMessage ?: "Retry karein"}")
                false
            }
        }
    }
}
