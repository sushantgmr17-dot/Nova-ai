package com.example.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingManager
import com.example.repository.ChatRepository
import com.example.model.GeneratedImage
import com.example.ui.profile.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.OutputStream

class ImageGenerationViewModel(
    private val repository: ChatRepository,
    private val billingManager: BillingManager,
    private val userProfileManager: UserProfileManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Base64 string of the generated image
    private val _generatedImageBase64 = MutableStateFlow<String?>(null)
    val generatedImageBase64: StateFlow<String?> = _generatedImageBase64.asStateFlow()

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

    val imageHistory: StateFlow<List<GeneratedImage>> = repository.getAllGeneratedImages()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun deleteImage(id: Int) {
        viewModelScope.launch {
            repository.deleteGeneratedImage(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllImages()
        }
    }

    fun clearGeneratedImage() {
        _generatedImageBase64.value = null
    }

    fun generateImage(prompt: String, style: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _generatedImageBase64.value = null
            
            try {
                val base64 = repository.generateImage(prompt, style, aspectRatio)
                _generatedImageBase64.value = base64
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveImageToGallery(context: Context, base64: String) {
        viewModelScope.launch {
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "nova_generated_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovaAI")
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    val outStream: OutputStream? = context.contentResolver.openOutputStream(it)
                    outStream?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to save image: ${e.message}"
            }
        }
    }
}
