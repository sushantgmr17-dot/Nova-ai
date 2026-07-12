package com.example.ui.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String,
    val email: String,
    val avatarIndex: Int,
    val preferredLanguage: String,
    val preferredLanguageCode: String
)

class UserProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(
        UserProfile(
            name = prefs.getString("user_name", "Guest User") ?: "Guest User",
            email = prefs.getString("user_email", "guest@nova.ai") ?: "guest@nova.ai",
            avatarIndex = prefs.getInt("avatar_index", 0),
            preferredLanguage = prefs.getString("pref_language", "English") ?: "English",
            preferredLanguageCode = prefs.getString("pref_language_code", "en") ?: "en"
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isLocalPremium = MutableStateFlow(prefs.getBoolean("local_premium", false))
    val isLocalPremium: StateFlow<Boolean> = _isLocalPremium.asStateFlow()

    fun updateProfile(name: String, email: String, avatarIndex: Int) {
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putInt("avatar_index", avatarIndex)
            apply()
        }
        _profile.value = _profile.value.copy(name = name, email = email, avatarIndex = avatarIndex)
    }

    fun updateLanguage(language: String, code: String) {
        prefs.edit().apply {
            putString("pref_language", language)
            putString("pref_language_code", code)
            apply()
        }
        _profile.value = _profile.value.copy(preferredLanguage = language, preferredLanguageCode = code)
    }

    fun setLocalPremium(active: Boolean) {
        prefs.edit().putBoolean("local_premium", active).apply()
        _isLocalPremium.value = active
    }

    companion object {
        val LANGUAGES = listOf(
            "English" to "en",
            "Español (Spanish)" to "es",
            "हिन्दी (Hindi)" to "hi",
            "中文 (Mandarin)" to "zh",
            "Français (French)" to "fr",
            "Deutsch (German)" to "de",
            "العربية (Arabic)" to "ar",
            "Português (Portuguese)" to "pt",
            "Русский (Russian)" to "ru",
            "日本語 (Japanese)" to "ja",
            "বাংলা (Bengali)" to "bn",
            "ਪੰਜਾਬੀ (Punjabi)" to "pa",
            "اردو (Urdu)" to "ur",
            "Bahasa Indonesia (Indonesian)" to "id",
            "Kiswahili (Swahili)" to "sw",
            "Türkçe (Turkish)" to "tr",
            "Italiano (Italian)" to "it",
            "한국어 (Korean)" to "ko",
            "Tiếng Việt (Vietnamese)" to "vi",
            "मराठी (Marathi)" to "mr",
            "తెలుగు (Telugu)" to "te",
            "தமிழ் (Tamil)" to "ta"
        )
    }
}
