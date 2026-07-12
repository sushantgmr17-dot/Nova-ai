package com.example.ui.chat

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersonaManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("persona_prefs", Context.MODE_PRIVATE)

    private val _selectedPersona = MutableStateFlow(prefs.getString("selected_persona", "balanced") ?: "balanced")
    val selectedPersona: StateFlow<String> = _selectedPersona.asStateFlow()

    fun setPersona(persona: String) {
        prefs.edit().putString("selected_persona", persona).apply()
        _selectedPersona.value = persona
    }

    fun getSystemInstructionForPersona(persona: String): String {
        return when (persona) {
            "concise" -> "You are Nova AI. Your responses must be extremely concise, direct, and to-the-point. Avoid fluff, long introductions, and unnecessary explanations. Provide only the essential facts, answers, or code blocks."
            "detailed" -> "You are Nova AI. Provide highly detailed, in-depth explanations. Elaborate on every aspect, cover edge cases, provide thorough examples, and explain the underlying principles behind your answers."
            "creative" -> "You are Nova AI. Be highly creative, imaginative, and engaging. Use rich metaphors, explore unique angles, brainstorm innovative concepts, and inject a playful, inspired tone into your explanations."
            else -> "You are Nova AI, an expert coding assistant and software engineer. You provide precise, accurate, and optimal code solutions. You excel at debugging, architecture, and explaining complex programming concepts. Always format code blocks clearly."
        }
    }
}
