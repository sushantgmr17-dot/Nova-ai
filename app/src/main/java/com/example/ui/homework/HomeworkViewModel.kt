package com.example.ui.homework

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
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

class HomeworkViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ocrResult = MutableStateFlow<String?>(null)
    val ocrResult: StateFlow<String?> = _ocrResult.asStateFlow()

    private val _homeworkSolution = MutableStateFlow<String?>(null)
    val homeworkSolution: StateFlow<String?> = _homeworkSolution.asStateFlow()

    private val _pdfSummary = MutableStateFlow<String?>(null)
    val pdfSummary: StateFlow<String?> = _pdfSummary.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearAll() {
        _ocrResult.value = null
        _homeworkSolution.value = null
        _pdfSummary.value = null
        _error.value = null
    }

    fun scanImageForText(base64Image: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _ocrResult.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("Please configure your Gemini API key in the Secrets panel.")
                }

                val promptPart = Part(text = "You are an advanced OCR engine. Extract all readable text from this image exactly as it appears. Do not add summaries, comments, or explanations. Just return the raw extracted text.")
                val imagePart = Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))

                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = listOf(promptPart, imagePart)))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Could not extract any text from the image.")

                _ocrResult.value = resultText
            } catch (e: Exception) {
                _error.value = "OCR Scan Failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun solveHomework(subject: String, question: String, base64Image: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _homeworkSolution.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("Please configure your Gemini API key in the Secrets panel.")
                }

                val systemPrompt = "You are Nova AI, an expert academic tutor specializing in $subject. Provide a clear, detailed, and structured step-by-step educational explanation to solve the homework query. Focus on helping the student understand the core principles, formulas, or history."

                val promptPart = Part(text = "Subject: $subject\nQuestion: $question\n\nPlease solve this step-by-step.")
                val partsList = mutableListOf(promptPart)

                if (base64Image != null) {
                    partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = partsList)),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val solution = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No solution generated.")

                _homeworkSolution.value = solution
            } catch (e: Exception) {
                _error.value = "Failed to solve: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun summarizePdf(fileName: String, fileBytes: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _pdfSummary.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("Please configure your Gemini API key in the Secrets panel.")
                }

                val promptText = "Summarize the academic/business contents of the document '$fileName' (${fileBytes.size} bytes). Provide a high-level executive summary followed by key structured takeaways in bullet format."
                
                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = listOf(Part(text = promptText))))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val summary = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Could not generate summary.")

                _pdfSummary.value = summary
            } catch (e: Exception) {
                _error.value = "PDF Summary Failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
