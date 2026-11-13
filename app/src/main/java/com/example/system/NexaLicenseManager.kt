package com.example.system

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class LicenseUiState(
  val isVipUnlocked: Boolean = false,
  val activeKey: String = "",
  val keyLabel: String = "",
  val isBanned: Boolean = false,
  val needsPhoneNumber: Boolean = false,
  val statusMessage: String = "",
  val isLoading: Boolean = false,
)

object NexaLicenseManager {
  private val _state = MutableStateFlow(LicenseUiState())
  val state: StateFlow<LicenseUiState> = _state.asStateFlow()

  private const val PREFS = "nexa_vip_license_prefs"

  @SuppressLint("HardwareIds")
  fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      ?: "unknown_device"
  }

  fun getDeviceModel(): String {
    return "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
  }

  fun initAndSyncUser(context: Context) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val savedKey = prefs.getString("vip_key", "").orEmpty()
    val savedLabel = prefs.getString("vip_label", "").orEmpty()
    if (savedKey.isNotEmpty()) {
      _state.value = _state.value.copy(
        isVipUnlocked = true,
        activeKey = savedKey,
        keyLabel = savedLabel,
      )
    }

    val user = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()
    val deviceId = getDeviceId(context)
    val deviceModel = getDeviceModel()

    val userRef = db.collection("users").document(user.uid)
    userRef.get().addOnSuccessListener { snap ->
      val existingPhone = snap.getString("phone").orEmpty()
      val authPhone = user.phoneNumber.orEmpty()
      val finalPhone = existingPhone.ifBlank { authPhone }

      val profileMap = hashMapOf<String, Any>(
        "uid" to user.uid,
        "name" to (user.displayName ?: snap.getString("name") ?: "NEXA User"),
        "email" to (user.email ?: snap.getString("email") ?: ""),
        "phone" to finalPhone,
        "deviceId" to deviceId,
        "deviceModel" to deviceModel,
        "vipKey" to savedKey,
        "lastActiveAt" to System.currentTimeMillis(),
      )
      if (!snap.exists()) {
        profileMap["isBanned"] = false
        profileMap["createdAt"] = System.currentTimeMillis()
      }
      userRef.set(profileMap, SetOptions.merge())

      if (finalPhone.isBlank()) {
        _state.value = _state.value.copy(needsPhoneNumber = true)
      }
    }

    // Real-time Ban Listener on User
    userRef.addSnapshotListener { snap, _ ->
      if (snap != null && snap.exists()) {
        val banned = snap.getBoolean("isBanned") == true
        _state.value = _state.value.copy(isBanned = banned)
      }
    }

    // Real-time Listener on Saved License Key
    if (savedKey.isNotEmpty()) {
      db.collection("license_keys").document(savedKey).addSnapshotListener { snap, _ ->
        if (snap == null || !snap.exists()) {
          clearLocalKey(context, "License Key admin dwara hata di gayi hai.")
          return@addSnapshotListener
        }
        val keyBanned = snap.getBoolean("isBanned") == true
        val isActive = snap.getBoolean("isActive") != false
        val boundId = snap.getString("boundDeviceId").orEmpty()
        val expiresAt = snap.getLong("expiresAt") ?: 0L
        val isExpired = expiresAt > 0L && System.currentTimeMillis() > expiresAt

        if (keyBanned || !isActive || isExpired || (boundId.isNotEmpty() && boundId != deviceId)) {
          clearLocalKey(
            context,
            if (keyBanned) "Ye License Key Admin dwara BAN kar di gayi hai!"
            else if (isExpired) "License Key expire ho chuki hai!"
            else "Key kisi doosre phone par shift ho gayi hai."
          )
        }
      }
    }
  }

  fun saveUserPhone(context: Context, phoneInput: String) {
    val cleanPhone = phoneInput.trim()
    if (cleanPhone.length < 8) return
    val user = FirebaseAuth.getInstance().currentUser ?: return
    FirebaseFirestore.getInstance().collection("users").document(user.uid)
      .set(mapOf("phone" to cleanPhone), SetOptions.merge())
    _state.value = _state.value.copy(needsPhoneNumber = false)
  }

  suspend fun activateKey(context: Context, rawKey: String): Pair<Boolean, String> {
    val cleanKey = rawKey.trim().uppercase()
    if (cleanKey.isEmpty()) return false to "Pehle VIP License Key paste karein!"

    _state.value = _state.value.copy(isLoading = true, statusMessage = "")
    return try {
      val db = FirebaseFirestore.getInstance()
      val docRef = db.collection("license_keys").document(cleanKey)
      val snap = docRef.get().await()

      if (!snap.exists()) {
        _state.value = _state.value.copy(isLoading = false, statusMessage = "Invalid License Key! Admin se sahi key lein.")
        return false to "Invalid License Key!"
      }

      val isBanned = snap.getBoolean("isBanned") == true
      val isActive = snap.getBoolean("isActive") != false
      val expiresAt = snap.getLong("expiresAt") ?: 0L
      if (isBanned || !isActive) {
        _state.value = _state.value.copy(isLoading = false, statusMessage = "Ye Key BANNED ya inactive hai!")
        return false to "Key Banned!"
      }
      if (expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
        _state.value = _state.value.copy(isLoading = false, statusMessage = "Ye Key expire ho chuki hai!")
        return false to "Key Expired!"
      }

      val currentDeviceId = getDeviceId(context)
      val boundDeviceId = snap.getString("boundDeviceId").orEmpty()
      if (boundDeviceId.isNotEmpty() && boundDeviceId != currentDeviceId) {
        val msg = "Ye Key pehle se kisi doosre phone par active hai! Pehle purane phone se 'Logout Key' karein."
        _state.value = _state.value.copy(isLoading = false, statusMessage = msg)
        return false to msg
      }

      val user = FirebaseAuth.getInstance().currentUser
      val label = snap.getString("label").orEmpty()
      val updates = hashMapOf<String, Any>(
        "boundDeviceId" to currentDeviceId,
        "boundDeviceModel" to getDeviceModel(),
        "boundUserId" to (user?.uid ?: ""),
        "boundUserName" to (user?.displayName ?: "NEXA User"),
        "boundUserEmail" to (user?.email ?: ""),
        "boundUserPhone" to (user?.phoneNumber ?: ""),
      )
      docRef.set(updates, SetOptions.merge()).await()

      if (user != null) {
        db.collection("users").document(user.uid)
          .set(mapOf("vipKey" to cleanKey), SetOptions.merge())
      }

      context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("vip_key", cleanKey)
        .putString("vip_label", label)
        .apply()

      _state.value = _state.value.copy(
        isVipUnlocked = true,
        activeKey = cleanKey,
        keyLabel = label,
        isLoading = false,
        statusMessage = "VIP Unlocked! Sabhi 3D OS features active ho gaye.",
      )
      true to "VIP Unlocked!"
    } catch (e: Exception) {
      val err = "Network/Server Error: ${e.message}"
      _state.value = _state.value.copy(isLoading = false, statusMessage = err)
      false to err
    }
  }

  suspend fun logoutKey(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val currentKey = prefs.getString("vip_key", "").orEmpty()
    _state.value = _state.value.copy(isLoading = true)
    try {
      if (currentKey.isNotEmpty()) {
        val db = FirebaseFirestore.getInstance()
        db.collection("license_keys").document(currentKey)
          .set(mapOf("boundDeviceId" to "", "boundDeviceModel" to ""), SetOptions.merge())
          .await()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
          db.collection("users").document(user.uid)
            .set(mapOf("vipKey" to ""), SetOptions.merge())
        }
      }
    } catch (_: Exception) {}
    clearLocalKey(context, "Key logout ho gayi! Ab aap ise kisi bhi naye phone me laga sakte hain.")
    return "Key Unlinked!"
  }

  private fun clearLocalKey(context: Context, msg: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    _state.value = _state.value.copy(
      isVipUnlocked = false,
      activeKey = "",
      keyLabel = "",
      isLoading = false,
      statusMessage = msg,
    )
  }
}
