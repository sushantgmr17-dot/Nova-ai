package com.example.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class AppLockManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE)

    private val _isLockEnabled = MutableStateFlow(prefs.getBoolean("is_enabled", false))
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    fun enableLock(pin: String) {
        val hashed = hashPin(pin)
        prefs.edit()
            .putBoolean("is_enabled", true)
            .putString("pin_hash", hashed)
            .apply()
        _isLockEnabled.value = true
    }

    fun disableLock() {
        prefs.edit()
            .putBoolean("is_enabled", false)
            .remove("pin_hash")
            .apply()
        _isLockEnabled.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", "") ?: ""
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        return try {
            val bytes = pin.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            pin
        }
    }
}
