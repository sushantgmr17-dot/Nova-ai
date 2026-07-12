package com.example.ui.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.repository.ChatRepository
import com.example.model.*
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _userSpeechText = MutableStateFlow("")
    val userSpeechText: StateFlow<String> = _userSpeechText.asStateFlow()

    private val _aiSpeechResponse = MutableStateFlow("Tap the mic to start talking with Nova.")
    val aiSpeechResponse: StateFlow<String> = _aiSpeechResponse.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Initialize Native Speech Recognizer
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            setupRecognizerListener()
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Recognizer initialization failed: ${e.message}")
        }

        // Initialize Text-To-Speech
        textToSpeech = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("VoiceViewModel", "TTS US Language not supported")
                } else {
                    isTtsInitialized = true
                }
            }
        } else {
            Log.e("VoiceViewModel", "TTS Initialization failed")
        }
    }

    private fun setupRecognizerListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _error.value = null
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(errorType: Int) {
                _isListening.value = false
                val message = when (errorType) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice assistant busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown recognizer error"
                }
                _error.value = message
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    _userSpeechText.value = spokenText
                    sendVoiceQueryToAi(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _error.value = "Cannot start voice recognition: ${e.message}"
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    private fun sendVoiceQueryToAi(query: String) {
        viewModelScope.launch {
            _aiSpeechResponse.value = "Nova is thinking..."
            _isSpeaking.value = false

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("Please configure your Gemini API key in the Secrets panel.")
                }

                val systemPrompt = "You are Nova AI, a friendly spoken voice assistant. Keep your response extremely concise (1-3 clear sentences maximum), warm, conversational, and direct, because your output will be spoken aloud."

                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = listOf(Part(text = query)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Empty AI answer.")

                _aiSpeechResponse.value = replyText
                speakOut(replyText)
            } catch (e: Exception) {
                _aiSpeechResponse.value = "Sorry, I couldn't process that: ${e.message}"
            }
        }
    }

    private fun speakOut(text: String) {
        if (isTtsInitialized && textToSpeech != null) {
            _isSpeaking.value = true
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_tts_id")
            
            // Periodically check if TTS is still speaking to reset state
            viewModelScope.launch {
                while (textToSpeech?.isSpeaking == true) {
                    kotlinx.coroutines.delay(100)
                }
                _isSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
