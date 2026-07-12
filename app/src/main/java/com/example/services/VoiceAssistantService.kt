package com.example.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAssistantService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.setLanguage(Locale.US)
            isInitialized = true
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun speak(text: String) {
        if (isInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "service_tts")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
