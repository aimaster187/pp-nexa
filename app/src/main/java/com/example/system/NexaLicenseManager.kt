package com.example.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class LicenseUiState(
    val isVipUnlocked: Boolean = false,
    val activeKey: String = "",
    val clientName: String = "",
    val userEmail: String = "",
    val userDisplayName: String = "",
    val loginProvider: String = "",
    val isLoggedIn: Boolean = false,
    val isBanned: Boolean = false,
    val isChecking: Boolean = false,
    val errorMessage: String? = null,
    val dailyFreeLimitSec: Int = 600,
    val globalMaintenance: Boolean = false,
    val globalBanner: String = ""
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
    private const val KEY_LOGGED_IN = "is_logged_in"

    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    private fun customDb(): FirebaseFirestore {
        return runCatching {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "ai-studio-android-nexaai-74a37412-09eb-422f-8076-ca2691f1d973")
        }.getOrElse {
            FirebaseFirestore.getInstance()
        }
    }

    private fun defaultDb(): FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedKey = p.getString(KEY_CODE, "") ?: ""
        val savedClient = p.getString(KEY_CLIENT, "") ?: ""
        val savedEmail = p.getString(KEY_USER_EMAIL, "") ?: ""
        val savedName = p.getString(KEY_USER_NAME, "") ?: ""
        val savedProv = p.getString(KEY_LOGIN_PROVIDER, "") ?: ""
        val loggedIn = p.getBoolean(KEY_LOGGED_IN, false)

        _state.value = _state.value.copy(
            isVipUnlocked = savedKey.isNotBlank(),
            activeKey = savedKey,
            clientName = savedClient,
            userEmail = savedEmail,
            userDisplayName = savedName,
            loginProvider = savedProv,
            isLoggedIn = loggedIn
        )
    }

    suspend fun initAndSyncUser(context: Context) {
        init(context)
        runCatching {
            val db = customDb()
            val cfg = db.collection("zed_config").document("app_control").get().await()
            if (cfg.exists()) {
                val freeMins = (cfg.getLong("freeTrialMinutes") ?: 10L).toInt()
                val maint = cfg.getBoolean("maintenanceMode") ?: false
                val banner = cfg.getString("globalBanner") ?: ""
                _state.value = _state.value.copy(
                    dailyFreeLimitSec = (freeMins * 60).coerceAtLeast(0),
                    globalMaintenance = maint,
                    globalBanner = banner
                )
            }
            val email = _state.value.userEmail
            if (email.isNotBlank()) {
                val docId = email.replace(Regex("[^a-z0-9]"), "_")
                val userDoc = db.collection("zed_users").document(docId).get().await()
                if (userDoc.exists()) {
                    val banned = userDoc.getBoolean("banned") ?: false
                    val grantedKey = userDoc.getString("grantedVipKey") ?: ""
                    if (banned) {
                        _state.value = _state.value.copy(isBanned = true, isVipUnlocked = false)
                    } else if (grantedKey.isNotBlank() && !_state.value.isVipUnlocked) {
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_CODE, grantedKey)
                            .apply()
                        _state.value = _state.value.copy(isVipUnlocked = true, activeKey = grantedKey)
                    }
                }
            }
        }
    }

    fun saveUserPhone(context: Context, phone: String) {}

    suspend fun registerOrLoginUser(context: Context, name: String, email: String, provider: String) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
        val devId = getDeviceId(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putString(KEY_USER_EMAIL, cleanEmail)
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_LOGIN_PROVIDER, provider)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()

        _state.value = _state.value.copy(
            userEmail = cleanEmail,
            userDisplayName = cleanName,
            loginProvider = provider,
            isLoggedIn = true,
            errorMessage = null
        )

        runCatching {
            val db = customDb()
            val docId = cleanEmail.replace(Regex("[^a-z0-9]"), "_")
            val snap = db.collection("zed_users").document(docId).get().await()
            val banned = snap.getBoolean("banned") ?: false
            val grantedVipKey = snap.getString("grantedVipKey") ?: ""

            val userData = hashMapOf(
                "uid" to docId,
                "name" to cleanName,
                "email" to cleanEmail,
                "provider" to provider,
                "deviceId" to devId,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "isVip" to (_state.value.isVipUnlocked || grantedVipKey.isNotBlank()),
                "activeKey" to (_state.value.activeKey.ifBlank { grantedVipKey }),
                "lastActiveAt" to System.currentTimeMillis()
            )
            db.collection("zed_users").document(docId).set(userData, SetOptions.merge()).await()

            if (banned) {
                _state.value = _state.value.copy(isBanned = true, isVipUnlocked = false, errorMessage = "Aapka account Admin dwara suspend kiya gaya hai.")
            } else if (grantedVipKey.isNotBlank() && !_state.value.isVipUnlocked) {
                p.edit().putString(KEY_CODE, grantedKey).putString(KEY_CLIENT, cleanName).apply()
                _state.value = _state.value.copy(isVipUnlocked = true, activeKey = grantedVipKey, clientName = cleanName)
            }
        }
    }

    fun logoutKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_CODE)
            .remove(KEY_CLIENT)
            .apply()
        _state.value = _state.value.copy(isVipUnlocked = false, activeKey = "", clientName = "")
    }

    fun logoutUser(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
        _state.value = _state.value.copy(isLoggedIn = false)
    }

    suspend fun activateKey(context: Context, rawKey: String): Boolean = verifyAndActivateKey(context, rawKey)

    suspend fun verifyAndActivateKey(context: Context, rawKey: String): Boolean {
        val key = rawKey.trim().uppercase()
        if (key.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Kripya VIP License Key dalein.")
            return false
        }
        _state.value = _state.value.copy(isChecking = true, errorMessage = null)
        return try {
            var activeDb = customDb()
            var doc = runCatching { activeDb.collection("zed_licenses").document(key).get().await() }.getOrNull()
            if (doc == null || !doc.exists()) {
                activeDb = defaultDb()
                doc = runCatching { activeDb.collection("zed_licenses").document(key).get().await() }.getOrNull()
            }

            if (doc == null || !doc.exists()) {
                _state.value = _state.value.copy(isChecking = false, errorMessage = "Invalid License Key! Admin se sahi key lein.")
                false
            } else {
                val status = doc.getString("status") ?: "active"
                val boundDevice = doc.getString("deviceId") ?: ""
                val client = doc.getString("clientName") ?: "VIP Boss"
                val expiresAt = doc.getLong("expiresAt") ?: 0L
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
                    val userEmail = _state.value.userEmail.ifBlank { "vip_user@zed.ai" }
                    val userName = _state.value.userDisplayName.ifBlank { client }
                    runCatching {
                        activeDb.collection("zed_licenses").document(key).set(
                            mapOf(
                                "deviceId" to myDevice,
                                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                                "activatedByEmail" to userEmail,
                                "activatedByName" to userName,
                                "activatedAt" to System.currentTimeMillis(),
                                "status" to "used"
                            ),
                            SetOptions.merge()
                        ).await()
                        val docId = userEmail.replace(Regex("[^a-z0-9]"), "_")
                        activeDb.collection("zed_users").document(docId).set(
                            mapOf(
                                "uid" to docId,
                                "name" to userName,
                                "email" to userEmail,
                                "isVip" to true,
                                "activeKey" to key,
                                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                                "lastActiveAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        ).await()
                    }

                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_CODE, key)
                        .putString(KEY_CLIENT, client)
                        .putBoolean(KEY_LOGGED_IN, true)
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
            _state.value = _state.value.copy(isChecking = false, errorMessage = "Network Error: ${e.localizedMessage ?: "Internet check karein"}")
            false
        }
    }
}
