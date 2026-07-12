package com.example.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingManager
import com.example.security.AppLockManager
import com.example.ui.theme.ThemeManager
import com.example.ui.profile.UserProfileManager
import com.example.ui.profile.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val billingManager: BillingManager,
    private val appLockManager: AppLockManager,
    private val themeManager: ThemeManager,
    private val userProfileManager: UserProfileManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userProfileManager.profile
    val isLocalPremium: StateFlow<Boolean> = userProfileManager.isLocalPremium

    val isPremium: StateFlow<Boolean> = combine(
        billingManager.isPremium,
        userProfileManager.isLocalPremium
    ) { bPremium, lPremium ->
        bPremium || lPremium
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        billingManager.isPremium.value || userProfileManager.isLocalPremium.value
    )

    val isAppLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled
    val themeMode: StateFlow<String> = themeManager.themeMode

    fun updateProfile(name: String, email: String, avatarIndex: Int) {
        userProfileManager.updateProfile(name, email, avatarIndex)
    }

    fun updateLanguage(language: String, code: String) {
        userProfileManager.updateLanguage(language, code)
    }

    fun setLocalPremium(active: Boolean) {
        userProfileManager.setLocalPremium(active)
    }

    fun subscribe(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun enableAppLock(pin: String) {
        appLockManager.enableLock(pin)
    }

    fun disableAppLock() {
        appLockManager.disableLock()
    }

    fun verifyPin(pin: String): Boolean {
        return appLockManager.verifyPin(pin)
    }

    fun setThemeMode(mode: String) {
        themeManager.setThemeMode(mode)
    }
}
