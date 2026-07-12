package com.example.ui

import android.app.Application
import com.example.billing.BillingManager
import com.example.database.AppDatabase
import com.example.repository.ChatRepository
import com.example.security.AppLockManager
import com.example.ui.theme.ThemeManager
import com.example.ui.chat.PersonaManager
import com.example.ui.profile.UserProfileManager

class NovaApplication : Application() {
    lateinit var container: ChatRepository
    lateinit var billingManager: BillingManager
    lateinit var appLockManager: AppLockManager
    lateinit var themeManager: ThemeManager
    lateinit var personaManager: PersonaManager
    lateinit var userProfileManager: UserProfileManager

    override fun onCreate() {
        super.onCreate()
        ensureWebViewCacheDirsExist()
        container = ChatRepository(AppDatabase.getDatabase(this).chatDao())
        billingManager = BillingManager(this)
        appLockManager = AppLockManager(this)
        themeManager = ThemeManager(this)
        personaManager = PersonaManager(this)
        userProfileManager = UserProfileManager(this)
    }

    private fun ensureWebViewCacheDirsExist() {
        try {
            val baseCacheDir = cacheDir
            if (baseCacheDir != null) {
                val jsCacheDir = java.io.File(baseCacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                val wasmCacheDir = java.io.File(baseCacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                
                if (!jsCacheDir.exists()) {
                    jsCacheDir.mkdirs()
                }
                if (!wasmCacheDir.exists()) {
                    wasmCacheDir.mkdirs()
                }
            }
        } catch (e: Exception) {
            // Safe fallback to prevent app crashes on directory failure
        }
    }
}
