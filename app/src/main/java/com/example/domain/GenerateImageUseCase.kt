package com.example.domain

import com.example.repository.ChatRepository

class GenerateImageUseCase(private val chatRepository: ChatRepository) {
    suspend fun execute(prompt: String, style: String, aspectRatio: String): String {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }
        return chatRepository.generateImage(prompt, style, aspectRatio)
    }
}
