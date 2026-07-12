package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingManager
import com.example.model.ChatMessage
import com.example.repository.ChatRepository
import com.example.ui.profile.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val billingManager: BillingManager,
    private val personaManager: PersonaManager,
    private val userProfileManager: UserProfileManager
) : ViewModel() {

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

    val selectedPersona: StateFlow<String> = personaManager.selectedPersona

    fun setPersona(persona: String) {
        personaManager.setPersona(persona)
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _generatingMessage = MutableStateFlow<String?>(null)
    val generatingMessage: StateFlow<String?> = _generatingMessage.asStateFlow()

    private var messageCollectionJob: kotlinx.coroutines.Job? = null
    private var currentSessionId: Int? = null
    private var lastFailedMessage: String? = null

    fun clearError() {
        _error.value = null
    }

    fun retryLastMessage() {
        val msg = lastFailedMessage
        if (msg != null) {
            lastFailedMessage = null
            sendMessage(msg)
        }
    }

    fun startNewSession(prompt: String? = null) {
        viewModelScope.launch {
            val title = prompt?.take(20) ?: "New Chat"
            currentSessionId = repository.createSession(title)
            _messages.value = emptyList()
            if (prompt != null) {
                sendMessage(prompt)
            }
        }
    }

    fun loadSession(sessionId: Int) {
        currentSessionId = sessionId
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            repository.getMessages(sessionId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = currentSessionId
        if (sessionId == null) {
            startNewSession(text)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _generatingMessage.value = ""
            
            val currentHistory = repository.getMessages(sessionId).firstOrNull() ?: emptyList()
            val alreadySaved = currentHistory.lastOrNull()?.let { it.role == "user" && it.text == text } == true
            
            val updatedHistory = if (!alreadySaved) {
                repository.saveMessage(sessionId, "user", text)
                currentHistory + ChatMessage(sessionId = sessionId, role = "user", text = text)
            } else {
                currentHistory
            }
            
            try {
                var fullResponse = ""
                val currentPersona = personaManager.selectedPersona.value
                val baseInstruction = personaManager.getSystemInstructionForPersona(currentPersona)
                val prefLang = userProfileManager.profile.value.preferredLanguage
                val systemInstruction = "$baseInstruction\n\nIMPORTANT: You must respond, write, and converse entirely in the $prefLang language. If the user writes in a different language, reply in $prefLang."
                repository.generateAiResponseStream(updatedHistory, systemInstruction).collect { chunk ->
                    fullResponse += chunk
                    _generatingMessage.value = fullResponse
                }
                repository.saveMessage(sessionId, "model", fullResponse)
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
                lastFailedMessage = text
            } finally {
                _generatingMessage.value = null
                _isLoading.value = false
            }
        }
    }

    fun editMessage(messageId: Int, newText: String) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _generatingMessage.value = ""
            
            try {
                repository.editMessageAndWipeAfter(sessionId, messageId, newText)
                val updatedHistory = repository.getMessages(sessionId).firstOrNull() ?: emptyList()
                
                var fullResponse = ""
                val currentPersona = personaManager.selectedPersona.value
                val baseInstruction = personaManager.getSystemInstructionForPersona(currentPersona)
                val prefLang = userProfileManager.profile.value.preferredLanguage
                val systemInstruction = "$baseInstruction\n\nIMPORTANT: You must respond, write, and converse entirely in the $prefLang language. If the user writes in a different language, reply in $prefLang."
                repository.generateAiResponseStream(updatedHistory, systemInstruction).collect { chunk ->
                    fullResponse += chunk
                    _generatingMessage.value = fullResponse
                }
                repository.saveMessage(sessionId, "model", fullResponse)
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
                lastFailedMessage = newText
            } finally {
                _generatingMessage.value = null
                _isLoading.value = false
            }
        }
    }
}
